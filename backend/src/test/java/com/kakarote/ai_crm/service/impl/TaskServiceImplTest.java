package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.TaskQueryBO;
import com.kakarote.ai_crm.entity.VO.TaskVO;
import com.kakarote.ai_crm.mapper.TaskMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskServiceImplTest {

    @Test
    void queryPageListShouldReturnStatusCountsIgnoringCurrentStatusFilter() {
        TaskServiceImpl service = new TaskServiceImpl();
        TaskMapper taskMapper = mock(TaskMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", taskMapper);

        doAnswer(invocation -> {
            BasePage<TaskVO> page = invocation.getArgument(0);
            page.setRecords(List.of(task(1L, "PENDING")));
            page.setTotal(1);
            return page;
        }).when(taskMapper).queryPageList(any(BasePage.class), any(TaskQueryBO.class));
        when(taskMapper.queryList(any(TaskQueryBO.class))).thenReturn(List.of(
                task(1L, "PENDING"),
                task(2L, "IN_PROGRESS"),
                task(3L, "COMPLETED")
        ));

        TaskQueryBO queryBO = new TaskQueryBO();
        queryBO.setPage(1);
        queryBO.setLimit(10);
        queryBO.setStatus("PENDING");

        BasePage<TaskVO> page = service.queryPageList(queryBO);

        assertThat(page.getExtraData()).isInstanceOf(Map.class);
        Map<?, ?> extraData = (Map<?, ?>) page.getExtraData();
        assertThat(extraData.get("statusCounts")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> statusCounts = (Map<String, Object>) extraData.get("statusCounts");
        assertThat(statusCounts)
                .containsEntry("all", 3L)
                .containsEntry("PENDING", 1L)
                .containsEntry("IN_PROGRESS", 1L)
                .containsEntry("COMPLETED", 1L);
    }

    private static TaskVO task(Long taskId, String status) {
        TaskVO task = new TaskVO();
        task.setTaskId(taskId);
        task.setStatus(status);
        return task;
    }
}
