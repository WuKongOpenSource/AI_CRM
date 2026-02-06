package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.kakarote.ai_crm.config.OidcConfig;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.service.ManageUserService;
import com.kakarote.ai_crm.service.OidcService;
import com.kakarote.ai_crm.utils.RequestContextUtil;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * OIDC 服务实现
 * 用于 MinIO SSO 登录
 */
@Slf4j
@Service
public class OidcServiceImpl implements OidcService {

    private static final String OIDC_SESSION_KEY = "oidc:session:";
    private static final String OIDC_CODE_KEY = "oidc:code:";
    private static final String OIDC_ACCESS_TOKEN_KEY = "oidc:access_token:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private OidcConfig oidcConfig;

    @Autowired
    private ManageUserService manageUserService;

    @Override
    public String createSession(LoginUser loginUser) {
        String sessionId = IdUtil.fastSimpleUUID();
        String key = OIDC_SESSION_KEY + sessionId;
        // Session 有效期与 token 有效期一致
        redisTemplate.opsForValue().set(key, JSON.toJSONString(loginUser), oidcConfig.getTokenExpiry(), TimeUnit.SECONDS);
        log.debug("创建 OIDC Session: {}", sessionId);
        return sessionId;
    }

    @Override
    public LoginUser getLoginUserBySession(String sessionId) {
        if (StrUtil.isEmpty(sessionId)) {
            return null;
        }
        String key = OIDC_SESSION_KEY + sessionId;
        String json = redisTemplate.opsForValue().get(key);
        if (StrUtil.isEmpty(json)) {
            return null;
        }
        return JSON.parseObject(json, LoginUser.class);
    }

    @Override
    public void removeSession(String sessionId) {
        if (StrUtil.isNotEmpty(sessionId)) {
            String key = OIDC_SESSION_KEY + sessionId;
            redisTemplate.delete(key);
            log.debug("删除 OIDC Session: {}", sessionId);
        }
    }

    @Override
    public String generateAuthorizationCode(Long userId, String redirectUri) {
        String code = IdUtil.fastSimpleUUID();
        String key = OIDC_CODE_KEY + code;
        // 存储格式: userId|redirectUri
        String value = userId + "|" + redirectUri;
        redisTemplate.opsForValue().set(key, value, oidcConfig.getCodeExpiry(), TimeUnit.SECONDS);
        log.debug("生成授权码: {} for user: {}", code, userId);
        return code;
    }

    @Override
    public Long exchangeCode(String code, String redirectUri) {
        if (StrUtil.isEmpty(code)) {
            return null;
        }
        String key = OIDC_CODE_KEY + code;
        String value = redisTemplate.opsForValue().get(key);
        if (StrUtil.isEmpty(value)) {
            log.warn("授权码不存在或已过期: {}", code);
            return null;
        }
        // 消费授权码（一次性使用）
        redisTemplate.delete(key);

        String[] parts = value.split("\\|", 2);
        if (parts.length != 2) {
            log.warn("授权码格式错误: {}", code);
            return null;
        }

        String storedRedirectUri = parts[1];
        if (!storedRedirectUri.equals(redirectUri)) {
            log.warn("redirect_uri 不匹配: expected={}, actual={}", storedRedirectUri, redirectUri);
            return null;
        }

        return Long.parseLong(parts[0]);
    }

    @Override
    public String generateIdToken(LoginUser loginUser) {
        return generateIdToken(loginUser, RequestContextUtil.getBaseUrl());
    }

