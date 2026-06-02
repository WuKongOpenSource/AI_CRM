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

    private static final Map<String, String> MODULE_BY_MAPPER = Map.ofEntries(
            Map.entry("CustomerMapper", "customer"),
            Map.entry("ContactMapper", "contact"),
            Map.entry("TaskMapper", "task"),
            Map.entry("FollowUpMapper", "followup"),
            Map.entry("KnowledgeMapper", "knowledge"),
            Map.entry("WecomEmployeeMapper", "wecomEmployeeSession"),
            Map.entry("WecomExternalCustomerMapper", "wecomCustomer"),
            Map.entry("WecomCustomerBindingMapper", "wecomCustomer")
    );

    @Autowired
    private ObjectProvider<DataPermissionService> dataPermissionServiceProvider;

    /**
     * 获取SQLSegment。
     */
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

        // MyBatis-Plus 会按 SQL 中出现的表逐个回调这里，因此需要先把 mapper 映射成模块，再按具体表名生成过滤片段。
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

    /**
     * 解析模块。
     */
    private String resolveModule(String mappedStatementId) {
        // 用 mapper 名推断权限模块，避免在每个 XML/方法上手工重复声明模块信息。
        return MODULE_BY_MAPPER.entrySet().stream()
                .filter(entry -> mappedStatementId.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * 构建SQLSegment。
     */
    private String buildSqlSegment(String module, Table table, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            // 无可见用户时返回恒假表达式，显式拒绝访问，避免因为 null 片段而误放行整表数据。
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
            case "wecomEmployeeSession" -> "crm_wecom_employee".equals(tableName)
                    ? qualifiedColumn(table, "crm_user_id") + " IN (" + inClause + ")"
                    : null;
            case "wecomCustomer" -> switch (tableName) {
                case "crm_wecom_external_customer", "crm_wecom_customer_binding" ->
                        qualifiedColumn(table, "customer_id")
                                + " IN (SELECT customer_id FROM crm_customer WHERE owner_id IN (" + inClause + "))";
                default -> null;
            };
            default -> null;
        };
    }

    /**
     * 标准化Table名称。
     */
    private String normalizeTableName(String tableName) {
        return Optional.ofNullable(tableName)
                .map(name -> name.replace("\"", "").toLowerCase())
                .orElse("");
    }

    /**
     * 处理qualifiedColumn方法逻辑。
     */
    private String qualifiedColumn(Table table, String column) {
        Alias alias = table.getAlias();
        String prefix = alias != null ? alias.getName() : null;
        if (prefix == null || prefix.isBlank()) {
            return column;
        }
        // 统一带别名可避免 JOIN 场景下列名歧义，否则数据权限 SQL 很容易在复杂查询里失效。
        return prefix + "." + column;
    }
}
