package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.ai.app.ChatApplicationCodes;
import com.kakarote.ai_crm.ai.provider.AiModelCapabilities;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.ChatSendBO;
import com.kakarote.ai_crm.entity.BO.ProjectBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.Project;
import com.kakarote.ai_crm.entity.PO.ProjectAttachment;
import com.kakarote.ai_crm.entity.PO.ProjectLane;
import com.kakarote.ai_crm.entity.PO.ProjectSchedule;
import com.kakarote.ai_crm.entity.PO.ProjectTask;
import com.kakarote.ai_crm.entity.PO.ProjectTaskAttachment;
import com.kakarote.ai_crm.entity.VO.KnowledgeVO;
import com.kakarote.ai_crm.entity.VO.ProjectVO;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.ManageUserMapper;
import com.kakarote.ai_crm.mapper.ProjectAttachmentMapper;
import com.kakarote.ai_crm.mapper.ProjectLaneMapper;
import com.kakarote.ai_crm.mapper.ProjectMapper;
import com.kakarote.ai_crm.mapper.ProjectScheduleMapper;
import com.kakarote.ai_crm.mapper.ProjectTaskAttachmentMapper;
import com.kakarote.ai_crm.mapper.ProjectTaskMapper;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.IKnowledgeService;
import com.kakarote.ai_crm.service.IProjectService;
import com.kakarote.ai_crm.service.ISystemConfigService;
import com.kakarote.ai_crm.service.PermissionService;
import com.kakarote.ai_crm.utils.AiMediaUtil;
import com.kakarote.ai_crm.utils.DocumentTextExtractor;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.MimeType;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ProjectServiceImpl implements IProjectService {

    private static final String STATUS_NOT_STARTED = "NOT_STARTED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String TASK_STATUS_TODO = "TODO";
    private static final String TASK_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String TASK_STATUS_COMPLETED = "COMPLETED";
    private static final String PRIORITY_MEDIUM = "MEDIUM";
    private static final String PROJECT_ROLE_PERMISSION_CONFIG_KEY = "project.role.permissions";
    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MEMBER = "MEMBER";
    private static final String ROLE_READONLY = "READONLY";
    private static final String ROLE_EXTERNAL = "EXTERNAL";
    private static final String MEMBER_STATUS_ACTIVE = "ACTIVE";
    private static final int MAX_AI_CONTEXT_TEXT_LENGTH = 8_000;
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String PERMISSION_VIEW_PROJECT = "VIEW_PROJECT";
    private static final String PERMISSION_EDIT_PROJECT = "EDIT_PROJECT";
    private static final String PERMISSION_DELETE_PROJECT = "DELETE_PROJECT";
    private static final String PERMISSION_ARCHIVE_PROJECT = "ARCHIVE_PROJECT";
    private static final String PERMISSION_ADD_MEMBER = "ADD_MEMBER";
    private static final String PERMISSION_REMOVE_MEMBER = "REMOVE_MEMBER";
    private static final String PERMISSION_MODIFY_MEMBER_PERMISSION = "MODIFY_MEMBER_PERMISSION";
    private static final String PERMISSION_CREATE_TASK = "CREATE_TASK";
    private static final String PERMISSION_EDIT_TASK = "EDIT_TASK";
    private static final String PERMISSION_DELETE_TASK = "DELETE_TASK";
    private static final String PERMISSION_MOVE_TASK = "MOVE_TASK";
    private static final String PERMISSION_ADD_LANE = "ADD_LANE";
    private static final String PERMISSION_EDIT_LANE = "EDIT_LANE";
    private static final String PERMISSION_DELETE_LANE = "DELETE_LANE";
    private static final String PERMISSION_USE_AI_CHAT = "USE_AI_CHAT";
    private static final String PERMISSION_UPLOAD_ATTACHMENT = "UPLOAD_ATTACHMENT";
    private static final String PERMISSION_DELETE_ATTACHMENT = "DELETE_ATTACHMENT";
    private static final String PERMISSION_CREATE_SCHEDULE = "CREATE_SCHEDULE";
    private static final List<String> PROJECT_ROLES = List.of(ROLE_OWNER, ROLE_ADMIN, ROLE_MEMBER, ROLE_READONLY, ROLE_EXTERNAL);
    private static final List<String> ALL_PERMISSIONS = List.of(
            PERMISSION_VIEW_PROJECT,
            PERMISSION_EDIT_PROJECT,
            PERMISSION_DELETE_PROJECT,
            PERMISSION_ARCHIVE_PROJECT,
            PERMISSION_ADD_MEMBER,
            PERMISSION_REMOVE_MEMBER,
            PERMISSION_MODIFY_MEMBER_PERMISSION,
            PERMISSION_CREATE_TASK,
            PERMISSION_EDIT_TASK,
            PERMISSION_DELETE_TASK,
            PERMISSION_MOVE_TASK,
            PERMISSION_ADD_LANE,
            PERMISSION_EDIT_LANE,
            PERMISSION_DELETE_LANE,
            PERMISSION_USE_AI_CHAT,
            "AI_CREATE_TASK",
            PERMISSION_UPLOAD_ATTACHMENT,
            PERMISSION_DELETE_ATTACHMENT,
            PERMISSION_CREATE_SCHEDULE,
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
    private final DynamicChatClientProvider chatClientProvider;
    private final FileStorageService fileStorageService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    private PermissionService permissionService;

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
                              @Lazy
                              DynamicChatClientProvider chatClientProvider,
                              FileStorageService fileStorageService,
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
        this.chatClientProvider = chatClientProvider;
        this.fileStorageService = fileStorageService;
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
        Long currentUserId = UserUtil.getUserId();
        query.setCurrentUserId(currentUserId);
        query.setAdminAccess(isSystemAdmin());
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
        ensureProjectPermission(project, PERMISSION_VIEW_PROJECT);
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
        ensureProjectPermission(project, PERMISSION_EDIT_PROJECT);
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
        ensureProjectPermission(project, PERMISSION_ARCHIVE_PROJECT);
        project.setStatus(STATUS_ARCHIVED);
        project.setUpdateUserId(UserUtil.getUserId());
        projectMapper.updateById(project);
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO restoreProject(Long projectId) {
        Project project = getProjectEntity(projectId);
        ensureProjectPermission(project, PERMISSION_ARCHIVE_PROJECT);
        project.setStatus(STATUS_NOT_STARTED);
        project.setUpdateUserId(UserUtil.getUserId());
        projectMapper.updateById(project);
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO addProjectAttachment(Long projectId, ProjectBO.ProjectAttachmentSave attachmentBO) {
        ensureProjectPermission(getProjectEntity(projectId), PERMISSION_UPLOAD_ATTACHMENT);
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
        ensureProjectPermission(projectId, PERMISSION_DELETE_ATTACHMENT);
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
        ensureProjectPermission(getProjectEntity(projectId), PERMISSION_CREATE_SCHEDULE);
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
        ensureProjectPermission(projectId, PERMISSION_CREATE_SCHEDULE);
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
        ensureProjectPermission(getProjectEntity(projectId), PERMISSION_DELETE_PROJECT);
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
        ensureProjectPermission(getProjectEntity(projectId), PERMISSION_ADD_LANE);
        Long currentUserId = UserUtil.getUserId();
        int sortOrder = nextLaneSortOrder(projectId);
        insertLane(projectId, null, laneBO.getName(), sortOrder, false, currentUserId);
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO updateLane(Long projectId, ProjectBO.LaneSave laneBO) {
        ensureProjectPermission(getProjectEntity(projectId), PERMISSION_EDIT_LANE);
        ProjectLane lane = getProjectLane(projectId, laneBO.getLaneId());
        lane.setName(requireName(laneBO.getName()));
        lane.setUpdateUserId(UserUtil.getUserId());
        projectLaneMapper.updateById(lane);
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO deleteLane(Long projectId, Long laneId) {
        ensureProjectPermission(getProjectEntity(projectId), PERMISSION_DELETE_LANE);
        ProjectLane lane = getProjectLane(projectId, laneId);
        if (Boolean.TRUE.equals(lane.getSystemFlag())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "System lane cannot be deleted");
        }
        ProjectLane fallbackLane = firstLaneExcluding(projectId, laneId);
        ProjectTask movedTask = new ProjectTask();
        movedTask.setLaneId(fallbackLane.getLaneId());
        movedTask.setStatus(isCompletedLane(fallbackLane) ? TASK_STATUS_COMPLETED : TASK_STATUS_TODO);
        movedTask.setUpdateUserId(UserUtil.getUserId());
        projectTaskMapper.update(movedTask, Wrappers.<ProjectTask>lambdaUpdate()
                .eq(ProjectTask::getProjectId, projectId)
                .eq(ProjectTask::getLaneId, laneId));
        projectLaneMapper.deleteById(laneId);
        touchProject(projectId);
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO addTask(Long projectId, ProjectBO.TaskSave taskBO) {
        ensureProjectPermission(getProjectEntity(projectId), PERMISSION_CREATE_TASK);
        ProjectLane lane = taskBO.getLaneId() == null ? firstLane(projectId) : getProjectLane(projectId, taskBO.getLaneId());
        ProjectTask task = new ProjectTask();
        copyTaskFields(task, taskBO);
        task.setProjectId(projectId);
        task.setLaneId(lane.getLaneId());
        task.setStatus(laneToTaskStatus(lane));
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
        ensureProjectPermission(getProjectEntity(projectId), PERMISSION_EDIT_TASK);
        ProjectTask task = getProjectTask(projectId, taskBO.getTaskId());
        copyTaskFields(task, taskBO);
        if (taskBO.getLaneId() != null) {
            ProjectLane lane = getProjectLane(projectId, taskBO.getLaneId());
            task.setLaneId(lane.getLaneId());
            task.setStatus(laneToTaskStatus(lane));
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
        ensureProjectPermission(projectId, PERMISSION_UPLOAD_ATTACHMENT);
        getProjectTask(projectId, taskId);
        saveTaskAttachment(projectId, taskId, attachmentBO);
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO deleteTaskAttachment(Long projectId, Long taskId, Long attachmentId) {
        ensureProjectPermission(projectId, PERMISSION_DELETE_ATTACHMENT);
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
        ensureProjectPermission(projectId, PERMISSION_DELETE_TASK);
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
        ensureProjectPermission(projectId, PERMISSION_MOVE_TASK);
        ProjectTask task = getProjectTask(projectId, moveBO.getTaskId());
        ProjectLane lane = getProjectLane(projectId, moveBO.getLaneId());
        task.setLaneId(lane.getLaneId());
        task.setStatus(laneToTaskStatus(lane));
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
        ensureProjectPermission(getProjectEntity(projectId), PERMISSION_ADD_MEMBER);
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
        ensureProjectPermission(getProjectEntity(projectId), PERMISSION_MODIFY_MEMBER_PERMISSION);
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
        ensureProjectPermission(getProjectEntity(projectId), PERMISSION_MODIFY_MEMBER_PERMISSION);
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
        String permission = "REMOVED".equalsIgnoreCase(statusBO.getStatus()) ? PERMISSION_REMOVE_MEMBER : PERMISSION_MODIFY_MEMBER_PERMISSION;
        ensureProjectPermission(getProjectEntity(projectId), permission);
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
        ensureProjectPermission(getProjectEntity(projectId), PERMISSION_USE_AI_CHAT);
        String content = requireName(commandBO.getContent());
        appendProjectChat(projectId, "user", content);
        if (hasAiReferenceMaterials(commandBO)) {
            String reply = buildProjectMaterialAnalysisReply(projectId, null, content, commandBO);
            appendProjectChat(projectId, "assistant", reply);
            touchProject(projectId);
            return getProject(projectId);
        }
        String reply = "我已经记录到当前项目上下文。你也可以继续让我创建任务、总结进展或查询项目任务。";
        ProjectAiCommandParser.ParsedCommand command = ProjectAiCommandParser.parse(content);

        switch (command.action()) {
            case CREATE_TASK -> {
                ProjectBO.TaskSave taskBO = parseProjectAiTask(projectId, content, command.title());
                ProjectTask task = insertAiTask(projectId, taskBO);
                String scheduleReply = "";
                if (command.createTaskSchedule()) {
                    appendTaskSchedule(projectId, task.getTaskId(), task.getTitle() + "相关日程", task.getDueDate());
                    scheduleReply = " 同时已为该任务创建相关日程。";
                }
                reply = "已为当前项目创建任务「" + task.getTitle() + "」，并默认放入「未开始」泳道。"
                        + (task.getDueDate() == null ? "" : " 截止时间：" + task.getDueDate())
                        + scheduleReply;
            }
            case CREATE_LANE -> {
                ProjectBO.LaneSave laneBO = new ProjectBO.LaneSave();
                laneBO.setName(command.title());
                addLane(projectId, laneBO);
                reply = "已为当前项目创建泳道「" + command.title() + "」。";
            }
            case CREATE_PROJECT_SCHEDULE -> {
                ProjectBO.ProjectScheduleSave scheduleBO = new ProjectBO.ProjectScheduleSave();
                scheduleBO.setTitle(command.title());
                scheduleBO.setScheduleTime(parseRelativeDateTime(content));
                addProjectSchedule(projectId, scheduleBO);
                reply = "已为当前项目创建日程「" + command.title() + "」。";
            }
            case CREATE_PROJECT_ATTACHMENT -> {
                ProjectBO.ProjectAttachmentSave attachmentBO = new ProjectBO.ProjectAttachmentSave();
                attachmentBO.setName(command.title());
                addProjectAttachment(projectId, attachmentBO);
                reply = "已将「" + command.title() + "」记录为项目附件。";
            }
            case UPDATE_PROJECT_STATUS -> {
                updateProjectStatus(projectId, command.targetStatus());
                reply = "已将项目状态更新为「" + projectStatusName(command.targetStatus()) + "」。";
            }
            case ARCHIVE_PROJECT -> {
                updateProjectStatus(projectId, STATUS_ARCHIVED);
                reply = "已将项目状态更新为「已归档」。";
            }
            case SUMMARIZE_PROJECT -> reply = summarizeProject(projectId);
            case QUERY_TASKS -> reply = summarizeProjectTasks(projectId);
            case UNSAFE_DELETE -> reply = "删除类操作风险较高，请通过项目、任务或泳道的手动删除入口确认执行。";
            case UNKNOWN -> {
                // Keep the default context reply.
            }
        }

        appendProjectChat(projectId, "assistant", reply);
        touchProject(projectId);
        return getProject(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO handleTaskAiCommand(Long projectId, Long taskId, ProjectBO.AiCommand commandBO) {
        ensureProjectPermission(projectId, PERMISSION_USE_AI_CHAT);
        ProjectTask task = getProjectTask(projectId, taskId);
        String content = requireName(commandBO.getContent());
        appendTaskChat(projectId, taskId, "user", content);
        if (hasAiReferenceMaterials(commandBO)) {
            String reply = buildProjectMaterialAnalysisReply(projectId, task, content, commandBO);
            appendTaskChat(projectId, taskId, "assistant", reply);
            touchProject(projectId);
            return getProject(projectId);
        }
        String reply = "当前对话对象是任务「" + task.getTitle() + "」，我已经收到你的指令。";
        if (containsAny(content, "改到", "改成", "调整到", "延期到")
                && hasDateTimeIntent(content)) {
            Date dueDate = parseRelativeDateTime(content);
            if (dueDate != null) {
                task.setDueDate(dueDate);
                task.setUpdateUserId(UserUtil.getUserId());
                projectTaskMapper.updateById(task);
                reply = "已将任务「" + task.getTitle() + "」的截止时间更新为 " + dueDate + "。";
            }
        } else if (containsAny(content, "优先级")) {
            String priority = parsePriority(content);
            task.setPriority(priority);
            task.setUpdateUserId(UserUtil.getUserId());
            projectTaskMapper.updateById(task);
            reply = "已将任务优先级更新为「" + priority + "」。";
        } else if (containsAny(content, "移动", "移到", "挪到", "转到", "拖到")) {
            ensureProjectPermission(projectId, PERMISSION_MOVE_TASK);
            ProjectLane lane = findLaneByKeyword(projectId, content);
            if (lane != null) {
                task.setLaneId(lane.getLaneId());
                task.setStatus(laneToTaskStatus(lane));
                task.setUpdateUserId(UserUtil.getUserId());
                projectTaskMapper.updateById(task);
                reply = "已将任务「" + task.getTitle() + "」移动到「" + lane.getName() + "」。";
            }
        } else if (containsAny(content, "日程", "安排", "提醒")) {
            Date scheduleTime = parseRelativeDateTime(content);
            String title = extractScheduleTitle(content, task.getTitle());
            appendTaskSchedule(projectId, taskId, title, scheduleTime);
            reply = "已为任务创建相关日程「" + title + "」。";
        } else if (containsAny(content, "备注", "补充说明", "追加说明")) {
            appendTaskNote(projectId, taskId, extractNote(content));
            reply = "已为任务追加备注。";
        } else if (containsAny(content, "大纲", "执行方案", "方案")) {
            reply = buildTaskExecutionPlan(task);
            appendTaskNote(projectId, taskId, reply);
        }
        appendTaskChat(projectId, taskId, "assistant", reply);
        touchProject(projectId);
        return getProject(projectId);
    }

    private ProjectTask insertAiTask(Long projectId, ProjectBO.TaskSave taskBO) {
        ProjectLane lane = taskBO.getLaneId() == null ? firstLane(projectId) : getProjectLane(projectId, taskBO.getLaneId());
        ProjectTask task = new ProjectTask();
        copyTaskFields(task, taskBO);
        task.setProjectId(projectId);
        task.setLaneId(lane.getLaneId());
        task.setStatus(TASK_STATUS_TODO);
        task.setPriority(normalizePriority(taskBO.getPriority()));
        task.setGeneratedByAi(true);
        task.setSource("ai");
        task.setAiSourceText(normalizeOptional(taskBO.getAiSourceText()));
        Long currentUserId = UserUtil.getUserId();
        task.setCreateUserId(currentUserId);
        task.setUpdateUserId(currentUserId);
        projectTaskMapper.insert(task);
        saveTaskAttachments(projectId, task.getTaskId(), taskBO.getAttachments());
        return task;
    }

    private boolean hasAiReferenceMaterials(ProjectBO.AiCommand commandBO) {
        return commandBO != null && (hasItems(commandBO.getAttachments()) || hasItems(commandBO.getKnowledgeIds()));
    }

    private boolean hasItems(List<?> items) {
        return items != null && !items.isEmpty();
    }

    private String buildProjectMaterialAnalysisReply(Long projectId,
                                                     ProjectTask task,
                                                     String content,
                                                     ProjectBO.AiCommand commandBO) {
        Project project = getProjectEntity(projectId);
        try {
            DynamicChatClientProvider.AiRuntimeConfigSnapshot runtimeConfig = chatClientProvider.getRuntimeConfigSnapshot(
                    commandBO.getModelProvider(),
                    commandBO.getModelName()
            );
            if (StrUtil.isBlank(runtimeConfig.apiKey())) {
                return "请先在系统设置-系统参数设置-AI/API设置中配置 AI 大模型相关信息。";
            }

            List<KnowledgeVO> selectedKnowledge = loadSelectedKnowledge(commandBO.getKnowledgeIds());
            AiModelCapabilities capabilities = runtimeConfig.capabilities();
            String attachmentContext = buildAiAttachmentContext(commandBO.getAttachments());
            String knowledgeContext = buildAiKnowledgeContext(selectedKnowledge);
            String systemPrompt = buildProjectMaterialSystemPrompt(project, task);
            String userContent = buildProjectMaterialUserContent(content, project, task, attachmentContext, knowledgeContext);

            if (containsImageReference(commandBO.getAttachments(), selectedKnowledge)
                    && (capabilities == null || !capabilities.isSupportsVision())) {
                userContent = userContent + "\n\n[系统提示] 当前选择的模型不支持图片理解，请仅基于可提取文本、文件名和已有上下文回答；如需图片内容分析，请切换到支持视觉的模型。";
            }

            List<Media> mediaList = buildAiMediaList(commandBO.getAttachments(), selectedKnowledge, capabilities);
            String finalUserContent = userContent;
            ChatClient chatClient = chatClientProvider.getChatClient(
                    commandBO.getModelProvider(),
                    commandBO.getModelName(),
                    ChatApplicationCodes.PROJECT
            );
            ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt().system(systemPrompt);
            String response = hasItems(mediaList)
                    ? requestSpec.user(user -> user.text(finalUserContent).media(mediaList.toArray(new Media[0]))).call().content()
                    : requestSpec.user(finalUserContent).call().content();
            if (StrUtil.isBlank(response)) {
                return "我没有从当前资料中得到可用结论，请换一种问法或重新选择文件后再试。";
            }
            return response.trim();
        } catch (BusinessException e) {
            return e.getMsg();
        } catch (Exception e) {
            log.warn("项目资料 AI 分析失败: projectId={}, taskId={}", projectId, task == null ? null : task.getTaskId(), e);
            return "项目资料分析失败，请稍后再试。";
        }
    }

    private String buildProjectMaterialSystemPrompt(Project project, ProjectTask task) {
        String taskContext = task == null
                ? ""
                : "\n当前任务：%s；状态：%s；优先级：%s；截止时间：%s。".formatted(
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate()
        );
        return """
                你是悟空 CRM 的项目资料分析助手。请基于当前项目/任务上下文、用户上传的图片或附件，以及用户选中的知识库文件回答。
                要求：
                1. 优先分析用户本次选择的图片、附件和知识库内容。
                2. 如果资料中没有答案，请明确说明缺少哪些信息，不要编造。
                3. 如果用户要求总结、提炼风险、生成执行建议，请给出可操作的条目。
                4. 如果资料不足，请列出还需要补充的信息。
                5. 使用简体中文，回答要清晰、具体。

                当前项目ID：%s；当前项目：%s；状态：%s；关联客户：%s；负责人ID：%s。%s
                """.formatted(
                project.getProjectId(),
                project.getName(),
                project.getStatus(),
                StrUtil.blankToDefault(project.getCustomerName(), "未关联"),
                project.getOwnerId(),
                taskContext
        );
    }

    private String buildProjectMaterialUserContent(String content,
                                                   Project project,
                                                   ProjectTask task,
                                                   String attachmentContext,
                                                   String knowledgeContext) {
        StringBuilder builder = new StringBuilder();
        builder.append("用户问题：").append(content).append("\n\n");
        builder.append("[项目上下文]\n");
        builder.append("- 项目名称：").append(project.getName()).append("\n");
        builder.append("- 项目描述：").append(StrUtil.blankToDefault(project.getDescription(), "无")).append("\n");
        builder.append("- 项目状态：").append(project.getStatus()).append("\n");
        builder.append("- 关联客户：").append(StrUtil.blankToDefault(project.getCustomerName(), "未关联")).append("\n");
        if (task != null) {
            builder.append("\n[任务上下文]\n");
            builder.append("- 任务名称：").append(task.getTitle()).append("\n");
            builder.append("- 任务描述：").append(StrUtil.blankToDefault(task.getDescription(), "无")).append("\n");
            builder.append("- 任务状态：").append(task.getStatus()).append("\n");
            builder.append("- 优先级：").append(task.getPriority()).append("\n");
            builder.append("- 截止时间：").append(task.getDueDate()).append("\n");
        }
        if (StrUtil.isNotBlank(attachmentContext)) {
            builder.append("\n").append(attachmentContext);
        }
        if (StrUtil.isNotBlank(knowledgeContext)) {
            builder.append("\n").append(knowledgeContext);
        }
        return builder.toString();
    }

    private List<KnowledgeVO> loadSelectedKnowledge(List<Long> knowledgeIds) {
        if (!hasItems(knowledgeIds)) {
            return Collections.emptyList();
        }
        List<KnowledgeVO> result = new ArrayList<>();
        for (Long knowledgeId : knowledgeIds) {
            if (knowledgeId == null) {
                continue;
            }
            try {
                KnowledgeVO knowledge = knowledgeService.getKnowledgeDetail(knowledgeId);
                if (knowledge != null) {
                    result.add(knowledge);
                }
            } catch (Exception e) {
                log.warn("读取项目对话知识库文件失败: knowledgeId={}, error={}", knowledgeId, e.getMessage());
            }
        }
        return result;
    }

    private String buildAiKnowledgeContext(List<KnowledgeVO> selectedKnowledge) {
        if (!hasItems(selectedKnowledge)) {
            return "";
        }
        StringBuilder context = new StringBuilder("[用户从知识库选中的文件]\n");
        for (KnowledgeVO knowledge : selectedKnowledge) {
            context.append("- 知识库文件：")
                    .append(StrUtil.blankToDefault(knowledge.getName(), "未命名文件"))
                    .append("（knowledgeId=").append(knowledge.getKnowledgeId()).append("）\n");
            if (StrUtil.isNotBlank(knowledge.getSummary())) {
                context.append("  摘要：").append(abbreviateAiContext(knowledge.getSummary(), 800)).append("\n");
            }
            String contentText = StrUtil.blankToDefault(knowledge.getContentText(), "");
            if (StrUtil.isBlank(contentText) && StrUtil.isNotBlank(knowledge.getFilePath())) {
                contentText = extractAttachmentText(knowledge.getFilePath(), knowledge.getMimeType(), knowledge.getName());
            }
            if (StrUtil.isNotBlank(contentText)) {
                context.append("  内容摘录：\n```\n")
                        .append(abbreviateAiContext(contentText, MAX_AI_CONTEXT_TEXT_LENGTH))
                        .append("\n```\n");
            } else if (isImageFile(knowledge.getMimeType(), knowledge.getName())) {
                context.append("  说明：该知识库文件是图片，已在模型支持时作为图片传入。\n");
            } else {
                context.append("  说明：该文件暂无可读取文本，请结合文件名、摘要和用户问题谨慎回答。\n");
            }
        }
        return context.toString();
    }

    private String buildAiAttachmentContext(List<ChatSendBO.AttachmentDTO> attachments) {
        if (!hasItems(attachments)) {
            return "";
        }
        StringBuilder context = new StringBuilder("[用户上传的文件]\n");
        for (ChatSendBO.AttachmentDTO attachment : attachments) {
            if (attachment == null) {
                continue;
            }
            String fileName = StrUtil.blankToDefault(attachment.getFileName(), "未命名文件");
            String mimeType = attachment.getMimeType();
            if (isImageFile(mimeType, fileName)) {
                context.append("- 图片：").append(fileName).append("（已在模型支持时作为图片传入，请直接分析图片内容）\n");
                continue;
            }
            String text = extractAttachmentText(attachment.getFilePath(), mimeType, fileName);
            if (StrUtil.isNotBlank(text)) {
                context.append("- 文件：").append(fileName).append("，内容摘录如下：\n```\n")
                        .append(abbreviateAiContext(text, MAX_AI_CONTEXT_TEXT_LENGTH))
                        .append("\n```\n");
            } else {
                context.append("- 文件：").append(fileName)
                        .append("（类型：").append(StrUtil.blankToDefault(mimeType, "未知")).append("，暂无法提取文本）\n");
            }
        }
        return context.toString();
    }

    private List<Media> buildAiMediaList(List<ChatSendBO.AttachmentDTO> attachments,
                                         List<KnowledgeVO> selectedKnowledge,
                                         AiModelCapabilities capabilities) {
        if (capabilities == null || !capabilities.isSupportsVision()) {
            return Collections.emptyList();
        }
        List<Media> mediaList = new ArrayList<>();
        if (hasItems(attachments)) {
            for (ChatSendBO.AttachmentDTO attachment : attachments) {
                if (attachment != null && isImageFile(attachment.getMimeType(), attachment.getFileName())) {
                    addImageMedia(mediaList, attachment.getFileName(), attachment.getFilePath(), attachment.getMimeType());
                }
            }
        }
        if (hasItems(selectedKnowledge)) {
            for (KnowledgeVO knowledge : selectedKnowledge) {
                if (knowledge != null && isImageFile(knowledge.getMimeType(), knowledge.getName())) {
                    addImageMedia(mediaList, knowledge.getName(), knowledge.getFilePath(), knowledge.getMimeType());
                }
            }
        }
        return mediaList;
    }

    private void addImageMedia(List<Media> mediaList, String fileName, String filePath, String mimeType) {
        if (StrUtil.isBlank(filePath)) {
            return;
        }
        try {
            MimeType resolvedMimeType = MimeType.valueOf(StrUtil.blankToDefault(mimeType, "image/jpeg"));
            mediaList.add(AiMediaUtil.buildMedia(fileStorageService, filePath, resolvedMimeType));
        } catch (Exception e) {
            log.warn("构建项目对话图片媒体失败: fileName={}, filePath={}", fileName, filePath, e);
        }
    }

    private boolean containsImageReference(List<ChatSendBO.AttachmentDTO> attachments, List<KnowledgeVO> selectedKnowledge) {
        if (hasItems(attachments) && attachments.stream()
                .filter(Objects::nonNull)
                .anyMatch(attachment -> isImageFile(attachment.getMimeType(), attachment.getFileName()))) {
            return true;
        }
        return hasItems(selectedKnowledge) && selectedKnowledge.stream()
                .filter(Objects::nonNull)
                .anyMatch(knowledge -> isImageFile(knowledge.getMimeType(), knowledge.getName()));
    }

    private String extractAttachmentText(String filePath, String mimeType, String fileName) {
        if (StrUtil.isBlank(filePath)) {
            return "";
        }
        if (isTextFile(mimeType, fileName)) {
            return extractTextFile(filePath);
        }
        if (isDocumentFile(mimeType, fileName)) {
            return extractDocumentFile(filePath);
        }
        return "";
    }

    private String extractTextFile(String filePath) {
        try (InputStream inputStream = fileStorageService.getFileStream(filePath)) {
            if (inputStream == null) {
                return "";
            }
            byte[] bytes = inputStream.readNBytes(MAX_AI_CONTEXT_TEXT_LENGTH * 3);
            return abbreviateAiContext(new String(bytes, StandardCharsets.UTF_8), MAX_AI_CONTEXT_TEXT_LENGTH);
        } catch (Exception e) {
            log.warn("读取项目对话文本附件失败: {}", filePath, e);
            return "";
        }
    }

    private String extractDocumentFile(String filePath) {
        try (InputStream inputStream = fileStorageService.getFileStream(filePath)) {
            if (inputStream == null) {
                return "";
            }
            String text = DocumentTextExtractor.parseToString(inputStream, null, filePath);
            return abbreviateAiContext(text, MAX_AI_CONTEXT_TEXT_LENGTH);
        } catch (Exception e) {
            log.warn("提取项目对话文档附件失败: {}", filePath, e);
            return "";
        }
    }

    private boolean isImageFile(String mimeType, String fileName) {
        if (mimeType != null && mimeType.startsWith("image/")) {
            return true;
        }
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".bmp");
    }

    private boolean isTextFile(String mimeType, String fileName) {
        if (mimeType != null && (mimeType.startsWith("text/") || "application/json".equals(mimeType))) {
            return true;
        }
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv")
                || lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".yaml")
                || lower.endsWith(".yml") || lower.endsWith(".log");
    }

    private boolean isDocumentFile(String mimeType, String fileName) {
        if (mimeType != null
                && (mimeType.equals("application/pdf")
                || mimeType.equals("application/msword")
                || mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || mimeType.equals("application/vnd.ms-excel")
                || mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                || mimeType.equals("application/vnd.ms-powerpoint")
                || mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation"))) {
            return true;
        }
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx")
                || lower.endsWith(".xls") || lower.endsWith(".xlsx")
                || lower.endsWith(".ppt") || lower.endsWith(".pptx");
    }

    private String abbreviateAiContext(String text, int limit) {
        String normalized = StrUtil.trim(text);
        if (StrUtil.isBlank(normalized)) {
            return "";
        }
        if (normalized.length() <= limit) {
            return normalized;
        }
        return normalized.substring(0, limit) + "\n...(内容过长已截断)";
    }

    private ProjectBO.TaskSave parseProjectAiTask(Long projectId, String content, String parsedTitle) {
        Project project = getProjectEntity(projectId);
        ProjectBO.TaskSave task = new ProjectBO.TaskSave();
        task.setTitle(StrUtil.blankToDefault(normalizeOptional(parsedTitle), "项目任务"));
        task.setDescription("由 AI 指令创建：" + content);
        task.setDueDate(parseRelativeDateTime(content));
        task.setPriority(parsePriority(content));
        task.setCustomerId(project.getCustomerId());
        task.setCustomerName(project.getCustomerName());
        task.setGeneratedByAi(true);
        task.setAiSourceText(content);
        return task;
    }

    private void appendTaskSchedule(Long projectId, Long taskId, String title, Date scheduleTime) {
        jdbcTemplate.update("""
                INSERT INTO crm_project_task_schedule(schedule_id, project_id, task_id, title, schedule_time, create_user_id, create_user_name)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, IdWorker.getId(), projectId, taskId, requireName(title), scheduleTime,
                UserUtil.getUserId(), currentUserDisplayName());
    }

    private void appendTaskNote(Long projectId, Long taskId, String content) {
        jdbcTemplate.update("""
                INSERT INTO crm_project_task_note(note_id, project_id, task_id, content, create_user_id, create_user_name)
                VALUES (?, ?, ?, ?, ?, ?)
                """, IdWorker.getId(), projectId, taskId, requireName(content),
                UserUtil.getUserId(), currentUserDisplayName());
    }

    private void updateProjectStatus(Long projectId, String status) {
        Project project = new Project();
        project.setProjectId(projectId);
        project.setStatus(normalizeProjectStatus(status));
        project.setUpdateUserId(UserUtil.getUserId());
        projectMapper.updateById(project);
    }

    private void touchProject(Long projectId) {
        Project project = new Project();
        project.setProjectId(projectId);
        project.setUpdateUserId(UserUtil.getUserId());
        projectMapper.updateById(project);
    }

    private String summarizeProject(Long projectId) {
        ProjectVO project = getProject(projectId);
        long completed = project.getTasks().stream()
                .filter(task -> TASK_STATUS_COMPLETED.equalsIgnoreCase(task.getStatus()))
                .count();
        return "项目「" + project.getName() + "」当前状态为「" + projectStatusName(project.getStatus()) + "」，共有 "
                + project.getTasks().size() + " 个任务，已完成 " + completed + " 个，未完成 "
                + Math.max(project.getTasks().size() - completed, 0) + " 个。";
    }

    private String summarizeProjectTasks(Long projectId) {
        List<ProjectVO.ProjectTaskVO> tasks = getProject(projectId).getTasks();
        if (tasks.isEmpty()) {
            return "当前项目暂无任务。";
        }
        StringBuilder builder = new StringBuilder("当前项目任务：");
        for (int i = 0; i < Math.min(tasks.size(), 8); i++) {
            ProjectVO.ProjectTaskVO task = tasks.get(i);
            builder.append("\n- ").append(task.getTitle())
                    .append("（").append(StrUtil.blankToDefault(task.getStatus(), "未设置")).append("）");
            if (task.getDueDate() != null) {
                builder.append("，截止 ").append(task.getDueDate());
            }
        }
        if (tasks.size() > 8) {
            builder.append("\n还有 ").append(tasks.size() - 8).append(" 个任务未列出。");
        }
        return builder.toString();
    }

    private Date parseRelativeDateTime(String content) {
        if (!hasDateTimeIntent(content)) {
            return null;
        }
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        LocalDate date = today;
        LocalDate explicitDate = parseExplicitDate(content, today);
        DayOfWeek weekday = parseChineseWeekday(content);
        if (explicitDate != null) {
            date = explicitDate;
        } else if (weekday != null) {
            LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            if (content.contains("下周")) {
                weekStart = weekStart.plusWeeks(1);
            }
            date = weekStart.plusDays(weekday.getValue() - 1L);
            if (!content.contains("下周") && date.isBefore(today)) {
                date = date.plusWeeks(1);
            }
        } else if (content.contains("后天")) {
            date = date.plusDays(2);
        } else if (content.contains("明天")) {
            date = date.plusDays(1);
        } else if (content.contains("本周") || content.contains("这周")) {
            date = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        } else if (content.contains("下周")) {
            date = date.plusWeeks(1);
        } else if (content.contains("本月") || content.contains("月底") || content.contains("月末")) {
            date = date.with(TemporalAdjusters.lastDayOfMonth());
        }

        int hour = containsAny(content, "下午", "晚上", "本周", "这周", "本月", "月底", "月末", "完成", "截止") ? 18 : 9;
        int minute = 0;
        Matcher colon = Pattern.compile("(\\d{1,2})[:：](\\d{1,2})").matcher(content);
        if (colon.find()) {
            hour = Integer.parseInt(colon.group(1));
            minute = Integer.parseInt(colon.group(2));
        } else {
            Matcher chineseHour = Pattern.compile("(\\d{1,2})\\s*点(?:\\s*(\\d{1,2})\\s*分?)?").matcher(content);
            if (chineseHour.find()) {
                hour = Integer.parseInt(chineseHour.group(1));
                if (StrUtil.isNotBlank(chineseHour.group(2))) {
                    minute = Integer.parseInt(chineseHour.group(2));
                }
            } else if (content.contains("三点")) {
                hour = 3;
            } else if (content.contains("两点") || content.contains("二点")) {
                hour = 2;
            }
        }
        if ((content.contains("下午") || content.contains("晚上")) && hour < 12) {
            hour += 12;
        }
        return Date.from(LocalDateTime.of(date, LocalTime.of(Math.min(Math.max(hour, 0), 23), Math.min(Math.max(minute, 0), 59)))
                .atZone(DEFAULT_ZONE)
                .toInstant());
    }

    private boolean hasDateTimeIntent(String content) {
        if (StrUtil.isBlank(content)) {
            return false;
        }
        return containsAny(content, "今天", "明天", "后天", "本周", "这周", "下周", "本月", "月底", "月末", "上午", "下午", "晚上", "点", ":", "：")
                || Pattern.compile("\\d{4}-\\d{1,2}-\\d{1,2}|\\d{1,2}/\\d{1,2}|\\d{1,2}月\\d{1,2}[日号]?|周[一二三四五六日天]|星期[一二三四五六日天]").matcher(content).find();
    }

    private LocalDate parseExplicitDate(String content, LocalDate baseDate) {
        Matcher iso = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})").matcher(content);
        if (iso.find()) {
            return localDateOf(Integer.parseInt(iso.group(1)), Integer.parseInt(iso.group(2)), Integer.parseInt(iso.group(3)));
        }
        Matcher slash = Pattern.compile("(\\d{1,2})/(\\d{1,2})").matcher(content);
        if (slash.find()) {
            return localDateOf(baseDate.getYear(), Integer.parseInt(slash.group(1)), Integer.parseInt(slash.group(2)));
        }
        Matcher chinese = Pattern.compile("(\\d{1,2})月(\\d{1,2})[日号]?").matcher(content);
        if (chinese.find()) {
            return localDateOf(baseDate.getYear(), Integer.parseInt(chinese.group(1)), Integer.parseInt(chinese.group(2)));
        }
        return null;
    }

    private LocalDate localDateOf(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException ignored) {
            return null;
        }
    }

    private DayOfWeek parseChineseWeekday(String content) {
        if (containsAny(content, "周一", "星期一")) {
            return DayOfWeek.MONDAY;
        }
        if (containsAny(content, "周二", "星期二")) {
            return DayOfWeek.TUESDAY;
        }
        if (containsAny(content, "周三", "星期三")) {
            return DayOfWeek.WEDNESDAY;
        }
        if (containsAny(content, "周四", "星期四")) {
            return DayOfWeek.THURSDAY;
        }
        if (containsAny(content, "周五", "星期五")) {
            return DayOfWeek.FRIDAY;
        }
        if (containsAny(content, "周六", "星期六")) {
            return DayOfWeek.SATURDAY;
        }
        if (containsAny(content, "周日", "周天", "星期日", "星期天")) {
            return DayOfWeek.SUNDAY;
        }
        return null;
    }

    private String parsePriority(String content) {
        if (containsAny(content, "紧急", "urgent")) {
            return "URGENT";
        }
        if (containsAny(content, "高", "重要", "high")) {
            return "HIGH";
        }
        if (containsAny(content, "低", "low")) {
            return "LOW";
        }
        return PRIORITY_MEDIUM;
    }

    private ProjectLane findLaneByKeyword(Long projectId, String content) {
        if (StrUtil.isBlank(content)) {
            return null;
        }
        List<ProjectLane> lanes = projectLaneMapper.selectList(Wrappers.<ProjectLane>lambdaQuery()
                .eq(ProjectLane::getProjectId, projectId)
                .orderByAsc(ProjectLane::getSortOrder)
                .orderByAsc(ProjectLane::getCreateTime));
        for (ProjectLane lane : lanes) {
            if (lane == null) {
                continue;
            }
            if (StrUtil.isNotBlank(lane.getName()) && content.contains(lane.getName())) {
                return lane;
            }
            if (StrUtil.isNotBlank(lane.getCode())
                    && content.toLowerCase(Locale.ROOT).contains(lane.getCode().toLowerCase(Locale.ROOT))) {
                return lane;
            }
            if (containsAny(content, "完成", "完结", "结束")
                    && ("completed".equals(lane.getCode()) || "已完成".equals(lane.getName()))) {
                return lane;
            }
            if (containsAny(content, "进行", "处理中")
                    && ("in_progress".equals(lane.getCode()) || "进行中".equals(lane.getName()))) {
                return lane;
            }
            if (containsAny(content, "开始", "待办", "未开始")
                    && ("todo".equals(lane.getCode()) || "not_started".equals(lane.getCode()) || "未开始".equals(lane.getName()))) {
                return lane;
            }
        }
        return null;
    }

    private String extractScheduleTitle(String content, String fallback) {
        String title = StrUtil.blankToDefault(content, fallback);
        title = title.replaceAll("请|帮我|创建|新增|添加|安排|日程|会议|提醒", "").trim();
        return StrUtil.blankToDefault(title, fallback + "相关日程");
    }

    private String extractNote(String content) {
        String note = StrUtil.blankToDefault(content, "").replaceAll("备注|补充说明|追加说明", "").trim();
        return StrUtil.blankToDefault(note, content);
    }

    private String buildTaskExecutionPlan(ProjectTask task) {
        return "任务「" + task.getTitle() + "」建议按以下步骤推进：\n"
                + "1. 明确交付目标和验收标准。\n"
                + "2. 拆分关键事项并确认负责人。\n"
                + "3. 跟踪阻塞点并及时更新进度。";
    }

    private String projectStatusName(String status) {
        return switch (StrUtil.blankToDefault(status, "").toUpperCase(Locale.ROOT)) {
            case STATUS_NOT_STARTED -> "未开始";
            case STATUS_IN_PROGRESS -> "进行中";
            case STATUS_COMPLETED -> "已完成";
            case "PAUSED" -> "已暂停";
            case STATUS_ARCHIVED -> "已归档";
            default -> StrUtil.blankToDefault(status, "未设置");
        };
    }

    private boolean containsAny(String content, String... keywords) {
        if (StrUtil.isBlank(content)) {
            return false;
        }
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                return true;
            }
        }
        return false;
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

    private void ensureProjectPermission(Long projectId, String permission) {
        ensureProjectPermission(getProjectEntity(projectId), permission);
    }

    private void ensureProjectPermission(Project project, String permission) {
        if (!hasProjectPermission(project, permission)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_AUTH, "当前账号没有该项目操作权限");
        }
    }

    private void ensureProjectPermission(ProjectVO project, String permission) {
        if (project == null || !hasProjectPermission(project.getProjectId(), project.getOwnerId(), permission)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_AUTH, "No project permission");
        }
    }

    private boolean hasProjectPermission(Project project, String permission) {
        if (project == null) {
            return false;
        }
        return hasProjectPermission(project.getProjectId(), project.getOwnerId(), permission);
    }

    private boolean hasProjectPermission(Long projectId, Long ownerId, String permission) {
        if (projectId == null) {
            return false;
        }
        Long currentUserId = UserUtil.getUserId();
        if (isSystemAdmin() || Objects.equals(ownerId, currentUserId)) {
            return true;
        }
        ProjectVO.ProjectMemberVO member = findActiveMember(projectId, currentUserId);
        return member != null
                && member.getPermissions() != null
                && member.getPermissions().contains(permission);
    }

    private boolean isSystemAdmin() {
        try {
            Long currentUserId = UserUtil.getUserId();
            if (Objects.equals(currentUserId, UserUtil.getSuperUserId())) {
                return true;
            }
            return permissionService != null
                    && (permissionService.hasPermission("config")
                    || permissionService.hasPermission("user")
                    || permissionService.hasPermission("role"));
        } catch (Exception ignored) {
            return false;
        }
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

    private ProjectLane firstLaneExcluding(Long projectId, Long excludedLaneId) {
        ProjectLane lane = projectLaneMapper.selectOne(Wrappers.<ProjectLane>lambdaQuery()
                .eq(ProjectLane::getProjectId, projectId)
                .ne(ProjectLane::getLaneId, excludedLaneId)
                .orderByDesc(ProjectLane::getSystemFlag)
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
        return isCompletedLane(lane);
    }

    private boolean isCompletedLane(ProjectLane lane) {
        return lane != null && ("completed".equals(lane.getCode()) || "已完成".equals(lane.getName()));
    }

    private String laneToTaskStatus(ProjectLane lane) {
        if (lane == null) {
            return TASK_STATUS_TODO;
        }
        String code = StrUtil.blankToDefault(lane.getCode(), "").trim().toLowerCase(Locale.ROOT);
        String name = StrUtil.blankToDefault(lane.getName(), "").trim();
        if ("completed".equals(code) || "done".equals(code) || "已完成".equals(name)) {
            return TASK_STATUS_COMPLETED;
        }
        if ("in-progress".equals(code) || "in_progress".equals(code) || "doing".equals(code) || "进行中".equals(name)) {
            return TASK_STATUS_IN_PROGRESS;
        }
        return TASK_STATUS_TODO;
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
