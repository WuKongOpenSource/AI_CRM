-- ============================================
-- V43: IM Phase 3 — reactions, @mentions, threads
-- ============================================
CREATE TABLE IF NOT EXISTS crm_im_message_reaction (
    id              BIGINT PRIMARY KEY,
    message_id      BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    emoji           VARCHAR(32) NOT NULL,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_im_reaction ON crm_im_message_reaction (message_id, user_id, emoji);
CREATE INDEX IF NOT EXISTS idx_im_reaction_msg ON crm_im_message_reaction (message_id);

ALTER TABLE crm_im_message ADD COLUMN IF NOT EXISTS parent_id BIGINT;
ALTER TABLE crm_im_message ADD COLUMN IF NOT EXISTS reply_count INT DEFAULT 0;
ALTER TABLE crm_im_message ADD COLUMN IF NOT EXISTS last_reply_time TIMESTAMP;
ALTER TABLE crm_im_message ADD COLUMN IF NOT EXISTS mentioned_user_ids VARCHAR(500);
ALTER TABLE crm_im_message ADD COLUMN IF NOT EXISTS mention_all BOOLEAN DEFAULT false;

COMMENT ON COLUMN crm_im_message.parent_id IS '话题根消息ID（普通/根消息为空）';
COMMENT ON COLUMN crm_im_message.reply_count IS '根消息的话题回复数';
COMMENT ON COLUMN crm_im_message.mentioned_user_ids IS '@提及的用户ID(csv)';

CREATE INDEX IF NOT EXISTS idx_im_msg_conv_parent ON crm_im_message (conversation_id, parent_id);
