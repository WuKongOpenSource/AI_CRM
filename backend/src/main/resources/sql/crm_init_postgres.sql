-- ============================================
-- AI智能CRM系统 - PostgreSQL 数据库初始化脚本
-- 适用于 ParadeDB / PostgreSQL 17
-- ============================================

-- ============================================
-- 创建更新时间触发器函数
-- ============================================
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- 1. 客户表 (crm_customer)
-- ============================================
DROP TABLE IF EXISTS crm_customer CASCADE;
CREATE TABLE crm_customer (
    customer_id BIGINT,
    relation_id BIGINT,
    company_name VARCHAR(255) NOT NULL,
    industry VARCHAR(100),
    stage VARCHAR(50) NOT NULL DEFAULT 'lead',
    owner_id BIGINT NOT NULL,
    level VARCHAR(10),
    source VARCHAR(100),
    address VARCHAR(500),
    website VARCHAR(255),
    logo VARCHAR(500),
    quotation DECIMAL(15,2) DEFAULT 0,
    contract_amount DECIMAL(15,2) DEFAULT 0,
    revenue DECIMAL(15,2) DEFAULT 0,
    last_contact_time TIMESTAMP,
    next_follow_time TIMESTAMP,
    remark TEXT,
    status SMALLINT DEFAULT 1,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (customer_id)
);

CREATE INDEX idx_customer_owner_id ON crm_customer (owner_id);
CREATE INDEX idx_customer_stage ON crm_customer (stage);
CREATE INDEX idx_customer_level ON crm_customer (level);
CREATE INDEX idx_customer_create_time ON crm_customer (create_time);
CREATE INDEX idx_customer_update_time ON crm_customer (update_time DESC, create_time DESC);

CREATE TRIGGER trg_customer_update_time
    BEFORE UPDATE ON crm_customer
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_customer IS '客户表';
COMMENT ON COLUMN crm_customer.customer_id IS '客户ID';
COMMENT ON COLUMN crm_customer.company_name IS '公司名称';
COMMENT ON COLUMN crm_customer.industry IS '行业';
COMMENT ON COLUMN crm_customer.stage IS '阶段: lead, qualified, proposal, negotiation, closed, lost';
COMMENT ON COLUMN crm_customer.owner_id IS '负责人ID';
COMMENT ON COLUMN crm_customer.level IS 'Customer level';
COMMENT ON COLUMN crm_customer.source IS '客户来源';
COMMENT ON COLUMN crm_customer.status IS '状态: 0-禁用, 1-正常';

-- ============================================
-- 2. 客户团队成员表 (crm_customer_team)
-- ============================================
DROP TABLE IF EXISTS crm_customer_team CASCADE;
CREATE TABLE crm_customer_team (
    id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(50) DEFAULT 'member',
    create_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (customer_id, user_id)
);

CREATE INDEX idx_customer_team_user_id ON crm_customer_team (user_id);

COMMENT ON TABLE crm_customer_team IS '客户团队成员表';

