package com.kakarote.ai_crm.ai.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.ai.tools.support.AiToolPermission;
import com.kakarote.ai_crm.ai.tools.support.AiToolCustomerResolver;
import com.kakarote.ai_crm.entity.BO.ContactAddBO;
import com.kakarote.ai_crm.entity.BO.ContactUpdateBO;
import com.kakarote.ai_crm.entity.PO.Contact;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.service.IContactService;
import com.kakarote.ai_crm.service.ICustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 联系人相关 AI Tool - 用于 Spring AI Function Calling
 */
@Slf4j
@Component
public class ContactTools {

    @Autowired
    private IContactService contactService;

    @Autowired
    private ICustomerService customerService;

    @Autowired
    private AiToolCustomerResolver customerResolver;

    /**
     * 创建联系人。
     */
    @Tool(description = "创建新联系人。当用户要给某个客户添加联系人、新增联系人、记录联系人姓名/电话/邮箱/职位时调用。客户解析优先级：显式customerIdStr > 显式客户名称 > 当前客户对话绑定客户。当前客户对话中，如果用户只说“这个客户/当前客户/他们”等，不要把代词作为customerName，留空即可让工具默认关联当前客户。联系人姓名必填；如果只提供电话但没有姓名，请先向用户询问联系人姓名。")
    @AiToolPermission(value = "contact:create", action = "创建联系人")
    public String createContact(
            @ToolParam(description = "Optional CRM customer ID returned by createCustomer or getCustomerDetail. Explicit ID has highest priority.", required = false) String customerIdStr,
            @ToolParam(description = "客户名称（公司名）；客户对话模式下未显式指定其他客户时可留空", required = false) String customerName,
            @ToolParam(description = "联系人姓名，必填") String name,
            @ToolParam(description = "职位", required = false) String position,
            @ToolParam(description = "电话/手机号", required = false) String phone,
            @ToolParam(description = "邮箱", required = false) String email,
            @ToolParam(description = "微信", required = false) String wechat,
            @ToolParam(description = "是否主联系人：true/false 或 1/0，默认 false", required = false) String isPrimary,
            @ToolParam(description = "备注", required = false) String notes) {

        log.info("【Tool调用】createContact: customerId={}, customerName={}, name={}, phone={}",
                customerIdStr, customerName, name, phone);

        try {
            String normalizedName = normalizeRequiredText(name);
            if (normalizedName == null) {
                return "创建联系人失败: 缺少联系人姓名。请先提供联系人姓名。";
            }

            AiToolCustomerResolver.CustomerResolveResult customerResolve = customerResolver.resolveForCreate(
                    customerIdStr,
                    customerName,
                    "创建联系人",
                    "创建联系人失败",
                    "添加联系人"
            );
            if (customerResolve.errorMessage() != null) {
                return customerResolve.errorMessage();
            }
            Customer customer = customerResolve.customer();
            if (customer == null) {
                return "创建联系人失败: 缺少关联客户。请提供客户名称或客户ID。";
            }

            ContactAddBO bo = new ContactAddBO();
            bo.setCustomerId(customer.getCustomerId());
            bo.setName(normalizedName);
            bo.setPosition(normalizeOptionalText(position));
            bo.setPhone(normalizeOptionalText(phone));
            bo.setEmail(normalizeOptionalText(email));
            bo.setWechat(normalizeOptionalText(wechat));
            bo.setIsPrimary(parseBooleanFlag(isPrimary) ? 1 : 0);
            bo.setNotes(normalizeOptionalText(notes));

            Long contactId = contactService.addContact(bo);

            StringBuilder result = new StringBuilder();
            result.append("联系人“").append(normalizedName).append("”已创建成功！");
            result.append("\n- 联系人ID: ").append(contactId);
            result.append("\n- 所属客户: ").append(customer.getCompanyName())
                    .append("（客户ID: ").append(customer.getCustomerId()).append("）");
            if (bo.getPosition() != null) {
                result.append("\n- 职位: ").append(bo.getPosition());
            }
            if (bo.getPhone() != null) {
                result.append("\n- 电话: ").append(bo.getPhone());
            }
            if (bo.getEmail() != null) {
                result.append("\n- 邮箱: ").append(bo.getEmail());
            }
            if (bo.getIsPrimary() != null && bo.getIsPrimary() == 1) {
                result.append("\n- 已设为主联系人");
            }
            return result.toString();
        } catch (Exception e) {
            log.error("【Tool调用】createContact 失败: {}", e.getMessage(), e);
            return "创建联系人失败: " + e.getMessage();
        }
    }

