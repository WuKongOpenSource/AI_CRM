package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.ProductCategoryAddBO;
import com.kakarote.ai_crm.entity.BO.ProductCategoryMoveBO;
import com.kakarote.ai_crm.entity.BO.ProductCategoryUpdateBO;
import com.kakarote.ai_crm.entity.VO.ProductCategoryVO;
import com.kakarote.ai_crm.service.IProductCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/product/category")
@Tag(name = "产品类目")
public class ProductCategoryController {

    @Autowired
    private IProductCategoryService productCategoryService;

    @GetMapping("/tree")
    @Operation(summary = "产品类目树")
    @RequirePermission("product:view")
    public Result<List<ProductCategoryVO>> tree() {
        return Result.ok(productCategoryService.tree());
    }

    @PostMapping("/add")
    @Operation(summary = "新增产品类目")
    @RequirePermission("product:category_manage")
    public Result<Long> addCategory(@Valid @RequestBody ProductCategoryAddBO bo) {
        return Result.ok(productCategoryService.addCategory(bo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新产品类目")
    @RequirePermission("product:category_manage")
    public Result<Void> updateCategory(@Valid @RequestBody ProductCategoryUpdateBO bo) {
        productCategoryService.updateCategory(bo);
        return Result.ok();
    }

    @PostMapping("/move")
    @Operation(summary = "移动产品类目")
    @RequirePermission("product:category_manage")
    public Result<Void> moveCategory(@Valid @RequestBody ProductCategoryMoveBO bo) {
        productCategoryService.moveCategory(bo);
        return Result.ok();
    }

    @PostMapping("/delete/{categoryId}")
    @Operation(summary = "删除产品类目")
    @RequirePermission("product:category_manage")
    public Result<Void> deleteCategory(@PathVariable Long categoryId) {
        productCategoryService.deleteCategory(categoryId);
        return Result.ok();
    }
}
