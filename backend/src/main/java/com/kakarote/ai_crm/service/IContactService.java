package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.ContactAddBO;
import com.kakarote.ai_crm.entity.BO.ContactQueryBO;
import com.kakarote.ai_crm.entity.BO.ContactUpdateBO;
import com.kakarote.ai_crm.entity.PO.Contact;
import com.kakarote.ai_crm.entity.VO.ContactVO;

import java.util.List;

/**
 * 联系人服务接口
 */
public interface IContactService extends IService<Contact> {

    /**
     * 添加联系人
     */
    Long addContact(ContactAddBO contactAddBO);

    /**
     * 更新联系人
     */
    void updateContact(ContactUpdateBO contactUpdateBO);

    /**
     * 删除联系人
     */
    void deleteContact(Long contactId);

    /**
     * 按客户查询联系人
     */
    List<ContactVO> queryByCustomer(Long customerId);

    /**
     * 设置为主联系人
     */
    void setPrimary(Long contactId);

    /**
     * 分页查询联系人
     */
    BasePage<ContactVO> queryPageList(ContactQueryBO queryBO);
}
