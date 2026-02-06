package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.TaskAddBO;
import com.kakarote.ai_crm.entity.BO.TaskQueryBO;
import com.kakarote.ai_crm.entity.BO.TaskUpdateBO;
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

    @PostMapping("/add")
    @Operation(summary = "创建任务")
    public Result<Long> add(@Valid @RequestBody TaskAddBO taskAddBO) {
        Long taskId = taskService.addTask(taskAddBO);
        return Result.ok(taskId);
    }

    @PostMapping("/update")
    @Operation(summary = "更新任务")
    public Result<String> update(@Valid @RequestBody TaskUpdateBO taskUpdateBO) {
        taskService.updateTask(taskUpdateBO);
        return Result.ok();
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "删除任务")
    public Result<String> delete(@PathVariable("id") Long id) {
        taskService.deleteTask(id);
        return Result.ok();
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "分页查询任务")
    public Result<BasePage<TaskVO>> queryPageList(@RequestBody TaskQueryBO queryBO) {
        return Result.ok(taskService.queryPageList(queryBO));
    }

    @PostMapping("/updateStatus")
    @Operation(summary = "更新任务状态")
    public Result<String> updateStatus(
            @Parameter(description = "任务ID") @RequestParam Long taskId,
            @Parameter(description = "状态") @RequestParam String status) {
        taskService.updateStatus(taskId, status);
        return Result.ok();
    }

    @GetMapping("/myTasks")
    @Operation(summary = "查询我的任务")
    public Result<List<TaskVO>> myTasks(
            @Parameter(description = "筛选: all/today/thisWeek/overdue") @RequestParam(defaultValue = "all") String filter) {
        return Result.ok(taskService.getMyTasks(filter));
    }
}
