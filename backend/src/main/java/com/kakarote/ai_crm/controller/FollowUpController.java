package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.FollowUpAddBO;
import com.kakarote.ai_crm.entity.BO.FollowUpQueryBO;
import com.kakarote.ai_crm.entity.VO.FollowUpVO;
import com.kakarote.ai_crm.service.IFollowUpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 跟进记录控制器
 */
@RestController
@RequestMapping("/followup")
@Tag(name = "跟进记录")
public class FollowUpController {

    @Autowired
    private IFollowUpService followUpService;

    @PostMapping("/add")
    @Operation(summary = "添加跟进记录")
    public Result<Long> add(@Valid @RequestBody FollowUpAddBO followUpAddBO) {
        Long followUpId = followUpService.addFollowUp(followUpAddBO);
        return Result.ok(followUpId);
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "分页查询跟进记录")
    public Result<BasePage<FollowUpVO>> queryPageList(@RequestBody FollowUpQueryBO queryBO) {
        return Result.ok(followUpService.queryPageList(queryBO));
    }

    @PostMapping("/queryByCustomer")
    @Operation(summary = "按客户查询跟进记录")
    public Result<List<FollowUpVO>> queryByCustomer(
            @Parameter(description = "客户ID") @RequestParam Long customerId) {
        return Result.ok(followUpService.queryByCustomer(customerId));
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "删除跟进记录")
    public Result<String> delete(@PathVariable("id") Long id) {
        followUpService.deleteFollowUp(id);
        return Result.ok();
    }
}
