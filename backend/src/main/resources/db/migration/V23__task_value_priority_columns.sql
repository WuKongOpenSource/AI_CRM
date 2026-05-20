ALTER TABLE crm_task
    ADD COLUMN IF NOT EXISTS value_priority_score INTEGER,
    ADD COLUMN IF NOT EXISTS value_priority_tier VARCHAR(20),
    ADD COLUMN IF NOT EXISTS value_priority_reason TEXT,
    ADD COLUMN IF NOT EXISTS high_value BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN crm_task.value_priority_score IS 'High-value priority score';
COMMENT ON COLUMN crm_task.value_priority_tier IS 'High-value priority tier: HIGH/MEDIUM/LOW';
COMMENT ON COLUMN crm_task.value_priority_reason IS 'Reason for the high-value priority score';
COMMENT ON COLUMN crm_task.high_value IS 'Whether the task is in the high-value bucket';