-- ============================================
-- 3. 客户标签表 (crm_customer_tag)
-- ============================================
DROP TABLE IF EXISTS crm_customer_tag CASCADE;
CREATE TABLE crm_customer_tag (
    id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    tag_name VARCHAR(100) NOT NULL,
    color VARCHAR(20) DEFAULT '#3b82f6',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_customer_tag_customer_id ON crm_customer_tag (customer_id);
CREATE INDEX idx_customer_tag_tag_name ON crm_customer_tag (tag_name);

COMMENT ON TABLE crm_customer_tag IS '客户标签表';

-- ============================================
-- 4. 联系人表 (crm_contact)
-- ============================================
DROP TABLE IF EXISTS crm_contact CASCADE;
CREATE TABLE crm_contact (
    contact_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    position VARCHAR(100),
    phone VARCHAR(50),
    email VARCHAR(100),
    wechat VARCHAR(100),
    is_primary SMALLINT DEFAULT 0,
    last_contact_time TIMESTAMP,
    notes TEXT,
    status SMALLINT DEFAULT 1,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (contact_id)
);

CREATE INDEX idx_contact_customer_id ON crm_contact (customer_id);
CREATE INDEX idx_contact_name ON crm_contact (name);

CREATE TRIGGER trg_contact_update_time
    BEFORE UPDATE ON crm_contact
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_contact IS '联系人表';
COMMENT ON COLUMN crm_contact.is_primary IS '是否主联系人: 0-否, 1-是';
COMMENT ON COLUMN crm_contact.status IS '状态: 0-禁用, 1-正常';

-- ============================================
-- 5. 联系人标签表 (crm_contact_tag)
-- ============================================
DROP TABLE IF EXISTS crm_contact_tag CASCADE;
CREATE TABLE crm_contact_tag (
    id BIGINT NOT NULL,
    contact_id BIGINT NOT NULL,
    tag_name VARCHAR(100) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_contact_tag_contact_id ON crm_contact_tag (contact_id);

-- ============================================
-- 5.1. External relation table (crm_relation)
-- ============================================
DROP TABLE IF EXISTS crm_relation CASCADE;
CREATE TABLE crm_relation (
    relation_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    avatar VARCHAR(500),
    phone VARCHAR(50),
    wechat VARCHAR(100),
    email VARCHAR(100),
    relation_type VARCHAR(50) DEFAULT 'other',
    company VARCHAR(255),
    customer_id BIGINT,
    remark TEXT,
    source VARCHAR(50) NOT NULL DEFAULT 'manual',
    source_customer_id BIGINT,
    source_contact_id BIGINT,
    status SMALLINT DEFAULT 1,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (relation_id)
);

CREATE INDEX idx_relation_owner ON crm_relation (create_user_id, status);
CREATE INDEX idx_relation_customer_id ON crm_relation (customer_id);
CREATE INDEX idx_relation_source_contact ON crm_relation (source_contact_id, create_user_id);
CREATE INDEX idx_relation_name ON crm_relation (name);

CREATE TRIGGER trg_relation_update_time
    BEFORE UPDATE ON crm_relation
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_relation IS 'External relation records';
COMMENT ON COLUMN crm_relation.customer_id IS 'Linked customer ID';

-- ============================================
-- 5.2. Product catalog tables (crm_product, crm_product_category)
-- ============================================
DROP TABLE IF EXISTS crm_product CASCADE;
DROP TABLE IF EXISTS crm_product_category CASCADE;
CREATE TABLE crm_product_category (
    category_id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL DEFAULT 0,
    category_name VARCHAR(100) NOT NULL,
    category_path VARCHAR(500) NOT NULL,
    level SMALLINT NOT NULL DEFAULT 1,
    sort_order INTEGER NOT NULL DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1,
    del_flag SMALLINT NOT NULL DEFAULT 0,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (category_id)
);

CREATE INDEX idx_product_category_parent
    ON crm_product_category (parent_id, sort_order, category_id)
    WHERE del_flag = 0;

CREATE UNIQUE INDEX uk_product_category_parent_name
    ON crm_product_category (parent_id, category_name)
    WHERE del_flag = 0;

CREATE TRIGGER trg_product_category_update_time
    BEFORE UPDATE ON crm_product_category
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE crm_product (
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_code VARCHAR(100),
    main_image VARCHAR(500),
    category_id BIGINT,
    product_type VARCHAR(50) NOT NULL DEFAULT 'goods',
    unit VARCHAR(50),
    standard_price NUMERIC(18, 2),
    cost_price NUMERIC(18, 2),
    owner_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    description TEXT,
    del_flag SMALLINT NOT NULL DEFAULT 0,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id)
);

CREATE UNIQUE INDEX uk_product_code
    ON crm_product (product_code)
    WHERE del_flag = 0 AND product_code IS NOT NULL AND product_code <> '';

CREATE INDEX idx_product_owner
    ON crm_product (owner_id, status, update_time DESC)
    WHERE del_flag = 0;

CREATE INDEX idx_product_category
    ON crm_product (category_id)
    WHERE del_flag = 0;

CREATE INDEX idx_product_status
    ON crm_product (status)
    WHERE del_flag = 0;

CREATE TRIGGER trg_product_update_time
    BEFORE UPDATE ON crm_product
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

INSERT INTO crm_product_category (
    category_id,
    parent_id,
    category_name,
    category_path,
    level,
    sort_order,
    status,
    del_flag,
    create_user_id,
    update_user_id,
    create_time,
    update_time
) VALUES (
    1,
    0,
    '未分类',
    '未分类',
    1,
    0,
    1,
    0,
    1,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

COMMENT ON TABLE crm_product IS 'Product catalog';
COMMENT ON TABLE crm_product_category IS 'Product categories';
COMMENT ON COLUMN crm_product.status IS 'active=enabled, inactive=disabled';

-- ============================================
-- 5.3. Project board tables (crm_project, crm_project_lane, crm_project_task)
-- ============================================
DROP TABLE IF EXISTS crm_project_task CASCADE;
DROP TABLE IF EXISTS crm_project_task_attachment CASCADE;
DROP TABLE IF EXISTS crm_project_schedule CASCADE;
DROP TABLE IF EXISTS crm_project_attachment CASCADE;
DROP TABLE IF EXISTS crm_project_lane CASCADE;
DROP TABLE IF EXISTS crm_project CASCADE;
CREATE TABLE crm_project (
    project_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED',
    owner_id BIGINT,
    customer_id BIGINT,
    customer_name VARCHAR(255),
    start_date TIMESTAMP,
    due_date TIMESTAMP,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id)
);

CREATE INDEX idx_crm_project_update_time ON crm_project(update_time DESC);
CREATE INDEX idx_crm_project_owner_id ON crm_project(owner_id);
CREATE INDEX idx_crm_project_customer_id ON crm_project(customer_id);
CREATE INDEX idx_crm_project_status ON crm_project(status);

CREATE TRIGGER trg_project_update_time
    BEFORE UPDATE ON crm_project
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE crm_project_attachment (
    attachment_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    file_url TEXT,
    create_user_id BIGINT,
    create_user_name VARCHAR(100),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (attachment_id)
);

CREATE INDEX idx_crm_project_attachment_project_time
    ON crm_project_attachment(project_id, create_time DESC);

CREATE TABLE crm_project_schedule (
    schedule_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    schedule_time TIMESTAMP,
    create_user_id BIGINT,
    create_user_name VARCHAR(100),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (schedule_id)
);

CREATE INDEX idx_crm_project_schedule_project_time
    ON crm_project_schedule(project_id, COALESCE(schedule_time, create_time) DESC);

CREATE TABLE crm_project_lane (
    lane_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(64),
    sort_order INTEGER NOT NULL DEFAULT 0,
    system_flag BOOLEAN NOT NULL DEFAULT FALSE,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (lane_id)
);

CREATE INDEX idx_crm_project_lane_project_order
    ON crm_project_lane(project_id, sort_order);

CREATE TRIGGER trg_project_lane_update_time
    BEFORE UPDATE ON crm_project_lane
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE crm_project_task (
    task_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    lane_id BIGINT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'TODO',
    due_date TIMESTAMP,
    owner_id BIGINT,
    owner_name VARCHAR(100),
    priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    customer_id BIGINT,
    customer_name VARCHAR(255),
    generated_by_ai BOOLEAN NOT NULL DEFAULT FALSE,
    ai_source_text TEXT,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id)
);

CREATE INDEX idx_crm_project_task_project_lane
    ON crm_project_task(project_id, lane_id);
CREATE INDEX idx_crm_project_task_owner
    ON crm_project_task(owner_id);
CREATE INDEX idx_crm_project_task_due_date
    ON crm_project_task(due_date);

CREATE TRIGGER trg_project_task_update_time
    BEFORE UPDATE ON crm_project_task
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE crm_project_task_attachment (
    attachment_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    file_url TEXT,
    file_path TEXT,
    file_size BIGINT,
    mime_type VARCHAR(255),
    create_user_id BIGINT,
    create_user_name VARCHAR(100),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (attachment_id)
);

