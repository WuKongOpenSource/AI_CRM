package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.CandidateAddBO;
import com.kakarote.ai_crm.entity.BO.CandidateFieldUpdateBO;
import com.kakarote.ai_crm.entity.BO.CandidateQueryBO;
import com.kakarote.ai_crm.entity.BO.CandidateResumeParseBO;
import com.kakarote.ai_crm.entity.BO.CandidateUpdateBO;
import com.kakarote.ai_crm.entity.VO.CandidateDetailVO;
import com.kakarote.ai_crm.entity.VO.CandidateListVO;
import com.kakarote.ai_crm.entity.VO.CandidateResumeParseVO;
import com.kakarote.ai_crm.service.ICandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/candidate")
@Tag(name = "候选人管理")
public class CandidateController {

    @Autowired
    private ICandidateService candidateService;

    @PostMapping("/add")
    @Operation(summary = "新增候选人")
    @RequirePermission("candidate:create")
    public Result<Long> add(@Valid @RequestBody CandidateAddBO bo) {
        return Result.ok(candidateService.addCandidate(bo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新候选人")
    @RequirePermission("candidate:edit")
    public Result<Void> update(@Valid @RequestBody CandidateUpdateBO bo) {
        candidateService.updateCandidate(bo);
        return Result.ok();
    }

    @PostMapping("/updateField")
    @Operation(summary = "更新候选人单字段")
    @RequirePermission("candidate:edit")
    public Result<CandidateDetailVO> updateField(@Valid @RequestBody CandidateFieldUpdateBO bo) {
        return Result.ok(candidateService.updateCandidateField(bo));
    }

    @PostMapping("/delete/{candidateId}")
    @Operation(summary = "删除候选人")
    @RequirePermission("candidate:delete")
    public Result<Void> delete(@PathVariable Long candidateId) {
        candidateService.deleteCandidate(candidateId);
        return Result.ok();
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "分页查询候选人")
    @RequirePermission("candidate:view")
    public Result<BasePage<CandidateListVO>> queryPageList(@RequestBody CandidateQueryBO queryBO) {
        return Result.ok(candidateService.queryPageList(queryBO));
    }

    @GetMapping("/detail/{candidateId}")
    @Operation(summary = "候选人详情")
    @RequirePermission("candidate:view")
    public Result<CandidateDetailVO> detail(@PathVariable Long candidateId) {
        return Result.ok(candidateService.getCandidateDetail(candidateId));
    }

    @PostMapping("/updateStage")
    @Operation(summary = "更新候选人阶段")
    @RequirePermission("candidate:change_stage")
    public Result<Void> updateStage(
            @Parameter(description = "候选人ID") @RequestParam Long candidateId,
            @Parameter(description = "阶段") @RequestParam String stage) {
        candidateService.updateStage(candidateId, stage);
        return Result.ok();
    }

    @PostMapping("/ai-parse-resume")
    @Operation(summary = "AI 简历解析")
    @RequirePermission("candidate:create")
    public Result<CandidateResumeParseVO> aiParseResume(@RequestBody CandidateResumeParseBO bo) {
        return Result.ok(candidateService.aiParseResume(bo));
    }
}
