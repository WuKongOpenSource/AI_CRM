-- ============================================
-- V5: AI Token 用量追踪
-- 添加 prompt/completion tokens 细粒度字段
-- 添加索引优化统计查询
-- ============================================

-- 1. 添加细粒度 token 字段
ALTER TABLE crm_chat_message ADD COLUMN IF NOT EXISTS prompt_tokens INT DEFAULT 0;
ALTER TABLE crm_chat_message ADD COLUMN IF NOT EXISTS completion_tokens INT DEFAULT 0;

COMMENT ON COLUMN crm_chat_message.prompt_tokens IS '输入token数';
COMMENT ON COLUMN crm_chat_message.completion_tokens IS '输出token数';

-- 2. 添加索引优化统计查询
CREATE INDEX IF NOT EXISTS idx_chat_message_role ON crm_chat_message (role);
CREATE INDEX IF NOT EXISTS idx_chat_session_user_create ON crm_chat_session (user_id, create_time);
