package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.TaskAddBO;
import com.kakarote.ai_crm.entity.BO.TaskAiParseBO;
import com.kakarote.ai_crm.entity.BO.TaskQueryBO;
import com.kakarote.ai_crm.entity.BO.TaskUpdateBO;
import com.kakarote.ai_crm.entity.VO.TaskAiParseVO;
import com.kakarote.ai_crm.entity.VO.TaskVO;
import com.kakarote.ai_crm.service.ITaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务管理控制器
 */
@RestController
@RequestMapping("/task")
@Tag(name = "任务管理")
public class TaskController {

    @Autowired
    private ITaskService taskService;

    /**
     * 创建任务。
     */
    @PostMapping("/add")
    @Operation(summary = "创建任务")
    @RequirePermission("task:create")
    public Result<Long> add(@Valid @RequestBody TaskAddBO taskAddBO) {
        Long taskId = taskService.addTask(taskAddBO);
        return Result.ok(taskId);
    }

    /**
     * 更新任务。
     */
    @PostMapping("/update")
    @Operation(summary = "更新任务")
    @RequirePermission("task:edit")
    public Result<String> update(@Valid @RequestBody TaskUpdateBO taskUpdateBO) {
        taskService.updateTask(taskUpdateBO);
        return Result.ok();
    }

    /**
     * 删除任务。
     */
    @PostMapping("/delete/{id}")
    @Operation(summary = "删除任务")
    @RequirePermission("task:delete")
    public Result<String> delete(@PathVariable("id") Long id) {
        taskService.deleteTask(id);
        return Result.ok();
    }

    /**
     * 分页查询任务。
     */
    @PostMapping("/queryPageList")
    @Operation(summary = "分页查询任务")
    @RequirePermission("task:view")
    public Result<BasePage<TaskVO>> queryPageList(@RequestBody TaskQueryBO queryBO) {
        return Result.ok(taskService.queryPageList(queryBO));
    }

    /**
     * 更新任务状态。
     */
    @PostMapping("/updateStatus")
    @Operation(summary = "更新任务状态")
    @RequirePermission("task:update_status")
    public Result<String> updateStatus(
            @Parameter(description = "任务ID") @RequestParam Long taskId,
            @Parameter(description = "状态") @RequestParam String status) {
        taskService.updateStatus(taskId, status);
        return Result.ok();
    }

    /**
     * 查询我的任务。
     */
    @GetMapping("/myTasks")
    @Operation(summary = "查询我的任务")
    @RequirePermission("task:view")
    public Result<List<TaskVO>> myTasks(
            @Parameter(description = "筛选: all/today/thisWeek/overdue") @RequestParam(defaultValue = "all") String filter) {
        return Result.ok(taskService.getMyTasks(filter));
    }

    /**
     * AI智能解析任务。
     */
    @PostMapping("/ai-parse")
    @Operation(summary = "AI智能解析任务")
    @RequirePermission("task:create")
    public Result<TaskAiParseVO> aiParse(@Valid @RequestBody TaskAiParseBO parseBO) {
        return Result.ok(taskService.aiParseTask(parseBO));
    }
}