    /**
     * 搜索联系人。
     */
    @Tool(description = "查询联系人。当用户要搜索、查找联系人时调用。支持按姓名和/或手机号搜索。")
    @AiToolPermission(value = "contact:view", action = "查看联系人")
    public String searchContacts(
            @ToolParam(description = "联系人姓名，支持模糊搜索", required = false) String name,
            @ToolParam(description = "联系人手机号/电话，支持模糊搜索", required = false) String phone) {

        log.info("【Tool调用】searchContacts: name={}, phone={}", name, phone);

        try {
            boolean hasName = name != null && !name.isEmpty() && !"null".equalsIgnoreCase(name);
            boolean hasPhone = phone != null && !phone.isEmpty() && !"null".equalsIgnoreCase(phone);

            if (!hasName && !hasPhone) {
                return "请提供联系人姓名或手机号进行查询。";
            }

            LambdaQueryWrapper<Contact> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Contact::getStatus, 1);
            if (hasName) {
                wrapper.like(Contact::getName, name);
            }
            if (hasPhone) {
                wrapper.like(Contact::getPhone, phone);
            }
            wrapper.orderByDesc(Contact::getCreateTime);
            wrapper.last("LIMIT 20");

            List<Contact> contacts = contactService.list(wrapper);

            if (contacts.isEmpty()) {
                return "没有找到符合条件的联系人。";
            }

            // 批量查客户名称
            List<Long> customerIds = contacts.stream()
                    .map(Contact::getCustomerId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, String> customerNames = customerService.listByIds(customerIds).stream()
                    .collect(Collectors.toMap(Customer::getCustomerId, Customer::getCompanyName, (a, b) -> a));

            StringBuilder sb = new StringBuilder();
            sb.append("📋 **联系人查询结果**（共 ").append(contacts.size()).append(" 位）\n\n");
            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

            for (int i = 0; i < contacts.size(); i++) {
                Contact c = contacts.get(i);
                sb.append(String.format("%d. **%s**（联系人ID: %d）", i + 1, c.getName(), c.getContactId()));
                if (c.getPosition() != null) {
                    sb.append("（").append(c.getPosition()).append("）");
                }
                sb.append("\n");
                String custName = customerNames.get(c.getCustomerId());
                if (custName != null) {
                    sb.append("   🏢 ").append(custName).append("（客户ID: ").append(c.getCustomerId()).append("）\n");
                }
                if (c.getPhone() != null) {
                    sb.append("   📞 ").append(c.getPhone()).append("\n");
                }
                if (c.getEmail() != null) {
                    sb.append("   ✉️ ").append(c.getEmail()).append("\n");
                }
                if (c.getWechat() != null) {
                    sb.append("   💬 微信: ").append(c.getWechat()).append("\n");
                }
                sb.append("\n");
            }

            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
            sb.append("💡 如需查看某个联系人的详细信息，请告诉我联系人姓名。");

            return sb.toString();
        } catch (Exception e) {
            log.error("【Tool调用】searchContacts 失败: {}", e.getMessage(), e);
            return "查询联系人失败: " + e.getMessage();
        }
    }

