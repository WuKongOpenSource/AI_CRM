package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.CustomerAddBO;
import com.kakarote.ai_crm.entity.BO.CustomerAiParseBO;
import com.kakarote.ai_crm.entity.BO.CustomerAiSearchParseBO;
import com.kakarote.ai_crm.entity.BO.CustomerExportBO;
import com.kakarote.ai_crm.entity.BO.CustomerImportBO;
import com.kakarote.ai_crm.entity.BO.CustomerQueryBO;
import com.kakarote.ai_crm.entity.BO.CustomerTransferBO;
import com.kakarote.ai_crm.entity.BO.CustomerUpdateBO;
import com.kakarote.ai_crm.entity.VO.CustomerAiParseVO;
import com.kakarote.ai_crm.entity.VO.CustomerAiReportVO;
import com.kakarote.ai_crm.entity.VO.CustomerAiSearchParseVO;
import com.kakarote.ai_crm.entity.VO.CustomerDetailVO;
import com.kakarote.ai_crm.entity.VO.CustomerImportPreviewVO;
import com.kakarote.ai_crm.entity.VO.CustomerImportResultVO;
import com.kakarote.ai_crm.entity.VO.CustomerListVO;
import com.kakarote.ai_crm.entity.VO.DashboardStatsVO;
import com.kakarote.ai_crm.service.ICustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
    @RequirePermission("customer:create")
    public Result<Long> add(@Valid @RequestBody CustomerAddBO customerAddBO) {
        Long customerId = customerService.addCustomer(customerAddBO);
        return Result.ok(customerId);
    }

    @PostMapping("/update")
    @Operation(summary = "更新客户")
    @RequirePermission("customer:edit")
    public Result<String> update(@Valid @RequestBody CustomerUpdateBO customerUpdateBO) {
        customerService.updateCustomer(customerUpdateBO);
        return Result.ok();
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "删除客户")
    @RequirePermission("customer:delete")
    public Result<String> delete(@PathVariable("id") Long id) {
        customerService.deleteCustomer(id);
        return Result.ok();
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "分页查询客户")
    @RequirePermission("customer:view")
    public Result<BasePage<CustomerListVO>> queryPageList(@RequestBody CustomerQueryBO queryBO) {
        return Result.ok(customerService.queryPageList(queryBO));
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "获取客户详情")
    @RequirePermission("customer:view")
    public Result<CustomerDetailVO> detail(@PathVariable("id") Long id) {
        return Result.ok(customerService.getCustomerDetail(id));
    }

    @PostMapping("/updateStage")
    @Operation(summary = "更新商机阶段")
    @RequirePermission("customer:change_stage")
    public Result<String> updateStage(
            @Parameter(description = "客户ID") @RequestParam Long customerId,
            @Parameter(description = "阶段") @RequestParam String stage) {
        customerService.updateStage(customerId, stage);
        return Result.ok();
    }

    @PostMapping("/addTag")
    @Operation(summary = "添加客户标签")
    @RequirePermission("customer:edit")
    public Result<String> addTag(
            @Parameter(description = "客户ID") @RequestParam Long customerId,
            @Parameter(description = "标签名称") @RequestParam String tagName,
            @Parameter(description = "标签颜色") @RequestParam(required = false) String color) {
        customerService.addTag(customerId, tagName, color);
        return Result.ok();
    }

    @PostMapping("/removeTag")
    @Operation(summary = "删除客户标签")
    @RequirePermission("customer:edit")
    public Result<String> removeTag(
            @Parameter(description = "客户ID") @RequestParam Long customerId,
            @Parameter(description = "标签ID") @RequestParam Long tagId) {
        customerService.removeTag(customerId, tagId);
        return Result.ok();
    }

    @PostMapping("/transfer")
    @Operation(summary = "变更客户负责人")
    @RequirePermission("customer:transfer")
    public Result<String> transfer(@Valid @RequestBody CustomerTransferBO transferBO) {
        customerService.transferCustomer(transferBO);
        return Result.ok();
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取客户统计信息")
    @RequirePermission("customer:view")
    public Result<DashboardStatsVO> statistics() {
        return Result.ok(customerService.getStatistics());
    }

    @PostMapping("/export")
    @Operation(summary = "导出客户 Excel")
    @RequirePermission("customer:export")
    public void exportCustomers(@RequestBody CustomerExportBO exportBO, HttpServletResponse response) {
        customerService.exportCustomers(exportBO, response);
    }

    @GetMapping("/import/template")
    @Operation(summary = "下载导入模板")
    @RequirePermission("customer:import")
    public void downloadImportTemplate(HttpServletResponse response) {
        customerService.downloadImportTemplate(response);
    }

    @PostMapping("/import/preview")
    @Operation(summary = "导入预览")
    @RequirePermission("customer:import")
    public Result<CustomerImportPreviewVO> importPreview(@RequestParam("file") MultipartFile file) {
        return Result.ok(customerService.importPreview(file));
    }

    @PostMapping("/import/confirm")
    @Operation(summary = "确认导入")
    @RequirePermission("customer:import")
    public Result<CustomerImportResultVO> confirmImport(@RequestBody List<CustomerImportBO> rows) {
        return Result.ok(customerService.confirmImport(rows));
    }

    @PostMapping("/ai-parse")
    @Operation(summary = "AI 智能录入解析客户信息")
    @RequirePermission("customer:create")
    public Result<CustomerAiParseVO> aiParse(@Valid @RequestBody CustomerAiParseBO parseBO) {
        return Result.ok(customerService.aiParseCustomer(parseBO));
    }

    @PostMapping("/ai-search/parse")
    @Operation(summary = "AI 解析客户搜索场景")
    @RequirePermission("customer:view")
    public Result<CustomerAiSearchParseVO> aiSearchParse(@Valid @RequestBody CustomerAiSearchParseBO parseBO) {
        return Result.ok(customerService.aiParseSearch(parseBO));
    }
    @PostMapping("/{id}/ai-report")
    @Operation(summary = "生成客户 AI 分析报告")
    @RequirePermission("customer:edit")
    public Result<CustomerAiReportVO> generateAiReport(@PathVariable("id") Long id) {
        return Result.ok(customerService.generateAiReport(id));
    }
}
