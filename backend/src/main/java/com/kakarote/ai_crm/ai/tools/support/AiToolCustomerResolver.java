package com.kakarote.ai_crm.ai.tools.support;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.service.ICustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Resolves the customer scope for AI tools that create customer-related records.
 */
@Component
public class AiToolCustomerResolver {

    private static final Set<String> BOUND_CUSTOMER_REFERENCES = Set.of(
        "这个客户",
        "这位客户",
        "这家客户",
        "这家公司",
        "该客户",
        "该公司",
        "此客户",
        "此公司",
        "当前客户",
        "当前公司",
        "当前这家公司",
        "客户",
        "公司",
        "他们",
        "它们",
        "他",
        "她",
        "它"
    );

    @Autowired
    private ICustomerService customerService;

    @Autowired
    private AiCustomerMatcher aiCustomerMatcher;

    public CustomerResolveResult resolveForCreate(String customerIdStr,
                                                  String customerName,
                                                  String noAccessActionLabel,
                                                  String failurePrefix,
                                                  String missingActionName) {
        String normalizedCustomerId = normalizeOptionalText(customerIdStr);
        if (normalizedCustomerId != null) {
            return resolveById(normalizedCustomerId, noAccessActionLabel);
        }

        String normalizedCustomerName = normalizeOptionalText(customerName);
        if (normalizedCustomerName != null && !isBoundCustomerReference(normalizedCustomerName)) {
            return resolveByName(normalizedCustomerName, noAccessActionLabel, failurePrefix, missingActionName);
        }

        return resolveBoundCustomer(noAccessActionLabel);
    }

    public CustomerResolveResult resolveBoundCustomer(String noAccessActionLabel) {
        Long boundCustomerId = AiContextHolder.getCurrentCustomerId();
        if (boundCustomerId == null) {
            return new CustomerResolveResult(null, null);
        }
        return resolveById(String.valueOf(boundCustomerId), noAccessActionLabel);
    }

    public boolean isBoundCustomerReference(String customerName) {
        String normalized = normalizeOptionalText(customerName);
        if (normalized == null) {
            return false;
        }
        String compact = normalized.replaceAll("[\\s\\p{P}\\p{S}]+", "");
        return BOUND_CUSTOMER_REFERENCES.contains(compact);
    }

    private CustomerResolveResult resolveById(String customerIdStr, String noAccessActionLabel) {
        try {
            Long customerId = Long.parseLong(customerIdStr);
            Customer customer = customerService.getById(customerId);
            if (isVisibleCustomer(customer)) {
                return new CustomerResolveResult(customer, null);
            }

            Customer existingCustomer = customerService.findCustomerByIdIgnoreDataPermission(customerId);
            if (isVisibleCustomer(existingCustomer)) {
                String message = AiCustomerMatcher.CustomerMatchResult
                    .existsNoAccess(customerIdStr, existingCustomer)
                    .formatNoAccessMessage(noAccessActionLabel);
                return new CustomerResolveResult(null, message);
            }
            return new CustomerResolveResult(null, "操作未执行：客户不存在或已停用，无法" + noAccessActionLabel + "。");
        } catch (NumberFormatException e) {
            return new CustomerResolveResult(null, "操作未执行：客户ID必须是数字。");
        }
    }

    private CustomerResolveResult resolveByName(String customerName,
                                                String noAccessActionLabel,
                                                String failurePrefix,
                                                String missingActionName) {
        AiCustomerMatcher.CustomerMatchResult customerMatch = aiCustomerMatcher.match(customerName);
        if (customerMatch.isExistsNoAccess()) {
            return new CustomerResolveResult(null, customerMatch.formatNoAccessMessage(noAccessActionLabel));
        }
        if (customerMatch.isAmbiguous()) {
            return new CustomerResolveResult(null,
                failurePrefix + ": 客户名称「" + customerName + "」无法唯一匹配，可能是："
                    + customerMatch.formatCandidateNames() + "。请提供更完整的客户名称。");
        }
        if (!customerMatch.isMatched()) {
            return new CustomerResolveResult(null,
                failurePrefix + ": 系统中未找到名为「" + customerName + "」的客户。请先创建该客户后再"
                    + missingActionName + "，或确认客户名称是否正确。");
        }
        return new CustomerResolveResult(customerMatch.getCustomer(), null);
    }

    private boolean isVisibleCustomer(Customer customer) {
        return customer != null && !Integer.valueOf(0).equals(customer.getStatus());
    }

    private String normalizeOptionalText(String value) {
        String normalized = StrUtil.trim(value);
        if (StrUtil.isBlank(normalized) || "null".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    public record CustomerResolveResult(Customer customer, String errorMessage) {
    }
}
