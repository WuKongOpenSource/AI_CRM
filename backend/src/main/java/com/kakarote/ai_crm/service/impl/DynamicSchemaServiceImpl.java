package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.service.IDynamicSchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 动态Schema服务实现
 */
@Service
public class DynamicSchemaServiceImpl implements IDynamicSchemaService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 实体类型 -> 表名映射
     */
    private static final Map<String, String> TABLE_MAPPING = Map.of(
            "customer", "crm_customer",
            "contact", "crm_contact"
    );

    /**
     * 实体类型 -> 主键列名映射
     */
    private static final Map<String, String> ID_COLUMN_MAPPING = Map.of(
            "customer", "customer_id",
            "contact", "contact_id"
    );

    /**
     * 支持的实体类型集合
     */
    public static final Set<String> SUPPORTED_ENTITIES = Set.of("customer", "contact");

    /**
     * 允许的列名正则（防止 SQL 注入）
     * 格式：cf_ + 小写字母开头 + 小写字母/数字/下划线
     */
    private static final Pattern COLUMN_NAME_PATTERN = Pattern.compile("^cf_[a-z][a-z0-9_]*$");

    @Override
    public void addColumn(String tableName, String columnName, String columnType, String comment) {
        // 安全验证
        validateTableName(tableName);
        validateColumnName(columnName);

        if (columnExists(tableName, columnName)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "列已存在: " + columnName);
        }

        // PostgreSQL: 先添加列，再添加注释
        String addColumnSql = String.format(
                "ALTER TABLE %s ADD COLUMN %s %s",
                tableName, columnName, columnType
        );
        jdbcTemplate.execute(addColumnSql);

        // 添加列注释（PostgreSQL 使用 COMMENT ON）
        if (comment != null && !comment.isEmpty()) {
            String escapedComment = comment.replace("'", "''");
            String commentSql = String.format(
                    "COMMENT ON COLUMN %s.%s IS '%s'",
                    tableName, columnName, escapedComment
            );
            jdbcTemplate.execute(commentSql);
        }
    }

    @Override
    public void dropColumn(String tableName, String columnName) {
        validateTableName(tableName);
        validateColumnName(columnName);

        if (!columnExists(tableName, columnName)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "列不存在: " + columnName);
        }

        String sql = String.format("ALTER TABLE %s DROP COLUMN %s", tableName, columnName);
        jdbcTemplate.execute(sql);
    }

    @Override
    public boolean columnExists(String tableName, String columnName) {
        // PostgreSQL: 使用 current_database() 替代 DATABASE()
        String sql = """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_catalog = current_database()
                AND table_name = ? AND column_name = ?
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    @Override
    public String getTableName(String entityType) {
        String tableName = TABLE_MAPPING.get(entityType);
        if (tableName == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "未知的实体类型: " + entityType);
        }
        return tableName;
    }

    @Override
    public String getIdColumnName(String entityType) {
        String idColumn = ID_COLUMN_MAPPING.get(entityType);
        if (idColumn == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "未知的实体类型: " + entityType);
        }
        return idColumn;
    }

    /**
     * 验证列名安全性
     */
    private void validateColumnName(String columnName) {
        if (columnName == null || !COLUMN_NAME_PATTERN.matcher(columnName).matches()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "非法的列名: " + columnName);
        }
    }

    /**
     * 验证表名安全性
     */
    private void validateTableName(String tableName) {
        if (!TABLE_MAPPING.containsValue(tableName)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "非法的表名: " + tableName);
        }
    }
}
