ALTER TABLE crm_customer
    ADD COLUMN IF NOT EXISTS ai_status_detection TEXT,
    ADD COLUMN IF NOT EXISTS ai_insight TEXT,
    ADD COLUMN IF NOT EXISTS ai_parse_snapshot TEXT,
    ADD COLUMN IF NOT EXISTS ai_analysis_status VARCHAR(32),
    ADD COLUMN IF NOT EXISTS ai_analysis_requested_at TIMESTAMP(3),
    ADD COLUMN IF NOT EXISTS search_text TEXT;

ALTER TABLE crm_task
    ADD COLUMN IF NOT EXISTS relation_id BIGINT,
    ADD COLUMN IF NOT EXISTS project_id BIGINT,
    ADD COLUMN IF NOT EXISTS lane_id BIGINT,
    ADD COLUMN IF NOT EXISTS source_follow_up_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_task_relation_id
    ON crm_task(relation_id);

CREATE INDEX IF NOT EXISTS idx_task_project_id
    ON crm_task(project_id);

CREATE INDEX IF NOT EXISTS idx_task_lane_id
    ON crm_task(lane_id);

ALTER TABLE crm_knowledge
    ADD COLUMN IF NOT EXISTS employee_id BIGINT,
    ADD COLUMN IF NOT EXISTS relation_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_knowledge_employee_id
    ON crm_knowledge(employee_id);

CREATE INDEX IF NOT EXISTS idx_knowledge_relation_id
    ON crm_knowledge(relation_id);
