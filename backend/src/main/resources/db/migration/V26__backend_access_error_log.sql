CREATE TABLE IF NOT EXISTS crm_access_log (
    log_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT,
    username VARCHAR(100),
    method VARCHAR(16) NOT NULL,
    request_uri VARCHAR(500) NOT NULL,
    query_string TEXT,
    request_headers TEXT,
    request_body TEXT,
    response_body TEXT,
    status_code INTEGER,
    business_code INTEGER,
    success BOOLEAN NOT NULL DEFAULT FALSE,
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    trace_id VARCHAR(64) NOT NULL,
    cost_ms BIGINT,
    request_truncated BOOLEAN NOT NULL DEFAULT FALSE,
    response_truncated BOOLEAN NOT NULL DEFAULT FALSE,
    result_response BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id)
);

CREATE TABLE IF NOT EXISTS crm_error_log (
    error_id BIGINT NOT NULL,
    access_log_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT,
    trace_id VARCHAR(64) NOT NULL,
    exception_name VARCHAR(500),
    error_message TEXT,
    stack_trace TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (error_id)
);

CREATE INDEX IF NOT EXISTS idx_access_log_create_time ON crm_access_log (create_time);
CREATE INDEX IF NOT EXISTS idx_access_log_trace_id ON crm_access_log (trace_id);
CREATE INDEX IF NOT EXISTS idx_access_log_tenant_time ON crm_access_log (tenant_id, create_time);
CREATE INDEX IF NOT EXISTS idx_access_log_user_time ON crm_access_log (user_id, create_time);

CREATE INDEX IF NOT EXISTS idx_error_log_create_time ON crm_error_log (create_time);
CREATE INDEX IF NOT EXISTS idx_error_log_trace_id ON crm_error_log (trace_id);
CREATE INDEX IF NOT EXISTS idx_error_log_tenant_time ON crm_error_log (tenant_id, create_time);
CREATE INDEX IF NOT EXISTS idx_error_log_user_time ON crm_error_log (user_id, create_time);
CREATE INDEX IF NOT EXISTS idx_error_log_access_log_id ON crm_error_log (access_log_id);

COMMENT ON TABLE crm_access_log IS 'Backend HTTP access log';
COMMENT ON TABLE crm_error_log IS 'Backend system exception log';
COMMENT ON COLUMN crm_access_log.request_body IS 'Sanitized request body, truncated at the application limit';
COMMENT ON COLUMN crm_access_log.response_body IS 'Sanitized Result response summary only';
COMMENT ON COLUMN crm_access_log.result_response IS 'Whether response_body came from unified Result JSON';
COMMENT ON COLUMN crm_error_log.access_log_id IS 'Related crm_access_log.log_id';
