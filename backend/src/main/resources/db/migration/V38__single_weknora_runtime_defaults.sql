-- WEKNORA runtime defaults for single-node deployments.
-- Earlier single-node seeds wrote weknora_enabled=false into crm_system_config.
-- Because database config has priority, that disabled RAG even when deployment
-- configuration provided a valid WEKNORA service, API key, and knowledge base.
UPDATE crm_system_config enabled_cfg
SET config_value = '',
    update_time = CURRENT_TIMESTAMP
WHERE enabled_cfg.config_key = 'weknora_enabled'
  AND LOWER(TRIM(COALESCE(enabled_cfg.config_value, ''))) = 'false'
  AND NOT EXISTS (
      SELECT 1
      FROM crm_system_config configured
      WHERE configured.config_key IN ('weknora_base_url', 'weknora_api_key', 'weknora_knowledge_base_id')
        AND COALESCE(TRIM(configured.config_value), '') <> ''
  );

WITH seed_config(config_id, config_key, config_value, config_type, description) AS (
    VALUES
        (101::BIGINT, 'weknora_enabled', '', 'weknora', 'WeKnora enabled; blank follows runtime WEKNORA configuration'),
        (102::BIGINT, 'weknora_base_url', '', 'weknora', 'WeKnora API base URL'),
        (103::BIGINT, 'weknora_api_key', '', 'weknora', 'WeKnora API key'),
        (104::BIGINT, 'weknora_knowledge_base_id', '', 'weknora', 'Default WeKnora knowledge base ID'),
        (105::BIGINT, 'weknora_match_count', '5', 'weknora', 'WeKnora max match count'),
        (106::BIGINT, 'weknora_vector_threshold', '0.5', 'weknora', 'WeKnora vector threshold'),
        (107::BIGINT, 'weknora_auto_rag_enabled', 'true', 'weknora', 'WeKnora auto RAG enabled')
)
INSERT INTO crm_system_config (config_id, config_key, config_value, config_type, description, create_time)
SELECT sc.config_id,
       sc.config_key,
       sc.config_value,
       sc.config_type,
       sc.description,
       CURRENT_TIMESTAMP
FROM seed_config sc
WHERE NOT EXISTS (
    SELECT 1
    FROM crm_system_config existing
    WHERE existing.config_key = sc.config_key
)
  AND NOT EXISTS (
      SELECT 1
      FROM crm_system_config existing
      WHERE existing.config_id = sc.config_id
  );
