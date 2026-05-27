package com.kakarote.syncdata.incremental;

import com.kakarote.syncdata.util.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

@Repository
public class EntitySyncStateRepository {

    private final JdbcTemplate target;
    private final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(15);

    public EntitySyncStateRepository(@Qualifier("targetJdbcTemplate") JdbcTemplate targetJdbcTemplate) {
        this.target = targetJdbcTemplate;
    }

    public boolean shouldApplyCrmEvent(IncrementalSyncEvent event, String targetTable, Long targetId) {
        Instant eventTime = event.eventTime();
        if (eventTime == null || targetId == null) {
            return true;
        }
        try {
            Timestamp lastAicrmEventTime = target.queryForObject("""
                    SELECT last_aicrm_event_time
                    FROM sync_entity_state
                    WHERE source_system = ?
                      AND source_table = ?
                      AND source_company_id = ?
                      AND source_id = ?
                      AND target_table = ?
                    """, Timestamp.class, event.resolvedSourceSystem(), event.sourceTable(),
                    safeCompanyId(event.sourceCompanyId()), event.sourceId(), targetTable);
            return lastAicrmEventTime == null || !lastAicrmEventTime.toInstant().isAfter(eventTime);
        } catch (EmptyResultDataAccessException ignored) {
            return true;
        }
    }

    public void markApplied(IncrementalSyncEvent event, Long bindingId, String targetTable, Long targetId, Long tenantId) {
        if (targetId == null || event.sourceTable() == null || event.sourceId() == null) {
            return;
        }
        boolean crmDirection = event.resolvedDirection() == SyncDirection.CRM_TO_AICRM;
        target.update("""
                INSERT INTO sync_entity_state(
                    state_id, binding_id, source_system, source_company_id, source_table, source_id,
                    target_table, target_id, tenant_id, last_crm_event_time, last_aicrm_event_time,
                    last_direction, last_trace_id, status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'active')
                ON CONFLICT (source_system, source_table, source_company_id, source_id, target_table)
                DO UPDATE SET binding_id = EXCLUDED.binding_id,
                              target_id = EXCLUDED.target_id,
                              tenant_id = COALESCE(EXCLUDED.tenant_id, sync_entity_state.tenant_id),
                              last_crm_event_time = COALESCE(EXCLUDED.last_crm_event_time, sync_entity_state.last_crm_event_time),
                              last_aicrm_event_time = COALESCE(EXCLUDED.last_aicrm_event_time, sync_entity_state.last_aicrm_event_time),
                              last_direction = EXCLUDED.last_direction,
                              last_trace_id = EXCLUDED.last_trace_id,
                              status = EXCLUDED.status,
                              update_time = CURRENT_TIMESTAMP
                """,
                idGenerator.nextId(),
                bindingId,
                event.resolvedSourceSystem(),
                safeCompanyId(event.sourceCompanyId()),
                event.sourceTable(),
                event.sourceId(),
                targetTable,
                targetId,
                tenantId,
                crmDirection ? toTimestamp(event.eventTime()) : null,
                crmDirection ? null : toTimestamp(event.eventTime()),
                event.resolvedDirection().name(),
                event.traceId()
        );
    }

    public void markWaitingAck(IncrementalSyncEvent event, Long bindingId, String targetTable, Long targetId, Long tenantId) {
        markApplied(event, bindingId, targetTable, targetId, tenantId);
        if (targetId != null) {
            target.update("""
                    UPDATE sync_entity_state
                    SET status = 'waiting_ack',
                        update_time = CURRENT_TIMESTAMP
                    WHERE target_table = ?
                      AND target_id = ?
                    """, targetTable, targetId);
        }
    }

    public void markAcked(String sourceSystem, Long sourceCompanyId, String sourceTable,
                          String sourceId, String targetTable, Long targetId) {
        target.update("""
                UPDATE sync_entity_state
                SET status = 'active',
                    update_time = CURRENT_TIMESTAMP
                WHERE source_system = ?
                  AND source_company_id = ?
                  AND source_table = ?
                  AND source_id = ?
                  AND target_table = ?
                  AND target_id = ?
                """, sourceSystem, safeCompanyId(sourceCompanyId), sourceTable, sourceId, targetTable, targetId);
    }

    public Long payloadLong(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return null;
        }
        return Long.valueOf(text);
    }

    private long safeCompanyId(Long sourceCompanyId) {
        return sourceCompanyId == null ? 0L : sourceCompanyId;
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
