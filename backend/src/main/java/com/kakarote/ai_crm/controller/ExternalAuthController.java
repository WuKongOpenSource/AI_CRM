package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.ExternalAuthTicketLoginBO;
import com.kakarote.ai_crm.entity.VO.ExternalAuthAuthorizeVO;
import com.kakarote.ai_crm.entity.VO.ExternalAuthBindingVO;
import com.kakarote.ai_crm.entity.VO.ExternalAuthProviderVO;
import com.kakarote.ai_crm.service.ExternalAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/external")
@Tag(name = "外部登录")
public class ExternalAuthController {

    @Autowired
    private ExternalAuthService externalAuthService;

    @GetMapping("/providers")
    @Operation(summary = "获取外部登录服务商")
    public Result<List<ExternalAuthProviderVO>> providers() {
        return Result.ok(externalAuthService.listProviders());
    }

    @GetMapping("/{provider}/authorize")
    @Operation(summary = "创建外部登录授权地址")
    public Result<ExternalAuthAuthorizeVO> authorize(@PathVariable String provider,
                                                     @RequestParam(required = false) String redirect,
                                                     HttpServletRequest request) {
        return Result.ok(externalAuthService.createAuthorizeUrl(provider, redirect, request));
    }

    @GetMapping("/{provider}/callback")
    @Operation(summary = "外部登录回调")
    public void callback(@PathVariable String provider,
                         @RequestParam(required = false) String code,
                         @RequestParam(required = false) String state,
                         @RequestParam(required = false) String error,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        response.sendRedirect(externalAuthService.handleCallback(provider, code, state, error, request));
    }

    @PostMapping("/login-ticket")
    @Operation(summary = "兑换外部登录票据")
    public Result<Map<String, Object>> loginTicket(@Valid @RequestBody ExternalAuthTicketLoginBO loginBO,
                                                   HttpServletResponse response) {
        return Result.ok(externalAuthService.loginByTicket(loginBO, response));
    }

    @GetMapping("/bindings")
    @Operation(summary = "获取当前用户外部登录绑定")
    public Result<List<ExternalAuthBindingVO>> bindings() {
        return Result.ok(externalAuthService.listBindings());
    }

    @GetMapping("/{provider}/bind/authorize")
    @Operation(summary = "创建外部登录绑定授权地址")
    public Result<ExternalAuthAuthorizeVO> bindAuthorize(@PathVariable String provider,
                                                        @RequestParam(required = false) String redirect,
                                                        HttpServletRequest request) {
        return Result.ok(externalAuthService.createBindAuthorizeUrl(provider, redirect, request));
    }

    @DeleteMapping("/{provider}/binding")
    @Operation(summary = "解绑当前用户外部登录身份")
    public Result<String> unbind(@PathVariable String provider) {
        externalAuthService.unbind(provider);
        return Result.ok();
    }
}
