package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.entity.BO.PluginClientRegisterBO;
import com.kakarote.ai_crm.entity.PO.CrmOauthClient;
import com.kakarote.ai_crm.entity.PO.CrmPluginAuditLog;
import com.kakarote.ai_crm.entity.PO.CrmPluginGrant;
import com.kakarote.ai_crm.entity.PO.CrmPluginSubscription;
import com.kakarote.ai_crm.entity.PO.CrmPluginToken;
import com.kakarote.ai_crm.entity.VO.GrantResultVO;
import com.kakarote.ai_crm.entity.VO.PluginClientRegisteredVO;
import com.kakarote.ai_crm.mapper.CrmOauthClientMapper;
import com.kakarote.ai_crm.mapper.CrmPluginAuditLogMapper;
import com.kakarote.ai_crm.mapper.CrmPluginGrantMapper;
import com.kakarote.ai_crm.mapper.CrmPluginSubscriptionMapper;
import com.kakarote.ai_crm.mapper.CrmPluginTokenMapper;
import com.kakarote.ai_crm.common.auth.DataPermissionHolder;
import com.kakarote.ai_crm.common.enums.LoginTypeEnum;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.service.ManageUserService;
import com.kakarote.ai_crm.service.PermissionService;
import com.kakarote.ai_crm.service.PluginAuthService;
import com.kakarote.ai_crm.service.PluginManifestService;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
public class PluginAuthServiceImpl implements PluginAuthService {

    /** 插件令牌有效期（秒）。 */
    private static final long TOKEN_TTL_SECONDS = 3600L;
    /** 每插件每分钟最大网关调用数。 */
    private static final long RATE_LIMIT_PER_MINUTE = 120L;

    @Autowired
    private CrmOauthClientMapper oauthClientMapper;

    @Autowired
    private CrmPluginTokenMapper pluginTokenMapper;

    @Autowired
    private CrmPluginGrantMapper pluginGrantMapper;

    @Autowired
    private CrmPluginAuditLogMapper pluginAuditLogMapper;

    @Autowired
    private CrmPluginSubscriptionMapper pluginSubscriptionMapper;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ManageUserService manageUserService;

    @Autowired
    private PluginManifestService pluginManifestService;

    @Autowired
    private SecretTextCipher secretTextCipher;

    @Override
    public PluginClientRegisteredVO registerClient(PluginClientRegisterBO bo) {
        String clientId = "plg_" + IdUtil.fastSimpleUUID();
        String rawSecret = IdUtil.fastSimpleUUID() + IdUtil.fastSimpleUUID();

        List<String> grantTypes = (bo.getGrantTypes() == null || bo.getGrantTypes().isEmpty())
                ? List.of("client_credentials")
                : bo.getGrantTypes();

        CrmOauthClient client = new CrmOauthClient();
        client.setClientId(clientId);
        client.setPluginId(bo.getPluginId());
        client.setDisplayName(bo.getDisplayName());
        client.setClientSecretHash(sha256Hex(rawSecret));
        client.setGrantTypes(joinCsv(grantTypes));
        client.setDefaultScopes(joinCsv(bo.getDefaultScopes()));
        client.setRedirectUris(joinCsv(bo.getRedirectUris()));
        client.setStatus(1);
        oauthClientMapper.insert(client);

        PluginClientRegisteredVO vo = new PluginClientRegisteredVO();
        vo.setClientId(clientId);
        vo.setClientSecret(rawSecret);
        vo.setPluginId(bo.getPluginId());
        vo.setDisplayName(bo.getDisplayName());
        log.info("注册插件 OAuth 客户端: clientId={}, pluginId={}", clientId, bo.getPluginId());
        return vo;
    }

    @Override
    public List<CrmOauthClient> listClients() {
        List<CrmOauthClient> list = oauthClientMapper.selectList(
                new LambdaQueryWrapper<CrmOauthClient>().orderByDesc(CrmOauthClient::getCreateTime));
        for (CrmOauthClient c : list) {
            c.setClientSecretHash(null);
        }
        return list;
    }

