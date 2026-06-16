package com.kakarote.ai_crm.ai.tools;

import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.entity.BO.ProjectBO;
import com.kakarote.ai_crm.entity.VO.ProjectVO;
import com.kakarote.ai_crm.service.IProjectService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectToolsContextTest {

    private static final Long SESSION_ID = 9001L;
    private static final Long USER_ID = 1001L;
    private static final Long CUSTOMER_ID = 2001L;
    private static final Long PROJECT_ID = 11L;
    private static final Long TASK_ID = 22L;

    @Mock
    private IProjectService projectService;

    private ProjectTools projectTools;

    @BeforeEach
    void setUp() {
        projectTools = new ProjectTools();
        ReflectionTestUtils.setField(projectTools, "projectService", projectService);
        AiContextHolder.setContext(SESSION_ID, USER_ID, CUSTOMER_ID, null, null, null, PROJECT_ID, TASK_ID);
    }

    @AfterEach
    void tearDown() {
        AiContextHolder.clear();
        AiContextHolder.clearSession(SESSION_ID);
    }

    @Test
    void createProjectTask_shouldUseCurrentProjectWhenProjectIdOmitted() {
        ProjectVO project = projectWithTask(null);
        ProjectVO updated = projectWithTask(33L);
        when(projectService.getProject(PROJECT_ID)).thenReturn(project);
        when(projectService.addTask(any(), any(ProjectBO.TaskSave.class))).thenReturn(updated);

        String result = projectTools.createProjectTask(
                null, "整理方案", null, null, null, null, null, null, "medium", null);

        ArgumentCaptor<ProjectBO.TaskSave> captor = ArgumentCaptor.forClass(ProjectBO.TaskSave.class);
        verify(projectService).addTask(eq(PROJECT_ID), captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("整理方案");
        assertThat(result).contains("整理方案");
    }

    @Test
    void updateProjectTask_shouldUseCurrentProjectAndTaskWhenIdsOmitted() {
        ProjectVO project = projectWithTask(TASK_ID);
        when(projectService.getProject(PROJECT_ID)).thenReturn(project);
        when(projectService.updateTask(any(), any(ProjectBO.TaskSave.class))).thenAnswer(invocation -> {
            ProjectBO.TaskSave taskSave = invocation.getArgument(1, ProjectBO.TaskSave.class);
            ProjectVO updated = projectWithTask(TASK_ID);
            updated.getTasks().getFirst().setTitle(taskSave.getTitle());
            updated.getTasks().getFirst().setPriority(taskSave.getPriority());
            return updated;
        });

        String result = projectTools.updateProjectTask(
                null, null, "更新后的任务", null, null, null, null, null, null, "high", null);

        ArgumentCaptor<ProjectBO.TaskSave> captor = ArgumentCaptor.forClass(ProjectBO.TaskSave.class);
        verify(projectService).updateTask(eq(PROJECT_ID), captor.capture());
        assertThat(captor.getValue().getTaskId()).isEqualTo(TASK_ID);
        assertThat(captor.getValue().getTitle()).isEqualTo("更新后的任务");
        assertThat(result).contains("更新后的任务");
    }

    private ProjectVO projectWithTask(Long taskId) {
        ProjectVO project = new ProjectVO();
        project.setProjectId(PROJECT_ID);
        project.setName("测试项目");
        project.setTasks(new ArrayList<>());
        project.setLanes(new ArrayList<>());

        if (taskId != null) {
            ProjectVO.ProjectTaskVO task = new ProjectVO.ProjectTaskVO();
            task.setTaskId(taskId);
            task.setTitle(taskId.equals(TASK_ID) ? "原任务" : "整理方案");
            task.setPriority("MEDIUM");
            task.setParticipantIds(List.of());
            task.setParticipantNames(List.of());
            project.setTasks(new ArrayList<>(List.of(task)));
        }
        return project;
    }
}
