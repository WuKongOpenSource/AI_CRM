-- Consolidated SQL moved out of previously edited historical migrations.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE crm_customer
    ADD COLUMN IF NOT EXISTS ai_status_detection VARCHAR(100);

ALTER TABLE crm_customer
    ADD COLUMN IF NOT EXISTS ai_insight TEXT;

ALTER TABLE crm_custom_field
    ADD COLUMN IF NOT EXISTS field_source VARCHAR(50) DEFAULT 'custom';

UPDATE crm_custom_field
SET field_source = 'system'
WHERE field_source IS NULL
  AND entity_type = 'customer'
  AND field_name IN (
      'companyName', 'industry', 'stage', 'ownerId', 'level', 'source',
      'address', 'website', 'primaryContactName', 'primaryContactPhone',
      'primaryContactPosition', 'tagNames', 'quotation', 'remark',
      'contractAmount', 'revenue'
  );

DO $$
BEGIN
    -- ParadeDB is not available in every PostgreSQL environment. Keep the
    -- standard trigram index from V9, and only add BM25 when the extension exists.
    IF EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'pdb')
       AND EXISTS (SELECT 1 FROM pg_am WHERE amname = 'bm25') THEN
        EXECUTE $bm25$
            CREATE INDEX IF NOT EXISTS idx_global_search_bm25 ON crm_global_search_index
            USING bm25 (
                search_id,
                (search_text::pdb.chinese_compatible('alias=global_search_cjk')),
                (search_text::pdb.ngram(2,3,'alias=global_search_ngram'))
            )
            WITH (key_field = 'search_id')
        $bm25$;
    END IF;
END $$;
