package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kakarote.ai_crm.entity.BO.ContactQueryBO;
import com.kakarote.ai_crm.entity.PO.Contact;
import com.kakarote.ai_crm.entity.VO.ContactVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 联系人Mapper
 */
@Mapper
public interface ContactMapper extends BaseMapper<Contact> {

    /**
     * 分页查询联系人列表
     */
    IPage<ContactVO> queryPageList(IPage<ContactVO> page, @Param("query") ContactQueryBO query);

    /**
     * 查询主联系人
     */
    Contact getPrimaryContact(@Param("customerId") Long customerId);
}
