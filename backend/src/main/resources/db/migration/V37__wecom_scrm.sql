CREATE TABLE IF NOT EXISTS crm_wecom_corp_config (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    corp_id VARCHAR(128) NOT NULL,
    corp_name VARCHAR(255),
    agent_id VARCHAR(64),
    app_secret_encrypted TEXT,
    contact_secret_encrypted TEXT,
    archive_secret_encrypted TEXT,
    archive_private_key_encrypted TEXT,
    archive_public_key_version VARCHAR(128),
    archive_enabled BOOLEAN DEFAULT FALSE,
    customer_contact_enabled BOOLEAN DEFAULT TRUE,
    sync_enabled BOOLEAN DEFAULT TRUE,
    last_sync_time TIMESTAMP(3),
    last_sync_status VARCHAR(32),
    last_sync_error TEXT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wecom_config_tenant_corp
    ON crm_wecom_corp_config (tenant_id, corp_id);

DROP TRIGGER IF EXISTS trg_wecom_corp_config_update_time ON crm_wecom_corp_config;
CREATE TRIGGER trg_wecom_corp_config_update_time
    BEFORE UPDATE ON crm_wecom_corp_config
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_wecom_employee (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    corp_id VARCHAR(128) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    crm_user_id BIGINT,
    name VARCHAR(255),
    alias VARCHAR(255),
    department_list TEXT,
    mobile VARCHAR(64),
    email VARCHAR(255),
    avatar VARCHAR(500),
    qr_code VARCHAR(500),
    position VARCHAR(255),
    status INTEGER DEFAULT 1,
    synced_at TIMESTAMP(3),
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wecom_employee_user
    ON crm_wecom_employee (tenant_id, corp_id, user_id);
CREATE INDEX IF NOT EXISTS idx_wecom_employee_crm_user
    ON crm_wecom_employee (tenant_id, crm_user_id);

DROP TRIGGER IF EXISTS trg_wecom_employee_update_time ON crm_wecom_employee;
CREATE TRIGGER trg_wecom_employee_update_time
    BEFORE UPDATE ON crm_wecom_employee
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_wecom_external_customer (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    corp_id VARCHAR(128) NOT NULL,
    external_user_id VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    avatar VARCHAR(500),
    type INTEGER,
    gender INTEGER,
    union_id VARCHAR(255),
    position VARCHAR(255),
    corp_name VARCHAR(255),
    corp_full_name VARCHAR(500),
    external_profile TEXT,
    bind_status VARCHAR(32) DEFAULT 'UNBOUND',
    customer_id BIGINT,
    synced_at TIMESTAMP(3),
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wecom_external_customer_user
    ON crm_wecom_external_customer (tenant_id, corp_id, external_user_id);
CREATE INDEX IF NOT EXISTS idx_wecom_external_customer_bind
    ON crm_wecom_external_customer (tenant_id, bind_status, customer_id);

DROP TRIGGER IF EXISTS trg_wecom_external_customer_update_time ON crm_wecom_external_customer;
CREATE TRIGGER trg_wecom_external_customer_update_time
    BEFORE UPDATE ON crm_wecom_external_customer
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_wecom_external_customer_follow (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    corp_id VARCHAR(128) NOT NULL,
    external_customer_id BIGINT NOT NULL,
    external_user_id VARCHAR(255) NOT NULL,
    employee_id BIGINT,
    employee_user_id VARCHAR(255) NOT NULL,
    remark VARCHAR(500),
    description TEXT,
    add_way INTEGER,
    state VARCHAR(255),
    tags_json TEXT,
    relation_create_time TIMESTAMP(3),
    status INTEGER DEFAULT 1,
    synced_at TIMESTAMP(3),
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wecom_external_follow
    ON crm_wecom_external_customer_follow (tenant_id, external_customer_id, employee_user_id);
CREATE INDEX IF NOT EXISTS idx_wecom_external_follow_employee
    ON crm_wecom_external_customer_follow (tenant_id, employee_user_id);

DROP TRIGGER IF EXISTS trg_wecom_external_customer_follow_update_time ON crm_wecom_external_customer_follow;
CREATE TRIGGER trg_wecom_external_customer_follow_update_time
    BEFORE UPDATE ON crm_wecom_external_customer_follow
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_wecom_group_chat (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    corp_id VARCHAR(128) NOT NULL,
    chat_id VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    owner_user_id VARCHAR(255),
    member_list TEXT,
    customer_list TEXT,
    status INTEGER DEFAULT 1,
    notice TEXT,
    external_create_time TIMESTAMP(3),
    synced_at TIMESTAMP(3),
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wecom_group_chat
    ON crm_wecom_group_chat (tenant_id, corp_id, chat_id);
CREATE INDEX IF NOT EXISTS idx_wecom_group_chat_owner
    ON crm_wecom_group_chat (tenant_id, owner_user_id);

DROP TRIGGER IF EXISTS trg_wecom_group_chat_update_time ON crm_wecom_group_chat;
CREATE TRIGGER trg_wecom_group_chat_update_time
    BEFORE UPDATE ON crm_wecom_group_chat
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_wecom_conversation (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    corp_id VARCHAR(128) NOT NULL,
    conversation_type VARCHAR(32) NOT NULL,
    employee_id BIGINT,
    employee_user_id VARCHAR(255),
    external_customer_id BIGINT,
    external_user_id VARCHAR(255),
    group_chat_id BIGINT,
    chat_id VARCHAR(255),
    title VARCHAR(500),
    peer_name VARCHAR(255),
    peer_avatar VARCHAR(500),
    customer_id BIGINT,
    owner_user_id BIGINT,
    last_msg_id VARCHAR(255),
    last_msg_time TIMESTAMP(3),
    last_msg_preview TEXT,
    message_count INTEGER DEFAULT 0,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wecom_conversation_chat
    ON crm_wecom_conversation (tenant_id, corp_id, chat_id);
CREATE INDEX IF NOT EXISTS idx_wecom_conversation_type_time
    ON crm_wecom_conversation (tenant_id, conversation_type, last_msg_time DESC);
CREATE INDEX IF NOT EXISTS idx_wecom_conversation_customer
    ON crm_wecom_conversation (tenant_id, customer_id, last_msg_time DESC);
CREATE INDEX IF NOT EXISTS idx_wecom_conversation_owner
    ON crm_wecom_conversation (tenant_id, owner_user_id);

DROP TRIGGER IF EXISTS trg_wecom_conversation_update_time ON crm_wecom_conversation;
CREATE TRIGGER trg_wecom_conversation_update_time
    BEFORE UPDATE ON crm_wecom_conversation
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_wecom_message (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    corp_id VARCHAR(128),
    msg_id VARCHAR(255) NOT NULL,
    seq BIGINT,
    action VARCHAR(32),
    msg_type VARCHAR(64),
    sender_id VARCHAR(255),
    sender_type VARCHAR(32),
    receiver_list TEXT,
    msg_time TIMESTAMP(3),
    content_text TEXT,
    content_json TEXT,
    media_id BIGINT,
    sdk_file_id VARCHAR(500),
    file_name VARCHAR(500),
    file_size BIGINT,
    file_url VARCHAR(500),
    recalled BOOLEAN DEFAULT FALSE,
    raw_json TEXT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wecom_message_msg
    ON crm_wecom_message (tenant_id, msg_id);
CREATE INDEX IF NOT EXISTS idx_wecom_message_conversation_time
    ON crm_wecom_message (tenant_id, conversation_id, msg_time);
CREATE INDEX IF NOT EXISTS idx_wecom_message_seq
    ON crm_wecom_message (tenant_id, seq);

DROP TRIGGER IF EXISTS trg_wecom_message_update_time ON crm_wecom_message;
CREATE TRIGGER trg_wecom_message_update_time
    BEFORE UPDATE ON crm_wecom_message
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_wecom_media (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    corp_id VARCHAR(128),
    msg_id VARCHAR(255),
    sdk_file_id VARCHAR(500),
    media_type VARCHAR(64),
    file_name VARCHAR(500),
    content_type VARCHAR(255),
    file_size BIGINT,
    file_path VARCHAR(500),
    download_status VARCHAR(32) DEFAULT 'pending',
    download_error TEXT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_wecom_media_msg
    ON crm_wecom_media (tenant_id, msg_id);
CREATE INDEX IF NOT EXISTS idx_wecom_media_sdk_file
    ON crm_wecom_media (tenant_id, sdk_file_id);

DROP TRIGGER IF EXISTS trg_wecom_media_update_time ON crm_wecom_media;
CREATE TRIGGER trg_wecom_media_update_time
    BEFORE UPDATE ON crm_wecom_media
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_wecom_customer_binding (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    external_customer_id BIGINT NOT NULL,
    external_user_id VARCHAR(255),
    corp_id VARCHAR(128),
    bind_user_id BIGINT,
    bind_time TIMESTAMP(3),
    unbind_time TIMESTAMP(3),
    status INTEGER DEFAULT 1,
    remark VARCHAR(500),
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wecom_customer_binding_active
    ON crm_wecom_customer_binding (tenant_id, external_customer_id)
    WHERE status = 1;
CREATE INDEX IF NOT EXISTS idx_wecom_customer_binding_customer
    ON crm_wecom_customer_binding (tenant_id, customer_id, status);

DROP TRIGGER IF EXISTS trg_wecom_customer_binding_update_time ON crm_wecom_customer_binding;
CREATE TRIGGER trg_wecom_customer_binding_update_time
    BEFORE UPDATE ON crm_wecom_customer_binding
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_wecom_sync_cursor (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    corp_id VARCHAR(128) NOT NULL,
    cursor_type VARCHAR(64) NOT NULL,
    cursor_key VARCHAR(255) NOT NULL,
    cursor_value TEXT,
    seq BIGINT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wecom_sync_cursor
    ON crm_wecom_sync_cursor (tenant_id, corp_id, cursor_type, cursor_key);

DROP TRIGGER IF EXISTS trg_wecom_sync_cursor_update_time ON crm_wecom_sync_cursor;
CREATE TRIGGER trg_wecom_sync_cursor_update_time
    BEFORE UPDATE ON crm_wecom_sync_cursor
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_wecom_sync_log (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    corp_id VARCHAR(128),
    sync_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    fetched_count INTEGER DEFAULT 0,
    saved_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    started_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP(3),
    error_message TEXT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_wecom_sync_log_time
    ON crm_wecom_sync_log (tenant_id, started_at DESC);

INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type)
VALUES
    (2400, 0, 'wecomEmployeeSession', '企微员工会话', 3),
    (2401, 2400, 'wecomEmployeeSession:view', '查看列表', 5),
    (2402, 2400, 'wecomEmployeeSession:detail', '查看详情', 5),
    (2410, 0, 'wecomCustomerSession', '企微客户会话', 3),
    (2411, 2410, 'wecomCustomerSession:view', '查看列表', 5),
    (2412, 2410, 'wecomCustomerSession:detail', '查看详情', 5),
    (2420, 0, 'wecomGroupSession', '企微信群会话', 3),
    (2421, 2420, 'wecomGroupSession:view', '查看列表', 5),
    (2422, 2420, 'wecomGroupSession:detail', '查看详情', 5),
    (2430, 0, 'wecomCustomer', '企微客户', 3),
    (2431, 2430, 'wecomCustomer:view', '查看列表', 5),
    (2432, 2430, 'wecomCustomer:detail', '查看详情', 5),
    (2433, 2430, 'wecomCustomer:bind', '绑定客户', 5),
    (2434, 2430, 'wecomCustomer:unbind', '解绑客户', 5)
ON CONFLICT (menu_id) DO NOTHING;
