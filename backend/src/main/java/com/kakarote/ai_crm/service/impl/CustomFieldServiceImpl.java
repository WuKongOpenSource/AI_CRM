package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
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

    @Lazy
    @Autowired
    private CustomerServiceImpl customerService;

    private static final Set<String> CUSTOMER_SEARCHABLE_TEXT_FIELD_TYPES = Set.of("text", "textarea", "select", "multiselect");
    private static final String FIELD_SOURCE_SYSTEM = "system";
    private static final String FIELD_SOURCE_CUSTOM = "custom";
    private static final Object SYSTEM_FIELD_INIT_LOCK = new Object();
    private static final Map<String, List<SystemFieldDefinition>> SYSTEM_FIELD_DEFINITIONS = Map.of(
            "customer", List.of(
                    systemField("companyName", "公司名称", "text", "company_name", "VARCHAR(255)", null, "请输入公司名称", true, true, true, null, 10),
                    systemField("industry", "所属行业", "text", "industry", "VARCHAR(100)", null, "请输入所属行业", false, true, true, null, 20),
                    systemField("stage", "商机阶段", "select", "stage", "VARCHAR(50)", "lead", null, false, false, true, List.of(
                            option("lead", "线索"),
                            option("qualified", "资格审核"),
                            option("proposal", "方案报价"),
                            option("negotiation", "谈判中"),
                            option("closed", "已成交"),
                            option("lost", "已流失")
                    ), 30),
                    systemField("level", "客户级别", "select", "level", "CHAR(1)", null, null, false, false, true, List.of(
                            option("A", "A级客户"),
                            option("B", "B级客户"),
                            option("C", "C级客户")
                    ), 40),
                    systemField("source", "来源", "text", "source", "VARCHAR(100)", null, "请输入客户来源", false, true, true, null, 50),
                    systemField("website", "网站", "text", "website", "VARCHAR(255)", null, "请输入网站地址", false, false, true, null, 60),
                    systemField("quotation", "预计成交金额", "number", "quotation", "DECIMAL(15,2)", null, "请输入预计成交金额", false, false, true, null, 70),
                    systemField("address", "地址", "textarea", "address", "VARCHAR(500)", null, "请输入客户地址", false, false, false, null, 100),
                    systemField("nextFollowTime", "下次跟进时间", "datetime", "next_follow_time", "TIMESTAMP", null, "请选择下次跟进时间", false, false, true, null, 110),
                    systemField("remark", "备注", "textarea", "remark", "TEXT", null, "请输入备注", false, false, false, null, 120)
            ),
            "contact", List.of(
                    systemField("name", "姓名", "text", "name", "VARCHAR(100)", null, "请输入姓名", true, true, true, null, 10),
                    systemField("position", "职位", "text", "position", "VARCHAR(100)", null, "请输入职位", false, true, true, null, 20),
                    systemField("phone", "电话", "text", "phone", "VARCHAR(50)", null, "请输入电话", false, true, true, null, 30),
                    systemField("email", "邮箱", "text", "email", "VARCHAR(100)", null, "请输入邮箱", false, true, true, null, 40),
                    systemField("wechat", "微信", "text", "wechat", "VARCHAR(100)", null, "请输入微信号", false, true, false, null, 50),
                    systemField("isPrimary", "主联系人", "checkbox", "is_primary", "SMALLINT", "0", null, false, false, true, null, 60),
                    systemField("notes", "备注", "textarea", "notes", "TEXT", null, "请输入备注", false, false, false, null, 70)
            )
    );
    /*

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
                .eq(CustomField::getEntityType, bo.getEntityType())
                .and(this::appendCustomFieldSourceCondition));
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
        field.setFieldSource(FIELD_SOURCE_CUSTOM);
        field.setColumnName(columnName);
        field.setColumnType(poolEntry.getColumnType());
        field.setDefaultValue(bo.getDefaultValue());
        field.setPlaceholder(bo.getPlaceholder());
        field.setIsRequired(Boolean.TRUE.equals(bo.getIsRequired()) ? 1 : 0);
        field.setIsSearchable(Boolean.TRUE.equals(bo.getIsSearchable()) ? 1 : 0);
        field.setIsShowInList(bo.getIsShowInList() == null || bo.getIsShowInList() ? 1 : 0);
        field.setIsUnique(Boolean.TRUE.equals(bo.getIsUnique()) ? 1 : 0);
        field.setOptions(bo.getOptions() != null ? JSON.toJSONString(bo.getOptions()) : null);
        field.setValidationRules(bo.getValidation() != null ? JSON.toJSONString(bo.getValidation()) : null);
        field.setSortOrder(bo.getSortOrder() != null ? bo.getSortOrder() : getNextSortOrder(bo.getEntityType()));
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
        if (bo.getIsUnique() != null) {
            field.setIsUnique(bo.getIsUnique() ? 1 : 0);
        }
        if (bo.getOptions() != null && !isSystemField(field)) {
            field.setOptions(JSON.toJSONString(bo.getOptions()));
        }
        if (bo.getValidation() != null && !isSystemField(field)) {
            field.setValidationRules(JSON.toJSONString(bo.getValidation()));
        }
        if (bo.getSortOrder() != null) {
            field.setSortOrder(bo.getSortOrder());
        }

        boolean needsRefresh = isCustomField(field) && shouldRefreshCustomerSearchText(
                field.getEntityType(),
                field.getFieldType(),
                field.getIsSearchable() != null && field.getIsSearchable() == 1
        ) && bo.getIsSearchable() != null;

        updateById(field);

        if (needsRefresh) {
            customerService.refreshAllCustomerSearchText();
        }
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

        if (isCustomField(field)
                && shouldRefreshCustomerSearchText(field.getEntityType(), field.getFieldType(), field.getIsSearchable() != null && field.getIsSearchable() == 1)) {
            customerService.refreshAllCustomerSearchText();
        }
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

        if (isCustomField(field)
                && shouldRefreshCustomerSearchText(field.getEntityType(), field.getFieldType(), field.getIsSearchable() != null && field.getIsSearchable() == 1)) {
            customerService.refreshAllCustomerSearchText();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteField(Long fieldId) {
        CustomField field = getById(fieldId);
        if (ObjectUtil.isNull(field)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "字段不存在");
        }

        // 仅删除元数据，物理列保留在字段池中供其他租户或未来复用
        if (isSystemField(field)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "系统字段不支持删除");
        }

        removeById(fieldId);

        // 清理该字段的所有用户排序记录
        customFieldSortService.removeByFieldId(fieldId);

        if (isCustomField(field)
                && shouldRefreshCustomerSearchText(field.getEntityType(), field.getFieldType(), field.getIsSearchable() != null && field.getIsSearchable() == 1)) {
            customerService.refreshAllCustomerSearchText();
        }
    }

    @Override
    public List<CustomFieldVO> getFieldsByEntity(String entityType) {
        initializeSystemFields(entityType);
        List<CustomField> fields = list(new LambdaQueryWrapper<CustomField>()
                .eq(CustomField::getEntityType, entityType)
                .orderByAsc(CustomField::getSortOrder)
                .orderByAsc(CustomField::getCreateTime));
        return convertToVO(fields);
    }

    @Override
    public List<CustomFieldVO> getEnabledFieldsByEntity(String entityType) {
        initializeSystemFields(entityType);
        List<CustomField> fields = list(new LambdaQueryWrapper<CustomField>()
                .eq(CustomField::getEntityType, entityType)
                .eq(CustomField::getStatus, 1)
                .orderByAsc(CustomField::getSortOrder)
                .orderByAsc(CustomField::getCreateTime));
        return convertToVO(fields);
    }

    @Override
    public List<CustomFieldVO> getListFieldsByEntity(String entityType) {
        initializeSystemFields(entityType);
        List<CustomField> fields = list(new LambdaQueryWrapper<CustomField>()
                .eq(CustomField::getEntityType, entityType)
                .eq(CustomField::getStatus, 1)
                .eq(CustomField::getIsShowInList, 1)
                .orderByAsc(CustomField::getSortOrder)
                .orderByAsc(CustomField::getCreateTime));
        return convertToVO(fields);
    }

    @Override
    public List<CustomFieldVO> getFormFieldsByEntity(String entityType) {
        initializeSystemFields(entityType);
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
        List<CustomFieldVO> fields = getEnabledCustomFieldVOs(entityType);
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

        List<CustomFieldVO> fields = getEnabledCustomFieldVOs(entityType);
        if (fields.isEmpty()) {
            return;
        }

        Map<String, CustomFieldVO> fieldMap = fields.stream()
                .collect(Collectors.toMap(CustomFieldVO::getFieldName, field -> field));

        String tableName = dynamicSchemaService.getTableName(entityType);
        String idColumn = dynamicSchemaService.getIdColumnName(entityType);

        // 将字段名转换为列名，过滤空值，按字段类型做 Java 类型转换
        Map<String, Object> columnValues = new HashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            CustomFieldVO field = fieldMap.get(entry.getKey());
            if (field == null) {
                continue;
            }
            Object val = entry.getValue();
            if (val == null || "".equals(val)) {
                continue;
            }
            val = convertValueForJdbc(field, val, entry.getKey());
            if (val != null) {
                validateUniqueCustomFieldValue(tableName, idColumn, entityId, field, val);
                columnValues.put(field.getColumnName(), val);
            }
        }

        if (columnValues.isEmpty()) {
            return;
        }

        baseMapper.updateCustomFieldValues(tableName, idColumn, entityId, columnValues);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomFieldValue(String entityType, Long entityId, String fieldName, Object value) {
        if (entityId == null || StrUtil.isBlank(fieldName)) {
            return;
        }

        CustomFieldVO field = getEnabledCustomFieldVOs(entityType).stream()
                .filter(item -> fieldName.equals(item.getFieldName()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Custom field not found"));

        String tableName = dynamicSchemaService.getTableName(entityType);
        String idColumn = dynamicSchemaService.getIdColumnName(entityType);
        if (!dynamicSchemaService.columnExists(tableName, field.getColumnName())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Custom field column not found");
        }

        Object jdbcValue = "".equals(value) ? null : convertValueForJdbc(field, value, fieldName);
        validateUniqueCustomFieldValue(tableName, idColumn, entityId, field, jdbcValue);
        Map<String, Object> columnValues = new HashMap<>();
        columnValues.put(field.getColumnName(), jdbcValue);
        baseMapper.updateCustomFieldValues(tableName, idColumn, entityId, columnValues);
    }

    @Override
    public void validateUniqueCustomFieldValues(String entityType, Long entityId, Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return;
        }

        List<CustomFieldVO> fields = getEnabledFieldsByEntity(entityType);
        if (fields.isEmpty()) {
            return;
        }

        Map<String, CustomFieldVO> fieldMap = fields.stream()
                .collect(Collectors.toMap(CustomFieldVO::getFieldName, field -> field));
        String tableName = dynamicSchemaService.getTableName(entityType);
        String idColumn = dynamicSchemaService.getIdColumnName(entityType);

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            CustomFieldVO field = fieldMap.get(entry.getKey());
            if (field == null || !Boolean.TRUE.equals(field.getIsUnique())) {
                continue;
            }
            Object val = entry.getValue();
            if (val == null || "".equals(val)) {
                continue;
            }
            val = convertValueForJdbc(field, val, entry.getKey());
            validateUniqueCustomFieldValue(tableName, idColumn, entityId, field, val);
        }
    }

    @Override
    public void validateUniqueFieldValue(String entityType, Long entityId, String fieldName, Object value) {
        if (StrUtil.isBlank(fieldName)) {
            return;
        }
        Map<String, Object> values = new HashMap<>();
        values.put(fieldName, value);
        validateUniqueCustomFieldValues(entityType, entityId, values);
    }

    @Override
    public Map<Long, Map<String, Object>> getBatchCustomFieldValues(String entityType, List<Long> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<CustomFieldVO> fields = getEnabledCustomFieldVOs(entityType);
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
    /*

     * 按字段类型将前端传入的值转换为 JDBC 兼容的 Java 类型
     * text/textarea/select → String
     * number → BigDecimal
     * date → java.sql.Date
     * datetime → java.sql.Timestamp
     * checkbox → Boolean
     * multiselect (List) → JSON String
     */
    private Object convertValueForJdbc(CustomFieldVO field, Object val, String fieldName) {
        if (field == null) {
            return val;
        }
        Object converted = convertValueForJdbc(field.getFieldType(), val, fieldName);
        if (converted instanceof Boolean bool
                && "checkbox".equals(field.getFieldType())
                && field.getColumnType() != null
                && field.getColumnType().toUpperCase(Locale.ROOT).contains("SMALLINT")) {
            return bool ? 1 : 0;
        }
        return converted;
    }

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

    private void validateUniqueCustomFieldValue(String tableName,
                                                String idColumn,
                                                Long entityId,
                                                CustomFieldVO field,
                                                Object value) {
        if (field == null || !Boolean.TRUE.equals(field.getIsUnique()) || value == null) {
            return;
        }
        if (value instanceof String strValue && StrUtil.isBlank(strValue)) {
            return;
        }
        if (StrUtil.isBlank(field.getColumnName()) || !dynamicSchemaService.columnExists(tableName, field.getColumnName())) {
            return;
        }

        Long count = baseMapper.countByCustomFieldValue(
                tableName,
                idColumn,
                entityId,
                field.getColumnName(),
                value
        );
        if (count != null && count > 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    "字段「" + field.getFieldLabel() + "」的值已存在");
        }
    }

    /**
     * 将PO列表转换为VO列表
        List<SystemFieldDefinition> definitions = SYSTEM_FIELD_DEFINITIONS.get(entityType);
        if (definitions == null || definitions.isEmpty()) {
            return;
        }

        synchronized (SYSTEM_FIELD_INIT_LOCK) {
            Set<String> existingFieldNames = list(new LambdaQueryWrapper<CustomField>()
                    .eq(CustomField::getEntityType, entityType))
                    .stream()
                    .map(CustomField::getFieldName)
                    .collect(Collectors.toSet());

            List<CustomField> missingFields = definitions.stream()
                    .filter(definition -> !existingFieldNames.contains(definition.fieldName()))
                    .map(definition -> buildSystemField(entityType, definition))
                    .toList();

            boolean refreshCustomerSearchText = false;
            for (CustomField missingField : missingFields) {
                try {
                    save(missingField);
                    if ("customer".equals(entityType)
                            && missingField.getIsSearchable() != null
                            && missingField.getIsSearchable() == 1
                            && CUSTOMER_SEARCHABLE_TEXT_FIELD_TYPES.contains(missingField.getFieldType())) {
                        refreshCustomerSearchText = true;
                    }
                } catch (Exception exception) {
                    log.warn("初始化系统字段失败: entityType={}, fieldName={}, error={}",
                            entityType, missingField.getFieldName(), exception.getMessage());
                }
            }
            if (refreshCustomerSearchText) {
                customerService.refreshAllCustomerSearchText();
            }
        }
    }

    */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initializeSystemFields(String entityType) {
        List<SystemFieldDefinition> definitions = SYSTEM_FIELD_DEFINITIONS.get(entityType);
        if (definitions == null || definitions.isEmpty()) {
            return;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            return;
        }

        synchronized (SYSTEM_FIELD_INIT_LOCK) {
            Set<String> existingFieldNames = list(new LambdaQueryWrapper<CustomField>()
                    .eq(CustomField::getEntityType, entityType))
                    .stream()
                    .map(CustomField::getFieldName)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toSet());

            definitions.stream()
                    .filter(definition -> !existingFieldNames.contains(definition.fieldName()))
                    .map(definition -> buildSystemField(entityType, definition))
                    .forEach(field -> {
                        try {
                            save(field);
                        } catch (Exception exception) {
                            log.warn("初始化系统字段失败: entityType={}, fieldName={}, error={}",
                                    entityType, field.getFieldName(), exception.getMessage());
                        }
                    });
        }
    }

    private CustomField buildSystemField(String entityType, SystemFieldDefinition definition) {
        CustomField field = new CustomField();
        field.setEntityType(entityType);
        field.setFieldName(definition.fieldName());
        field.setFieldLabel(definition.fieldLabel());
        field.setFieldType(definition.fieldType());
        field.setFieldSource(FIELD_SOURCE_SYSTEM);
        field.setColumnName(definition.columnName());
        field.setColumnType(definition.columnType());
        field.setDefaultValue(definition.defaultValue());
        field.setPlaceholder(definition.placeholder());
        field.setIsRequired(definition.required() ? 1 : 0);
        field.setIsSearchable(definition.searchable() ? 1 : 0);
        field.setIsShowInList(definition.showInList() ? 1 : 0);
        field.setIsUnique(0);
        field.setOptions(definition.options() == null ? null : JSON.toJSONString(definition.options()));
        field.setValidationRules(null);
        field.setSortOrder(definition.sortOrder());
        field.setStatus(1);
        return field;
    }

    private static SystemFieldDefinition systemField(String fieldName,
                                                     String fieldLabel,
                                                     String fieldType,
                                                     String columnName,
                                                     String columnType,
                                                     String defaultValue,
                                                     String placeholder,
                                                     boolean required,
                                                     boolean searchable,
                                                     boolean showInList,
                                                     List<FieldOption> options,
                                                     int sortOrder) {
        return new SystemFieldDefinition(
                fieldName,
                fieldLabel,
                fieldType,
                columnName,
                columnType,
                defaultValue,
                placeholder,
                required,
                searchable,
                showInList,
                options,
                sortOrder
        );
    }

    private static FieldOption option(String value, String label) {
        FieldOption option = new FieldOption();
        option.setValue(value);
        option.setLabel(label);
        return option;
    }

    private int getNextSortOrder(String entityType) {
        CustomField lastField = getOne(new LambdaQueryWrapper<CustomField>()
                .eq(CustomField::getEntityType, entityType)
                .orderByDesc(CustomField::getSortOrder)
                .orderByDesc(CustomField::getCreateTime)
                .last("LIMIT 1"), false);
        if (lastField == null || lastField.getSortOrder() == null) {
            return 1;
        }
        return lastField.getSortOrder() + 1;
    }

    private void appendCustomFieldSourceCondition(LambdaQueryWrapper<CustomField> wrapper) {
        wrapper.isNull(CustomField::getFieldSource)
                .or()
                .eq(CustomField::getFieldSource, FIELD_SOURCE_CUSTOM);
    }

    private boolean isSystemField(CustomField field) {
        return field != null && FIELD_SOURCE_SYSTEM.equalsIgnoreCase(field.getFieldSource());
    }

    private boolean isCustomField(CustomField field) {
        return !isSystemField(field);
    }

    private List<CustomFieldVO> getEnabledCustomFieldVOs(String entityType) {
        return getEnabledFieldsByEntity(entityType).stream()
                .filter(field -> !FIELD_SOURCE_SYSTEM.equalsIgnoreCase(field.getFieldSource()))
                .collect(Collectors.toList());
    }

    private List<CustomFieldVO> convertToVO(List<CustomField> fields) {
        return fields.stream().map(field -> {
            CustomFieldVO vo = BeanUtil.copyProperties(field, CustomFieldVO.class,"options");
            vo.setIsRequired(field.getIsRequired() != null && field.getIsRequired() == 1);
            vo.setIsSearchable(field.getIsSearchable() != null && field.getIsSearchable() == 1);
            vo.setIsShowInList(field.getIsShowInList() != null && field.getIsShowInList() == 1);
            vo.setIsUnique(field.getIsUnique() != null && field.getIsUnique() == 1);

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

    private boolean shouldRefreshCustomerSearchText(String entityType, String fieldType, boolean searchable) {
        return searchable
                && "customer".equals(entityType)
                && CUSTOMER_SEARCHABLE_TEXT_FIELD_TYPES.contains(fieldType);
    }

    private record SystemFieldDefinition(String fieldName,
                                         String fieldLabel,
                                         String fieldType,
                                         String columnName,
                                         String columnType,
                                         String defaultValue,
                                         String placeholder,
                                         boolean required,
                                         boolean searchable,
                                         boolean showInList,
                                         List<FieldOption> options,
                                         int sortOrder) {
    }

}
