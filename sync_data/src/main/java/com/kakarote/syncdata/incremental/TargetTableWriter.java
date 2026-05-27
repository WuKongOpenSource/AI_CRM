package com.kakarote.syncdata.incremental;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class TargetTableWriter {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[a-z][a-z0-9_]{0,99}");
    private static final Set<String> ALLOWED_TARGET_TABLES = Set.of(
            "crm_tenant", "manager_dept", "manager_role", "manager_user", "manager_user_role",
            "manager_role_menu", "crm_custom_field", "crm_customer", "crm_contact",
            "crm_follow_up", "crm_schedule", "crm_task"
    );
    private static final Set<String> RELATION_TARGET_TABLES = Set.of("manager_user_role", "manager_role_menu");
    private static final Set<String> NUMERIC_STATUS_TARGET_TABLES = Set.of(
            "crm_tenant", "manager_user", "crm_custom_field", "crm_customer", "crm_contact"
    );

    private final JdbcTemplate target;

    public TargetTableWriter(@Qualifier("targetJdbcTemplate") JdbcTemplate targetJdbcTemplate) {
        this.target = targetJdbcTemplate;
    }

    public void upsert(String tableName, String keyColumn, LinkedHashMap<String, Object> values) {
        validateTargetTable(tableName);
        validateIdentifier(keyColumn);
        values.keySet().forEach(this::validateIdentifier);
        if (!values.containsKey(keyColumn)) {
            throw new IllegalStateException("Missing conflict key " + keyColumn + " for " + tableName);
        }
        String columns = String.join(", ", values.keySet());
        String placeholders = String.join(", ", values.keySet().stream().map(ignored -> "?").toList());
        List<String> updateColumns = values.keySet().stream()
                .filter(column -> !column.equals(keyColumn))
                .map(column -> column + " = EXCLUDED." + column)
                .toList();
        String conflict = updateColumns.isEmpty()
                ? " DO NOTHING"
                : " DO UPDATE SET " + String.join(", ", updateColumns);
        target.update("INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ") "
                        + "ON CONFLICT (" + keyColumn + ")" + conflict,
                values.values().toArray());
    }

    public boolean softDeleteOrDelete(String tableName, String keyColumn, Long targetId) {
        validateTargetTable(tableName);
        validateIdentifier(keyColumn);
        if (targetId == null) {
            return true;
        }
        if (RELATION_TARGET_TABLES.contains(tableName)) {
            target.update("DELETE FROM " + tableName + " WHERE " + keyColumn + " = ?", targetId);
            return true;
        }
        if (NUMERIC_STATUS_TARGET_TABLES.contains(tableName)) {
            target.update("UPDATE " + tableName + " SET status = 0 WHERE " + keyColumn + " = ?", targetId);
            return true;
        }
        return false;
    }

    public void updateColumn(String tableName, String keyColumn, Long targetId, String columnName, Object value) {
        validateTargetTable(tableName);
        validateIdentifier(keyColumn);
        validateIdentifier(columnName);
        if (targetId == null) {
            return;
        }
        target.update("UPDATE " + tableName + " SET " + columnName + " = ? WHERE " + keyColumn + " = ?",
                value, targetId);
    }

    private void validateTargetTable(String tableName) {
        validateIdentifier(tableName);
        if (!ALLOWED_TARGET_TABLES.contains(tableName)) {
            throw new IllegalArgumentException("Unexpected target table: " + tableName);
        }
    }

    private void validateIdentifier(String value) {
        if (value == null || !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Unsafe SQL identifier: " + value);
        }
    }
}
