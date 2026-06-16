package com.kakarote.ai_crm.ai.tools;

import com.kakarote.ai_crm.ai.tools.support.AiToolCustomerResolver;
import com.kakarote.ai_crm.entity.BO.TaskAddBO;
import com.kakarote.ai_crm.service.ITaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskToolsTest {

    @Mock
    private ITaskService taskService;

    @Mock
    private AiToolCustomerResolver customerResolver;

    private TaskTools taskTools;

    @BeforeEach
    void setUp() {
        taskTools = new TaskTools();
        ReflectionTestUtils.setField(taskTools, "taskService", taskService);
        ReflectionTestUtils.setField(taskTools, "customerResolver", customerResolver);
    }

    @Test
    void createTask_shouldStopAndPromptWhenCustomerNotFound() {
        when(customerResolver.resolveForCreate(
            null, "科技创新有限公司", "关联该客户创建任务", "创建任务失败", "创建任务"))
            .thenReturn(new AiToolCustomerResolver.CustomerResolveResult(
                null, "创建任务失败: 系统中未找到名为「科技创新有限公司」的客户。请先创建该客户后再创建任务，或确认客户名称是否正确。"));

        String result = taskTools.createTask(
            null,
            "准备 Q4 扩容方案",
            null,
            "科技创新有限公司",
            "medium",
            "2026-04-28"
        );

        assertTrue(result.contains("创建任务失败"));
        assertTrue(result.contains("请先创建该客户"));
        verify(taskService, never()).addTask(any(TaskAddBO.class));
    }
}
