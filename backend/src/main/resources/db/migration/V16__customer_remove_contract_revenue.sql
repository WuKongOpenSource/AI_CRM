DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'crm_custom_field_sort'
    ) THEN
        DELETE FROM crm_custom_field_sort
        WHERE field_id IN (
            SELECT field_id
            FROM crm_custom_field
            WHERE entity_type = 'customer'
              AND field_source = 'system'
              AND field_name IN ('contractAmount', 'revenue')
        );
    END IF;
END $$;

UPDATE crm_custom_field
SET field_label = '预计成交金额',
    placeholder = '请输入预计成交金额',
    update_time = NOW()
WHERE entity_type = 'customer'
  AND field_source = 'system'
  AND field_name = 'quotation';

DELETE FROM crm_custom_field
WHERE entity_type = 'customer'
  AND field_source = 'system'
  AND field_name IN ('contractAmount', 'revenue');

ALTER TABLE crm_customer DROP COLUMN IF EXISTS contract_amount;
ALTER TABLE crm_customer DROP COLUMN IF EXISTS revenue;
