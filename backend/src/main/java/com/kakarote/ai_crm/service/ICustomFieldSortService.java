package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.BO.FieldSortUpdateBO;
import com.kakarote.ai_crm.entity.PO.CustomFieldSort;
import com.kakarote.ai_crm.entity.VO.CustomFieldVO;

import java.util.List;

/**
 * 用户自定义字段排序服务接口
 */
public interface ICustomFieldSortService extends IService<CustomFieldSort> {

    /**
     * 获取当前用户的列表列配置（已排序、过滤隐藏字段）
     */
    List<CustomFieldVO> getUserFieldConfig(String entityType);

    /**
     * 获取当前用户的全部字段配置（含隐藏标记），用于设置界面
     */
    List<CustomFieldVO> getUserAllFieldConfig(String entityType);

    /**
     * 批量保存用户的字段排序和显隐配置
     */
    void saveUserFieldConfig(FieldSortUpdateBO bo);

    /**
     * 删除指定字段的所有用户排序记录（字段被删除时调用）
     */
    void removeByFieldId(Long fieldId);
}
