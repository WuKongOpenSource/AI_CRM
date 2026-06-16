package com.kakarote.ai_crm.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.BO.ProjectBO;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.Project;
import com.kakarote.ai_crm.entity.PO.ProjectAttachment;
import com.kakarote.ai_crm.entity.PO.ProjectLane;
import com.kakarote.ai_crm.entity.PO.ProjectSchedule;
import com.kakarote.ai_crm.entity.PO.ProjectTask;
import com.kakarote.ai_crm.entity.PO.ProjectTaskAttachment;
import com.kakarote.ai_crm.entity.VO.ProjectVO;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.ManageUserMapper;
import com.kakarote.ai_crm.mapper.ProjectAttachmentMapper;
import com.kakarote.ai_crm.mapper.ProjectLaneMapper;
import com.kakarote.ai_crm.mapper.ProjectMapper;
import com.kakarote.ai_crm.mapper.ProjectScheduleMapper;
import com.kakarote.ai_crm.mapper.ProjectTaskAttachmentMapper;
import com.kakarote.ai_crm.mapper.ProjectTaskMapper;
import com.kakarote.ai_crm.service.IKnowledgeService;
import com.kakarote.ai_crm.service.ISystemConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectLaneMapper projectLaneMapper;

    @Mock
    private ProjectTaskMapper projectTaskMapper;

    @Mock
    private ProjectTaskAttachmentMapper projectTaskAttachmentMapper;

    @Mock
    private ProjectAttachmentMapper projectAttachmentMapper;

    @Mock
    private ProjectScheduleMapper projectScheduleMapper;

    @Mock
    private ManageUserMapper manageUserMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private IKnowledgeService knowledgeService;

    @Mock
    private ISystemConfigService systemConfigService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ProjectServiceImpl projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectServiceImpl(
            projectMapper,
            projectLaneMapper,
            projectTaskMapper,
            projectTaskAttachmentMapper,
            projectAttachmentMapper,
            projectScheduleMapper,
            manageUserMapper,
            customerMapper,
            knowledgeService,
            systemConfigService,
            jdbcTemplate,
            new ObjectMapper()
        );
        setCurrentUser(1001L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void queryPageListExcludesArchivedProjectsByDefault() {
        when(projectMapper.queryPageList(any(), any())).thenReturn(new BasePage<>(1, 15));

        ProjectBO.Query query = new ProjectBO.Query();
        projectService.queryPageList(query);

        ArgumentCaptor<ProjectBO.Query> queryCaptor = ArgumentCaptor.forClass(ProjectBO.Query.class);
        verify(projectMapper).queryPageList(any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue().getStatus()).isNull();
        assertThat(queryCaptor.getValue().getIncludeArchived()).isFalse();
    }

    @Test
    void queryPageListAllowsArchivedProjectsWhenExplicitlyFiltered() {
        when(projectMapper.queryPageList(any(), any())).thenReturn(new BasePage<>(1, 15));

        ProjectBO.Query query = new ProjectBO.Query();
        query.setStatus("ARCHIVED");
        projectService.queryPageList(query);

        ArgumentCaptor<ProjectBO.Query> queryCaptor = ArgumentCaptor.forClass(ProjectBO.Query.class);
        verify(projectMapper).queryPageList(any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue().getStatus()).isEqualTo("ARCHIVED");
        assertThat(queryCaptor.getValue().getIncludeArchived()).isTrue();
    }

    @Test
    void createProjectDefaultsOwnerStatusAndSystemLanes() {
        when(manageUserMapper.getUserId(1001L)).thenReturn(activeUser(1001L));

        ProjectBO.Create createBO = new ProjectBO.Create();
        createBO.setName("Implementation");
        createBO.setDescription("Rollout work");

        ProjectVO result = projectService.createProject(createBO);

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).insert(projectCaptor.capture());
        Project savedProject = projectCaptor.getValue();
        assertThat(savedProject.getName()).isEqualTo("Implementation");
        assertThat(savedProject.getStatus()).isEqualTo("NOT_STARTED");
        assertThat(savedProject.getOwnerId()).isEqualTo(1001L);
        assertThat(savedProject.getCreateUserId()).isEqualTo(1001L);
        assertThat(savedProject.getUpdateUserId()).isEqualTo(1001L);
        assertThat(result.getName()).isEqualTo("Implementation");

        ArgumentCaptor<ProjectLane> laneCaptor = ArgumentCaptor.forClass(ProjectLane.class);
        verify(projectLaneMapper, times(3)).insert(laneCaptor.capture());
        List<String> laneNames = laneCaptor.getAllValues().stream().map(ProjectLane::getName).toList();
        assertThat(laneNames).containsExactly("未开始", "进行中", "已完成");
        assertThat(laneCaptor.getAllValues()).allSatisfy(lane -> {
            assertThat(lane.getProjectId()).isEqualTo(savedProject.getProjectId());
            assertThat(lane.getSystemFlag()).isTrue();
        });
    }

    @Test
    void addTaskAttachmentStoresFileMetadataAndReturnsProjectDetail() {
        Project project = new Project();
        project.setProjectId(2001L);
        project.setName("Implementation");
        project.setCustomerId(5001L);
        ProjectTask task = new ProjectTask();
        task.setTaskId(3001L);
        task.setProjectId(2001L);
        task.setTitle("Prepare proposal");
        when(projectTaskMapper.selectById(3001L)).thenReturn(task);
        when(projectMapper.selectById(2001L)).thenReturn(project);
        when(projectMapper.getProjectById(2001L)).thenReturn(projectDetail(2001L));
        when(projectLaneMapper.selectList(any())).thenReturn(List.of());
        when(projectTaskMapper.selectProjectTasks(2001L, null)).thenReturn(List.of(taskVO(3001L)));
        when(projectTaskAttachmentMapper.selectList(any())).thenReturn(List.of());

        ProjectBO.TaskAttachmentSave attachment = new ProjectBO.TaskAttachmentSave();
        attachment.setFileName("proposal.pdf");
        attachment.setFilePath("project/2001/proposal.pdf");
        attachment.setFileSize(1234L);
        attachment.setMimeType("application/pdf");

        ProjectVO result = projectService.addTaskAttachment(2001L, 3001L, attachment);

        ArgumentCaptor<ProjectTaskAttachment> captor = ArgumentCaptor.forClass(ProjectTaskAttachment.class);
        verify(projectTaskAttachmentMapper).insert(captor.capture());
        ProjectTaskAttachment saved = captor.getValue();
        assertThat(saved.getProjectId()).isEqualTo(2001L);
        assertThat(saved.getTaskId()).isEqualTo(3001L);
        assertThat(saved.getName()).isEqualTo("proposal.pdf");
        assertThat(saved.getFilePath()).isEqualTo("project/2001/proposal.pdf");
        assertThat(saved.getFileSize()).isEqualTo(1234L);
        assertThat(saved.getMimeType()).isEqualTo("application/pdf");
        assertThat(saved.getCreateUserId()).isEqualTo(1001L);
        assertThat(result.getProjectId()).isEqualTo(2001L);
        verify(knowledgeService).archiveExistingStandaloneFile(
                eq("proposal.pdf"),
                eq("project/2001/proposal.pdf"),
                eq(1234L),
                eq("application/pdf"),
                eq(5001L),
                eq("项目任务附件自动同步。项目：Implementation；任务：Prepare proposal")
        );
    }

    @Test
    void addProjectAttachmentStoresMetadataAndReturnsProjectDetail() {
        when(projectMapper.selectById(2001L)).thenReturn(new Project());
        when(projectMapper.getProjectById(2001L)).thenReturn(projectDetail(2001L));
        when(projectLaneMapper.selectList(any())).thenReturn(List.of());
        when(projectTaskMapper.selectProjectTasks(2001L, null)).thenReturn(List.of());
        when(projectAttachmentMapper.selectList(any())).thenReturn(List.of());
        when(projectScheduleMapper.selectList(any())).thenReturn(List.of());

        ProjectBO.ProjectAttachmentSave attachment = new ProjectBO.ProjectAttachmentSave();
        attachment.setName("charter.docx");
        attachment.setFileUrl("project/2001/charter.docx");

        ProjectVO result = projectService.addProjectAttachment(2001L, attachment);

        ArgumentCaptor<ProjectAttachment> captor = ArgumentCaptor.forClass(ProjectAttachment.class);
        verify(projectAttachmentMapper).insert(captor.capture());
        ProjectAttachment saved = captor.getValue();
        assertThat(saved.getProjectId()).isEqualTo(2001L);
        assertThat(saved.getName()).isEqualTo("charter.docx");
        assertThat(saved.getFileUrl()).isEqualTo("project/2001/charter.docx");
        assertThat(saved.getCreateUserId()).isEqualTo(1001L);
        assertThat(result.getProjectId()).isEqualTo(2001L);
    }

    private ManagerUser activeUser(Long userId) {
        ManagerUser user = new ManagerUser();
        user.setUserId(userId);
        user.setUsername("user" + userId);
        user.setRealname("User " + userId);
        user.setStatus(1);
        return user;
    }

    private ProjectVO projectDetail(Long projectId) {
        ProjectVO project = new ProjectVO();
        project.setProjectId(projectId);
        project.setName("Implementation");
        project.setStatus("NOT_STARTED");
        return project;
    }

    private ProjectVO.ProjectTaskVO taskVO(Long taskId) {
        ProjectVO.ProjectTaskVO task = new ProjectVO.ProjectTaskVO();
        task.setTaskId(taskId);
        task.setProjectId(2001L);
        task.setTitle("Prepare proposal");
        return task;
    }

    private void setCurrentUser(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUser(activeUser(userId));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities())
        );
    }
}
