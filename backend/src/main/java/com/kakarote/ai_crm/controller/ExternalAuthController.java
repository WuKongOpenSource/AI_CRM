package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.ExternalAuthRegisterBO;
import com.kakarote.ai_crm.entity.BO.ExternalAuthTicketLoginBO;
import com.kakarote.ai_crm.entity.VO.ExternalAuthAuthorizeVO;
import com.kakarote.ai_crm.entity.VO.ExternalAuthBindingVO;
import com.kakarote.ai_crm.entity.VO.ExternalAuthProviderVO;
import com.kakarote.ai_crm.entity.VO.LoginResponseVO;
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

@RestController
@RequestMapping("/auth/external")
@Tag(name = "External auth")
public class ExternalAuthController {

    @Autowired
    private ExternalAuthService externalAuthService;

    @GetMapping("/providers")
    @Operation(summary = "List external auth providers")
    public Result<List<ExternalAuthProviderVO>> providers() {
        return Result.ok(externalAuthService.listProviders());
    }

    @GetMapping("/{provider}/authorize")
    @Operation(summary = "Create external auth authorize URL")
    public Result<ExternalAuthAuthorizeVO> authorize(@PathVariable String provider,
                                                     @RequestParam(required = false) String redirect,
                                                     HttpServletRequest request) {
        return Result.ok(externalAuthService.createAuthorizeUrl(provider, redirect, request));
    }

    @GetMapping("/{provider}/callback")
    @Operation(summary = "External auth callback")
    public void callback(@PathVariable String provider,
                         @RequestParam(required = false) String code,
                         @RequestParam(required = false) String state,
                         @RequestParam(required = false) String error,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        response.sendRedirect(externalAuthService.handleCallback(provider, code, state, error, request));
    }

    @PostMapping("/login-ticket")
    @Operation(summary = "Exchange external auth login ticket")
    public Result<LoginResponseVO> loginTicket(@Valid @RequestBody ExternalAuthTicketLoginBO loginBO,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {
        return Result.ok(externalAuthService.loginByTicket(loginBO, request, response));
    }

    @PostMapping("/register")
    @Operation(summary = "Complete external auth registration")
    public Result<LoginResponseVO> register(@Valid @RequestBody ExternalAuthRegisterBO registerBO,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        return Result.ok(externalAuthService.registerByTicket(registerBO, request, response));
    }

    @GetMapping("/bindings")
    @Operation(summary = "List current user external auth bindings")
    public Result<List<ExternalAuthBindingVO>> bindings() {
        return Result.ok(externalAuthService.listBindings());
    }

    @GetMapping("/{provider}/bind/authorize")
    @Operation(summary = "Create external auth bind authorize URL")
    public Result<ExternalAuthAuthorizeVO> bindAuthorize(@PathVariable String provider,
                                                        @RequestParam(required = false) String redirect,
                                                        HttpServletRequest request) {
        return Result.ok(externalAuthService.createBindAuthorizeUrl(provider, redirect, request));
    }

    @DeleteMapping("/{provider}/binding")
    @Operation(summary = "Unbind current user external auth identity")
    public Result<String> unbind(@PathVariable String provider) {
        externalAuthService.unbind(provider);
        return Result.ok();
    }
}
