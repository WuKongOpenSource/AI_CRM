ALTER TABLE crm_follow_up
    ALTER COLUMN customer_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS relation_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_follow_up_relation_id
    ON crm_follow_up (relation_id);

INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type)
SELECT 1304, 1300, 'followup:edit', '编辑', 5
WHERE NOT EXISTS (
    SELECT 1 FROM manager_menu WHERE realm = 'followup:edit'
);
