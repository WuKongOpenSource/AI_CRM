package com.kakarote.ai_crm.ai.tools;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.ai.state.PendingCustomerCreationStore;
import com.kakarote.ai_crm.ai.tools.support.AiCustomerMatcher;
import com.kakarote.ai_crm.ai.tools.support.AiToolPermission;
import com.kakarote.ai_crm.ai.tools.support.AiToolPermissionSupport;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.CustomerAddBO;
import com.kakarote.ai_crm.entity.BO.CustomerQueryBO;
import com.kakarote.ai_crm.entity.BO.CustomerUpdateBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.VO.CustomerDetailVO;
import com.kakarote.ai_crm.entity.VO.CustomerListVO;
import com.kakarote.ai_crm.service.ICustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 客户相关 AI Tool，用于 Spring AI Function Calling。
 */
@Slf4j
@Component
public class CustomerTools {

    @Autowired
    private ICustomerService customerService;

    @Autowired
    private PendingCustomerCreationStore pendingCustomerCreationStore;

    @Autowired
    private AiCustomerMatcher aiCustomerMatcher;

    @Autowired
    private AiToolPermissionSupport permissionSupport;

    @Tool(description = "创建新客户档案。系统会先检查是否已存在同名客户；如果存在，不会直接创建，而是进入待确认状态。只有用户明确确认后，才能再调用 confirmPendingCustomerCreation 完成创建。")
    @AiToolPermission(value = "customer:create", action = "创建客户")
    public String createCustomer(
            @ToolParam(description = "公司名称，必填") String companyName,
            @ToolParam(description = "行业，如互联网、金融、制造业等", required = false) String industry,
            @ToolParam(description = "客户级别：A（重要客户）、B（普通客户）、C（一般客户），默认B", required = false) String level,
            @ToolParam(description = "联系人姓名", required = false) String contactName,
            @ToolParam(description = "联系人电话", required = false) String contactPhone,
            @ToolParam(description = "联系人邮箱", required = false) String contactEmail,
            @ToolParam(description = "联系人职位", required = false) String contactPosition,
            @ToolParam(description = "地址", required = false) String address,
            @ToolParam(description = "网站", required = false) String website,
            @ToolParam(description = "备注", required = false) String remark,
            @ToolParam(description = "报价金额", required = false) String quotation) {

        log.info("【Tool调用】createCustomer: companyName={}, industry={}, level={}",
            companyName, industry, level);

        try {
            String normalizedCompanyName = normalizeRequiredText(companyName);
            if (normalizedCompanyName == null) {
                return "创建客户失败：缺少公司名称";
            }

            Long sessionId = AiContextHolder.getCurrentSessionId();
            if (sessionId != null) {
                pendingCustomerCreationStore.clear(sessionId);
            }

            CustomerAddBO bo = buildCustomerAddBO(
                normalizedCompanyName,
                industry,
                level,
                contactName,
                contactPhone,
                contactEmail,
                contactPosition,
                address,
                website,
                remark,
                quotation
            );

            List<Customer> existingCustomers = customerService.findCustomersByExactCompanyName(normalizedCompanyName);
            if (!existingCustomers.isEmpty()) {
                if (sessionId != null) {
                    pendingCustomerCreationStore.save(sessionId, bo);
                }
                return buildDuplicateConfirmationMessage(bo, existingCustomers, sessionId != null);
            }

            Long customerId = customerService.addCustomer(bo);
            return buildCreateSuccessMessage(bo, customerId);
        } catch (Exception e) {
            log.error("【Tool调用】createCustomer 失败: {}", e.getMessage(), e);
            return "创建客户失败: " + e.getMessage();
        }
    }

    @Tool(description = "确认创建重复客户。只有在 createCustomer 检测到同名客户后，且用户明确表示“确认创建”“继续创建”“仍然创建”时才调用。会基于当前会话中暂存的草稿真正创建客户。")
    @AiToolPermission(value = "customer:create", action = "确认创建客户")
    public String confirmPendingCustomerCreation() {
        Long sessionId = AiContextHolder.getCurrentSessionId();
        if (sessionId == null) {
            return "确认创建失败：当前会话不存在，无法找到待确认的客户草稿";
        }

        PendingCustomerCreationStore.PendingCustomerCreation pendingCreation = pendingCustomerCreationStore.get(sessionId);
        if (pendingCreation == null) {
            return "当前没有待确认的重复客户创建请求。";
        }

        try {
            CustomerAddBO customerAddBO = pendingCreation.customerAddBO();
            Long customerId = customerService.addCustomer(customerAddBO);
            pendingCustomerCreationStore.clear(sessionId);
            return buildCreateSuccessMessage(customerAddBO, customerId);
        } catch (Exception e) {
            log.error("【Tool调用】confirmPendingCustomerCreation 失败: {}", e.getMessage(), e);
            return "确认创建客户失败: " + e.getMessage();
        }
    }

