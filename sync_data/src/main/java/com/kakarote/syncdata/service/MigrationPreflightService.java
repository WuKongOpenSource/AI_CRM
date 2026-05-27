package com.kakarote.syncdata.service;

import com.kakarote.syncdata.SyncProperties;
import com.kakarote.syncdata.db.CompanyBindingRepository;
import com.kakarote.syncdata.db.MappingRepository;
import com.kakarote.syncdata.mq.RocketMqSyncSettings;
import com.kakarote.syncdata.model.CompanyBinding;
import com.kakarote.syncdata.model.MigrationModuleCoverage;
import com.kakarote.syncdata.model.MigrationPreflightResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MigrationPreflightService {

    private static final List<ModuleDefinition> SUPPORTED_MODULES = List.of(
            new ModuleDefinition("tenants", "Tenant binding", "sync_company_binding", "crm_tenant", null, List.of(), false),
            new ModuleDefinition("departments", "Departments", "wk_admin_dept", "manager_dept", null, List.of("company_id", "dept_id"), false),
            new ModuleDefinition("roles", "Roles", "wk_admin_role", "manager_role", null, List.of("company_id", "role_id"), false),
            new ModuleDefinition("users", "Users", "wk_admin_user", "manager_user", null, List.of("company_id", "user_id"), false),
            new ModuleDefinition("user_roles", "User roles", "wk_admin_user_role", "manager_user_role", null, List.of("company_id", "id", "user_id", "role_id"), false),
            new ModuleDefinition("custom_fields", "Custom fields", "wk_crm_field", "crm_custom_field", "label IN (2, 3)", List.of("company_id", "field_id", "label"), false),
            new ModuleDefinition("customers", "Customers", "wk_crm_customer", "crm_customer", null, List.of("company_id", "customer_id"), true),
            new ModuleDefinition("contacts", "Contacts", "wk_crm_contacts", "crm_contact", null, List.of("company_id", "contacts_id", "customer_id"), false),
            new ModuleDefinition("customer_custom_values", "Customer custom values", "wk_crm_customer_data", "crm_customer", null, List.of("company_id", "id", "field_id"), false),
            new ModuleDefinition("contact_custom_values", "Contact custom values", "wk_crm_contacts_data", "crm_contact", null, List.of("company_id", "id", "field_id"), false),
            new ModuleDefinition("follow_ups", "Follow ups", "wk_crm_activity", "crm_follow_up", "type = 1 AND COALESCE(status, 1) = 1 AND activity_type IN (2, 3)", List.of("company_id", "id"), false),
            new ModuleDefinition("schedules", "Schedules", "wk_oa_event", "crm_schedule", null, List.of("company_id", "event_id", "start_time"), false),
            new ModuleDefinition("project_tasks", "Project tasks", "wk_project_task", "crm_task", "COALESCE(ishidden, 0) = 0", List.of("company_id", "task_id"), false),
            new ModuleDefinition("work_tasks", "Work tasks", "wk_work_task", "crm_task", "COALESCE(ishidden, 0) = 0", List.of("company_id", "task_id"), false)
    );

    private static final List<UnsupportedModule> UNSUPPORTED_MODULES = List.of(
            new UnsupportedModule("business", "Business", "wk_crm_business"),
            new UnsupportedModule("contracts", "Contracts", "wk_crm_contract"),
            new UnsupportedModule("receivables", "Receivables", "wk_crm_receivables"),
            new UnsupportedModule("products", "Products", "wk_crm_product"),
            new UnsupportedModule("invoices", "Invoices", "wk_crm_invoice"),
            new UnsupportedModule("approvals", "Approvals", "wk_crm_check")
    );

    private final JdbcTemplate oldCrm;
    private final CompanyBindingRepository bindingRepository;
    private final MappingRepository mappingRepository;
    private final SyncProperties properties;

    public MigrationPreflightService(@Qualifier("oldCrmJdbcTemplate") JdbcTemplate oldCrmJdbcTemplate,
                                     CompanyBindingRepository bindingRepository,
                                     MappingRepository mappingRepository,
                                     SyncProperties properties) {
        this.oldCrm = oldCrmJdbcTemplate;
        this.bindingRepository = bindingRepository;
        this.mappingRepository = mappingRepository;
        this.properties = properties;
    }

    public MigrationPreflightResult preflight(Long tenantId, Long companyId, Boolean incrementalRequested) {
        List<MigrationPreflightResult.PreflightIssue> errors = new ArrayList<>();
        List<MigrationPreflightResult.PreflightIssue> warnings = new ArrayList<>();
        List<MigrationModuleCoverage> modules = new ArrayList<>();
        Map<String, Long> rowCounts = new LinkedHashMap<>();

        if (tenantId == null) {
            errors.add(issue("tenant_required", "Current tenant is required before sync.", null));
        }
        if (companyId == null) {
            errors.add(issue("company_required", "Please choose a source company.", null));
        }

        CompanyBinding tenantBinding = tenantId == null ? null : bindingRepository.findByTenantId(tenantId);
        CompanyBinding companyBinding = companyId == null ? null : bindingRepository.findActiveByCompanyId(companyId);
        if (tenantBinding != null && companyId != null && !tenantBinding.sourceCompanyId().equals(companyId)) {
            warnings.add(issue("tenant_rebind", "Current tenant is already bound to another source company.", null));
        }
        if (companyBinding != null && tenantId != null && !companyBinding.tenantId().equals(tenantId)) {
            errors.add(issue("company_bound_to_other_tenant", "This source company is already bound to another tenant.", null));
        }

        for (ModuleDefinition module : SUPPORTED_MODULES) {
            MigrationModuleCoverage coverage = inspectSupportedModule(module, companyId, errors, warnings);
            modules.add(coverage);
            rowCounts.put(module.key(), coverage.rowCount());
        }
        for (UnsupportedModule module : UNSUPPORTED_MODULES) {
            MigrationModuleCoverage coverage = inspectUnsupportedModule(module, companyId);
            modules.add(coverage);
            rowCounts.put(module.key(), coverage.rowCount());
            if ("unavailable".equals(coverage.status())) {
                warnings.add(issue("module_unavailable", coverage.label() + " source data is not migrated.", coverage.key()));
            }
        }

        if (Boolean.TRUE.equals(incrementalRequested)) {
            if (!rocketMqEnabled()) {
                warnings.add(issue("rocketmq_disabled",
                        "Incremental sync was selected, but RocketMQ is disabled.",
                        null));
            }
            if (deprecatedDirectionalTopicConfigured()) {
                warnings.add(issue("rocketmq_direction_topic_deprecated",
                        "Deprecated directional topic configuration was detected; unified topic config will be used.",
                        null));
            }
        }

        long mappingCount = companyId == null ? 0L : mappingRepository.countMappingsForCompany(companyId);
        MigrationPreflightResult.RerunInfo rerun = new MigrationPreflightResult.RerunInfo(
                tenantBinding != null || companyBinding != null,
                mappingCount > 0,
                mappingCount,
                mappingCount > 0
                        ? "Existing mappings will be reused and target rows will be updated."
                        : "No mappings were found for this source company; new mappings will be created."
        );
        MigrationPreflightResult.CleanupInfo cleanup = new MigrationPreflightResult.CleanupInfo(
                properties.isTruncateBeforeSync(),
                properties.isTruncateBeforeSync()
                        ? "Cleanup before sync is enabled for rows recorded in sync_mapping."
                        : "Cleanup before sync is disabled; reruns update target rows by sync_mapping."
        );
        if (properties.isTruncateBeforeSync()) {
            warnings.add(issue("cleanup_enabled", cleanup.message(), null));
        }

        MigrationPreflightResult.IncrementalCapability incremental =
                new MigrationPreflightResult.IncrementalCapability(
                        rocketMqEnabled(),
                        rocketMqEnabled() ? "available" : "rocketmq_disabled",
                        rocketMqEnabled()
                                ? "Bidirectional incremental sync is available."
                                : "Bidirectional incremental sync is unavailable. Check message channel configuration.",
                        rocketMqEnabled(),
                        rocketMqEnabled(),
                        mqTopic(),
                        crmToAicrmTopic(),
                        crmToAicrmTag(),
                        crmToAicrmGroup(),
                        aicrmToCrmTopic(),
                        aicrmToCrmTag(),
                        aicrmToCrmGroup(),
                        "event_time_newer_wins"
                );

        return new MigrationPreflightResult(
                tenantId,
                companyId,
                errors.isEmpty(),
                List.copyOf(errors),
                List.copyOf(warnings),
                List.copyOf(modules),
                Map.copyOf(rowCounts),
                rerun,
                cleanup,
                incremental
        );
    }

    private MigrationModuleCoverage inspectSupportedModule(ModuleDefinition module,
                                                           Long companyId,
                                                           List<MigrationPreflightResult.PreflightIssue> errors,
                                                           List<MigrationPreflightResult.PreflightIssue> warnings) {
        if ("sync_company_binding".equals(module.sourceTable())) {
            return new MigrationModuleCoverage(module.key(), module.label(), module.sourceTable(),
                    module.targetTable(), "supported", 1L, "Using existing tenant binding.");
        }

        if (!oldTableExists(module.sourceTable())) {
            if (module.required()) {
                errors.add(issue("source_table_missing", "Source table is missing: " + module.sourceTable(), module.key()));
                return coverage(module, "blocked", 0L, "Required source table is missing.");
            }
            warnings.add(issue("source_table_skipped", "Source table is missing and will be skipped: " + module.sourceTable(), module.key()));
            return coverage(module, "skipped", 0L, "Source table is missing.");
        }

        List<String> missingColumns = module.requiredColumns().stream()
                .filter(column -> !oldColumnExists(module.sourceTable(), column))
                .toList();
        if (!missingColumns.isEmpty()) {
            errors.add(issue("source_columns_missing",
                    "Source table " + module.sourceTable() + " is missing columns: " + String.join(", ", missingColumns),
                    module.key()));
            return coverage(module, "blocked", 0L, "Source columns are incompatible.");
        }

        Long count = countRows(module.sourceTable(), module.whereClause(), companyId);
        return coverage(module, count == 0 ? "skipped" : "supported", count,
                count == 0 ? "No source rows for this company." : "Rows will be included in full sync.");
    }

    private MigrationModuleCoverage inspectUnsupportedModule(UnsupportedModule module, Long companyId) {
        if (!oldTableExists(module.sourceTable())) {
            return new MigrationModuleCoverage(module.key(), module.label(), module.sourceTable(), null,
                    "skipped", 0L, "Source table is missing.");
        }
        Long count = countRows(module.sourceTable(), null, companyId);
        return new MigrationModuleCoverage(module.key(), module.label(), module.sourceTable(), null,
                count > 0 ? "unavailable" : "skipped", count,
                count > 0 ? "Target business model is not supported by this sync." : "No source rows for this company.");
    }

    private MigrationModuleCoverage coverage(ModuleDefinition module, String status, Long count, String message) {
        return new MigrationModuleCoverage(module.key(), module.label(), module.sourceTable(),
                module.targetTable(), status, count, message);
    }

    private Long countRows(String tableName, String whereClause, Long companyId) {
        String where = buildWhere(tableName, whereClause, companyId);
        Long count = oldCrm.queryForObject("SELECT COUNT(*) FROM " + tableName + where, Long.class);
        return count == null ? 0L : count;
    }

    private String buildWhere(String tableName, String whereClause, Long companyId) {
        List<String> conditions = new ArrayList<>();
        if (whereClause != null && !whereClause.isBlank()) {
            conditions.add("(" + whereClause + ")");
        }
        if (companyId != null && oldColumnExists(tableName, "company_id")) {
            conditions.add("company_id = " + companyId);
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    private boolean oldTableExists(String tableName) {
        Integer count = oldCrm.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean oldColumnExists(String tableName, String columnName) {
        Integer count = oldCrm.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private MigrationPreflightResult.PreflightIssue issue(String code, String message, String module) {
        return new MigrationPreflightResult.PreflightIssue(code, message, module);
    }

    private boolean rocketMqEnabled() {
        return properties.getRocketmq() != null && properties.getRocketmq().isEnabled();
    }

    private String mqTopic() {
        return RocketMqSyncSettings.topic(properties);
    }

    private String crmToAicrmTopic() {
        return mqTopic();
    }

    private String crmToAicrmTag() {
        return RocketMqSyncSettings.crmToAicrmTag(properties);
    }

    private String crmToAicrmGroup() {
        return RocketMqSyncSettings.crmToAicrmGroup(properties);
    }

    private String aicrmToCrmTopic() {
        return mqTopic();
    }

    private String aicrmToCrmTag() {
        return RocketMqSyncSettings.aicrmToCrmTag(properties);
    }

    private String aicrmToCrmGroup() {
        return RocketMqSyncSettings.aicrmToCrmGroup(properties);
    }

    private boolean deprecatedDirectionalTopicConfigured() {
        if (properties.getRocketmq() == null) {
            return false;
        }
        String crmTopic = properties.getRocketmq().getCrmToAicrm() == null
                ? null : properties.getRocketmq().getCrmToAicrm().getTopic();
        String aicrmTopic = properties.getRocketmq().getAicrmToCrm() == null
                ? null : properties.getRocketmq().getAicrmToCrm().getTopic();
        String topic = mqTopic();
        return (crmTopic != null && !crmTopic.isBlank() && !crmTopic.equals(topic))
                || (aicrmTopic != null && !aicrmTopic.isBlank() && !aicrmTopic.equals(topic));
    }

    private record ModuleDefinition(String key, String label, String sourceTable, String targetTable,
                                    String whereClause, List<String> requiredColumns, boolean required) {
    }

    private record UnsupportedModule(String key, String label, String sourceTable) {
    }
}
