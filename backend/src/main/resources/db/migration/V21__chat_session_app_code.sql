ALTER TABLE crm_chat_session
    ADD COLUMN IF NOT EXISTS app_code VARCHAR(50) NOT NULL DEFAULT 'general';

CREATE INDEX IF NOT EXISTS idx_chat_session_app_code
    ON crm_chat_session (app_code);
