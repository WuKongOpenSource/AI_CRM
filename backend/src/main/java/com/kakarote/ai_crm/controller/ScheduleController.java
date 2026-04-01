package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.ScheduleAddBO;
import com.kakarote.ai_crm.entity.BO.ScheduleQueryBO;
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

    @PostMapping("/add")
    @Operation(summary = "创建日程")
    public Result<Long> add(@Valid @RequestBody ScheduleAddBO scheduleAddBO) {
        Long scheduleId = scheduleService.addSchedule(scheduleAddBO);
        return Result.ok(scheduleId);
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "删除日程")
    public Result<String> delete(@PathVariable("id") Long id) {
        scheduleService.deleteSchedule(id);
        return Result.ok();
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "分页查询日程")
    public Result<BasePage<ScheduleVO>> queryPageList(@RequestBody ScheduleQueryBO queryBO) {
        return Result.ok(scheduleService.queryPageList(queryBO));
    }

    @GetMapping("/mySchedules")
    @Operation(summary = "查询我的日程")
    public Result<List<ScheduleVO>> mySchedules(
            @Parameter(description = "筛选: all/today/thisWeek") @RequestParam(defaultValue = "all") String filter) {
        return Result.ok(scheduleService.getMySchedules(filter));
    }
}
