package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.CustomFieldSort;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户自定义字段排序Mapper
 */
@Mapper
public interface CustomFieldSortMapper extends BaseMapper<CustomFieldSort> {
}
