package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.CustomerQueryBO;
import com.kakarote.ai_crm.entity.PO.CrmPluginToken;
import com.kakarote.ai_crm.entity.VO.CustomerListVO;
import com.kakarote.ai_crm.service.ICustomerService;
import com.kakarote.ai_crm.service.PluginAiUsageMeter;
import com.kakarote.ai_crm.service.PluginAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Extension Gateway（P0-B）：插件唯一入站入口。
 * 鉴权用 Bearer 插件令牌（非 Manager-Token,本路径在 SecurityConfig 放行,由网关自校验）。
 * 每次调用：校验令牌 → 限流 → scope 校验 → 写审计；AI 路由再用 PluginAiUsageMeter 包裹。
 *
 * <p>注意：真实的 CRM 资源代理（以安装者身份执行、套用数据权限）属于 P2；本阶段的资源
 * 路由返回占位响应,重点是网关的鉴权/scope/审计管道。</p>
 */
@RestController
@RequestMapping("/plugin/gateway")
@Tag(name = "插件 Extension Gateway")
public class PluginGatewayController {

    @Autowired
    private PluginAuthService pluginAuthService;

    @Autowired
    private PluginAiUsageMeter aiUsageMeter;

    @Autowired
    private ICustomerService customerService;

    @GetMapping("/whoami")
    @Operation(summary = "返回当前插件令牌身份与 scope（无需特定 scope）")
    public ResponseEntity<?> whoami(@RequestHeader(value = "Authorization", required = false) String authorization) {
        CrmPluginToken token = authenticate(authorization);
        if (token == null) {
            return unauthorized();
        }
        if (!pluginAuthService.allowRequest(token.getPluginId())) {
            return throttled(token, "GET /plugin/gateway/whoami", "");
        }
        pluginAuthService.audit(token.getPluginId(), actor(token), "GET /plugin/gateway/whoami", "", "success", "");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pluginId", token.getPluginId());
        body.put("clientId", token.getClientId());
        body.put("scopes", token.getScopes() == null ? "" : token.getScopes());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/crm/customers")
    @Operation(summary = "受限资源：需要 customer:view；以安装者身份返回真实(数据权限内)客户")
    public ResponseEntity<?> customers(@RequestHeader(value = "Authorization", required = false) String authorization) {
        CrmPluginToken token = authenticate(authorization);
        if (token == null) {
            return unauthorized();
        }
        String action = "GET /plugin/gateway/crm/customers";
        if (!pluginAuthService.allowRequest(token.getPluginId())) {
            return throttled(token, action, "customer:view");
        }
        if (!pluginAuthService.tokenHasScope(token, "customer:view")) {
            pluginAuthService.audit(token.getPluginId(), actor(token), action, "customer:view", "denied", "insufficient_scope");
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "insufficient_scope");
            err.put("required", "customer:view");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
        }
        Long actorUserId = pluginAuthService.resolveActorUserId(token.getClientId());
        if (actorUserId == null) {
            pluginAuthService.audit(token.getPluginId(), actor(token), action, "customer:view", "denied", "no_installing_actor");
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "no_installing_actor");
            err.put("error_description", "该插件无安装者身份(grant.granted_by_user_id),无法以其权限读取数据");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
        }
        try {
            // 以安装者身份执行：RBAC + 行级数据权限(GlobalDataPermissionHandler)自动生效
            Map<String, Object> data = pluginAuthService.runAsActor(actorUserId, () -> {
                CustomerQueryBO query = new CustomerQueryBO();
                query.setPage(1);
                query.setLimit(10);
                BasePage<CustomerListVO> page = customerService.queryPageList(query);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("runAsUserId", actorUserId);
                m.put("total", page.getTotal());
                m.put("customers", page.getRecords());
                return m;
            });
            pluginAuthService.audit(token.getPluginId(), actor(token), action, "customer:view", "success", "");
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            pluginAuthService.audit(token.getPluginId(), actor(token), action, "customer:view", "error", e.getMessage());
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "gateway_error");
            err.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @GetMapping("/crm/tasks")
    @Operation(summary = "示例受限资源：需要 task:create")
    public ResponseEntity<?> tasks(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return scoped(authorization, "task:create", "GET /plugin/gateway/crm/tasks", token -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("authorized", true);
            body.put("scope", "task:create");
            return body;
        });
    }

    @PostMapping("/ai/echo")
    @Operation(summary = "示例 AI 消耗型调用：演示 PluginAiUsageMeter 包裹（no-op）")
    public ResponseEntity<?> aiEcho(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestBody(required = false) Map<String, Object> requestBody) {
        CrmPluginToken token = authenticate(authorization);
        if (token == null) {
            return unauthorized();
        }
        if (!pluginAuthService.allowRequest(token.getPluginId())) {
            return throttled(token, "POST /plugin/gateway/ai/echo", "");
        }
        // AI 计量 hook：前置预检 + 后置记账（默认 no-op）
        aiUsageMeter.ensure(token.getPluginId(), 100L);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("echo", requestBody == null ? Map.of() : requestBody);
        body.put("metered", true);
        aiUsageMeter.record(token.getPluginId(), 100L);

        pluginAuthService.audit(token.getPluginId(), actor(token), "POST /plugin/gateway/ai/echo", "", "success", "");
        return ResponseEntity.ok(body);
    }

    // ---------------------------------------------------------------- helpers

    private ResponseEntity<?> scoped(String authorization, String requiredScope, String action,
                                     Function<CrmPluginToken, Object> handler) {
        CrmPluginToken token = authenticate(authorization);
        if (token == null) {
            return unauthorized();
        }
        if (!pluginAuthService.allowRequest(token.getPluginId())) {
            return throttled(token, action, requiredScope);
        }
        if (!pluginAuthService.tokenHasScope(token, requiredScope)) {
            pluginAuthService.audit(token.getPluginId(), actor(token), action, requiredScope, "denied", "insufficient_scope");
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "insufficient_scope");
            err.put("required", requiredScope);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
        }
        Object result = handler.apply(token);
        pluginAuthService.audit(token.getPluginId(), actor(token), action, requiredScope, "success", "");
        return ResponseEntity.ok(result);
    }

    private CrmPluginToken authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return pluginAuthService.validateToken(authorization.substring(7).trim());
    }

    private static String actor(CrmPluginToken token) {
        return "plugin:" + token.getClientId();
    }

    private ResponseEntity<?> unauthorized() {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("error", "invalid_token");
        err.put("error_description", "缺少或无效的插件令牌");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
    }

    private ResponseEntity<?> throttled(CrmPluginToken token, String action, String scope) {
        pluginAuthService.audit(token.getPluginId(), actor(token), action, scope, "throttled", "rate_limit");
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("error", "rate_limited");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(err);
    }
}
