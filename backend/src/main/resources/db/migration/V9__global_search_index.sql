-- Unified global search index for customer/contact/task/schedule/knowledge

ALTER TABLE crm_schedule ADD COLUMN IF NOT EXISTS participant_user_ids TEXT;

CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS crm_global_search_index (
    search_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    entity_type VARCHAR(30) NOT NULL,
    entity_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255),
    summary TEXT,
    search_text TEXT NOT NULL DEFAULT '',
    customer_id BIGINT,
    customer_name VARCHAR(255),
    owner_user_id BIGINT,
    customer_owner_id BIGINT,
    assigned_user_id BIGINT,
    upload_user_id BIGINT,
    create_user_id BIGINT,
    participant_user_ids TEXT,
    route_path VARCHAR(500),
    sort_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_global_search_entity UNIQUE (tenant_id, entity_type, entity_id)
);

CREATE INDEX IF NOT EXISTS idx_global_search_tenant_type ON crm_global_search_index (tenant_id, entity_type);
CREATE INDEX IF NOT EXISTS idx_global_search_customer_id ON crm_global_search_index (tenant_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_global_search_owner_user_id ON crm_global_search_index (tenant_id, owner_user_id);
CREATE INDEX IF NOT EXISTS idx_global_search_customer_owner_id ON crm_global_search_index (tenant_id, customer_owner_id);
CREATE INDEX IF NOT EXISTS idx_global_search_assigned_user_id ON crm_global_search_index (tenant_id, assigned_user_id);
CREATE INDEX IF NOT EXISTS idx_global_search_upload_user_id ON crm_global_search_index (tenant_id, upload_user_id);
CREATE INDEX IF NOT EXISTS idx_global_search_create_user_id ON crm_global_search_index (tenant_id, create_user_id);
CREATE INDEX IF NOT EXISTS idx_global_search_sort_time ON crm_global_search_index (tenant_id, sort_time DESC);
CREATE INDEX IF NOT EXISTS idx_global_search_search_text_trgm ON crm_global_search_index USING GIN (search_text gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_global_search_bm25 ON crm_global_search_index
USING bm25 (
    search_id,
    (search_text::pdb.chinese_compatible('alias=global_search_cjk')),
    (search_text::pdb.ngram(2,3,'alias=global_search_ngram'))
)
WITH (key_field = 'search_id');

DROP TRIGGER IF EXISTS trg_global_search_index_update_time ON crm_global_search_index;

CREATE TRIGGER trg_global_search_index_update_time
    BEFORE UPDATE ON crm_global_search_index
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

INSERT INTO crm_global_search_index (
    tenant_id, entity_type, entity_id, title, subtitle, summary, search_text,
    customer_id, customer_name, owner_user_id, customer_owner_id, assigned_user_id,
    upload_user_id, create_user_id, participant_user_ids, route_path, sort_time
)
SELECT
    c.tenant_id,
    'customer',
    c.customer_id,
    c.company_name,
    CASE WHEN COALESCE(c.industry, '') <> '' THEN '客户 · ' || c.industry ELSE '客户' END,
    LEFT(COALESCE(c.remark, ''), 200),
    LOWER(TRIM(REGEXP_REPLACE(CONCAT_WS(' ',
        c.company_name, c.industry, c.source, c.address, c.website, c.remark,
        c.primary_contact_name, c.primary_contact_phone, c.primary_contact_position,
        c.tag_names, c.level, c.stage
    ), '[[:space:],;，；]+', ' ', 'g'))),
    c.customer_id,
    c.company_name,
    c.owner_id,
    c.owner_id,
    NULL,
    NULL,
    c.create_user_id,
    NULL,
    '/customer/' || c.customer_id,
    COALESCE(c.update_time, c.create_time)
FROM crm_customer c
WHERE COALESCE(c.status, 1) = 1
ON CONFLICT (tenant_id, entity_type, entity_id) DO UPDATE SET
    title = EXCLUDED.title,
    subtitle = EXCLUDED.subtitle,
    summary = EXCLUDED.summary,
    search_text = EXCLUDED.search_text,
    customer_id = EXCLUDED.customer_id,
    customer_name = EXCLUDED.customer_name,
    owner_user_id = EXCLUDED.owner_user_id,
    customer_owner_id = EXCLUDED.customer_owner_id,
    assigned_user_id = EXCLUDED.assigned_user_id,
    upload_user_id = EXCLUDED.upload_user_id,
    create_user_id = EXCLUDED.create_user_id,
    participant_user_ids = EXCLUDED.participant_user_ids,
    route_path = EXCLUDED.route_path,
    sort_time = EXCLUDED.sort_time,
    update_time = CURRENT_TIMESTAMP;

INSERT INTO crm_global_search_index (
    tenant_id, entity_type, entity_id, title, subtitle, summary, search_text,
    customer_id, customer_name, owner_user_id, customer_owner_id, assigned_user_id,
    upload_user_id, create_user_id, participant_user_ids, route_path, sort_time
)
SELECT
    ct.tenant_id,
    'contact',
    ct.contact_id,
    ct.name,
    CASE WHEN COALESCE(c.company_name, '') <> '' THEN '联系人 · ' || c.company_name ELSE '联系人' END,
    LEFT(COALESCE(ct.notes, ''), 200),
    LOWER(TRIM(REGEXP_REPLACE(CONCAT_WS(' ',
        ct.name, ct.position, ct.phone, ct.email, ct.wechat, ct.notes,
        c.company_name, tag_agg.tag_names
    ), '[[:space:],;，；]+', ' ', 'g'))),
    ct.customer_id,
    c.company_name,
    NULL,
    c.owner_id,
    NULL,
    NULL,
    ct.create_user_id,
    NULL,
    '/customer/' || ct.customer_id || '?openContactId=' || ct.contact_id,
    COALESCE(ct.update_time, ct.create_time)
FROM crm_contact ct
LEFT JOIN crm_customer c ON c.customer_id = ct.customer_id
LEFT JOIN (
    SELECT contact_id, STRING_AGG(tag_name, ' ' ORDER BY create_time) AS tag_names
    FROM crm_contact_tag
    GROUP BY contact_id
) tag_agg ON tag_agg.contact_id = ct.contact_id
WHERE COALESCE(ct.status, 1) = 1
ON CONFLICT (tenant_id, entity_type, entity_id) DO UPDATE SET
    title = EXCLUDED.title,
    subtitle = EXCLUDED.subtitle,
    summary = EXCLUDED.summary,
    search_text = EXCLUDED.search_text,
    customer_id = EXCLUDED.customer_id,
    customer_name = EXCLUDED.customer_name,
    owner_user_id = EXCLUDED.owner_user_id,
    customer_owner_id = EXCLUDED.customer_owner_id,
    assigned_user_id = EXCLUDED.assigned_user_id,
    upload_user_id = EXCLUDED.upload_user_id,
    create_user_id = EXCLUDED.create_user_id,
    participant_user_ids = EXCLUDED.participant_user_ids,
    route_path = EXCLUDED.route_path,
    sort_time = EXCLUDED.sort_time,
    update_time = CURRENT_TIMESTAMP;

INSERT INTO crm_global_search_index (
    tenant_id, entity_type, entity_id, title, subtitle, summary, search_text,
    customer_id, customer_name, owner_user_id, customer_owner_id, assigned_user_id,
    upload_user_id, create_user_id, participant_user_ids, route_path, sort_time
)
SELECT
    t.tenant_id,
    'task',
    t.task_id,
    t.title,
    CASE
        WHEN COALESCE(c.company_name, '') <> '' THEN '任务 · ' || c.company_name
        WHEN COALESCE(assignee.realname, '') <> '' THEN '任务 · ' || assignee.realname
        ELSE '任务'
    END,
    LEFT(COALESCE(t.description, ''), 200),
    LOWER(TRIM(REGEXP_REPLACE(CONCAT_WS(' ',
        t.title, t.description, t.task_type, t.participant_names,
        c.company_name, assignee.realname, creator.realname, t.priority, t.status
    ), '[[:space:],;，；]+', ' ', 'g'))),
    t.customer_id,
    c.company_name,
    NULL,
    c.owner_id,
    t.assigned_to,
    NULL,
    t.create_user_id,
    NULL,
    '/task?openTaskId=' || t.task_id,
    COALESCE(t.update_time, t.create_time)
FROM crm_task t
LEFT JOIN crm_customer c ON c.customer_id = t.customer_id
LEFT JOIN manager_user assignee ON assignee.user_id = t.assigned_to
LEFT JOIN manager_user creator ON creator.user_id = t.create_user_id
ON CONFLICT (tenant_id, entity_type, entity_id) DO UPDATE SET
    title = EXCLUDED.title,
    subtitle = EXCLUDED.subtitle,
    summary = EXCLUDED.summary,
    search_text = EXCLUDED.search_text,
    customer_id = EXCLUDED.customer_id,
    customer_name = EXCLUDED.customer_name,
    owner_user_id = EXCLUDED.owner_user_id,
    customer_owner_id = EXCLUDED.customer_owner_id,
    assigned_user_id = EXCLUDED.assigned_user_id,
    upload_user_id = EXCLUDED.upload_user_id,
    create_user_id = EXCLUDED.create_user_id,
    participant_user_ids = EXCLUDED.participant_user_ids,
    route_path = EXCLUDED.route_path,
    sort_time = EXCLUDED.sort_time,
    update_time = CURRENT_TIMESTAMP;

INSERT INTO crm_global_search_index (
    tenant_id, entity_type, entity_id, title, subtitle, summary, search_text,
    customer_id, customer_name, owner_user_id, customer_owner_id, assigned_user_id,
    upload_user_id, create_user_id, participant_user_ids, route_path, sort_time
)
SELECT
    s.tenant_id,
    'schedule',
    s.schedule_id,
    s.title,
    CASE WHEN COALESCE(c.company_name, '') <> '' THEN '日程 · ' || c.company_name ELSE '日程' END,
    LEFT(COALESCE(s.description, ''), 200),
    LOWER(TRIM(REGEXP_REPLACE(CONCAT_WS(' ',
        s.title, s.description, s.location, s.type, c.company_name, ct.name
    ), '[[:space:],;，；]+', ' ', 'g'))),
    s.customer_id,
    c.company_name,
    NULL,
    c.owner_id,
    NULL,
    NULL,
    s.create_user_id,
    s.participant_user_ids,
    '/calendar?openScheduleId=' || s.schedule_id,
    COALESCE(s.update_time, s.start_time, s.create_time)
FROM crm_schedule s
LEFT JOIN crm_customer c ON c.customer_id = s.customer_id
LEFT JOIN crm_contact ct ON ct.contact_id = s.contact_id
ON CONFLICT (tenant_id, entity_type, entity_id) DO UPDATE SET
    title = EXCLUDED.title,
    subtitle = EXCLUDED.subtitle,
    summary = EXCLUDED.summary,
    search_text = EXCLUDED.search_text,
    customer_id = EXCLUDED.customer_id,
    customer_name = EXCLUDED.customer_name,
    owner_user_id = EXCLUDED.owner_user_id,
    customer_owner_id = EXCLUDED.customer_owner_id,
    assigned_user_id = EXCLUDED.assigned_user_id,
    upload_user_id = EXCLUDED.upload_user_id,
    create_user_id = EXCLUDED.create_user_id,
    participant_user_ids = EXCLUDED.participant_user_ids,
    route_path = EXCLUDED.route_path,
    sort_time = EXCLUDED.sort_time,
    update_time = CURRENT_TIMESTAMP;

INSERT INTO crm_global_search_index (
    tenant_id, entity_type, entity_id, title, subtitle, summary, search_text,
    customer_id, customer_name, owner_user_id, customer_owner_id, assigned_user_id,
    upload_user_id, create_user_id, participant_user_ids, route_path, sort_time
)
SELECT
    k.tenant_id,
    'knowledge',
    k.knowledge_id,
    k.name,
    CASE WHEN COALESCE(c.company_name, '') <> '' THEN '知识库 · ' || c.company_name ELSE '知识库' END,
    LEFT(COALESCE(k.summary, ''), 200),
    LOWER(TRIM(REGEXP_REPLACE(CONCAT_WS(' ',
        k.name, k.type, c.company_name, k.summary, tag_agg.tag_names,
        LEFT(COALESCE(k.content_text, ''), 10000)
    ), '[[:space:],;，；]+', ' ', 'g'))),
    k.customer_id,
    c.company_name,
    NULL,
    c.owner_id,
    NULL,
    k.upload_user_id,
    NULL,
    NULL,
    '/knowledge?openKnowledgeId=' || k.knowledge_id,
    COALESCE(k.update_time, k.create_time)
FROM crm_knowledge k
LEFT JOIN crm_customer c ON c.customer_id = k.customer_id
LEFT JOIN (
    SELECT knowledge_id, STRING_AGG(tag_name, ' ' ORDER BY create_time) AS tag_names
    FROM crm_knowledge_tag
    GROUP BY knowledge_id
) tag_agg ON tag_agg.knowledge_id = k.knowledge_id
WHERE COALESCE(k.status, 1) <> 2
ON CONFLICT (tenant_id, entity_type, entity_id) DO UPDATE SET
    title = EXCLUDED.title,
    subtitle = EXCLUDED.subtitle,
    summary = EXCLUDED.summary,
    search_text = EXCLUDED.search_text,
    customer_id = EXCLUDED.customer_id,
    customer_name = EXCLUDED.customer_name,
    owner_user_id = EXCLUDED.owner_user_id,
    customer_owner_id = EXCLUDED.customer_owner_id,
    assigned_user_id = EXCLUDED.assigned_user_id,
    upload_user_id = EXCLUDED.upload_user_id,
    create_user_id = EXCLUDED.create_user_id,
    participant_user_ids = EXCLUDED.participant_user_ids,
    route_path = EXCLUDED.route_path,
    sort_time = EXCLUDED.sort_time,
    update_time = CURRENT_TIMESTAMP;
