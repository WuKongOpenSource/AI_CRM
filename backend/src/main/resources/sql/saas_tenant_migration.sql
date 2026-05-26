-- ============================================================
-- SaaS 多租户改造 SQL
-- 1. 创建租户主表
-- 2. 为所有业务表添加 tenant_id 字段和索引
-- 3. 回填默认租户数据
-- ============================================================

-- 1. 创建租户主表
CREATE TABLE IF NOT EXISTS crm_tenant (
    tenant_id   BIGINT PRIMARY KEY,
    tenant_name VARCHAR(100) NOT NULL,
    contact_name VARCHAR(50),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    status      INTEGER NOT NULL DEFAULT 1,  -- 0=禁用, 1=正常, 2=试用
    expire_time TIMESTAMP,                    -- 租户到期时间，NULL=永不过期
    max_users   INTEGER DEFAULT 50,           -- 最大用户数
    gift_credit_total BIGINT DEFAULT 300,     -- 注册赠送积分总量
    gift_credit_used  BIGINT DEFAULT 0,       -- 赠送积分已使用量
    purchased_credit_total BIGINT DEFAULT 0,  -- 已购买积分总量
    purchased_credit_used  BIGINT DEFAULT 0,  -- 已购买积分已使用量
    remark      VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE crm_tenant IS '租户主表';
COMMENT ON COLUMN crm_tenant.status IS '状态：0=禁用, 1=正常, 2=试用';

-- 插入默认租户（用于迁移现有数据）
INSERT INTO crm_tenant (tenant_id, tenant_name, status, gift_credit_total, gift_credit_used, purchased_credit_total, purchased_credit_used)
VALUES (1, '默认租户', 1, 300, 0, 0, 0)
ON CONFLICT (tenant_id) DO NOTHING;

-- ============================================================
-- 2. CRM 业务核心表 添加 tenant_id
-- ============================================================

ALTER TABLE crm_customer ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE crm_contact ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE crm_follow_up ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE crm_task ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE crm_knowledge ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

-- 业务附属表
ALTER TABLE crm_customer_tag ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE crm_contact_tag ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE crm_knowledge_tag ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE crm_customer_team ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

-- AI / 聊天表
ALTER TABLE crm_chat_session ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE crm_chat_message ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE crm_chat_attachment ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE crm_ai_agent ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

-- 系统表
ALTER TABLE crm_system_config ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE crm_operation_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE crm_custom_field ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

-- RBAC 表
ALTER TABLE manager_user ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE manager_dept ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE manager_role ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE manager_user_role ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

-- ============================================================
-- 3. 回填默认租户 ID = 1（现有数据全部归属默认租户）
-- ============================================================

UPDATE crm_customer SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE crm_contact SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE crm_follow_up SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE crm_task SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE crm_knowledge SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE crm_customer_tag SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE crm_contact_tag SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE crm_knowledge_tag SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE crm_customer_team SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE crm_chat_session SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE crm_chat_message SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE crm_chat_attachment SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE crm_ai_agent SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE crm_system_config SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE crm_operation_log SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE crm_custom_field SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE manager_user SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE manager_dept SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE manager_role SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE manager_user_role SET tenant_id = 1 WHERE tenant_id IS NULL;

-- ============================================================
-- 4. 设置 NOT NULL 约束（回填完成后）
-- ============================================================

ALTER TABLE crm_customer ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE crm_contact ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE crm_follow_up ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE crm_task ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE crm_knowledge ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE crm_customer_tag ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE crm_contact_tag ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE crm_knowledge_tag ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE crm_customer_team ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE crm_chat_session ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE crm_chat_message ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE crm_chat_attachment ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE crm_ai_agent ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE crm_system_config ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE crm_operation_log ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE crm_custom_field ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE manager_user ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE manager_dept ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE manager_role ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE manager_user_role ALTER COLUMN tenant_id SET NOT NULL;

-- ============================================================
-- 5. 创建索引（加速租户隔离查询）
-- ============================================================

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
CREATE INDEX IF NOT EXISTS idx_manager_user_tenant ON manager_user(tenant_id);
CREATE INDEX IF NOT EXISTS idx_manager_dept_tenant ON manager_dept(tenant_id);
CREATE INDEX IF NOT EXISTS idx_manager_role_tenant ON manager_role(tenant_id);
CREATE INDEX IF NOT EXISTS idx_manager_user_role_tenant ON manager_user_role(tenant_id);

-- system_config 表需要租户级唯一键（同一租户内 config_key 唯一）
DROP INDEX IF EXISTS crm_system_config_config_key_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_system_config_tenant_key ON crm_system_config(tenant_id, config_key);
