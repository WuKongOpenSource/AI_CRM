package com.kakarote.ai_crm.service;

import com.kakarote.ai_crm.entity.BO.LoginUser;

import java.util.Map;

/**
 * OIDC 服务接口
 * 用于 MinIO SSO 登录
 */
public interface OidcService {

    /**
     * 创建 OIDC Session
     * @param loginUser 登录用户
     * @return session ID
     */
    String createSession(LoginUser loginUser);

    /**
     * 根据 session ID 获取登录用户
     * @param sessionId session ID
     * @return 登录用户，不存在返回 null
     */
    LoginUser getLoginUserBySession(String sessionId);

    /**
     * 删除 session
     * @param sessionId session ID
     */
    void removeSession(String sessionId);

    /**
     * 生成授权码
     * @param userId 用户 ID
     * @param redirectUri 回调地址
     * @return 授权码
     */
    String generateAuthorizationCode(Long userId, String redirectUri);

    /**
     * 验证并消费授权码
     * @param code 授权码
     * @param redirectUri 回调地址
     * @return 用户 ID，验证失败返回 null
     */
    Long exchangeCode(String code, String redirectUri);

    /**
     * 生成 ID Token（JWT 格式）
     * @param loginUser 登录用户
     * @return JWT 字符串
     */
    String generateIdToken(LoginUser loginUser);

    /**
     * 生成 ID Token（JWT 格式）- 指定 issuer
     * @param loginUser 登录用户
     * @param issuer 发行者地址（动态获取）
     * @return JWT 字符串
     */
    String generateIdToken(LoginUser loginUser, String issuer);

    /**
     * 生成 Access Token
     * @param loginUser 登录用户
     * @return access token
     */
    String generateAccessToken(LoginUser loginUser);

    /**
     * 根据 access token 获取用户信息
     * @param accessToken access token
     * @return 用户信息 Map
     */
    Map<String, Object> getUserInfoByAccessToken(String accessToken);

    /**
     * 获取 JWKS（JSON Web Key Set）
     * @return JWKS JSON 字符串
     */
    String getJwks();

    /**
     * 获取 OIDC 发现文档
     * @return 发现文档 Map
     */
    Map<String, Object> getDiscoveryDocument();

    /**
     * 获取 OIDC 发现文档 - 指定 issuer
     * @param issuer 发行者地址（动态获取）
     * @return 发现文档 Map
     */
    Map<String, Object> getDiscoveryDocument(String issuer);

    /**
     * 根据用户角色确定 MinIO 策略
     * @param loginUser 登录用户
     * @return MinIO 策略名称
     */
    String determineMinioPolicy(LoginUser loginUser);
}
