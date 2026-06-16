package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.ContactAddBO;
import com.kakarote.ai_crm.entity.BO.ContactQueryBO;
import com.kakarote.ai_crm.entity.BO.ContactUpdateBO;
import com.kakarote.ai_crm.entity.BO.CustomerAiParseBO;
import com.kakarote.ai_crm.entity.PO.Contact;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.VO.ContactVO;
import com.kakarote.ai_crm.entity.VO.CustomerAiParseVO;
import com.kakarote.ai_crm.mapper.ContactMapper;
import com.kakarote.ai_crm.service.IContactService;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.IGlobalSearchIndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 联系人服务实现
 */
@Service
public class ContactServiceImpl extends ServiceImpl<ContactMapper, Contact> implements IContactService {

    @Autowired
    private ICustomFieldService customFieldService;

    @Autowired
    @Lazy
    private CustomerServiceImpl customerService;

    @Autowired
    private IGlobalSearchIndexService globalSearchIndexService;

    /**
     * 新增联系人。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addContact(ContactAddBO contactAddBO) {
        Customer customer = customerService.getById(contactAddBO.getCustomerId());
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在或无权限访问");
        }
        Contact contact = BeanUtil.copyProperties(contactAddBO, Contact.class);
        contact.setStatus(1);
        if (contact.getIsPrimary() == null) {
            contact.setIsPrimary(0);
        }
        customFieldService.validateUniqueCustomFieldValues("contact", null,
                buildContactUniqueFieldValues(contact, contactAddBO.getCustomFields()));
        save(contact);
        // 若设为主联系人，重置该客户下其他联系人的主联系人标记
        if (contact.getIsPrimary() != null && contact.getIsPrimary() == 1) {
            lambdaUpdate()
                .eq(Contact::getCustomerId, contact.getCustomerId())
                .ne(Contact::getContactId, contact.getContactId())
                .set(Contact::getIsPrimary, 0)
                .update();
        }
        // 保存自定义字段
        if (contactAddBO.getCustomFields() != null && !contactAddBO.getCustomFields().isEmpty()) {
            customFieldService.updateCustomFieldValues("contact", contact.getContactId(), contactAddBO.getCustomFields());
        }
        // 同步客户冗余字段
        customerService.syncContactCache(contact.getCustomerId());
        globalSearchIndexService.refreshContactIndex(contact.getContactId());
        return contact.getContactId();
    }

    /**
     * 更新联系人。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContact(ContactUpdateBO contactUpdateBO) {
        Contact contact = getById(contactUpdateBO.getContactId());
        if (ObjectUtil.isNull(contact)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "联系人不存在");
        }
        Long oldCustomerId = contact.getCustomerId();
        BeanUtil.copyProperties(contactUpdateBO, contact, "contactId", "createUserId", "createTime", "customFields");
        customFieldService.validateUniqueCustomFieldValues("contact", contact.getContactId(),
                buildContactUniqueFieldValues(contact, contactUpdateBO.getCustomFields()));
        updateById(contact);

        // 若设为主联系人，重置该客户下其他联系人的主联系人标记
        if (contact.getIsPrimary() != null && contact.getIsPrimary() == 1) {
            lambdaUpdate()
                .eq(Contact::getCustomerId, contact.getCustomerId())
                .ne(Contact::getContactId, contact.getContactId())
                .set(Contact::getIsPrimary, 0)
                .update();
        }

        // 更新自定义字段
        if (contactUpdateBO.getCustomFields() != null && !contactUpdateBO.getCustomFields().isEmpty()) {
            customFieldService.updateCustomFieldValues("contact", contactUpdateBO.getContactId(), contactUpdateBO.getCustomFields());
        }
        // 同步客户冗余字段
        if (oldCustomerId != null && !oldCustomerId.equals(contact.getCustomerId())) {
            customerService.syncContactCache(oldCustomerId);
        }
        customerService.syncContactCache(contact.getCustomerId());
        globalSearchIndexService.refreshContactIndex(contact.getContactId());
    }

    /**
     * 构建联系人Unique字段值。
     */
    private Map<String, Object> buildContactUniqueFieldValues(Contact contact, Map<String, Object> customFields) {
        Map<String, Object> values = new HashMap<>();
        if (contact != null) {
            values.put("name", contact.getName());
            values.put("position", contact.getPosition());
            values.put("phone", contact.getPhone());
            values.put("email", contact.getEmail());
            values.put("wechat", contact.getWechat());
            values.put("isPrimary", contact.getIsPrimary());
            values.put("notes", contact.getNotes());
        }
        if (customFields != null && !customFields.isEmpty()) {
            values.putAll(customFields);
        }
        return values;
    }

    /**
     * 删除联系人。
     */
    @Override
    public void deleteContact(Long contactId) {
        Contact contact = getById(contactId);
        if (ObjectUtil.isNull(contact)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "联系人不存在");
        }
        Long customerId = contact.getCustomerId();
        removeById(contactId);
        // 同步客户冗余字段
        customerService.syncContactCache(customerId);
        globalSearchIndexService.deleteByEntity("contact", contactId);
    }

    /**
     * 按客户查询联系人。
     */
    @Override
    public List<ContactVO> queryByCustomer(Long customerId) {
        List<Contact> contacts = lambdaQuery()
            .eq(Contact::getCustomerId, customerId)
            .eq(Contact::getStatus, 1)
            .orderByDesc(Contact::getIsPrimary)
            .orderByDesc(Contact::getCreateTime)
            .list();
        List<ContactVO> voList = BeanUtil.copyToList(contacts, ContactVO.class);

        // 批量获取联系人的自定义字段（避免N+1）
        List<Long> contactIds = voList.stream().map(ContactVO::getContactId).toList();
        Map<Long, Map<String, Object>> cfMap = customFieldService.getBatchCustomFieldValues("contact", contactIds);
        for (ContactVO vo : voList) {
            vo.setCustomFields(cfMap.getOrDefault(vo.getContactId(), Collections.emptyMap()));
        }

        return voList;
    }

    /**
     * 设置主联系人。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setPrimary(Long contactId) {
        Contact contact = getById(contactId);
        if (ObjectUtil.isNull(contact)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "联系人不存在");
        }

        // Reset all contacts for this customer
        lambdaUpdate()
            .eq(Contact::getCustomerId, contact.getCustomerId())
            .set(Contact::getIsPrimary, 0)
            .update();

        // Set this one as primary
        contact.setIsPrimary(1);
        updateById(contact);
        // 同步客户冗余字段
        customerService.syncContactCache(contact.getCustomerId());
        globalSearchIndexService.refreshContactIndex(contactId);
    }

    /**
     * 分页查询联系人列表。
     */
    @Override
    public BasePage<ContactVO> queryPageList(ContactQueryBO queryBO) {
        BasePage<ContactVO> page = queryBO.parse();
        baseMapper.queryPageList(page, queryBO);

        List<ContactVO> records = page.getRecords();
        if (records != null && !records.isEmpty()) {
            List<Long> contactIds = records.stream().map(ContactVO::getContactId).toList();
            Map<Long, Map<String, Object>> cfMap = customFieldService.getBatchCustomFieldValues("contact", contactIds);
            for (ContactVO vo : records) {
                vo.setCustomFields(cfMap.getOrDefault(vo.getContactId(), Collections.emptyMap()));
            }
        }

        return page;
    }

    /**
     * 使用 AI 解析联系人。
     */
    @Override
    public CustomerAiParseVO aiParseContact(CustomerAiParseBO parseBO) {
        return customerService.aiParseCustomer(parseBO);
    }
}
