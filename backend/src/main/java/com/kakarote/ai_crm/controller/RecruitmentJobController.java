package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.RecruitmentJobAddBO;
import com.kakarote.ai_crm.entity.BO.RecruitmentJobQueryBO;
import com.kakarote.ai_crm.entity.BO.RecruitmentJobUpdateBO;
import com.kakarote.ai_crm.entity.VO.RecruitmentJobVO;
import com.kakarote.ai_crm.service.IRecruitmentJobService;
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
@RequestMapping("/recruitment-job")
@Tag(name = "招聘岗位配置")
public class RecruitmentJobController {

    @Autowired
    private IRecruitmentJobService recruitmentJobService;

    @PostMapping("/add")
    @Operation(summary = "新增招聘岗位")
    @RequirePermission("candidate:edit")
    public Result<Long> add(@Valid @RequestBody RecruitmentJobAddBO bo) {
        return Result.ok(recruitmentJobService.addRecruitmentJob(bo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新招聘岗位")
    @RequirePermission("candidate:edit")
    public Result<Void> update(@Valid @RequestBody RecruitmentJobUpdateBO bo) {
        recruitmentJobService.updateRecruitmentJob(bo);
        return Result.ok();
    }

    @PostMapping("/delete/{recruitmentJobId}")
    @Operation(summary = "删除招聘岗位")
    @RequirePermission("candidate:edit")
    public Result<Void> delete(@PathVariable Long recruitmentJobId) {
        recruitmentJobService.deleteRecruitmentJob(recruitmentJobId);
        return Result.ok();
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "分页查询招聘岗位")
    @RequirePermission("candidate:view")
    public Result<BasePage<RecruitmentJobVO>> queryPageList(@RequestBody RecruitmentJobQueryBO queryBO) {
        return Result.ok(recruitmentJobService.queryPageList(queryBO));
    }

    @PostMapping("/listOptions")
    @Operation(summary = "招聘岗位选项")
    @RequirePermission("candidate:view")
    public Result<List<RecruitmentJobVO>> listOptions(@RequestBody(required = false) RecruitmentJobQueryBO queryBO) {
        return Result.ok(recruitmentJobService.listOptions(queryBO));
    }

    @GetMapping("/detail/{recruitmentJobId}")
    @Operation(summary = "招聘岗位详情")
    @RequirePermission("candidate:view")
    public Result<RecruitmentJobVO> detail(@PathVariable Long recruitmentJobId) {
        return Result.ok(recruitmentJobService.getRecruitmentJobDetail(recruitmentJobId));
    }
}
