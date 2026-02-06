package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.CustomFieldAddBO;
import com.kakarote.ai_crm.entity.BO.CustomFieldUpdateBO;
import com.kakarote.ai_crm.entity.BO.FieldSortBO;
import com.kakarote.ai_crm.entity.VO.CustomFieldVO;
import com.kakarote.ai_crm.service.ICustomFieldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自定义字段管理控制器
 */
@RestController
@RequestMapping("/custom-field")
@Tag(name = "自定义字段管理")
public class CustomFieldController {

    @Autowired
    private ICustomFieldService customFieldService;

    @PostMapping("/add")
    @Operation(summary = "添加自定义字段")
    public Result<Long> add(@Valid @RequestBody CustomFieldAddBO bo) {
        return Result.ok(customFieldService.addField(bo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新自定义字段")
    public Result<String> update(@Valid @RequestBody CustomFieldUpdateBO bo) {
        customFieldService.updateField(bo);
        return Result.ok();
    }

    @PostMapping("/disable/{id}")
    @Operation(summary = "禁用自定义字段")
    public Result<String> disable(
            @Parameter(description = "字段ID") @PathVariable("id") Long id) {
        customFieldService.disableField(id);
        return Result.ok();
    }

    @PostMapping("/enable/{id}")
    @Operation(summary = "启用自定义字段")
    public Result<String> enable(
            @Parameter(description = "字段ID") @PathVariable("id") Long id) {
        customFieldService.enableField(id);
        return Result.ok();
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "删除自定义字段")
    public Result<String> delete(
            @Parameter(description = "字段ID") @PathVariable("id") Long id) {
        customFieldService.deleteField(id);
        return Result.ok();
    }

    @GetMapping("/list/{entityType}")
    @Operation(summary = "查询实体的所有字段")
    public Result<List<CustomFieldVO>> list(
            @Parameter(description = "实体类型: customer, contact") @PathVariable("entityType") String entityType) {
        return Result.ok(customFieldService.getFieldsByEntity(entityType));
    }

    @GetMapping("/enabled/{entityType}")
    @Operation(summary = "查询实体的已启用字段")
    public Result<List<CustomFieldVO>> enabled(
            @Parameter(description = "实体类型: customer, contact") @PathVariable("entityType") String entityType) {
        return Result.ok(customFieldService.getEnabledFieldsByEntity(entityType));
    }

    @PostMapping("/sort")
    @Operation(summary = "调整字段排序")
    public Result<String> sort(@RequestBody List<FieldSortBO> sortList) {
        customFieldService.updateSortOrder(sortList);
        return Result.ok();
    }
}
