package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.CustomerAddBO;
import com.kakarote.ai_crm.entity.BO.CustomerQueryBO;
import com.kakarote.ai_crm.entity.BO.CustomerUpdateBO;
import com.kakarote.ai_crm.entity.VO.CustomerDetailVO;
import com.kakarote.ai_crm.entity.VO.CustomerListVO;
import com.kakarote.ai_crm.entity.VO.DashboardStatsVO;
import com.kakarote.ai_crm.service.ICustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 客户管理控制器
 */
@RestController
@RequestMapping("/customer")
@Tag(name = "客户管理")
public class CustomerController {

    @Autowired
    private ICustomerService customerService;

    @PostMapping("/add")
    @Operation(summary = "创建客户")
    public Result<Long> add(@Valid @RequestBody CustomerAddBO customerAddBO) {
        Long customerId = customerService.addCustomer(customerAddBO);
        return Result.ok(customerId);
    }

    @PostMapping("/update")
    @Operation(summary = "更新客户")
    public Result<String> update(@Valid @RequestBody CustomerUpdateBO customerUpdateBO) {
        customerService.updateCustomer(customerUpdateBO);
        return Result.ok();
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "删除客户")
    public Result<String> delete(@PathVariable("id") Long id) {
        customerService.deleteCustomer(id);
        return Result.ok();
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "分页查询客户")
    public Result<BasePage<CustomerListVO>> queryPageList(@RequestBody CustomerQueryBO queryBO) {
        return Result.ok(customerService.queryPageList(queryBO));
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "获取客户详情")
    public Result<CustomerDetailVO> detail(@PathVariable("id") Long id) {
        return Result.ok(customerService.getCustomerDetail(id));
    }

    @PostMapping("/updateStage")
    @Operation(summary = "更新商机阶段")
    public Result<String> updateStage(
            @Parameter(description = "客户ID") @RequestParam Long customerId,
            @Parameter(description = "阶段") @RequestParam String stage) {
        customerService.updateStage(customerId, stage);
        return Result.ok();
    }

    @PostMapping("/addTag")
    @Operation(summary = "添加客户标签")
    public Result<String> addTag(
            @Parameter(description = "客户ID") @RequestParam Long customerId,
            @Parameter(description = "标签名称") @RequestParam String tagName,
            @Parameter(description = "标签颜色") @RequestParam(required = false) String color) {
        customerService.addTag(customerId, tagName, color);
        return Result.ok();
    }

    @PostMapping("/removeTag")
    @Operation(summary = "删除客户标签")
    public Result<String> removeTag(
            @Parameter(description = "客户ID") @RequestParam Long customerId,
            @Parameter(description = "标签ID") @RequestParam Long tagId) {
        customerService.removeTag(customerId, tagId);
        return Result.ok();
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取客户统计信息")
    public Result<DashboardStatsVO> statistics() {
        return Result.ok(customerService.getStatistics());
    }
}
