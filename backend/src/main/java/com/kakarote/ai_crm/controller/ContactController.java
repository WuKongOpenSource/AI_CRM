package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.ContactAddBO;
import com.kakarote.ai_crm.entity.BO.ContactQueryBO;
import com.kakarote.ai_crm.entity.BO.ContactUpdateBO;
import com.kakarote.ai_crm.entity.BO.CustomerAiParseBO;
import com.kakarote.ai_crm.entity.VO.ContactVO;
import com.kakarote.ai_crm.entity.VO.CustomerAiParseVO;
import com.kakarote.ai_crm.service.IContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 联系人管理控制器
 */
@RestController
@RequestMapping("/contact")
@Tag(name = "联系人管理")
public class ContactController {

    @Autowired
    private IContactService contactService;

    /**
     * 创建联系人。
     */
    @PostMapping("/add")
    @Operation(summary = "创建联系人")
    @RequirePermission("contact:create")
    public Result<Long> add(@Valid @RequestBody ContactAddBO contactAddBO) {
        Long contactId = contactService.addContact(contactAddBO);
        return Result.ok(contactId);
    }

    /**
     * 分页查询联系人。
     */
    @PostMapping("/queryPageList")
    @Operation(summary = "分页查询联系人")
    @RequirePermission("contact:view")
    public Result<BasePage<ContactVO>> queryPageList(@RequestBody ContactQueryBO queryBO) {
        return Result.ok(contactService.queryPageList(queryBO));
    }

    /**
     * 更新联系人。
     */
    @PostMapping("/update")
    @Operation(summary = "更新联系人")
    @RequirePermission("contact:edit")
    public Result<String> update(@Valid @RequestBody ContactUpdateBO contactUpdateBO) {
        contactService.updateContact(contactUpdateBO);
        return Result.ok();
    }

    /**
     * 删除联系人。
     */
    @PostMapping("/delete/{id}")
    @Operation(summary = "删除联系人")
    @RequirePermission("contact:delete")
    public Result<String> delete(@PathVariable("id") Long id) {
        contactService.deleteContact(id);
        return Result.ok();
    }

    /**
     * 按客户查询联系人。
     */
    @PostMapping("/queryByCustomer")
    @Operation(summary = "按客户查询联系人")
    @RequirePermission("contact:view")
    public Result<List<ContactVO>> queryByCustomer(
            @Parameter(description = "客户ID") @RequestParam Long customerId) {
        return Result.ok(contactService.queryByCustomer(customerId));
    }

    /**
     * 设置为主联系人。
     */
    @PostMapping("/setPrimary/{id}")
    @Operation(summary = "设置为主联系人")
    @RequirePermission("contact:set_primary")
    public Result<String> setPrimary(@PathVariable("id") Long id) {
        contactService.setPrimary(id);
        return Result.ok();
    }

    /**
     * AI 智能录入解析联系人信息。
     */
    @PostMapping("/ai-parse")
    @Operation(summary = "AI 鏅鸿兘褰曞叆瑙ｆ瀽鑱旂郴浜轰俊鎭?")
    @RequirePermission("contact:create")
    public Result<CustomerAiParseVO> aiParse(@Valid @RequestBody CustomerAiParseBO parseBO) {
        return Result.ok(contactService.aiParseContact(parseBO));
    }
}
