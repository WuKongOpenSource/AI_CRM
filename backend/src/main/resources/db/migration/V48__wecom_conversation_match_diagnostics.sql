ALTER TABLE crm_wecom_conversation
    ADD COLUMN IF NOT EXISTS archive_external_user_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS contact_employee_user_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS match_status VARCHAR(64),
    ADD COLUMN IF NOT EXISTS match_error TEXT;

CREATE INDEX IF NOT EXISTS idx_wecom_conversation_archive_external
    ON crm_wecom_conversation (tenant_id, corp_id, archive_external_user_id);

CREATE INDEX IF NOT EXISTS idx_wecom_conversation_match_status
    ON crm_wecom_conversation (tenant_id, corp_id, match_status);
