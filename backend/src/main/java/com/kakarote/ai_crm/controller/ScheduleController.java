package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.ScheduleAddBO;
import com.kakarote.ai_crm.entity.BO.ScheduleAiParseBO;
import com.kakarote.ai_crm.entity.BO.ScheduleQueryBO;
import com.kakarote.ai_crm.entity.BO.ScheduleUpdateBO;
import com.kakarote.ai_crm.entity.VO.ScheduleAiParseVO;
import com.kakarote.ai_crm.entity.VO.ScheduleVO;
import com.kakarote.ai_crm.service.IScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 日程管理控制器
 */
@RestController
@RequestMapping("/schedule")
@Tag(name = "日程管理")
public class ScheduleController {

    @Autowired
    private IScheduleService scheduleService;

    /**
     * 创建日程。
     */
    @PostMapping("/add")
    @Operation(summary = "创建日程")
    @RequirePermission("schedule:create")
    public Result<Long> add(@Valid @RequestBody ScheduleAddBO scheduleAddBO) {
        Long scheduleId = scheduleService.addSchedule(scheduleAddBO);
        return Result.ok(scheduleId);
    }

    /**
     * 更新日程。
     */
    @PostMapping("/update")
    @Operation(summary = "更新日程")
    @RequirePermission("schedule:edit")
    public Result<String> update(@Valid @RequestBody ScheduleUpdateBO scheduleUpdateBO) {
        scheduleService.updateSchedule(scheduleUpdateBO);
        return Result.ok();
    }

    /**
     * 删除日程。
     */
    @PostMapping("/delete/{id}")
    @Operation(summary = "删除日程")
    @RequirePermission("schedule:delete")
    public Result<String> delete(@PathVariable("id") Long id) {
        scheduleService.deleteSchedule(id);
        return Result.ok();
    }

    /**
     * 分页查询日程。
     */
    @PostMapping("/queryPageList")
    @Operation(summary = "分页查询日程")
    @RequirePermission("schedule:view")
    public Result<BasePage<ScheduleVO>> queryPageList(@RequestBody ScheduleQueryBO queryBO) {
        return Result.ok(scheduleService.queryPageList(queryBO));
    }

    /**
     * 查询我的日程。
     */
    @GetMapping("/mySchedules")
    @Operation(summary = "查询我的日程")
    @RequirePermission("schedule:view")
    public Result<List<ScheduleVO>> mySchedules(
            @Parameter(description = "筛选 all/today/thisWeek") @RequestParam(defaultValue = "all") String filter) {
        return Result.ok(scheduleService.getMySchedules(filter));
    }

    /**
     * AI智能解析日程。
     */
    @PostMapping("/ai-parse")
    @Operation(summary = "AI智能解析日程")
    @RequirePermission("schedule:create")
    public Result<ScheduleAiParseVO> aiParse(@Valid @RequestBody ScheduleAiParseBO parseBO) {
        return Result.ok(scheduleService.aiParseSchedule(parseBO));
    }
}
