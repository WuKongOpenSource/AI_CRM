package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kakarote.ai_crm.entity.BO.TaskQueryBO;
import com.kakarote.ai_crm.entity.PO.Task;
import com.kakarote.ai_crm.entity.VO.TaskVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
     * 查询列表。
     */
    List<TaskVO> queryList(@Param("query") TaskQueryBO query);

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

    /**
     * 查询按ID忽略数据权限。
     */
    @InterceptorIgnore(dataPermission = "true")
    @Select("SELECT * FROM crm_task WHERE task_id = #{taskId}")
    Task selectByIdIgnoreDataPermission(@Param("taskId") Long taskId);

    /**
     * 查询按客户ID忽略数据权限。
     */
    @InterceptorIgnore(dataPermission = "true")
    @Select("SELECT * FROM crm_task WHERE customer_id = #{customerId}")
    List<Task> selectByCustomerIdIgnoreDataPermission(@Param("customerId") Long customerId);

    /**
     * 更新值优先级按ID。
     */
    @InterceptorIgnore(dataPermission = "true")
    @Update("""
        UPDATE crm_task
        SET value_priority_score = #{score},
            value_priority_tier = #{tier},
            value_priority_reason = #{reason},
            high_value = #{highValue}
        WHERE task_id = #{taskId}
        """)
    int updateValuePriorityById(@Param("taskId") Long taskId,
                                @Param("score") Integer score,
                                @Param("tier") String tier,
                                @Param("reason") String reason,
                                @Param("highValue") Boolean highValue);
}
