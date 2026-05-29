package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.config.OidcConfig;
import com.kakarote.ai_crm.entity.BO.AiCreditRecordQueryBO;
import com.kakarote.ai_crm.entity.BO.AiProviderActivateBO;
import com.kakarote.ai_crm.entity.BO.AiConfigUpdateBO;
import com.kakarote.ai_crm.entity.BO.EnterpriseConfigUpdateBO;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.VO.AiBillingConfigVO;
import com.kakarote.ai_crm.entity.VO.AiConfigVO;
import com.kakarote.ai_crm.entity.VO.AiConnectionTestVO;
import com.kakarote.ai_crm.entity.VO.AiCreditRecordVO;
import com.kakarote.ai_crm.entity.VO.EnterpriseConfigVO;
import com.kakarote.ai_crm.service.ISystemConfigService;
import com.kakarote.ai_crm.service.OidcService;
import com.kakarote.ai_crm.service.impl.AiCreditRecordService;
import com.kakarote.ai_crm.utils.RequestContextUtil;
import com.kakarote.ai_crm.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/systemConfig")
@Tag(name = "System Config APIs")
public class SystemConfigController {

    @Autowired
    private ISystemConfigService systemConfigService;

    @Autowired
    private AiCreditRecordService aiCreditRecordService;

    @Autowired
    private OidcService oidcService;

    @Autowired
    private OidcConfig oidcConfig;

    @Value("${minio.enabled:false}")
    private boolean minioEnabled;

    /**
     * 获取AI 配置。
     */
    @GetMapping("/ai")
    @Operation(summary = "Get AI config")
    public Result<AiConfigVO> getAiConfig() {
        return Result.ok(systemConfigService.getAiConfig());
    }

    /**
     * 获取AI 配置详情。
     */
    @GetMapping("/ai/detail")
    @Operation(summary = "Get AI config detail")
    @RequirePermission("config:ai")
    public Result<AiConfigVO> getAiConfigDetail() {
        return Result.ok(systemConfigService.getAiConfigDetail());
    }

    /**
     * 更新AI 配置。
     */
    @PostMapping("/ai/update")
    @Operation(summary = "Update AI config")
    @RequirePermission("config:ai")
    public Result<String> updateAiConfig(@Valid @RequestBody AiConfigUpdateBO updateBO) {
        systemConfigService.updateAiConfig(updateBO);
        return Result.ok();
    }

    /**
     * 激活AI 服务商。
     */
    @PostMapping("/ai/activate")
    @Operation(summary = "Activate saved AI provider")
    @RequirePermission("config:ai")
    public Result<String> activateAiProvider(@Valid @RequestBody AiProviderActivateBO activateBO) {
        systemConfigService.activateAiProvider(activateBO.getProvider());
        return Result.ok();
    }

    /**
     * 切换使用赠送AI 配置。
     */
    @PostMapping("/ai/useGift")
    @Operation(summary = "Use gift AI quota")
    @RequirePermission("config:ai")
    public Result<String> useGiftAiConfig() {
        systemConfigService.useGiftAiConfig();
        return Result.ok();
    }

    /**
     * 切换使用自定义AI 配置。
     */
    @PostMapping("/ai/useCustom")
    @Operation(summary = "Use saved custom AI config")
    @RequirePermission("config:ai")
    public Result<String> useCustomAiConfig() {
        systemConfigService.useCustomAiConfig();
        return Result.ok();
    }

    @GetMapping("/ai/billing")
    @Operation(summary = "Get AI billing config")
    @RequirePermission("config:ai")
    public Result<AiBillingConfigVO> getAiBillingConfig() {
        return Result.ok(systemConfigService.getAiBillingConfig());
    }

    @PostMapping("/ai/creditRecords/queryPageList")
    @Operation(summary = "Query AI credit records")
    @RequirePermission("config:ai")
    public Result<BasePage<AiCreditRecordVO>> queryAiCreditRecords(@RequestBody(required = false) AiCreditRecordQueryBO queryBO) {
        return Result.ok(aiCreditRecordService.queryPageList(queryBO));
    }

    /**
     * 处理testAiConnection方法逻辑。
     */
    @PostMapping("/ai/test")
    @Operation(summary = "Test AI connection")
    @RequirePermission("config:ai")
    public Result<AiConnectionTestVO> testAiConnection(@Valid @RequestBody AiConfigUpdateBO configBO) {
        return Result.ok(systemConfigService.testAiConnection(configBO));
    }

    /**
     * 获取配置按类型。
     */
    @GetMapping("/byType/{type}")
    @Operation(summary = "Get config by type")
    @RequirePermission("config")
    public Result<Map<String, String>> getConfigsByType(
            @PathVariable("type") @Parameter(description = "Config type") String type) {
        return Result.ok(systemConfigService.getConfigsByType(type));
    }

    /**
     * 清理缓存。
     */
    @PostMapping("/clearCache")
    @Operation(summary = "Clear config cache")
    @RequirePermission("config")
    public Result<String> clearCache() {
        systemConfigService.clearConfigCache();
        return Result.ok();
    }

    /**
     * 获取MinioConsole地址。
     */
    @GetMapping("/minio/consoleUrl")
    @Operation(summary = "Get MinIO console url")
    @RequirePermission("config:storage")
    public Result<Map<String, Object>> getMinioConsoleUrl() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", minioEnabled);
        String consoleUrl = RequestContextUtil.getMinioConsoleUrl(oidcConfig.getMinioConsolePort());
        result.put("consoleUrl", consoleUrl);
        return Result.ok(result);
    }

    /**
     * 获取MinioSSO地址。
     */
    @GetMapping("/minio/ssoUrl")
    @Operation(summary = "Get MinIO SSO url")
    @RequirePermission("config:storage")
    public Result<Map<String, Object>> getMinioSsoUrl() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", minioEnabled);

        if (!minioEnabled) {
            result.put("ssoUrl", null);
            return Result.ok(result);
        }

        LoginUser loginUser = UserUtil.getLoginUser();
        String sessionToken = oidcService.createSession(loginUser);

        String baseUrl = RequestContextUtil.getBaseUrl();
        String ssoUrl = baseUrl + "/oauth2/minio-sso?session_token=" + sessionToken;
        result.put("ssoUrl", ssoUrl);
        return Result.ok(result);
    }

    /**
     * 获取企业配置。
     */
    @GetMapping("/enterprise")
    @Operation(summary = "Get enterprise config")
    public Result<EnterpriseConfigVO> getEnterpriseConfig() {
        return Result.ok(systemConfigService.getEnterpriseConfig());
    }

    /**
     * 更新企业配置。
     */
    @PostMapping("/enterprise/update")
    @Operation(summary = "Update enterprise config")
    @RequirePermission("config:storage")
    public Result<String> updateEnterpriseConfig(@Valid @RequestBody EnterpriseConfigUpdateBO updateBO) {
        systemConfigService.updateEnterpriseConfig(updateBO);
        return Result.ok();
    }
}
