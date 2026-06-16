package com.kakarote.ai_crm.ai.tools;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.ai.tools.support.AiToolPermission;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.MailDraftCreateBO;
import com.kakarote.ai_crm.entity.BO.MailMessageQueryBO;
import com.kakarote.ai_crm.entity.VO.MailDraftVO;
import com.kakarote.ai_crm.entity.VO.MailMessageVO;
import com.kakarote.ai_crm.service.IMailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.List;

@Slf4j
@Component
public class MailTools {

    @Autowired
    private IMailService mailService;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Tool(description = "搜索已同步到 CRM 的邮件。当用户询问邮件往来、客户邮件、邮箱内容、邮件主题或发件人时调用。只能读取已经授权并同步到系统内的邮件，不能直接实时访问外部邮箱。")
    @AiToolPermission(value = "mail:view", action = "查看邮件")
    public String searchSyncedMails(
            @ToolParam(description = "搜索关键词，可匹配主题、发件人、收件人和正文", required = false) String keyword,
            @ToolParam(description = "客户ID，数字类型；客户对话模式下可留空，默认使用当前绑定客户", required = false) String customerIdStr,
            @ToolParam(description = "最多返回数量，默认5，最大10", required = false) String limitStr) {
        try {
            MailMessageQueryBO queryBO = new MailMessageQueryBO();
            queryBO.setKeyword(StrUtil.trimToNull(keyword));
            queryBO.setLimit(resolveLimit(limitStr));
            queryBO.setPage(1);
            Long customerId = parseLong(customerIdStr);
            if (customerId == null) {
                customerId = AiContextHolder.getCurrentCustomerId();
            }
            queryBO.setCustomerId(customerId);
            BasePage<MailMessageVO> page = mailService.queryMessages(queryBO);
            return renderMailList(page.getList());
        } catch (Exception e) {
            log.warn("AI 搜索邮件失败: {}", e.getMessage());
            return "搜索邮件失败：" + e.getMessage();
        }
    }

