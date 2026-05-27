-- sync_data target schema bootstrap for AI CRM PostgreSQL.
-- Execute this script before running full or incremental sync.

CREATE TABLE IF NOT EXISTS sync_full_job (
    job_id BIGINT PRIMARY KEY,
    sync_mode VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP,
    total_count BIGINT DEFAULT 0,
    success_count BIGINT DEFAULT 0,
    fail_count BIGINT DEFAULT 0,
    message TEXT
);

CREATE TABLE IF NOT EXISTS sync_job_module (
    id BIGINT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    module_name VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    source_table VARCHAR(64),
    target_table VARCHAR(64),
    total_count BIGINT DEFAULT 0,
    success_count BIGINT DEFAULT 0,
    fail_count BIGINT DEFAULT 0,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP,
    message TEXT,
    CONSTRAINT uk_sync_job_module UNIQUE (job_id, module_name)
);

CREATE TABLE IF NOT EXISTS sync_job_error (
    id BIGINT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    module_name VARCHAR(64) NOT NULL,
    source_table VARCHAR(64),
    source_company_id BIGINT,
    source_id VARCHAR(128),
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sync_company_binding (
    binding_id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    source_system VARCHAR(32) NOT NULL DEFAULT 'wk_crm',
    source_db VARCHAR(64) NOT NULL DEFAULT 'wk_crm',
    source_company_id BIGINT NOT NULL,
    source_company_name VARCHAR(255),
    sync_direction VARCHAR(32) NOT NULL DEFAULT 'old_to_new',
    full_sync_status VARCHAR(32) NOT NULL DEFAULT 'not_started',
    full_sync_job_id BIGINT,
    last_full_sync_at TIMESTAMP,
    incremental_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    mq_topic VARCHAR(128),
    mq_group VARCHAR(128),
    last_incremental_event_time TIMESTAMP,
    last_incremental_offset VARCHAR(255),
    crm_to_aicrm_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    aicrm_to_crm_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    crm_to_aicrm_topic VARCHAR(128),
    crm_to_aicrm_group VARCHAR(128),
    aicrm_to_crm_topic VARCHAR(128),
    aicrm_to_crm_group VARCHAR(128),
    last_crm_to_aicrm_event_time TIMESTAMP,
    last_crm_to_aicrm_offset VARCHAR(255),
    last_aicrm_to_crm_event_time TIMESTAMP,
    last_aicrm_to_crm_offset VARCHAR(255),
    status SMALLINT NOT NULL DEFAULT 1,
    remark TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sync_company_binding_tenant UNIQUE (tenant_id),
    CONSTRAINT uk_sync_company_binding_source UNIQUE (source_system, source_company_id)
);

CREATE TABLE IF NOT EXISTS sync_incremental_event_log (
    event_log_id BIGINT PRIMARY KEY,
    binding_id BIGINT,
    direction VARCHAR(32) NOT NULL DEFAULT 'CRM_TO_AICRM',
    event_type VARCHAR(96),
    event_id VARCHAR(128),
    dedup_key VARCHAR(255),
    origin_system VARCHAR(32),
    source_system VARCHAR(32) NOT NULL DEFAULT 'wk_crm',
    target_system VARCHAR(32),
    tenant_id BIGINT,
    source_company_id BIGINT NOT NULL,
    entity_type VARCHAR(64),
    source_table VARCHAR(64) NOT NULL,
    source_id VARCHAR(128),
    target_id VARCHAR(128),
    operation VARCHAR(32) NOT NULL,
    schema_version VARCHAR(32),
    attempt_count INTEGER NOT NULL DEFAULT 1,
    trace_id VARCHAR(128),
    event_time TIMESTAMP,
    consume_status VARCHAR(32) NOT NULL DEFAULT 'received',
    error_message TEXT,
    raw_payload TEXT,
    applied_at TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sync_entity_state (
    state_id BIGINT PRIMARY KEY,
    binding_id BIGINT,
    source_system VARCHAR(32) NOT NULL DEFAULT 'wk_crm',
    source_company_id BIGINT NOT NULL DEFAULT 0,
    source_table VARCHAR(64) NOT NULL,
    source_id VARCHAR(128) NOT NULL,
    target_table VARCHAR(64) NOT NULL,
    target_id BIGINT NOT NULL,
    tenant_id BIGINT,
    last_crm_event_time TIMESTAMP,
    last_aicrm_event_time TIMESTAMP,
    last_direction VARCHAR(32),
    last_trace_id VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sync_entity_state UNIQUE
        (source_system, source_table, source_company_id, source_id, target_table)
);

CREATE TABLE IF NOT EXISTS sync_mapping (
    id BIGINT PRIMARY KEY,
    source_system VARCHAR(32) NOT NULL DEFAULT 'wk_crm',
    source_table VARCHAR(64) NOT NULL,
    source_company_id BIGINT NOT NULL DEFAULT 0,
    source_id VARCHAR(128) NOT NULL,
    target_table VARCHAR(64) NOT NULL,
    target_id BIGINT NOT NULL,
    tenant_id BIGINT,
    source_hash VARCHAR(128),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sync_mapping UNIQUE
        (source_system, source_table, source_company_id, source_id, target_table)
);

ALTER TABLE IF EXISTS sync_full_job ALTER COLUMN job_id DROP DEFAULT;
ALTER TABLE IF EXISTS sync_job_module ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS sync_job_error ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS sync_company_binding ALTER COLUMN binding_id DROP DEFAULT;
ALTER TABLE IF EXISTS sync_incremental_event_log ALTER COLUMN event_log_id DROP DEFAULT;
ALTER TABLE IF EXISTS sync_mapping ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS sync_entity_state ALTER COLUMN state_id DROP DEFAULT;

ALTER TABLE sync_job_module ADD COLUMN IF NOT EXISTS status VARCHAR(32) DEFAULT 'pending';

ALTER TABLE sync_company_binding ADD COLUMN IF NOT EXISTS crm_to_aicrm_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sync_company_binding ADD COLUMN IF NOT EXISTS aicrm_to_crm_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sync_company_binding ADD COLUMN IF NOT EXISTS crm_to_aicrm_topic VARCHAR(128);
ALTER TABLE sync_company_binding ADD COLUMN IF NOT EXISTS crm_to_aicrm_group VARCHAR(128);
ALTER TABLE sync_company_binding ADD COLUMN IF NOT EXISTS aicrm_to_crm_topic VARCHAR(128);
ALTER TABLE sync_company_binding ADD COLUMN IF NOT EXISTS aicrm_to_crm_group VARCHAR(128);
ALTER TABLE sync_company_binding ADD COLUMN IF NOT EXISTS last_crm_to_aicrm_event_time TIMESTAMP;
ALTER TABLE sync_company_binding ADD COLUMN IF NOT EXISTS last_crm_to_aicrm_offset VARCHAR(255);
ALTER TABLE sync_company_binding ADD COLUMN IF NOT EXISTS last_aicrm_to_crm_event_time TIMESTAMP;
ALTER TABLE sync_company_binding ADD COLUMN IF NOT EXISTS last_aicrm_to_crm_offset VARCHAR(255);

ALTER TABLE sync_incremental_event_log ADD COLUMN IF NOT EXISTS direction VARCHAR(32) NOT NULL DEFAULT 'CRM_TO_AICRM';
ALTER TABLE sync_incremental_event_log ADD COLUMN IF NOT EXISTS event_type VARCHAR(96);
ALTER TABLE sync_incremental_event_log ADD COLUMN IF NOT EXISTS event_id VARCHAR(128);
ALTER TABLE sync_incremental_event_log ADD COLUMN IF NOT EXISTS dedup_key VARCHAR(255);
ALTER TABLE sync_incremental_event_log ADD COLUMN IF NOT EXISTS origin_system VARCHAR(32);
ALTER TABLE sync_incremental_event_log ADD COLUMN IF NOT EXISTS target_system VARCHAR(32);
ALTER TABLE sync_incremental_event_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE sync_incremental_event_log ADD COLUMN IF NOT EXISTS entity_type VARCHAR(64);
ALTER TABLE sync_incremental_event_log ADD COLUMN IF NOT EXISTS target_id VARCHAR(128);
ALTER TABLE sync_incremental_event_log ADD COLUMN IF NOT EXISTS schema_version VARCHAR(32);
ALTER TABLE sync_incremental_event_log ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 1;
ALTER TABLE sync_incremental_event_log ADD COLUMN IF NOT EXISTS applied_at TIMESTAMP;
ALTER TABLE sync_incremental_event_log ADD COLUMN IF NOT EXISTS update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_sync_company_binding_company
    ON sync_company_binding(source_system, source_company_id);
CREATE INDEX IF NOT EXISTS idx_sync_company_binding_tenant
    ON sync_company_binding(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sync_incremental_event_binding
    ON sync_incremental_event_log(binding_id, create_time);
CREATE INDEX IF NOT EXISTS idx_sync_incremental_event_source
    ON sync_incremental_event_log(source_company_id, source_table, source_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sync_incremental_event_dedup
    ON sync_incremental_event_log(dedup_key) WHERE dedup_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_sync_incremental_event_direction
    ON sync_incremental_event_log(direction, consume_status, create_time);
CREATE INDEX IF NOT EXISTS idx_sync_incremental_event_type
    ON sync_incremental_event_log(event_type, create_time);
CREATE INDEX IF NOT EXISTS idx_sync_entity_state_target
    ON sync_entity_state(target_table, target_id);
CREATE INDEX IF NOT EXISTS idx_sync_entity_state_binding
    ON sync_entity_state(binding_id);
CREATE INDEX IF NOT EXISTS idx_sync_mapping_target
    ON sync_mapping(target_table, target_id);
CREATE INDEX IF NOT EXISTS idx_sync_mapping_tenant
    ON sync_mapping(tenant_id);

CREATE OR REPLACE FUNCTION sync_mapping_update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext('sync_mapping_update_time_trigger')::bigint);
    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'trg_sync_mapping_update_time'
          AND tgrelid = 'sync_mapping'::regclass
          AND NOT tgisinternal
    ) THEN
        EXECUTE 'CREATE TRIGGER trg_sync_mapping_update_time
            BEFORE UPDATE ON sync_mapping
            FOR EACH ROW EXECUTE FUNCTION sync_mapping_update_timestamp()';
    END IF;
END;
$$;

CREATE TABLE IF NOT EXISTS crm_tenant (
    tenant_id BIGINT PRIMARY KEY,
    tenant_name VARCHAR(100) NOT NULL,
    contact_name VARCHAR(50),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    status INTEGER NOT NULL DEFAULT 1,
    expire_time TIMESTAMP,
    max_users INTEGER DEFAULT 50,
    gift_credit_total BIGINT DEFAULT 300,
    gift_credit_used BIGINT DEFAULT 0,
    purchased_credit_total BIGINT DEFAULT 0,
    purchased_credit_used BIGINT DEFAULT 0,
    remark VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS manager_dept (
    dept_id BIGINT PRIMARY KEY,
    dept_name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    sort_order INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT
);

CREATE TABLE IF NOT EXISTS crm_schedule (
    schedule_id BIGINT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    type VARCHAR(20) DEFAULT 'meeting',
    customer_id BIGINT,
    contact_id BIGINT,
    location VARCHAR(255),
    create_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS crm_custom_field_pool (
    pool_id BIGINT PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    column_type VARCHAR(100) NOT NULL,
    field_type VARCHAR(50) NOT NULL,
    column_created BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_custom_field_pool_entity_column
    ON crm_custom_field_pool(entity_type, column_name);
CREATE INDEX IF NOT EXISTS idx_custom_field_pool_entity_type
    ON crm_custom_field_pool(entity_type, field_type);

ALTER TABLE IF EXISTS crm_customer ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS crm_customer ADD COLUMN IF NOT EXISTS primary_contact_name VARCHAR(100);
ALTER TABLE IF EXISTS crm_customer ADD COLUMN IF NOT EXISTS primary_contact_phone VARCHAR(50);
ALTER TABLE IF EXISTS crm_customer ADD COLUMN IF NOT EXISTS primary_contact_position VARCHAR(100);
ALTER TABLE IF EXISTS crm_customer ADD COLUMN IF NOT EXISTS contact_count INTEGER DEFAULT 0;
ALTER TABLE IF EXISTS crm_customer ADD COLUMN IF NOT EXISTS tag_names TEXT DEFAULT '';
ALTER TABLE IF EXISTS crm_customer ADD COLUMN IF NOT EXISTS search_text TEXT;
ALTER TABLE IF EXISTS crm_customer ADD COLUMN IF NOT EXISTS logo VARCHAR(500);

ALTER TABLE IF EXISTS crm_contact ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

ALTER TABLE IF EXISTS crm_follow_up ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS crm_follow_up ADD COLUMN IF NOT EXISTS summary VARCHAR(500);
ALTER TABLE IF EXISTS crm_follow_up ADD COLUMN IF NOT EXISTS scene_type VARCHAR(100);
ALTER TABLE IF EXISTS crm_follow_up ADD COLUMN IF NOT EXISTS ai_generated SMALLINT DEFAULT 0;

ALTER TABLE IF EXISTS crm_task ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS crm_task ADD COLUMN IF NOT EXISTS source_follow_up_id BIGINT;
ALTER TABLE IF EXISTS crm_task ADD COLUMN IF NOT EXISTS task_type VARCHAR(50);
ALTER TABLE IF EXISTS crm_task ADD COLUMN IF NOT EXISTS participant_names TEXT;

ALTER TABLE IF EXISTS crm_schedule ADD COLUMN IF NOT EXISTS participant_user_ids TEXT;

ALTER TABLE IF EXISTS manager_user ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS manager_dept ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS manager_role ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS manager_user_role ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS manager_role_menu ADD COLUMN IF NOT EXISTS data_scope INTEGER;

ALTER TABLE IF EXISTS crm_tenant ADD COLUMN IF NOT EXISTS gift_credit_total BIGINT DEFAULT 300;
ALTER TABLE IF EXISTS crm_tenant ADD COLUMN IF NOT EXISTS gift_credit_used BIGINT DEFAULT 0;
ALTER TABLE IF EXISTS crm_tenant ADD COLUMN IF NOT EXISTS purchased_credit_total BIGINT DEFAULT 0;
ALTER TABLE IF EXISTS crm_tenant ADD COLUMN IF NOT EXISTS purchased_credit_used BIGINT DEFAULT 0;

ALTER TABLE IF EXISTS crm_custom_field ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS crm_custom_field ADD COLUMN IF NOT EXISTS field_source VARCHAR(32);
