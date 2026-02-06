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
import com.kakarote.ai_crm.entity.PO.Contact;
import com.kakarote.ai_crm.entity.VO.ContactVO;
import com.kakarote.ai_crm.mapper.ContactMapper;
import com.kakarote.ai_crm.service.IContactService;
import com.kakarote.ai_crm.service.ICustomFieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 联系人服务实现
 */
@Service
public class ContactServiceImpl extends ServiceImpl<ContactMapper, Contact> implements IContactService {

    @Autowired
    private ICustomFieldService customFieldService;

    @Override
    public Long addContact(ContactAddBO contactAddBO) {
        Contact contact = BeanUtil.copyProperties(contactAddBO, Contact.class);
        contact.setStatus(1);
        if (contact.getIsPrimary() == null) {
            contact.setIsPrimary(0);
        }
        save(contact);
        return contact.getContactId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContact(ContactUpdateBO contactUpdateBO) {
        Contact contact = getById(contactUpdateBO.getContactId());
        if (ObjectUtil.isNull(contact)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "联系人不存在");
        }
        BeanUtil.copyProperties(contactUpdateBO, contact, "contactId", "createUserId", "createTime", "customFields");
        updateById(contact);

        // 更新自定义字段
        if (contactUpdateBO.getCustomFields() != null && !contactUpdateBO.getCustomFields().isEmpty()) {
            customFieldService.updateCustomFieldValues("contact", contactUpdateBO.getContactId(), contactUpdateBO.getCustomFields());
        }
    }

    @Override
    public void deleteContact(Long contactId) {
        Contact contact = getById(contactId);
        if (ObjectUtil.isNull(contact)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "联系人不存在");
        }
        removeById(contactId);
    }

    @Override
    public List<ContactVO> queryByCustomer(Long customerId) {
        List<Contact> contacts = lambdaQuery()
            .eq(Contact::getCustomerId, customerId)
            .eq(Contact::getStatus, 1)
            .orderByDesc(Contact::getIsPrimary)
            .orderByDesc(Contact::getCreateTime)
            .list();
        List<ContactVO> voList = BeanUtil.copyToList(contacts, ContactVO.class);

        // 获取每个联系人的自定义字段
        for (ContactVO vo : voList) {
            Map<String, Object> customFields = customFieldService.getCustomFieldValues("contact", vo.getContactId());
            vo.setCustomFields(customFields);
        }

        return voList;
    }

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
    }

    @Override
    public BasePage<ContactVO> queryPageList(ContactQueryBO queryBO) {
        BasePage<ContactVO> page = queryBO.parse();
        baseMapper.queryPageList(page, queryBO);
        return page;
    }
}
