package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.ContactTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 联系人标签Mapper
 */
@Mapper
public interface ContactTagMapper extends BaseMapper<ContactTag> {

    /**
     * 查询联系人标签列表
     */
    List<String> getTagsByContactId(@Param("contactId") Long contactId);

    /**
     * 删除联系人标签
     */
    int deleteByContactId(@Param("contactId") Long contactId);
}
