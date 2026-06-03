ALTER TABLE crm_customer
    ADD COLUMN IF NOT EXISTS ai_analysis_status VARCHAR(32);

ALTER TABLE crm_customer
    ADD COLUMN IF NOT EXISTS ai_analysis_requested_at TIMESTAMP(3);

UPDATE crm_customer
SET ai_analysis_status = 'success'
WHERE ai_analysis_status IS NULL
  AND (
      (ai_parse_snapshot IS NOT NULL AND ai_parse_snapshot <> '')
      OR (ai_insight IS NOT NULL AND ai_insight <> '')
      OR (ai_status_detection IS NOT NULL AND ai_status_detection <> '')
  );
