package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.BO.CustomFieldAddBO;
import com.kakarote.ai_crm.entity.BO.CustomFieldUpdateBO;
import com.kakarote.ai_crm.entity.BO.FieldSortBO;
import com.kakarote.ai_crm.entity.PO.CustomField;
import com.kakarote.ai_crm.entity.VO.CustomFieldVO;

import java.util.List;
import java.util.Map;

/**
 * 自定义字段服务接口
 */
public interface ICustomFieldService extends IService<CustomField> {

    /**
     * 添加自定义字段（包含 ALTER TABLE）
     *
     * @param bo 添加请求对象
     * @return 字段ID
     */
    Long addField(CustomFieldAddBO bo);

    /**
     * 更新字段（只能更新标签、选项、验证规则等，不能修改类型）
     *
     * @param bo 更新请求对象
     */
    void updateField(CustomFieldUpdateBO bo);

    /**
     * 禁用字段（不删除列，只标记禁用）
     *
     * @param fieldId 字段ID
     */
    void disableField(Long fieldId);

    /**
     * 启用字段
     *
     * @param fieldId 字段ID
     */
    void enableField(Long fieldId);

    /**
     * 删除字段（真正删除列，需谨慎）
     *
     * @param fieldId 字段ID
     */
    void deleteField(Long fieldId);

    /**
     * 查询实体的所有字段定义
     *
     * @param entityType 实体类型
     * @return 字段列表
     */
    List<CustomFieldVO> getFieldsByEntity(String entityType);

    /**
     * 查询实体的已启用字段
     *
     * @param entityType 实体类型
     * @return 字段列表
     */
    List<CustomFieldVO> getEnabledFieldsByEntity(String entityType);

    /**
     * 调整字段排序
     *
     * @param sortList 排序列表
     */
    void updateSortOrder(List<FieldSortBO> sortList);

    /**
     * 获取实体的自定义字段值
     *
     * @param entityType 实体类型
     * @param entityId   实体ID
     * @return 自定义字段值Map（fieldName -> value）
     */
    Map<String, Object> getCustomFieldValues(String entityType, Long entityId);

    /**
     * 更新实体的自定义字段值
     *
     * @param entityType 实体类型
     * @param entityId   实体ID
     * @param values     自定义字段值Map（fieldName -> value）
     */
    void updateCustomFieldValues(String entityType, Long entityId, Map<String, Object> values);
}
