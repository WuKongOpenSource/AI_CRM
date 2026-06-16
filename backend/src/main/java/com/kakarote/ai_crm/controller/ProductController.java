package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.ProductAddBO;
import com.kakarote.ai_crm.entity.BO.ProductImportBO;
import com.kakarote.ai_crm.entity.BO.ProductQueryBO;
import com.kakarote.ai_crm.entity.BO.ProductSettingsUpdateBO;
import com.kakarote.ai_crm.entity.BO.ProductStatusUpdateBO;
import com.kakarote.ai_crm.entity.BO.ProductTransferBO;
import com.kakarote.ai_crm.entity.BO.ProductUpdateBO;
import com.kakarote.ai_crm.entity.VO.ProductImportPreviewVO;
import com.kakarote.ai_crm.entity.VO.ProductImportResultVO;
import com.kakarote.ai_crm.entity.VO.ProductSettingsVO;
import com.kakarote.ai_crm.entity.VO.ProductVO;
import com.kakarote.ai_crm.service.IProductService;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/product")
@Tag(name = "产品管理")
public class ProductController {

    @Autowired
    private IProductService productService;

    @PostMapping("/add")
    @Operation(summary = "新增产品")
    @RequirePermission("product:create")
    public Result<Long> addProduct(@Valid @RequestBody ProductAddBO bo) {
        return Result.ok(productService.addProduct(bo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新产品")
    @RequirePermission("product:edit")
    public Result<Void> updateProduct(@Valid @RequestBody ProductUpdateBO bo) {
        productService.updateProduct(bo);
        return Result.ok();
    }

    @PostMapping("/delete/{productId}")
    @Operation(summary = "删除产品")
    @RequirePermission("product:delete")
    public Result<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return Result.ok();
    }

    @PostMapping("/status")
    @Operation(summary = "启用或停用产品")
    @RequirePermission("product:update_status")
    public Result<Void> updateStatus(@Valid @RequestBody ProductStatusUpdateBO bo) {
        productService.updateStatus(bo);
        return Result.ok();
    }

    @PostMapping("/transfer")
    @Operation(summary = "转移产品负责人")
    @RequirePermission("product:transfer")
    public Result<Void> transferProducts(@Valid @RequestBody ProductTransferBO bo) {
        productService.transferProducts(bo);
        return Result.ok();
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "分页查询产品")
    @RequirePermission("product:view")
    public Result<BasePage<ProductVO>> queryPageList(@RequestBody ProductQueryBO queryBO) {
        return Result.ok(productService.queryPageList(queryBO));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "产品详情")
    @RequirePermission("product:view")
    public Result<ProductVO> detail(@PathVariable Long productId) {
        return Result.ok(productService.getProductDetail(productId));
    }

    @GetMapping("/settings")
    @Operation(summary = "产品设置")
    @RequirePermission("product:settings")
    public Result<ProductSettingsVO> getSettings() {
        return Result.ok(productService.getSettings());
    }

    @PostMapping("/settings")
    @Operation(summary = "更新产品设置")
    @RequirePermission("product:settings")
    public Result<Void> updateSettings(@RequestBody ProductSettingsUpdateBO bo) {
        productService.updateSettings(bo);
        return Result.ok();
    }

    @PostMapping("/export")
    @Operation(summary = "导出产品")
    @RequirePermission("product:export")
    public void exportProducts(@RequestBody ProductQueryBO queryBO, HttpServletResponse response) {
        productService.exportProducts(queryBO, response);
    }

    @GetMapping("/import/template")
    @Operation(summary = "下载产品导入模板")
    @RequirePermission("product:import")
    public void downloadImportTemplate(HttpServletResponse response) {
        productService.downloadImportTemplate(response);
    }

    @PostMapping("/import/preview")
    @Operation(summary = "产品导入预览")
    @RequirePermission("product:import")
    public Result<ProductImportPreviewVO> importPreview(@RequestParam("file") MultipartFile file) {
        return Result.ok(productService.importPreview(file));
    }

    @PostMapping("/import/confirm")
    @Operation(summary = "确认导入产品")
    @RequirePermission("product:import")
    public Result<ProductImportResultVO> confirmImport(@RequestBody List<ProductImportBO> rows) {
        return Result.ok(productService.confirmImport(rows));
    }
}
