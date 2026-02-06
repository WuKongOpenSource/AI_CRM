package com.kakarote.ai_crm.config.security.handle;

import cn.hutool.extra.servlet.JakartaServletUtil;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 认证失败处理类 返回未授权
 * @author zhangzhiwei
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint, Serializable {
    private static final long serialVersionUID = -8970718410437077606L;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) {
        JakartaServletUtil.write(response, Result.error(SystemCodeEnum.SYSTEM_NOT_LOGIN).toJSONString(),"application/json;charset=UTF-8");
    }
}
