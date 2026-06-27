package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.ApplyManifestBO;
import com.kakarote.ai_crm.entity.BO.PluginClientActionBO;
import com.kakarote.ai_crm.entity.BO.PluginClientRegisterBO;
import com.kakarote.ai_crm.entity.BO.PluginGrantBO;
import com.kakarote.ai_crm.entity.BO.PluginSubscribeBO;
import com.kakarote.ai_crm.entity.PO.CrmOauthClient;
import com.kakarote.ai_crm.entity.VO.GrantResultVO;
import com.kakarote.ai_crm.entity.VO.ManifestApplyResultVO;
import com.kakarote.ai_crm.entity.VO.PluginClientRegisteredVO;
import com.kakarote.ai_crm.service.PluginAuthService;
import com.kakarote.ai_crm.service.PluginManifestService;
import com.kakarote.ai_crm.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 插件 OAuth 客户端管理（P0-A，仅超级管理员）。
 */
@RestController
@RequestMapping("/plugin/client")
@Tag(name = "插件 OAuth 客户端管理")
public class PluginClientController {

    @Autowired
    private PluginAuthService pluginAuthService;

    @Autowired
    private PluginManifestService pluginManifestService;

    @PostMapping("/register")
    @Operation(summary = "注册插件 OAuth 客户端（client_secret 仅返回一次）")
    public Result<PluginClientRegisteredVO> register(@RequestBody PluginClientRegisterBO bo) {
        requireSuperAdmin();
        return Result.ok(pluginAuthService.registerClient(bo));
    }

    @PostMapping("/list")
    @Operation(summary = "列出插件 OAuth 客户端")
    public Result<List<CrmOauthClient>> list() {
        requireSuperAdmin();
        return Result.ok(pluginAuthService.listClients());
    }

    @PostMapping("/grant")
    @Operation(summary = "为插件客户端授予 scope（实际授予 = 申请 ∩ 当前管理员所持权限）")
    public Result<GrantResultVO> grant(@RequestBody PluginGrantBO bo) {
        requireSuperAdmin();
        return Result.ok(pluginAuthService.grantScopes(bo.getClientId(), bo.getRequestedScopes(), UserUtil.getUserId()));
    }

    @PostMapping("/rotate-secret")
    @Operation(summary = "轮换 client_secret（新 secret 仅返回一次）")
    public Result<Map<String, Object>> rotateSecret(@RequestBody PluginClientActionBO bo) {
        requireSuperAdmin();
        String secret = pluginAuthService.rotateClientSecret(bo.getClientId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("clientId", bo.getClientId());
        m.put("clientSecret", secret);
        return Result.ok(m);
    }

    @PostMapping("/enable")
    @Operation(summary = "启用/停用插件客户端（停用同时吊销其令牌）")
    public Result<String> enable(@RequestBody PluginClientActionBO bo) {
        requireSuperAdmin();
        pluginAuthService.setClientEnabled(bo.getClientId(), Boolean.TRUE.equals(bo.getEnabled()));
        return Result.ok();
    }

    @PostMapping("/uninstall")
    @Operation(summary = "卸载插件客户端（反向声明式扩展 + 吊销令牌 + 删除授权 + 停用）")
    public Result<String> uninstall(@RequestBody PluginClientActionBO bo) {
        requireSuperAdmin();
        pluginAuthService.uninstallClient(bo.getClientId());
        return Result.ok();
    }

    @PostMapping("/apply-manifest")
    @Operation(summary = "为插件客户端应用声明式 manifest（v1：customFields + aiApplications）")
    public Result<ManifestApplyResultVO> applyManifest(@RequestBody ApplyManifestBO bo) {
        requireSuperAdmin();
        return Result.ok(pluginManifestService.applyManifest(bo.getClientId(), bo.getManifest()));
    }

    @PostMapping("/subscribe")
    @Operation(summary = "为插件客户端创建事件订阅（signingSecret 仅返回一次）")
    public Result<Map<String, Object>> subscribe(@RequestBody PluginSubscribeBO bo) {
        requireSuperAdmin();
        return Result.ok(pluginAuthService.createSubscription(bo.getClientId(), bo.getEventType(), bo.getEndpointUrl()));
    }

    private void requireSuperAdmin() {
        if (!Objects.equals(UserUtil.getUserId(), UserUtil.getSuperUserId())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_AUTH, "仅超级管理员可管理插件客户端");
        }
    }
}
