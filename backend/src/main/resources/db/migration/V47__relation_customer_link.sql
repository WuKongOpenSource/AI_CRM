ALTER TABLE crm_relation
    ADD COLUMN IF NOT EXISTS customer_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_relation_customer_id
    ON crm_relation (tenant_id, customer_id);

COMMENT ON COLUMN crm_relation.customer_id IS '关联客户ID，用于关系人的所属公司选择';

UPDATE crm_custom_field
SET field_name = 'relationType'
WHERE entity_type = 'relation'
  AND field_name = 'relation_type'
  AND NOT EXISTS (
      SELECT 1
      FROM crm_custom_field existing
      WHERE existing.entity_type = 'relation'
        AND existing.field_name = 'relationType'
  );
