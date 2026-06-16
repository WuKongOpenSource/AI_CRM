package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kakarote.ai_crm.entity.BO.ScheduleQueryBO;
import com.kakarote.ai_crm.entity.PO.Schedule;
import com.kakarote.ai_crm.entity.VO.ScheduleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * 日程Mapper
 */
@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {

    /**
     * 分页查询日程
     */
    IPage<ScheduleVO> queryPageList(IPage<ScheduleVO> page, @Param("query") ScheduleQueryBO query);

    /**
     * 查询我的日程（带筛选）
     */
    List<ScheduleVO> getMySchedules(@Param("userId") Long userId,
                                     @Param("filter") String filter,
                                     @Param("today") Date today,
                                     @Param("weekEnd") Date weekEnd,
                                     @Param("allData") Boolean allData,
                                     @Param("userIds") List<Long> userIds);

    /**
     * 查询按ID忽略数据权限。
     */
    @InterceptorIgnore(dataPermission = "true")
    @Select("SELECT * FROM crm_schedule WHERE schedule_id = #{scheduleId}")
    Schedule selectByIdIgnoreDataPermission(@Param("scheduleId") Long scheduleId);

    /**
     * 查询按客户ID忽略数据权限。
     */
    @InterceptorIgnore(dataPermission = "true")
    @Select("SELECT * FROM crm_schedule WHERE customer_id = #{customerId}")
    List<Schedule> selectByCustomerIdIgnoreDataPermission(@Param("customerId") Long customerId);
}
