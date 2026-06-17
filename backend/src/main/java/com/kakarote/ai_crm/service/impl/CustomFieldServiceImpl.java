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
import com.kakarote.ai_crm.service.ICustomerService;
import com.kakarote.ai_crm.service.IDynamicSchemaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private ICustomerService customerService;

    /**
     * 单个实体最大自定义字段数
     */
    private static final int MAX_FIELDS_PER_ENTITY = 50;
    private static final long OPTIONS_CACHE_TTL_MS = 60_000L;
    private static final Set<String> CUSTOMER_SEARCHABLE_TEXT_FIELD_TYPES = Set.of("text", "textarea", "select", "multiselect");
    private static final String FIELD_SOURCE_SYSTEM = "system";
    private static final String FIELD_SOURCE_CUSTOM = "custom";
    private static final Set<String> HIDDEN_RELATION_SYSTEM_FIELDS = Set.of("avatar", "company");
    private static final Object SYSTEM_FIELD_INIT_LOCK = new Object();

    private final Map<String, OptionsCacheEntry> optionsCache = new ConcurrentHashMap<>();

    private record OptionsCacheEntry(long expireAt, List<FieldOption> options) {
    }

    private static final Map<String, List<FieldOption>> BUILTIN_FIELD_OPTIONS = Map.of(
            optionKey("customer", "stage"), List.of(
                    option("lead", "线索"),
                    option("qualified", "资格审核"),
                    option("proposal", "方案报价"),
                    option("negotiation", "谈判中"),
                    option("closed", "已成交"),
                    option("lost", "已流失")
            ),
            optionKey("customer", "level"), List.of(
                    option("A", "A级客户"),
                    option("B", "B级客户"),
                    option("C", "C级客户")
            ),
            optionKey("relation", "relation_type"), List.of(
                    option("decision_maker", "决策人"),
                    option("influencer", "影响人"),
                    option("partner", "合作伙伴"),
                    option("customer_contact", "客户联系人"),
                    option("other", "其他")
            ),
            optionKey("relation", "relationType"), List.of(
                    option("decision_maker", "决策人"),
                    option("influencer", "影响人"),
                    option("partner", "合作伙伴"),
                    option("customer_contact", "客户联系人"),
                    option("other", "其他")
            ),
            optionKey("relation", "source"), List.of(
                    option("manual", "手动创建"),
                    option("customer_contact", "客户联系人"),
                    option("other", "其他")
            )
    );

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
                    systemField("level", "客户级别", "select", "level", "VARCHAR(10)", null, null, false, false, true, List.of(
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
            ),
            "relation", List.of(
                    systemField("name", "姓名", "text", "name", "VARCHAR(100)", null, "请输入姓名", true, true, true, null, 10),
                    systemField("avatar", "头像", "text", "avatar", "VARCHAR(500)", null, "请输入头像地址", false, false, false, null, 20),
                    systemField("phone", "手机号", "text", "phone", "VARCHAR(50)", null, "请输入手机号", false, true, true, null, 30),
                    systemField("wechat", "微信号", "text", "wechat", "VARCHAR(100)", null, "请输入微信号", false, true, true, null, 40),
                    systemField("email", "邮箱", "text", "email", "VARCHAR(100)", null, "请输入邮箱", false, true, true, null, 50),
                    systemField("relationType", "关系类型", "select", "relation_type", "VARCHAR(50)", "other", null, false, true, true, List.of(
                            option("friend", "朋友"),
                            option("family", "家人"),
                            option("relative", "亲戚"),
                            option("partner", "合作伙伴"),
                            option("customer_contact", "客户联系人"),
                            option("supplier", "供应商"),
                            option("investor", "投资人"),
                            option("other", "其他")
                    ), 60),
                    systemField("company", "所属公司", "text", "company", "VARCHAR(255)", null, "请输入所属公司", false, true, true, null, 70),
                    systemField("remark", "备注", "textarea", "remark", "TEXT", null, "请输入备注", false, false, false, null, 80),
                    systemField("source", "来源", "select", "source", "VARCHAR(50)", "manual", null, false, false, true, List.of(
                            option("manual", "手动创建"),
                            option("customer_contact", "客户联系人")
                    ), 90)
            ),
            "product", List.of(
                    systemField("productName", "产品名称", "text", "product_name", "VARCHAR(255)", null, "请输入产品名称", true, true, true, null, 10),
                    systemField("productCode", "产品编码", "text", "product_code", "VARCHAR(100)", null, "请输入产品编码", false, true, true, null, 20),
                    systemField("categoryId", "产品类目", "number", "category_id", "BIGINT", null, null, false, false, true, null, 30),
                    systemField("productType", "产品类型", "select", "product_type", "VARCHAR(50)", "goods", null, false, true, true, List.of(
                            option("goods", "实物"),
                            option("service", "服务"),
                            option("subscription", "订阅"),
                            option("other", "其他")
                    ), 40),
                    systemField("unit", "单位", "select", "unit", "VARCHAR(50)", null, "请选择单位", false, true, true, List.of(
                            option("个", "个"),
                            option("套", "套"),
                            option("台", "台"),
                            option("件", "件"),
                            option("年", "年"),
                            option("月", "月"),
                            option("次", "次")
                    ), 50),
                    systemField("standardPrice", "标准价", "number", "standard_price", "DECIMAL(18,2)", null, null, false, false, true, null, 60),
                    systemField("costPrice", "成本价", "number", "cost_price", "DECIMAL(18,2)", null, null, false, false, false, null, 70),
                    systemField("ownerId", "负责人", "number", "owner_id", "BIGINT", null, null, true, false, true, null, 80),
                    systemField("status", "状态", "select", "status", "VARCHAR(20)", "active", null, false, true, true, List.of(
                            option("active", "启用"),
                            option("inactive", "停用")
                    ), 90),
                    systemField("description", "描述", "textarea", "description", "TEXT", null, "请输入描述", false, true, false, null, 100)
            )
    );

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
        evictOptionsCache();
        refreshCustomerSearchTextIfNeeded(field);

        return field.getFieldId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateField(CustomFieldUpdateBO bo) {
        CustomField field = getById(bo.getFieldId());
        if (ObjectUtil.isNull(field)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "字段不存在");
        }

        boolean wasSearchable = isCustomerSearchTextField(field);
        boolean optionsChanged = false;

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
        if (bo.getOptions() != null) {
            if (isSystemField(field)) {
                validateSystemFieldOptions(field, bo.getOptions());
            }
            field.setOptions(JSON.toJSONString(bo.getOptions()));
            optionsChanged = true;
        }
        if (bo.getValidation() != null && !isSystemField(field)) {
            field.setValidationRules(JSON.toJSONString(bo.getValidation()));
        }
        if (bo.getSortOrder() != null) {
            field.setSortOrder(bo.getSortOrder());
        }

        updateById(field);
        evictOptionsCache();
        if (wasSearchable || isCustomerSearchTextField(field) || optionsChanged) {
            refreshCustomerSearchTextIfNeeded(field, wasSearchable);
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
        refreshCustomerSearchTextIfNeeded(field, true);
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
        refreshCustomerSearchTextIfNeeded(field);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteField(Long fieldId) {
        CustomField field = getById(fieldId);
        if (ObjectUtil.isNull(field)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "字段不存在");
        }

        // 仅删除元数据，物理列保留在字段池中供未来复用
        if (isSystemField(field)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "系统字段不支持删除");
        }
        removeById(fieldId);
        evictOptionsCache();

        // 清理该字段的所有用户排序记录
        customFieldSortService.removeByFieldId(fieldId);
        refreshCustomerSearchTextIfNeeded(field, true);
    }

    @Override
    public List<CustomFieldVO> getFieldsByEntity(String entityType) {
        List<CustomField> fields = list(new LambdaQueryWrapper<CustomField>()
                .eq(CustomField::getEntityType, entityType)
                .orderByAsc(CustomField::getSortOrder)
                .orderByAsc(CustomField::getCreateTime));
        return convertToVO(filterHiddenSystemFields(fields));
    }

    @Override
    public List<CustomFieldVO> getEnabledFieldsByEntity(String entityType) {
        List<CustomField> fields = list(new LambdaQueryWrapper<CustomField>()
                .eq(CustomField::getEntityType, entityType)
                .eq(CustomField::getStatus, 1)
                .orderByAsc(CustomField::getSortOrder)
                .orderByAsc(CustomField::getCreateTime));
        return convertToVO(filterHiddenSystemFields(fields));
    }

    @Override
    public List<CustomFieldVO> getListFieldsByEntity(String entityType) {
        return getEnabledFieldsByEntity(entityType).stream()
                .filter(field -> Boolean.TRUE.equals(field.getIsShowInList()))
                .toList();
    }

    @Override
    public List<CustomFieldVO> getFormFieldsByEntity(String entityType) {
        return getEnabledFieldsByEntity(entityType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initializeSystemFields(String entityType) {
        String normalizedEntity = StrUtil.trimToEmpty(entityType).toLowerCase(Locale.ROOT);
        List<SystemFieldDefinition> definitions = SYSTEM_FIELD_DEFINITIONS.get(normalizedEntity);
        if (definitions == null || definitions.isEmpty()) {
            return;
        }

        synchronized (SYSTEM_FIELD_INIT_LOCK) {
            List<CustomField> existingFields = list(new LambdaQueryWrapper<CustomField>()
                    .eq(CustomField::getEntityType, normalizedEntity));
            Map<String, CustomField> existingFieldMap = existingFields.stream()
                    .filter(field -> StrUtil.isNotBlank(field.getFieldName()))
                    .collect(Collectors.toMap(CustomField::getFieldName, field -> field, (left, right) -> left));

            for (SystemFieldDefinition definition : definitions) {
                CustomField existing = existingFieldMap.get(definition.fieldName());
                if (existing != null) {
                    syncSystemFieldMetadata(existing, definition);
                    continue;
                }
                try {
                    save(buildSystemField(normalizedEntity, definition));
                } catch (Exception exception) {
                    log.warn("初始化系统字段失败: entityType={}, fieldName={}, error={}",
                            normalizedEntity, definition.fieldName(), exception.getMessage());
                }
            }
        }
        evictOptionsCache();
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

        Map<String, CustomFieldVO> fieldMap = fields.stream()
                .collect(Collectors.toMap(CustomFieldVO::getFieldName, field -> field, (a, b) -> a));

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

        CustomFieldVO field = getEnabledFieldsByEntity(entityType).stream()
                .filter(item -> fieldName.equals(item.getFieldName()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "自定义字段不存在"));

        String tableName = dynamicSchemaService.getTableName(entityType);
        String idColumn = dynamicSchemaService.getIdColumnName(entityType);
        if (StrUtil.isBlank(field.getColumnName()) || !dynamicSchemaService.columnExists(tableName, field.getColumnName())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "自定义字段列不存在");
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
                .collect(Collectors.toMap(CustomFieldVO::getFieldName, field -> field, (a, b) -> a));
        String tableName = dynamicSchemaService.getTableName(entityType);
        String idColumn = dynamicSchemaService.getIdColumnName(entityType);

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            CustomFieldVO field = fieldMap.get(entry.getKey());
            if (field == null || !Boolean.TRUE.equals(field.getIsUnique())) {
                continue;
            }
            Object value = entry.getValue();
            if (value == null || "".equals(value)) {
                continue;
            }
            Object jdbcValue = convertValueForJdbc(field, value, entry.getKey());
            validateUniqueCustomFieldValue(tableName, idColumn, entityId, field, jdbcValue);
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
    public List<FieldOption> getFieldOptions(String entityType, String fieldName) {
        if (StrUtil.isBlank(entityType) || StrUtil.isBlank(fieldName)) {
            return Collections.emptyList();
        }
        String key = optionsCacheKey(entityType, fieldName);
        OptionsCacheEntry cached = optionsCache.get(key);
        if (cached != null && cached.expireAt() >= System.currentTimeMillis()) {
            return cached.options();
        }

        List<String> fieldNames = optionFieldNames(entityType, fieldName);
        List<CustomField> fields = list(new LambdaQueryWrapper<CustomField>()
                .eq(CustomField::getEntityType, entityType)
                .in(CustomField::getFieldName, fieldNames)
                .eq(CustomField::getStatus, 1));
        CustomField field = fields.stream()
                .filter(item -> StrUtil.equals(item.getFieldName(), fieldName) && StrUtil.isNotBlank(item.getOptions()))
                .findFirst()
                .orElseGet(() -> fields.stream()
                        .filter(item -> StrUtil.isNotBlank(item.getOptions()))
                        .findFirst()
                        .orElse(null));
        List<FieldOption> options = field != null && StrUtil.isNotBlank(field.getOptions())
                ? JSON.parseArray(field.getOptions(), FieldOption.class)
                : builtinOptions(entityType, builtinFieldName(entityType, fieldName));
        if (options == null) {
            options = Collections.emptyList();
        }
        optionsCache.put(key, new OptionsCacheEntry(System.currentTimeMillis() + OPTIONS_CACHE_TTL_MS, options));
        return options;
    }

    @Override
    public String resolveOptionLabel(String entityType, String fieldName, String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        for (FieldOption option : getFieldOptions(entityType, fieldName)) {
            if (option != null && StrUtil.equals(option.getValue(), value)) {
                return StrUtil.blankToDefault(option.getLabel(), value);
            }
        }
        return value;
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

        Long count = baseMapper.countDuplicateCustomFieldValue(
                tableName,
                idColumn,
                entityId,
                field.getColumnName(),
                value
        );
        if (count != null && count > 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    StrUtil.blankToDefault(field.getFieldLabel(), field.getFieldName()) + "已存在");
        }
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

    private boolean isHiddenSystemField(CustomField field) {
        return isSystemField(field)
                && "relation".equals(field.getEntityType())
                && HIDDEN_RELATION_SYSTEM_FIELDS.contains(field.getFieldName());
    }

    private List<CustomField> filterHiddenSystemFields(List<CustomField> fields) {
        if (fields == null || fields.isEmpty()) {
            return Collections.emptyList();
        }
        return fields.stream()
                .filter(field -> !isHiddenSystemField(field))
                .toList();
    }

    private List<CustomFieldVO> getEnabledCustomFieldVOs(String entityType) {
        return getEnabledFieldsByEntity(entityType).stream()
                .filter(field -> !FIELD_SOURCE_SYSTEM.equalsIgnoreCase(field.getFieldSource()))
                .collect(Collectors.toList());
    }

    private List<String> optionFieldNames(String entityType, String fieldName) {
        if ("relation".equals(entityType)) {
            if ("relationType".equals(fieldName)) {
                return List.of("relationType", "relation_type");
            }
            if ("relation_type".equals(fieldName)) {
                return List.of("relation_type", "relationType");
            }
        }
        return List.of(fieldName);
    }

    private String builtinFieldName(String entityType, String fieldName) {
        if ("relation".equals(entityType) && "relation_type".equals(fieldName)) {
            return "relationType";
        }
        return fieldName;
    }

    private List<FieldOption> builtinOptions(String entityType, String fieldName) {
        List<SystemFieldDefinition> definitions = SYSTEM_FIELD_DEFINITIONS.get(entityType);
        String normalizedFieldName = builtinFieldName(entityType, fieldName);
        if (definitions != null) {
            for (SystemFieldDefinition definition : definitions) {
                if (definition.fieldName().equals(normalizedFieldName) && definition.options() != null) {
                    return definition.options();
                }
            }
        }
        return BUILTIN_FIELD_OPTIONS.getOrDefault(optionKey(entityType, fieldName), Collections.emptyList());
    }

    private void validateSystemFieldOptions(CustomField field, List<FieldOption> options) {
        Set<String> submittedValues = new LinkedHashSet<>();
        int maxLen = parseColumnLength(field.getColumnType());
        for (FieldOption option : options) {
            if (option == null || StrUtil.isBlank(option.getValue())) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "选项值不能为空");
            }
            if (!submittedValues.add(option.getValue())) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "选项值重复: " + option.getValue());
            }
            if (maxLen > 0 && option.getValue().length() > maxLen) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                        "选项值「" + option.getValue() + "」长度超过该字段限制(" + maxLen + ")");
            }
        }
        for (FieldOption builtin : builtinOptions(field.getEntityType(), builtinFieldName(field.getEntityType(), field.getFieldName()))) {
            if (!submittedValues.contains(builtin.getValue())) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                        "系统内置选项不可删除或修改取值: " + builtin.getValue());
            }
        }
    }

    private int parseColumnLength(String columnType) {
        if (StrUtil.isBlank(columnType)) {
            return 0;
        }
        int start = columnType.indexOf('(');
        if (start < 0) {
            return 0;
        }
        int end = columnType.indexOf(')', start + 1);
        if (end <= start + 1) {
            return 0;
        }
        try {
            return Integer.parseInt(columnType.substring(start + 1, end).trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void refreshCustomerSearchTextIfNeeded(CustomField field) {
        refreshCustomerSearchTextIfNeeded(field, false);
    }

    private void refreshCustomerSearchTextIfNeeded(CustomField field, boolean force) {
        if ((force && isCustomerSearchTextCandidate(field)) || isCustomerSearchTextField(field)) {
            customerService.refreshAllCustomerSearchText();
        }
    }

    private boolean isCustomerSearchTextField(CustomField field) {
        return isCustomerSearchTextCandidate(field)
                && field.getIsSearchable() != null
                && field.getIsSearchable() == 1;
    }

    private boolean isCustomerSearchTextCandidate(CustomField field) {
        return field != null
                && isCustomField(field)
                && "customer".equals(field.getEntityType())
                && CUSTOMER_SEARCHABLE_TEXT_FIELD_TYPES.contains(field.getFieldType());
    }

    private void evictOptionsCache() {
        optionsCache.clear();
    }

    private void syncSystemFieldMetadata(CustomField field, SystemFieldDefinition definition) {
        boolean changed = false;
        boolean fieldTypeChanged = false;

        if (!FIELD_SOURCE_SYSTEM.equalsIgnoreCase(field.getFieldSource())) {
            field.setFieldSource(FIELD_SOURCE_SYSTEM);
            changed = true;
        }
        if (!StrUtil.equals(field.getFieldType(), definition.fieldType())) {
            field.setFieldType(definition.fieldType());
            changed = true;
            fieldTypeChanged = true;
        }
        if (!StrUtil.equals(field.getColumnName(), definition.columnName())) {
            field.setColumnName(definition.columnName());
            changed = true;
        }
        if (!StrUtil.equals(field.getColumnType(), definition.columnType())) {
            field.setColumnType(definition.columnType());
            changed = true;
        }
        if ((fieldTypeChanged || StrUtil.isBlank(field.getPlaceholder()))
                && StrUtil.isNotBlank(definition.placeholder())
                && !StrUtil.equals(field.getPlaceholder(), definition.placeholder())) {
            field.setPlaceholder(definition.placeholder());
            changed = true;
        }
        if (definition.options() != null && (fieldTypeChanged || StrUtil.isBlank(field.getOptions()))) {
            field.setOptions(JSON.toJSONString(definition.options()));
            changed = true;
        }
        if (changed) {
            updateById(field);
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

    private static String optionKey(String entityType, String fieldName) {
        return StrUtil.blankToDefault(entityType, "") + ":" + StrUtil.blankToDefault(fieldName, "");
    }

    private String optionsCacheKey(String entityType, String fieldName) {
        return optionKey(entityType, fieldName);
    }

    private static FieldOption option(String value, String label) {
        FieldOption option = new FieldOption();
        option.setValue(value);
        option.setLabel(label);
        return option;
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

    /**
     * 将PO列表转换为VO列表
     */
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

}
