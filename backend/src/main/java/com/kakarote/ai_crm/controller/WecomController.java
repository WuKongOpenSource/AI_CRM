package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.WecomConfigSaveBO;
import com.kakarote.ai_crm.entity.BO.WecomConversationQueryBO;
import com.kakarote.ai_crm.entity.BO.WecomCustomerBindBO;
import com.kakarote.ai_crm.entity.BO.WecomCustomerQueryBO;
import com.kakarote.ai_crm.entity.BO.WecomCustomerUnbindBO;
import com.kakarote.ai_crm.entity.BO.WecomEmployeeSessionQueryBO;
import com.kakarote.ai_crm.entity.BO.WecomSyncRunBO;
import com.kakarote.ai_crm.entity.VO.WecomConfigVO;
import com.kakarote.ai_crm.entity.VO.WecomConversationVO;
import com.kakarote.ai_crm.entity.VO.WecomCustomerBindingVO;
import com.kakarote.ai_crm.entity.VO.WecomEmployeeSessionVO;
import com.kakarote.ai_crm.entity.VO.WecomExternalCustomerVO;
import com.kakarote.ai_crm.entity.VO.WecomMessageVO;
import com.kakarote.ai_crm.entity.VO.WecomSyncStatusVO;
import com.kakarote.ai_crm.service.impl.WecomCustomerBindingServiceImpl;
import com.kakarote.ai_crm.service.impl.WecomServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wecom")
@Tag(name = "WeCom SCRM")
public class WecomController {

    @Autowired
    private WecomServiceImpl wecomService;

    @Autowired
    private WecomCustomerBindingServiceImpl bindingService;

    @GetMapping("/config")
    @Operation(summary = "Get WeCom config")
    @RequirePermission("config:ai")
    public Result<WecomConfigVO> getConfig() {
        return Result.ok(wecomService.getConfig());
    }

    @PostMapping("/config")
    @Operation(summary = "Save WeCom config")
    @RequirePermission("config:ai")
    public Result<WecomConfigVO> saveConfig(@RequestBody WecomConfigSaveBO saveBO) {
        return Result.ok(wecomService.saveConfig(saveBO));
    }

    @PostMapping("/sync/run")
    @Operation(summary = "Run WeCom sync")
    @RequirePermission("config:ai")
    public Result<WecomSyncStatusVO> runSync(@RequestBody(required = false) WecomSyncRunBO runBO) {
        return Result.ok(wecomService.runSync(runBO));
    }

    @GetMapping("/sync/status")
    @Operation(summary = "Get WeCom sync status")
    @RequirePermission("config:ai")
    public Result<WecomSyncStatusVO> getSyncStatus() {
        return Result.ok(wecomService.getSyncStatus());
    }

    @PostMapping("/scrm/employees")
    @Operation(summary = "Query WeCom employee sessions")
    @RequirePermission("wecomEmployeeSession:view")
    public Result<BasePage<WecomEmployeeSessionVO>> queryEmployees(@RequestBody WecomEmployeeSessionQueryBO queryBO) {
        return Result.ok(wecomService.queryEmployeeSessions(queryBO));
    }

    @PostMapping("/scrm/conversations")
    @Operation(summary = "Query WeCom conversations")
    @RequirePermission("wecomCustomerSession:view")
    public Result<BasePage<WecomConversationVO>> queryConversations(@RequestBody WecomConversationQueryBO queryBO) {
        return Result.ok(wecomService.queryConversations(queryBO));
    }

    @GetMapping("/scrm/conversations/{id}/messages")
    @Operation(summary = "Query WeCom conversation messages")
    @RequirePermission("wecomCustomerSession:view")
    public Result<BasePage<WecomMessageVO>> queryMessages(@PathVariable("id") Long id,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "100") int limit) {
        return Result.ok(wecomService.queryConversationMessages(id, page, limit));
    }

    @PostMapping("/customers/queryPageList")
    @Operation(summary = "Query WeCom customers")
    @RequirePermission("wecomCustomer:view")
    public Result<BasePage<WecomExternalCustomerVO>> queryExternalCustomers(@RequestBody WecomCustomerQueryBO queryBO) {
        return Result.ok(wecomService.queryExternalCustomers(queryBO));
    }

    @GetMapping("/customers/{id}")
    @Operation(summary = "Get WeCom customer detail")
    @RequirePermission("wecomCustomer:detail")
    public Result<WecomExternalCustomerVO> getExternalCustomer(@PathVariable("id") Long id) {
        return Result.ok(wecomService.getExternalCustomer(id));
    }

    @PostMapping("/customers/bind")
    @Operation(summary = "Bind WeCom customer to CRM customer")
    @RequirePermission("wecomCustomer:bind")
    public Result<WecomCustomerBindingVO> bind(@RequestBody WecomCustomerBindBO bindBO) {
        bindingService.bindCustomer(bindBO);
        return Result.ok(wecomService.getCustomerBindings(bindBO.getCustomerId()).stream()
                .filter(item -> item.getExternalCustomerId().equals(bindBO.getExternalCustomerId()))
                .findFirst()
                .orElse(null));
    }

    @PostMapping("/customers/unbind")
    @Operation(summary = "Unbind WeCom customer")
    @RequirePermission("wecomCustomer:unbind")
    public Result<String> unbind(@RequestBody WecomCustomerUnbindBO unbindBO) {
        bindingService.unbindCustomer(unbindBO);
        return Result.ok();
    }
}
