ALTER TABLE crm_candidate
    ADD COLUMN IF NOT EXISTS avatar VARCHAR(500) DEFAULT NULL;

WITH candidate_avatar_field (
    field_id, entity_type, field_name, field_label, field_type, column_name, column_type,
    default_value, placeholder, is_required, is_searchable, is_show_in_list, is_unique,
    options, validation_rules, sort_order
) AS (
    VALUES
        (370000000000422::BIGINT, 'candidate', 'avatar', '头像', 'image', 'avatar', 'VARCHAR(500)', NULL, NULL, 0, 0, 0, 0, NULL, NULL, 15)
)
INSERT INTO crm_custom_field (
    field_id, entity_type, field_name, field_label, field_type, field_source, column_name, column_type,
    default_value, placeholder, is_required, is_searchable, is_show_in_list, is_unique,
    options, validation_rules, sort_order, status, create_user_id, create_time, update_time
)
SELECT field_id, entity_type, field_name, field_label, field_type, 'system', column_name, column_type,
       default_value, placeholder, is_required, is_searchable, is_show_in_list, is_unique,
       options, validation_rules, sort_order, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM candidate_avatar_field cf
WHERE NOT EXISTS (
    SELECT 1
    FROM crm_custom_field existing
    WHERE existing.entity_type = cf.entity_type
      AND existing.field_name = cf.field_name
)
  AND NOT EXISTS (
      SELECT 1
      FROM crm_custom_field existing
      WHERE existing.field_id = cf.field_id
  );
