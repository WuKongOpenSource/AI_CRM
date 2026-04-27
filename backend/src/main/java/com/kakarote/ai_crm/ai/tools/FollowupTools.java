package com.kakarote.ai_crm.ai.tools;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.ai.tools.support.AiCustomerMatcher;
import com.kakarote.ai_crm.ai.tools.support.AiToolPermission;
import com.kakarote.ai_crm.entity.BO.FollowUpAddBO;
import com.kakarote.ai_crm.entity.PO.Contact;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.VO.FollowUpVO;
import com.kakarote.ai_crm.service.IContactService;
import com.kakarote.ai_crm.service.ICustomerService;
import com.kakarote.ai_crm.service.IFollowUpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

/**
 * 跟进记录相关 AI Tool - 用于 Spring AI Function Calling
 */
@Slf4j
@Component
public class FollowupTools {

    private static final DateTimeFormatter OUTPUT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private IFollowUpService followUpService;

    @Autowired
    private AiCustomerMatcher aiCustomerMatcher;

    @Autowired
    private IContactService contactService;

    @Autowired
    private ICustomerService customerService;

    /**
     * 创建跟进。
     */
    @Tool(description = "创建跟进记录。当用户描述已经发生的沟通、拜访、电话、会议、邮件等事项时调用。直接传入客户名称和联系人姓名即可，无需先查询ID。如找不到联系人，仍继续创建跟进记录。涉及签约、回款、成交等关键节点时，可在内容前加【关键节点】标记。")
    @AiToolPermission(value = "followup:create", action = "创建跟进记录")
    public String createFollowUp(
            @ToolParam(description = "Optional CRM customer ID returned by createCustomer or confirmPendingCustomerCreation. For a newly created customer, pass this ID to avoid name rematching.", required = false) String customerIdStr,
            @ToolParam(description = "客户名称（公司名），必填") String customerName,
            @ToolParam(description = "跟进类型：call(电话)/meeting(会议)/email(邮件)/visit(拜访)，可根据内容推断，默认visit", required = false) String type,
            @ToolParam(description = "跟进内容，必填") String content,
            @ToolParam(description = "跟进时间，优先使用 yyyy-MM-dd HH:mm:ss；也兼容 yyyy-MM-dd HH:mm 或 yyyy-MM-dd，默认当前时间", required = false) String followTime,
            @ToolParam(description = "联系人姓名", required = false) String contactName,
            @ToolParam(description = "下次跟进时间，优先使用 yyyy-MM-dd HH:mm:ss；也兼容 yyyy-MM-dd HH:mm 或 yyyy-MM-dd", required = false) String nextFollowTime) {

        log.info("【Tool调用】createFollowUp 被调用: customerName={}, type={}, content={}, followTime={}, contactName={}, nextFollowTime={}",
            customerName, type, content, followTime, contactName, nextFollowTime);

        try {
            CustomerResolveResult customerResolve = resolveCustomerById(customerIdStr, "createFollowUp");
            if (customerResolve.errorMessage() != null) {
                return customerResolve.errorMessage();
            }
            if (customerResolve.customer() != null) {
                return createFollowUpForCustomer(customerResolve.customer(), type, content, followTime, contactName, nextFollowTime);
            }

            AiCustomerMatcher.CustomerMatchResult customerMatch = aiCustomerMatcher.match(customerName);
            if (customerMatch.isExistsNoAccess()) {
                return customerMatch.formatNoAccessMessage("创建跟进记录");
            }
            if (customerMatch.isAmbiguous()) {
                return "创建跟进失败: 客户名称「" + customerName + "」无法唯一匹配，可能是：" + customerMatch.formatCandidateNames() + "。请提供更完整的客户名称。";
            }
            if (!customerMatch.isMatched()) {
                return "创建跟进失败: 未找到名为「" + customerName + "」的客户，请确认客户名称是否正确";
            }

            Long customerId = customerMatch.getCustomer().getCustomerId();
            String matchedCompanyName = customerMatch.getCustomer().getCompanyName();

            Long contactId = null;
            if (StrUtil.isNotBlank(contactName) && !"null".equalsIgnoreCase(contactName)) {
                contactId = findContactIdByName(contactName, customerId);
                if (contactId == null) {
                    log.info("未找到联系人「{}」，将不关联联系人", contactName);
                }
            }

            FollowUpAddBO bo = new FollowUpAddBO();
            bo.setCustomerId(customerId);
            bo.setContactId(contactId);
            bo.setType(StrUtil.isNotBlank(type) && !"null".equalsIgnoreCase(type) ? type : "visit");
            bo.setContent(content);
            bo.setFollowTime(parseFollowUpTime(followTime, new Date()));
            bo.setNextFollowTime(parseOptionalTime(nextFollowTime));

            Long followUpId = followUpService.addFollowUp(bo);

            StringBuilder result = new StringBuilder();
            result.append("跟进记录创建成功！\n\n");
            result.append("- 公司名称: ").append(matchedCompanyName);
            result.append("\n- 类型: ").append(getTypeName(bo.getType()));
            result.append("\n- 内容: ").append(content);
            result.append("\n- 跟进时间: ").append(formatDateTime(bo.getFollowTime()));
            if (contactId != null) {
                result.append("\n- 联系人: ").append(contactName);
            } else if (StrUtil.isNotBlank(contactName) && !"null".equalsIgnoreCase(contactName)) {
                result.append("\n- 联系人: ").append(contactName).append("（未在系统中找到，未关联）");
            }
            if (bo.getNextFollowTime() != null) {
                result.append("\n- 下次跟进时间: ").append(formatDateTime(bo.getNextFollowTime()));
            }
            result.append("\n\n跟进ID: ").append(followUpId);
            return result.toString();
        } catch (Exception e) {
            log.error("【Tool调用】createFollowUp 失败: {}", e.getMessage(), e);
            return "创建跟进记录失败: " + e.getMessage();
        }
    }

