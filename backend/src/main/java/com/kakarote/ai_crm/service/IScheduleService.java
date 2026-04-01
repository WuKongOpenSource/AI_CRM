package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.ScheduleAddBO;
import com.kakarote.ai_crm.entity.BO.ScheduleQueryBO;
import com.kakarote.ai_crm.entity.PO.Schedule;
import com.kakarote.ai_crm.entity.VO.ScheduleVO;

import java.util.List;

/**
 * 日程服务接口
 */
public interface IScheduleService extends IService<Schedule> {

    /**
     * 添加日程
     */
    Long addSchedule(ScheduleAddBO scheduleAddBO);

    /**
     * 删除日程
     */
    void deleteSchedule(Long scheduleId);

    /**
     * 查询我的日程
     */
    List<ScheduleVO> getMySchedules(String filter);

    /**
     * 分页查询日程
     */
    BasePage<ScheduleVO> queryPageList(ScheduleQueryBO queryBO);
}
