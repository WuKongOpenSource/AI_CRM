ALTER TABLE crm_custom_field
    ADD COLUMN IF NOT EXISTS is_unique SMALLINT DEFAULT 0;

UPDATE crm_custom_field
SET is_unique = 0
WHERE is_unique IS NULL;

COMMENT ON COLUMN crm_custom_field.is_unique IS '是否唯一: 0否 1是';