CREATE INDEX idx_crm_project_task_attachment_project
    ON crm_project_task_attachment(project_id, create_time DESC);
CREATE INDEX idx_crm_project_task_attachment_task
    ON crm_project_task_attachment(task_id, create_time DESC);

COMMENT ON TABLE crm_project IS 'Project board';
COMMENT ON TABLE crm_project_attachment IS 'Project attachments';
COMMENT ON TABLE crm_project_schedule IS 'Project schedules';
COMMENT ON TABLE crm_project_lane IS 'Project board lanes';
COMMENT ON TABLE crm_project_task IS 'Project board tasks';
COMMENT ON TABLE crm_project_task_attachment IS 'Project task attachments';
COMMENT ON COLUMN crm_project_task_attachment.file_path IS 'Stored file path';
COMMENT ON COLUMN crm_project_task_attachment.file_size IS 'File size in bytes';
COMMENT ON COLUMN crm_project_task_attachment.mime_type IS 'File MIME type';

-- ============================================
-- 5.4. Mail tables
-- ============================================
DROP TABLE IF EXISTS crm_mail_sync_cursor CASCADE;
DROP TABLE IF EXISTS crm_mail_template CASCADE;
DROP TABLE IF EXISTS crm_mail_draft CASCADE;
DROP TABLE IF EXISTS crm_mail_sync_log CASCADE;
DROP TABLE IF EXISTS crm_mail_attachment CASCADE;
DROP TABLE IF EXISTS crm_mail_message CASCADE;
DROP TABLE IF EXISTS crm_mail_account CASCADE;

