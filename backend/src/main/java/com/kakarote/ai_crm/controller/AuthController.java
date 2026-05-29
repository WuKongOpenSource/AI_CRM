package com.kakarote.ai_crm.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.enums.LoginTypeEnum;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.OidcConfig;
import com.kakarote.ai_crm.config.security.service.TokenService;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.BO.LoginUserBO;
import com.kakarote.ai_crm.entity.BO.RegisterBO;
import com.kakarote.ai_crm.entity.BO.ResetPasswordBO;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.VO.LoginResponseVO;
import com.kakarote.ai_crm.entity.VO.ManageUserVO;
import com.kakarote.ai_crm.service.AuthSessionService;
import com.kakarote.ai_crm.service.ManageUserService;
import com.kakarote.ai_crm.service.OidcService;
import com.kakarote.ai_crm.service.RegistrationService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@Tag(name = "认证接口")
public class AuthController {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ManageUserService manageUserService;

    @Autowired
    private OidcService oidcService;

    @Autowired
    private OidcConfig oidcConfig;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 完成用户注册。
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<String> register(@Valid @RequestBody RegisterBO registerBO) {
        registrationService.register(registerBO);
        return Result.ok("注册成功");
    }

    /**
     * 重置密码。
     */
    @PostMapping("/reset-password")
    @Operation(summary = "重置密码")
    public Result<String> resetPassword(@Valid @RequestBody ResetPasswordBO resetPasswordBO) {
        registrationService.resetPassword(resetPasswordBO);
        return Result.ok("密码重置成功");
    }

    /**
     * 完成用户登录。
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginResponseVO> login(@Valid @RequestBody LoginUserBO loginUserBO,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        String username = StrUtil.trim(loginUserBO.getUsername());
        String password = StrUtil.trim(loginUserBO.getPassword());
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "用户名和密码不能为空");
        }

        List<ManagerUser> candidateUsers = manageUserService.queryUsersByUsername(username);
        if (CollUtil.isEmpty(candidateUsers)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_USER_DOES_NOT_EXIST);
        }

        List<ManagerUser> passwordMatchedUsers = candidateUsers.stream()
                .filter(user -> StrUtil.isNotBlank(user.getPassword()))
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .toList();
        if (CollUtil.isEmpty(passwordMatchedUsers)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_USERNAME_OR_PASSWORD_ERROR);
        }

        if (loginUserBO.getTenantId() != null) {
            passwordMatchedUsers = passwordMatchedUsers.stream()
                    .filter(user -> Objects.equals(user.getTenantId(), loginUserBO.getTenantId()))
                    .toList();
            if (CollUtil.isEmpty(passwordMatchedUsers)) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_USERNAME_OR_PASSWORD_ERROR);
            }
        }

        List<ManagerUser> enabledUsers = passwordMatchedUsers.stream()
                .filter(this::isEnabled)
                .toList();
        if (CollUtil.isEmpty(enabledUsers)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_USER_DISABLED);
        }

        if (loginUserBO.getTenantId() == null) {
            Set<Long> tenantIds = enabledUsers.stream()
                    .map(ManagerUser::getTenantId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (tenantIds.size() > 1) {
                LoginResponseVO responseVO = new LoginResponseVO();
                responseVO.setRequiresTenantSelection(Boolean.TRUE);
                responseVO.setTenantOptions(manageUserService.buildLoginTenantOptions(enabledUsers));
                return Result.ok(responseVO);
            }
        }

        if (enabledUsers.size() > 1) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "当前企业下存在重复账号，请联系管理员处理");
        }

        return Result.ok(authSessionService.createLoginResponse(
                enabledUsers.get(0),
                LoginTypeEnum.resolve(loginUserBO.getLoginType()),
                request,
                response
        ));
    }

    /**
     * 退出当前登录会话。
     */
    @PostMapping("/logout")
    @Operation(summary = "用户退出")
    public Result<String> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            LoginUser loginUser = UserUtil.getLoginUser();
            if (loginUser != null) {
                tokenService.delLoginUser(loginUser.getToken());
            }

            String sessionId = getSessionIdFromCookie(request);
            if (sessionId != null) {
                oidcService.removeSession(sessionId);
            }

            ResponseCookie clearCookie = ResponseCookie.from(oidcConfig.getSessionCookie(), "")
                    .httpOnly(true)
                    .path("/")
                    .maxAge(0)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
        } catch (Exception ignored) {
        }
        return Result.ok();
    }

    /**
     * 获取当前用户信息。
     */
    @GetMapping("/userInfo")
    @Operation(summary = "获取当前用户信息")
    public Result<ManageUserVO> getUserInfo() {
        return Result.ok(manageUserService.queryLoginUser());
    }

    /**
     * 获取 OIDC Session Token。
     */
    @GetMapping("/oidc-session")
    @Operation(summary = "获取 OIDC Session Token")
    public Result<Map<String, String>> getOidcSessionToken() {
        LoginUser loginUser = UserUtil.getLoginUser();
        if (loginUser == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NOT_LOGIN);
        }
        String sessionId = oidcService.createSession(loginUser);
        Map<String, String> result = new HashMap<>();
        result.put("sessionToken", sessionId);
        return Result.ok(result);
    }

    /**
     * 获取会话IDCookie。
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
     * 判断是否启用项。
     */
    private boolean isEnabled(ManagerUser user) {
        return user != null && Integer.valueOf(1).equals(user.getStatus());
    }
}
