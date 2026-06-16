package com.kakarote.ai_crm.config.security.handle;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.kakarote.ai_crm.common.Const;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 认证失败处理类，返回未登录
 *
 * @author zhangzhiwei
 */
@Component
@Slf4j
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint, Serializable {
    private static final long serialVersionUID = -8970718410437077606L;

    /**
     * 处理commence方法逻辑。
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) {
        log.error("Current request authentication failed, uri={}", request.getRequestURI(), e);
        String kickoutMessage = (String) request.getAttribute(Const.LOGIN_KICKOUT_MESSAGE_ATTR);
        Result<String> result = StrUtil.isNotBlank(kickoutMessage)
                ? Result.error(SystemCodeEnum.SYSTEM_NOT_LOGIN, kickoutMessage)
                : Result.error(SystemCodeEnum.SYSTEM_NOT_LOGIN);
        JakartaServletUtil.write(response, result.toJSONString(), "application/json;charset=UTF-8");
    }
}
