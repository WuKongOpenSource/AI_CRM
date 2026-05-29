CREATE TABLE IF NOT EXISTS crm_ai_credit_record (
    record_id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT,
    action_name VARCHAR(80) NOT NULL,
    model_source VARCHAR(20) NOT NULL DEFAULT 'system',
    billing_model_provider VARCHAR(50),
    billing_model_name VARCHAR(100),
    prompt_tokens INTEGER NOT NULL DEFAULT 0,
    completion_tokens INTEGER NOT NULL DEFAULT 0,
    total_tokens INTEGER NOT NULL DEFAULT 0,
    tokens_per_credit INTEGER NOT NULL DEFAULT 800,
    credit_multiplier NUMERIC(10, 4) NOT NULL DEFAULT 1.0000,
    chargeable BOOLEAN NOT NULL DEFAULT TRUE,
    credits_used BIGINT NOT NULL DEFAULT 0,
    gift_credits_used BIGINT NOT NULL DEFAULT 0,
    purchased_credits_used BIGINT NOT NULL DEFAULT 0,
    balance_before BIGINT NOT NULL DEFAULT 0,
    balance_after BIGINT NOT NULL DEFAULT 0,
    reference_type VARCHAR(50),
    reference_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_ai_credit_record_tokens_nonnegative
        CHECK (prompt_tokens >= 0 AND completion_tokens >= 0 AND total_tokens >= 0),
    CONSTRAINT ck_ai_credit_record_credits_nonnegative
        CHECK (credits_used >= 0 AND gift_credits_used >= 0 AND purchased_credits_used >= 0),
    CONSTRAINT ck_ai_credit_record_balance_nonnegative
        CHECK (balance_before >= 0 AND balance_after >= 0),
    CONSTRAINT ck_ai_credit_record_multiplier_positive CHECK (credit_multiplier > 0),
    CONSTRAINT ck_ai_credit_record_tokens_per_credit_positive CHECK (tokens_per_credit > 0)
);

COMMENT ON TABLE crm_ai_credit_record IS 'AI credit consumption record';
COMMENT ON COLUMN crm_ai_credit_record.action_name IS 'AI action name';
COMMENT ON COLUMN crm_ai_credit_record.model_source IS 'Model source: system/custom';
COMMENT ON COLUMN crm_ai_credit_record.chargeable IS 'Whether tenant credits were charged';
COMMENT ON COLUMN crm_ai_credit_record.credits_used IS 'Credits charged for this usage';
COMMENT ON COLUMN crm_ai_credit_record.gift_credits_used IS 'Gift credits charged for this usage';
COMMENT ON COLUMN crm_ai_credit_record.purchased_credits_used IS 'Purchased credits charged for this usage';
COMMENT ON COLUMN crm_ai_credit_record.reference_type IS 'Related business object type';
COMMENT ON COLUMN crm_ai_credit_record.reference_id IS 'Related business object id';

CREATE INDEX IF NOT EXISTS idx_ai_credit_record_tenant_time
    ON crm_ai_credit_record (tenant_id, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_ai_credit_record_tenant_action_time
    ON crm_ai_credit_record (tenant_id, action_name, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_ai_credit_record_tenant_source_time
    ON crm_ai_credit_record (tenant_id, model_source, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_ai_credit_record_tenant_reference
    ON crm_ai_credit_record (tenant_id, reference_type, reference_id);
