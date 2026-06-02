package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.VO.WecomConversationTabVO;
import com.kakarote.ai_crm.entity.VO.WecomCustomerBindingVO;
import com.kakarote.ai_crm.entity.VO.WecomMessageVO;
import com.kakarote.ai_crm.service.impl.WecomServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/customer")
@Tag(name = "Customer WeCom")
public class CustomerWecomController {

    @Autowired
    private WecomServiceImpl wecomService;

    @GetMapping("/{id}/wecom-bindings")
    @Operation(summary = "Get customer WeCom bindings")
    @RequirePermission("customer:view")
    public Result<List<WecomCustomerBindingVO>> getBindings(@PathVariable("id") Long id) {
        return Result.ok(wecomService.getCustomerBindings(id));
    }

    @GetMapping("/{id}/wecom-conversation-tabs")
    @Operation(summary = "Get customer WeCom conversation tabs")
    @RequirePermission("customer:view")
    public Result<List<WecomConversationTabVO>> getConversationTabs(@PathVariable("id") Long id) {
        return Result.ok(wecomService.getCustomerConversationTabs(id));
    }

    @GetMapping("/{id}/wecom-conversations/{conversationId}/messages")
    @Operation(summary = "Get customer WeCom conversation messages")
    @RequirePermission("customer:view")
    public Result<BasePage<WecomMessageVO>> getConversationMessages(@PathVariable("id") Long id,
                                                                    @PathVariable("conversationId") Long conversationId,
                                                                    @RequestParam(defaultValue = "1") int page,
                                                                    @RequestParam(defaultValue = "100") int limit) {
        return Result.ok(wecomService.queryCustomerConversationMessages(id, conversationId, page, limit));
    }
}
