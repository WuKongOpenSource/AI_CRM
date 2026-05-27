package com.kakarote.syncdata.incremental;

import com.kakarote.syncdata.util.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class IncrementalEventLogRepository {

    private final JdbcTemplate target;
    private final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(14);

    public IncrementalEventLogRepository(@Qualifier("targetJdbcTemplate") JdbcTemplate targetJdbcTemplate) {
        this.target = targetJdbcTemplate;
    }

    public String existingTerminalStatus(String dedupKey) {
        if (dedupKey == null || dedupKey.isBlank()) {
            return null;
        }
        try {
            String status = target.queryForObject("""
                    SELECT consume_status
                    FROM sync_incremental_event_log
                    WHERE dedup_key = ?
                    ORDER BY create_time DESC
                    LIMIT 1
                    """, String.class, dedupKey);
            if (status == null) {
                return null;
            }
            return switch (status) {
                case "applied", "published", "waiting_ack", "ignored", "conflict_skipped", "pending_manual" -> status;
                default -> null;
            };
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    public void markReceived(IncrementalSyncEvent event, Long bindingId, String rawPayload) {
        target.update("""
                INSERT INTO sync_incremental_event_log(
                    event_log_id, binding_id, direction, event_type, event_id, dedup_key, origin_system,
                    source_system, target_system, tenant_id, source_company_id, entity_type,
                    source_table, source_id, target_id, operation, schema_version, trace_id,
                    event_time, consume_status, raw_payload
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'received', ?)
                ON CONFLICT (dedup_key)
                DO UPDATE SET attempt_count = sync_incremental_event_log.attempt_count + 1,
                              event_type = EXCLUDED.event_type,
                              consume_status = 'received',
                              error_message = NULL,
                              raw_payload = EXCLUDED.raw_payload,
                              update_time = CURRENT_TIMESTAMP
                """,
                idGenerator.nextId(),
                bindingId,
                event.resolvedDirection().name(),
                event.resolvedType(),
                event.resolvedEventId(),
                event.dedupKey(),
                event.originSystem(),
                event.resolvedSourceSystem(),
                event.resolvedTargetSystem(),
                event.tenantId(),
                event.sourceCompanyId() == null ? 0L : event.sourceCompanyId(),
                event.entityType(),
                event.sourceTable(),
                event.sourceId(),
                event.targetId(),
                event.resolvedOperation(),
                event.schemaVersion() == null ? "1" : event.schemaVersion(),
                event.traceId(),
                toTimestamp(event.eventTime()),
                rawPayload
        );
    }

    public void markStatus(IncrementalSyncEvent event, String status, String message) {
        target.update("""
                UPDATE sync_incremental_event_log
                SET consume_status = ?,
                    error_message = ?,
                    applied_at = CASE WHEN ? IN ('applied', 'published', 'waiting_ack') THEN CURRENT_TIMESTAMP ELSE applied_at END,
                    update_time = CURRENT_TIMESTAMP
                WHERE dedup_key = ?
                """, status, message, status, event.dedupKey());
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
