package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.ProjectBO;
import com.kakarote.ai_crm.entity.VO.ProjectVO;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.IProjectService;
import com.kakarote.ai_crm.utils.DocToHtmlConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/project")
@Tag(name = "Project")
public class ProjectController {

    private final IProjectService projectService;
    private final FileStorageService fileStorageService;

    public ProjectController(IProjectService projectService, FileStorageService fileStorageService) {
        this.projectService = projectService;
        this.fileStorageService = fileStorageService;
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

    @PostMapping("/{projectId}/attachment/add")
    @Operation(summary = "Add project attachment")
    public Result<ProjectVO> addProjectAttachment(@PathVariable Long projectId,
                                                  @RequestBody ProjectBO.ProjectAttachmentSave attachmentBO) {
        return Result.ok(projectService.addProjectAttachment(projectId, attachmentBO));
    }

    @PostMapping("/{projectId}/attachment/delete/{attachmentId}")
    @Operation(summary = "Delete project attachment")
    public Result<ProjectVO> deleteProjectAttachment(@PathVariable Long projectId,
                                                     @PathVariable Long attachmentId) {
        return Result.ok(projectService.deleteProjectAttachment(projectId, attachmentId));
    }

    @PostMapping("/{projectId}/schedule/add")
    @Operation(summary = "Add project schedule")
    public Result<ProjectVO> addProjectSchedule(@PathVariable Long projectId,
                                                @Valid @RequestBody ProjectBO.ProjectScheduleSave scheduleBO) {
        return Result.ok(projectService.addProjectSchedule(projectId, scheduleBO));
    }

    @PostMapping("/{projectId}/schedule/delete/{scheduleId}")
    @Operation(summary = "Delete project schedule")
    public Result<ProjectVO> deleteProjectSchedule(@PathVariable Long projectId,
                                                   @PathVariable Long scheduleId) {
        return Result.ok(projectService.deleteProjectSchedule(projectId, scheduleId));
    }

    @PostMapping("/delete/{projectId}")
    @Operation(summary = "Delete project")
    public Result<String> delete(@PathVariable Long projectId) {
        projectService.deleteProject(projectId);
        return Result.ok();
    }

    @GetMapping({"/role-permissions", "/rolePermissions"})
    @Operation(summary = "Project role permission config")
    public Result<ProjectVO.ProjectRolePermissionConfigVO> getRolePermissions() {
        return Result.ok(projectService.getProjectRolePermissionConfig());
    }

    @PostMapping({"/role-permissions", "/rolePermissions"})
    @Operation(summary = "Update project role permission config")
    public Result<ProjectVO.ProjectRolePermissionConfigVO> updateRolePermissions(
            @RequestBody ProjectBO.RolePermissionConfig configBO) {
        return Result.ok(projectService.updateProjectRolePermissionConfig(configBO));
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

    @PostMapping("/{projectId}/task/{taskId}/attachment/add")
    @Operation(summary = "Add project task attachment")
    public Result<ProjectVO> addTaskAttachment(@PathVariable Long projectId,
                                               @PathVariable Long taskId,
                                               @RequestBody ProjectBO.TaskAttachmentSave attachmentBO) {
        return Result.ok(projectService.addTaskAttachment(projectId, taskId, attachmentBO));
    }

    @PostMapping("/{projectId}/task/{taskId}/attachment/delete/{attachmentId}")
    @Operation(summary = "Delete project task attachment")
    public Result<ProjectVO> deleteTaskAttachment(@PathVariable Long projectId,
                                                  @PathVariable Long taskId,
                                                  @PathVariable Long attachmentId) {
        return Result.ok(projectService.deleteTaskAttachment(projectId, taskId, attachmentId));
    }

    @PostMapping("/{projectId}/task/{taskId}/attachment/{attachmentId}/delete")
    @Operation(summary = "Delete project task attachment")
    public Result<ProjectVO> deleteTaskAttachmentCompat(@PathVariable Long projectId,
                                                        @PathVariable Long taskId,
                                                        @PathVariable Long attachmentId) {
        return Result.ok(projectService.deleteTaskAttachment(projectId, taskId, attachmentId));
    }

    @GetMapping("/{projectId}/task/{taskId}/attachment/{attachmentId}/preview-html")
    @Operation(summary = "Preview project task attachment as HTML")
    public Result<String> previewTaskAttachmentHtml(@PathVariable Long projectId,
                                                    @PathVariable Long taskId,
                                                    @PathVariable Long attachmentId) {
        ProjectVO.ProjectTaskAttachmentVO attachment = projectService.getTaskAttachment(projectId, taskId, attachmentId);
        String fileName = attachment.getName() == null ? "" : attachment.getName();
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Only .doc files support HTML preview");
        }
        if (attachment.getFilePath() == null || attachment.getFilePath().isBlank()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Attachment file path is empty");
        }

        try (InputStream inputStream = fileStorageService.getFileStream(attachment.getFilePath())) {
            return Result.ok(DocToHtmlConverter.convertToHtml(inputStream));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "Document conversion failed");
        }
    }

