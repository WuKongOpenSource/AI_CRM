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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/relation")
@Tag(name = "Relation")
public class RelationController {

    private final IRelationService relationService;

    public RelationController(IRelationService relationService) {
        this.relationService = relationService;
    }

    @PostMapping("/add")
    @Operation(summary = "Create relation")
    public Result<Long> add(@Valid @RequestBody RelationAddBO relationAddBO) {
        return Result.ok(relationService.addRelation(relationAddBO));
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "Query relation page")
    public Result<BasePage<RelationVO>> queryPageList(@RequestBody RelationQueryBO queryBO) {
        return Result.ok(relationService.queryPageList(queryBO));
    }

    @PostMapping("/detail/{id}")
    @Operation(summary = "Relation detail")
    public Result<RelationDetailVO> detail(@PathVariable("id") Long id) {
        return Result.ok(relationService.detail(id));
    }

    @PostMapping("/update")
    @Operation(summary = "Update relation")
    public Result<String> update(@Valid @RequestBody RelationUpdateBO relationUpdateBO) {
        relationService.updateRelation(relationUpdateBO);
        return Result.ok();
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "Delete relation")
    public Result<String> delete(@PathVariable("id") Long id) {
        relationService.deleteRelation(id);
        return Result.ok();
    }

    @PostMapping("/addFromContact/{contactId}")
    @Operation(summary = "Create relation from customer contact")
    public Result<Long> addFromContact(@PathVariable("contactId") Long contactId) {
        return Result.ok(relationService.addFromContact(contactId));
    }
}
