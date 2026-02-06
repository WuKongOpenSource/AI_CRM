package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.CustomerTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 客户标签Mapper
 */
@Mapper
public interface CustomerTagMapper extends BaseMapper<CustomerTag> {

    /**
     * 查询客户标签列表
     */
    List<String> getTagsByCustomerId(@Param("customerId") Long customerId);

    /**
     * 删除客户标签
     */
    int deleteByCustomerId(@Param("customerId") Long customerId);
}
