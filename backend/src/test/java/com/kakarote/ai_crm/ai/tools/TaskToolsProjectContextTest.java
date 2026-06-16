package com.kakarote.ai_crm.ai.tools;

import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.ai.tools.support.AiToolCustomerResolver;
import com.kakarote.ai_crm.entity.BO.ProjectBO;
import com.kakarote.ai_crm.entity.VO.ProjectVO;
import com.kakarote.ai_crm.service.IProjectService;
import com.kakarote.ai_crm.service.ITaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskToolsProjectContextTest {

    private static final Long SESSION_ID = 9002L;
    private static final Long USER_ID = 1001L;
    private static final Long CUSTOMER_ID = 2001L;
    private static final Long PROJECT_ID = 11L;

    @Mock
    private ITaskService taskService;

    @Mock
    private IProjectService projectService;

    @Mock
    private AiToolCustomerResolver customerResolver;

    private TaskTools taskTools;

    @BeforeEach
    void setUp() {
        taskTools = new TaskTools();
        ReflectionTestUtils.setField(taskTools, "taskService", taskService);
        ReflectionTestUtils.setField(taskTools, "projectService", projectService);
        ReflectionTestUtils.setField(taskTools, "customerResolver", customerResolver);
        AiContextHolder.setContext(SESSION_ID, USER_ID, CUSTOMER_ID, null, null, null, PROJECT_ID, null);
    }

    @AfterEach
    void tearDown() {
        AiContextHolder.clear();
        AiContextHolder.clearSession(SESSION_ID);
    }

    @Test
    void createTask_shouldCreateProjectTaskWhenProjectContextExists() {
        ProjectVO project = new ProjectVO();
        project.setProjectId(PROJECT_ID);
        project.setName("测试项目");
        project.setTasks(new ArrayList<>());
        project.setLanes(new ArrayList<>());
        when(projectService.addTask(any(), any(ProjectBO.TaskSave.class))).thenReturn(project);

        String result = taskTools.createTask(null, "测试CRM", "项目内任务", null, "medium", null);

        ArgumentCaptor<ProjectBO.TaskSave> captor = ArgumentCaptor.forClass(ProjectBO.TaskSave.class);
        verify(projectService).addTask(eq(PROJECT_ID), captor.capture());
        verify(taskService, never()).addTask(any());
        assertThat(captor.getValue().getTitle()).isEqualTo("测试CRM");
        assertThat(captor.getValue().getDescription()).isEqualTo("项目内任务");
        assertThat(result).contains("项目任务创建成功").contains("测试项目");
    }
}
