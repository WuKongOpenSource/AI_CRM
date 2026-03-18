package com.kakarote.ai_crm.ai.tools;

import com.kakarote.ai_crm.entity.BO.FollowUpAddBO;
import com.kakarote.ai_crm.entity.VO.FollowUpVO;
import com.kakarote.ai_crm.service.IFollowUpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 跟进记录相关 AI Tool - 用于 Spring AI Function Calling
 */
@Slf4j
@Component
public class FollowupTools {

    @Autowired
    private IFollowUpService followUpService;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Tool(description = "创建跟进记录。当用户要新建、添加、记录跟进时调用。重要：调用此工具前，如果用户提到客户名称，先调用 queryCustomers 获取客户ID；如果提到联系人，先调用 queryContacts 查询联系人ID。如果联系人查询不到，仍然继续创建跟进记录（联系人ID为可选参数，不传即可）。然后将所有参数一次性传入此工具，只调用一次。用户确认信息后不要再次调用此工具。根据跟进内容推断类型：拜访=visit，电话/打电话=call，邮件=email，会议/开会=meeting，其他=visit。")
    public String createFollowUp(
            @ToolParam(description = "关联客户ID（数字），必填。如果用户提到客户名称，请先调用 queryCustomers 查询获取客户ID") String customerIdStr,
            @ToolParam(description = "跟进类型：call(电话)/meeting(会议)/email(邮件)/visit(拜访)，根据内容推断，默认visit") String type,
            @ToolParam(description = "跟进内容，必填") String content,
            @ToolParam(description = "跟进时间，格式：yyyy-MM-dd，默认今天") String followTime,
            @ToolParam(description = "关联联系人ID（数字）。如果用户提到联系人姓名，请先调用 queryContacts 查询获取联系人ID", required = false) String contactIdStr,
            @ToolParam(description = "下次跟进时间，格式：yyyy-MM-dd", required = false) String nextFollowTime) {

        log.info("【Tool调用】createFollowUp 被调用: customerIdStr={}, type={}, content={}, followTime={}, contactIdStr={}, nextFollowTime={}",
            customerIdStr, type, content, followTime, contactIdStr, nextFollowTime);

        try {
            // 解析客户ID
            Long customerId = null;
            if (customerIdStr != null && !customerIdStr.isEmpty() && !"null".equalsIgnoreCase(customerIdStr)) {
                try {
                    customerId = Long.parseLong(customerIdStr);
                } catch (NumberFormatException e) {
                    // 忽略无效的 customerId
                }
            }
            if (customerId == null) {
                return "创建跟进失败: 缺少客户ID参数，请先查询客户获取ID";
            }

            // 解析联系人ID
            Long contactId = null;
            if (contactIdStr != null && !contactIdStr.isEmpty() && !"null".equalsIgnoreCase(contactIdStr)) {
                try {
                    contactId = Long.parseLong(contactIdStr);
                } catch (NumberFormatException e) {
                    // 忽略无效的 contactId
                }
            }

            FollowUpAddBO bo = new FollowUpAddBO();
            bo.setCustomerId(customerId);
            bo.setContactId(contactId);
            bo.setType(type != null && !type.isEmpty() && !"null".equalsIgnoreCase(type) ? type : "visit");
            bo.setContent(content);

            // 解析跟进时间
            if (followTime != null && !followTime.isEmpty() && !"null".equalsIgnoreCase(followTime)) {
                try {
                    bo.setFollowTime(dateFormat.parse(followTime));
                } catch (Exception e) {
                    bo.setFollowTime(new Date());
                }
            } else {
                bo.setFollowTime(new Date());
            }

            // 解析下次跟进时间
            if (nextFollowTime != null && !nextFollowTime.isEmpty() && !"null".equalsIgnoreCase(nextFollowTime)) {
                try {
                    bo.setNextFollowTime(dateFormat.parse(nextFollowTime));
                } catch (Exception e) {
                    log.warn("下次跟进时间格式无效: {}", nextFollowTime);
                }
            }

            Long followUpId = followUpService.addFollowUp(bo);

            log.info("【Tool调用】createFollowUp 成功: followUpId={}", followUpId);

            String typeName = getTypeName(bo.getType());
            StringBuilder result = new StringBuilder();
            result.append(String.format("跟进记录创建成功！跟进ID: %d。", followUpId));
            result.append(String.format("\n- 类型: %s", typeName));
            result.append(String.format("\n- 内容: %s", content));
            result.append(String.format("\n- 跟进时间: %s", followTime != null && !followTime.isEmpty() && !"null".equalsIgnoreCase(followTime) ? followTime : dateFormat.format(new Date())));
            if (nextFollowTime != null && !nextFollowTime.isEmpty() && !"null".equalsIgnoreCase(nextFollowTime)) {
                result.append(String.format("\n- 下次跟进时间: %s", nextFollowTime));
            }

            return result.toString();
        } catch (Exception e) {
            log.error("【Tool调用】createFollowUp 失败: {}", e.getMessage(), e);
            return "创建跟进记录失败: " + e.getMessage();
        }
    }

    @Tool(description = "查询客户的跟进记录。当用户要查看某个客户的跟进历史、跟进记录时调用。如果用户提到客户名称，请先调用 queryCustomers 获取客户ID。")
    public String queryFollowUps(
            @ToolParam(description = "客户ID（数字），必填。如果用户提到客户名称，请先调用 queryCustomers 查询获取客户ID") String customerIdStr) {

        try {
            if (customerIdStr == null || customerIdStr.isEmpty() || "null".equalsIgnoreCase(customerIdStr)) {
                return "查询跟进记录失败: 缺少客户ID参数";
            }

            Long customerId;
            try {
                customerId = Long.parseLong(customerIdStr);
            } catch (NumberFormatException e) {
                return "查询跟进记录失败: 客户ID格式无效";
            }

            List<FollowUpVO> followUps = followUpService.queryByCustomer(customerId);

            if (followUps.isEmpty()) {
                return "该客户暂无跟进记录。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("## 跟进记录（共%d条）\n\n", followUps.size()));

            for (FollowUpVO vo : followUps) {
                String typeName = getTypeName(vo.getType());
                String time = vo.getFollowTime() != null ? dateFormat.format(vo.getFollowTime()) : "未知";
                sb.append(String.format("- **[%s]** %s - %s", typeName, time, vo.getContent()));
                if (vo.getNextFollowTime() != null) {
                    sb.append(String.format("（下次跟进: %s）", dateFormat.format(vo.getNextFollowTime())));
                }
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "查询跟进记录失败: " + e.getMessage();
        }
    }

    private String getTypeName(String type) {
        if (type == null) return "拜访";
        return switch (type.toLowerCase()) {
            case "call" -> "电话";
            case "meeting" -> "会议";
            case "email" -> "邮件";
            case "visit" -> "拜访";
            default -> type;
        };
    }
}
