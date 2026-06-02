package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.TencentMeetingBindBO;
import com.kakarote.ai_crm.entity.BO.TencentMeetingCandidateQueryBO;
import com.kakarote.ai_crm.entity.BO.TencentMeetingConfigSaveBO;
import com.kakarote.ai_crm.entity.BO.TencentMeetingQueryBO;
import com.kakarote.ai_crm.entity.BO.TencentMeetingSyncRunBO;
import com.kakarote.ai_crm.entity.BO.TencentMeetingUnbindBO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingCandidateVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingConfigVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingDetailVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingSyncStatusVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingVO;
import com.kakarote.ai_crm.service.impl.TencentMeetingServiceImpl;
import com.kakarote.ai_crm.service.impl.TencentMeetingWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tencent-meeting")
@Tag(name = "腾讯会议")
public class TencentMeetingController {

    @Autowired
    private TencentMeetingServiceImpl meetingService;

    @Autowired
    private TencentMeetingWebhookService webhookService;

    @GetMapping("/config")
    @Operation(summary = "获取腾讯会议配置")
    @RequirePermission("tencentMeeting:config")
    public Result<TencentMeetingConfigVO> getConfig() {
        return Result.ok(meetingService.getConfig());
    }

    @PostMapping("/config")
    @Operation(summary = "保存腾讯会议配置")
    @RequirePermission("tencentMeeting:config")
    public Result<TencentMeetingConfigVO> saveConfig(@RequestBody TencentMeetingConfigSaveBO saveBO) {
        return Result.ok(meetingService.saveConfig(saveBO));
    }

    @PostMapping("/sync/run")
    @Operation(summary = "运行腾讯会议同步")
    @RequirePermission("tencentMeeting:sync")
    public Result<TencentMeetingSyncStatusVO> runSync(@RequestBody(required = false) TencentMeetingSyncRunBO runBO) {
        return Result.ok(meetingService.runSync(runBO));
    }

    @GetMapping("/sync/status")
    @Operation(summary = "获取腾讯会议同步状态")
    @RequirePermission("tencentMeeting:view")
    public Result<TencentMeetingSyncStatusVO> getSyncStatus() {
        return Result.ok(meetingService.getSyncStatus());
    }

    @PostMapping("/webhook")
    @Operation(summary = "腾讯会议Webhook")
    public Result<Boolean> webhook(@RequestBody String body,
                                   @RequestHeader(value = "timestamp", required = false) String timestamp,
                                   @RequestHeader(value = "nonce", required = false) String nonce,
                                   @RequestHeader(value = "signature", required = false) String signature) {
        return Result.ok(webhookService.handleWebhook(body, timestamp, nonce, signature));
    }

    @GetMapping("/webhook")
    @Operation(summary = "腾讯会议Webhook URL验证")
    public String verifyWebhook(@RequestParam("check_str") String checkStr,
                                @RequestHeader(value = "timestamp", required = false) String timestamp,
                                @RequestHeader(value = "nonce", required = false) String nonce,
                                @RequestHeader(value = "signature", required = false) String signature) {
        return webhookService.verifyWebhookUrl(checkStr, timestamp, nonce, signature);
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "分页查询腾讯会议")
    @RequirePermission("tencentMeeting:view")
    public Result<BasePage<TencentMeetingVO>> queryPageList(@RequestBody(required = false) TencentMeetingQueryBO queryBO) {
        return Result.ok(meetingService.queryPageList(queryBO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取腾讯会议详情")
    @RequirePermission("tencentMeeting:detail")
    public Result<TencentMeetingDetailVO> getDetail(@PathVariable("id") Long id) {
        return Result.ok(meetingService.getDetail(id));
    }

    @PostMapping("/{id}/refresh")
    @Operation(summary = "刷新腾讯会议")
    @RequirePermission("tencentMeeting:sync")
    public Result<String> refresh(@PathVariable("id") Long id) {
        meetingService.refreshMeeting(id);
        return Result.ok();
    }

    @PostMapping("/bind")
    @Operation(summary = "关联客户")
    @RequirePermission("tencentMeeting:bind")
    public Result<String> bind(@RequestBody TencentMeetingBindBO bindBO) {
        meetingService.bind(bindBO);
        return Result.ok();
    }

    @PostMapping("/unbind")
    @Operation(summary = "取消关联客户")
    @RequirePermission("tencentMeeting:unbind")
    public Result<String> unbind(@RequestBody TencentMeetingUnbindBO unbindBO) {
        meetingService.unbind(unbindBO);
        return Result.ok();
    }

    @PostMapping("/candidates")
    @Operation(summary = "腾讯会议候选")
    @RequirePermission("tencentMeeting:view")
    public Result<List<TencentMeetingCandidateVO>> candidates(@RequestBody(required = false) TencentMeetingCandidateQueryBO queryBO) {
        return Result.ok(meetingService.queryCandidates(queryBO));
    }
}