CREATE TABLE crm_mail_account (
    account_id BIGINT NOT NULL,
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

CREATE UNIQUE INDEX uk_mail_account_user_email ON crm_mail_account (user_id, lower(email_address));
CREATE INDEX idx_mail_account_user ON crm_mail_account (user_id);
CREATE INDEX idx_mail_account_sync ON crm_mail_account (enabled, last_sync_time);

CREATE TRIGGER trg_mail_account_update_time
    BEFORE UPDATE ON crm_mail_account
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE crm_mail_message (
    message_id BIGINT NOT NULL,
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

CREATE UNIQUE INDEX uk_mail_message_provider_id ON crm_mail_message (account_id, provider_message_id);
CREATE INDEX idx_mail_message_customer ON crm_mail_message (customer_id, received_time DESC);
CREATE INDEX idx_mail_message_user_time ON crm_mail_message (user_id, received_time DESC);
CREATE INDEX idx_mail_message_account_dir ON crm_mail_message (account_id, direction, received_time DESC);
CREATE INDEX idx_mail_message_subject ON crm_mail_message (subject);
CREATE INDEX idx_mail_message_from ON crm_mail_message (lower(from_address));
CREATE INDEX idx_mail_message_thread ON crm_mail_message (account_id, thread_id);

CREATE TRIGGER trg_mail_message_update_time
    BEFORE UPDATE ON crm_mail_message
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE crm_mail_attachment (
    attachment_id BIGINT NOT NULL,
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

CREATE INDEX idx_mail_attachment_message ON crm_mail_attachment (message_id);

CREATE TRIGGER trg_mail_attachment_update_time
    BEFORE UPDATE ON crm_mail_attachment
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE crm_mail_sync_log (
    log_id BIGINT NOT NULL,
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

CREATE INDEX idx_mail_sync_log_account_time ON crm_mail_sync_log (account_id, started_at DESC);

CREATE TABLE crm_mail_draft (
    draft_id BIGINT NOT NULL,
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

CREATE INDEX idx_mail_draft_customer ON crm_mail_draft (customer_id, create_time DESC);
CREATE INDEX idx_mail_draft_user ON crm_mail_draft (user_id, create_time DESC);

CREATE TRIGGER trg_mail_draft_update_time
    BEFORE UPDATE ON crm_mail_draft
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE crm_mail_template (
    template_id BIGINT NOT NULL,
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

CREATE INDEX idx_mail_template_user ON crm_mail_template (user_id, update_time DESC);
CREATE INDEX idx_mail_template_category ON crm_mail_template (user_id, category);

CREATE TRIGGER trg_mail_template_update_time
    BEFORE UPDATE ON crm_mail_template
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE crm_mail_sync_cursor (
    cursor_id BIGINT NOT NULL,
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

CREATE UNIQUE INDEX uk_mail_sync_cursor_account_folder ON crm_mail_sync_cursor (account_id, folder);

CREATE TRIGGER trg_mail_sync_cursor_update_time
    BEFORE UPDATE ON crm_mail_sync_cursor
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_contact_tag IS '联系人标签表';

-- ============================================
-- 6. 跟进记录表 (crm_follow_up)
-- ============================================
DROP TABLE IF EXISTS crm_follow_up CASCADE;
CREATE TABLE crm_follow_up (
    follow_up_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    contact_id BIGINT,
    type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    summary VARCHAR(500),
    scene_type VARCHAR(100),
    ai_generated SMALLINT DEFAULT 0,
    follow_time TIMESTAMP NOT NULL,
    next_follow_time TIMESTAMP,
    create_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follow_up_id)
);

CREATE INDEX idx_follow_up_customer_id ON crm_follow_up (customer_id);
CREATE INDEX idx_follow_up_relation_id ON crm_follow_up (relation_id);
CREATE INDEX idx_follow_up_follow_time ON crm_follow_up (follow_time);

COMMENT ON TABLE crm_follow_up IS '跟进记录表';
COMMENT ON COLUMN crm_follow_up.type IS '类型: call, meeting, email, visit';

-- ============================================
-- 7. 知识库项目表 (crm_knowledge)
-- ============================================
DROP TABLE IF EXISTS crm_follow_up_attachment CASCADE;
CREATE TABLE crm_follow_up_attachment (
    attachment_id BIGINT NOT NULL,
    follow_up_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(255),
    sort INT DEFAULT 0,
    analysis_status VARCHAR(30) DEFAULT 'idle',
    analysis_content TEXT,
    analysis_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (attachment_id)
);

CREATE INDEX idx_follow_up_attachment_follow_up_id ON crm_follow_up_attachment (follow_up_id, sort, attachment_id);

CREATE TRIGGER trg_follow_up_attachment_update_time
    BEFORE UPDATE ON crm_follow_up_attachment
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

DROP TABLE IF EXISTS crm_knowledge CASCADE;
CREATE TABLE crm_knowledge (
    knowledge_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    file_path VARCHAR(500),
    file_size BIGINT DEFAULT 0,
    mime_type VARCHAR(100),
    customer_id BIGINT,
    summary TEXT,
    content_text TEXT,
    status SMALLINT DEFAULT 1,
    upload_user_id BIGINT NOT NULL,
    weknora_knowledge_id VARCHAR(100),
    weknora_parse_status VARCHAR(20),
    ai_analysis_snapshot TEXT,
    ai_analysis_time TIMESTAMP(3),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (knowledge_id)
);

CREATE INDEX idx_knowledge_customer_id ON crm_knowledge (customer_id);
CREATE INDEX idx_knowledge_type ON crm_knowledge (type);
CREATE INDEX idx_knowledge_upload_user_id ON crm_knowledge (upload_user_id);

CREATE TRIGGER trg_knowledge_update_time
    BEFORE UPDATE ON crm_knowledge
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_knowledge IS '知识库项目表';
COMMENT ON COLUMN crm_knowledge.type IS '类型: meeting, email, recording, document, proposal, contract';
COMMENT ON COLUMN crm_knowledge.status IS '状态: 0-处理中, 1-正常, 2-处理失败';
COMMENT ON COLUMN crm_knowledge.weknora_parse_status IS 'WeKnora解析状态: pending, processing, completed, failed, unsupported';
COMMENT ON COLUMN crm_knowledge.ai_analysis_snapshot IS 'AI analysis result snapshot';
COMMENT ON COLUMN crm_knowledge.ai_analysis_time IS 'AI analysis completion time';

-- ParadeDB BM25 全文搜索索引 (中文支持)
-- 注意：需要先安装 pg_search 扩展
-- CREATE EXTENSION IF NOT EXISTS pg_search;
-- CREATE INDEX idx_knowledge_search ON crm_knowledge
-- USING bm25 (name, content_text)
-- WITH (
--   key_field = 'knowledge_id',
--   text_fields = '{
--     "name": {"tokenizer": {"type": "chinese_compatible"}},
--     "content_text": {"tokenizer": {"type": "chinese_compatible"}}
--   }'
-- );

-- ============================================
-- 8. 知识库标签表 (crm_knowledge_tag)
-- ============================================
DROP TABLE IF EXISTS crm_knowledge_tag CASCADE;
CREATE TABLE crm_knowledge_tag (
    id BIGINT NOT NULL,
    knowledge_id BIGINT NOT NULL,
    tag_name VARCHAR(100) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_knowledge_tag_knowledge_id ON crm_knowledge_tag (knowledge_id);

COMMENT ON TABLE crm_knowledge_tag IS '知识库标签表';

-- ============================================
-- 9. 任务表 (crm_task)
-- ============================================
-- ============================================
-- 8.5 Schedule table (crm_schedule)
-- ============================================
DROP TABLE IF EXISTS crm_schedule CASCADE;
CREATE TABLE crm_schedule (
    schedule_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    type VARCHAR(20) DEFAULT 'meeting',
    customer_id BIGINT,
    relation_id BIGINT,
    contact_id BIGINT,
    location VARCHAR(255),
    participant_user_ids TEXT,
    create_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (schedule_id)
);

CREATE INDEX idx_schedule_start ON crm_schedule(start_time);
CREATE INDEX idx_schedule_customer ON crm_schedule(customer_id);
CREATE INDEX idx_schedule_relation ON crm_schedule(relation_id);
CREATE INDEX idx_schedule_user ON crm_schedule(create_user_id);

CREATE TRIGGER trg_schedule_update_time
    BEFORE UPDATE ON crm_schedule
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_schedule IS 'Schedule table';
COMMENT ON COLUMN crm_schedule.relation_id IS 'Related relation ID';
COMMENT ON COLUMN crm_schedule.participant_user_ids IS 'Comma-separated participant user IDs';

DROP TABLE IF EXISTS crm_task CASCADE;
CREATE TABLE crm_task (
    task_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_date TIMESTAMP,
    priority VARCHAR(20) DEFAULT 'medium',
    status VARCHAR(20) DEFAULT 'pending',
    assigned_to BIGINT,
    customer_id BIGINT,
    generated_by_ai SMALLINT DEFAULT 0,
    ai_context TEXT,
    completed_time TIMESTAMP,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id)
);

CREATE INDEX idx_task_assigned_to ON crm_task (assigned_to);
CREATE INDEX idx_task_customer_id ON crm_task (customer_id);
CREATE INDEX idx_task_status ON crm_task (status);
CREATE INDEX idx_task_due_date ON crm_task (due_date);

CREATE TRIGGER trg_task_update_time
    BEFORE UPDATE ON crm_task
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_task IS '任务表';
COMMENT ON COLUMN crm_task.priority IS '优先级: high, medium, low';
COMMENT ON COLUMN crm_task.status IS '状态: pending, in_progress, completed';
COMMENT ON COLUMN crm_task.generated_by_ai IS '是否AI生成: 0-否, 1-是';

-- ============================================
-- 10. AI智能体表 (crm_ai_agent)
-- ============================================
DROP TABLE IF EXISTS crm_ai_agent CASCADE;
CREATE TABLE crm_ai_agent (
    agent_id BIGINT NOT NULL,
    label VARCHAR(100) NOT NULL,
    icon_name VARCHAR(100),
    prompt TEXT NOT NULL,
    persona TEXT,
    knowledge_base_types VARCHAR(500),
    enabled SMALLINT DEFAULT 1,
    sort_order INT DEFAULT 0,
    category VARCHAR(50) DEFAULT 'custom',
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (agent_id)
);

CREATE INDEX idx_ai_agent_enabled ON crm_ai_agent (enabled);
CREATE INDEX idx_ai_agent_category ON crm_ai_agent (category);

CREATE TRIGGER trg_ai_agent_update_time
    BEFORE UPDATE ON crm_ai_agent
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_ai_agent IS 'AI智能体表';
COMMENT ON COLUMN crm_ai_agent.enabled IS '是否启用: 0-否, 1-是';
COMMENT ON COLUMN crm_ai_agent.category IS '分类: default, custom';

-- ============================================
-- 11. 会话表 (crm_chat_session)
-- ============================================
DROP TABLE IF EXISTS crm_chat_session CASCADE;
CREATE TABLE crm_chat_session (
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    agent_id BIGINT,
    customer_id BIGINT,
    employee_id BIGINT,
    relation_id BIGINT,
    product_id BIGINT,
    project_id BIGINT,
    project_task_id BIGINT,
    app_code VARCHAR(50) NOT NULL DEFAULT 'general',
    title VARCHAR(255),
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    pinned_time TIMESTAMP,
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (session_id)
);

CREATE INDEX idx_chat_session_user_id ON crm_chat_session (user_id);
CREATE INDEX idx_chat_session_agent_id ON crm_chat_session (agent_id);
CREATE INDEX idx_chat_session_customer_id ON crm_chat_session (customer_id);
CREATE INDEX idx_chat_session_employee_id ON crm_chat_session (employee_id);
CREATE INDEX idx_chat_session_relation_id ON crm_chat_session (relation_id);
CREATE INDEX idx_chat_session_product_id ON crm_chat_session (product_id);
CREATE INDEX idx_chat_session_project_id ON crm_chat_session (project_id);
CREATE INDEX idx_chat_session_project_task_id ON crm_chat_session (project_task_id);
CREATE INDEX idx_chat_session_app_code ON crm_chat_session (app_code);
CREATE INDEX idx_chat_session_user_pin ON crm_chat_session (user_id, pinned DESC, pinned_time DESC, update_time DESC);

CREATE OR REPLACE FUNCTION update_chat_session_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    IF to_jsonb(NEW) - 'pinned' - 'pinned_time' - 'update_time'
       IS NOT DISTINCT FROM to_jsonb(OLD) - 'pinned' - 'pinned_time' - 'update_time' THEN
        NEW.update_time = OLD.update_time;
    ELSE
        NEW.update_time = CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_chat_session_update_time
    BEFORE UPDATE ON crm_chat_session
    FOR EACH ROW EXECUTE FUNCTION update_chat_session_timestamp();

COMMENT ON TABLE crm_chat_session IS '会话表';
COMMENT ON COLUMN crm_chat_session.status IS '状态: 0-已归档, 1-活跃';

-- ============================================
-- 12. 聊天消息表 (crm_chat_message)
-- ============================================
DROP TABLE IF EXISTS crm_chat_message CASCADE;
CREATE TABLE crm_chat_message (
    message_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    tokens_used INT DEFAULT 0,
    model_name VARCHAR(100),
    function_call TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id)
);

CREATE INDEX idx_chat_message_session_id ON crm_chat_message (session_id);
CREATE INDEX idx_chat_message_create_time ON crm_chat_message (create_time);

COMMENT ON TABLE crm_chat_message IS '聊天消息表';
COMMENT ON COLUMN crm_chat_message.role IS '角色: user, assistant, system';

-- ============================================
-- 13. 消息附件表 (crm_chat_attachment)
-- ============================================
DROP TABLE IF EXISTS crm_chat_attachment CASCADE;
CREATE TABLE crm_chat_attachment (
    id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT DEFAULT 0,
    mime_type VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_chat_attachment_message_id ON crm_chat_attachment (message_id);

COMMENT ON TABLE crm_chat_attachment IS '聊天消息附件表';

-- ============================================
-- 14. 系统配置表 (crm_system_config)
-- ============================================
DROP TABLE IF EXISTS crm_system_config CASCADE;
CREATE TABLE crm_system_config (
    config_id BIGINT NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    config_type VARCHAR(50),
    description VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (config_id),
    UNIQUE (config_key)
);

CREATE TRIGGER trg_system_config_update_time
    BEFORE UPDATE ON crm_system_config
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_system_config IS '系统配置表';

-- ============================================
-- 15. 操作日志表 (crm_operation_log)
-- ============================================
DROP TABLE IF EXISTS crm_operation_log CASCADE;
CREATE TABLE crm_operation_log (
    log_id BIGINT NOT NULL,
    module VARCHAR(50) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    target_id BIGINT,
    target_type VARCHAR(50),
    content TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    create_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id)
);

CREATE INDEX idx_operation_log_module ON crm_operation_log (module);
CREATE INDEX idx_operation_log_target ON crm_operation_log (target_type, target_id);
CREATE INDEX idx_operation_log_create_time ON crm_operation_log (create_time);

COMMENT ON TABLE crm_operation_log IS '操作日志表';

-- ============================================
-- 16. 外部账号绑定表 (crm_external_auth_identity)
-- ============================================
DROP TABLE IF EXISTS crm_external_auth_identity CASCADE;
CREATE TABLE crm_external_auth_identity (
    id BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    email VARCHAR(255),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    display_name VARCHAR(100),
    avatar_url VARCHAR(500),
    raw_profile TEXT,
    status INTEGER NOT NULL DEFAULT 1,
    bind_time TIMESTAMP,
    last_login_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (provider, subject),
    UNIQUE (provider, user_id)
);

CREATE INDEX idx_external_auth_user_id ON crm_external_auth_identity (user_id);

CREATE TRIGGER trg_external_auth_identity_update_time
    BEFORE UPDATE ON crm_external_auth_identity
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_external_auth_identity IS '外部账号绑定表';

-- ============================================
-- 17. 后端访问日志表 (crm_access_log)
-- ============================================
DROP TABLE IF EXISTS crm_error_log CASCADE;
DROP TABLE IF EXISTS crm_access_log CASCADE;
CREATE TABLE crm_access_log (
    log_id BIGINT NOT NULL,
    user_id BIGINT,
    username VARCHAR(100),
    method VARCHAR(16) NOT NULL,
    request_uri VARCHAR(500) NOT NULL,
    query_string TEXT,
    request_headers TEXT,
    request_body TEXT,
    response_body TEXT,
    status_code INTEGER,
    business_code INTEGER,
    success BOOLEAN NOT NULL DEFAULT FALSE,
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    trace_id VARCHAR(64) NOT NULL,
    cost_ms BIGINT,
    request_truncated BOOLEAN NOT NULL DEFAULT FALSE,
    response_truncated BOOLEAN NOT NULL DEFAULT FALSE,
    result_response BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id)
);

CREATE TABLE crm_error_log (
    error_id BIGINT NOT NULL,
    access_log_id BIGINT NOT NULL,
    user_id BIGINT,
    trace_id VARCHAR(64) NOT NULL,
    exception_name VARCHAR(500),
    error_message TEXT,
    stack_trace TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (error_id)
);

CREATE INDEX idx_access_log_create_time ON crm_access_log (create_time);
CREATE INDEX idx_access_log_trace_id ON crm_access_log (trace_id);
CREATE INDEX idx_access_log_user_time ON crm_access_log (user_id, create_time);
CREATE INDEX idx_error_log_create_time ON crm_error_log (create_time);
CREATE INDEX idx_error_log_trace_id ON crm_error_log (trace_id);
CREATE INDEX idx_error_log_user_time ON crm_error_log (user_id, create_time);
CREATE INDEX idx_error_log_access_log_id ON crm_error_log (access_log_id);

COMMENT ON TABLE crm_access_log IS '后端 HTTP 访问日志';
COMMENT ON TABLE crm_error_log IS '后端系统异常日志';
COMMENT ON COLUMN crm_access_log.request_body IS '脱敏后的请求体，按应用限制截断';
COMMENT ON COLUMN crm_access_log.response_body IS '脱敏后的统一响应摘要';
COMMENT ON COLUMN crm_access_log.result_response IS 'response_body 是否来自统一 Result JSON';
COMMENT ON COLUMN crm_error_log.access_log_id IS '关联的 crm_access_log.log_id';

-- ============================================
-- 初始化演示数据
-- ============================================

-- 插入默认AI智能体
INSERT INTO crm_ai_agent (agent_id, label, icon_name, prompt, persona, knowledge_base_types, enabled, sort_order, category, create_user_id, create_time) VALUES
(1, '创建新客户', 'UserPlus', '你是一个CRM助手，帮助用户创建新的客户信息。请引导用户提供公司名称、行业、联系人等必要信息。', '你是一个专业的CRM助手，擅长帮助销售人员高效地录入客户信息。你会主动询问必要的信息，并给出合理的建议。', NULL, 1, 1, 'default', 1, NOW()),
(2, '上传会议记录', 'Upload', '你是一个CRM助手，帮助用户上传和整理会议记录。请协助用户将会议内容关联到正确的客户，并提取关键信息。', '你是一个专业的文档管理助手，擅长整理和归档会议记录。你会帮助用户提取会议要点，并建议后续行动。', '["meeting"]', 1, 2, 'default', 1, NOW()),
(3, '查询客户状态', 'Search', '你是一个CRM助手，帮助用户查询和分析客户状态。你可以根据用户的问题，提供客户的详细信息、跟进历史等。', '你是一个专业的数据分析助手，擅长客户数据分析和状态查询。你会提供清晰的客户概况和跟进建议。', NULL, 1, 3, 'default', 1, NOW()),
(4, '生成跟进任务', 'ListTodo', '你是一个CRM助手，帮助用户基于客户情况生成跟进任务。分析客户状态并建议合适的跟进行动和时间。', '你是一个专业的销售助手，擅长制定跟进计划和任务安排。你会根据客户的阶段和历史，给出针对性的跟进建议。', NULL, 1, 4, 'default', 1, NOW());

-- 插入示例客户数据
INSERT INTO crm_customer (customer_id, company_name, industry, stage, owner_id, level, source, address, website, quotation, contract_amount, revenue, last_contact_time, next_follow_time, remark, status, create_user_id, create_time) VALUES
(1001, '北京科技有限公司', '科技互联网', 'negotiation', 1, 'A', '官网咨询', '北京市海淀区中关村大街1号', 'https://www.bjtech.com', 500000.00, 0, 0, '2024-01-20 10:30:00', '2024-01-27 14:00:00', '大型互联网公司，对AI产品有强烈需求', 1, 1, NOW()),
(1002, '上海金融集团', '金融服务', 'proposal', 1, 'A', '行业展会', '上海市浦东新区陆家嘴金融中心', 'https://www.shfinance.com', 800000.00, 0, 0, '2024-01-18 15:00:00', '2024-01-25 10:00:00', '金融行业头部客户，决策周期较长', 1, 1, NOW()),
(1003, '广州制造业股份公司', '制造业', 'qualified', 1, 'B', '销售拜访', '广州市番禺区工业园区', 'https://www.gzmfg.com', 300000.00, 0, 0, '2024-01-15 09:00:00', '2024-01-28 09:00:00', '传统制造业转型，需要数字化解决方案', 1, 1, NOW()),
(1004, '深圳教育科技公司', '教育培训', 'lead', 1, 'B', '线上推广', '深圳市南山区科技园', 'https://www.szedu.com', 0, 0, 0, NULL, '2024-01-30 14:00:00', '新线索，需要初步沟通了解需求', 1, 1, NOW()),
(1005, '杭州电商平台', '零售电商', 'closed', 1, 'A', '客户转介绍', '杭州市西湖区互联网小镇', 'https://www.hzecom.com', 600000.00, 580000.00, 580000.00, '2024-01-10 16:00:00', NULL, '已成交客户，后续考虑追加销售', 1, 1, NOW()),
(1006, '成都医疗健康公司', '医疗健康', 'lost', 1, 'C', '官网咨询', '成都市高新区天府软件园', 'https://www.cdhealth.com', 200000.00, 0, 0, '2024-01-05 11:00:00', NULL, '预算不足，暂时搁置', 1, 1, NOW());

-- 插入客户标签
INSERT INTO crm_customer_tag (id, customer_id, tag_name, create_time) VALUES
(1, 1001, 'AI需求', NOW()),
(2, 1001, '大客户', NOW()),
(3, 1002, '金融行业', NOW()),
(4, 1002, '决策周期长', NOW()),
(5, 1003, '数字化转型', NOW()),
(6, 1005, 'VIP客户', NOW()),
(7, 1005, '可追加销售', NOW());

-- 插入联系人数据
INSERT INTO crm_contact (contact_id, customer_id, name, position, phone, email, wechat, is_primary, last_contact_time, notes, status, create_user_id, create_time) VALUES
(2001, 1001, '张明', '技术总监', '13800138001', 'zhangming@bjtech.com', 'zhangming_bj', 1, '2024-01-20 10:30:00', '技术决策人，对产品功能很关注', 1, 1, NOW()),
(2002, 1001, '李华', '采购经理', '13800138002', 'lihua@bjtech.com', 'lihua_bj', 0, '2024-01-19 14:00:00', '负责采购流程', 1, 1, NOW()),
(2003, 1002, '王芳', '副总裁', '13900139001', 'wangfang@shfinance.com', 'wangfang_sh', 1, '2024-01-18 15:00:00', '最终决策人', 1, 1, NOW()),
(2004, 1002, '陈强', 'IT总监', '13900139002', 'chenqiang@shfinance.com', 'chenqiang_sh', 0, '2024-01-17 10:00:00', '技术评估负责人', 1, 1, NOW()),
(2005, 1003, '刘伟', '厂长', '13700137001', 'liuwei@gzmfg.com', 'liuwei_gz', 1, '2024-01-15 09:00:00', '生产和运营负责人', 1, 1, NOW()),
(2006, 1004, '赵敏', 'CEO', '13600136001', 'zhaomin@szedu.com', 'zhaomin_sz', 1, NULL, '创始人，对教育科技很感兴趣', 1, 1, NOW()),
(2007, 1005, '孙丽', '运营总监', '13500135001', 'sunli@hzecom.com', 'sunli_hz', 1, '2024-01-10 16:00:00', '项目对接人', 1, 1, NOW());

-- 插入联系人标签
INSERT INTO crm_contact_tag (id, contact_id, tag_name, create_time) VALUES
(1, 2001, '技术决策人', NOW()),
(2, 2003, '高管', NOW()),
(3, 2003, '最终决策人', NOW()),
(4, 2006, '创始人', NOW());

-- 插入跟进记录
INSERT INTO crm_follow_up (follow_up_id, customer_id, contact_id, type, content, follow_time, next_follow_time, create_user_id, create_time) VALUES
(3001, 1001, 2001, 'meeting', '与张明总监进行了产品演示，对方对AI功能非常感兴趣，计划下周安排技术团队深入沟通。', '2024-01-20 10:30:00', '2024-01-27 14:00:00', 1, NOW()),
(3002, 1001, 2002, 'call', '与李华确认了采购流程，需要准备商务报价单。', '2024-01-19 14:00:00', '2024-01-22 10:00:00', 1, NOW()),
(3003, 1002, 2003, 'meeting', '高层会议，王芳副总对方案整体认可，但需要等待预算审批。', '2024-01-18 15:00:00', '2024-01-25 10:00:00', 1, NOW()),
(3004, 1002, 2004, 'email', '发送了技术方案文档，等待技术评估反馈。', '2024-01-17 10:00:00', '2024-01-20 10:00:00', 1, NOW()),
(3005, 1003, 2005, 'visit', '现场拜访，了解了工厂的生产流程和痛点，初步确认合作意向。', '2024-01-15 09:00:00', '2024-01-28 09:00:00', 1, NOW()),
(3006, 1005, 2007, 'call', '项目验收沟通，客户对交付成果满意，考虑后续追加采购。', '2024-01-10 16:00:00', NULL, 1, NOW());

-- 插入知识库数据
INSERT INTO crm_knowledge (knowledge_id, name, type, file_path, file_size, mime_type, customer_id, summary, content_text, status, upload_user_id, create_time) VALUES
(4001, '北京科技产品演示会议记录.docx', 'meeting', '/uploads/2024/01/meeting_bjtech_20240120.docx', 52480, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 1001, 'AI产品演示会议记录，客户对智能分析和自动化功能表现出浓厚兴趣，计划安排技术团队深入测试。', '会议时间：2024年1月20日
参会人：张明、李华、我方销售团队
会议内容：产品功能演示、技术架构介绍、商务初步沟通', 1, 1, NOW()),
(4002, '上海金融技术方案.pdf', 'proposal', '/uploads/2024/01/proposal_shfinance_v1.pdf', 1048576, 'application/pdf', 1002, '为上海金融集团定制的AI解决方案，包含数据分析、风控预警、智能报表等模块。', '上海金融集团AI解决方案
1. 项目背景
2. 解决方案
3. 实施计划
4. 商务报价', 1, 1, NOW()),
(4003, '广州制造现场调研录音.mp3', 'recording', '/uploads/2024/01/visit_gzmfg_20240115.mp3', 15728640, 'audio/mpeg', 1003, '工厂现场调研录音，记录了生产线自动化需求和MES系统升级诉求。', NULL, 1, 1, NOW()),
(4004, '杭州电商项目合同.pdf', 'contract', '/uploads/2024/01/contract_hzecom_2024.pdf', 524288, 'application/pdf', 1005, '与杭州电商平台签订的年度服务合同，合同金额58万元，服务期1年。', '服务合同
甲方：杭州电商平台
乙方：我司
合同金额：580,000元
服务期限：2024年1月1日至2024年12月31日', 1, 1, NOW());

-- 插入知识库标签
INSERT INTO crm_knowledge_tag (id, knowledge_id, tag_name, create_time) VALUES
(1, 4001, '产品演示', NOW()),
(2, 4001, 'AI功能', NOW()),
(3, 4002, '技术方案', NOW()),
(4, 4002, '金融行业', NOW()),
(5, 4003, '现场调研', NOW()),
(6, 4004, '正式合同', NOW());

-- 插入任务数据
INSERT INTO crm_task (task_id, title, description, due_date, priority, status, assigned_to, customer_id, generated_by_ai, ai_context, completed_time, create_user_id, create_time) VALUES
(5001, '准备北京科技技术交流会议', '准备技术交流材料，包括产品架构图、性能测试报告、集成方案等', '2024-01-27 14:00:00', 'high', 'pending', 1, 1001, 1, '基于会议跟进记录自动生成', NULL, 1, NOW()),
(5002, '发送上海金融报价单', '根据技术方案准备正式报价单，需要包含分期付款方案', '2024-01-25 10:00:00', 'high', 'in_progress', 1, 1002, 0, NULL, NULL, 1, NOW()),
(5003, '跟进广州制造需求确认', '确认MES系统升级的具体需求范围和预算情况', '2024-01-28 09:00:00', 'medium', 'pending', 1, 1003, 1, '基于现场调研自动生成', NULL, 1, NOW()),
(5004, '深圳教育初次拜访', '安排初次拜访，了解客户的教育科技需求和预算', '2024-01-30 14:00:00', 'medium', 'pending', 1, 1004, 0, NULL, NULL, 1, NOW()),
(5005, '杭州电商续约沟通', '与客户沟通续约事宜，了解追加采购需求', '2024-02-15 10:00:00', 'low', 'pending', 1, 1005, 1, '基于客户到期时间自动生成', NULL, 1, NOW()),
(5006, '更新CRM系统客户信息', '完善本周新增客户的详细信息', '2024-01-26 18:00:00', 'low', 'completed', 1, NULL, 0, NULL, '2024-01-25 17:30:00', 1, NOW());

-- 插入系统配置
INSERT INTO crm_system_config (config_id, config_key, config_value, config_type, description, create_time) VALUES
(1, 'ai_api_url', 'https://dashscope.aliyuncs.com/compatible-mode', 'ai', 'AI API地址', NOW()),
(2, 'ai_api_key', '', 'ai', 'AI API密钥', NOW()),
(3, 'ai_model', 'qwen3.5-plus', 'ai', 'AI模型名称', NOW()),
(4, 'ai_max_tokens', '2000', 'ai', '最大Token数', NOW()),
(5, 'ai_temperature', '0.7', 'ai', 'AI温度参数', NOW()),
(6, 'file_upload_path', '/uploads', 'file', '文件上传路径', NOW()),
(7, 'file_max_size', '52428800', 'file', '文件最大大小(字节)', NOW()),
(8, 'file_allowed_types', 'pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,mp3,mp4,jpg,png', 'file', '允许的文件类型', NOW()),
-- WeKnora 知识库服务配置
(101, 'weknora_enabled', 'false', 'weknora', 'WeKnora是否启用', NOW()),
(102, 'weknora_base_url', '', 'weknora', 'WeKnora API地址', NOW()),
(103, 'weknora_api_key', '', 'weknora', 'WeKnora API密钥', NOW()),
(104, 'weknora_knowledge_base_id', '', 'weknora', '默认知识库ID', NOW()),
(105, 'weknora_match_count', '5', 'weknora', '搜索最大结果数', NOW()),
(106, 'weknora_vector_threshold', '0.5', 'weknora', '向量相似度阈值', NOW()),
(107, 'weknora_auto_rag_enabled', 'true', 'weknora', '是否启用自动RAG', NOW());

-- 插入客户团队成员
INSERT INTO crm_customer_team (id, customer_id, user_id, role, create_user_id, create_time) VALUES
(1, 1001, 1, 'owner', 1, NOW()),
(2, 1002, 1, 'owner', 1, NOW()),
(3, 1003, 1, 'owner', 1, NOW()),
(4, 1004, 1, 'owner', 1, NOW()),
(5, 1005, 1, 'owner', 1, NOW()),
(6, 1006, 1, 'owner', 1, NOW());

-- 插入默认会话
INSERT INTO crm_chat_session (session_id, user_id, agent_id, customer_id, title, status, create_time) VALUES
(6001, 1, 1, NULL, '新客户录入', 1, NOW()),
(6002, 1, 3, 1001, '北京科技客户分析', 1, NOW());

-- 插入示例聊天消息
INSERT INTO crm_chat_message (message_id, session_id, role, content, tokens_used, model_name, create_time) VALUES
(7001, 6001, 'user', '我想创建一个新客户', 10, NULL, NOW()),
(7002, 6001, 'assistant', '好的，我来帮您创建新客户。请告诉我以下信息：

1. **公司名称**（必填）
2. **所属行业**（如：科技互联网、金融服务、制造业等）
3. **客户等级**（A/B/C）
4. **主要联系人姓名和电话**

您可以一次性告诉我，也可以逐步提供。', 120, 'gpt-3.5-turbo', NOW()),
(7003, 6002, 'user', '帮我分析一下北京科技这个客户的情况', 15, NULL, NOW()),
(7004, 6002, 'assistant', '好的，我来为您分析**北京科技有限公司**的情况：

## 基本信息
- **行业**：科技互联网
- **客户等级**：A级（重点客户）
- **当前阶段**：谈判中
- **预计金额**：50万元

## 最近动态
- 最后联系时间：2024年1月20日
- 最近进行了产品演示，技术总监张明对AI功能很感兴趣

## 待办事项
- 下次跟进：2024年1月27日（准备技术交流会议）

## 建议
1. 重点准备技术架构和性能相关资料
2. 关注采购流程，提前准备商务报价
3. 保持与技术决策人的沟通频率

需要我帮您做什么进一步的操作吗？', 280, 'gpt-3.5-turbo', NOW());
