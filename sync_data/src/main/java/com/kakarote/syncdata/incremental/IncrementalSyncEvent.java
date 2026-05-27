package com.kakarote.syncdata.incremental;

import java.time.Instant;
import java.util.Map;

/**
 * Standard event shape shared by HTTP replay, RocketMQ consumers, and AICRM publishers.
 */
public record IncrementalSyncEvent(
        String direction,
        String type,
        String eventId,
        String traceId,
        String originSystem,
        String sourceSystem,
        String targetSystem,
        Long tenantId,
        Long sourceCompanyId,
        String entityType,
        String sourceTable,
        String sourceId,
        String targetId,
        String operation,
        String offset,
        Instant eventTime,
        Map<String, Object> payload,
        String rawPayload,
        String schemaVersion
) {

    public IncrementalSyncEvent(
            String sourceSystem,
            Long sourceCompanyId,
            String sourceTable,
            String sourceId,
            String operation,
            String traceId,
            String offset,
            Instant eventTime,
            Map<String, Object> payload,
            String rawPayload
    ) {
        this(
                SyncDirection.CRM_TO_AICRM.name(),
                null,
                null,
                traceId,
                sourceSystem == null ? "crm" : sourceSystem,
                sourceSystem == null ? "wk_crm" : sourceSystem,
                "aicrm",
                null,
                sourceCompanyId,
                null,
                sourceTable,
                sourceId,
                null,
                operation,
                offset,
                eventTime,
                payload,
                rawPayload,
                "1"
        );
    }

    public SyncDirection resolvedDirection() {
        if (direction == null || direction.isBlank()) {
            return SyncDirection.CRM_TO_AICRM;
        }
        return SyncDirection.valueOf(direction.trim().toUpperCase());
    }

    public String resolvedOperation() {
        return operation == null || operation.isBlank()
                ? SyncOperation.UPDATE.name()
                : operation.trim().toUpperCase();
    }

    public String resolvedType() {
        if (type != null && !type.isBlank()) {
            return type.trim().toUpperCase();
        }
        if (SyncOperation.ACK.name().equals(resolvedOperation())) {
            return resolvedDirection() == SyncDirection.CRM_TO_AICRM
                    ? "CRM_ACK_AICRM_CREATE"
                    : "AICRM_ACK_CRM_CREATE";
        }
        String prefix = resolvedDirection() == SyncDirection.CRM_TO_AICRM ? "CRM" : "AICRM";
        return prefix + "_" + eventEntityName() + "_CHANGED";
    }

    public String resolvedSourceSystem() {
        if (sourceSystem != null && !sourceSystem.isBlank()) {
            return sourceSystem;
        }
        return resolvedDirection() == SyncDirection.CRM_TO_AICRM ? "wk_crm" : "aicrm";
    }

    public String resolvedTargetSystem() {
        if (targetSystem != null && !targetSystem.isBlank()) {
            return targetSystem;
        }
        return resolvedDirection() == SyncDirection.CRM_TO_AICRM ? "aicrm" : "wk_crm";
    }

    public String resolvedEventId() {
        if (eventId != null && !eventId.isBlank()) {
            return eventId;
        }
        return resolvedDirection() + ":" + sourceCompanyId + ":" + sourceTable + ":" + sourceId
                + ":" + resolvedOperation() + ":" + (eventTime == null ? "none" : eventTime.toEpochMilli());
    }

    public String dedupKey() {
        return resolvedDirection() + ":" + resolvedEventId();
    }

    private String eventEntityName() {
        if (entityType != null && !entityType.isBlank()) {
            return normalize(entityType);
        }
        if (sourceTable == null || sourceTable.isBlank()) {
            return "SYNC";
        }
        return switch (sourceTable) {
            case "wk_admin_company", "crm_tenant" -> "COMPANY";
            case "wk_admin_dept", "manager_dept" -> "DEPT";
            case "wk_admin_role", "manager_role" -> "ROLE";
            case "wk_admin_user", "manager_user" -> "USER";
            case "wk_admin_user_role", "manager_user_role" -> "USER_ROLE";
            case "wk_crm_field", "crm_custom_field" -> "CUSTOM_FIELD";
            case "wk_crm_customer", "crm_customer" -> "CUSTOMER";
            case "wk_crm_contacts", "crm_contact" -> "CONTACT";
            case "wk_crm_customer_data" -> "CUSTOMER_FIELD_VALUE";
            case "wk_crm_contacts_data" -> "CONTACT_FIELD_VALUE";
            case "wk_crm_activity", "crm_follow_up" -> "FOLLOW_UP";
            case "wk_oa_event", "crm_schedule" -> "SCHEDULE";
            case "wk_project_task" -> "PROJECT_TASK";
            case "wk_work_task" -> "WORK_TASK";
            case "crm_task" -> "TASK";
            default -> normalize(sourceTable);
        };
    }

    private String normalize(String value) {
        return value.trim()
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .replace('-', '_')
                .replace('.', '_')
                .toUpperCase();
    }
}