    @GetMapping("/{projectId}/task/{taskId}/attachment/{attachmentId}/download")
    @Operation(summary = "Download project task attachment")
    public ResponseEntity<Resource> downloadTaskAttachment(@PathVariable Long projectId,
                                                           @PathVariable Long taskId,
                                                           @PathVariable Long attachmentId) {
        ProjectVO.ProjectTaskAttachmentVO attachment = projectService.getTaskAttachment(projectId, taskId, attachmentId);
        String filePath = attachment.getFilePath();
        if (filePath == null || filePath.isBlank()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Attachment file path is empty");
        }

        String fileName = attachment.getName() == null || attachment.getName().isBlank()
            ? "attachment"
            : attachment.getName();
        InputStream inputStream = fileStorageService.getFileStream(filePath);
        Resource resource = new InputStreamResource(inputStream);

        MediaType mediaType = MediaTypeFactory.getMediaType(fileName)
            .orElse(MediaType.APPLICATION_OCTET_STREAM);
        if (attachment.getMimeType() != null && !attachment.getMimeType().isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(attachment.getMimeType());
            } catch (Exception ignored) {
                // fallback to filename-based media type
            }
        }

        String encodedFilename = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
            .replace("+", "%20");
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);
        if (attachment.getFileSize() != null && attachment.getFileSize() >= 0) {
            builder.contentLength(attachment.getFileSize());
        }
        return builder.body(resource);
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

    @PostMapping("/{projectId}/member/add")
    @Operation(summary = "Add or update project member")
    public Result<ProjectVO> addMember(@PathVariable Long projectId, @Valid @RequestBody ProjectBO.MemberSave memberBO) {
        return Result.ok(projectService.addMember(projectId, memberBO));
    }

    @PostMapping("/{projectId}/member/role")
    @Operation(summary = "Update project member role")
    public Result<ProjectVO> updateMemberRole(@PathVariable Long projectId, @Valid @RequestBody ProjectBO.MemberRole roleBO) {
        return Result.ok(projectService.updateMemberRole(projectId, roleBO));
    }

    @PostMapping("/{projectId}/member/permissions")
    @Operation(summary = "Update project member permissions")
    public Result<ProjectVO> updateMemberPermissions(@PathVariable Long projectId,
                                                     @Valid @RequestBody ProjectBO.MemberPermissions permissionsBO) {
        return Result.ok(projectService.updateMemberPermissions(projectId, permissionsBO));
    }

    @PostMapping("/{projectId}/member/status")
    @Operation(summary = "Update project member status")
    public Result<ProjectVO> updateMemberStatus(@PathVariable Long projectId,
                                                @Valid @RequestBody ProjectBO.MemberStatus statusBO) {
        return Result.ok(projectService.updateMemberStatus(projectId, statusBO));
    }

    @PostMapping("/{projectId}/ai-command")
    @Operation(summary = "Project AI command")
    public Result<ProjectVO> projectAiCommand(@PathVariable Long projectId, @Valid @RequestBody ProjectBO.AiCommand commandBO) {
        return Result.ok(projectService.handleProjectAiCommand(projectId, commandBO));
    }

    @PostMapping("/{projectId}/task/{taskId}/ai-command")
    @Operation(summary = "Project task AI command")
    public Result<ProjectVO> taskAiCommand(@PathVariable Long projectId,
                                           @PathVariable Long taskId,
                                           @Valid @RequestBody ProjectBO.AiCommand commandBO) {
        return Result.ok(projectService.handleTaskAiCommand(projectId, taskId, commandBO));
    }
}
