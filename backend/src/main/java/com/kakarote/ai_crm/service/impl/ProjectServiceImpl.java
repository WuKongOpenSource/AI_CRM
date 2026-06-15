package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.ProjectBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.Project;
import com.kakarote.ai_crm.entity.PO.ProjectAttachment;
import com.kakarote.ai_crm.entity.PO.ProjectLane;
import com.kakarote.ai_crm.entity.PO.ProjectSchedule;
import com.kakarote.ai_crm.entity.PO.ProjectTask;
import com.kakarote.ai_crm.entity.PO.ProjectTaskAttachment;
import com.kakarote.ai_crm.entity.VO.ProjectVO;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.ManageUserMapper;
import com.kakarote.ai_crm.mapper.ProjectAttachmentMapper;
import com.kakarote.ai_crm.mapper.ProjectLaneMapper;
import com.kakarote.ai_crm.mapper.ProjectMapper;
import com.kakarote.ai_crm.mapper.ProjectScheduleMapper;
import com.kakarote.ai_crm.mapper.ProjectTaskAttachmentMapper;
import com.kakarote.ai_crm.mapper.ProjectTaskMapper;
import com.kakarote.ai_crm.service.IKnowledgeService;
import com.kakarote.ai_crm.service.IProjectService;
import com.kakarote.ai_crm.service.ISystemConfigService;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
public class ProjectServiceImpl implements IProjectService {

    private static final String STATUS_NOT_STARTED = "NOT_STARTED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String TASK_STATUS_TODO = "TODO";
    private static final String TASK_STATUS_COMPLETED = "COMPLETED";
    private static final String PRIORITY_MEDIUM = "MEDIUM";
    private static final String PROJECT_ROLE_PERMISSION_CONFIG_KEY = "project.role.permissions";
    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MEMBER = "MEMBER";
    private static final String ROLE_READONLY = "READONLY";
    private static final String ROLE_EXTERNAL = "EXTERNAL";
    private static final String MEMBER_STATUS_ACTIVE = "ACTIVE";
    private static final List<String> PROJECT_ROLES = List.of(ROLE_OWNER, ROLE_ADMIN, ROLE_MEMBER, ROLE_READONLY, ROLE_EXTERNAL);
    private static final List<String> ALL_PERMISSIONS = List.of(
            "VIEW_PROJECT",
            "EDIT_PROJECT",
            "DELETE_PROJECT",
            "ARCHIVE_PROJECT",
            "ADD_MEMBER",
            "REMOVE_MEMBER",
            "MODIFY_MEMBER_PERMISSION",
            "CREATE_TASK",
            "EDIT_TASK",
            "DELETE_TASK",
            "MOVE_TASK",
            "ADD_LANE",
            "EDIT_LANE",
            "DELETE_LANE",
            "USE_AI_CHAT",
            "AI_CREATE_TASK",
            "UPLOAD_ATTACHMENT",
            "DELETE_ATTACHMENT",
            "CREATE_SCHEDULE",
            "VIEW_STATISTICS"
    );
    private static final Map<String, List<String>> DEFAULT_ROLE_PERMISSIONS = createDefaultRolePermissions();

