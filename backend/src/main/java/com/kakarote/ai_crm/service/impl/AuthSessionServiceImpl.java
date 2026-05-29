package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.common.enums.LoginTypeEnum;
import com.kakarote.ai_crm.config.OidcConfig;
import com.kakarote.ai_crm.config.security.service.TokenService;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.VO.LoginResponseVO;
import com.kakarote.ai_crm.entity.VO.ManageUserVO;
import com.kakarote.ai_crm.service.AuthSessionService;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.OidcService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class AuthSessionServiceImpl implements AuthSessionService {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private OidcService oidcService;

    @Autowired
    private OidcConfig oidcConfig;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public LoginResponseVO createLoginResponse(ManagerUser user,
                                               LoginTypeEnum loginType,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUser(user);
        loginUser.setLoginType(LoginTypeEnum.resolve(loginType));

        String token = tokenService.createToken(loginUser, tokenService.resolveLoginIp(request));
        String sessionId = oidcService.createSession(loginUser);
        ResponseCookie sessionCookie = ResponseCookie.from(oidcConfig.getSessionCookie(), sessionId)
                .httpOnly(true)
                .path("/")
                .maxAge(oidcConfig.getTokenExpiry())
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie.toString());

        LoginResponseVO result = new LoginResponseVO();
        result.setToken(token);
        result.setUserInfo(buildUserInfo(user));
        result.setRequiresTenantSelection(Boolean.FALSE);
        return result;
    }

    private ManageUserVO buildUserInfo(ManagerUser user) {
        ManageUserVO userVO = new ManageUserVO();
        userVO.setUserId(user.getUserId());
        userVO.setUsername(user.getUsername());
        userVO.setRealname(user.getRealname());
        userVO.setImg(user.getImg());
        userVO.setTenantId(user.getTenantId());
        if (StrUtil.isNotBlank(user.getImg())) {
            try {
                userVO.setImgUrl(fileStorageService.getUrl(user.getImg()));
            } catch (Exception ignored) {
            }
        }
        userVO.setMobile(user.getMobile());
        userVO.setEmail(user.getEmail());
        return userVO;
    }
}
