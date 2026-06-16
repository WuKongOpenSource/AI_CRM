package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.ProjectBO;
import com.kakarote.ai_crm.entity.VO.ProjectVO;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.IProjectService;
import com.kakarote.ai_crm.utils.DocToHtmlConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/project")
@Tag(name = "项目管理")
@Slf4j
public class ProjectController {

    private final IProjectService projectService;
    private final FileStorageService fileStorageService;

    @GetMapping("/list")
    @Operation(summary = "项目列表")
    public Result<List<ProjectVO>> list() {
        return Result.ok(projectService.listProjects());
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "项目分页列表")
    public Result<BasePage<ProjectVO>> queryPageList(@RequestBody ProjectBO.Query queryBO) {
        return Result.ok(projectService.queryPageList(queryBO == null ? new ProjectBO.Query() : queryBO));
    }

    @GetMapping("/detail/{projectId}")
    @Operation(summary = "项目详情")
    public Result<ProjectVO> detail(@PathVariable Long projectId, @RequestParam(required = false) String taskKeyword) {
        return Result.ok(projectService.getProject(projectId, taskKeyword));
    }

    @PostMapping("/add")
    @Operation(summary = "创建项目")
    public Result<ProjectVO> add(@Valid @RequestBody ProjectBO.Create createBO) {
        return Result.ok(projectService.createProject(createBO));
    }

    @PostMapping("/update")
    @Operation(summary = "更新项目")
    public Result<ProjectVO> update(@Valid @RequestBody ProjectBO.Update updateBO) {
        return Result.ok(projectService.updateProject(updateBO));
    }

    @PostMapping("/archive/{projectId}")
    @Operation(summary = "归档项目")
    public Result<ProjectVO> archive(@PathVariable Long projectId) {
        return Result.ok(projectService.archiveProject(projectId));
    }

    @PostMapping("/restore/{projectId}")
    @Operation(summary = "恢复项目")
    public Result<ProjectVO> restore(@PathVariable Long projectId) {
        return Result.ok(projectService.restoreProject(projectId));
    }

    @PostMapping("/delete/{projectId}")
    @Operation(summary = "删除项目")
    public Result<String> delete(@PathVariable Long projectId) {
        projectService.deleteProject(projectId);
        return Result.ok();
    }

    @GetMapping({"/role-permissions", "/rolePermissions"})
    @Operation(summary = "椤圭洰瑙掕壊榛樿鏉冮檺閰嶇疆")
    public Result<ProjectVO.ProjectRolePermissionConfigVO> getRolePermissions() {
        return Result.ok(projectService.getProjectRolePermissionConfig());
    }

    @PostMapping({"/role-permissions", "/rolePermissions"})
    @Operation(summary = "淇濆瓨椤圭洰瑙掕壊榛樿鏉冮檺閰嶇疆")
    public Result<ProjectVO.ProjectRolePermissionConfigVO> updateRolePermissions(
            @RequestBody ProjectBO.RolePermissionConfig configBO) {
        return Result.ok(projectService.updateProjectRolePermissionConfig(configBO));
    }

    @PostMapping("/{projectId}/lane/add")
    @Operation(summary = "新增项目泳道")
    public Result<ProjectVO> addLane(@PathVariable Long projectId, @Valid @RequestBody ProjectBO.LaneSave laneBO) {
        return Result.ok(projectService.addLane(projectId, laneBO));
    }

    @PostMapping("/{projectId}/lane/update")
    @Operation(summary = "更新项目泳道")
    public Result<ProjectVO> updateLane(@PathVariable Long projectId, @Valid @RequestBody ProjectBO.LaneSave laneBO) {
        return Result.ok(projectService.updateLane(projectId, laneBO));
    }

    @PostMapping("/{projectId}/lane/delete/{laneId}")
    @Operation(summary = "删除项目泳道")
    public Result<ProjectVO> deleteLane(@PathVariable Long projectId, @PathVariable Long laneId) {
        return Result.ok(projectService.deleteLane(projectId, laneId));
    }

    @PostMapping("/{projectId}/task/add")
    @Operation(summary = "新增项目任务")
    public Result<ProjectVO> addTask(@PathVariable Long projectId, @Valid @RequestBody ProjectBO.TaskSave taskBO) {
        return Result.ok(projectService.addTask(projectId, taskBO));
    }

    @PostMapping("/{projectId}/task/update")
    @Operation(summary = "更新项目任务")
    public Result<ProjectVO> updateTask(@PathVariable Long projectId, @Valid @RequestBody ProjectBO.TaskSave taskBO) {
        return Result.ok(projectService.updateTask(projectId, taskBO));
    }

    @PostMapping("/{projectId}/task/{taskId}/attachment/add")
    @Operation(summary = "添加项目任务附件")
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

    @DeleteMapping("/{projectId}/task/{taskId}/attachment/{attachmentId}")
    @Operation(summary = "Delete project task attachment")
    public Result<ProjectVO> deleteTaskAttachmentByDelete(@PathVariable Long projectId,
                                                          @PathVariable Long taskId,
                                                          @PathVariable Long attachmentId) {
        return Result.ok(projectService.deleteTaskAttachment(projectId, taskId, attachmentId));
    }

