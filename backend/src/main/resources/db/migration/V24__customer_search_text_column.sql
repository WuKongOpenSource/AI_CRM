ALTER TABLE crm_customer
    ADD COLUMN IF NOT EXISTS search_text TEXT DEFAULT '';

COMMENT ON COLUMN crm_customer.search_text IS 'Aggregated text used for keyword search';
