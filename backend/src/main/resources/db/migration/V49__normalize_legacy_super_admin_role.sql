-- Normalize legacy WK CRM super admin roles so permission checks can use the
-- current built-in super_admin realm after data migration.
WITH legacy_super_admin AS (
    SELECT role_id
    FROM (
        SELECT r.role_id,
               ROW_NUMBER() OVER (
                   PARTITION BY r.tenant_id
                   ORDER BY
                       CASE WHEN r.realm LIKE 'wk\_role\_%' ESCAPE '\' THEN 0 ELSE 1 END,
                       r.role_id
               ) AS rn
        FROM manager_role r
        WHERE r.role_name IN ('超级管理员', 'super admin')
          AND (r.realm LIKE 'wk\_role\_%' ESCAPE '\' OR r.realm IS NULL)
          AND r.realm IS DISTINCT FROM 'super_admin'
          AND NOT EXISTS (
              SELECT 1
              FROM manager_role existing
              WHERE existing.tenant_id = r.tenant_id
                AND existing.realm = 'super_admin'
          )
    ) candidates
    WHERE rn = 1
)
UPDATE manager_role r
SET realm = 'super_admin'
FROM legacy_super_admin legacy
WHERE r.role_id = legacy.role_id;
