package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.config.security.service.TokenService;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 默认响应接口
 *
 * @author zhangzhiwei
 */
@RestController
@Tag(name = "登录控制器")
public class IndexController {

    @Autowired
    private TokenService tokenService;

    @GetMapping(path = {"/", "/index"})
    public void index(HttpServletResponse response) throws Exception {
        response.sendRedirect("/index.html");
    }

    /**
     * 调用spring-security退出方法
     */
    @PostMapping("/logout")
    @Operation(summary = "退出")
    public Result<String> logout(HttpServletRequest request) {
        LoginUser loginUser = tokenService.getLoginUser(request);
        tokenService.delLoginUser(loginUser.getToken());
        return Result.ok();
    }

}