    /**
     * 查询跟进UPS。
     */
    @Tool(description = "查询客户的跟进记录。当用户要查看某个客户的跟进历史、跟进记录时调用。直接传入客户名称即可，无需先查询ID。")
    @AiToolPermission(value = "followup:view", action = "查看跟进记录")
    public String queryFollowUps(
            @ToolParam(description = "客户名称（公司名），必填") String customerName) {

        try {
            if (StrUtil.isBlank(customerName) || "null".equalsIgnoreCase(customerName)) {
                return "查询跟进记录失败: 缺少客户名称";
            }

            AiCustomerMatcher.CustomerMatchResult customerMatch = aiCustomerMatcher.match(customerName);
            if (customerMatch.isExistsNoAccess()) {
                return customerMatch.formatNoAccessMessage("查看跟进记录");
            }
            if (customerMatch.isAmbiguous()) {
                return "查询跟进记录失败: 客户名称「" + customerName + "」无法唯一匹配，可能是：" + customerMatch.formatCandidateNames() + "。请提供更完整的客户名称。";
            }
            if (!customerMatch.isMatched()) {
                return "查询跟进记录失败: 未找到名为「" + customerName + "」的客户";
            }

            List<FollowUpVO> followUps = followUpService.queryByCustomer(customerMatch.getCustomer().getCustomerId());
            if (followUps.isEmpty()) {
                return "该客户暂无跟进记录。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("## %s 的跟进记录（共%d条）%n%n", customerMatch.getCustomer().getCompanyName(), followUps.size()));
            for (FollowUpVO vo : followUps) {
                String time = vo.getFollowTime() != null ? formatDateTime(vo.getFollowTime()) : "未知";
                sb.append(String.format("- **[%s]** %s - %s", getTypeName(vo.getType()), time, vo.getContent()));
                if (vo.getNextFollowTime() != null) {
                    sb.append(String.format("（下次跟进: %s）", formatDateTime(vo.getNextFollowTime())));
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "查询跟进记录失败: " + e.getMessage();
        }
    }

    /**
     * 查找联系人ID按名称。
     */
    private Long findContactIdByName(String name, Long customerId) {
        if (StrUtil.isBlank(name)) {
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
     * 创建跟进用于客户。
     */
    private String createFollowUpForCustomer(Customer customer,
                                             String type,
                                             String content,
                                             String followTime,
                                             String contactName,
                                             String nextFollowTime) {
        Long customerId = customer.getCustomerId();
        Long contactId = null;
        if (StrUtil.isNotBlank(contactName) && !"null".equalsIgnoreCase(contactName)) {
            contactId = findContactIdByName(contactName, customerId);
            if (contactId == null) {
                log.info("Contact not found for AI follow-up creation: contactName={}, customerId={}", contactName, customerId);
            }
        }

        FollowUpAddBO bo = new FollowUpAddBO();
        bo.setCustomerId(customerId);
        bo.setContactId(contactId);
        bo.setType(StrUtil.isNotBlank(type) && !"null".equalsIgnoreCase(type) ? type : "visit");
        bo.setContent(content);
        bo.setFollowTime(parseFollowUpTime(followTime, new Date()));
        bo.setNextFollowTime(parseOptionalTime(nextFollowTime));

        Long followUpId = followUpService.addFollowUp(bo);

        StringBuilder result = new StringBuilder();
        result.append("跟进记录创建成功。\n\n");
        result.append("- 公司名称: ").append(customer.getCompanyName());
        result.append("\n- customerId: ").append(customerId);
        result.append("\n- 类型: ").append(getTypeName(bo.getType()));
        result.append("\n- 内容: ").append(content);
        result.append("\n- 跟进时间: ").append(formatDateTime(bo.getFollowTime()));
        if (contactId != null) {
            result.append("\n- 联系人: ").append(contactName);
        } else if (StrUtil.isNotBlank(contactName) && !"null".equalsIgnoreCase(contactName)) {
            result.append("\n- 联系人: ").append(contactName).append("（未在系统中找到，未关联）");
        }
        if (bo.getNextFollowTime() != null) {
            result.append("\n- 下次跟进时间: ").append(formatDateTime(bo.getNextFollowTime()));
        }
        result.append("\n\n跟进ID: ").append(followUpId);
        return result.toString();
    }

    /**
     * 解析客户按ID。
     */
    private CustomerResolveResult resolveCustomerById(String customerIdStr, String actionName) {
        String normalizedCustomerId = normalizeOptionalText(customerIdStr);
        if (normalizedCustomerId == null) {
            return new CustomerResolveResult(null, null);
        }

        try {
            Long customerId = Long.parseLong(normalizedCustomerId);
            Customer customer = customerService.getById(customerId);
            if (customer == null || Integer.valueOf(0).equals(customer.getStatus())) {
                Customer existingCustomer = customerService.findCustomerByIdIgnoreDataPermission(customerId);
                if (existingCustomer != null && !Integer.valueOf(0).equals(existingCustomer.getStatus())) {
                    String message = AiCustomerMatcher.CustomerMatchResult
                        .existsNoAccess(normalizedCustomerId, existingCustomer)
                        .formatNoAccessMessage("创建跟进记录");
                    return new CustomerResolveResult(null, message);
                }
                return new CustomerResolveResult(null, "操作未执行：客户不存在或已停用，无法创建跟进记录。");
            }
            return new CustomerResolveResult(customer, null);
        } catch (NumberFormatException e) {
            return new CustomerResolveResult(null, "操作未执行：客户ID必须是数字。");
        }
    }

    /**
     * 标准化Optional文本。
     */
    private String normalizeOptionalText(String value) {
        String normalized = StrUtil.trim(value);
        if (StrUtil.isBlank(normalized) || "null".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    private record CustomerResolveResult(Customer customer, String errorMessage) {
    }

    /**
     * 解析跟进时间。
     */
    private Date parseFollowUpTime(String value, Date fallback) {
        if (StrUtil.isBlank(value) || "null".equalsIgnoreCase(value)) {
            return fallback;
        }

        LocalDateTime fallbackDateTime = toLocalDateTime(fallback);
        LocalDateTime parsed = parseLocalDateTime(value.trim(), fallbackDateTime.toLocalTime());
        if (parsed == null) {
            log.warn("跟进时间格式无效: {}", value);
            return fallback;
        }
        return toDate(parsed);
    }

    /**
     * 解析Optional时间。
     */
    private Date parseOptionalTime(String value) {
        if (StrUtil.isBlank(value) || "null".equalsIgnoreCase(value)) {
            return null;
        }

        LocalDateTime parsed = parseLocalDateTime(value.trim(), LocalTime.now());
        if (parsed == null) {
            log.warn("下次跟进时间格式无效: {}", value);
            return null;
        }
        return toDate(parsed);
    }

    /**
     * 解析本地日期时间。
     */
    private LocalDateTime parseLocalDateTime(String value, LocalTime defaultTime) {
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }

        try {
            return LocalDate.parse(value, DATE_ONLY_FORMATTER).atTime(defaultTime);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    /**
     * 转换为本地日期时间。
     */
    private LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    /**
     * 转换为日期。
     */
    private Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 格式化日期时间。
     */
    private String formatDateTime(Date date) {
        return OUTPUT_TIME_FORMATTER.format(toLocalDateTime(date));
    }

    /**
     * 获取类型名称。
     */
    private String getTypeName(String type) {
        if (type == null) {
            return "拜访";
        }
        return switch (type.toLowerCase()) {
            case "call" -> "电话";
            case "meeting" -> "会议";
            case "email" -> "邮件";
            case "visit" -> "拜访";
            default -> type;
        };
    }
}
