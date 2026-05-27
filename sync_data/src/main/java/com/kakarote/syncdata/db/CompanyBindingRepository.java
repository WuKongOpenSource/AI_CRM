package com.kakarote.syncdata.db;

import com.kakarote.syncdata.model.CompanyBinding;
import com.kakarote.syncdata.util.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class CompanyBindingRepository {

    private static final String SOURCE_SYSTEM = "wk_crm";

    private final JdbcTemplate target;
    private final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(12);

    private final RowMapper<CompanyBinding> rowMapper = (rs, rowNum) -> new CompanyBinding(
            rs.getLong("binding_id"),
            rs.getLong("tenant_id"),
            rs.getString("source_system"),
            rs.getString("source_db"),
            rs.getLong("source_company_id"),
            rs.getString("source_company_name"),
            rs.getString("sync_direction"),
            rs.getString("full_sync_status"),
            rs.getObject("full_sync_job_id", Long.class),
            rs.getTimestamp("last_full_sync_at"),
            rs.getBoolean("incremental_enabled"),
            rs.getString("mq_topic"),
            rs.getString("mq_group"),
            rs.getTimestamp("last_incremental_event_time"),
            rs.getString("last_incremental_offset"),
            rs.getBoolean("crm_to_aicrm_enabled"),
            rs.getBoolean("aicrm_to_crm_enabled"),
            rs.getString("crm_to_aicrm_topic"),
            rs.getString("crm_to_aicrm_group"),
            rs.getString("aicrm_to_crm_topic"),
            rs.getString("aicrm_to_crm_group"),
            rs.getTimestamp("last_crm_to_aicrm_event_time"),
            rs.getString("last_crm_to_aicrm_offset"),
            rs.getTimestamp("last_aicrm_to_crm_event_time"),
            rs.getString("last_aicrm_to_crm_offset"),
            rs.getInt("status"),
            rs.getString("remark"),
            rs.getTimestamp("create_time"),
            rs.getTimestamp("update_time")
    );

    /**
     * 使用目标 ai_crm 数据库持久化和查询公司绑定关系。
     */
    public CompanyBindingRepository(@Qualifier("targetJdbcTemplate") JdbcTemplate targetJdbcTemplate) {
        this.target = targetJdbcTemplate;
    }

    /**
     * 插入或更新某个租户的绑定关系，并返回持久化后的记录。
     */
    public CompanyBinding upsertBinding(Long tenantId, Long companyId, String companyName,
                                        boolean incrementalEnabled, String mqTopic,
                                        String mqGroup, String remark) {
        return upsertBinding(tenantId, companyId, companyName,
                incrementalEnabled, false, mqTopic, mqGroup, mqTopic, null, remark);
    }

    public CompanyBinding upsertBinding(Long tenantId, Long companyId, String companyName,
                                        boolean crmToAicrmEnabled, boolean aicrmToCrmEnabled,
                                        String crmToAicrmTopic, String crmToAicrmGroup,
                                        String aicrmToCrmTopic, String aicrmToCrmGroup,
                                        String remark) {
        target.update("""
                INSERT INTO sync_company_binding(
                    binding_id, tenant_id, source_system, source_db, source_company_id, source_company_name,
                    incremental_enabled, mq_topic, mq_group,
                    crm_to_aicrm_enabled, aicrm_to_crm_enabled,
                    crm_to_aicrm_topic, crm_to_aicrm_group,
                    aicrm_to_crm_topic, aicrm_to_crm_group,
                    status, remark
                )
                VALUES (?, ?, ?, 'wk_crm', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?)
                ON CONFLICT (tenant_id)
                DO UPDATE SET source_company_id = EXCLUDED.source_company_id,
                              source_company_name = EXCLUDED.source_company_name,
                              incremental_enabled = EXCLUDED.incremental_enabled,
                              mq_topic = EXCLUDED.mq_topic,
                              mq_group = EXCLUDED.mq_group,
                              crm_to_aicrm_enabled = EXCLUDED.crm_to_aicrm_enabled,
                              aicrm_to_crm_enabled = EXCLUDED.aicrm_to_crm_enabled,
                              crm_to_aicrm_topic = EXCLUDED.crm_to_aicrm_topic,
                              crm_to_aicrm_group = EXCLUDED.crm_to_aicrm_group,
                              aicrm_to_crm_topic = EXCLUDED.aicrm_to_crm_topic,
                              aicrm_to_crm_group = EXCLUDED.aicrm_to_crm_group,
                              status = 1,
                              remark = EXCLUDED.remark,
                              update_time = CURRENT_TIMESTAMP
                """, idGenerator.nextId(), tenantId, SOURCE_SYSTEM, companyId, companyName,
                crmToAicrmEnabled, crmToAicrmTopic, crmToAicrmGroup,
                crmToAicrmEnabled, aicrmToCrmEnabled,
                crmToAicrmTopic, crmToAicrmGroup,
                aicrmToCrmTopic, aicrmToCrmGroup,
                remark);
        return findByTenantId(tenantId);
    }

    /**
     * 按最近更新时间倒序返回所有已配置绑定关系。
     */
    public List<CompanyBinding> listBindings() {
        return target.query("""
                SELECT *
                FROM sync_company_binding
                ORDER BY update_time DESC, binding_id DESC
                """, rowMapper);
    }

    /**
     * 根据主键查询绑定关系，不存在时返回 null。
     */
    public CompanyBinding findById(Long bindingId) {
        try {
            return target.queryForObject("""
                    SELECT *
                    FROM sync_company_binding
                    WHERE binding_id = ?
                    """, rowMapper, bindingId);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    /**
     * 根据 ai_crm tenant_id 查询绑定关系，不存在时返回 null。
     */
    public CompanyBinding findByTenantId(Long tenantId) {
        try {
            return target.queryForObject("""
                    SELECT *
                    FROM sync_company_binding
                    WHERE tenant_id = ?
                    """, rowMapper, tenantId);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    /**
     * 根据 wk_crm company_id 查询启用中的绑定关系，不存在时返回 null。
     */
    public CompanyBinding findActiveByCompanyId(Long companyId) {
        try {
            return target.queryForObject("""
                    SELECT *
                    FROM sync_company_binding
                    WHERE source_system = ?
                      AND source_company_id = ?
                      AND status = 1
                    """, rowMapper, SOURCE_SYSTEM, companyId);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    /**
     * 在全量同步开始前，将绑定关系标记为运行中。
     */
    public void markFullSyncRunning(Long bindingId, Long jobId) {
        if (bindingId == null) {
            return;
        }
        target.update("""
                UPDATE sync_company_binding
                SET full_sync_status = 'running',
                    full_sync_job_id = ?,
                    update_time = CURRENT_TIMESTAMP
                WHERE binding_id = ?
                """, jobId, bindingId);
    }

    /**
     * 保存绑定关系的全量同步最终状态和完成时间。
     */
    public void markFullSyncFinished(Long bindingId, Long jobId, boolean success) {
        if (bindingId == null) {
            return;
        }
        target.update("""
                UPDATE sync_company_binding
                SET full_sync_status = ?,
                    full_sync_job_id = ?,
                    last_full_sync_at = CURRENT_TIMESTAMP,
                    update_time = CURRENT_TIMESTAMP
                WHERE binding_id = ?
                """, success ? "completed" : "completed_with_errors", jobId, bindingId);
    }

    /**
     * 当全量同步任务异常中止时，将绑定关系标记为失败。
     */
    public void markFullSyncFailed(Long bindingId, Long jobId) {
        if (bindingId == null) {
            return;
        }
        target.update("""
                UPDATE sync_company_binding
                SET full_sync_status = 'failed',
                    full_sync_job_id = ?,
                    update_time = CURRENT_TIMESTAMP
                WHERE binding_id = ?
                """, jobId, bindingId);
    }

    /**
     * 将服务重启前残留的运行中绑定标记为失败，避免前端一直显示同步中。
     */
    public int markRunningFullSyncInterrupted() {
        return target.update("""
                UPDATE sync_company_binding
                SET full_sync_status = 'failed',
                    update_time = CURRENT_TIMESTAMP
                WHERE full_sync_status = 'running'
                """);
    }

    /**
     * 将不属于当前进程活跃任务的运行中绑定标记为失败。
     */
    public int markRunningFullSyncInterruptedExcept(Collection<Long> activeJobIds) {
        if (activeJobIds == null || activeJobIds.isEmpty()) {
            return markRunningFullSyncInterrupted();
        }
        String placeholders = String.join(", ", activeJobIds.stream().map(ignored -> "?").toList());
        return target.update("""
                UPDATE sync_company_binding
                SET full_sync_status = 'failed',
                    update_time = CURRENT_TIMESTAMP
                WHERE full_sync_status = 'running'
                  AND (full_sync_job_id IS NULL OR full_sync_job_id NOT IN (%s))
                """.formatted(placeholders), activeJobIds.toArray());
    }

    /**
     * 更新绑定关系最近消费的增量位点和消费时间。
     */
    public void updateIncrementalCheckpoint(Long bindingId, String offset) {
        target.update("""
                UPDATE sync_company_binding
                SET last_incremental_event_time = CURRENT_TIMESTAMP,
                    last_incremental_offset = ?,
                    last_crm_to_aicrm_event_time = CURRENT_TIMESTAMP,
                    last_crm_to_aicrm_offset = ?,
                    update_time = CURRENT_TIMESTAMP
                WHERE binding_id = ?
                """, offset, offset, bindingId);
    }

    public void updateDirectionalCheckpoint(Long bindingId, String direction, String offset) {
        if (bindingId == null) {
            return;
        }
        if ("AICRM_TO_CRM".equals(direction)) {
            target.update("""
                    UPDATE sync_company_binding
                    SET last_aicrm_to_crm_event_time = CURRENT_TIMESTAMP,
                        last_aicrm_to_crm_offset = ?,
                        update_time = CURRENT_TIMESTAMP
                    WHERE binding_id = ?
                    """, offset, bindingId);
            return;
        }
        target.update("""
                UPDATE sync_company_binding
                SET last_incremental_event_time = CURRENT_TIMESTAMP,
                    last_incremental_offset = ?,
                    last_crm_to_aicrm_event_time = CURRENT_TIMESTAMP,
                    last_crm_to_aicrm_offset = ?,
                    update_time = CURRENT_TIMESTAMP
                WHERE binding_id = ?
                """, offset, offset, bindingId);
    }
}
