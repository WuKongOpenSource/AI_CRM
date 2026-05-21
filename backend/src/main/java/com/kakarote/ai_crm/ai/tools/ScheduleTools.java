package com.kakarote.ai_crm.ai.tools;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.ai.tools.support.AiToolCustomerResolver;
import com.kakarote.ai_crm.ai.tools.support.AiToolPermission;
import com.kakarote.ai_crm.entity.BO.ScheduleAddBO;
import com.kakarote.ai_crm.entity.PO.Contact;
import com.kakarote.ai_crm.entity.VO.ScheduleVO;
import com.kakarote.ai_crm.service.IContactService;
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

    @Autowired
    private IContactService contactService;

    @Autowired
    private AiToolCustomerResolver customerResolver;

    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    /**
     * 创建日程。
     */
    @Tool(description = "创建日程安排。仅当用户提到具体时间点时调用。没有具体执行时间点、只有截止日期的应使用 createTask。客户解析优先级：显式customerIdStr > 显式客户名称 > 当前客户对话绑定客户。当前客户对话中，如果用户只说“这个客户/当前客户/他们”等，不要把代词作为customerName，留空即可让工具默认关联当前客户。如果传入客户名称但系统中不存在该客户，工具会中止创建并提示先创建客户。")
    @AiToolPermission(value = "schedule:create", action = "创建日程")
    public String createSchedule(
            @ToolParam(description = "Optional CRM customer ID returned by createCustomer or confirmPendingCustomerCreation. For a newly created customer, pass this ID to avoid name rematching.", required = false) String customerIdStr,
            @ToolParam(description = "日程标题，必填") String title,
            @ToolParam(description = "开始时间，格式：yyyy-MM-dd HH:mm，必填") String startTime,
            @ToolParam(description = "结束时间，格式：yyyy-MM-dd HH:mm", required = false) String endTime,
            @ToolParam(description = "日程类型：meeting(会议)/call(电话)/visit(拜访)，默认meeting", required = false) String type,
            @ToolParam(description = "关联客户名称（公司名）", required = false) String customerName,
            @ToolParam(description = "关联联系人姓名", required = false) String contactName,
            @ToolParam(description = "地点", required = false) String location,
            @ToolParam(description = "描述", required = false) String description) {

        log.info("【Tool调用】createSchedule 被调用: title={}, startTime={}, endTime={}, type={}, customerName={}, contactName={}",
            title, startTime, endTime, type, customerName, contactName);

        try {
            if (!hasTextValue(startTime)) {
                return "创建日程失败: 缺少开始时间参数";
            }

            ScheduleAddBO bo = new ScheduleAddBO();
            bo.setTitle(title);
            bo.setType(hasTextValue(type) ? type : "meeting");
            bo.setDescription(description);
            bo.setLocation(location);

            try {
                bo.setStartTime(dateTimeFormat.parse(startTime));
            } catch (Exception e) {
                return "创建日程失败: 开始时间格式无效，请使用 yyyy-MM-dd HH:mm 格式";
            }

            if (hasTextValue(endTime)) {
                try {
                    bo.setEndTime(dateTimeFormat.parse(endTime));
                } catch (Exception e) {
                    log.warn("结束时间格式无效: {}", endTime);
                }
            }

            String matchedCompanyName = null;
            Long customerId = null;
            AiToolCustomerResolver.CustomerResolveResult customerResolve = customerResolver.resolveForCreate(
                customerIdStr, customerName, "关联该客户创建日程", "创建日程失败", "创建日程");
            if (customerResolve.errorMessage() != null) {
                return customerResolve.errorMessage();
            }
            if (customerResolve.customer() != null) {
                customerId = customerResolve.customer().getCustomerId();
                matchedCompanyName = customerResolve.customer().getCompanyName();
                bo.setCustomerId(customerId);
            }

            if (hasTextValue(contactName)) {
                Long contactId = findContactIdByName(contactName, customerId);
                if (contactId == null) {
                    log.info("未找到联系人「{}」，将不关联联系人", contactName);
                }
                bo.setContactId(contactId);
            }

            Long scheduleId = scheduleService.addSchedule(bo);

            StringBuilder result = new StringBuilder();
            result.append(String.format("日程「%s」创建成功！日程ID: %d。", title, scheduleId));
            result.append(String.format("\n- 类型: %s", getTypeName(bo.getType())));
            result.append(String.format("\n- 开始时间: %s", startTime));
            if (hasTextValue(endTime)) {
                result.append(String.format("\n- 结束时间: %s", endTime));
            }
            if (matchedCompanyName != null) {
                result.append(String.format("\n- 公司名称: %s", matchedCompanyName));
            }
            if (customerId != null) {
                result.append("\n- customerId: ").append(customerId);
            }
            if (hasTextValue(location)) {
                result.append(String.format("\n- 地点: %s", location));
            }

            return result.toString();
        } catch (Exception e) {
            log.error("【Tool调用】createSchedule 失败: {}", e.getMessage(), e);
            return "创建日程失败: " + e.getMessage();
        }
    }

    /**
     * 查询日程。
     */
    @Tool(description = "查询我的日程安排。当用户要查看日程、今天的安排、本周日程时调用。")
    @AiToolPermission(value = "schedule:view", action = "查看日程")
    public String querySchedules(
            @ToolParam(description = "筛选条件：all(全部)/today(今天)/thisWeek(本周)，默认today", required = false) String filter) {

        try {
            String actualFilter = StrUtil.isNotBlank(filter) && !"null".equalsIgnoreCase(filter) ? filter : "today";
            List<ScheduleVO> schedules = scheduleService.getMySchedules(actualFilter);

            if (schedules.isEmpty()) {
                return switch (actualFilter) {
                    case "today" -> "今天没有日程安排。";
                    case "thisWeek" -> "本周没有日程安排。";
                    default -> "当前没有日程安排。";
                };
            }

            String title = switch (actualFilter) {
                case "today" -> "今日日程";
                case "thisWeek" -> "本周日程";
                default -> "日程安排";
            };

            StringBuilder sb = new StringBuilder();
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
                if (StrUtil.isNotBlank(vo.getLocation())) {
                    sb.append(String.format("，地点: %s", vo.getLocation()));
                }
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "查询日程失败: " + e.getMessage();
        }
    }

    /**
     * 查找联系人ID按名称。
     */
    private Long findContactIdByName(String name, Long customerId) {
        if (StrUtil.isBlank(name) || customerId == null) {
            return null;
        }

        LambdaQueryWrapper<Contact> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contact::getName, name);
        wrapper.eq(Contact::getCustomerId, customerId);
        wrapper.eq(Contact::getStatus, 1);
        wrapper.last("LIMIT 1");
        Contact contact = contactService.getOne(wrapper);
        if (contact != null) {
            return contact.getContactId();
        }

        wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Contact::getName, name);
        wrapper.eq(Contact::getCustomerId, customerId);
        wrapper.eq(Contact::getStatus, 1);
        wrapper.last("LIMIT 1");
        contact = contactService.getOne(wrapper);
        if (contact != null) {
            return contact.getContactId();
        }

        return null;
    }

    /**
     * 判断是否存在文本值。
     */
    private boolean hasTextValue(String value) {
        return StrUtil.isNotBlank(value) && !"null".equalsIgnoreCase(StrUtil.trim(value));
    }

    /**
     * 获取类型名称。
     */
    private String getTypeName(String type) {
        if (type == null) {
            return "会议";
        }
        return switch (type.toLowerCase()) {
            case "meeting" -> "会议";
            case "call" -> "电话";
            case "visit" -> "拜访";
            default -> type;
        };
    }
}
