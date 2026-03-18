package com.kakarote.ai_crm.ai.tools;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.CustomerAddBO;
import com.kakarote.ai_crm.entity.BO.CustomerQueryBO;
import com.kakarote.ai_crm.entity.BO.CustomerUpdateBO;
import com.kakarote.ai_crm.entity.VO.CustomerDetailVO;
import com.kakarote.ai_crm.entity.VO.CustomerListVO;
import com.kakarote.ai_crm.service.ICustomerService;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 客户相关 AI Tool - 用于 Spring AI Function Calling
 */
@Slf4j
@Component
public class CustomerTools {

    @Autowired
    private ICustomerService customerService;

    @Tool(description = "创建新客户档案。当用户要创建、新建、添加客户时调用此工具。")
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

        log.info("【Tool调用】createCustomer 被调用: companyName={}, industry={}, level={}",
            companyName, industry, level);

        try {
            CustomerAddBO bo = new CustomerAddBO();
            bo.setCompanyName(companyName);
            bo.setIndustry(industry);
            bo.setLevel(level != null ? level : "B");
            bo.setContactName(contactName);
            bo.setContactPhone(contactPhone);
            bo.setContactEmail(contactEmail);
            bo.setContactPosition(contactPosition);
            if (address != null && !address.isEmpty() && !"null".equalsIgnoreCase(address)) {
                bo.setAddress(address);
            }
            if (website != null && !website.isEmpty() && !"null".equalsIgnoreCase(website)) {
                bo.setWebsite(website);
            }
            if (remark != null && !remark.isEmpty() && !"null".equalsIgnoreCase(remark)) {
                bo.setRemark(remark);
            }
            if (quotation != null && !quotation.isEmpty() && !"null".equalsIgnoreCase(quotation)) {
                try {
                    bo.setQuotation(new BigDecimal(quotation));
                } catch (NumberFormatException e) {
                    log.warn("报价金额格式无效: {}", quotation);
                }
            }

            Long customerId = customerService.addCustomer(bo);

            log.info("【Tool调用】createCustomer 成功: customerId={}", customerId);
            return String.format("客户「%s」创建成功！客户ID: %d。%s",
                companyName,
                customerId,
                contactName != null ? String.format("已添加联系人: %s", contactName) : "");
        } catch (Exception e) {
            log.error("【Tool调用】createCustomer 失败: {}", e.getMessage(), e);
            return "创建客户失败: " + e.getMessage();
        }
    }

    @Tool(description = "查询客户列表。当用户查看、搜索、筛选客户时调用此工具。")
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
            sb.append("📋 **客户列表**（共 ").append(page.getTotalRow()).append(" 位，显示前 10 位）\n\n");
            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

            int index = 1;
            StringBuilder idMapping = new StringBuilder();

            for (CustomerListVO customer : page.getList()) {
                sb.append(String.format("%d. **%s**\n", index++, customer.getCompanyName()));
                sb.append(String.format("   🏷️ %s级客户 · 📊 %s",
                    customer.getLevel(),
                    getStageLabel(customer.getStage())));
                if (customer.getIndustry() != null) {
                    sb.append(String.format(" · 🏢 %s行业", customer.getIndustry()));
                }
                sb.append("\n\n");

                // 记录ID映射供AI内部使用
                if (idMapping.length() > 0) {
                    idMapping.append(", ");
                }
                idMapping.append(customer.getCompanyName()).append("#").append(customer.getCustomerId());
            }

            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
            sb.append("💡 如需查看某个客户的详细信息，请告诉我客户名称。\n\n");
            sb.append("---\n");
            sb.append("[系统备注] 客户标识: ").append(idMapping.toString());

            return sb.toString();
        } catch (Exception e) {
            return "查询客户失败: " + e.getMessage();
        }
    }

    @Tool(description = "修改客户信息。当用户要修改、编辑、更新已有客户的信息时调用。包括公司名称、行业、阶段、等级、地址、网站、金额等。")
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

        log.info("【Tool调用】updateCustomer 被调用: customerId={}, companyName={}, stage={}, level={}",
            customerIdStr, companyName, stage, level);

        try {
            // 参数验证
            if (customerIdStr == null || customerIdStr.isEmpty() || "null".equalsIgnoreCase(customerIdStr)) {
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

            // 只设置非空的字段
            if (companyName != null && !companyName.isEmpty() && !"null".equalsIgnoreCase(companyName)) {
                bo.setCompanyName(companyName);
            }
            if (industry != null && !industry.isEmpty() && !"null".equalsIgnoreCase(industry)) {
                bo.setIndustry(industry);
            }
            if (stage != null && !stage.isEmpty() && !"null".equalsIgnoreCase(stage)) {
                bo.setStage(stage);
            }
            if (level != null && !level.isEmpty() && !"null".equalsIgnoreCase(level)) {
                bo.setLevel(level);
            }
            if (address != null && !address.isEmpty() && !"null".equalsIgnoreCase(address)) {
                bo.setAddress(address);
            }
            if (website != null && !website.isEmpty() && !"null".equalsIgnoreCase(website)) {
                bo.setWebsite(website);
            }
            if (remark != null && !remark.isEmpty() && !"null".equalsIgnoreCase(remark)) {
                bo.setRemark(remark);
            }

            // 处理金额字段
            if (quotation != null && !quotation.isEmpty() && !"null".equalsIgnoreCase(quotation)) {
                try {
                    bo.setQuotation(new BigDecimal(quotation));
                } catch (NumberFormatException e) {
                    log.warn("报价金额格式无效: {}", quotation);
                }
            }
            if (contractAmount != null && !contractAmount.isEmpty() && !"null".equalsIgnoreCase(contractAmount)) {
                try {
                    bo.setContractAmount(new BigDecimal(contractAmount));
                } catch (NumberFormatException e) {
                    log.warn("合同金额格式无效: {}", contractAmount);
                }
            }
            if (revenue != null && !revenue.isEmpty() && !"null".equalsIgnoreCase(revenue)) {
                try {
                    bo.setRevenue(new BigDecimal(revenue));
                } catch (NumberFormatException e) {
                    log.warn("回款金额格式无效: {}", revenue);
                }
            }

            // 处理时间字段
            if (nextFollowTime != null && !nextFollowTime.isEmpty() && !"null".equalsIgnoreCase(nextFollowTime)) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    bo.setNextFollowTime(dateFormat.parse(nextFollowTime));
                } catch (Exception e) {
                    log.warn("跟进时间格式无效: {}", nextFollowTime);
                }
            }

            customerService.updateCustomer(bo);

            log.info("【Tool调用】updateCustomer 成功: customerId={}", customerId);

            // 构建返回信息
            StringBuilder result = new StringBuilder();
            result.append("客户信息已更新成功！");
            if (companyName != null && !companyName.isEmpty()) {
                result.append("\n- 公司名称: ").append(companyName);
            }
            if (stage != null && !stage.isEmpty()) {
                result.append("\n- 阶段: ").append(getStageLabel(stage));
            }
            if (level != null && !level.isEmpty()) {
                result.append("\n- 等级: ").append(level).append("级");
            }
            if (quotation != null && !quotation.isEmpty()) {
                result.append("\n- 报价金额: ").append(quotation);
            }

            return result.toString();
        } catch (Exception e) {
            log.error("【Tool调用】updateCustomer 失败: {}", e.getMessage(), e);
            return "更新客户失败: " + e.getMessage();
        }
    }

    @Tool(description = "获取客户详细信息。当用户询问某个客户的具体信息、联系人、跟进记录时调用。可以使用客户ID或公司名称查询。")
    public String getCustomerDetail(
            @ToolParam(description = "客户标识，可以是客户ID（数字）或公司名称（文本）。优先使用系统备注中的'公司名#ID'格式中的ID") String customerIdentifier) {

        try {
            if (customerIdentifier == null || customerIdentifier.isEmpty() || "null".equalsIgnoreCase(customerIdentifier)) {
                return "获取客户详情失败: 缺少客户标识参数";
            }

            Long customerId = null;

            // 先尝试解析为ID
            try {
                customerId = Long.parseLong(customerIdentifier.trim());
            } catch (NumberFormatException e) {
                // 不是数字，尝试按公司名称查询
                customerId = findCustomerIdByName(customerIdentifier.trim());
            }

            if (customerId == null) {
                return "获取客户详情失败: 未找到名为「" + customerIdentifier + "」的客户";
            }

            CustomerDetailVO detail = customerService.getCustomerDetail(customerId);

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("📋 **客户详情: %s**\n\n", detail.getCompanyName()));
            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
            sb.append(String.format("🏷️ **级别**: %s级\n", detail.getLevel()));
            sb.append(String.format("📊 **阶段**: %s\n", getStageLabel(detail.getStage())));
            if (detail.getIndustry() != null) {
                sb.append(String.format("🏢 **行业**: %s\n", detail.getIndustry()));
            }
            if (detail.getAddress() != null) {
                sb.append(String.format("📍 **地址**: %s\n", detail.getAddress()));
            }
            if (detail.getWebsite() != null) {
                sb.append(String.format("🌐 **网站**: %s\n", detail.getWebsite()));
            }

            // Contacts
            if (detail.getContacts() != null && !detail.getContacts().isEmpty()) {
                sb.append("\n### 👥 联系人\n");
                detail.getContacts().forEach(contact -> {
                    sb.append(String.format("- %s%s",
                        contact.getName(),
                        contact.getPosition() != null ? "（" + contact.getPosition() + "）" : ""));
                    if (contact.getPhone() != null) {
                        sb.append("，📞 " + contact.getPhone());
                    }
                    if (contact.getEmail() != null) {
                        sb.append("，✉️ " + contact.getEmail());
                    }
                    sb.append("\n");
                });
            }

            // Tags
            if (detail.getTags() != null && !detail.getTags().isEmpty()) {
                sb.append("\n### 🏷️ 标签\n");
                detail.getTags().forEach(tag -> sb.append("#" + tag.getTagName() + " "));
                sb.append("\n");
            }

            sb.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            sb.append("[系统备注] 客户标识: ").append(detail.getCompanyName()).append("#").append(detail.getCustomerId());

            return sb.toString();
        } catch (Exception e) {
            return "获取客户详情失败: " + e.getMessage();
        }
    }

    /**
     * 根据公司名称查找客户ID
     */
    private Long findCustomerIdByName(String companyName) {
        CustomerQueryBO queryBO = new CustomerQueryBO();
        queryBO.setKeyword(companyName);
        queryBO.setPage(1);
        queryBO.setLimit(1);

        BasePage<CustomerListVO> page = customerService.queryPageList(queryBO);
        if (page.getList().isEmpty()) {
            return null;
        }

        // 优先精确匹配
        for (CustomerListVO customer : page.getList()) {
            if (companyName.equals(customer.getCompanyName())) {
                return customer.getCustomerId();
            }
        }

        // 如果没有精确匹配，返回第一个结果
        return page.getList().get(0).getCustomerId();
    }

    private String getStageLabel(String stage) {
        if (stage == null) return "未知";
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
