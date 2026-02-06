package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.TaskAddBO;
import com.kakarote.ai_crm.entity.BO.TaskQueryBO;
import com.kakarote.ai_crm.entity.BO.TaskUpdateBO;
import com.kakarote.ai_crm.entity.PO.Task;
import com.kakarote.ai_crm.entity.VO.TaskVO;

import java.util.List;

/**
 * 任务服务接口
 */
public interface ITaskService extends IService<Task> {

    /**
     * 添加任务
     */
    Long addTask(TaskAddBO taskAddBO);

    /**
     * 更新任务
     */
    void updateTask(TaskUpdateBO taskUpdateBO);

    /**
     * 删除任务
     */
    void deleteTask(Long taskId);

    /**
     * 分页查询任务列表
     */
    BasePage<TaskVO> queryPageList(TaskQueryBO queryBO);

    /**
     * 更新任务状态
     */
    void updateStatus(Long taskId, String status);

    /**
     * 查询我的任务
     */
    List<TaskVO> getMyTasks(String filter);
}
