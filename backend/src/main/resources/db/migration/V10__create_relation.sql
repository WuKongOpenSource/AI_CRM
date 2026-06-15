CREATE TABLE IF NOT EXISTS crm_relation (
    relation_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    avatar VARCHAR(500),
    phone VARCHAR(50),
    wechat VARCHAR(100),
    email VARCHAR(100),
    relation_type VARCHAR(50) DEFAULT 'other',
    company VARCHAR(255),
    customer_id BIGINT,
    remark TEXT,
    source VARCHAR(50) NOT NULL DEFAULT 'manual',
    source_customer_id BIGINT,
    source_contact_id BIGINT,
    status SMALLINT DEFAULT 1,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (relation_id)
);

CREATE INDEX IF NOT EXISTS idx_relation_owner
    ON crm_relation (create_user_id, status);

CREATE INDEX IF NOT EXISTS idx_relation_customer_id
    ON crm_relation (customer_id);

CREATE INDEX IF NOT EXISTS idx_relation_source_contact
    ON crm_relation (source_contact_id, create_user_id);

CREATE INDEX IF NOT EXISTS idx_relation_name
    ON crm_relation (name);

DROP TRIGGER IF EXISTS trg_relation_update_time ON crm_relation;
CREATE TRIGGER trg_relation_update_time
    BEFORE UPDATE ON crm_relation
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_relation IS 'External relation records';
COMMENT ON COLUMN crm_relation.customer_id IS 'Linked customer ID';