    @Tool(description = "取消待确认的重复客户创建。只有在 createCustomer 检测到同名客户后，且用户明确表示“不创建”“取消”“算了”时才调用。")
    @AiToolPermission(value = "customer:create", action = "取消创建客户")
    public String cancelPendingCustomerCreation() {
        Long sessionId = AiContextHolder.getCurrentSessionId();
        if (sessionId == null) {
            return "取消创建失败：当前会话不存在，无法找到待确认的客户草稿";
        }

        PendingCustomerCreationStore.PendingCustomerCreation pendingCreation = pendingCustomerCreationStore.remove(sessionId);
        if (pendingCreation == null) {
            return "当前没有待确认的重复客户创建请求。";
        }

        return "已取消重复客户创建请求：" + pendingCreation.customerAddBO().getCompanyName();
    }

    @Tool(description = "查询客户列表。当用户查看、搜索、筛选客户时调用此工具。")
    @AiToolPermission(value = "customer:view", action = "查看客户")
    public String queryCustomers(
            @ToolParam(description = "搜索关键词，可搜索公司名称", required = false) String keyword,
            @ToolParam(description = "客户级别筛选：A/B/C", required = false) String level,
            @ToolParam(description = "商机阶段筛选：lead(线索)/qualified(已验证)/proposal(方案)/negotiation(谈判)/closed(成交)/lost(流失)", required = false) String stage,
            @ToolParam(description = "行业筛选", required = false) String industry) {

        try {
            CustomerQueryBO queryBO = new CustomerQueryBO();
            queryBO.setKeyword(keyword);
            queryBO.setLevel(level);
            queryBO.setStage(stage);
            queryBO.setIndustry(industry);
            queryBO.setPage(1);
            queryBO.setLimit(10);

            BasePage<CustomerListVO> page = customerService.queryPageList(queryBO);
            if (page.getList().isEmpty()) {
                return "没有找到符合条件的客户。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("客户列表（共 ").append(page.getTotalRow()).append(" 条，展示前 10 条）\n");
            for (int index = 0; index < page.getList().size(); index++) {
                CustomerListVO customer = page.getList().get(index);
                sb.append(index + 1)
                    .append(". ")
                    .append(customer.getCompanyName())
                    .append("（客户ID: ")
                    .append(customer.getCustomerId())
                    .append("，等级: ")
                    .append(StrUtil.blankToDefault(customer.getLevel(), "未知"))
                    .append("，阶段: ")
                    .append(getStageLabel(customer.getStage()))
                    .append(")");
                if (StrUtil.isNotBlank(customer.getIndustry())) {
                    sb.append("，行业: ").append(customer.getIndustry());
                }
                sb.append("\n");
            }
            sb.append("如需查看某个客户的详细信息，请告诉我客户名称或客户ID。");
            return sb.toString();
        } catch (Exception e) {
            return "查询客户失败: " + e.getMessage();
        }
    }

    @Tool(description = "修改客户信息。当用户要修改、编辑、更新已有客户的信息时调用。包括公司名称、行业、阶段、等级、地址、网站、金额等。")
    @AiToolPermission(value = "customer:edit", action = "编辑客户")
    public String updateCustomer(
            @ToolParam(description = "客户ID，数字类型，必填") String customerIdStr,
            @ToolParam(description = "公司名称", required = false) String companyName,
            @ToolParam(description = "行业，如互联网、金融、制造业等", required = false) String industry,
            @ToolParam(description = "商机阶段：lead(线索)/qualified(已验证)/proposal(方案)/negotiation(谈判)/closed(成交)/lost(流失)", required = false) String stage,
            @ToolParam(description = "客户级别：A（重要客户）、B（普通客户）、C（一般客户）", required = false) String level,
            @ToolParam(description = "地址", required = false) String address,
            @ToolParam(description = "网站", required = false) String website,
            @ToolParam(description = "报价金额", required = false) String quotation,
            @ToolParam(description = "合同金额", required = false) String contractAmount,
            @ToolParam(description = "回款金额", required = false) String revenue,
            @ToolParam(description = "下次跟进时间，格式：yyyy-MM-dd", required = false) String nextFollowTime,
            @ToolParam(description = "备注", required = false) String remark) {

        log.info("【Tool调用】updateCustomer: customerId={}, companyName={}, stage={}, level={}",
            customerIdStr, companyName, stage, level);

        try {
            if (StrUtil.isBlank(customerIdStr) || "null".equalsIgnoreCase(customerIdStr)) {
                return "更新客户失败: 缺少客户ID参数";
            }

            Long customerId;
            try {
                customerId = Long.parseLong(customerIdStr);
            } catch (NumberFormatException e) {
                return "更新客户失败: 客户ID格式无效";
            }

            CustomerUpdateBO bo = new CustomerUpdateBO();
            bo.setCustomerId(customerId);

            String normalizedCompanyName = normalizeOptionalText(companyName);
            if (normalizedCompanyName != null) {
                bo.setCompanyName(normalizedCompanyName);
            }
            String normalizedIndustry = normalizeOptionalText(industry);
            if (normalizedIndustry != null) {
                bo.setIndustry(normalizedIndustry);
            }
            String normalizedStage = normalizeOptionalText(stage);
            if (normalizedStage != null) {
                String denied = permissionSupport.denyMessage("customer:change_stage", "变更客户阶段");
                if (denied != null) {
                    return denied;
                }
                bo.setStage(normalizedStage);
            }
            String normalizedLevel = normalizeOptionalText(level);
            if (normalizedLevel != null) {
                bo.setLevel(normalizedLevel);
            }
            String normalizedAddress = normalizeOptionalText(address);
            if (normalizedAddress != null) {
                bo.setAddress(normalizedAddress);
            }
            String normalizedWebsite = normalizeOptionalText(website);
            if (normalizedWebsite != null) {
                bo.setWebsite(normalizedWebsite);
            }
            String normalizedRemark = normalizeOptionalText(remark);
            if (normalizedRemark != null) {
                bo.setRemark(normalizedRemark);
            }

            if (normalizeOptionalText(quotation) != null) {
                try {
                    bo.setQuotation(new BigDecimal(quotation.trim()));
                } catch (NumberFormatException e) {
                    log.warn("报价金额格式无效: {}", quotation);
                }
            }
            if (normalizeOptionalText(contractAmount) != null) {
                try {
                    bo.setContractAmount(new BigDecimal(contractAmount.trim()));
                } catch (NumberFormatException e) {
                    log.warn("合同金额格式无效: {}", contractAmount);
                }
            }
            if (normalizeOptionalText(revenue) != null) {
                try {
                    bo.setRevenue(new BigDecimal(revenue.trim()));
                } catch (NumberFormatException e) {
                    log.warn("回款金额格式无效: {}", revenue);
                }
            }

            if (normalizeOptionalText(nextFollowTime) != null) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    bo.setNextFollowTime(dateFormat.parse(nextFollowTime.trim()));
                } catch (Exception e) {
                    log.warn("下次跟进时间格式无效: {}", nextFollowTime);
                }
            }

            customerService.updateCustomer(bo);

            StringBuilder result = new StringBuilder("客户信息已更新成功！");
            if (normalizedCompanyName != null) {
                result.append("\n- 公司名称: ").append(normalizedCompanyName);
            }
            if (normalizedStage != null) {
                result.append("\n- 阶段: ").append(getStageLabel(normalizedStage));
            }
            if (normalizedLevel != null) {
                result.append("\n- 等级: ").append(normalizedLevel);
            }
            if (normalizeOptionalText(quotation) != null) {
                result.append("\n- 报价金额: ").append(quotation.trim());
            }
            return result.toString();
        } catch (Exception e) {
            log.error("【Tool调用】updateCustomer 失败: {}", e.getMessage(), e);
            return "更新客户失败: " + e.getMessage();
        }
    }

    @Tool(description = "获取客户详细信息。当用户询问某个客户的具体信息、联系人、跟进记录时调用。可以使用客户ID或公司名称查询。")
    @AiToolPermission(value = "customer:view", action = "查看客户详情")
    public String getCustomerDetail(
            @ToolParam(description = "客户标识，可以是客户ID（数字）或公司名称（文本）。优先使用客户ID") String customerIdentifier) {

        try {
            String normalizedIdentifier = normalizeRequiredText(customerIdentifier);
            if (normalizedIdentifier == null) {
                return "获取客户详情失败: 缺少客户标识参数";
            }

            Long customerId;
            String matchedCompanyName = normalizedIdentifier;
            try {
                customerId = Long.parseLong(normalizedIdentifier);
            } catch (NumberFormatException e) {
                AiCustomerMatcher.CustomerMatchResult matchResult = aiCustomerMatcher.match(normalizedIdentifier);
                if (matchResult.isAmbiguous()) {
                    return "获取客户详情失败: 客户名称「" + normalizedIdentifier + "」无法唯一匹配，可能是：" + matchResult.formatCandidateNames() + "。请提供更完整的客户名称。";
                }
                if (!matchResult.isMatched()) {
                    return "获取客户详情失败: 未找到名为「" + normalizedIdentifier + "」的客户";
                }
                customerId = matchResult.getCustomer().getCustomerId();
                matchedCompanyName = matchResult.getCustomer().getCompanyName();
            }

            if (customerId == null) {
                return "获取客户详情失败: 未找到名为“" + normalizedIdentifier + "”的客户";
            }

            CustomerDetailVO detail = customerService.getCustomerDetail(customerId);
            StringBuilder sb = new StringBuilder();
            sb.append("客户详情: ").append(detail.getCompanyName())
                .append("（客户ID: ").append(detail.getCustomerId()).append("）\n")
                .append("- 等级: ").append(StrUtil.blankToDefault(detail.getLevel(), "未知")).append("\n")
                .append("- 阶段: ").append(getStageLabel(detail.getStage())).append("\n");

            if (StrUtil.isNotBlank(detail.getIndustry())) {
                sb.append("- 行业: ").append(detail.getIndustry()).append("\n");
            }
            if (StrUtil.isNotBlank(detail.getAddress())) {
                sb.append("- 地址: ").append(detail.getAddress()).append("\n");
            }
            if (StrUtil.isNotBlank(detail.getWebsite())) {
                sb.append("- 网站: ").append(detail.getWebsite()).append("\n");
            }

            if (detail.getContacts() != null && !detail.getContacts().isEmpty()) {
                sb.append("联系人:\n");
                detail.getContacts().forEach(contact -> {
                    sb.append("- ").append(contact.getName());
                    if (StrUtil.isNotBlank(contact.getPosition())) {
                        sb.append("（").append(contact.getPosition()).append("）");
                    }
                    if (StrUtil.isNotBlank(contact.getPhone())) {
                        sb.append("，电话: ").append(contact.getPhone());
                    }
                    if (StrUtil.isNotBlank(contact.getEmail())) {
                        sb.append("，邮箱: ").append(contact.getEmail());
                    }
                    sb.append("\n");
                });
            }

            if (detail.getTags() != null && !detail.getTags().isEmpty()) {
                sb.append("标签: ");
                detail.getTags().forEach(tag -> sb.append("#").append(tag.getTagName()).append(" "));
            }

            return sb.toString().trim();
        } catch (Exception e) {
            return "获取客户详情失败: " + e.getMessage();
        }
    }

    private CustomerAddBO buildCustomerAddBO(String companyName,
                                             String industry,
                                             String level,
                                             String contactName,
                                             String contactPhone,
                                             String contactEmail,
                                             String contactPosition,
                                             String address,
                                             String website,
                                             String remark,
                                             String quotation) {
        CustomerAddBO bo = new CustomerAddBO();
        bo.setCompanyName(companyName);
        bo.setIndustry(normalizeOptionalText(industry));
        bo.setLevel(StrUtil.blankToDefault(normalizeOptionalText(level), "B"));
        bo.setContactName(normalizeOptionalText(contactName));
        bo.setContactPhone(normalizeOptionalText(contactPhone));
        bo.setContactEmail(normalizeOptionalText(contactEmail));
        bo.setContactPosition(normalizeOptionalText(contactPosition));
        bo.setAddress(normalizeOptionalText(address));
        bo.setWebsite(normalizeOptionalText(website));
        bo.setRemark(normalizeOptionalText(remark));

        String normalizedQuotation = normalizeOptionalText(quotation);
        if (normalizedQuotation != null) {
            try {
                bo.setQuotation(new BigDecimal(normalizedQuotation));
            } catch (NumberFormatException e) {
                log.warn("报价金额格式无效: {}", quotation);
            }
        }
        return bo;
    }

    private String buildDuplicateConfirmationMessage(CustomerAddBO bo,
                                                     List<Customer> existingCustomers,
                                                     boolean pendingSaved) {
        StringBuilder result = new StringBuilder();
        result.append("检测到系统中已存在同名客户“")
            .append(bo.getCompanyName())
            .append("”，本次请求尚未创建新客户。");

        if (pendingSaved) {
            result.append("我已经暂存了本次创建草稿，请先向用户确认是否仍要创建新的客户档案。");
        } else {
            result.append("当前无法暂存创建草稿，请先向用户确认是否仍要创建新的客户档案。");
        }

        result.append("\n已存在客户：\n");
        for (int i = 0; i < existingCustomers.size(); i++) {
            result.append(i + 1)
                .append(". ")
                .append(formatExistingCustomer(existingCustomers.get(i)))
                .append("\n");
        }

        result.append("待创建草稿：").append(formatDraftCustomer(bo));

        if (pendingSaved) {
            result.append("\n如果用户明确表示“确认创建 / 继续创建 / 仍然创建”，请调用 confirmPendingCustomerCreation。");
            result.append("\n如果用户明确表示“不创建 / 取消 / 算了”，请调用 cancelPendingCustomerCreation。");
        }
        return result.toString().trim();
    }

    private String formatExistingCustomer(Customer customer) {
        StringBuilder sb = new StringBuilder();
        sb.append("客户ID=").append(customer.getCustomerId());
        if (StrUtil.isNotBlank(customer.getLevel())) {
            sb.append("，等级=").append(customer.getLevel());
        }
        if (StrUtil.isNotBlank(customer.getStage())) {
            sb.append("，阶段=").append(getStageLabel(customer.getStage()));
        }
        if (StrUtil.isNotBlank(customer.getPrimaryContactName())) {
            sb.append("，主联系人=").append(customer.getPrimaryContactName());
        }
        if (StrUtil.isNotBlank(customer.getPrimaryContactPhone())) {
            sb.append("，联系电话=").append(customer.getPrimaryContactPhone());
        }
        return sb.toString();
    }

    private String formatDraftCustomer(CustomerAddBO bo) {
        StringBuilder sb = new StringBuilder();
        sb.append("公司名称=").append(bo.getCompanyName());
        if (StrUtil.isNotBlank(bo.getIndustry())) {
            sb.append("，行业=").append(bo.getIndustry());
        }
        if (StrUtil.isNotBlank(bo.getLevel())) {
            sb.append("，等级=").append(bo.getLevel());
        }
        if (StrUtil.isNotBlank(bo.getContactName())) {
            sb.append("，联系人=").append(bo.getContactName());
        }
        if (StrUtil.isNotBlank(bo.getContactPhone())) {
            sb.append("，联系电话=").append(bo.getContactPhone());
        }
        return sb.toString();
    }

    private String buildCreateSuccessMessage(CustomerAddBO bo, Long customerId) {
        StringBuilder result = new StringBuilder();
        result.append("客户“")
            .append(bo.getCompanyName())
            .append("”已创建成功！客户ID: ")
            .append(customerId);
        if (StrUtil.isNotBlank(bo.getContactName())) {
            result.append("\n已添加联系人: ").append(bo.getContactName());
        }
        return result.toString();
    }

    /**
     * 根据公司名称查找客户ID。
     */
    private Long findCustomerIdByName(String companyName) {
        List<Customer> exactCustomers = customerService.findCustomersByExactCompanyName(companyName);
        if (!exactCustomers.isEmpty()) {
            return exactCustomers.get(0).getCustomerId();
        }

        CustomerQueryBO queryBO = new CustomerQueryBO();
        queryBO.setKeyword(companyName);
        queryBO.setPage(1);
        queryBO.setLimit(10);

        BasePage<CustomerListVO> page = customerService.queryPageList(queryBO);
        if (page.getList().isEmpty()) {
            return null;
        }

        for (CustomerListVO customer : page.getList()) {
            if (companyName.equals(customer.getCompanyName())) {
                return customer.getCustomerId();
            }
        }

        return page.getList().get(0).getCustomerId();
    }

    private String normalizeRequiredText(String value) {
        String normalized = StrUtil.trim(value);
        if (StrUtil.isBlank(normalized) || "null".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        return normalizeRequiredText(value);
    }

    private String getStageLabel(String stage) {
        if (stage == null) {
            return "未知";
        }
        return switch (stage.toLowerCase()) {
            case "lead" -> "线索";
            case "qualified" -> "已验证";
            case "proposal" -> "方案阶段";
            case "negotiation" -> "商务谈判";
            case "closed" -> "已成交";
            case "lost" -> "已流失";
            default -> stage;
        };
    }
}
