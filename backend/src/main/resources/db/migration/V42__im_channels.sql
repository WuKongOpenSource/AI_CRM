-- ============================================
-- V42: IM group channels. Extends crm_im_conversation (type now direct|channel).
-- ============================================
ALTER TABLE crm_im_conversation ADD COLUMN IF NOT EXISTS name VARCHAR(100);
ALTER TABLE crm_im_conversation ADD COLUMN IF NOT EXISTS description VARCHAR(500);
ALTER TABLE crm_im_conversation ADD COLUMN IF NOT EXISTS visibility VARCHAR(16);
ALTER TABLE crm_im_conversation ADD COLUMN IF NOT EXISTS owner_id BIGINT;

COMMENT ON COLUMN crm_im_conversation.name IS '频道名称（direct 为空）';
COMMENT ON COLUMN crm_im_conversation.visibility IS 'public/private（direct 为空）';
COMMENT ON COLUMN crm_im_conversation.owner_id IS '频道创建者用户ID';

CREATE INDEX IF NOT EXISTS idx_im_conv_channel ON crm_im_conversation (type, visibility);