    @Tool(description = "读取某一封已同步邮件的详情。当用户已指定邮件ID并要求查看邮件正文或细节时调用。")
    @AiToolPermission(value = "mail:view", action = "查看邮件详情")
    public String getSyncedMailDetail(
            @ToolParam(description = "邮件ID，数字类型") String messageIdStr) {
        try {
            Long messageId = parseLong(messageIdStr);
            if (messageId == null) {
                return "请提供有效的邮件ID。";
            }
            MailMessageVO mail = mailService.getMessageDetail(messageId);
            StringBuilder sb = new StringBuilder();
            appendMailHeader(sb, mail);
            String body = StrUtil.blankToDefault(mail.getBodyText(), mail.getSummary());
            if (StrUtil.isNotBlank(body)) {
                sb.append("\n正文/摘要:\n").append(StrUtil.maxLength(body, 4000));
            } else {
                sb.append("\n正文: 暂无可读取文本。");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("AI 读取邮件详情失败: {}", e.getMessage());
            return "读取邮件详情失败：" + e.getMessage();
        }
    }

    @Tool(description = "Create a CRM mail draft only. Use this after the user asks to draft an email. This tool never sends email; the user must review and confirm in the product before any send action.")
    @AiToolPermission(value = "mail:manage", action = "create mail draft")
    public String createMailDraft(
            @ToolParam(description = "Recipient email addresses, comma separated") String toAddresses,
            @ToolParam(description = "Email subject") String subject,
            @ToolParam(description = "Email body text") String bodyText,
            @ToolParam(description = "Optional account ID to send from later", required = false) String accountIdStr,
            @ToolParam(description = "Optional CRM customer ID", required = false) String customerIdStr,
            @ToolParam(description = "Optional source synced mail ID", required = false) String sourceMessageIdStr,
            @ToolParam(description = "Optional CC addresses", required = false) String ccAddresses,
            @ToolParam(description = "Optional attachment references", required = false) String attachmentRefs) {
        try {
            MailDraftCreateBO draftBO = new MailDraftCreateBO();
            draftBO.setAccountId(parseLong(accountIdStr));
            Long customerId = parseLong(customerIdStr);
            if (customerId == null) {
                customerId = AiContextHolder.getCurrentCustomerId();
            }
            draftBO.setCustomerId(customerId);
            draftBO.setSourceMessageId(parseLong(sourceMessageIdStr));
            draftBO.setToAddresses(toAddresses);
            draftBO.setCcAddresses(StrUtil.trimToNull(ccAddresses));
            draftBO.setSubject(subject);
            draftBO.setBodyText(bodyText);
            draftBO.setAttachmentRefs(StrUtil.trimToNull(attachmentRefs));
            MailDraftVO draft = mailService.createDraft(draftBO);
            StringBuilder sb = new StringBuilder();
            sb.append("Mail draft created. It has not been sent.\n");
            sb.append("- draftId: ").append(draft.getDraftId()).append('\n');
            sb.append("- subject: ").append(draft.getSubject()).append('\n');
            sb.append("- riskStatus: ").append(draft.getRiskStatus());
            if (StrUtil.isNotBlank(draft.getRiskReasons())) {
                sb.append("\n- riskReasons: ").append(draft.getRiskReasons());
            }
            sb.append("\nAsk the user to review and confirm before sending.");
            return sb.toString();
        } catch (Exception e) {
            log.warn("AI create mail draft failed: {}", e.getMessage());
            return "Create mail draft failed: " + e.getMessage();
        }
    }

    private String renderMailList(List<MailMessageVO> mails) {
        if (mails == null || mails.isEmpty()) {
            return "没有找到匹配的已同步邮件。若刚绑定邮箱，请先在邮箱连接页执行同步。";
        }
        StringBuilder sb = new StringBuilder("找到以下已同步邮件：\n");
        for (MailMessageVO mail : mails) {
            sb.append("- ID ").append(mail.getMessageId())
                    .append(" | ").append(StrUtil.blankToDefault(mail.getSubject(), "(无主题)"))
                    .append(" | 发件人: ").append(StrUtil.blankToDefault(mail.getFromAddress(), "-"));
            if (mail.getReceivedTime() != null) {
                sb.append(" | 时间: ").append(dateFormat.format(mail.getReceivedTime()));
            }
            String preview = StrUtil.blankToDefault(mail.getSummary(), mail.getBodyText());
            if (StrUtil.isNotBlank(preview)) {
                sb.append("\n  摘要: ").append(StrUtil.maxLength(preview, 220));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private void appendMailHeader(StringBuilder sb, MailMessageVO mail) {
        sb.append("邮件ID: ").append(mail.getMessageId()).append('\n');
        sb.append("主题: ").append(StrUtil.blankToDefault(mail.getSubject(), "(无主题)")).append('\n');
        sb.append("发件人: ").append(StrUtil.blankToDefault(mail.getFromAddress(), "-")).append('\n');
        sb.append("收件人: ").append(StrUtil.blankToDefault(mail.getToAddresses(), "-")).append('\n');
        if (mail.getReceivedTime() != null) {
            sb.append("时间: ").append(dateFormat.format(mail.getReceivedTime())).append('\n');
        }
        if (mail.getCustomerName() != null) {
            sb.append("关联客户: ").append(mail.getCustomerName()).append('\n');
        }
        if (StrUtil.isNotBlank(mail.getSummary())) {
            sb.append("摘要: ").append(mail.getSummary()).append('\n');
        }
        if (StrUtil.isNotBlank(mail.getActionItemsJson())) {
            sb.append("待办线索: ").append(mail.getActionItemsJson()).append('\n');
        }
    }

    private int resolveLimit(String limitStr) {
        Long value = parseLong(limitStr);
        if (value == null || value <= 0) {
            return 5;
        }
        return Math.min(value.intValue(), 10);
    }

    private Long parseLong(String value) {
        if (StrUtil.isBlank(value) || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
