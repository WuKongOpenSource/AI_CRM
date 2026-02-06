package com.kakarote.ai_crm.controller;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.config.OidcConfig;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.service.ManageUserService;
import com.kakarote.ai_crm.service.OidcService;
import com.kakarote.ai_crm.utils.RequestContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OIDC Provider 控制器
 * 提供 MinIO SSO 登录所需的 OIDC 端点
 */
@Slf4j
@RestController
@Tag(name = "OIDC Provider")
public class OidcController {

    @Autowired
    private OidcService oidcService;

    @Autowired
    private OidcConfig oidcConfig;

    @Autowired
    private ManageUserService manageUserService;

    /**
     * OIDC 发现文档
     */
    @GetMapping(value = "/.well-known/openid-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "OIDC 发现文档")
    public ResponseEntity<Map<String, Object>> discovery() {
        return ResponseEntity.ok(oidcService.getDiscoveryDocument());
    }

    /**
     * 授权端点
     * 浏览器重定向到此端点，如果用户已登录则生成授权码并重定向回客户端
     * 支持两种方式获取 session：
     * 1. 从 Cookie 中读取（同域场景）
     * 2. 从 URL 参数 session_token 读取（跨域场景）
     */
    @GetMapping("/oauth2/authorize")
    @Operation(summary = "OIDC 授权端点")
    public void authorize(
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("response_type") String responseType,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "session_token", required = false) String sessionToken,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // 优先从 URL 参数获取 session_token（跨域场景），否则从 Cookie 获取
        String sessionId = StrUtil.isNotEmpty(sessionToken) ? sessionToken : getSessionIdFromCookie(request);

        // 验证 client_id
        if (!oidcConfig.getClientId().equals(clientId)) {
            log.warn("OIDC 授权失败: 无效的 client_id={}", clientId);
            response.sendError(HttpStatus.BAD_REQUEST.value(), "invalid_client");
            return;
        }

        // 验证 response_type
        if (!"code".equals(responseType)) {
            log.warn("OIDC 授权失败: 不支持的 response_type={}", responseType);
            response.sendError(HttpStatus.BAD_REQUEST.value(), "unsupported_response_type");
            return;
        }

        // 验证 redirect_uri（动态获取期望的 redirect_uri）
        String expectedRedirectUri = RequestContextUtil.getMinioOAuthCallbackUrl(oidcConfig.getMinioConsolePort());
        if (!expectedRedirectUri.equals(redirectUri)) {
            log.warn("OIDC 授权失败: 无效的 redirect_uri={}, expected={}", redirectUri, expectedRedirectUri);
            response.sendError(HttpStatus.BAD_REQUEST.value(), "invalid_redirect_uri");
            return;
        }

        LoginUser loginUser = oidcService.getLoginUserBySession(sessionId);

        if (loginUser == null) {
            // 用户未登录，重定向到前端登录页面（动态获取）
            String currentUrl = request.getRequestURL().toString();
            String queryString = request.getQueryString();
            if (StrUtil.isNotEmpty(queryString)) {
                currentUrl += "?" + queryString;
            }
            String frontendLoginUrl = RequestContextUtil.getFrontendLoginUrl();
            String loginUrl = frontendLoginUrl + "?redirect=" + URLEncoder.encode(currentUrl, StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
            return;
        }

        // 生成授权码
        Long userId = loginUser.getUser().getUserId();
        String code = oidcService.generateAuthorizationCode(userId, redirectUri);

        // 构建重定向 URL
        StringBuilder redirectUrl = new StringBuilder(redirectUri);
        redirectUrl.append("?code=").append(code);
        if (StrUtil.isNotEmpty(state)) {
            redirectUrl.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        }

        log.debug("OIDC 授权成功: userId={}", userId);
        response.sendRedirect(redirectUrl.toString());
    }

    /**
     * Token 端点
     * 客户端用授权码换取 access_token 和 id_token
     * 支持两种客户端认证方式：
     * 1. client_secret_post: client_id 和 client_secret 作为 POST 参数
     * 2. client_secret_basic: client_id 和 client_secret 通过 HTTP Basic Auth 传递
     */
    @PostMapping(value = "/oauth2/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "OIDC Token 端点")
    public ResponseEntity<?> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam("code") String code,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        // 验证 grant_type
        if (!"authorization_code".equals(grantType)) {
            log.warn("OIDC Token 失败: 不支持的 grant_type={}", grantType);
            return errorResponse("unsupported_grant_type", "Only authorization_code is supported");
        }

        // 支持 HTTP Basic Auth 方式传递 client credentials
        String resolvedClientId = clientId;
        String resolvedClientSecret = clientSecret;

        if (StrUtil.isNotEmpty(authorization) && authorization.startsWith("Basic ")) {
            try {
                String base64Credentials = authorization.substring(6);
                String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
                String[] parts = credentials.split(":", 2);
                if (parts.length == 2) {
                    resolvedClientId = parts[0];
                    resolvedClientSecret = parts[1];
                }
            } catch (Exception e) {
                log.warn("OIDC Token: 解析 Basic Auth 失败");
            }
        }

        // 验证 client_id
        if (!oidcConfig.getClientId().equals(resolvedClientId)) {
            log.warn("OIDC Token 失败: 无效的 client_id");
            return errorResponse("invalid_client", "Invalid client_id");
        }

        // 验证 client_secret
        if (!oidcConfig.getClientSecret().equals(resolvedClientSecret)) {
            log.warn("OIDC Token 失败: 无效的 client_secret");
            return errorResponse("invalid_client", "Invalid client_secret");
        }

        // 验证并消费授权码
        Long userId = oidcService.exchangeCode(code, redirectUri);
        if (userId == null) {
            log.warn("OIDC Token 失败: 无效或已过期的授权码");
            return errorResponse("invalid_grant", "Invalid or expired authorization code");
        }

        // 获取用户信息
        ManagerUser user = manageUserService.getById(userId);
        if (user == null) {
            log.warn("OIDC Token 失败: 用户不存在 userId={}", userId);
            return errorResponse("invalid_grant", "User not found");
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUser(user);

        // 生成 tokens（使用动态 issuer）
        String baseUrl = RequestContextUtil.getBaseUrl();
        String accessToken = oidcService.generateAccessToken(loginUser);
        String idToken = oidcService.generateIdToken(loginUser, baseUrl);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("access_token", accessToken);
        result.put("token_type", "Bearer");
        result.put("expires_in", oidcConfig.getTokenExpiry());
        result.put("id_token", idToken);

        log.debug("OIDC Token 生成成功: userId={}", userId);
        return ResponseEntity.ok(result);
    }

    /**
     * UserInfo 端点
     * 返回当前用户的信息
     */
    @GetMapping(value = "/oauth2/userinfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "OIDC UserInfo 端点")
    public ResponseEntity<?> userinfo(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (StrUtil.isEmpty(authorization)) {
            return errorResponse("invalid_token", "Missing Authorization header");
        }

        Map<String, Object> userInfo = oidcService.getUserInfoByAccessToken(authorization);
        if (userInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_token", "error_description", "Invalid or expired access token"));
        }

        return ResponseEntity.ok(userInfo);
    }

    /**
     * JWKS 端点
     * 返回用于验证 ID Token 签名的公钥
     */
    @GetMapping(value = "/oauth2/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "OIDC JWKS 端点")
    public ResponseEntity<String> jwks() {
        return ResponseEntity.ok(oidcService.getJwks());
    }

    /**
     * MinIO SSO 跳转端点
     * 从 URL 参数读取 session_token，设置 Cookie，重定向到 MinIO Console 的 OIDC 登录入口
     */
    @GetMapping("/oauth2/minio-sso")
    @Operation(summary = "MinIO SSO 跳转")
    public void minioSso(
            @RequestParam(value = "session_token", required = false) String sessionToken,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        LoginUser loginUser = null;

        // 优先从 URL 参数获取 session_token
        if (StrUtil.isNotEmpty(sessionToken)) {
            loginUser = oidcService.getLoginUserBySession(sessionToken);
        }

        // 如果没有 session_token，尝试从 Cookie 获取
        if (loginUser == null) {
            String existingSessionId = getSessionIdFromCookie(request);
            if (existingSessionId != null) {
                loginUser = oidcService.getLoginUserBySession(existingSessionId);
            }
        }

        if (loginUser == null) {
            // 用户未登录，重定向到前端登录页面（动态获取）
            String minioConsoleUrl = RequestContextUtil.getMinioConsoleUrl(oidcConfig.getMinioConsolePort());
            String minioLoginUrl = minioConsoleUrl + "/login/openid";
            String frontendLoginUrl = RequestContextUtil.getFrontendLoginUrl();
            String loginUrl = frontendLoginUrl + "?redirect=" + URLEncoder.encode(minioLoginUrl, StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
            return;
        }

        // 创建新的 OIDC Session 用于 Cookie
        String newSessionId = oidcService.createSession(loginUser);

        // 设置 Cookie
        Cookie sessionCookie = new Cookie(oidcConfig.getSessionCookie(), newSessionId);
        sessionCookie.setHttpOnly(true);
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(oidcConfig.getTokenExpiry());
        response.addCookie(sessionCookie);

        // 重定向到 MinIO Console 的 OIDC 登录入口（动态获取）
        String minioConsoleUrl = RequestContextUtil.getMinioConsoleUrl(oidcConfig.getMinioConsolePort());
        String minioLoginUrl = minioConsoleUrl + "/login/openid";
        response.sendRedirect(minioLoginUrl);
    }

    /**
     * 从 Cookie 中获取 session ID
     */
    private String getSessionIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (oidcConfig.getSessionCookie().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 生成错误响应
     */
    private ResponseEntity<Map<String, String>> errorResponse(String error, String description) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("error_description", description);
        return ResponseEntity.badRequest().body(body);
    }
}
