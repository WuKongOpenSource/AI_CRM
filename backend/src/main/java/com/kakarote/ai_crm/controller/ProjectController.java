package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.ProjectBO;
import com.kakarote.ai_crm.entity.VO.ProjectVO;
import com.kakarote.ai_crm.service.IProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/project")
@Tag(name = "Project")
public class ProjectController {

    private final IProjectService projectService;

    public ProjectController(IProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/list")
    @Operation(summary = "Project list")
    public Result<List<ProjectVO>> list() {
        return Result.ok(projectService.listProjects());
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "Project page")
    public Result<BasePage<ProjectVO>> queryPageList(@RequestBody ProjectBO.Query queryBO) {
        return Result.ok(projectService.queryPageList(queryBO == null ? new ProjectBO.Query() : queryBO));
    }

    @GetMapping("/detail/{projectId}")
    @Operation(summary = "Project detail")
    public Result<ProjectVO> detail(@PathVariable Long projectId, @RequestParam(required = false) String taskKeyword) {
        return Result.ok(projectService.getProject(projectId, taskKeyword));
    }

    @PostMapping("/add")
    @Operation(summary = "Create project")
    public Result<ProjectVO> add(@Valid @RequestBody ProjectBO.Create createBO) {
        return Result.ok(projectService.createProject(createBO));
    }

    @PostMapping("/update")
    @Operation(summary = "Update project")
    public Result<ProjectVO> update(@Valid @RequestBody ProjectBO.Update updateBO) {
        return Result.ok(projectService.updateProject(updateBO));
    }

    @PostMapping("/archive/{projectId}")
    @Operation(summary = "Archive project")
    public Result<ProjectVO> archive(@PathVariable Long projectId) {
        return Result.ok(projectService.archiveProject(projectId));
    }

    @PostMapping("/restore/{projectId}")
    @Operation(summary = "Restore project")
    public Result<ProjectVO> restore(@PathVariable Long projectId) {
        return Result.ok(projectService.restoreProject(projectId));
    }

    @PostMapping("/delete/{projectId}")
    @Operation(summary = "Delete project")
    public Result<String> delete(@PathVariable Long projectId) {
        projectService.deleteProject(projectId);
        return Result.ok();
    }

    @PostMapping("/{projectId}/lane/add")
    @Operation(summary = "Create project lane")
    public Result<ProjectVO> addLane(@PathVariable Long projectId, @Valid @RequestBody ProjectBO.LaneSave laneBO) {
        return Result.ok(projectService.addLane(projectId, laneBO));
    }

    @PostMapping("/{projectId}/lane/update")
    @Operation(summary = "Update project lane")
    public Result<ProjectVO> updateLane(@PathVariable Long projectId, @Valid @RequestBody ProjectBO.LaneSave laneBO) {
        return Result.ok(projectService.updateLane(projectId, laneBO));
    }

    @PostMapping("/{projectId}/lane/delete/{laneId}")
    @Operation(summary = "Delete project lane")
    public Result<ProjectVO> deleteLane(@PathVariable Long projectId, @PathVariable Long laneId) {
        return Result.ok(projectService.deleteLane(projectId, laneId));
    }

    @PostMapping("/{projectId}/task/add")
    @Operation(summary = "Create project task")
    public Result<ProjectVO> addTask(@PathVariable Long projectId, @Valid @RequestBody ProjectBO.TaskSave taskBO) {
        return Result.ok(projectService.addTask(projectId, taskBO));
    }

    @PostMapping("/{projectId}/task/update")
    @Operation(summary = "Update project task")
    public Result<ProjectVO> updateTask(@PathVariable Long projectId, @Valid @RequestBody ProjectBO.TaskSave taskBO) {
        return Result.ok(projectService.updateTask(projectId, taskBO));
    }

    @PostMapping("/{projectId}/task/delete/{taskId}")
    @Operation(summary = "Delete project task")
    public Result<ProjectVO> deleteTask(@PathVariable Long projectId, @PathVariable Long taskId) {
        return Result.ok(projectService.deleteTask(projectId, taskId));
    }

    @PostMapping("/{projectId}/task/move")
    @Operation(summary = "Move project task")
    public Result<ProjectVO> moveTask(@PathVariable Long projectId, @Valid @RequestBody ProjectBO.TaskMove moveBO) {
        return Result.ok(projectService.moveTask(projectId, moveBO));
    }
}
