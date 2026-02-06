package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.TaskAddBO;
import com.kakarote.ai_crm.entity.BO.TaskQueryBO;
import com.kakarote.ai_crm.entity.BO.TaskUpdateBO;
import com.kakarote.ai_crm.entity.PO.Task;
import com.kakarote.ai_crm.entity.VO.TaskVO;
import com.kakarote.ai_crm.mapper.TaskMapper;
import com.kakarote.ai_crm.service.ITaskService;
import com.kakarote.ai_crm.utils.UserUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 任务服务实现
 */
@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements ITaskService {

    @Override
    public Long addTask(TaskAddBO taskAddBO) {
        Task task = BeanUtil.copyProperties(taskAddBO, Task.class);
        if (StrUtil.isEmpty(task.getStatus())) {
            task.setStatus("pending");
        }
        if (StrUtil.isEmpty(task.getPriority())) {
            task.setPriority("medium");
        }
        if (task.getAssignedTo() == null) {
            task.setAssignedTo(UserUtil.getUserId());
        }
        if (task.getGeneratedByAi() == null) {
            task.setGeneratedByAi(0);
        }
        save(task);
        return task.getTaskId();
    }

    @Override
    public void updateTask(TaskUpdateBO taskUpdateBO) {
        Task task = getById(taskUpdateBO.getTaskId());
        if (ObjectUtil.isNull(task)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "任务不存在");
        }
        BeanUtil.copyProperties(taskUpdateBO, task, "taskId", "createUserId", "createTime");
        updateById(task);
    }

    @Override
    public void deleteTask(Long taskId) {
        Task task = getById(taskId);
        if (ObjectUtil.isNull(task)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "任务不存在");
        }
        removeById(taskId);
    }

    @Override
    public BasePage<TaskVO> queryPageList(TaskQueryBO queryBO) {
        BasePage<TaskVO> page = queryBO.parse();
        baseMapper.queryPageList(page, queryBO);
        fillTaskNames(page.getList());
        return page;
    }

    @Override
    public void updateStatus(Long taskId, String status) {
        Task task = getById(taskId);
        if (ObjectUtil.isNull(task)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "任务不存在");
        }
        task.setStatus(status);
        if ("completed".equals(status)) {
            task.setCompletedTime(new Date());
        }
        updateById(task);
    }

    @Override
    public List<TaskVO> getMyTasks(String filter) {
        Long userId = UserUtil.getUserId();
        Date today = DateUtil.beginOfDay(new Date());
        Date weekEnd = DateUtil.endOfWeek(new Date());

        // 使用 mapper 查询以获取关联的负责人姓名
        List<TaskVO> tasks = baseMapper.getMyTasksFiltered(userId, filter, today, weekEnd);
        fillTaskNames(tasks);
        return tasks;
    }

    /**
     * 获取优先级中文名称（忽略大小写）
     */
    private String getPriorityName(String priority) {
        if (priority == null) return null;
        return switch (priority.toLowerCase()) {
            case "high" -> "高";
            case "medium" -> "中";
            case "low" -> "低";
            default -> priority;
        };
    }

    /**
     * 获取状态中文名称（忽略大小写）
     */
    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status.toLowerCase()) {
            case "pending" -> "待处理";
            case "in_progress" -> "进行中";
            case "completed" -> "已完成";
            default -> status;
        };
    }

    /**
     * 填充任务的中文名称字段，并标准化 priority/status 为大写
     */
    private void fillTaskNames(TaskVO task) {
        // 标准化为大写（前端期望大写格式）
        if (task.getPriority() != null) {
            task.setPriority(task.getPriority().toUpperCase());
        }
        if (task.getStatus() != null) {
            task.setStatus(task.getStatus().toUpperCase());
        }
        task.setPriorityName(getPriorityName(task.getPriority()));
        task.setStatusName(getStatusName(task.getStatus()));
    }

    /**
     * 批量填充任务列表的中文名称字段
     */
    private void fillTaskNames(List<TaskVO> tasks) {
        if (tasks != null) {
            tasks.forEach(this::fillTaskNames);
        }
    }
}
