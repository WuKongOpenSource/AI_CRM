package com.kakarote.syncdata.incremental;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.syncdata.SyncProperties;
import com.kakarote.syncdata.db.MappingRepository;
import com.kakarote.syncdata.db.TargetSchema;
import com.kakarote.syncdata.model.CompanyBinding;
import com.kakarote.syncdata.util.Rows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class CrmToAicrmEventApplier {

    private static final Map<String, SourceSpec> SOURCE_SPECS = Map.ofEntries(
            Map.entry("wk_admin_company", new SourceSpec("crm_tenant", "company_id", "tenant_id")),
            Map.entry("wk_admin_dept", new SourceSpec("manager_dept", "dept_id", "dept_id")),
            Map.entry("wk_admin_role", new SourceSpec("manager_role", "role_id", "role_id")),
            Map.entry("wk_admin_user", new SourceSpec("manager_user", "user_id", "user_id")),
            Map.entry("wk_admin_user_role", new SourceSpec("manager_user_role", "id", "id")),
            Map.entry("wk_crm_field", new SourceSpec("crm_custom_field", "field_id", "field_id")),
            Map.entry("wk_crm_customer", new SourceSpec("crm_customer", "customer_id", "customer_id")),
            Map.entry("wk_crm_contacts", new SourceSpec("crm_contact", "contacts_id", "contact_id")),
            Map.entry("wk_crm_customer_data", new SourceSpec("crm_customer", "id", "customer_id")),
            Map.entry("wk_crm_contacts_data", new SourceSpec("crm_contact", "id", "contact_id")),
            Map.entry("wk_crm_activity", new SourceSpec("crm_follow_up", "id", "follow_up_id")),
            Map.entry("wk_oa_event", new SourceSpec("crm_schedule", "event_id", "schedule_id")),
            Map.entry("wk_project_task", new SourceSpec("crm_task", "task_id", "task_id")),
            Map.entry("wk_work_task", new SourceSpec("crm_task", "task_id", "task_id"))
    );

    private static final Set<String> CUSTOM_VALUE_TABLES = Set.of("wk_crm_customer_data", "wk_crm_contacts_data");

    private final MappingRepository mappingRepository;
    private final TargetTableWriter writer;
    private final TargetSchema targetSchema;
    private final JdbcTemplate target;
    private final SyncProperties properties;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public CrmToAicrmEventApplier(MappingRepository mappingRepository,
                                  TargetTableWriter writer,
                                  TargetSchema targetSchema,
                                  @Qualifier("targetJdbcTemplate") JdbcTemplate target,
                                  SyncProperties properties,
                                  ObjectMapper objectMapper) {
        this.mappingRepository = mappingRepository;
        this.writer = writer;
        this.targetSchema = targetSchema;
        this.target = target;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public ApplyResult apply(IncrementalSyncEvent event, CompanyBinding binding) {
        SourceSpec spec = SOURCE_SPECS.get(event.sourceTable());
        if (spec == null) {
            return ApplyResult.skipped("pending_manual", "Unsupported incremental source table: " + event.sourceTable());
        }
        if (CUSTOM_VALUE_TABLES.contains(event.sourceTable())) {
            return applyCustomValue(event, binding);
        }
        Long companyId = event.sourceCompanyId() == null ? binding.sourceCompanyId() : event.sourceCompanyId();
        Long tenantId = binding.tenantId();
        Object sourceId = sourceId(event, spec);
        if (sourceId == null) {
            return ApplyResult.skipped("failed", "Missing source id for " + event.sourceTable());
        }
        Long targetId = mappingRepository.findTargetId(event.sourceTable(), companyId, sourceId, spec.targetTable());
        if (SyncOperation.DELETE.name().equals(event.resolvedOperation())) {
            if (targetId == null) {
                return ApplyResult.skipped("ignored", "Delete ignored because target mapping is missing.");
            }
            if (!writer.softDeleteOrDelete(spec.targetTable(), spec.targetKeyColumn(), targetId)) {
                return ApplyResult.skipped("pending_manual",
                        "No automatic delete rule for target table: " + spec.targetTable());
            }
            return ApplyResult.applied(spec.targetTable(), targetId, tenantId);
        }

        if ("wk_admin_company".equals(event.sourceTable())) {
            targetId = tenantId;
            mappingRepository.upsertMapping(event.sourceTable(), companyId, sourceId, spec.targetTable(), targetId, tenantId);
        } else {
            targetId = mappingRepository.getOrCreateTargetId(event.sourceTable(), companyId, sourceId,
                    spec.targetTable(), tenantId);
        }
        LinkedHashMap<String, Object> values = convert(event.sourceTable(), event.payload(), companyId, tenantId, targetId);
        if (values == null) {
            return ApplyResult.skipped("pending_manual", "No converter for source table: " + event.sourceTable());
        }
        writer.upsert(spec.targetTable(), spec.targetKeyColumn(), values);
        return ApplyResult.applied(spec.targetTable(), targetId, tenantId);
    }

    public ApplyResult resolveTarget(IncrementalSyncEvent event, CompanyBinding binding) {
        SourceSpec spec = SOURCE_SPECS.get(event.sourceTable());
        if (spec == null || CUSTOM_VALUE_TABLES.contains(event.sourceTable())) {
            return ApplyResult.skipped("unresolved", "No direct target mapping.");
        }
        Long companyId = event.sourceCompanyId() == null ? binding.sourceCompanyId() : event.sourceCompanyId();
        Object sourceId = sourceId(event, spec);
        if (sourceId == null) {
            return ApplyResult.skipped("unresolved", "Missing source id.");
        }
        Long targetId = mappingRepository.findTargetId(event.sourceTable(), companyId, sourceId, spec.targetTable());
        return new ApplyResult("resolved", "resolved", spec.targetTable(), targetId, binding.tenantId());
    }

    public ApplyResult applyAck(IncrementalSyncEvent event, CompanyBinding binding) {
        Map<String, Object> payload = event.payload();
        Long aicrmId = firstLong(payload, "aicrmId", "targetId", "target_id");
        Long crmId = firstLong(payload, "crmId", "sourceId", "source_id");
        String sourceTable = firstString(payload, "sourceTable", "source_table");
        String targetTable = firstString(payload, "targetTable", "target_table");
        if (aicrmId == null || crmId == null || sourceTable == null || targetTable == null) {
            return ApplyResult.skipped("failed", "ACK requires aicrmId, crmId, sourceTable, and targetTable.");
        }
        mappingRepository.upsertMapping(sourceTable, binding.sourceCompanyId(), crmId, targetTable, aicrmId, binding.tenantId());
        return ApplyResult.applied(targetTable, aicrmId, binding.tenantId());
    }

    private ApplyResult applyCustomValue(IncrementalSyncEvent event, CompanyBinding binding) {
        Long companyId = event.sourceCompanyId() == null ? binding.sourceCompanyId() : event.sourceCompanyId();
        boolean customerValue = "wk_crm_customer_data".equals(event.sourceTable());
        String entitySourceTable = customerValue ? "wk_crm_customer" : "wk_crm_contacts";
        String targetTable = customerValue ? "crm_customer" : "crm_contact";
        String keyColumn = customerValue ? "customer_id" : "contact_id";
        Long oldEntityId = firstLong(event.payload(), "data_id", customerValue ? "customer_id" : "contacts_id");
        Long oldFieldId = firstLong(event.payload(), "field_id");
        Long fieldTargetId = oldFieldId == null ? null : mappingRepository.findTargetId(
                "wk_crm_field", companyId, oldFieldId, "crm_custom_field");
        Long entityTargetId = oldEntityId == null ? null : mappingRepository.findTargetId(
                entitySourceTable, companyId, oldEntityId, targetTable);
        if (fieldTargetId == null || entityTargetId == null) {
            return ApplyResult.skipped("pending_mapping", "Custom field or entity mapping is missing.");
        }
        String columnName = loadCustomFieldColumn(fieldTargetId);
        if (columnName == null) {
            return ApplyResult.skipped("pending_mapping", "Custom field target column is missing.");
        }
        writer.updateColumn(targetTable, keyColumn, entityTargetId, columnName, firstString(event.payload(), "value"));
        return ApplyResult.applied(targetTable, entityTargetId, binding.tenantId());
    }

    private LinkedHashMap<String, Object> convert(String sourceTable, Map<String, Object> row,
                                                  Long companyId, Long tenantId, Long targetId) {
        return switch (sourceTable) {
            case "wk_admin_company" -> tenantValues(row, tenantId, companyId);
            case "wk_admin_dept" -> deptValues(row, companyId, tenantId, targetId);
            case "wk_admin_role" -> roleValues(row, companyId, tenantId, targetId);
            case "wk_admin_user" -> userValues(row, companyId, tenantId, targetId);
            case "wk_admin_user_role" -> userRoleValues(row, companyId, tenantId, targetId);
            case "wk_crm_field" -> customFieldValues(row, companyId, tenantId, targetId);
            case "wk_crm_customer" -> customerValues(row, companyId, tenantId, targetId);
            case "wk_crm_contacts" -> contactValues(row, companyId, tenantId, targetId);
            case "wk_crm_activity" -> followUpValues(row, companyId, tenantId, targetId);
            case "wk_oa_event" -> scheduleValues(row, companyId, tenantId, targetId);
            case "wk_project_task", "wk_work_task" -> taskValues(row, companyId, tenantId, targetId,
                    "wk_project_task".equals(sourceTable) ? "project" : "work");
            default -> null;
        };
    }

    private LinkedHashMap<String, Object> tenantValues(Map<String, Object> row, Long tenantId, Long companyId) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("tenant_id", tenantId);
        values.put("tenant_name", trim(firstNonBlank(str(row, "tenant_name"), str(row, "name"), "WK CRM " + companyId), 100));
        values.put("contact_phone", trim(firstNonBlank(str(row, "phone"), str(row, "company_manage")), 20));
        values.put("status", intValue(row, "company_status") == null || intValue(row, "company_status") == 1 ? 1 : 0);
        values.put("max_users", 200);
        values.put("remark", "Updated by CRM incremental event.");
        values.put("create_time", timestamp(row, "create_time"));
        values.put("update_time", now());
        return values;
    }

    private LinkedHashMap<String, Object> deptValues(Map<String, Object> row, Long companyId, Long tenantId, Long targetId) {
        Long oldParentId = longValue(row, "parent_id");
        Long parentId = oldParentId == null || oldParentId <= 0 ? 0L
                : mappingRepository.findTargetId("wk_admin_dept", companyId, oldParentId, "manager_dept");
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("dept_id", targetId);
        values.put("dept_name", trim(firstNonBlank(str(row, "name"), str(row, "dept_name"), "未命名部门"), 100));
        values.put("parent_id", parentId == null ? 0L : parentId);
        values.put("sort_order", intValue(row, "num"));
        values.put("create_time", timestamp(row, "create_time"));
        values.put("tenant_id", tenantId);
        return values;
    }

    private LinkedHashMap<String, Object> roleValues(Map<String, Object> row, Long companyId, Long tenantId, Long targetId) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("role_id", targetId);
        values.put("role_name", trim(firstNonBlank(str(row, "role_name"), "迁移角色"), 64));
        values.put("realm", "wk_role_" + targetId);
        values.put("description", trim(str(row, "remark"), 255));
        values.put("data_type", normalizeDataType(intValue(row, "data_type")));
        values.put("create_user_id", mappedUser(companyId, longValue(row, "create_user_id")));
        values.put("update_user_id", mappedUser(companyId, longValue(row, "update_user_id")));
        values.put("create_time", timestamp(row, "create_time"));
        values.put("update_time", timestamp(row, "update_time"));
        values.put("tenant_id", tenantId);
        return values;
    }

    private LinkedHashMap<String, Object> userValues(Map<String, Object> row, Long companyId, Long tenantId, Long targetId) {
        Long oldDeptId = longValue(row, "dept_id");
        Long deptId = oldDeptId == null ? null : mappingRepository.findTargetId("wk_admin_dept", companyId, oldDeptId, "manager_dept");
        Long oldParentId = longValue(row, "parent_id");
        Long parentId = oldParentId == null || oldParentId <= 0 ? 0L
                : mappingRepository.findTargetId("wk_admin_user", companyId, oldParentId, "manager_user");
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("user_id", targetId);
        values.put("username", trim(firstNonBlank(str(row, "username"), "wk_user_" + targetId), 64));
        values.put("password", passwordEncoder.encode(properties.getResetPassword()));
        values.put("salt", trim(str(row, "salt"), 64));
        values.put("img", trim(firstNonBlank(str(row, "wx_avatar"), str(row, "img")), 255));
        values.put("create_time", firstTimestamp(row, "create_time", now()));
        values.put("realname", trim(firstNonBlank(str(row, "realname"), "未命名用户"), 64));
        values.put("num", trim(str(row, "num"), 64));
        values.put("mobile", trim(firstNonBlank(str(row, "mobile"), str(row, "wx_mobile")), 32));
        values.put("email", trim(str(row, "email"), 128));
        values.put("sex", intValue(row, "sex"));
        values.put("dept_id", deptId);
        values.put("post", trim(str(row, "post"), 64));
        values.put("status", normalizeUserStatus(intValue(row, "status")));
        values.put("parent_id", parentId);
        values.put("tenant_id", tenantId);
        return values;
    }

    private LinkedHashMap<String, Object> userRoleValues(Map<String, Object> row, Long companyId, Long tenantId, Long targetId) {
        Long userId = mappingRepository.findTargetId("wk_admin_user", companyId, longValue(row, "user_id"), "manager_user");
        Long roleId = mappingRepository.findTargetId("wk_admin_role", companyId, longValue(row, "role_id"), "manager_role");
        if (userId == null || roleId == null) {
            throw new IllegalStateException("User-role event is missing user or role mapping.");
        }
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("id", targetId);
        values.put("user_id", userId);
        values.put("role_id", roleId);
        values.put("create_user_id", mappedUser(companyId, longValue(row, "create_user_id")));
        values.put("update_user_id", mappedUser(companyId, longValue(row, "update_user_id")));
        values.put("create_time", timestamp(row, "create_time"));
        values.put("update_time", timestamp(row, "update_time"));
        values.put("tenant_id", tenantId);
        return values;
    }

    private LinkedHashMap<String, Object> customFieldValues(Map<String, Object> row, Long companyId, Long tenantId, Long targetId) {
        String entityType = Objects.equals(intValue(row, "label"), 3) ? "contact" : "customer";
        String columnName = firstNonBlank(str(row, "column_name"), "field_" + targetId);
        ensureCustomColumn(entityType, columnName, "TEXT");
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("field_id", targetId);
        values.put("entity_type", entityType);
        values.put("field_name", columnName);
        values.put("field_label", trim(firstNonBlank(str(row, "name"), str(row, "field_name"), columnName), 100));
        values.put("field_type", "text");
        values.put("field_source", "custom");
        values.put("column_name", columnName);
        values.put("column_type", "TEXT");
        values.put("is_required", Objects.equals(intValue(row, "is_null"), 1) ? 1 : 0);
        values.put("is_searchable", 1);
        values.put("is_show_in_list", Objects.equals(intValue(row, "is_hidden"), 1) ? 0 : 1);
        values.put("is_unique", Objects.equals(intValue(row, "is_unique"), 1) ? 1 : 0);
        values.put("options", toOptionsJson(str(row, "options")));
        values.put("sort_order", intValue(row, "sorting"));
        values.put("status", Objects.equals(intValue(row, "is_hidden"), 1) ? 0 : 1);
        values.put("create_user_id", mappedUser(companyId, longValue(row, "create_user_id")));
        values.put("create_time", timestamp(row, "create_time"));
        values.put("update_time", timestamp(row, "update_time"));
        values.put("tenant_id", tenantId);
        return values;
    }

    private LinkedHashMap<String, Object> customerValues(Map<String, Object> row, Long companyId, Long tenantId, Long targetId) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("customer_id", targetId);
        values.put("company_name", trim(firstNonBlank(str(row, "customer_name"), str(row, "company_name"), "未命名客户"), 255));
        values.put("stage", Objects.equals(intValue(row, "deal_status"), 1) ? "closed" : "lead");
        values.put("owner_id", mappedUser(companyId, firstLong(row, "owner_user_id", "create_user_id")));
        values.put("source", "wk_crm");
        values.put("address", trim(Rows.joinNonBlank(" ", str(row, "address"), str(row, "location"), str(row, "detail_address")), 500));
        values.put("website", trim(str(row, "website"), 255));
        values.put("logo", trim(str(row, "data_img"), 500));
        values.put("last_contact_time", timestamp(row, "last_time"));
        values.put("next_follow_time", timestamp(row, "next_time"));
        values.put("remark", str(row, "remark"));
        values.put("primary_contact_phone", trim(firstNonBlank(str(row, "mobile"), str(row, "telephone")), 50));
        values.put("contact_count", 0);
        values.put("tag_names", "");
        values.put("search_text", buildCustomerSearchText(row));
        values.put("status", Objects.equals(intValue(row, "status"), 3) ? 0 : 1);
        values.put("create_user_id", mappedUser(companyId, longValue(row, "create_user_id")));
        values.put("update_user_id", mappedUser(companyId, longValue(row, "update_user_id")));
        values.put("create_time", firstTimestamp(row, "create_time", now()));
        values.put("update_time", timestamp(row, "update_time"));
        values.put("tenant_id", tenantId);
        return values;
    }

    private LinkedHashMap<String, Object> contactValues(Map<String, Object> row, Long companyId, Long tenantId, Long targetId) {
        Long customerId = mappingRepository.findTargetId("wk_crm_customer", companyId, longValue(row, "customer_id"), "crm_customer");
        if (customerId == null) {
            throw new IllegalStateException("Contact event is missing customer mapping.");
        }
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("contact_id", targetId);
        values.put("customer_id", customerId);
        values.put("name", trim(firstNonBlank(str(row, "name"), "未命名联系人"), 100));
        values.put("position", trim(str(row, "post"), 100));
        values.put("phone", trim(firstNonBlank(str(row, "mobile"), str(row, "telephone")), 50));
        values.put("email", trim(str(row, "email"), 100));
        values.put("is_primary", 0);
        values.put("last_contact_time", timestamp(row, "last_time"));
        values.put("notes", Rows.joinNonBlank("\n", str(row, "remark"), str(row, "address")));
        values.put("status", 1);
        values.put("create_user_id", mappedUser(companyId, longValue(row, "create_user_id")));
        values.put("update_user_id", mappedUser(companyId, longValue(row, "update_user_id")));
        values.put("create_time", timestamp(row, "create_time"));
        values.put("update_time", timestamp(row, "update_time"));
        values.put("tenant_id", tenantId);
        return values;
    }

    private LinkedHashMap<String, Object> followUpValues(Map<String, Object> row, Long companyId, Long tenantId, Long targetId) {
        Long targetCustomerId = mappingRepository.findTargetId("wk_crm_customer", companyId,
                longValue(row, "activity_type_id"), "crm_customer");
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("follow_up_id", targetId);
        values.put("customer_id", targetCustomerId);
        values.put("type", mapFollowUpType(str(row, "category")));
        values.put("content", firstNonBlank(str(row, "content"), "无内容"));
        values.put("scene_type", trim(str(row, "category"), 100));
        values.put("ai_generated", 0);
        values.put("follow_time", firstTimestamp(row, "create_time", now()));
        values.put("next_follow_time", timestamp(row, "next_time"));
        values.put("create_user_id", mappedUser(companyId, longValue(row, "create_user_id")));
        values.put("create_time", timestamp(row, "create_time"));
        values.put("tenant_id", tenantId);
        return values;
    }

    private LinkedHashMap<String, Object> scheduleValues(Map<String, Object> row, Long companyId, Long tenantId, Long targetId) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("schedule_id", targetId);
        values.put("title", trim(firstNonBlank(str(row, "title"), "未命名日程"), 255));
        values.put("start_time", firstTimestamp(row, "start_time", now()));
        values.put("end_time", timestamp(row, "end_time"));
        values.put("type", "meeting");
        values.put("participant_user_ids", str(row, "owner_user_ids"));
        values.put("create_user_id", mappedUser(companyId, longValue(row, "create_user_id")));
        values.put("create_time", timestamp(row, "create_time"));
        values.put("update_time", timestamp(row, "update_time"));
        values.put("tenant_id", tenantId);
        return values;
    }

    private LinkedHashMap<String, Object> taskValues(Map<String, Object> row, Long companyId, Long tenantId,
                                                     Long targetId, String taskType) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("task_id", targetId);
        values.put("title", trim(firstNonBlank(str(row, "name"), "未命名任务"), 255));
        values.put("description", str(row, "description"));
        values.put("due_date", timestamp(row, "stop_time"));
        values.put("priority", mapPriority(intValue(row, "priority")));
        values.put("status", mapTaskStatus(intValue(row, "status")));
        values.put("assigned_to", mappedUser(companyId, longValue(row, "main_user_id")));
        values.put("task_type", taskType);
        values.put("generated_by_ai", 0);
        values.put("high_value", false);
        values.put("completed_time", timestamp(row, "finish_time"));
        values.put("create_user_id", mappedUser(companyId, longValue(row, "create_user_id")));
        values.put("update_user_id", mappedUser(companyId, longValue(row, "update_user_id")));
        values.put("create_time", timestamp(row, "create_time"));
        values.put("update_time", timestamp(row, "update_time"));
        values.put("tenant_id", tenantId);
        return values;
    }

    private Object sourceId(IncrementalSyncEvent event, SourceSpec spec) {
        if (event.sourceId() != null && !event.sourceId().isBlank()) {
            return event.sourceId();
        }
        return firstLong(event.payload(), spec.sourceIdColumn());
    }

    private Long mappedUser(Long companyId, Long oldUserId) {
        return oldUserId == null ? null : mappingRepository.findTargetId("wk_admin_user", companyId, oldUserId, "manager_user");
    }

    private String loadCustomFieldColumn(Long fieldId) {
        if (fieldId == null) {
            return null;
        }
        return target.queryForList("""
                SELECT column_name
                FROM crm_custom_field
                WHERE field_id = ?
                LIMIT 1
                """, String.class, fieldId).stream().findFirst().map(this::nullSafeString).orElse(null);
    }

    private void ensureCustomColumn(String entityType, String columnName, String columnType) {
        String targetTable = "contact".equals(entityType) ? "crm_contact" : "crm_customer";
        targetSchema.addCustomFieldColumnIfMissing(targetTable, columnName, columnType);
    }

    private Long longValue(Map<String, Object> row, String key) {
        try {
            return Rows.longValue(row, key);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer intValue(Map<String, Object> row, String key) {
        try {
            return Rows.intValue(row, key);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String str(Map<String, Object> row, String key) {
        return row == null ? null : Rows.str(row, key);
    }

    private Timestamp timestamp(Map<String, Object> row, String key) {
        try {
            return Rows.timestamp(row, key);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Timestamp firstTimestamp(Map<String, Object> row, String key, Timestamp defaultValue) {
        Timestamp value = timestamp(row, key);
        return value == null ? defaultValue : value;
    }

    private Timestamp now() {
        return Timestamp.valueOf(LocalDateTime.now());
    }

    private Long firstLong(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Long value = longValue(row, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstString(Map<String, Object> row, String... keys) {
        if (row == null) {
            return null;
        }
        for (String key : keys) {
            String value = str(row, key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String trim(String value, int length) {
        return Rows.trimToLength(value, length);
    }

    private String nullSafeString(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String buildCustomerSearchText(Map<String, Object> row) {
        String text = Rows.joinNonBlank(" ", str(row, "customer_name"), str(row, "mobile"),
                str(row, "telephone"), str(row, "email"), str(row, "website"),
                str(row, "address"), str(row, "location"), str(row, "detail_address"),
                str(row, "remark"), str(row, "last_content"));
        return text == null ? null : text.toLowerCase(Locale.ROOT);
    }

    private String mapFollowUpType(String category) {
        if (category == null) {
            return "other";
        }
        String normalized = category.toLowerCase(Locale.ROOT);
        if (normalized.contains("call") || normalized.contains("电话")) {
            return "call";
        }
        if (normalized.contains("mail") || normalized.contains("email") || normalized.contains("邮件")) {
            return "email";
        }
        if (normalized.contains("visit") || normalized.contains("拜访")) {
            return "visit";
        }
        if (normalized.contains("meeting") || normalized.contains("会议")) {
            return "meeting";
        }
        return "other";
    }

    private String mapPriority(Integer priority) {
        if (priority == null) {
            return "medium";
        }
        if (priority >= 3) {
            return "high";
        }
        if (priority == 1) {
            return "low";
        }
        return "medium";
    }

    private String mapTaskStatus(Integer status) {
        if (status == null) {
            return "pending";
        }
        return switch (status) {
            case 2 -> "in_progress";
            case 3, 5, 6 -> "completed";
            default -> "pending";
        };
    }

    private Integer normalizeDataType(Integer dataType) {
        return dataType == null || dataType < 1 || dataType > 5 ? 5 : dataType;
    }

    private Integer normalizeUserStatus(Integer status) {
        if (status == null) {
            return 2;
        }
        return switch (status) {
            case 0 -> 0;
            case 1 -> 1;
            default -> 2;
        };
    }

    private String toOptionsJson(String options) {
        if (options == null || options.isBlank()) {
            return null;
        }
        String trimmed = options.trim();
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            return trimmed;
        }
        try {
            return objectMapper.writeValueAsString(trimmed.split("[,，\\n]"));
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private record SourceSpec(String targetTable, String sourceIdColumn, String targetKeyColumn) {
    }
}
