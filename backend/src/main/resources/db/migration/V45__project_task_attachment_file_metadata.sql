ALTER TABLE crm_project_task_attachment
    ADD COLUMN IF NOT EXISTS file_path TEXT;

ALTER TABLE crm_project_task_attachment
    ADD COLUMN IF NOT EXISTS file_size BIGINT;

ALTER TABLE crm_project_task_attachment
    ADD COLUMN IF NOT EXISTS mime_type VARCHAR(255);
