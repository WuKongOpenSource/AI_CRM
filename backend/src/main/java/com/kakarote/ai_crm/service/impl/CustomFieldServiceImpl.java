package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.CustomFieldAddBO;
import com.kakarote.ai_crm.entity.BO.CustomFieldUpdateBO;
import com.kakarote.ai_crm.entity.BO.FieldOption;
import com.kakarote.ai_crm.entity.BO.FieldSortBO;
import com.kakarote.ai_crm.entity.BO.FieldValidation;
import com.kakarote.ai_crm.entity.PO.CustomField;
import com.kakarote.ai_crm.entity.VO.CustomFieldVO;
import com.kakarote.ai_crm.mapper.CustomFieldMapper;
import com.kakarote.ai_crm.entity.PO.CustomFieldPool;
import com.kakarote.ai_crm.service.ICustomFieldPoolService;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.ICustomFieldSortService;
import com.kakarote.ai_crm.service.IDynamicSchemaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 自定义字段服务实现
 */
@Slf4j
@Service
public class CustomFieldServiceImpl extends ServiceImpl<CustomFieldMapper, CustomField>
        implements ICustomFieldService {

    @Autowired
    private IDynamicSchemaService dynamicSchemaService;

    @Autowired
    private ICustomFieldPoolService customFieldPoolService;

    @Lazy
    @Autowired
    private ICustomFieldSortService customFieldSortService;

    /**
     * 单个实体最大自定义字段数
     */
    private static final int MAX_FIELDS_PER_ENTITY = 50;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addField(CustomFieldAddBO bo) {
        // 1. 验证实体类型
        if (!DynamicSchemaServiceImpl.SUPPORTED_ENTITIES.contains(bo.getEntityType())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "不支持的实体类型: " + bo.getEntityType());
        }

        // 2. 检查字段数量限制
        long count = count(new LambdaQueryWrapper<CustomField>()
                .eq(CustomField::getEntityType, bo.getEntityType()));
        if (count >= MAX_FIELDS_PER_ENTITY) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "自定义字段数量已达上限(" + MAX_FIELDS_PER_ENTITY + "个)");
        }

        // 3. 从字段池获取槽位（自动复用或新建物理列）
        CustomFieldPool poolEntry = customFieldPoolService.acquireSlot(bo.getEntityType(), bo.getFieldType());
        String columnName = poolEntry.getColumnName();

        // 4. 保存元数据（fieldName = columnName，不再由用户提供）
        CustomField field = new CustomField();
        field.setEntityType(bo.getEntityType());
        field.setFieldName(columnName);
        field.setFieldLabel(bo.getFieldLabel());
        field.setFieldType(bo.getFieldType());
        field.setColumnName(columnName);
        field.setColumnType(poolEntry.getColumnType());
        field.setDefaultValue(bo.getDefaultValue());
        field.setPlaceholder(bo.getPlaceholder());
        field.setIsRequired(Boolean.TRUE.equals(bo.getIsRequired()) ? 1 : 0);
        field.setIsSearchable(Boolean.TRUE.equals(bo.getIsSearchable()) ? 1 : 0);
        field.setIsShowInList(bo.getIsShowInList() == null || bo.getIsShowInList() ? 1 : 0);
        field.setOptions(bo.getOptions() != null ? JSON.toJSONString(bo.getOptions()) : null);
        field.setValidationRules(bo.getValidation() != null ? JSON.toJSONString(bo.getValidation()) : null);
        field.setSortOrder(bo.getSortOrder() != null ? bo.getSortOrder() : 0);
        field.setStatus(1);
        save(field);

        return field.getFieldId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateField(CustomFieldUpdateBO bo) {
        CustomField field = getById(bo.getFieldId());
        if (ObjectUtil.isNull(field)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "字段不存在");
        }

        // 只更新允许修改的字段
        if (StrUtil.isNotEmpty(bo.getFieldLabel())) {
            field.setFieldLabel(bo.getFieldLabel());
        }
        if (bo.getDefaultValue() != null) {
            field.setDefaultValue(bo.getDefaultValue());
        }
        if (bo.getPlaceholder() != null) {
            field.setPlaceholder(bo.getPlaceholder());
        }
        if (bo.getIsRequired() != null) {
            field.setIsRequired(bo.getIsRequired() ? 1 : 0);
        }
        if (bo.getIsSearchable() != null) {
            field.setIsSearchable(bo.getIsSearchable() ? 1 : 0);
        }
        if (bo.getIsShowInList() != null) {
            field.setIsShowInList(bo.getIsShowInList() ? 1 : 0);
        }
        if (bo.getOptions() != null) {
            field.setOptions(JSON.toJSONString(bo.getOptions()));
        }
        if (bo.getValidation() != null) {
            field.setValidationRules(JSON.toJSONString(bo.getValidation()));
        }
        if (bo.getSortOrder() != null) {
            field.setSortOrder(bo.getSortOrder());
        }

        updateById(field);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableField(Long fieldId) {
        CustomField field = getById(fieldId);
        if (ObjectUtil.isNull(field)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "字段不存在");
        }
        field.setStatus(0);
        updateById(field);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableField(Long fieldId) {
        CustomField field = getById(fieldId);
        if (ObjectUtil.isNull(field)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "字段不存在");
        }
        field.setStatus(1);
        updateById(field);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteField(Long fieldId) {
        CustomField field = getById(fieldId);
        if (ObjectUtil.isNull(field)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "字段不存在");
        }

        // 仅删除元数据，物理列保留在字段池中供其他租户或未来复用
        removeById(fieldId);

        // 清理该字段的所有用户排序记录
        customFieldSortService.removeByFieldId(fieldId);
    }

    @Override
    public List<CustomFieldVO> getFieldsByEntity(String entityType) {
        List<CustomField> fields = list(new LambdaQueryWrapper<CustomField>()
                .eq(CustomField::getEntityType, entityType)
                .orderByAsc(CustomField::getSortOrder)
                .orderByAsc(CustomField::getCreateTime));
        return convertToVO(fields);
    }

    @Override
    public List<CustomFieldVO> getEnabledFieldsByEntity(String entityType) {
        List<CustomField> fields = list(new LambdaQueryWrapper<CustomField>()
                .eq(CustomField::getEntityType, entityType)
                .eq(CustomField::getStatus, 1)
                .orderByAsc(CustomField::getSortOrder)
                .orderByAsc(CustomField::getCreateTime));
        return convertToVO(fields);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSortOrder(List<FieldSortBO> sortList) {
        if (sortList == null || sortList.isEmpty()) {
            return;
        }
        for (FieldSortBO item : sortList) {
            CustomField field = new CustomField();
            field.setFieldId(item.getFieldId());
            field.setSortOrder(item.getSortOrder());
            updateById(field);
        }
    }

    @Override
    public Map<String, Object> getCustomFieldValues(String entityType, Long entityId) {
        List<CustomFieldVO> fields = getEnabledFieldsByEntity(entityType);
        if (fields.isEmpty()) {
            return Collections.emptyMap();
        }

        String tableName = dynamicSchemaService.getTableName(entityType);
        String idColumn = dynamicSchemaService.getIdColumnName(entityType);

        // 过滤掉数据库中不存在的列，防止SQL错误
        List<String> columnNames = new ArrayList<>();
        for (CustomFieldVO field : fields) {
            if (dynamicSchemaService.columnExists(tableName, field.getColumnName())) {
                columnNames.add(field.getColumnName());
            } else {
                log.warn("自定义字段列不存在: {}.{}, fieldName={}", tableName, field.getColumnName(), field.getFieldName());
            }
        }

        if (columnNames.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> columnValues = baseMapper.getCustomFieldValues(tableName, idColumn, entityId, columnNames);
        if (columnValues == null) {
            return Collections.emptyMap();
        }

        // 将列名转换为字段名
        Map<String, Object> result = new HashMap<>();
        for (CustomFieldVO field : fields) {
            Object value = columnValues.get(field.getColumnName());
            if (value != null) {
                result.put(field.getFieldName(), value);
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomFieldValues(String entityType, Long entityId, Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return;
        }

        List<CustomFieldVO> fields = getEnabledFieldsByEntity(entityType);
        if (fields.isEmpty()) {
            return;
        }

        // 构建fieldName -> columnName的映射
        Map<String, String> fieldToColumn = fields.stream()
                .collect(Collectors.toMap(CustomFieldVO::getFieldName, CustomFieldVO::getColumnName));

        // 构建fieldName -> fieldType的映射，用于值类型转换
        Map<String, String> fieldToType = fields.stream()
                .collect(Collectors.toMap(CustomFieldVO::getFieldName, CustomFieldVO::getFieldType));

        // 将字段名转换为列名，过滤空值，按字段类型做 Java 类型转换
        Map<String, Object> columnValues = new HashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String columnName = fieldToColumn.get(entry.getKey());
            if (columnName == null) {
                continue;
            }
            Object val = entry.getValue();
            if (val == null || "".equals(val)) {
                continue;
            }
            String fieldType = fieldToType.get(entry.getKey());
            val = convertValueForJdbc(fieldType, val, entry.getKey());
            if (val != null) {
                columnValues.put(columnName, val);
            }
        }

        if (columnValues.isEmpty()) {
            return;
        }

        String tableName = dynamicSchemaService.getTableName(entityType);
        String idColumn = dynamicSchemaService.getIdColumnName(entityType);

        baseMapper.updateCustomFieldValues(tableName, idColumn, entityId, columnValues);
    }

    @Override
    public Map<Long, Map<String, Object>> getBatchCustomFieldValues(String entityType, List<Long> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<CustomFieldVO> fields = getEnabledFieldsByEntity(entityType);
        if (fields.isEmpty()) {
            return Collections.emptyMap();
        }

        String tableName = dynamicSchemaService.getTableName(entityType);
        String idColumn = dynamicSchemaService.getIdColumnName(entityType);

        // 过滤掉数据库中不存在的列（只检查一次）
        List<String> columnNames = new ArrayList<>();
        Map<String, String> columnToField = new HashMap<>();
        for (CustomFieldVO field : fields) {
            if (dynamicSchemaService.columnExists(tableName, field.getColumnName())) {
                columnNames.add(field.getColumnName());
                columnToField.put(field.getColumnName(), field.getFieldName());
            }
        }

        if (columnNames.isEmpty()) {
            return Collections.emptyMap();
        }

        // 一次批量查询
        List<Map<String, Object>> rows = baseMapper.getBatchCustomFieldValues(tableName, idColumn, entityIds, columnNames);

        // 按entityId分组，列名转字段名
        Map<Long, Map<String, Object>> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object idObj = row.get("entity_id");
            if (idObj == null) continue;
            Long entityId = Long.valueOf(idObj.toString());

            Map<String, Object> fieldValues = new HashMap<>();
            for (Map.Entry<String, String> entry : columnToField.entrySet()) {
                Object value = row.get(entry.getKey());
                if (value != null) {
                    fieldValues.put(entry.getValue(), value);
                }
            }
            result.put(entityId, fieldValues);
        }

        return result;
    }

    /**
     * 按字段类型将前端传入的值转换为 JDBC 兼容的 Java 类型
     * text/textarea/select → String
     * number → BigDecimal
     * date → java.sql.Date
     * datetime → java.sql.Timestamp
     * checkbox → Boolean
     * multiselect (List) → JSON String
     */
    private Object convertValueForJdbc(String fieldType, Object val, String fieldName) {
        if (val == null || fieldType == null) {
            return val;
        }
        try {
            return switch (fieldType) {
                case "number" -> {
                    if (val instanceof Number n) yield new java.math.BigDecimal(n.toString());
                    yield new java.math.BigDecimal(val.toString());
                }
                case "date" -> {
                    // "2026-03-28" → java.sql.Date
                    if (val instanceof java.sql.Date) yield val;
                    if (val instanceof java.util.Date d) yield new java.sql.Date(d.getTime());
                    yield java.sql.Date.valueOf(val.toString());
                }
                case "datetime" -> {
                    // "2026-03-28 17:00:00" → java.sql.Timestamp
                    if (val instanceof java.sql.Timestamp) yield val;
                    if (val instanceof java.util.Date d) yield new java.sql.Timestamp(d.getTime());
                    yield java.sql.Timestamp.valueOf(val.toString());
                }
                case "checkbox" -> {
                    if (val instanceof Boolean) yield val;
                    String s = val.toString();
                    yield "true".equalsIgnoreCase(s) || "1".equals(s);
                }
                case "multiselect" -> {
                    if (val instanceof List) yield JSON.toJSONString(val);
                    yield val.toString();
                }
                default -> val.toString(); // text, textarea, select
            };
        } catch (Exception e) {
            log.warn("自定义字段值转换失败: field={}, type={}, value={}, error={}", fieldName, fieldType, val, e.getMessage());
            return null;
        }
    }

    /**
     * 将PO列表转换为VO列表
     */
    private List<CustomFieldVO> convertToVO(List<CustomField> fields) {
        return fields.stream().map(field -> {
            CustomFieldVO vo = BeanUtil.copyProperties(field, CustomFieldVO.class,"options");
            vo.setIsRequired(field.getIsRequired() != null && field.getIsRequired() == 1);
            vo.setIsSearchable(field.getIsSearchable() != null && field.getIsSearchable() == 1);
            vo.setIsShowInList(field.getIsShowInList() != null && field.getIsShowInList() == 1);

            // 解析options JSON
            if (StrUtil.isNotEmpty(field.getOptions())) {
                vo.setOptions(JSON.parseArray(field.getOptions(), FieldOption.class));
            }
            // 解析validation JSON
            if (StrUtil.isNotEmpty(field.getValidationRules())) {
                vo.setValidation(JSON.parseObject(field.getValidationRules(), FieldValidation.class));
            }
            return vo;
        }).collect(Collectors.toList());
    }

}
