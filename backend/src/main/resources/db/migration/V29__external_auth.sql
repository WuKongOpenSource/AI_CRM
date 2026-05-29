CREATE TABLE IF NOT EXISTS crm_external_auth_identity (
    id BIGINT PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    email VARCHAR(255),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    display_name VARCHAR(100),
    avatar_url VARCHAR(500),
    external_tenant_key VARCHAR(255),
    raw_profile TEXT,
    status INTEGER NOT NULL DEFAULT 1,
    bind_time TIMESTAMP,
    last_login_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_external_auth_identity_subject
    ON crm_external_auth_identity (provider, subject);

CREATE UNIQUE INDEX IF NOT EXISTS uk_external_auth_identity_user_provider
    ON crm_external_auth_identity (provider, tenant_id, user_id);

CREATE INDEX IF NOT EXISTS idx_external_auth_identity_user
    ON crm_external_auth_identity (tenant_id, user_id);

CREATE INDEX IF NOT EXISTS idx_external_auth_identity_external_tenant
    ON crm_external_auth_identity (provider, external_tenant_key);

CREATE TABLE IF NOT EXISTS crm_external_tenant_binding (
    id BIGINT PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    external_tenant_key VARCHAR(255) NOT NULL,
    tenant_id BIGINT NOT NULL,
    external_tenant_name VARCHAR(255),
    status INTEGER NOT NULL DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_external_tenant_binding_key
    ON crm_external_tenant_binding (provider, external_tenant_key);

CREATE INDEX IF NOT EXISTS idx_external_tenant_binding_tenant
    ON crm_external_tenant_binding (tenant_id);
