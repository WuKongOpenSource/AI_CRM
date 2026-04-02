package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.PO.CustomField;
import com.kakarote.ai_crm.entity.PO.CustomFieldPool;
import com.kakarote.ai_crm.mapper.CustomFieldPoolMapper;
import com.kakarote.ai_crm.service.ICustomFieldPoolService;
import com.kakarote.ai_crm.service.IDynamicSchemaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 自定义字段池服务实现
 */
@Slf4j
@Service
public class CustomFieldPoolServiceImpl extends ServiceImpl<CustomFieldPoolMapper, CustomFieldPool>
        implements ICustomFieldPoolService {

    @Autowired
    private IDynamicSchemaService dynamicSchemaService;

    @Autowired
    private com.kakarote.ai_crm.mapper.CustomFieldMapper customFieldMapper;

    /**
     * 字段类型 -> PostgreSQL列类型映射
     */
    private static final Map<String, String> TYPE_MAPPING = Map.of(
            "text", "VARCHAR(500)",
            "textarea", "TEXT",
            "number", "DECIMAL(15,2)",
            "date", "DATE",
            "datetime", "TIMESTAMP",
            "select", "VARCHAR(100)",
            "multiselect", "TEXT",
            "checkbox", "BOOLEAN"
    );

    private static final String COLUMN_PREFIX = "field_";
    private static final int COLUMN_SUFFIX_LENGTH = 6;
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int MAX_GENERATE_ATTEMPTS = 10;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomFieldPool acquireSlot(String entityType, String fieldType) {
        String columnType = TYPE_MAPPING.get(fieldType);
        if (columnType == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "不支持的字段类型: " + fieldType);
        }

        // 1. 查当前租户已使用的 columnName 集合（自动租户过滤）
        List<CustomField> usedFields = customFieldMapper.selectList(
                new LambdaQueryWrapper<CustomField>()
                        .eq(CustomField::getEntityType, entityType)
                        .select(CustomField::getColumnName));
        Set<String> usedColumns = usedFields.stream()
                .map(CustomField::getColumnName)
                .collect(Collectors.toSet());

        // 2. 查池中同 entityType + fieldType 且未被当前租户使用的记录
        List<CustomFieldPool> candidates = list(new LambdaQueryWrapper<CustomFieldPool>()
                .eq(CustomFieldPool::getEntityType, entityType)
                .eq(CustomFieldPool::getFieldType, fieldType)
                .orderByAsc(CustomFieldPool::getCreateTime));

        CustomFieldPool available = candidates.stream()
                .filter(p -> !usedColumns.contains(p.getColumnName()))
                .findFirst()
                .orElse(null);

        if (available != null) {
            // 确保物理列已创建
            ensureColumnCreated(available, entityType);
            return available;
        }

        // 3. 池中无可用槽位，生成新列
        return createNewSlot(entityType, fieldType, columnType);
    }

    private void ensureColumnCreated(CustomFieldPool pool, String entityType) {
        if (Boolean.TRUE.equals(pool.getColumnCreated())) {
            return;
        }
        String tableName = dynamicSchemaService.getTableName(entityType);
        dynamicSchemaService.addColumn(tableName, pool.getColumnName(), pool.getColumnType(), null);
        pool.setColumnCreated(true);
        updateById(pool);
    }

    private CustomFieldPool createNewSlot(String entityType, String fieldType, String columnType) {
        String columnName = generateUniqueColumnName(entityType);
        String tableName = dynamicSchemaService.getTableName(entityType);

        dynamicSchemaService.addColumn(tableName, columnName, columnType, null);

        CustomFieldPool pool = new CustomFieldPool();
        pool.setEntityType(entityType);
        pool.setColumnName(columnName);
        pool.setColumnType(columnType);
        pool.setFieldType(fieldType);
        pool.setColumnCreated(true);
        save(pool);

        log.info("字段池新建槽位: entityType={}, columnName={}, fieldType={}", entityType, columnName, fieldType);
        return pool;
    }

    private String generateUniqueColumnName(String entityType) {
        for (int i = 0; i < MAX_GENERATE_ATTEMPTS; i++) {
            String name = COLUMN_PREFIX + randomSuffix();
            boolean exists = count(new LambdaQueryWrapper<CustomFieldPool>()
                    .eq(CustomFieldPool::getEntityType, entityType)
                    .eq(CustomFieldPool::getColumnName, name)) > 0;
            if (!exists) {
                return name;
            }
        }
        throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "生成字段列名失败，请重试");
    }

    private String randomSuffix() {
        StringBuilder sb = new StringBuilder(COLUMN_SUFFIX_LENGTH);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < COLUMN_SUFFIX_LENGTH; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
