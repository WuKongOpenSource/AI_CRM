-- Consolidated SQL moved out of PostgreSQL initialization scripts.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE crm_customer
    ADD COLUMN IF NOT EXISTS search_text TEXT NOT NULL DEFAULT '';

CREATE INDEX IF NOT EXISTS idx_customer_search_text_trgm
    ON crm_customer USING GIN (search_text gin_trgm_ops);

CREATE TABLE IF NOT EXISTS crm_relation (
    relation_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    avatar VARCHAR(500),
    phone VARCHAR(50),
    wechat VARCHAR(100),
    email VARCHAR(100),
    relation_type VARCHAR(50) DEFAULT 'other',
    company VARCHAR(255),
    remark TEXT,
    source VARCHAR(50) NOT NULL DEFAULT 'manual',
    source_customer_id BIGINT,
    source_contact_id BIGINT,
    status SMALLINT DEFAULT 1,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT,
    PRIMARY KEY (relation_id)
);

CREATE INDEX IF NOT EXISTS idx_relation_owner
    ON crm_relation (tenant_id, create_user_id, status);
CREATE INDEX IF NOT EXISTS idx_relation_source_contact
    ON crm_relation (tenant_id, source_contact_id, create_user_id);
CREATE INDEX IF NOT EXISTS idx_relation_name
    ON crm_relation (tenant_id, name);

DROP TRIGGER IF EXISTS trg_relation_update_time ON crm_relation;
CREATE TRIGGER trg_relation_update_time
    BEFORE UPDATE ON crm_relation
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_relation IS '关系人表';
COMMENT ON COLUMN crm_relation.relation_type IS '关系类型: friend, family, relative, partner, customer_contact, supplier, investor, other';
COMMENT ON COLUMN crm_relation.source IS '来源: manual, customer_contact';

ALTER TABLE crm_follow_up
    ALTER COLUMN customer_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS relation_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_follow_up_relation_id
    ON crm_follow_up (relation_id);

ALTER TABLE crm_knowledge
    ADD COLUMN IF NOT EXISTS relation_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_knowledge_relation_id
    ON crm_knowledge (relation_id);

ALTER TABLE crm_task
    ADD COLUMN IF NOT EXISTS relation_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_task_relation_id
    ON crm_task (relation_id);

ALTER TABLE crm_chat_session
    ADD COLUMN IF NOT EXISTS relation_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_chat_session_relation_id
    ON crm_chat_session (relation_id);

CREATE TABLE IF NOT EXISTS crm_tenant (
    tenant_id BIGINT PRIMARY KEY,
    tenant_name VARCHAR(100) NOT NULL,
    contact_name VARCHAR(50),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    status INTEGER NOT NULL DEFAULT 1,
    expire_time TIMESTAMP,
    max_users INTEGER DEFAULT 50,
    remark VARCHAR(500),
    gift_credit_total BIGINT DEFAULT 300,
    gift_credit_used BIGINT DEFAULT 0,
    purchased_credit_total BIGINT DEFAULT 0,
    purchased_credit_used BIGINT DEFAULT 0,
    weknora_api_key VARCHAR(255),
    weknora_knowledge_base_id VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE crm_tenant
    ADD COLUMN IF NOT EXISTS weknora_api_key VARCHAR(255),
    ADD COLUMN IF NOT EXISTS weknora_knowledge_base_id VARCHAR(255);

CREATE TABLE IF NOT EXISTS manager_user (
    user_id BIGINT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255),
    salt VARCHAR(64),
    img VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    realname VARCHAR(100),
    num VARCHAR(100),
    mobile VARCHAR(50),
    email VARCHAR(100),
    sex INTEGER DEFAULT 0,
    dept_id BIGINT,
    post VARCHAR(100),
    status INTEGER NOT NULL DEFAULT 1,
    parent_id BIGINT DEFAULT 0,
    tenant_id BIGINT
);

ALTER TABLE manager_user
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

CREATE TABLE IF NOT EXISTS manager_dept (
    dept_id BIGINT PRIMARY KEY,
    dept_name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    sort_order INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT
);

ALTER TABLE manager_dept
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

CREATE TABLE IF NOT EXISTS manager_role (
    role_id BIGINT PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL,
    realm VARCHAR(100),
    description VARCHAR(500),
    data_type INTEGER DEFAULT 5,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT
);

ALTER TABLE manager_role
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

CREATE TABLE IF NOT EXISTS manager_user_role (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT
);

ALTER TABLE manager_user_role
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

CREATE TABLE IF NOT EXISTS manager_menu (
    menu_id BIGINT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    realm VARCHAR(100),
    realm_name VARCHAR(100),
    type INTEGER
);

CREATE TABLE IF NOT EXISTS manager_role_menu (
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_scope INTEGER
);

ALTER TABLE manager_role_menu
    ADD COLUMN IF NOT EXISTS data_scope INTEGER;

CREATE INDEX IF NOT EXISTS idx_manager_user_username ON manager_user(username);
CREATE INDEX IF NOT EXISTS idx_manager_user_tenant ON manager_user(tenant_id);
CREATE INDEX IF NOT EXISTS idx_manager_role_tenant ON manager_role(tenant_id);
CREATE INDEX IF NOT EXISTS idx_manager_user_role_user ON manager_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_manager_user_role_tenant ON manager_user_role(tenant_id);
CREATE INDEX IF NOT EXISTS idx_manager_dept_tenant ON manager_dept(tenant_id);

ALTER TABLE IF EXISTS crm_customer ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS crm_contact ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS crm_follow_up ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS crm_task ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS crm_knowledge ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS crm_customer_tag ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS crm_contact_tag ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS crm_knowledge_tag ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS crm_customer_team ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS crm_chat_session ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS crm_chat_message ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS crm_chat_attachment ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS crm_ai_agent ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS crm_system_config ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS crm_operation_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE IF EXISTS crm_custom_field ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_customer_tenant ON crm_customer(tenant_id);
CREATE INDEX IF NOT EXISTS idx_contact_tenant ON crm_contact(tenant_id);
CREATE INDEX IF NOT EXISTS idx_follow_up_tenant ON crm_follow_up(tenant_id);
CREATE INDEX IF NOT EXISTS idx_task_tenant ON crm_task(tenant_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_tenant ON crm_knowledge(tenant_id);
CREATE INDEX IF NOT EXISTS idx_customer_tag_tenant ON crm_customer_tag(tenant_id);
CREATE INDEX IF NOT EXISTS idx_contact_tag_tenant ON crm_contact_tag(tenant_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_tag_tenant ON crm_knowledge_tag(tenant_id);
CREATE INDEX IF NOT EXISTS idx_customer_team_tenant ON crm_customer_team(tenant_id);
CREATE INDEX IF NOT EXISTS idx_chat_session_tenant ON crm_chat_session(tenant_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_tenant ON crm_chat_message(tenant_id);
CREATE INDEX IF NOT EXISTS idx_chat_attachment_tenant ON crm_chat_attachment(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ai_agent_tenant ON crm_ai_agent(tenant_id);
CREATE INDEX IF NOT EXISTS idx_system_config_tenant ON crm_system_config(tenant_id);
CREATE INDEX IF NOT EXISTS idx_operation_log_tenant ON crm_operation_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_custom_field_tenant ON crm_custom_field(tenant_id);
