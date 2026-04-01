package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.ScheduleAddBO;
import com.kakarote.ai_crm.entity.BO.ScheduleQueryBO;
import com.kakarote.ai_crm.entity.PO.Schedule;
import com.kakarote.ai_crm.entity.VO.ScheduleVO;
import com.kakarote.ai_crm.mapper.ScheduleMapper;
import com.kakarote.ai_crm.service.IScheduleService;
import com.kakarote.ai_crm.utils.UserUtil;
import cn.hutool.core.date.DateUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 日程服务实现
 */
@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule> implements IScheduleService {

    @Override
    public Long addSchedule(ScheduleAddBO scheduleAddBO) {
        Schedule schedule = BeanUtil.copyProperties(scheduleAddBO, Schedule.class);
        if (StrUtil.isEmpty(schedule.getType())) {
            schedule.setType("meeting");
        }
        save(schedule);
        return schedule.getScheduleId();
    }

    @Override
    public void deleteSchedule(Long scheduleId) {
        Schedule schedule = getById(scheduleId);
        if (ObjectUtil.isNull(schedule)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "日程不存在");
        }
        removeById(scheduleId);
    }

    @Override
    public List<ScheduleVO> getMySchedules(String filter) {
        Long userId = UserUtil.getUserId();
        Date today = DateUtil.beginOfDay(new Date());
        Date weekEnd = DateUtil.endOfWeek(new Date());

        List<ScheduleVO> schedules = baseMapper.getMySchedules(userId, filter, today, weekEnd);
        fillTypeName(schedules);
        return schedules;
    }

    @Override
    public BasePage<ScheduleVO> queryPageList(ScheduleQueryBO queryBO) {
        BasePage<ScheduleVO> page = queryBO.parse();
        baseMapper.queryPageList(page, queryBO);
        fillTypeName(page.getList());
        return page;
    }

    private void fillTypeName(List<ScheduleVO> schedules) {
        if (schedules != null) {
            schedules.forEach(s -> s.setTypeName(getTypeName(s.getType())));
        }
    }

    private String getTypeName(String type) {
        if (type == null) return "会议";
        return switch (type.toLowerCase()) {
            case "meeting" -> "会议";
            case "call" -> "电话";
            case "visit" -> "拜访";
            default -> type;
        };
    }
}
