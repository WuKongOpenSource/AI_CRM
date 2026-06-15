CREATE TABLE IF NOT EXISTS crm_project (
    project_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED',
    owner_id BIGINT,
    customer_id BIGINT,
    customer_name VARCHAR(255),
    start_date TIMESTAMP,
    due_date TIMESTAMP,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id)
);

CREATE INDEX IF NOT EXISTS idx_crm_project_update_time ON crm_project(update_time DESC);
CREATE INDEX IF NOT EXISTS idx_crm_project_owner_id ON crm_project(owner_id);
CREATE INDEX IF NOT EXISTS idx_crm_project_customer_id ON crm_project(customer_id);
CREATE INDEX IF NOT EXISTS idx_crm_project_status ON crm_project(status);

DROP TRIGGER IF EXISTS trg_project_update_time ON crm_project;
CREATE TRIGGER trg_project_update_time
    BEFORE UPDATE ON crm_project
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_project_lane (
    lane_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(64),
    sort_order INTEGER NOT NULL DEFAULT 0,
    system_flag BOOLEAN NOT NULL DEFAULT FALSE,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (lane_id)
);

CREATE INDEX IF NOT EXISTS idx_crm_project_lane_project_order
    ON crm_project_lane(project_id, sort_order);

DROP TRIGGER IF EXISTS trg_project_lane_update_time ON crm_project_lane;
CREATE TRIGGER trg_project_lane_update_time
    BEFORE UPDATE ON crm_project_lane
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TABLE IF NOT EXISTS crm_project_task (
    task_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    lane_id BIGINT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'TODO',
    due_date TIMESTAMP,
    owner_id BIGINT,
    owner_name VARCHAR(100),
    priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    customer_id BIGINT,
    customer_name VARCHAR(255),
    generated_by_ai BOOLEAN NOT NULL DEFAULT FALSE,
    ai_source_text TEXT,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id)
);

CREATE INDEX IF NOT EXISTS idx_crm_project_task_project_lane
    ON crm_project_task(project_id, lane_id);
CREATE INDEX IF NOT EXISTS idx_crm_project_task_owner
    ON crm_project_task(owner_id);
CREATE INDEX IF NOT EXISTS idx_crm_project_task_due_date
    ON crm_project_task(due_date);

DROP TRIGGER IF EXISTS trg_project_task_update_time ON crm_project_task;
CREATE TRIGGER trg_project_task_update_time
    BEFORE UPDATE ON crm_project_task
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_project IS 'Project board';
COMMENT ON TABLE crm_project_lane IS 'Project board lanes';
COMMENT ON TABLE crm_project_task IS 'Project board tasks';
