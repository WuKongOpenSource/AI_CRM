CREATE TABLE IF NOT EXISTS crm_mail_account (
    account_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL,
    auth_type VARCHAR(32) NOT NULL,
    email_address VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    imap_host VARCHAR(255),
    imap_port INTEGER,
    imap_ssl BOOLEAN DEFAULT TRUE,
    smtp_host VARCHAR(255),
    smtp_port INTEGER,
    smtp_ssl BOOLEAN DEFAULT TRUE,
    username VARCHAR(255),
    credential_json TEXT,
    folders VARCHAR(500),
    sync_days INTEGER DEFAULT 90,
    sync_limit INTEGER DEFAULT 500,
    body_sync_mode VARCHAR(32) DEFAULT 'summary',
    attachment_sync_mode VARCHAR(32) DEFAULT 'metadata',
    max_auto_attachment_size BIGINT DEFAULT 10485760,
    retention_days INTEGER DEFAULT 180,
    extract_actions BOOLEAN DEFAULT TRUE,
    enabled BOOLEAN DEFAULT TRUE,
    is_default BOOLEAN DEFAULT FALSE,
    connection_status VARCHAR(32) DEFAULT 'connected',
    last_used_time TIMESTAMP(3),
    last_sync_time TIMESTAMP(3),
    last_sync_status VARCHAR(32),
    last_sync_error TEXT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_mail_account_user_email
    ON crm_mail_account (tenant_id, user_id, lower(email_address));
CREATE INDEX IF NOT EXISTS idx_mail_account_user ON crm_mail_account (tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_mail_account_sync ON crm_mail_account (enabled, last_sync_time);

CREATE TRIGGER trg_mail_account_update_time
    BEFORE UPDATE ON crm_mail_account
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_mail_message (
    message_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL,
    provider_message_id VARCHAR(500) NOT NULL,
    internet_message_id VARCHAR(500),
    thread_id VARCHAR(500),
    folder VARCHAR(128),
    direction VARCHAR(16),
    subject VARCHAR(500),
    from_name VARCHAR(255),
    from_address VARCHAR(255),
    to_addresses TEXT,
    cc_addresses TEXT,
    bcc_addresses TEXT,
    sent_time TIMESTAMP(3),
    received_time TIMESTAMP(3),
    body_sync_mode VARCHAR(32) DEFAULT 'summary',
    body_sync_status VARCHAR(32) DEFAULT 'summary',
    summary TEXT,
    keywords VARCHAR(1000),
    intent VARCHAR(255),
    action_items_json TEXT,
    reply_deadline_time TIMESTAMP(3),
    extraction_status VARCHAR(32),
    extraction_error TEXT,
    body_text TEXT,
    body_html TEXT,
    raw_file_path VARCHAR(500),
    raw_file_size BIGINT DEFAULT 0,
    has_attachments BOOLEAN DEFAULT FALSE,
    read_status VARCHAR(16) DEFAULT 'unread',
    starred BOOLEAN DEFAULT FALSE,
    deleted BOOLEAN DEFAULT FALSE,
    customer_id BIGINT,
    contact_id BIGINT,
    knowledge_id BIGINT,
    sync_status VARCHAR(32),
    sync_error TEXT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_mail_message_provider_id
    ON crm_mail_message (account_id, provider_message_id);
CREATE INDEX IF NOT EXISTS idx_mail_message_customer ON crm_mail_message (tenant_id, customer_id, received_time DESC);
CREATE INDEX IF NOT EXISTS idx_mail_message_user_time ON crm_mail_message (tenant_id, user_id, received_time DESC);
CREATE INDEX IF NOT EXISTS idx_mail_message_account_dir ON crm_mail_message (tenant_id, account_id, direction, received_time DESC);
CREATE INDEX IF NOT EXISTS idx_mail_message_subject ON crm_mail_message (tenant_id, subject);
CREATE INDEX IF NOT EXISTS idx_mail_message_from ON crm_mail_message (tenant_id, lower(from_address));
CREATE INDEX IF NOT EXISTS idx_mail_message_thread ON crm_mail_message (tenant_id, account_id, thread_id);

CREATE TRIGGER trg_mail_message_update_time
    BEFORE UPDATE ON crm_mail_message
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_mail_attachment (
    attachment_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    provider_attachment_id VARCHAR(500),
    file_name VARCHAR(500),
    content_type VARCHAR(255),
    file_size BIGINT DEFAULT 0,
    file_path VARCHAR(500),
    content_text TEXT,
    knowledge_id BIGINT,
    download_status VARCHAR(32) DEFAULT 'metadata',
    scan_status VARCHAR(32) DEFAULT 'pending',
    sync_mode VARCHAR(32) DEFAULT 'metadata',
    download_error TEXT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (attachment_id)
);

CREATE INDEX IF NOT EXISTS idx_mail_attachment_message ON crm_mail_attachment (message_id);

CREATE TRIGGER trg_mail_attachment_update_time
    BEFORE UPDATE ON crm_mail_attachment
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_mail_sync_log (
    log_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    sync_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    fetched_count INTEGER DEFAULT 0,
    saved_count INTEGER DEFAULT 0,
    skipped_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    started_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP(3),
    error_message TEXT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id)
);

CREATE INDEX IF NOT EXISTS idx_mail_sync_log_account_time
    ON crm_mail_sync_log (tenant_id, account_id, started_at DESC);

CREATE TABLE IF NOT EXISTS crm_mail_draft (
    draft_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    account_id BIGINT,
    customer_id BIGINT,
    contact_id BIGINT,
    source_message_id BIGINT,
    to_addresses TEXT NOT NULL,
    cc_addresses TEXT,
    bcc_addresses TEXT,
    subject VARCHAR(500) NOT NULL,
    body_text TEXT NOT NULL,
    attachment_refs TEXT,
    status VARCHAR(32) DEFAULT 'draft',
    risk_status VARCHAR(32) DEFAULT 'pending_review',
    risk_reasons TEXT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (draft_id)
);

CREATE INDEX IF NOT EXISTS idx_mail_draft_customer ON crm_mail_draft (tenant_id, customer_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_mail_draft_user ON crm_mail_draft (tenant_id, user_id, create_time DESC);

CREATE TRIGGER trg_mail_draft_update_time
    BEFORE UPDATE ON crm_mail_draft
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_mail_template (
    template_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(64),
    subject VARCHAR(500) NOT NULL,
    body_text TEXT NOT NULL,
    variables VARCHAR(1000),
    is_common BOOLEAN DEFAULT FALSE,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (template_id)
);

CREATE INDEX IF NOT EXISTS idx_mail_template_user ON crm_mail_template (tenant_id, user_id, update_time DESC);
CREATE INDEX IF NOT EXISTS idx_mail_template_category ON crm_mail_template (tenant_id, user_id, category);

CREATE TRIGGER trg_mail_template_update_time
    BEFORE UPDATE ON crm_mail_template
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_mail_sync_cursor (
    cursor_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    folder VARCHAR(128) NOT NULL,
    cursor_type VARCHAR(32) NOT NULL,
    cursor_value TEXT,
    last_uid BIGINT,
    last_history_id VARCHAR(128),
    delta_link TEXT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cursor_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_mail_sync_cursor_account_folder
    ON crm_mail_sync_cursor (account_id, folder);

CREATE TRIGGER trg_mail_sync_cursor_update_time
    BEFORE UPDATE ON crm_mail_sync_cursor
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type)
VALUES
    (2300, 0, 'mail', '邮件读取', 3),
    (2301, 2300, 'mail:view', '查看', 5),
    (2302, 2300, 'mail:manage', '管理邮箱', 5),
    (2303, 2300, 'mail:sync', '同步', 5),
    (2304, 2300, 'mail:delete', '删除', 5)
ON CONFLICT (menu_id) DO NOTHING;