    private final ProjectMapper projectMapper;
    private final ProjectLaneMapper projectLaneMapper;
    private final ProjectTaskMapper projectTaskMapper;
    private final ProjectTaskAttachmentMapper projectTaskAttachmentMapper;
    private final ProjectAttachmentMapper projectAttachmentMapper;
    private final ProjectScheduleMapper projectScheduleMapper;
    private final ManageUserMapper manageUserMapper;
    private final CustomerMapper customerMapper;
    private final IKnowledgeService knowledgeService;
    private final ISystemConfigService systemConfigService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ProjectServiceImpl(ProjectMapper projectMapper,
                              ProjectLaneMapper projectLaneMapper,
                              ProjectTaskMapper projectTaskMapper,
                              ProjectTaskAttachmentMapper projectTaskAttachmentMapper,
                              ProjectAttachmentMapper projectAttachmentMapper,
                              ProjectScheduleMapper projectScheduleMapper,
                              ManageUserMapper manageUserMapper,
                              CustomerMapper customerMapper,
                              IKnowledgeService knowledgeService,
                              ISystemConfigService systemConfigService,
                              JdbcTemplate jdbcTemplate,
                              ObjectMapper objectMapper) {
        this.projectMapper = projectMapper;
        this.projectLaneMapper = projectLaneMapper;
        this.projectTaskMapper = projectTaskMapper;
        this.projectTaskAttachmentMapper = projectTaskAttachmentMapper;
        this.projectAttachmentMapper = projectAttachmentMapper;
        this.projectScheduleMapper = projectScheduleMapper;
        this.manageUserMapper = manageUserMapper;
        this.customerMapper = customerMapper;
        this.knowledgeService = knowledgeService;
        this.systemConfigService = systemConfigService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ProjectVO> listProjects() {
        ProjectBO.Query query = new ProjectBO.Query();
        query.setPage(1);
        query.setLimit(1000);
        return queryPageList(query).getRecords();
    }

    @Override
    public BasePage<ProjectVO> queryPageList(ProjectBO.Query queryBO) {
        ProjectBO.Query query = queryBO == null ? new ProjectBO.Query() : queryBO;
        query.setStatus(normalizeProjectQueryStatus(query.getStatus()));
        query.setIncludeArchived(STATUS_ARCHIVED.equals(query.getStatus()));
        BasePage<ProjectVO> page = projectMapper.queryPageList(query.parse(), query);
        page.getRecords().forEach(this::fillProjectAccess);
        return page;
    }

    @Override
    public ProjectVO getProject(Long projectId) {
        return getProject(projectId, null);
    }

    @Override
    public ProjectVO getProject(Long projectId, String taskKeyword) {
        ProjectVO project = projectMapper.getProjectById(projectId);
        if (project == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Project does not exist");
        }
        project.setLanes(projectLaneMapper.selectList(Wrappers.<ProjectLane>lambdaQuery()
                        .eq(ProjectLane::getProjectId, projectId)
                        .orderByAsc(ProjectLane::getSortOrder)
                        .orderByAsc(ProjectLane::getCreateTime))
                .stream()
                .map(this::toLaneVO)
                .toList());
        List<ProjectVO.ProjectTaskVO> tasks = projectTaskMapper.selectProjectTasks(projectId, StrUtil.trimToNull(taskKeyword));
        tasks.forEach(this::fillTaskDetails);
        project.setTasks(tasks);
        project.setAttachments(loadProjectAttachments(projectId));
        project.setSchedules(loadProjectSchedules(projectId));
        project.setChatMessages(loadProjectChatMessages(projectId));
        project.setMembers(loadProjectMembers(projectId));
        project.setMemberLogs(loadProjectMemberLogs(projectId));
        fillProjectAccess(project);
        return project;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO createProject(ProjectBO.Create createBO) {
        Long currentUserId = UserUtil.getUserId();
        Long ownerId = createBO.getOwnerId() == null ? currentUserId : createBO.getOwnerId();
        ManagerUser owner = resolveUser(ownerId);

        Project project = new Project();
        project.setName(requireName(createBO.getName()));
        project.setDescription(normalizeOptional(createBO.getDescription()));
        project.setStatus(normalizeProjectStatus(createBO.getStatus()));
        project.setOwnerId(ownerId);
        project.setCustomerId(createBO.getCustomerId());
        project.setCustomerName(resolveCustomerName(createBO.getCustomerId(), createBO.getCustomerName()));
        project.setStartDate(createBO.getStartDate());
        project.setDueDate(createBO.getDueDate());
        project.setCreateUserId(currentUserId);
        project.setUpdateUserId(currentUserId);
        projectMapper.insert(project);

        insertLane(project.getProjectId(), "not-started", "未开始", 0, true, currentUserId);
        insertLane(project.getProjectId(), "in-progress", "进行中", 1, true, currentUserId);
        insertLane(project.getProjectId(), "completed", "已完成", 2, true, currentUserId);

        upsertMember(project.getProjectId(), ownerId, StrUtil.blankToDefault(owner.getRealname(), owner.getUsername()),
                owner.getUsername(), null, ROLE_OWNER, roleDefaultPermissions(ROLE_OWNER), "项目负责人",
                MEMBER_STATUS_ACTIVE, false);

        ProjectVO vo = BeanUtil.copyProperties(project, ProjectVO.class);
        vo.setOwnerName(StrUtil.blankToDefault(owner.getRealname(), owner.getUsername()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO updateProject(ProjectBO.Update updateBO) {
        Project project = getProjectEntity(updateBO.getProjectId());
        if (updateBO.getName() != null) {
            project.setName(requireName(updateBO.getName()));
        }
        if (updateBO.getDescription() != null) {
            project.setDescription(normalizeOptional(updateBO.getDescription()));
        }
        if (updateBO.getStatus() != null) {
            project.setStatus(normalizeProjectStatus(updateBO.getStatus()));
        }
        if (updateBO.getOwnerId() != null) {
            ManagerUser owner = resolveUser(updateBO.getOwnerId());
            project.setOwnerId(owner.getUserId());
            upsertMember(project.getProjectId(), owner.getUserId(), StrUtil.blankToDefault(owner.getRealname(), owner.getUsername()),
                    owner.getUsername(), null, ROLE_OWNER, roleDefaultPermissions(ROLE_OWNER), "项目负责人",
                    MEMBER_STATUS_ACTIVE, false);
        }
        if (updateBO.getCustomerId() != null || updateBO.getCustomerName() != null) {
            project.setCustomerId(updateBO.getCustomerId());
            project.setCustomerName(resolveCustomerName(updateBO.getCustomerId(), updateBO.getCustomerName()));
        }
        if (updateBO.getStartDate() != null) {
            project.setStartDate(updateBO.getStartDate());
        }
        if (updateBO.getDueDate() != null) {
            project.setDueDate(updateBO.getDueDate());
        }
        project.setUpdateUserId(UserUtil.getUserId());
        projectMapper.updateById(project);
        return getProject(project.getProjectId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO archiveProject(Long projectId) {
        Project project = getProjectEntity(projectId);
        project.setStatus(STATUS_ARCHIVED);
        project.setUpdateUserId(UserUtil.getUserId());
        projectMapper.updateById(project);
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO restoreProject(Long projectId) {
        Project project = getProjectEntity(projectId);
        project.setStatus(STATUS_NOT_STARTED);
        project.setUpdateUserId(UserUtil.getUserId());
        projectMapper.updateById(project);
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO addProjectAttachment(Long projectId, ProjectBO.ProjectAttachmentSave attachmentBO) {
        getProjectEntity(projectId);
        if (attachmentBO == null || StrUtil.isAllBlank(attachmentBO.getName(), attachmentBO.getFileUrl())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Attachment is required");
        }
        ProjectAttachment attachment = new ProjectAttachment();
        attachment.setProjectId(projectId);
        attachment.setName(StrUtil.blankToDefault(normalizeOptional(attachmentBO.getName()), normalizeOptional(attachmentBO.getFileUrl())));
        attachment.setFileUrl(normalizeOptional(attachmentBO.getFileUrl()));
        attachment.setCreateUserId(UserUtil.getUserId());
        attachment.setCreateUserName(currentUserDisplayName());
        projectAttachmentMapper.insert(attachment);
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO deleteProjectAttachment(Long projectId, Long attachmentId) {
        ProjectAttachment attachment = projectAttachmentMapper.selectById(attachmentId);
        if (attachment == null || !Objects.equals(attachment.getProjectId(), projectId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Project attachment does not exist");
        }
        projectAttachmentMapper.deleteById(attachmentId);
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO addProjectSchedule(Long projectId, ProjectBO.ProjectScheduleSave scheduleBO) {
        getProjectEntity(projectId);
        if (scheduleBO == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Schedule is required");
        }
        ProjectSchedule schedule = new ProjectSchedule();
        schedule.setProjectId(projectId);
        schedule.setTitle(requireName(scheduleBO.getTitle()));
        schedule.setScheduleTime(scheduleBO.getScheduleTime());
        schedule.setCreateUserId(UserUtil.getUserId());
        schedule.setCreateUserName(currentUserDisplayName());
        projectScheduleMapper.insert(schedule);
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO deleteProjectSchedule(Long projectId, Long scheduleId) {
        ProjectSchedule schedule = projectScheduleMapper.selectById(scheduleId);
        if (schedule == null || !Objects.equals(schedule.getProjectId(), projectId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Project schedule does not exist");
        }
        projectScheduleMapper.deleteById(scheduleId);
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long projectId) {
        getProjectEntity(projectId);
        projectAttachmentMapper.delete(Wrappers.<ProjectAttachment>lambdaQuery()
                .eq(ProjectAttachment::getProjectId, projectId));
        projectScheduleMapper.delete(Wrappers.<ProjectSchedule>lambdaQuery()
                .eq(ProjectSchedule::getProjectId, projectId));
        projectTaskAttachmentMapper.delete(Wrappers.<ProjectTaskAttachment>lambdaQuery()
                .eq(ProjectTaskAttachment::getProjectId, projectId));
        projectTaskMapper.delete(Wrappers.<ProjectTask>lambdaQuery().eq(ProjectTask::getProjectId, projectId));
        projectLaneMapper.delete(Wrappers.<ProjectLane>lambdaQuery().eq(ProjectLane::getProjectId, projectId));
        projectMapper.deleteById(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO addLane(Long projectId, ProjectBO.LaneSave laneBO) {
        getProjectEntity(projectId);
        Long currentUserId = UserUtil.getUserId();
        int sortOrder = nextLaneSortOrder(projectId);
        insertLane(projectId, null, laneBO.getName(), sortOrder, false, currentUserId);
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO updateLane(Long projectId, ProjectBO.LaneSave laneBO) {
        getProjectEntity(projectId);
        ProjectLane lane = getProjectLane(projectId, laneBO.getLaneId());
        lane.setName(requireName(laneBO.getName()));
        lane.setUpdateUserId(UserUtil.getUserId());
        projectLaneMapper.updateById(lane);
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO deleteLane(Long projectId, Long laneId) {
        getProjectEntity(projectId);
        ProjectLane lane = getProjectLane(projectId, laneId);
        if (Boolean.TRUE.equals(lane.getSystemFlag())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "System lane cannot be deleted");
        }
        Long taskCount = projectTaskMapper.selectCount(Wrappers.<ProjectTask>lambdaQuery()
                .eq(ProjectTask::getProjectId, projectId)
                .eq(ProjectTask::getLaneId, laneId));
        if (taskCount != null && taskCount > 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Lane has tasks");
        }
        projectLaneMapper.deleteById(laneId);
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO addTask(Long projectId, ProjectBO.TaskSave taskBO) {
        getProjectEntity(projectId);
        ProjectLane lane = taskBO.getLaneId() == null ? firstLane(projectId) : getProjectLane(projectId, taskBO.getLaneId());
        ProjectTask task = new ProjectTask();
        copyTaskFields(task, taskBO);
        task.setProjectId(projectId);
        task.setLaneId(lane.getLaneId());
        task.setStatus(TASK_STATUS_TODO);
        task.setPriority(normalizePriority(taskBO.getPriority()));
        task.setGeneratedByAi(Boolean.TRUE.equals(taskBO.getGeneratedByAi()));
        Long currentUserId = UserUtil.getUserId();
        task.setCreateUserId(currentUserId);
        task.setUpdateUserId(currentUserId);
        projectTaskMapper.insert(task);
        saveTaskAttachments(projectId, task.getTaskId(), taskBO.getAttachments());
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO updateTask(Long projectId, ProjectBO.TaskSave taskBO) {
        getProjectEntity(projectId);
        ProjectTask task = getProjectTask(projectId, taskBO.getTaskId());
        copyTaskFields(task, taskBO);
        if (taskBO.getLaneId() != null) {
            task.setLaneId(getProjectLane(projectId, taskBO.getLaneId()).getLaneId());
        }
        if (taskBO.getPriority() != null) {
            task.setPriority(normalizePriority(taskBO.getPriority()));
        }
        task.setUpdateUserId(UserUtil.getUserId());
        projectTaskMapper.updateById(task);
        saveTaskAttachments(projectId, task.getTaskId(), taskBO.getAttachments());
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO addTaskAttachment(Long projectId, Long taskId, ProjectBO.TaskAttachmentSave attachmentBO) {
        getProjectTask(projectId, taskId);
        saveTaskAttachment(projectId, taskId, attachmentBO);
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO deleteTaskAttachment(Long projectId, Long taskId, Long attachmentId) {
        ProjectTaskAttachment attachment = getProjectTaskAttachment(projectId, taskId, attachmentId);
        projectTaskAttachmentMapper.deleteById(attachment.getAttachmentId());
        return getProject(projectId);
    }

    @Override
    public ProjectVO.ProjectTaskAttachmentVO getTaskAttachment(Long projectId, Long taskId, Long attachmentId) {
        return toTaskAttachmentVO(getProjectTaskAttachment(projectId, taskId, attachmentId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO deleteTask(Long projectId, Long taskId) {
        getProjectTask(projectId, taskId);
        projectTaskAttachmentMapper.delete(Wrappers.<ProjectTaskAttachment>lambdaQuery()
                .eq(ProjectTaskAttachment::getProjectId, projectId)
                .eq(ProjectTaskAttachment::getTaskId, taskId));
        projectTaskMapper.deleteById(taskId);
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO moveTask(Long projectId, ProjectBO.TaskMove moveBO) {
        ProjectTask task = getProjectTask(projectId, moveBO.getTaskId());
        task.setLaneId(getProjectLane(projectId, moveBO.getLaneId()).getLaneId());
        if (isCompletedLane(task.getLaneId())) {
            task.setStatus(TASK_STATUS_COMPLETED);
        }
        task.setUpdateUserId(UserUtil.getUserId());
        projectTaskMapper.updateById(task);
        return getProject(projectId);
    }

    @Override
    public ProjectVO.ProjectRolePermissionConfigVO getProjectRolePermissionConfig() {
        return buildRolePermissionConfigVO(loadProjectRolePermissions());
    }

    @Override
    public ProjectVO.ProjectRolePermissionConfigVO updateProjectRolePermissionConfig(ProjectBO.RolePermissionConfig configBO) {
        Map<String, List<String>> normalized = normalizeRolePermissionConfig(
                configBO == null ? null : configBO.getRolePermissions());
        systemConfigService.updateConfig(PROJECT_ROLE_PERMISSION_CONFIG_KEY, writeJson(normalized));
        return buildRolePermissionConfigVO(normalized);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO addMember(Long projectId, ProjectBO.MemberSave memberBO) {
        getProjectEntity(projectId);
        ManagerUser user = resolveUser(memberBO.getUserId());
        String memberName = StrUtil.blankToDefault(normalizeOptional(memberBO.getMemberName()),
                StrUtil.blankToDefault(user.getRealname(), user.getUsername()));
        String account = StrUtil.blankToDefault(normalizeOptional(memberBO.getAccount()), user.getUsername());
        String role = normalizeRole(memberBO.getRole());
        List<String> permissions = memberBO.getPermissions() == null || memberBO.getPermissions().isEmpty()
                ? roleDefaultPermissions(role)
                : normalizePermissions(memberBO.getPermissions());
        upsertMember(projectId, user.getUserId(), memberName, account, normalizeOptional(memberBO.getDeptName()),
                role, permissions, normalizeOptional(memberBO.getRemark()), normalizeMemberStatus(memberBO.getStatus()), true);
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO updateMemberRole(Long projectId, ProjectBO.MemberRole roleBO) {
        getProjectEntity(projectId);
        ProjectVO.ProjectMemberVO before = findMember(projectId, roleBO.getUserId());
        String role = normalizeRole(roleBO.getRole());
        jdbcTemplate.update("""
                UPDATE crm_project_member
                SET role = ?, permissions = ?, last_action_time = CURRENT_TIMESTAMP, update_user_id = ?, update_time = CURRENT_TIMESTAMP
                WHERE project_id = ? AND user_id = ?
                """, role, writeJson(roleDefaultPermissions(role)), UserUtil.getUserId(), projectId, roleBO.getUserId());
        ProjectVO.ProjectMemberVO after = findMember(projectId, roleBO.getUserId());
        appendMemberLog(projectId, "UPDATE_ROLE", roleBO.getUserId(),
                after == null ? null : after.getMemberName(), memberSummary(before), memberSummary(after));
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO updateMemberPermissions(Long projectId, ProjectBO.MemberPermissions permissionsBO) {
        getProjectEntity(projectId);
        ProjectVO.ProjectMemberVO before = findMember(projectId, permissionsBO.getUserId());
        List<String> permissions = normalizePermissions(permissionsBO.getPermissions());
        jdbcTemplate.update("""
                UPDATE crm_project_member
                SET permissions = ?, last_action_time = CURRENT_TIMESTAMP, update_user_id = ?, update_time = CURRENT_TIMESTAMP
                WHERE project_id = ? AND user_id = ?
                """, writeJson(permissions), UserUtil.getUserId(), projectId, permissionsBO.getUserId());
        ProjectVO.ProjectMemberVO after = findMember(projectId, permissionsBO.getUserId());
        appendMemberLog(projectId, "UPDATE_PERMISSION", permissionsBO.getUserId(),
                after == null ? null : after.getMemberName(), memberSummary(before), memberSummary(after));
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO updateMemberStatus(Long projectId, ProjectBO.MemberStatus statusBO) {
        getProjectEntity(projectId);
        ProjectVO.ProjectMemberVO before = findMember(projectId, statusBO.getUserId());
        String status = normalizeMemberStatus(statusBO.getStatus());
        jdbcTemplate.update("""
                UPDATE crm_project_member
                SET status = ?, last_action_time = CURRENT_TIMESTAMP, update_user_id = ?, update_time = CURRENT_TIMESTAMP
                WHERE project_id = ? AND user_id = ?
                """, status, UserUtil.getUserId(), projectId, statusBO.getUserId());
        ProjectVO.ProjectMemberVO after = findMember(projectId, statusBO.getUserId());
        appendMemberLog(projectId, "UPDATE_STATUS", statusBO.getUserId(),
                after == null ? null : after.getMemberName(), memberSummary(before), memberSummary(after));
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO handleProjectAiCommand(Long projectId, ProjectBO.AiCommand commandBO) {
        getProjectEntity(projectId);
        String content = requireName(commandBO.getContent());
        appendProjectChat(projectId, "user", content);
        appendProjectChat(projectId, "assistant", "已记录项目指令。请继续在项目任务中跟进处理。");
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO handleTaskAiCommand(Long projectId, Long taskId, ProjectBO.AiCommand commandBO) {
        getProjectTask(projectId, taskId);
        String content = requireName(commandBO.getContent());
        appendTaskChat(projectId, taskId, "user", content);
        appendTaskChat(projectId, taskId, "assistant", "已记录任务指令。请继续在任务中跟进处理。");
        return getProject(projectId);
    }

    private void copyTaskFields(ProjectTask task, ProjectBO.TaskSave taskBO) {
        task.setTitle(requireName(taskBO.getTitle()));
        task.setDescription(normalizeOptional(taskBO.getDescription()));
        task.setDueDate(taskBO.getDueDate());
        task.setOwnerId(taskBO.getOwnerId());
        task.setOwnerName(normalizeOptional(taskBO.getOwnerName()));
        task.setParticipantUserIds(writeJson(taskBO.getParticipantIds() == null ? Collections.emptyList() : taskBO.getParticipantIds()));
        task.setParticipantNames(writeJson(taskBO.getParticipantNames() == null ? Collections.emptyList() : taskBO.getParticipantNames()));
        task.setCustomerId(taskBO.getCustomerId());
        task.setCustomerName(resolveCustomerName(taskBO.getCustomerId(), taskBO.getCustomerName()));
        task.setHasSchedule(Boolean.TRUE.equals(taskBO.getHasSchedule()));
        task.setSource(Boolean.TRUE.equals(taskBO.getGeneratedByAi()) ? "ai" : "manual");
        task.setAiSourceText(normalizeOptional(taskBO.getAiSourceText()));
    }

    private List<ProjectVO.ProjectAttachmentVO> loadProjectAttachments(Long projectId) {
        return projectAttachmentMapper.selectList(Wrappers.<ProjectAttachment>lambdaQuery()
                        .eq(ProjectAttachment::getProjectId, projectId)
                        .orderByDesc(ProjectAttachment::getCreateTime)
                        .orderByDesc(ProjectAttachment::getAttachmentId))
                .stream()
                .map(this::toProjectAttachmentVO)
                .toList();
    }

    private List<ProjectVO.ProjectScheduleVO> loadProjectSchedules(Long projectId) {
        return projectScheduleMapper.selectList(Wrappers.<ProjectSchedule>lambdaQuery()
                        .eq(ProjectSchedule::getProjectId, projectId)
                        .orderByDesc(ProjectSchedule::getScheduleTime)
                        .orderByDesc(ProjectSchedule::getCreateTime)
                        .orderByDesc(ProjectSchedule::getScheduleId))
                .stream()
                .map(this::toProjectScheduleVO)
                .toList();
    }

    private ProjectVO.ProjectAttachmentVO toProjectAttachmentVO(ProjectAttachment attachment) {
        ProjectVO.ProjectAttachmentVO vo = new ProjectVO.ProjectAttachmentVO();
        vo.setAttachmentId(attachment.getAttachmentId());
        vo.setName(attachment.getName());
        vo.setFileUrl(attachment.getFileUrl());
        vo.setCreatedByName(attachment.getCreateUserName());
        vo.setCreateTime(attachment.getCreateTime());
        return vo;
    }

    private ProjectVO.ProjectScheduleVO toProjectScheduleVO(ProjectSchedule schedule) {
        ProjectVO.ProjectScheduleVO vo = new ProjectVO.ProjectScheduleVO();
        vo.setScheduleId(schedule.getScheduleId());
        vo.setTitle(schedule.getTitle());
        vo.setScheduleTime(schedule.getScheduleTime());
        vo.setCreatedByName(schedule.getCreateUserName());
        vo.setCreateTime(schedule.getCreateTime());
        return vo;
    }

    private void saveTaskAttachments(Long projectId, Long taskId, List<ProjectBO.TaskAttachmentSave> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        attachments.stream()
                .filter(Objects::nonNull)
                .forEach(attachment -> saveTaskAttachment(projectId, taskId, attachment));
    }

    private void saveTaskAttachment(Long projectId, Long taskId, ProjectBO.TaskAttachmentSave attachmentBO) {
        if (attachmentBO == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Attachment is required");
        }
        String filePath = normalizeOptional(attachmentBO.getFilePath());
        String name = normalizeOptional(attachmentBO.getFileName());
        if (StrUtil.isBlank(name)) {
            name = StrUtil.blankToDefault(filePath, "Attachment");
        }
        ProjectTaskAttachment attachment = new ProjectTaskAttachment();
        attachment.setProjectId(projectId);
        attachment.setTaskId(taskId);
        attachment.setName(name);
        attachment.setFilePath(filePath);
        attachment.setFileUrl(filePath);
        attachment.setFileSize(attachmentBO.getFileSize());
        attachment.setMimeType(StrUtil.blankToDefault(normalizeOptional(attachmentBO.getMimeType()), "application/octet-stream"));
        attachment.setCreateUserId(UserUtil.getUserId());
        attachment.setCreateUserName(currentUserDisplayName());
        projectTaskAttachmentMapper.insert(attachment);
        scheduleTaskAttachmentKnowledgeSync(projectId, taskId, name, filePath, attachment.getFileSize(), attachment.getMimeType());
    }

    private void scheduleTaskAttachmentKnowledgeSync(Long projectId, Long taskId, String fileName,
                                                     String filePath, Long fileSize, String mimeType) {
        if (StrUtil.isBlank(filePath)) {
            return;
        }
        Runnable sync = () -> syncTaskAttachmentToKnowledge(projectId, taskId, fileName, filePath, fileSize, mimeType);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sync.run();
                }
            });
            return;
        }
        sync.run();
    }

    private void syncTaskAttachmentToKnowledge(Long projectId, Long taskId, String fileName,
                                               String filePath, Long fileSize, String mimeType) {
        try {
            Project project = getProjectEntity(projectId);
            ProjectTask task = getProjectTask(projectId, taskId);
            Long customerId = firstNonNull(task.getCustomerId(), project.getCustomerId());
            String summary = "项目任务附件自动同步。项目：" + project.getName() + "；任务：" + task.getTitle();
            knowledgeService.archiveExistingStandaloneFile(fileName, filePath, fileSize, mimeType, customerId, summary);
        } catch (Exception e) {
            log.warn("项目任务附件同步知识库失败: projectId={}, taskId={}, fileName={}, error={}",
                    projectId, taskId, fileName, e.getMessage(), e);
        }
    }

    private Long firstNonNull(Long first, Long second) {
        return first != null ? first : second;
    }

    private void fillTaskDetails(ProjectVO.ProjectTaskVO task) {
        ProjectTask entity = projectTaskMapper.selectById(task.getTaskId());
        if (entity != null) {
            task.setParticipantIds(parseLongList(entity.getParticipantUserIds()));
            task.setParticipantNames(parseStringList(entity.getParticipantNames()));
            task.setHasSchedule(Boolean.TRUE.equals(entity.getHasSchedule()));
            task.setSource(StrUtil.blankToDefault(entity.getSource(), Boolean.TRUE.equals(entity.getGeneratedByAi()) ? "ai" : "manual"));
        }
        List<ProjectVO.ProjectTaskAttachmentVO> attachments = projectTaskAttachmentMapper.selectList(
                        Wrappers.<ProjectTaskAttachment>lambdaQuery()
                                .eq(ProjectTaskAttachment::getTaskId, task.getTaskId())
                                .orderByDesc(ProjectTaskAttachment::getCreateTime)
                                .orderByDesc(ProjectTaskAttachment::getAttachmentId))
                .stream()
                .map(this::toTaskAttachmentVO)
                .toList();
        task.setAttachments(attachments);
        task.setHasAttachments(!attachments.isEmpty());
        task.setSchedules(loadTaskSchedules(task.getProjectId(), task.getTaskId()));
        task.setNotes(loadTaskNotes(task.getProjectId(), task.getTaskId()));
        task.setChatMessages(loadTaskChatMessages(task.getProjectId(), task.getTaskId()));
    }

    private ProjectVO.ProjectTaskAttachmentVO toTaskAttachmentVO(ProjectTaskAttachment attachment) {
        ProjectVO.ProjectTaskAttachmentVO vo = new ProjectVO.ProjectTaskAttachmentVO();
        vo.setAttachmentId(attachment.getAttachmentId());
        vo.setName(attachment.getName());
        vo.setFileUrl(attachment.getFileUrl());
        vo.setFilePath(attachment.getFilePath());
        vo.setFileSize(attachment.getFileSize());
        vo.setMimeType(attachment.getMimeType());
        vo.setCreatedByName(attachment.getCreateUserName());
        vo.setCreateTime(attachment.getCreateTime());
        return vo;
    }

    private List<ProjectVO.ProjectChatMessageVO> loadProjectChatMessages(Long projectId) {
        return jdbcTemplate.query("""
                SELECT message_id, role, content, create_time
                FROM crm_project_chat_message
                WHERE project_id = ?
                ORDER BY create_time ASC, message_id ASC
                """, (rs, rowNum) -> {
            ProjectVO.ProjectChatMessageVO vo = new ProjectVO.ProjectChatMessageVO();
            vo.setMessageId(rs.getLong("message_id"));
            vo.setRole(rs.getString("role"));
            vo.setContent(rs.getString("content"));
            vo.setCreateTime(rs.getTimestamp("create_time"));
            return vo;
        }, projectId);
    }

    private List<ProjectVO.ProjectChatMessageVO> loadTaskChatMessages(Long projectId, Long taskId) {
        return jdbcTemplate.query("""
                SELECT message_id, role, content, create_time
                FROM crm_project_task_chat_message
                WHERE project_id = ? AND task_id = ?
                ORDER BY create_time ASC, message_id ASC
                """, (rs, rowNum) -> {
            ProjectVO.ProjectChatMessageVO vo = new ProjectVO.ProjectChatMessageVO();
            vo.setMessageId(rs.getLong("message_id"));
            vo.setRole(rs.getString("role"));
            vo.setContent(rs.getString("content"));
            vo.setCreateTime(rs.getTimestamp("create_time"));
            return vo;
        }, projectId, taskId);
    }

    private List<ProjectVO.ProjectTaskScheduleVO> loadTaskSchedules(Long projectId, Long taskId) {
        return jdbcTemplate.query("""
                SELECT schedule_id, title, schedule_time, create_user_name, create_time
                FROM crm_project_task_schedule
                WHERE project_id = ? AND task_id = ?
                ORDER BY COALESCE(schedule_time, create_time) DESC, schedule_id DESC
                """, (rs, rowNum) -> {
            ProjectVO.ProjectTaskScheduleVO vo = new ProjectVO.ProjectTaskScheduleVO();
            vo.setScheduleId(rs.getLong("schedule_id"));
            vo.setTitle(rs.getString("title"));
            vo.setScheduleTime(rs.getTimestamp("schedule_time"));
            vo.setCreatedByName(rs.getString("create_user_name"));
            vo.setCreateTime(rs.getTimestamp("create_time"));
            return vo;
        }, projectId, taskId);
    }

    private List<ProjectVO.ProjectTaskNoteVO> loadTaskNotes(Long projectId, Long taskId) {
        return jdbcTemplate.query("""
                SELECT note_id, content, create_user_name, create_time
                FROM crm_project_task_note
                WHERE project_id = ? AND task_id = ?
                ORDER BY create_time DESC, note_id DESC
                """, (rs, rowNum) -> {
            ProjectVO.ProjectTaskNoteVO vo = new ProjectVO.ProjectTaskNoteVO();
            vo.setNoteId(rs.getLong("note_id"));
            vo.setContent(rs.getString("content"));
            vo.setCreatedByName(rs.getString("create_user_name"));
            vo.setCreateTime(rs.getTimestamp("create_time"));
            return vo;
        }, projectId, taskId);
    }

    private List<ProjectVO.ProjectMemberVO> loadProjectMembers(Long projectId) {
        return jdbcTemplate.query("""
                SELECT member_id, user_id, member_name, account, role, dept_name, joined_at,
                       last_action_time, status, permissions, remark
                FROM crm_project_member
                WHERE project_id = ?
                ORDER BY CASE WHEN status = 'ACTIVE' THEN 0 ELSE 1 END, joined_at ASC, member_id ASC
                """, (rs, rowNum) -> {
            ProjectVO.ProjectMemberVO vo = new ProjectVO.ProjectMemberVO();
            vo.setMemberId(rs.getLong("member_id"));
            vo.setUserId(rs.getLong("user_id"));
            vo.setMemberName(rs.getString("member_name"));
            vo.setAccount(rs.getString("account"));
            vo.setRole(rs.getString("role"));
            vo.setDeptName(rs.getString("dept_name"));
            vo.setJoinedAt(rs.getTimestamp("joined_at"));
            vo.setLastActionTime(rs.getTimestamp("last_action_time"));
            vo.setStatus(rs.getString("status"));
            vo.setPermissions(parseStringList(rs.getString("permissions")));
            vo.setRemark(rs.getString("remark"));
            return vo;
        }, projectId);
    }

    private List<ProjectVO.ProjectMemberLogVO> loadProjectMemberLogs(Long projectId) {
        return jdbcTemplate.query("""
                SELECT log_id, operator_id, operator_name, action_type, target_user_id, target_user_name,
                       before_summary, after_summary, create_time
                FROM crm_project_member_log
                WHERE project_id = ?
                ORDER BY create_time DESC, log_id DESC
                """, (rs, rowNum) -> {
            ProjectVO.ProjectMemberLogVO vo = new ProjectVO.ProjectMemberLogVO();
            vo.setLogId(rs.getLong("log_id"));
            vo.setOperatorId(rs.getLong("operator_id"));
            vo.setOperatorName(rs.getString("operator_name"));
            vo.setActionType(rs.getString("action_type"));
            vo.setTargetUserId(rs.getLong("target_user_id"));
            vo.setTargetUserName(rs.getString("target_user_name"));
            vo.setBeforeSummary(rs.getString("before_summary"));
            vo.setAfterSummary(rs.getString("after_summary"));
            vo.setCreateTime(rs.getTimestamp("create_time"));
            return vo;
        }, projectId);
    }

    private void fillProjectAccess(ProjectVO project) {
        Long currentUserId = UserUtil.getUserId();
        boolean owner = currentUserId != null && Objects.equals(project.getOwnerId(), currentUserId);
        if (owner) {
            project.setCurrentUserRole(ROLE_OWNER);
            project.setCurrentUserPermissions(new ArrayList<>(ALL_PERMISSIONS));
            return;
        }
        ProjectVO.ProjectMemberVO member = findActiveMember(project.getProjectId(), currentUserId);
        if (member != null) {
            project.setCurrentUserRole(member.getRole());
            project.setCurrentUserPermissions(member.getPermissions());
        }
    }

    private ProjectVO.ProjectMemberVO findActiveMember(Long projectId, Long userId) {
        ProjectVO.ProjectMemberVO member = findMember(projectId, userId);
        if (member == null || !MEMBER_STATUS_ACTIVE.equalsIgnoreCase(member.getStatus())) {
            return null;
        }
        return member;
    }

    private ProjectVO.ProjectMemberVO findMember(Long projectId, Long userId) {
        if (projectId == null || userId == null) {
            return null;
        }
        return loadProjectMembers(projectId).stream()
                .filter(member -> Objects.equals(member.getUserId(), userId))
                .findFirst()
                .orElse(null);
    }

    private void upsertMember(Long projectId, Long userId, String memberName, String account, String deptName,
                              String role, List<String> permissions, String remark, String status, boolean writeLog) {
        ProjectVO.ProjectMemberVO before = findMember(projectId, userId);
        if (before == null) {
            jdbcTemplate.update("""
                    INSERT INTO crm_project_member(
                        member_id, project_id, user_id, member_name, account, role, dept_name,
                        status, permissions, remark, create_user_id, update_user_id
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, IdWorker.getId(), projectId, userId, memberName, account, role, deptName,
                    status, writeJson(normalizePermissions(permissions)), remark, UserUtil.getUserId(), UserUtil.getUserId());
            if (writeLog) {
                appendMemberLog(projectId, "ADD_MEMBER", userId, memberName, null, "角色" + role);
            }
            return;
        }
        jdbcTemplate.update("""
                UPDATE crm_project_member
                SET member_name = ?, account = ?, role = ?, dept_name = ?, status = ?, permissions = ?,
                    remark = ?, last_action_time = CURRENT_TIMESTAMP, update_user_id = ?, update_time = CURRENT_TIMESTAMP
                WHERE project_id = ? AND user_id = ?
                """, memberName, account, role, deptName, status, writeJson(normalizePermissions(permissions)),
                remark, UserUtil.getUserId(), projectId, userId);
        if (writeLog) {
            ProjectVO.ProjectMemberVO after = findMember(projectId, userId);
            appendMemberLog(projectId, "UPDATE_PERMISSION", userId, memberName, memberSummary(before), memberSummary(after));
        }
    }

    private void appendMemberLog(Long projectId, String actionType, Long targetUserId, String targetUserName,
                                 String beforeSummary, String afterSummary) {
        jdbcTemplate.update("""
                INSERT INTO crm_project_member_log(
                    log_id, project_id, operator_id, operator_name, action_type, target_user_id,
                    target_user_name, before_summary, after_summary
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, IdWorker.getId(), projectId, UserUtil.getUserId(), currentUserDisplayName(), actionType,
                targetUserId, targetUserName, beforeSummary, afterSummary);
    }

    private void appendProjectChat(Long projectId, String role, String content) {
        jdbcTemplate.update("""
                INSERT INTO crm_project_chat_message(message_id, project_id, role, content, create_user_id, create_user_name)
                VALUES (?, ?, ?, ?, ?, ?)
                """, IdWorker.getId(), projectId, role, content, UserUtil.getUserId(), currentUserDisplayName());
    }

    private void appendTaskChat(Long projectId, Long taskId, String role, String content) {
        jdbcTemplate.update("""
                INSERT INTO crm_project_task_chat_message(message_id, project_id, task_id, role, content, create_user_id, create_user_name)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, IdWorker.getId(), projectId, taskId, role, content, UserUtil.getUserId(), currentUserDisplayName());
    }

    private void insertLane(Long projectId, String code, String name, int sortOrder, boolean systemFlag, Long userId) {
        ProjectLane lane = new ProjectLane();
        lane.setProjectId(projectId);
        lane.setCode(code);
        lane.setName(requireName(name));
        lane.setSortOrder(sortOrder);
        lane.setSystemFlag(systemFlag);
        lane.setCreateUserId(userId);
        lane.setUpdateUserId(userId);
        projectLaneMapper.insert(lane);
    }

    private Project getProjectEntity(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Project does not exist");
        }
        return project;
    }

    private ProjectLane getProjectLane(Long projectId, Long laneId) {
        ProjectLane lane = projectLaneMapper.selectById(laneId);
        if (lane == null || !Objects.equals(lane.getProjectId(), projectId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Project lane does not exist");
        }
        return lane;
    }

    private ProjectTask getProjectTask(Long projectId, Long taskId) {
        ProjectTask task = projectTaskMapper.selectById(taskId);
        if (task == null || !Objects.equals(task.getProjectId(), projectId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Project task does not exist");
        }
        return task;
    }

    private ProjectTaskAttachment getProjectTaskAttachment(Long projectId, Long taskId, Long attachmentId) {
        ProjectTaskAttachment attachment = projectTaskAttachmentMapper.selectById(attachmentId);
        if (attachment == null
                || !Objects.equals(attachment.getProjectId(), projectId)
                || !Objects.equals(attachment.getTaskId(), taskId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Project task attachment does not exist");
        }
        return attachment;
    }

    private ProjectLane firstLane(Long projectId) {
        ProjectLane lane = projectLaneMapper.selectOne(Wrappers.<ProjectLane>lambdaQuery()
                .eq(ProjectLane::getProjectId, projectId)
                .orderByAsc(ProjectLane::getSortOrder)
                .last("LIMIT 1"));
        if (lane == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Project lane does not exist");
        }
        return lane;
    }

    private int nextLaneSortOrder(Long projectId) {
        ProjectLane latest = projectLaneMapper.selectOne(Wrappers.<ProjectLane>lambdaQuery()
                .eq(ProjectLane::getProjectId, projectId)
                .orderByDesc(ProjectLane::getSortOrder)
                .last("LIMIT 1"));
        return latest == null || latest.getSortOrder() == null ? 10 : latest.getSortOrder() + 10;
    }

    private boolean isCompletedLane(Long laneId) {
        ProjectLane lane = projectLaneMapper.selectById(laneId);
        return lane != null && ("completed".equals(lane.getCode()) || "已完成".equals(lane.getName()));
    }

    private ManagerUser resolveUser(Long userId) {
        ManagerUser user = manageUserMapper.getUserId(userId);
        if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Owner does not exist");
        }
        return user;
    }

    private String resolveCustomerName(Long customerId, String fallback) {
        if (customerId == null) {
            return normalizeOptional(fallback);
        }
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null || Integer.valueOf(0).equals(customer.getStatus())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Customer does not exist");
        }
        return StrUtil.blankToDefault(customer.getCompanyName(), fallback);
    }

    private ProjectVO.ProjectLaneVO toLaneVO(ProjectLane lane) {
        ProjectVO.ProjectLaneVO vo = new ProjectVO.ProjectLaneVO();
        vo.setLaneId(lane.getLaneId());
        vo.setName(lane.getName());
        vo.setCode(lane.getCode());
        vo.setSortOrder(lane.getSortOrder());
        vo.setSystem(Boolean.TRUE.equals(lane.getSystemFlag()));
        return vo;
    }

    private String requireName(String value) {
        if (StrUtil.isBlank(value)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Name is required");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (StrUtil.isBlank(value) || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return value.trim();
    }

    private String currentUserDisplayName() {
        try {
            ManagerUser user = UserUtil.getLoginUser().getUser();
            return StrUtil.blankToDefault(user.getRealname(), user.getUsername());
        } catch (Exception ignored) {
            return String.valueOf(UserUtil.getUserId());
        }
    }

    private String normalizeProjectStatus(String status) {
        String normalized = StrUtil.blankToDefault(status, STATUS_NOT_STARTED).trim().toUpperCase(Locale.ROOT).replace("-", "_");
        return Set.of(STATUS_NOT_STARTED, STATUS_IN_PROGRESS, STATUS_COMPLETED, "PAUSED", STATUS_ARCHIVED).contains(normalized)
                ? normalized
                : STATUS_NOT_STARTED;
    }

    private String normalizeProjectQueryStatus(String status) {
        if (StrUtil.isBlank(status) || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        return normalizeProjectStatus(status);
    }

    private String normalizePriority(String priority) {
        String normalized = StrUtil.blankToDefault(priority, PRIORITY_MEDIUM).trim().toUpperCase(Locale.ROOT);
        return Set.of("LOW", PRIORITY_MEDIUM, "HIGH", "URGENT").contains(normalized) ? normalized : PRIORITY_MEDIUM;
    }

    private static Map<String, List<String>> createDefaultRolePermissions() {
        Map<String, List<String>> defaults = new LinkedHashMap<>();
        defaults.put(ROLE_OWNER, new ArrayList<>(ALL_PERMISSIONS));
        defaults.put(ROLE_ADMIN, ALL_PERMISSIONS.stream()
                .filter(permission -> !"DELETE_PROJECT".equals(permission))
                .toList());
        defaults.put(ROLE_MEMBER, List.of(
                "VIEW_PROJECT",
                "CREATE_TASK",
                "EDIT_TASK",
                "MOVE_TASK",
                "USE_AI_CHAT",
                "AI_CREATE_TASK",
                "UPLOAD_ATTACHMENT",
                "CREATE_SCHEDULE",
                "VIEW_STATISTICS"
        ));
        defaults.put(ROLE_READONLY, List.of("VIEW_PROJECT", "VIEW_STATISTICS"));
        defaults.put(ROLE_EXTERNAL, List.of("VIEW_PROJECT", "UPLOAD_ATTACHMENT"));
        return defaults;
    }

    private ProjectVO.ProjectRolePermissionConfigVO buildRolePermissionConfigVO(Map<String, List<String>> rolePermissions) {
        ProjectVO.ProjectRolePermissionConfigVO vo = new ProjectVO.ProjectRolePermissionConfigVO();
        vo.setRolePermissions(normalizeRolePermissionConfig(rolePermissions));
        return vo;
    }

    private Map<String, List<String>> loadProjectRolePermissions() {
        String config = systemConfigService.getConfigValue(PROJECT_ROLE_PERMISSION_CONFIG_KEY);
        if (StrUtil.isBlank(config)) {
            return createDefaultRolePermissions();
        }
        try {
            return normalizeRolePermissionConfig(objectMapper.readValue(config, new TypeReference<Map<String, List<String>>>() {}));
        } catch (Exception e) {
            log.warn("Failed to parse project role permission config", e);
            return createDefaultRolePermissions();
        }
    }

    private List<String> roleDefaultPermissions(String role) {
        Map<String, List<String>> rolePermissions = loadProjectRolePermissions();
        return new ArrayList<>(rolePermissions.getOrDefault(normalizeRole(role), rolePermissions.get(ROLE_MEMBER)));
    }

    private Map<String, List<String>> normalizeRolePermissionConfig(Map<String, List<String>> rolePermissions) {
        Map<String, List<String>> source = rolePermissions == null || rolePermissions.isEmpty()
                ? DEFAULT_ROLE_PERMISSIONS
                : rolePermissions;
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        PROJECT_ROLES.forEach(role -> normalized.put(role,
                normalizePermissions(source.getOrDefault(role, DEFAULT_ROLE_PERMISSIONS.get(role)))));
        source.forEach((role, permissions) -> {
            String normalizedRole = normalizeRole(role);
            if (PROJECT_ROLES.contains(normalizedRole)) {
                return;
            }
            normalized.put(normalizedRole, normalizePermissions(permissions));
        });
        return normalized;
    }

    private String normalizeRole(String role) {
        String normalized = StrUtil.blankToDefault(role, ROLE_MEMBER).trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? ROLE_MEMBER : normalized;
    }

    private String normalizeMemberStatus(String status) {
        String normalized = StrUtil.blankToDefault(status, MEMBER_STATUS_ACTIVE).trim().toUpperCase(Locale.ROOT);
        return Set.of(MEMBER_STATUS_ACTIVE, "REMOVED", "DISABLED").contains(normalized) ? normalized : MEMBER_STATUS_ACTIVE;
    }

    private List<String> normalizePermissions(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new ArrayList<>(DEFAULT_ROLE_PERMISSIONS.get(ROLE_MEMBER));
        }
        List<String> normalized = permissions.stream()
                .filter(StrUtil::isNotBlank)
                .map(permission -> permission.trim().toUpperCase(Locale.ROOT))
                .filter(ALL_PERMISSIONS::contains)
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (!normalized.contains("VIEW_PROJECT")) {
            normalized.add(0, "VIEW_PROJECT");
        }
        return normalized;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "Failed to serialize project data");
        }
    }

    private List<String> parseStringList(String value) {
        if (StrUtil.isBlank(value)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception ignored) {
            return java.util.Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .toList();
        }
    }

    private List<Long> parseLongList(String value) {
        if (StrUtil.isBlank(value)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<Long>>() {});
        } catch (Exception ignored) {
            return java.util.Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .map(Long::valueOf)
                    .toList();
        }
    }

    private String memberSummary(ProjectVO.ProjectMemberVO member) {
        if (member == null) {
            return null;
        }
        return "角色" + member.getRole() + "，状态" + member.getStatus() + "，权限" + member.getPermissions().size() + "项";
    }
}