    /**
     * 获取联系人详情。
     */
    @Tool(description = "获取联系人详细信息。当用户询问某个联系人的具体信息时调用。可以使用联系人ID或姓名查询。")
    @AiToolPermission(value = "contact:view", action = "查看联系人详情")
    public String getContactDetail(
            @ToolParam(description = "联系人标识，可以是联系人ID（数字）或姓名（文本）。优先使用联系人ID") String contactIdentifier) {

        log.info("【Tool调用】getContactDetail: identifier={}", contactIdentifier);

        try {
            if (contactIdentifier == null || contactIdentifier.isEmpty() || "null".equalsIgnoreCase(contactIdentifier)) {
                return "获取联系人详情失败: 缺少联系人标识参数";
            }

            Contact contact = null;

            // 先尝试解析为ID
            try {
                Long contactId = Long.parseLong(contactIdentifier.trim());
                contact = contactService.getById(contactId);
            } catch (NumberFormatException e) {
                // 不是数字，按姓名精确查询
                LambdaQueryWrapper<Contact> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Contact::getName, contactIdentifier.trim());
                wrapper.eq(Contact::getStatus, 1);
                wrapper.last("LIMIT 1");
                contact = contactService.getOne(wrapper);

                // 精确匹配不到则模糊查
                if (contact == null) {
                    wrapper = new LambdaQueryWrapper<>();
                    wrapper.like(Contact::getName, contactIdentifier.trim());
                    wrapper.eq(Contact::getStatus, 1);
                    wrapper.last("LIMIT 1");
                    contact = contactService.getOne(wrapper);
                }
            }

            if (contact == null) {
                return "获取联系人详情失败: 未找到「" + contactIdentifier + "」的联系人";
            }

            // 查客户名称
            String customerName = null;
            Customer customer = customerService.getById(contact.getCustomerId());
            if (customer != null) {
                customerName = customer.getCompanyName();
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("📋 **联系人详情: %s**（联系人ID: %d）\n\n", contact.getName(), contact.getContactId()));
            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

            if (contact.getPosition() != null) {
                sb.append(String.format("💼 **职位**: %s\n", contact.getPosition()));
            }
            if (customerName != null) {
                sb.append(String.format("🏢 **所属客户**: %s\n", customerName));
            }
            if (contact.getPhone() != null) {
                sb.append(String.format("📞 **电话**: %s\n", contact.getPhone()));
            }
            if (contact.getEmail() != null) {
                sb.append(String.format("✉️ **邮箱**: %s\n", contact.getEmail()));
            }
            if (contact.getWechat() != null) {
                sb.append(String.format("💬 **微信**: %s\n", contact.getWechat()));
            }
            sb.append(String.format("⭐ **主联系人**: %s\n", contact.getIsPrimary() != null && contact.getIsPrimary() == 1 ? "是" : "否"));
            if (contact.getLastContactTime() != null) {
                sb.append(String.format("🕐 **最后联系时间**: %tF\n", contact.getLastContactTime()));
            }
            if (contact.getNotes() != null) {
                sb.append(String.format("📝 **备注**: %s\n", contact.getNotes()));
            }

            sb.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

            return sb.toString();
        } catch (Exception e) {
            log.error("【Tool调用】getContactDetail 失败: {}", e.getMessage(), e);
            return "获取联系人详情失败: " + e.getMessage();
        }
    }

    /**
     * 更新联系人。
     */
    @Tool(description = "修改联系人信息。当用户要修改、编辑、更新联系人的姓名、职位、电话、邮箱、微信、备注等信息时调用。")
    @AiToolPermission(value = "contact:edit", action = "编辑联系人")
    public String updateContact(
            @ToolParam(description = "联系人ID，数字类型，必填") String contactIdStr,
            @ToolParam(description = "姓名", required = false) String name,
            @ToolParam(description = "职位", required = false) String position,
            @ToolParam(description = "电话/手机号", required = false) String phone,
            @ToolParam(description = "邮箱", required = false) String email,
            @ToolParam(description = "微信", required = false) String wechat,
            @ToolParam(description = "备注", required = false) String notes) {

        log.info("【Tool调用】updateContact: contactId={}, name={}, phone={}", contactIdStr, name, phone);

        try {
            if (contactIdStr == null || contactIdStr.isEmpty() || "null".equalsIgnoreCase(contactIdStr)) {
                return "更新联系人失败: 缺少联系人ID参数";
            }

            Long contactId;
            try {
                contactId = Long.parseLong(contactIdStr);
            } catch (NumberFormatException e) {
                return "更新联系人失败: 联系人ID格式无效";
            }

            ContactUpdateBO bo = new ContactUpdateBO();
            bo.setContactId(contactId);

            if (name != null && !name.isEmpty() && !"null".equalsIgnoreCase(name)) {
                bo.setName(name);
            }
            if (position != null && !position.isEmpty() && !"null".equalsIgnoreCase(position)) {
                bo.setPosition(position);
            }
            if (phone != null && !phone.isEmpty() && !"null".equalsIgnoreCase(phone)) {
                bo.setPhone(phone);
            }
            if (email != null && !email.isEmpty() && !"null".equalsIgnoreCase(email)) {
                bo.setEmail(email);
            }
            if (wechat != null && !wechat.isEmpty() && !"null".equalsIgnoreCase(wechat)) {
                bo.setWechat(wechat);
            }
            if (notes != null && !notes.isEmpty() && !"null".equalsIgnoreCase(notes)) {
                bo.setNotes(notes);
            }

            contactService.updateContact(bo);

            log.info("【Tool调用】updateContact 成功: contactId={}", contactId);

            StringBuilder result = new StringBuilder();
            result.append("联系人信息已更新成功！");
            if (name != null && !name.isEmpty() && !"null".equalsIgnoreCase(name)) {
                result.append("\n- 姓名: ").append(name);
            }
            if (position != null && !position.isEmpty() && !"null".equalsIgnoreCase(position)) {
                result.append("\n- 职位: ").append(position);
            }
            if (phone != null && !phone.isEmpty() && !"null".equalsIgnoreCase(phone)) {
                result.append("\n- 电话: ").append(phone);
            }
            if (email != null && !email.isEmpty() && !"null".equalsIgnoreCase(email)) {
                result.append("\n- 邮箱: ").append(email);
            }

            return result.toString();
        } catch (Exception e) {
            log.error("【Tool调用】updateContact 失败: {}", e.getMessage(), e);
            return "更新联系人失败: " + e.getMessage();
        }
    }

    /**
     * 删除联系人。
     */
    @Tool(description = "删除联系人。当用户要删除、移除某个联系人时调用。需要提供联系人ID。")
    @AiToolPermission(value = "contact:delete", action = "删除联系人")
    public String deleteContact(
            @ToolParam(description = "联系人ID，数字类型，必填") String contactIdStr) {

        log.info("【Tool调用】deleteContact: contactId={}", contactIdStr);

        try {
            if (contactIdStr == null || contactIdStr.isEmpty() || "null".equalsIgnoreCase(contactIdStr)) {
                return "删除联系人失败: 缺少联系人ID参数";
            }

            Long contactId;
            try {
                contactId = Long.parseLong(contactIdStr);
            } catch (NumberFormatException e) {
                return "删除联系人失败: 联系人ID格式无效";
            }

            // 先查联系人信息用于确认
            Contact contact = contactService.getById(contactId);
            if (contact == null) {
                return "删除联系人失败: 联系人不存在";
            }

            String contactName = contact.getName();
            contactService.deleteContact(contactId);

            log.info("【Tool调用】deleteContact 成功: contactId={}, name={}", contactId, contactName);
            return String.format("联系人「%s」已删除成功。", contactName);
        } catch (Exception e) {
            log.error("【Tool调用】deleteContact 失败: {}", e.getMessage(), e);
            return "删除联系人失败: " + e.getMessage();
        }
    }

    private String normalizeRequiredText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty() || "null".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        return normalizeRequiredText(value);
    }

    private boolean parseBooleanFlag(String value) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            return false;
        }
        return "1".equals(normalized)
                || "true".equalsIgnoreCase(normalized)
                || "yes".equalsIgnoreCase(normalized)
                || "y".equalsIgnoreCase(normalized)
                || "是".equals(normalized)
                || "主联系人".equals(normalized);
    }
}