    @GetMapping("/{projectId}/task/{taskId}/attachment/{attachmentId}/preview-html")
    @Operation(summary = "预览项目任务 .doc 附件HTML")
    public Result<String> previewTaskAttachmentHtml(@PathVariable Long projectId,
                                                    @PathVariable Long taskId,
                                                    @PathVariable Long attachmentId) {
        ProjectVO.ProjectTaskAttachmentVO attachment = projectService.getTaskAttachment(projectId, taskId, attachmentId);
        String fileName = attachment.getName();
        String fileNameLower = fileName != null ? fileName.toLowerCase() : "";
        if (!fileNameLower.endsWith(".doc") || fileNameLower.endsWith(".docx")) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "该文件不是 .doc 格式");
        }
        try (InputStream inputStream = fileStorageService.getFileStream(attachment.getFilePath())) {
            return Result.ok(DocToHtmlConverter.convertToHtml(inputStream));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to convert .doc project task attachment to HTML, projectId={}, taskId={}, attachmentId={}",
                    projectId, taskId, attachmentId, e);
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "文档转换失败，请下载后查看");
        }
    }

    @GetMapping("/{projectId}/task/{taskId}/attachment/{attachmentId}/download")
    @Operation(summary = "下载项目任务附件")
    public ResponseEntity<Resource> downloadTaskAttachment(@PathVariable Long projectId,
                                                           @PathVariable Long taskId,
                                                           @PathVariable Long attachmentId) {
        ProjectVO.ProjectTaskAttachmentVO attachment = projectService.getTaskAttachment(projectId, taskId, attachmentId);
        InputStream inputStream = fileStorageService.getFileStream(attachment.getFilePath());
        Resource resource = new InputStreamResource(inputStream);

        MediaType mediaType = MediaTypeFactory.getMediaType(attachment.getName())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        if (attachment.getMimeType() != null && !attachment.getMimeType().isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(attachment.getMimeType());
            } catch (Exception ignored) {
                // fallback to filename-based media type
            }
        }

        String encodedFilename = URLEncoder.encode(attachment.getName(), StandardCharsets.UTF_8)
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
    @Operation(summary = "删除项目任务")
    public Result<ProjectVO> deleteTask(@PathVariable Long projectId, @PathVariable Long taskId) {
        return Result.ok(projectService.deleteTask(projectId, taskId));
    }

    @PostMapping("/{projectId}/task/move")
    @Operation(summary = "移动项目任务泳道")
    public Result<ProjectVO> moveTask(@PathVariable Long projectId, @Valid @RequestBody ProjectBO.TaskMove moveBO) {
        return Result.ok(projectService.moveTask(projectId, moveBO));
    }

    @PostMapping("/{projectId}/member/add")
    @Operation(summary = "添加或更新项目成员")
    public Result<ProjectVO> addMember(@PathVariable Long projectId, @Valid @RequestBody ProjectBO.MemberSave memberBO) {
        return Result.ok(projectService.addMember(projectId, memberBO));
    }

    @PostMapping("/{projectId}/member/role")
    @Operation(summary = "修改项目成员角色")
    public Result<ProjectVO> updateMemberRole(@PathVariable Long projectId, @Valid @RequestBody ProjectBO.MemberRole roleBO) {
        return Result.ok(projectService.updateMemberRole(projectId, roleBO));
    }

    @PostMapping("/{projectId}/member/permissions")
    @Operation(summary = "修改项目成员权限")
    public Result<ProjectVO> updateMemberPermissions(@PathVariable Long projectId, @Valid @RequestBody ProjectBO.MemberPermissions permissionsBO) {
        return Result.ok(projectService.updateMemberPermissions(projectId, permissionsBO));
    }

    @PostMapping("/{projectId}/member/status")
    @Operation(summary = "修改项目成员状态")
    public Result<ProjectVO> updateMemberStatus(@PathVariable Long projectId, @Valid @RequestBody ProjectBO.MemberStatus statusBO) {
        return Result.ok(projectService.updateMemberStatus(projectId, statusBO));
    }

    @PostMapping("/{projectId}/ai-command")
    @Operation(summary = "项目AI对话指令")
    public Result<ProjectVO> projectAiCommand(@PathVariable Long projectId, @Valid @RequestBody ProjectBO.AiCommand commandBO) {
        return Result.ok(projectService.handleProjectAiCommand(projectId, commandBO));
    }

    @PostMapping("/{projectId}/task/{taskId}/ai-command")
    @Operation(summary = "项目任务AI对话指令")
    public Result<ProjectVO> taskAiCommand(@PathVariable Long projectId,
                                           @PathVariable Long taskId,
                                           @Valid @RequestBody ProjectBO.AiCommand commandBO) {
        return Result.ok(projectService.handleTaskAiCommand(projectId, taskId, commandBO));
    }
}