    @Override
    public GrantResultVO grantScopes(String clientId, List<String> requestedScopes, Long grantedByUserId) {
        CrmOauthClient client = oauthClientMapper.selectOne(
                new LambdaQueryWrapper<CrmOauthClient>().eq(CrmOauthClient::getClientId, clientId));
        List<String> granted = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        if (requestedScopes != null) {
            for (String scope : requestedScopes) {
                String s = StrUtil.trimToEmpty(scope);
                if (s.isEmpty()) {
                    continue;
                }
                // 下放：管理员本身持有该 module:action 才能授予
                if (permissionService.hasPermission(s)) {
                    granted.add(s);
                } else {
                    rejected.add(s);
                }
            }
        }

        if (client != null) {
            CrmPluginGrant existing = pluginGrantMapper.selectOne(
                    new LambdaQueryWrapper<CrmPluginGrant>().eq(CrmPluginGrant::getClientId, clientId));
            if (existing == null) {
                CrmPluginGrant grant = new CrmPluginGrant();
                grant.setPluginId(client.getPluginId());
                grant.setClientId(clientId);
                grant.setGrantedScopes(joinCsv(granted));
                grant.setGrantedByUserId(grantedByUserId);
                pluginGrantMapper.insert(grant);
            } else {
                existing.setGrantedScopes(joinCsv(granted));
                existing.setGrantedByUserId(grantedByUserId);
                pluginGrantMapper.updateById(existing);
            }
        }

        GrantResultVO vo = new GrantResultVO();
        vo.setClientId(clientId);
        vo.setGranted(granted);
        vo.setRejected(rejected);
        log.info("插件授权: clientId={}, granted={}, rejected={}", clientId, granted, rejected);
        return vo;
    }

    @Override
    public Map<String, Object> issueClientCredentialsToken(String clientId, String clientSecret) {
        if (StrUtil.isBlank(clientId) || StrUtil.isBlank(clientSecret)) {
            return null;
        }
        CrmOauthClient client = oauthClientMapper.selectOne(
                new LambdaQueryWrapper<CrmOauthClient>().eq(CrmOauthClient::getClientId, clientId));
        if (client == null || client.getStatus() == null || client.getStatus() != 1) {
            return null;
        }
        if (!csvContains(client.getGrantTypes(), "client_credentials")) {
            return null;
        }
        if (!constantTimeEquals(sha256Hex(clientSecret), client.getClientSecretHash())) {
            return null;
        }

        // 令牌 scope = 同意授权(crm_plugin_grant)；无授权则为空
        CrmPluginGrant grant = pluginGrantMapper.selectOne(
                new LambdaQueryWrapper<CrmPluginGrant>().eq(CrmPluginGrant::getClientId, clientId));
        String tokenScopes = (grant != null && StrUtil.isNotBlank(grant.getGrantedScopes()))
                ? grant.getGrantedScopes() : "";

        String rawToken = IdUtil.fastSimpleUUID() + IdUtil.fastSimpleUUID();
        Date expiresAt = new Date(System.currentTimeMillis() + TOKEN_TTL_SECONDS * 1000L);

        CrmPluginToken token = new CrmPluginToken();
        token.setPluginId(client.getPluginId());
        token.setClientId(clientId);
        token.setTokenHash(sha256Hex(rawToken));
        token.setScopes(tokenScopes);
        token.setExpiresAt(expiresAt);
        token.setRevoked(0);
        pluginTokenMapper.insert(token);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("access_token", rawToken);
        resp.put("token_type", "Bearer");
        resp.put("expires_in", TOKEN_TTL_SECONDS);
        resp.put("scope", scopesToSpaceDelimited(tokenScopes));
        log.info("签发插件令牌: clientId={}, pluginId={}, scopes={}", clientId, client.getPluginId(), tokenScopes);
        return resp;
    }

