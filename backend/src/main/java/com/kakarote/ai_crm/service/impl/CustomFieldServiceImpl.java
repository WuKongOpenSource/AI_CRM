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
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.IDynamicSchemaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * 字段类型 -> MySQL列类型映射
     */
    private static final Map<String, String> TYPE_MAPPING = Map.of(
            "text", "VARCHAR(500)",
            "textarea", "TEXT",
            "number", "DECIMAL(15,2)",
            "date", "DATE",
            "datetime", "DATETIME",
            "select", "VARCHAR(100)",
            "multiselect", "TEXT",
            "checkbox", "TINYINT"
    );

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

        // 3. 检查字段标识是否已存在
        Long existCount = count(new LambdaQueryWrapper<CustomField>()
                .eq(CustomField::getEntityType, bo.getEntityType())
                .eq(CustomField::getFieldName, bo.getFieldName()));
        if (existCount > 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "字段标识已存在: " + bo.getFieldName());
        }

        // 4. 生成列名（camelCase -> snake_case，加 cf_ 前缀）
        String columnName = "cf_" + toSnakeCase(bo.getFieldName());

        // 5. 获取列类型
        String columnType = TYPE_MAPPING.get(bo.getFieldType());
        if (columnType == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "不支持的字段类型: " + bo.getFieldType());
        }

        // 6. 执行 ALTER TABLE
        String tableName = dynamicSchemaService.getTableName(bo.getEntityType());
        dynamicSchemaService.addColumn(tableName, columnName, columnType, bo.getFieldLabel());

        // 7. 保存元数据
        CustomField field = new CustomField();
        field.setEntityType(bo.getEntityType());
        field.setFieldName(bo.getFieldName());
        field.setFieldLabel(bo.getFieldLabel());
        field.setFieldType(bo.getFieldType());
        field.setColumnName(columnName);
        field.setColumnType(columnType);
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

        // 执行 DROP COLUMN
        String tableName = dynamicSchemaService.getTableName(field.getEntityType());
        dynamicSchemaService.dropColumn(tableName, field.getColumnName());

        // 删除元数据
        removeById(fieldId);
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

        // 将字段名转换为列名
        Map<String, Object> columnValues = new HashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String columnName = fieldToColumn.get(entry.getKey());
            if (columnName != null) {
                columnValues.put(columnName, entry.getValue());
            }
        }

        if (columnValues.isEmpty()) {
            return;
        }

        String tableName = dynamicSchemaService.getTableName(entityType);
        String idColumn = dynamicSchemaService.getIdColumnName(entityType);

        baseMapper.updateCustomFieldValues(tableName, idColumn, entityId, columnValues);
    }

    /**
     * 将PO列表转换为VO列表
     */
    private List<CustomFieldVO> convertToVO(List<CustomField> fields) {
        return fields.stream().map(field -> {
            CustomFieldVO vo = BeanUtil.copyProperties(field, CustomFieldVO.class);
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

    /**
     * 将 camelCase 转换为 snake_case
     */
    private String toSnakeCase(String camelCase) {
        if (StrUtil.isEmpty(camelCase)) {
            return camelCase;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char ch = camelCase.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
