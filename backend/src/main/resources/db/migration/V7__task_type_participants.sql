-- Add task_type and participant_names columns to crm_task
ALTER TABLE crm_task ADD COLUMN task_type VARCHAR(50);
ALTER TABLE crm_task ADD COLUMN participant_names TEXT;
