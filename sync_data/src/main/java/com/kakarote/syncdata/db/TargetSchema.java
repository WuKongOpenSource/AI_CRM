package com.kakarote.syncdata.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class TargetSchema {

    private static final Logger log = LoggerFactory.getLogger(TargetSchema.class);
    private static final Pattern IDENTIFIER = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    private final JdbcTemplate target;

    public TargetSchema(JdbcTemplate targetJdbcTemplate) {
        this.target = targetJdbcTemplate;
    }

    public void addCustomFieldColumnIfMissing(String tableName, String columnName, String columnType) {
        validateIdentifier(tableName);
        validateIdentifier(columnName);
        target.execute("ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS " + columnName + " " + columnType);
        log.info("Ensured custom field column {}.{} {}", tableName, columnName, columnType);
    }

    private void validateIdentifier(String value) {
        if (value == null || !IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier: " + value);
        }
    }
}
