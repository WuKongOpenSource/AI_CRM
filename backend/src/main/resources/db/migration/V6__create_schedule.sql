-- 日程安排表
CREATE TABLE crm_schedule (
    schedule_id    BIGINT PRIMARY KEY,
    title          VARCHAR(255) NOT NULL,
    description    TEXT,
    start_time     TIMESTAMP NOT NULL,
    end_time       TIMESTAMP,
    type           VARCHAR(20) DEFAULT 'meeting',
    customer_id    BIGINT,
    contact_id     BIGINT,
    location       VARCHAR(255),
    create_user_id BIGINT,
    create_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id      BIGINT NOT NULL
);

CREATE INDEX idx_schedule_tenant ON crm_schedule(tenant_id);
CREATE INDEX idx_schedule_start ON crm_schedule(start_time);
CREATE INDEX idx_schedule_customer ON crm_schedule(customer_id);
CREATE INDEX idx_schedule_user ON crm_schedule(create_user_id);
