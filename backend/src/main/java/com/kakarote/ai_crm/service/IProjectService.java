package com.kakarote.ai_crm.service;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.ProjectBO;
import com.kakarote.ai_crm.entity.VO.ProjectVO;

import java.util.List;

public interface IProjectService {
    List<ProjectVO> listProjects();

    BasePage<ProjectVO> queryPageList(ProjectBO.Query queryBO);

    ProjectVO getProject(Long projectId);

    ProjectVO getProject(Long projectId, String taskKeyword);

    ProjectVO createProject(ProjectBO.Create createBO);

    ProjectVO updateProject(ProjectBO.Update updateBO);

    ProjectVO archiveProject(Long projectId);

    ProjectVO restoreProject(Long projectId);

    ProjectVO addProjectAttachment(Long projectId, ProjectBO.ProjectAttachmentSave attachmentBO);

    ProjectVO deleteProjectAttachment(Long projectId, Long attachmentId);

    ProjectVO addProjectSchedule(Long projectId, ProjectBO.ProjectScheduleSave scheduleBO);

    ProjectVO deleteProjectSchedule(Long projectId, Long scheduleId);

    void deleteProject(Long projectId);

    ProjectVO.ProjectRolePermissionConfigVO getProjectRolePermissionConfig();

    ProjectVO.ProjectRolePermissionConfigVO updateProjectRolePermissionConfig(ProjectBO.RolePermissionConfig configBO);

    ProjectVO addLane(Long projectId, ProjectBO.LaneSave laneBO);

    ProjectVO updateLane(Long projectId, ProjectBO.LaneSave laneBO);

    ProjectVO deleteLane(Long projectId, Long laneId);

    ProjectVO addTask(Long projectId, ProjectBO.TaskSave taskBO);

    ProjectVO updateTask(Long projectId, ProjectBO.TaskSave taskBO);

    ProjectVO addTaskAttachment(Long projectId, Long taskId, ProjectBO.TaskAttachmentSave attachmentBO);

    ProjectVO deleteTaskAttachment(Long projectId, Long taskId, Long attachmentId);

    ProjectVO.ProjectTaskAttachmentVO getTaskAttachment(Long projectId, Long taskId, Long attachmentId);

    ProjectVO deleteTask(Long projectId, Long taskId);

    ProjectVO moveTask(Long projectId, ProjectBO.TaskMove moveBO);

    ProjectVO addMember(Long projectId, ProjectBO.MemberSave memberBO);

    ProjectVO updateMemberRole(Long projectId, ProjectBO.MemberRole roleBO);

    ProjectVO updateMemberPermissions(Long projectId, ProjectBO.MemberPermissions permissionsBO);

    ProjectVO updateMemberStatus(Long projectId, ProjectBO.MemberStatus statusBO);

    ProjectVO handleProjectAiCommand(Long projectId, ProjectBO.AiCommand commandBO);

    ProjectVO handleTaskAiCommand(Long projectId, Long taskId, ProjectBO.AiCommand commandBO);
}
