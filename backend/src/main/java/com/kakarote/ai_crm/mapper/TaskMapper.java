package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kakarote.ai_crm.entity.BO.TaskQueryBO;
import com.kakarote.ai_crm.entity.PO.Task;
import com.kakarote.ai_crm.entity.VO.TaskVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 任务Mapper
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    /**
     * 分页查询任务
     */
    IPage<TaskVO> queryPageList(IPage<TaskVO> page, @Param("query") TaskQueryBO query);

    /**
     * 查询用户待办任务
     */
    List<TaskVO> getMyPendingTasks(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 统计待处理任务数
     */
    Long countPending(@Param("userId") Long userId);

    /**
     * 查询我的任务（带筛选条件和负责人信息）
     */
    List<TaskVO> getMyTasksFiltered(@Param("userId") Long userId,
                                     @Param("filter") String filter,
                                     @Param("today") Date today,
                                     @Param("weekEnd") Date weekEnd);
}
