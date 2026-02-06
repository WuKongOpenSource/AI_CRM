package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.OidcConfig;
import com.kakarote.ai_crm.config.security.service.TokenService;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.BO.LoginUserBO;
import com.kakarote.ai_crm.entity.VO.ManageUserVO;
import com.kakarote.ai_crm.service.ManageUserService;
import com.kakarote.ai_crm.service.OidcService;
import com.kakarote.ai_crm.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "认证接口")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ManageUserService manageUserService;

    @Autowired
    private OidcService oidcService;

    @Autowired
    private OidcConfig oidcConfig;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginUserBO loginUserBO,
                                              HttpServletResponse response) {
        // Authenticate
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginUserBO.getUsername(), loginUserBO.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_USERNAME_OR_PASSWORD_ERROR);
        } catch (InternalAuthenticationServiceException ex) {
            if (ex.getCause() instanceof BusinessException) {
                throw (BusinessException) ex.getCause();
            }
            throw new BusinessException(SystemCodeEnum.SYSTEM_USER_DOES_NOT_EXIST);
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String token = tokenService.createToken(loginUser);

        // 创建 OIDC Session 并设置 Cookie（用于 MinIO SSO）
        String sessionId = oidcService.createSession(loginUser);
        ResponseCookie sessionCookie = ResponseCookie.from(oidcConfig.getSessionCookie(), sessionId)
                .httpOnly(true)
                .path("/")
                .maxAge(oidcConfig.getTokenExpiry())
                .sameSite("Lax")       // HTTP 环境使用 Lax，HTTPS 环境可改为 None + Secure
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie.toString());

        // Build response
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);

        ManageUserVO userVO = new ManageUserVO();
        userVO.setUserId(loginUser.getUser().getUserId());
        userVO.setUsername(loginUser.getUsername());
        userVO.setRealname(loginUser.getUser().getRealname());
        userVO.setMobile(loginUser.getUser().getMobile());
        userVO.setEmail(loginUser.getUser().getEmail());
        result.put("userInfo", userVO);

        return Result.ok(result);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<String> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            LoginUser loginUser = UserUtil.getLoginUser();
            if (loginUser != null) {
                tokenService.delLoginUser(loginUser.getToken());
            }

            // 清除 OIDC Session Cookie
            String sessionId = getSessionIdFromCookie(request);
            if (sessionId != null) {
                oidcService.removeSession(sessionId);
            }
            // 清除 Cookie（使用 ResponseCookie 保持一致的属性）
            ResponseCookie clearCookie = ResponseCookie.from(oidcConfig.getSessionCookie(), "")
                    .httpOnly(true)
                    .path("/")
                    .maxAge(0)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
        } catch (Exception e) {
            // Ignore
        }
        return Result.ok();
    }

    @GetMapping("/userInfo")
    @Operation(summary = "获取当前用户信息")
    public Result<ManageUserVO> getUserInfo() {
        ManageUserVO userVO = manageUserService.queryLoginUser();
        return Result.ok(userVO);
    }

    @GetMapping("/oidc-session")
    @Operation(summary = "获取 OIDC Session Token（用于 MinIO SSO 跨域跳转）")
    public Result<Map<String, String>> getOidcSessionToken() {
        LoginUser loginUser = UserUtil.getLoginUser();
        if (loginUser == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NOT_LOGIN);
        }
        // 创建新的 OIDC Session
        String sessionId = oidcService.createSession(loginUser);
        Map<String, String> result = new HashMap<>();
        result.put("sessionToken", sessionId);
        return Result.ok(result);
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
}