    @Override
    public CrmPluginToken validateToken(String rawToken) {
        if (StrUtil.isBlank(rawToken)) {
            return null;
        }
        CrmPluginToken token = pluginTokenMapper.selectOne(
                new LambdaQueryWrapper<CrmPluginToken>()
                        .eq(CrmPluginToken::getTokenHash, sha256Hex(rawToken))
                        .eq(CrmPluginToken::getRevoked, 0));
        if (token == null) {
            return null;
        }
        if (token.getExpiresAt() != null && token.getExpiresAt().before(new Date())) {
            return null;
        }
        return token;
    }

    @Override
    public void revokeTokensForPlugin(String pluginId) {
        if (StrUtil.isBlank(pluginId)) {
            return;
        }
        CrmPluginToken patch = new CrmPluginToken();
        patch.setRevoked(1);
        pluginTokenMapper.update(patch,
                new LambdaQueryWrapper<CrmPluginToken>().eq(CrmPluginToken::getPluginId, pluginId));
    }

    @Override
    public boolean tokenHasScope(CrmPluginToken token, String requiredScope) {
        if (token == null) {
            return false;
        }
        if (StrUtil.isBlank(requiredScope)) {
            return true;
        }
        return csvContains(token.getScopes(), requiredScope);
    }

    @Override
    public boolean allowRequest(String pluginId) {
        if (StrUtil.isBlank(pluginId)) {
            return true;
        }
        try {
            String key = "plugin:ratelimit:" + pluginId;
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, 60, TimeUnit.SECONDS);
            }
            return count == null || count <= RATE_LIMIT_PER_MINUTE;
        } catch (Exception e) {
            // 限流不可用时不阻断业务
            log.warn("插件限流检查失败,放行: plugin={}, err={}", pluginId, e.getMessage());
            return true;
        }
    }

    @Override
    public void audit(String pluginId, String actor, String action, String scope, String result, String requestMeta) {
        try {
            CrmPluginAuditLog row = new CrmPluginAuditLog();
            row.setPluginId(pluginId);
            row.setActor(actor);
            row.setAction(action);
            row.setScope(scope);
            row.setResult(result);
            row.setRequestMeta(requestMeta);
            pluginAuditLogMapper.insert(row);
        } catch (Exception e) {
            log.warn("写插件审计失败: plugin={}, action={}, err={}", pluginId, action, e.getMessage());
        }
    }

    @Override
    public String rotateClientSecret(String clientId) {
        CrmOauthClient client = oauthClientMapper.selectOne(
                new LambdaQueryWrapper<CrmOauthClient>().eq(CrmOauthClient::getClientId, clientId));
        if (client == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_DATA_DOES_NOT_EXIST, "客户端不存在");
        }
        String rawSecret = IdUtil.fastSimpleUUID() + IdUtil.fastSimpleUUID();
        client.setClientSecretHash(sha256Hex(rawSecret));
        oauthClientMapper.updateById(client);
        log.info("轮换插件 client_secret: clientId={}", clientId);
        return rawSecret;
    }

    @Override
    public void setClientEnabled(String clientId, boolean enabled) {
        CrmOauthClient client = oauthClientMapper.selectOne(
                new LambdaQueryWrapper<CrmOauthClient>().eq(CrmOauthClient::getClientId, clientId));
        if (client == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_DATA_DOES_NOT_EXIST, "客户端不存在");
        }
        client.setStatus(enabled ? 1 : 0);
        oauthClientMapper.updateById(client);
        if (!enabled && StrUtil.isNotBlank(client.getPluginId())) {
            revokeTokensForPlugin(client.getPluginId());
        }
        log.info("插件 client 状态: clientId={}, enabled={}", clientId, enabled);
    }

    @Override
    public void uninstallClient(String clientId) {
        CrmOauthClient client = oauthClientMapper.selectOne(
                new LambdaQueryWrapper<CrmOauthClient>().eq(CrmOauthClient::getClientId, clientId));
        if (client == null) {
            return;
        }
        if (StrUtil.isNotBlank(client.getPluginId())) {
            // 反向声明式扩展（删除其新建的自定义字段等），删订阅，再吊销令牌
            pluginManifestService.reverseForPlugin(client.getPluginId());
            deleteSubscriptionsForPlugin(client.getPluginId());
            revokeTokensForPlugin(client.getPluginId());
        }
        pluginGrantMapper.delete(new LambdaQueryWrapper<CrmPluginGrant>().eq(CrmPluginGrant::getClientId, clientId));
        client.setStatus(0);
        oauthClientMapper.updateById(client);
        log.info("卸载插件 client: clientId={}", clientId);
    }

    @Override
    public Long resolveActorUserId(String clientId) {
        CrmPluginGrant grant = pluginGrantMapper.selectOne(
                new LambdaQueryWrapper<CrmPluginGrant>().eq(CrmPluginGrant::getClientId, clientId));
        return grant == null ? null : grant.getGrantedByUserId();
    }

    @Override
    public <T> T runAsActor(Long actorUserId, Supplier<T> action) {
        if (actorUserId == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_AUTH, "缺少安装者身份");
        }
        ManagerUser user = manageUserService.getById(actorUserId);
        if (user == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_USER_DOES_NOT_EXIST, "安装者用户不存在");
        }
        Authentication previous = SecurityContextHolder.getContext().getAuthentication();
        try {
            LoginUser loginUser = new LoginUser();
            loginUser.setUser(user);
            loginUser.setLoginType(LoginTypeEnum.PC);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    loginUser, null, loginUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            return action.get();
        } finally {
            // 防止 ThreadLocal 泄漏：清数据权限缓存并还原上下文
            DataPermissionHolder.clear();
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }

    @Override
    public Map<String, Object> createSubscription(String clientId, String eventType, String endpointUrl) {
        if (StrUtil.isBlank(eventType) || StrUtil.isBlank(endpointUrl)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "eventType / endpointUrl 必填");
        }
        CrmOauthClient client = oauthClientMapper.selectOne(
                new LambdaQueryWrapper<CrmOauthClient>().eq(CrmOauthClient::getClientId, clientId));
        if (client == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_DATA_DOES_NOT_EXIST, "客户端不存在");
        }
        String rawSecret = IdUtil.fastSimpleUUID() + IdUtil.fastSimpleUUID();
        CrmPluginSubscription sub = new CrmPluginSubscription();
        sub.setPluginId(client.getPluginId());
        sub.setEventType(eventType.trim());
        sub.setEndpointUrl(endpointUrl.trim());
        sub.setSigningSecretEncrypted(secretTextCipher.encrypt(rawSecret));
        sub.setActive(1);
        pluginSubscriptionMapper.insert(sub);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("subscriptionId", String.valueOf(sub.getSubscriptionId()));
        m.put("eventType", eventType);
        m.put("endpointUrl", endpointUrl);
        m.put("signingSecret", rawSecret);
        log.info("创建插件订阅: pluginId={}, event={}, url={}", client.getPluginId(), eventType, endpointUrl);
        return m;
    }

    @Override
    public void deleteSubscriptionsForPlugin(String pluginId) {
        if (StrUtil.isBlank(pluginId)) {
            return;
        }
        pluginSubscriptionMapper.delete(
                new LambdaQueryWrapper<CrmPluginSubscription>().eq(CrmPluginSubscription::getPluginId, pluginId));
    }

    // ---------------------------------------------------------------- helpers

    private static String joinCsv(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(",", values);
    }

    private static boolean csvContains(String csv, String value) {
        if (StrUtil.isBlank(csv)) {
            return false;
        }
        for (String p : Arrays.asList(csv.split(","))) {
            if (value.equals(p.trim())) {
                return true;
            }
        }
        return false;
    }

    private static String scopesToSpaceDelimited(String csv) {
        if (StrUtil.isBlank(csv)) {
            return "";
        }
        return csv.replace(",", " ").trim();
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
