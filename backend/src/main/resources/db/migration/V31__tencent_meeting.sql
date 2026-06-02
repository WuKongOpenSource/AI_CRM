CREATE TABLE IF NOT EXISTS crm_tencent_meeting_corp_config (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    app_id VARCHAR(128) NOT NULL,
    sdk_id VARCHAR(128),
    corp_name VARCHAR(255),
    secret_id_encrypted TEXT,
    secret_key_encrypted TEXT,
    webhook_secret_encrypted TEXT,
    sts_token_encrypted TEXT,
    sts_token_expire_time TIMESTAMP(3),
    operator_user_id VARCHAR(255),
    sync_enabled BOOLEAN DEFAULT TRUE,
    transcript_enabled BOOLEAN DEFAULT TRUE,
    archive_to_knowledge BOOLEAN DEFAULT TRUE,
    last_sync_time TIMESTAMP(3),
    last_sync_status VARCHAR(32),
    last_sync_error TEXT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tencent_meeting_config_tenant_app
    ON crm_tencent_meeting_corp_config (tenant_id, app_id);

CREATE TRIGGER trg_tencent_meeting_config_update_time
    BEFORE UPDATE ON crm_tencent_meeting_corp_config
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_tencent_meeting_user_mapping (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    app_id VARCHAR(128) NOT NULL,
    meeting_user_id VARCHAR(255) NOT NULL,
    user_name VARCHAR(255),
    crm_user_id BIGINT,
    status INTEGER DEFAULT 1,
    synced_at TIMESTAMP(3),
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tencent_meeting_user_mapping
    ON crm_tencent_meeting_user_mapping (tenant_id, app_id, meeting_user_id);
CREATE INDEX IF NOT EXISTS idx_tencent_meeting_user_crm
    ON crm_tencent_meeting_user_mapping (tenant_id, crm_user_id);

CREATE TRIGGER trg_tencent_meeting_user_mapping_update_time
    BEFORE UPDATE ON crm_tencent_meeting_user_mapping
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_tencent_meeting (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    app_id VARCHAR(128) NOT NULL,
    meeting_id VARCHAR(255) NOT NULL,
    meeting_code VARCHAR(255),
    subject VARCHAR(500),
    status VARCHAR(32),
    creator_user_id VARCHAR(255),
    creator_name VARCHAR(255),
    crm_creator_user_id BIGINT,
    participant_names TEXT,
    participant_count INTEGER,
    start_time TIMESTAMP(3),
    end_time TIMESTAMP(3),
    duration_seconds BIGINT,
    bind_status VARCHAR(32) DEFAULT 'UNBOUND',
    customer_id BIGINT,
    customer_name VARCHAR(255),
    summary TEXT,
    todo_text TEXT,
    transcript_text TEXT,
    raw_json TEXT,
    knowledge_id BIGINT,
    synced_at TIMESTAMP(3),
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tencent_meeting_external
    ON crm_tencent_meeting (tenant_id, app_id, meeting_id);
CREATE INDEX IF NOT EXISTS idx_tencent_meeting_time
    ON crm_tencent_meeting (tenant_id, start_time DESC);
CREATE INDEX IF NOT EXISTS idx_tencent_meeting_customer
    ON crm_tencent_meeting (tenant_id, customer_id, start_time DESC);
CREATE INDEX IF NOT EXISTS idx_tencent_meeting_creator
    ON crm_tencent_meeting (tenant_id, crm_creator_user_id);

CREATE TRIGGER trg_tencent_meeting_update_time
    BEFORE UPDATE ON crm_tencent_meeting
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_tencent_meeting_participant (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    app_id VARCHAR(128),
    meeting_db_id BIGINT NOT NULL,
    meeting_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255),
    user_name VARCHAR(255),
    role VARCHAR(64),
    join_time TIMESTAMP(3),
    leave_time TIMESTAMP(3),
    duration_seconds BIGINT,
    raw_json TEXT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_tencent_meeting_participant_meeting
    ON crm_tencent_meeting_participant (tenant_id, meeting_db_id);

CREATE TRIGGER trg_tencent_meeting_participant_update_time
    BEFORE UPDATE ON crm_tencent_meeting_participant
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_tencent_meeting_recording (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    app_id VARCHAR(128),
    meeting_db_id BIGINT NOT NULL,
    meeting_id VARCHAR(255) NOT NULL,
    record_file_id VARCHAR(255) NOT NULL,
    file_name VARCHAR(500),
    download_url TEXT,
    play_url TEXT,
    file_size BIGINT,
    duration_seconds BIGINT,
    transcript_status VARCHAR(64),
    summary TEXT,
    todo_text TEXT,
    raw_json TEXT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tencent_meeting_recording
    ON crm_tencent_meeting_recording (tenant_id, record_file_id);
CREATE INDEX IF NOT EXISTS idx_tencent_meeting_recording_meeting
    ON crm_tencent_meeting_recording (tenant_id, meeting_db_id);

CREATE TRIGGER trg_tencent_meeting_recording_update_time
    BEFORE UPDATE ON crm_tencent_meeting_recording
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_tencent_meeting_transcript_segment (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    recording_id BIGINT,
    meeting_db_id BIGINT NOT NULL,
    meeting_id VARCHAR(255) NOT NULL,
    record_file_id VARCHAR(255),
    pid VARCHAR(255),
    speaker_user_id VARCHAR(255),
    speaker_name VARCHAR(255),
    start_time_ms BIGINT,
    end_time_ms BIGINT,
    text TEXT,
    raw_json TEXT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_tencent_meeting_transcript_meeting
    ON crm_tencent_meeting_transcript_segment (tenant_id, meeting_db_id, start_time_ms);

CREATE TRIGGER trg_tencent_meeting_transcript_update_time
    BEFORE UPDATE ON crm_tencent_meeting_transcript_segment
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_tencent_meeting_customer_binding (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    meeting_id BIGINT NOT NULL,
    meeting_external_id VARCHAR(255),
    customer_id BIGINT NOT NULL,
    bind_user_id BIGINT,
    bind_time TIMESTAMP(3),
    unbind_time TIMESTAMP(3),
    status INTEGER DEFAULT 1,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tencent_meeting_active_binding
    ON crm_tencent_meeting_customer_binding (tenant_id, meeting_id)
    WHERE status = 1;
CREATE INDEX IF NOT EXISTS idx_tencent_meeting_binding_customer
    ON crm_tencent_meeting_customer_binding (tenant_id, customer_id);

CREATE TRIGGER trg_tencent_meeting_binding_update_time
    BEFORE UPDATE ON crm_tencent_meeting_customer_binding
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_tencent_meeting_sync_cursor (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    app_id VARCHAR(128) NOT NULL,
    cursor_type VARCHAR(64) NOT NULL,
    cursor_value VARCHAR(500),
    synced_at TIMESTAMP(3),
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tencent_meeting_sync_cursor
    ON crm_tencent_meeting_sync_cursor (tenant_id, app_id, cursor_type);

CREATE TRIGGER trg_tencent_meeting_sync_cursor_update_time
    BEFORE UPDATE ON crm_tencent_meeting_sync_cursor
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_tencent_meeting_sync_log (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    app_id VARCHAR(128),
    sync_type VARCHAR(64),
    status VARCHAR(32),
    fetched_count INTEGER DEFAULT 0,
    saved_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    started_at TIMESTAMP(3),
    finished_at TIMESTAMP(3),
    error_message TEXT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_tencent_meeting_sync_log_time
    ON crm_tencent_meeting_sync_log (tenant_id, started_at DESC);

CREATE TRIGGER trg_tencent_meeting_sync_log_update_time
    BEFORE UPDATE ON crm_tencent_meeting_sync_log
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_tencent_meeting_webhook_event (
    id BIGINT NOT NULL,
    tenant_id BIGINT,
    app_id VARCHAR(128),
    event_name VARCHAR(128),
    trace_id VARCHAR(255),
    raw_json TEXT,
    process_status VARCHAR(32),
    process_error TEXT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tencent_meeting_webhook_trace
    ON crm_tencent_meeting_webhook_event (trace_id)
    WHERE trace_id IS NOT NULL;

CREATE TRIGGER trg_tencent_meeting_webhook_update_time
    BEFORE UPDATE ON crm_tencent_meeting_webhook_event
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type)
VALUES
    (2440, 0, 'tencentMeeting', '腾讯会议', 3),
    (2441, 2440, 'tencentMeeting:view', '查看列表', 5),
    (2442, 2440, 'tencentMeeting:detail', '查看详情', 5),
    (2443, 2440, 'tencentMeeting:bind', '关联客户', 5),
    (2444, 2440, 'tencentMeeting:unbind', '取消关联', 5),
    (2445, 2440, 'tencentMeeting:sync', '同步会议', 5),
    (2446, 2440, 'tencentMeeting:config', '配置腾讯会议', 5)
ON CONFLICT (menu_id) DO NOTHING;
