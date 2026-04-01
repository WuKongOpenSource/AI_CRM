package com.kakarote.ai_crm.ai.tools;

import com.kakarote.ai_crm.entity.BO.ScheduleAddBO;
import com.kakarote.ai_crm.entity.VO.ScheduleVO;
import com.kakarote.ai_crm.service.IScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 日程相关 AI Tool - 用于 Spring AI Function Calling
 */
@Slf4j
@Component
public class ScheduleTools {

    @Autowired
    private IScheduleService scheduleService;

    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Tool(description = "创建日程安排。当用户提到有具体时间点的安排时调用（如会议、电话、拜访等有明确开始时间的活动）。重要：调用此工具前，必须先完成所有信息收集——如果用户提到客户名称，先调用 queryCustomers 获取客户ID；如果提到联系人，先调用 queryContacts 获取联系人ID。然后将所有参数一次性传入此工具，只调用一次。用户确认信息后不要再次调用此工具。注意：只有提到具体时间（如10:00、下午3点）才创建日程，只有截止日期没有具体时间的应创建任务。")
    public String createSchedule(
            @ToolParam(description = "日程标题，必填") String title,
            @ToolParam(description = "开始时间，格式：yyyy-MM-dd HH:mm，必填") String startTime,
            @ToolParam(description = "结束时间，格式：yyyy-MM-dd HH:mm", required = false) String endTime,
            @ToolParam(description = "日程类型：meeting(会议)/call(电话)/visit(拜访)，默认meeting", required = false) String type,
            @ToolParam(description = "关联客户ID（数字）。如果用户提到客户名称，请先调用 queryCustomers 查询获取客户ID", required = false) String customerIdStr,
            @ToolParam(description = "关联联系人ID（数字）。如果用户提到联系人姓名，请先调用 queryContacts 查询获取联系人ID", required = false) String contactIdStr,
            @ToolParam(description = "地点", required = false) String location,
            @ToolParam(description = "描述", required = false) String description) {

        log.info("【Tool调用】createSchedule 被调用: title={}, startTime={}, endTime={}, type={}, customerIdStr={}",
            title, startTime, endTime, type, customerIdStr);

        try {
            // 解析开始时间
            if (startTime == null || startTime.isEmpty() || "null".equalsIgnoreCase(startTime)) {
                return "创建日程失败: 缺少开始时间参数";
            }

            ScheduleAddBO bo = new ScheduleAddBO();
            bo.setTitle(title);
            bo.setType(type != null && !type.isEmpty() && !"null".equalsIgnoreCase(type) ? type : "meeting");
            bo.setDescription(description);
            bo.setLocation(location);

            // 解析开始时间
            try {
                bo.setStartTime(dateTimeFormat.parse(startTime));
            } catch (Exception e) {
                return "创建日程失败: 开始时间格式无效，请使用 yyyy-MM-dd HH:mm 格式";
            }

            // 解析结束时间
            if (endTime != null && !endTime.isEmpty() && !"null".equalsIgnoreCase(endTime)) {
                try {
                    bo.setEndTime(dateTimeFormat.parse(endTime));
                } catch (Exception e) {
                    log.warn("结束时间格式无效: {}", endTime);
                }
            }

            // 解析客户ID
            if (customerIdStr != null && !customerIdStr.isEmpty() && !"null".equalsIgnoreCase(customerIdStr)) {
                try {
                    bo.setCustomerId(Long.parseLong(customerIdStr));
                } catch (NumberFormatException e) {
                    // 忽略无效的 customerId
                }
            }

            // 解析联系人ID
            if (contactIdStr != null && !contactIdStr.isEmpty() && !"null".equalsIgnoreCase(contactIdStr)) {
                try {
                    bo.setContactId(Long.parseLong(contactIdStr));
                } catch (NumberFormatException e) {
                    // 忽略无效的 contactId
                }
            }

            Long scheduleId = scheduleService.addSchedule(bo);

            log.info("【Tool调用】createSchedule 成功: scheduleId={}", scheduleId);

            String typeName = getTypeName(bo.getType());
            StringBuilder result = new StringBuilder();
            result.append(String.format("日程「%s」创建成功！日程ID: %d。", title, scheduleId));
            result.append(String.format("\n- 类型: %s", typeName));
            result.append(String.format("\n- 开始时间: %s", startTime));
            if (endTime != null && !endTime.isEmpty() && !"null".equalsIgnoreCase(endTime)) {
                result.append(String.format("\n- 结束时间: %s", endTime));
            }
            if (location != null && !location.isEmpty() && !"null".equalsIgnoreCase(location)) {
                result.append(String.format("\n- 地点: %s", location));
            }

            return result.toString();
        } catch (Exception e) {
            log.error("【Tool调用】createSchedule 失败: {}", e.getMessage(), e);
            return "创建日程失败: " + e.getMessage();
        }
    }

    @Tool(description = "查询我的日程安排。当用户要查看日程、今天的安排、本周日程时调用。")
    public String querySchedules(
            @ToolParam(description = "筛选条件：all(全部)/today(今天)/thisWeek(本周)，默认today", required = false) String filter) {

        try {
            String actualFilter = filter != null && !filter.isEmpty() && !"null".equalsIgnoreCase(filter) ? filter : "today";
            List<ScheduleVO> schedules = scheduleService.getMySchedules(actualFilter);

            if (schedules.isEmpty()) {
                return switch (actualFilter) {
                    case "today" -> "今天没有日程安排。";
                    case "thisWeek" -> "本周没有日程安排。";
                    default -> "当前没有日程安排。";
                };
            }

            StringBuilder sb = new StringBuilder();
            String title = switch (actualFilter) {
                case "today" -> "今日日程";
                case "thisWeek" -> "本周日程";
                default -> "日程安排";
            };
            sb.append(String.format("## %s（共%d项）\n\n", title, schedules.size()));

            for (ScheduleVO vo : schedules) {
                String typeName = vo.getTypeName() != null ? vo.getTypeName() : getTypeName(vo.getType());
                String time = vo.getStartTime() != null ? dateTimeFormat.format(vo.getStartTime()) : "未知";
                sb.append(String.format("- **%s** [%s] %s", vo.getTitle(), typeName, time));
                if (vo.getEndTime() != null) {
                    sb.append(String.format(" ~ %s", dateTimeFormat.format(vo.getEndTime())));
                }
                if (vo.getCustomerName() != null) {
                    sb.append(String.format("，客户: %s", vo.getCustomerName()));
                }
                if (vo.getLocation() != null && !vo.getLocation().isEmpty()) {
                    sb.append(String.format("，地点: %s", vo.getLocation()));
                }
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "查询日程失败: " + e.getMessage();
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
