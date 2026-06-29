ALTER TABLE crm_candidate
    ADD COLUMN IF NOT EXISTS job_requirements TEXT DEFAULT NULL;

INSERT INTO crm_custom_field (
    field_id, entity_type, field_name, field_label, field_type, field_source, column_name, column_type,
    default_value, placeholder, is_required, is_searchable, is_show_in_list, is_unique,
    options, validation_rules, sort_order, status, create_user_id, create_time, update_time
)
SELECT
    370000000000421::BIGINT,
    'candidate',
    'jobRequirements',
    '岗位要求',
    'textarea',
    'system',
    'job_requirements',
    'TEXT',
    NULL,
    '请输入岗位职责、任职要求或招聘偏好',
    0,
    1,
    0,
    0,
    NULL,
    NULL,
    55,
    1,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM crm_custom_field
    WHERE entity_type = 'candidate'
      AND field_name = 'jobRequirements'
);

UPDATE crm_custom_field
SET field_label = '岗位要求',
    field_type = 'textarea',
    field_source = 'system',
    column_name = 'job_requirements',
    column_type = 'TEXT',
    placeholder = '请输入岗位职责、任职要求或招聘偏好',
    is_searchable = 1,
    sort_order = 55,
    status = 1,
    update_time = CURRENT_TIMESTAMP
WHERE entity_type = 'candidate'
  AND field_name = 'jobRequirements';
