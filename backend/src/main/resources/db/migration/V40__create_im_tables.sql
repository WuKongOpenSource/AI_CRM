-- ============================================
-- V40: Instant Messaging Phase 1 — direct-message tables + permission
-- Single-tenant: no tenant_id; scoping is by conversation membership.
-- ============================================

CREATE TABLE IF NOT EXISTS crm_im_conversation (
    id              BIGINT PRIMARY KEY,
    type            VARCHAR(20)  NOT NULL DEFAULT 'direct',
    member_key      VARCHAR(64),
    last_message_id BIGINT,
    create_time     TIMESTAMP(6),
    update_time     TIMESTAMP(6)
);
COMMENT ON TABLE crm_im_conversation IS 'IM 会话';
COMMENT ON COLUMN crm_im_conversation.type IS '会话类型: direct';
COMMENT ON COLUMN crm_im_conversation.member_key IS '私聊去重键: 两个用户ID升序拼接 minId_maxId';
-- Unique direct conversation per user pair
CREATE UNIQUE INDEX IF NOT EXISTS uk_im_conversation_member_key
    ON crm_im_conversation (member_key) WHERE member_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS crm_im_conversation_member (
    id                   BIGINT PRIMARY KEY,
    conversation_id      BIGINT NOT NULL,
    user_id              BIGINT NOT NULL,
    last_read_message_id BIGINT NOT NULL DEFAULT 0,
    create_time          TIMESTAMP(6),
    update_time          TIMESTAMP(6)
);
COMMENT ON TABLE crm_im_conversation_member IS 'IM 会话成员';
CREATE UNIQUE INDEX IF NOT EXISTS uk_im_member_conv_user
    ON crm_im_conversation_member (conversation_id, user_id);
CREATE INDEX IF NOT EXISTS idx_im_member_user ON crm_im_conversation_member (user_id);

CREATE TABLE IF NOT EXISTS crm_im_message (
    id              BIGINT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id       BIGINT NOT NULL,
    content_type    VARCHAR(20) NOT NULL DEFAULT 'text',
    content         TEXT,
    attachment_name VARCHAR(255),
    attachment_path VARCHAR(500),
    attachment_size BIGINT,
    attachment_mime VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'normal',
    create_time     TIMESTAMP(6),
    update_time     TIMESTAMP(6)
);
COMMENT ON TABLE crm_im_message IS 'IM 消息';
COMMENT ON COLUMN crm_im_message.content_type IS 'text/image/file';
COMMENT ON COLUMN crm_im_message.status IS 'normal/recalled';
CREATE INDEX IF NOT EXISTS idx_im_message_conv ON crm_im_message (conversation_id, id);

-- Permission menu for IM, granted to all existing roles (removable in role mgmt).
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type)
SELECT 9001, 0, 'im', '即时通讯', 3
WHERE NOT EXISTS (SELECT 1 FROM manager_menu WHERE menu_id = 9001);

INSERT INTO manager_role_menu (id, role_id, menu_id, data_scope, create_time)
SELECT (9001000000000 + r.role_id), r.role_id, 9001, 5, now()
FROM manager_role r
WHERE NOT EXISTS (
    SELECT 1 FROM manager_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = 9001
)
ON CONFLICT (id) DO NOTHING;
