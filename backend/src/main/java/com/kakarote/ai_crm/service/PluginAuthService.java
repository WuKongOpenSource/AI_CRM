package com.kakarote.ai_crm.service;

import com.kakarote.ai_crm.entity.BO.PluginClientRegisterBO;
import com.kakarote.ai_crm.entity.PO.CrmOauthClient;
import com.kakarote.ai_crm.entity.PO.CrmPluginToken;
import com.kakarote.ai_crm.entity.VO.GrantResultVO;
import com.kakarote.ai_crm.entity.VO.PluginClientRegisteredVO;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 插件机器对机器授权（P0）：OAuth 客户端注册/生命周期、同意授权(下放)、
 * client_credentials 令牌签发/校验、网关用的限流/审计,以及"以安装者身份执行"。
 * 复用现有 OIDC 端点；scope 取 module:action 词表。
 */
public interface PluginAuthService {

    /** 注册一个插件 OAuth 客户端；返回的 client_secret 仅此一次可见。 */
    PluginClientRegisteredVO registerClient(PluginClientRegisterBO bo);

    /** 列出已注册客户端（不含 secret，仅哈希在库）。 */
    List<CrmOauthClient> listClients();

    /** 轮换 client_secret；返回新的明文 secret(仅此一次)。 */
    String rotateClientSecret(String clientId);

    /** 启用/停用客户端(status 1/0)。停用同时吊销其全部令牌。 */
    void setClientEnabled(String clientId, boolean enabled);

    /** 卸载：吊销全部令牌、删除授权、停用客户端。 */
    void uninstallClient(String clientId);

    /**
     * 为客户端授予 scope：实际授予 = 申请的 scope ∩ 当前管理员所持权限(下放,绝不越权)。
     * 持久化到 crm_plugin_grant（按 clientId upsert）。
     */
    GrantResultVO grantScopes(String clientId, List<String> requestedScopes, Long grantedByUserId);

    /**
     * client_credentials 授权：校验 client 凭据,签发插件令牌。令牌 scope = 该客户端的
     * 同意授权(crm_plugin_grant)；无授权则 scope 为空(网关侧一律拒绝受限调用)。
     * 成功返回标准 OAuth JSON；凭据无效返回 null。
     */
    Map<String, Object> issueClientCredentialsToken(String clientId, String clientSecret);

    /** 校验插件令牌：有效则返回令牌行,否则 null（不存在/已吊销/已过期）。 */
    CrmPluginToken validateToken(String rawToken);

    /** 吊销某插件的全部令牌（停用/卸载时调用）。 */
    void revokeTokensForPlugin(String pluginId);

    /** 令牌是否持有某 scope（解析 CSV）。 */
    boolean tokenHasScope(CrmPluginToken token, String requiredScope);

    /** 每插件限流（Redis 滑窗,默认 120 次/分钟）。返回 true 表示放行。 */
    boolean allowRequest(String pluginId);

    /** 写一条不可变审计。 */
    void audit(String pluginId, String actor, String action, String scope, String result, String requestMeta);

    /** 创建事件订阅；返回 {subscriptionId, eventType, endpointUrl, signingSecret(仅此一次)}。 */
    Map<String, Object> createSubscription(String clientId, String eventType, String endpointUrl);

    /** 删除某插件的全部订阅（卸载时调用）。 */
    void deleteSubscriptionsForPlugin(String pluginId);

    /** 解析某客户端的"安装者"用户ID（= 同意授权的 granted_by_user_id）；无则 null。 */
    Long resolveActorUserId(String clientId);

    /**
     * 以安装者身份执行：临时把 Spring Security 上下文设为该用户,使 RBAC 与行级数据权限
     * (GlobalDataPermissionHandler) 正确生效；finally 中清理 DataPermissionHolder 并还原上下文。
     */
    <T> T runAsActor(Long actorUserId, Supplier<T> action);
}
