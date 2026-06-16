package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.RelationAddBO;
import com.kakarote.ai_crm.entity.BO.RelationQueryBO;
import com.kakarote.ai_crm.entity.BO.RelationUpdateBO;
import com.kakarote.ai_crm.entity.VO.RelationDetailVO;
import com.kakarote.ai_crm.entity.VO.RelationVO;
import com.kakarote.ai_crm.service.IRelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 关系人管理控制器。
 */
@RestController
@RequestMapping("/relation")
@Tag(name = "关系人管理")
public class RelationController {

    @Autowired
    private IRelationService relationService;

    @PostMapping("/add")
    @Operation(summary = "创建关系人")
    public Result<Long> add(@Valid @RequestBody RelationAddBO relationAddBO) {
        return Result.ok(relationService.addRelation(relationAddBO));
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "分页查询关系人")
    public Result<BasePage<RelationVO>> queryPageList(@RequestBody RelationQueryBO queryBO) {
        return Result.ok(relationService.queryPageList(queryBO));
    }

    @PostMapping("/detail/{id}")
    @Operation(summary = "关系人详情")
    public Result<RelationDetailVO> detail(@PathVariable("id") Long id) {
        return Result.ok(relationService.detail(id));
    }

    @PostMapping("/update")
    @Operation(summary = "更新关系人")
    public Result<String> update(@Valid @RequestBody RelationUpdateBO relationUpdateBO) {
        relationService.updateRelation(relationUpdateBO);
        return Result.ok();
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "删除关系人")
    public Result<String> delete(@PathVariable("id") Long id) {
        relationService.deleteRelation(id);
        return Result.ok();
    }

    @PostMapping("/addFromContact/{contactId}")
    @Operation(summary = "从客户联系人添加到关系")
    public Result<Long> addFromContact(@PathVariable("contactId") Long contactId) {
        return Result.ok(relationService.addFromContact(contactId));
    }
}
