package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.config.OidcConfig;
import com.kakarote.ai_crm.entity.BO.AiConfigUpdateBO;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.BO.WeKnoraConfigUpdateBO;
import com.kakarote.ai_crm.entity.VO.AiConfigVO;
import com.kakarote.ai_crm.entity.VO.AiConnectionTestVO;
import com.kakarote.ai_crm.entity.VO.WeKnoraConfigVO;
import com.kakarote.ai_crm.entity.VO.WeKnoraConnectionTestVO;
import com.kakarote.ai_crm.service.ISystemConfigService;
import com.kakarote.ai_crm.service.OidcService;
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

/**
 * 系统配置控制器
 */
@RestController
@RequestMapping("/systemConfig")
@Tag(name = "系统配置")
public class SystemConfigController {

    @Autowired
    private ISystemConfigService systemConfigService;

    @Autowired
    private OidcService oidcService;

    @Autowired
    private OidcConfig oidcConfig;

    @Value("${minio.enabled:false}")
    private boolean minioEnabled;

    @GetMapping("/ai")
    @Operation(summary = "获取AI配置")
    public Result<AiConfigVO> getAiConfig() {
        return Result.ok(systemConfigService.getAiConfig());
    }

    @PostMapping("/ai/update")
    @Operation(summary = "更新AI配置")
    public Result<String> updateAiConfig(@Valid @RequestBody AiConfigUpdateBO updateBO) {
        systemConfigService.updateAiConfig(updateBO);
        return Result.ok();
    }

    @PostMapping("/ai/test")
    @Operation(summary = "测试AI连接")
    public Result<AiConnectionTestVO> testAiConnection(@Valid @RequestBody AiConfigUpdateBO configBO) {
        return Result.ok(systemConfigService.testAiConnection(configBO));
    }

    @GetMapping("/byType/{type}")
    @Operation(summary = "按类型获取配置")
    public Result<Map<String, String>> getConfigsByType(
            @PathVariable("type") @Parameter(description = "配置类型") String type) {
        return Result.ok(systemConfigService.getConfigsByType(type));
    }

    @PostMapping("/clearCache")
    @Operation(summary = "清除配置缓存")
    public Result<String> clearCache() {
        systemConfigService.clearConfigCache();
        return Result.ok();
    }

    @GetMapping("/minio/consoleUrl")
    @Operation(summary = "获取MinIO控制台地址")
    public Result<Map<String, Object>> getMinioConsoleUrl() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", minioEnabled);
        // 动态获取 MinIO Console URL
        String consoleUrl = RequestContextUtil.getMinioConsoleUrl(oidcConfig.getMinioConsolePort());
        result.put("consoleUrl", consoleUrl);
        return Result.ok(result);
    }

    @GetMapping("/minio/ssoUrl")
    @Operation(summary = "获取MinIO SSO登录URL")
    public Result<Map<String, Object>> getMinioSsoUrl() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", minioEnabled);

        if (!minioEnabled) {
            result.put("ssoUrl", null);
            return Result.ok(result);
        }

        // 获取当前登录用户并创建 OIDC Session
        LoginUser loginUser = UserUtil.getLoginUser();
        String sessionToken = oidcService.createSession(loginUser);

        // 返回带 session_token 的 MinIO SSO 跳转端点（动态获取 base URL）
        String baseUrl = RequestContextUtil.getBaseUrl();
        String ssoUrl = baseUrl + "/oauth2/minio-sso?session_token=" + sessionToken;
        result.put("ssoUrl", ssoUrl);
        return Result.ok(result);
    }

    // ==================== WeKnora 配置接口 ====================

    @GetMapping("/weknora")
    @Operation(summary = "获取WeKnora配置")
    public Result<WeKnoraConfigVO> getWeKnoraConfig() {
        return Result.ok(systemConfigService.getWeKnoraConfig());
    }

    @PostMapping("/weknora/update")
    @Operation(summary = "更新WeKnora配置")
    public Result<String> updateWeKnoraConfig(@Valid @RequestBody WeKnoraConfigUpdateBO updateBO) {
        systemConfigService.updateWeKnoraConfig(updateBO);
        return Result.ok();
    }

    @PostMapping("/weknora/test")
    @Operation(summary = "测试WeKnora连接")
    public Result<WeKnoraConnectionTestVO> testWeKnoraConnection(@Valid @RequestBody WeKnoraConfigUpdateBO configBO) {
        return Result.ok(systemConfigService.testWeKnoraConnection(configBO));
    }
}
