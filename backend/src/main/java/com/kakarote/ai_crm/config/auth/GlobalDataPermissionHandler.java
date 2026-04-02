package com.kakarote.ai_crm.config.auth;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.kakarote.ai_crm.common.auth.DataPermissionContext;
import com.kakarote.ai_crm.service.DataPermissionService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GlobalDataPermissionHandler implements MultiDataPermissionHandler {

    private static final Map<String, String> MODULE_BY_MAPPER = Map.of(
            "CustomerMapper", "customer",
            "ContactMapper", "contact",
            "TaskMapper", "task",
            "FollowUpMapper", "followup",
            "KnowledgeMapper", "knowledge"
    );

    @Autowired
    private ObjectProvider<DataPermissionService> dataPermissionServiceProvider;

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        String module = resolveModule(mappedStatementId);
        if (module == null) {
            return null;
        }

        DataPermissionService dataPermissionService = dataPermissionServiceProvider.getIfAvailable();
        if (dataPermissionService == null) {
            log.warn("DataPermissionService is not available yet, skip data permission for {}", mappedStatementId);
            return null;
        }

        DataPermissionContext context = dataPermissionService.createContext(module);
        if (context.isAllData()) {
            return null;
        }

        String sqlSegment = buildSqlSegment(module, table, context.getUserIds());
        if (sqlSegment == null) {
            return null;
        }

        try {
            return CCJSqlParserUtil.parseCondExpression(sqlSegment);
        } catch (JSQLParserException e) {
            log.error("Failed to parse data permission SQL segment: {}", sqlSegment, e);
            throw new IllegalStateException("Failed to parse data permission SQL segment", e);
        }
    }

    private String resolveModule(String mappedStatementId) {
        return MODULE_BY_MAPPER.entrySet().stream()
                .filter(entry -> mappedStatementId.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private String buildSqlSegment(String module, Table table, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return "1 = 0";
        }

        String tableName = normalizeTableName(table.getName());
        String inClause = userIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        return switch (module) {
            case "customer" -> "crm_customer".equals(tableName)
                    ? qualifiedColumn(table, "owner_id") + " IN (" + inClause + ")"
                    : null;
            case "contact" -> "crm_contact".equals(tableName)
                    ? qualifiedColumn(table, "customer_id")
                    + " IN (SELECT customer_id FROM crm_customer WHERE owner_id IN (" + inClause + "))"
                    : null;
            case "task" -> "crm_task".equals(tableName)
                    ? qualifiedColumn(table, "assigned_to") + " IN (" + inClause + ")"
                    : null;
            case "followup" -> "crm_follow_up".equals(tableName)
                    ? qualifiedColumn(table, "customer_id")
                    + " IN (SELECT customer_id FROM crm_customer WHERE owner_id IN (" + inClause + "))"
                    : null;
            case "knowledge" -> "crm_knowledge".equals(tableName)
                    ? "((" + qualifiedColumn(table, "customer_id") + " IS NOT NULL AND "
                    + qualifiedColumn(table, "customer_id")
                    + " IN (SELECT customer_id FROM crm_customer WHERE owner_id IN (" + inClause + ")))"
                    + " OR (" + qualifiedColumn(table, "customer_id") + " IS NULL AND "
                    + qualifiedColumn(table, "upload_user_id") + " IN (" + inClause + ")))"
                    : null;
            default -> null;
        };
    }

    private String normalizeTableName(String tableName) {
        return Optional.ofNullable(tableName)
                .map(name -> name.replace("\"", "").toLowerCase())
                .orElse("");
    }

    private String qualifiedColumn(Table table, String column) {
        Alias alias = table.getAlias();
        String prefix = alias != null ? alias.getName() : null;
        if (prefix == null || prefix.isBlank()) {
            return column;
        }
        return prefix + "." + column;
    }
}