    @Override
    public String generateIdToken(LoginUser loginUser, String issuer) {
        try {
            ManagerUser user = loginUser.getUser();
            Date now = new Date();
            Date expiry = new Date(now.getTime() + oidcConfig.getTokenExpiry() * 1000L);

            // 使用动态获取的 issuer
            String actualIssuer = StrUtil.isNotBlank(issuer) ? issuer : "http://localhost";

            // 构建 JWT Claims
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(actualIssuer)
                    .subject(String.valueOf(user.getUserId()))
                    .audience(oidcConfig.getClientId())
                    .issueTime(now)
                    .expirationTime(expiry)
                    .claim("name", user.getRealname())
                    .claim("preferred_username", user.getUsername())
                    .claim("email", user.getEmail())
                    .claim("policy", determineMinioPolicy(loginUser))
                    .build();

            // 签名 JWT
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(oidcConfig.getKeyId())
                    .type(JOSEObjectType.JWT)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            JWSSigner signer = new RSASSASigner(oidcConfig.getPrivateKey());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("生成 ID Token 失败", e);
            throw new RuntimeException("生成 ID Token 失败", e);
        }
    }

    @Override
    public String generateAccessToken(LoginUser loginUser) {
        String accessToken = IdUtil.fastSimpleUUID();
        String key = OIDC_ACCESS_TOKEN_KEY + accessToken;
        redisTemplate.opsForValue().set(key, JSON.toJSONString(loginUser), oidcConfig.getTokenExpiry(), TimeUnit.SECONDS);
        log.debug("生成 Access Token: {}", accessToken);
        return accessToken;
    }

    @Override
    public Map<String, Object> getUserInfoByAccessToken(String accessToken) {
        if (StrUtil.isEmpty(accessToken)) {
            return null;
        }
        // 移除 Bearer 前缀
        if (accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        String key = OIDC_ACCESS_TOKEN_KEY + accessToken;
        String json = redisTemplate.opsForValue().get(key);
        if (StrUtil.isEmpty(json)) {
            return null;
        }

        LoginUser loginUser = JSON.parseObject(json, LoginUser.class);
        ManagerUser user = loginUser.getUser();

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("sub", String.valueOf(user.getUserId()));
        userInfo.put("name", user.getRealname());
        userInfo.put("preferred_username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("policy", determineMinioPolicy(loginUser));

        return userInfo;
    }

    @Override
    public String getJwks() {
        try {
            JWKSet jwkSet = new JWKSet(oidcConfig.getRsaKey().toPublicJWK());
            return jwkSet.toString();
        } catch (Exception e) {
            log.error("获取 JWKS 失败", e);
            throw new RuntimeException("获取 JWKS 失败", e);
        }
    }

    @Override
    public Map<String, Object> getDiscoveryDocument() {
        return getDiscoveryDocument(RequestContextUtil.getBaseUrl());
    }

    @Override
    public Map<String, Object> getDiscoveryDocument(String issuer) {
        // 使用动态获取的 issuer
        String actualIssuer = StrUtil.isNotBlank(issuer) ? issuer : "http://localhost";

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("issuer", actualIssuer);
        doc.put("authorization_endpoint", actualIssuer + "/oauth2/authorize");
        doc.put("token_endpoint", actualIssuer + "/oauth2/token");
        doc.put("userinfo_endpoint", actualIssuer + "/oauth2/userinfo");
        doc.put("jwks_uri", actualIssuer + "/oauth2/jwks");
        doc.put("response_types_supported", Arrays.asList("code"));
        doc.put("subject_types_supported", Arrays.asList("public"));
        doc.put("id_token_signing_alg_values_supported", Arrays.asList("RS256"));
        doc.put("scopes_supported", Arrays.asList("openid", "profile", "email"));
        doc.put("token_endpoint_auth_methods_supported", Arrays.asList("client_secret_post", "client_secret_basic"));
        doc.put("claims_supported", Arrays.asList("sub", "name", "preferred_username", "email", "policy"));
        doc.put("grant_types_supported", Arrays.asList("authorization_code"));

        return doc;
    }

    @Override
    public String determineMinioPolicy(LoginUser loginUser) {
        // 默认给所有用户 consoleAdmin 权限
        // 可以根据用户角色进行细分，例如：
        // - 管理员用户 -> consoleAdmin
        // - 普通用户 -> readwrite
        // - 只读用户 -> readonly

        // 目前简单实现：所有已登录用户都获得 consoleAdmin 权限
        return "consoleAdmin";
    }
}
