package com.kakarote.syncdata.controller;

import com.kakarote.syncdata.model.CompanyBinding;
import com.kakarote.syncdata.model.MigrationPreflightResult;
import com.kakarote.syncdata.model.OldCompanyOption;
import com.kakarote.syncdata.SyncProperties;
import com.kakarote.syncdata.mq.RocketMqSyncSettings;
import com.kakarote.syncdata.service.CompanyBindingService;
import com.kakarote.syncdata.service.FullSyncService;
import com.kakarote.syncdata.service.MigrationPreflightService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/sync")
public class SyncBindingController {

    private final CompanyBindingService bindingService;
    private final FullSyncService fullSyncService;
    private final MigrationPreflightService preflightService;
    private final JdbcTemplate target;
    private final SyncProperties properties;

    /**
     * 注入绑定服务、全量同步服务和目标库查询组件。
     */
    public SyncBindingController(CompanyBindingService bindingService,
                                 FullSyncService fullSyncService,
                                 MigrationPreflightService preflightService,
                                 @Qualifier("targetJdbcTemplate") JdbcTemplate targetJdbcTemplate,
                                 SyncProperties properties) {
        this.bindingService = bindingService;
        this.fullSyncService = fullSyncService;
        this.preflightService = preflightService;
        this.target = targetJdbcTemplate;
        this.properties = properties;
    }

    /**
     * 查询老 wk_crm 库中可供绑定的 company_id 列表。
     * managerPhone 为空时返回全部；不为空时仅返回 company_manage 等于该手机号的企业。
     */
    @GetMapping("/old-companies")
    public List<OldCompanyOption> oldCompanies(@RequestParam(required = false) String managerPhone) {
        return bindingService.listOldCompanies(managerPhone);
    }

    /**
     * 查询已保存的 ai_crm 租户与 wk_crm 公司绑定关系。
     */
    @GetMapping("/bindings")
    public List<CompanyBinding> bindings() {
        fullSyncService.recoverInactiveRunningJobs();
        return bindingService.listBindings();
    }

    /**
     * 在启动迁移前检查源库、目标库、绑定冲突、模块覆盖和增量能力状态。
     */
    @GetMapping("/preflight")
    public MigrationPreflightResult preflight(@RequestParam Long tenantId,
                                              @RequestParam Long companyId,
                                              @RequestParam(required = false) Boolean incrementalEnabled) {
        return preflightService.preflight(tenantId, companyId, incrementalEnabled);
    }

    /**
     * 查询同步服务当前暴露的功能能力。
     */
    @GetMapping("/capabilities")
    public Map<String, Object> capabilities() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("incrementalApplicationAvailable", properties.getRocketmq().isEnabled());
        result.put("incrementalStatus", properties.getRocketmq().isEnabled() ? "available" : "rocketmq_disabled");
        result.put("incrementalMessage", properties.getRocketmq().isEnabled()
                ? "双向增量同步已启用，CRM 与 AICRM 的后续变更将自动同步。"
                : "双向增量同步暂不可用，请联系管理员检查系统消息通道配置。");
        result.put("crmToAicrmAvailable", properties.getRocketmq().isEnabled());
        result.put("aicrmToCrmAvailable", properties.getRocketmq().isEnabled());
        result.put("mqTopic", RocketMqSyncSettings.topic(properties));
        result.put("crmToAicrmTopic", RocketMqSyncSettings.topic(properties));
        result.put("crmToAicrmTag", RocketMqSyncSettings.crmToAicrmTag(properties));
        result.put("crmToAicrmConsumerGroup", RocketMqSyncSettings.crmToAicrmGroup(properties));
        result.put("aicrmToCrmTopic", RocketMqSyncSettings.topic(properties));
        result.put("aicrmToCrmTag", RocketMqSyncSettings.aicrmToCrmTag(properties));
        result.put("aicrmToCrmProducerGroup", RocketMqSyncSettings.aicrmToCrmGroup(properties));
        return result;
    }

    /**
     * 新建或更新 ai_crm 租户与 wk_crm company_id 的绑定关系。
     */
    @PostMapping("/bindings")
    public CompanyBinding bind(@Valid @RequestBody BindCompanyRequest request) {
        String mqTopic = firstNonBlank(request.mqTopic(), request.crmToAicrmTopic(), request.aicrmToCrmTopic());
        return bindingService.bind(
                request.tenantId(),
                request.companyId(),
                request.crmToAicrmEnabled() == null ? request.incrementalEnabled() : request.crmToAicrmEnabled(),
                request.aicrmToCrmEnabled(),
                mqTopic,
                request.crmToAicrmGroup() == null ? request.mqGroup() : request.crmToAicrmGroup(),
                mqTopic,
                request.aicrmToCrmGroup(),
                request.remark()
        );
    }

    private String firstNonBlank(String... values) {
        return RocketMqSyncSettings.firstNonBlank(values);
    }

    /**
     * 根据绑定关系启动一次指定 company_id 的全量同步，并返回同步任务编号。
     */
    @PostMapping("/bindings/{bindingId}/full-sync")
    public Map<String, Object> startFullSync(@PathVariable Long bindingId) {
        CompanyBinding binding = bindingService.getBinding(bindingId);
        long jobId = fullSyncService.startCompanyJob(binding.tenantId(), binding.sourceCompanyId(), binding.bindingId());
        return Map.of("jobId", jobId, "bindingId", bindingId, "status", "running");
    }

    /**
     * 查询全量同步任务的汇总信息。
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<Map<String, Object>> job(@PathVariable Long jobId) {
        fullSyncService.recoverInactiveRunningJobs();
        List<Map<String, Object>> rows = target.queryForList(
                "SELECT * FROM sync_full_job WHERE job_id = ?",
                jobId
        );
        if (rows.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rows.get(0));
    }

    /**
     * 查询全量同步任务下各业务模块的同步统计。
     */
    @GetMapping("/jobs/{jobId}/modules")
    public List<Map<String, Object>> jobModules(@PathVariable Long jobId) {
        return target.queryForList("""
                SELECT *
                FROM sync_job_module
                WHERE job_id = ?
                ORDER BY id
                """, jobId);
    }

    /**
     * 查询全量同步任务执行过程中记录的最近错误明细。
     */
    @GetMapping("/jobs/{jobId}/errors")
    public List<Map<String, Object>> jobErrors(@PathVariable Long jobId) {
        return target.queryForList("""
                SELECT *
                FROM sync_job_error
                WHERE job_id = ?
                ORDER BY id DESC
                LIMIT 200
                """, jobId);
    }

    /**
     * 新建或更新租户与公司绑定关系的请求体。
     */
    public record BindCompanyRequest(
            @NotNull Long tenantId,
            @NotNull Long companyId,
            Boolean incrementalEnabled,
            String mqTopic,
            String mqGroup,
            Boolean crmToAicrmEnabled,
            Boolean aicrmToCrmEnabled,
            String crmToAicrmTopic,
            String crmToAicrmGroup,
            String aicrmToCrmTopic,
            String aicrmToCrmGroup,
            String remark
    ) {
    }
}
