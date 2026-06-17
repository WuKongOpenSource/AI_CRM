CREATE TABLE IF NOT EXISTS crm_custom_field (
    field_id BIGINT NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    field_label VARCHAR(100) NOT NULL,
    field_type VARCHAR(50) NOT NULL,
    field_source VARCHAR(20) NOT NULL DEFAULT 'custom',
    column_name VARCHAR(100) NOT NULL,
    column_type VARCHAR(100) NOT NULL,
    default_value VARCHAR(500) DEFAULT NULL,
    placeholder VARCHAR(200) DEFAULT NULL,
    is_required SMALLINT DEFAULT 0,
    is_searchable SMALLINT DEFAULT 0,
    is_show_in_list SMALLINT DEFAULT 1,
    is_unique SMALLINT DEFAULT 0,
    options TEXT DEFAULT NULL,
    validation_rules TEXT DEFAULT NULL,
    sort_order INT DEFAULT 0,
    status SMALLINT DEFAULT 1,
    create_user_id BIGINT DEFAULT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (field_id),
    UNIQUE (entity_type, field_name),
    UNIQUE (entity_type, column_name)
);

CREATE TABLE IF NOT EXISTS crm_custom_field_pool (
    pool_id BIGINT NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    column_type VARCHAR(100) NOT NULL,
    field_type VARCHAR(50) NOT NULL,
    column_created BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (pool_id),
    UNIQUE (entity_type, column_name)
);

CREATE TABLE IF NOT EXISTS crm_custom_field_sort (
    sort_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    field_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0,
    is_hidden SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sort_id),
    UNIQUE (user_id, entity_type, field_id)
);

ALTER TABLE crm_custom_field
    ADD COLUMN IF NOT EXISTS field_source VARCHAR(20) NOT NULL DEFAULT 'custom';

ALTER TABLE crm_custom_field
    ADD COLUMN IF NOT EXISTS is_unique SMALLINT DEFAULT 0;

UPDATE crm_custom_field
SET field_source = 'custom'
WHERE field_source IS NULL OR field_source = '';

CREATE INDEX IF NOT EXISTS idx_custom_field_entity_type ON crm_custom_field (entity_type);
CREATE INDEX IF NOT EXISTS idx_custom_field_status ON crm_custom_field (status);
CREATE INDEX IF NOT EXISTS idx_custom_field_source ON crm_custom_field (field_source);
CREATE INDEX IF NOT EXISTS idx_custom_field_pool_entity_type ON crm_custom_field_pool (entity_type, field_type);
CREATE INDEX IF NOT EXISTS idx_custom_field_sort_user_entity ON crm_custom_field_sort (user_id, entity_type);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_custom_field_update_time') THEN
        CREATE TRIGGER trg_custom_field_update_time
            BEFORE UPDATE ON crm_custom_field
            FOR EACH ROW EXECUTE FUNCTION update_timestamp();
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_custom_field_sort_update_time') THEN
        CREATE TRIGGER trg_custom_field_sort_update_time
            BEFORE UPDATE ON crm_custom_field_sort
            FOR EACH ROW EXECUTE FUNCTION update_timestamp();
    END IF;
END $$;

WITH system_fields (
    field_id, entity_type, field_name, field_label, field_type, column_name, column_type,
    default_value, placeholder, is_required, is_searchable, is_show_in_list, is_unique,
    options, validation_rules, sort_order
) AS (
    VALUES
        (370000000000001::BIGINT, 'customer', 'companyName', '公司名称', 'text', 'company_name', 'VARCHAR(255)', NULL, '请输入公司名称', 1, 1, 1, 0, NULL, NULL, 10),
        (370000000000002::BIGINT, 'customer', 'industry', '所属行业', 'text', 'industry', 'VARCHAR(100)', NULL, '请输入所属行业', 0, 1, 1, 0, NULL, NULL, 20),
        (370000000000003::BIGINT, 'customer', 'stage', '商机阶段', 'select', 'stage', 'VARCHAR(50)', 'lead', '请选择商机阶段', 0, 1, 1, 0, '[{"value":"lead","label":"线索"},{"value":"qualified","label":"资格审核"},{"value":"proposal","label":"方案报价"},{"value":"negotiation","label":"谈判中"},{"value":"closed","label":"已成交"},{"value":"lost","label":"已流失"}]', NULL, 30),
        (370000000000004::BIGINT, 'customer', 'level', '客户级别', 'select', 'level', 'VARCHAR(10)', NULL, '请选择客户级别', 0, 1, 1, 0, '[{"value":"A","label":"A 级客户"},{"value":"B","label":"B 级客户"},{"value":"C","label":"C 级客户"}]', NULL, 40),
        (370000000000005::BIGINT, 'customer', 'source', '客户来源', 'text', 'source', 'VARCHAR(100)', NULL, '请输入客户来源', 0, 1, 1, 0, NULL, NULL, 50),
        (370000000000006::BIGINT, 'customer', 'website', '公司网站', 'text', 'website', 'VARCHAR(255)', NULL, '请输入公司网站', 0, 1, 1, 0, NULL, NULL, 60),
        (370000000000007::BIGINT, 'customer', 'quotation', '预计成交金额', 'number', 'quotation', 'DECIMAL(15,2)', NULL, '请输入预计成交金额', 0, 1, 1, 0, NULL, '{"min":0}', 70),
        (370000000000008::BIGINT, 'customer', 'address', '客户地址', 'textarea', 'address', 'VARCHAR(500)', NULL, '请输入客户地址', 0, 1, 1, 0, NULL, NULL, 80),
        (370000000000009::BIGINT, 'customer', 'nextFollowTime', '下次跟进时间', 'datetime', 'next_follow_time', 'TIMESTAMP', NULL, '请选择下次跟进时间', 0, 1, 1, 0, NULL, NULL, 90),
        (370000000000010::BIGINT, 'customer', 'remark', '备注', 'textarea', 'remark', 'TEXT', NULL, '请输入备注', 0, 1, 1, 0, NULL, NULL, 100),
        (370000000000011::BIGINT, 'customer', 'primaryContactName', '主要联系人', 'text', 'primary_contact_name', 'VARCHAR(100)', NULL, NULL, 0, 1, 1, 0, NULL, NULL, 110),
        (370000000000012::BIGINT, 'customer', 'primaryContactPhone', '联系电话', 'text', 'primary_contact_phone', 'VARCHAR(50)', NULL, NULL, 0, 1, 1, 0, NULL, NULL, 120),
        (370000000000101::BIGINT, 'contact', 'name', '姓名', 'text', 'name', 'VARCHAR(100)', NULL, '请输入姓名', 1, 1, 1, 0, NULL, NULL, 10),
        (370000000000102::BIGINT, 'contact', 'position', '职位', 'text', 'position', 'VARCHAR(100)', NULL, '请输入职位', 0, 1, 1, 0, NULL, NULL, 20),
        (370000000000103::BIGINT, 'contact', 'phone', '电话', 'text', 'phone', 'VARCHAR(50)', NULL, '请输入电话', 0, 1, 1, 0, NULL, NULL, 30),
        (370000000000104::BIGINT, 'contact', 'email', '邮箱', 'text', 'email', 'VARCHAR(100)', NULL, '请输入邮箱', 0, 1, 1, 0, NULL, NULL, 40),
        (370000000000105::BIGINT, 'contact', 'wechat', '微信', 'text', 'wechat', 'VARCHAR(100)', NULL, '请输入微信号', 0, 1, 1, 0, NULL, NULL, 50),
        (370000000000106::BIGINT, 'contact', 'notes', '备注', 'textarea', 'notes', 'TEXT', NULL, '请输入备注', 0, 1, 1, 0, NULL, NULL, 60),
        (370000000000107::BIGINT, 'contact', 'isPrimary', '主要联系人', 'checkbox', 'is_primary', 'SMALLINT', '0', NULL, 0, 1, 1, 0, NULL, NULL, 70),
        (370000000000201::BIGINT, 'relation', 'name', '关系人', 'text', 'name', 'VARCHAR(100)', NULL, '请输入关系人姓名', 1, 1, 1, 0, NULL, NULL, 10),
        (370000000000202::BIGINT, 'relation', 'relationType', '关系类型', 'select', 'relation_type', 'VARCHAR(50)', 'other', '请选择关系类型', 0, 1, 1, 0, '[{"value":"decision_maker","label":"决策人"},{"value":"influencer","label":"影响人"},{"value":"partner","label":"合作伙伴"},{"value":"customer_contact","label":"客户联系人"},{"value":"other","label":"其他"}]', NULL, 20),
        (370000000000203::BIGINT, 'relation', 'phone', '电话', 'text', 'phone', 'VARCHAR(50)', NULL, '请输入电话', 0, 1, 1, 0, NULL, NULL, 30),
        (370000000000204::BIGINT, 'relation', 'email', '邮箱', 'text', 'email', 'VARCHAR(100)', NULL, '请输入邮箱', 0, 1, 1, 0, NULL, NULL, 40),
        (370000000000205::BIGINT, 'relation', 'wechat', '微信', 'text', 'wechat', 'VARCHAR(100)', NULL, '请输入微信号', 0, 1, 1, 0, NULL, NULL, 50),
        (370000000000206::BIGINT, 'relation', 'company', '所属公司', 'text', 'company', 'VARCHAR(255)', NULL, '请输入所属公司', 0, 1, 1, 0, NULL, NULL, 60),
        (370000000000207::BIGINT, 'relation', 'source', '来源', 'select', 'source', 'VARCHAR(50)', 'manual', '请选择来源', 0, 1, 1, 0, '[{"value":"manual","label":"手动创建"},{"value":"customer_contact","label":"客户联系人"},{"value":"other","label":"其他"}]', NULL, 70),
        (370000000000208::BIGINT, 'relation', 'remark', '备注', 'textarea', 'remark', 'TEXT', NULL, '请输入备注', 0, 1, 1, 0, NULL, NULL, 80),
        (370000000000301::BIGINT, 'product', 'productName', '产品名称', 'text', 'product_name', 'VARCHAR(255)', NULL, '请输入产品名称', 1, 1, 1, 0, NULL, NULL, 10),
        (370000000000302::BIGINT, 'product', 'productCode', '产品编码', 'text', 'product_code', 'VARCHAR(100)', NULL, '请输入产品编码', 0, 1, 1, 1, NULL, NULL, 20),
        (370000000000303::BIGINT, 'product', 'productType', '产品类型', 'select', 'product_type', 'VARCHAR(50)', 'goods', '请选择产品类型', 1, 1, 1, 0, '[{"value":"goods","label":"实物产品"},{"value":"service","label":"服务产品"}]', NULL, 30),
        (370000000000304::BIGINT, 'product', 'unit', '单位', 'select', 'unit', 'VARCHAR(50)', NULL, '请选择单位', 0, 1, 1, 0, '[{"value":"piece","label":"件"},{"value":"set","label":"套"},{"value":"box","label":"盒"}]', NULL, 40),
        (370000000000305::BIGINT, 'product', 'standardPrice', '标准价格', 'number', 'standard_price', 'NUMERIC(18,2)', NULL, '请输入标准价格', 0, 1, 1, 0, NULL, '{"min":0}', 50),
        (370000000000306::BIGINT, 'product', 'costPrice', '成本价格', 'number', 'cost_price', 'NUMERIC(18,2)', NULL, '请输入成本价格', 0, 1, 1, 0, NULL, '{"min":0}', 60),
        (370000000000307::BIGINT, 'product', 'status', '产品状态', 'select', 'status', 'VARCHAR(20)', 'active', '请选择产品状态', 1, 1, 1, 0, '[{"value":"active","label":"启用"},{"value":"inactive","label":"停用"}]', NULL, 70),
        (370000000000308::BIGINT, 'product', 'description', '产品描述', 'textarea', 'description', 'TEXT', NULL, '请输入产品描述', 0, 1, 1, 0, NULL, NULL, 80)
)
INSERT INTO crm_custom_field (
    field_id, entity_type, field_name, field_label, field_type, field_source, column_name, column_type,
    default_value, placeholder, is_required, is_searchable, is_show_in_list, is_unique,
    options, validation_rules, sort_order, status, create_user_id, create_time, update_time
)
SELECT field_id, entity_type, field_name, field_label, field_type, 'system', column_name, column_type,
       default_value, placeholder, is_required, is_searchable, is_show_in_list, is_unique,
       options, validation_rules, sort_order, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM system_fields sf
WHERE NOT EXISTS (
    SELECT 1
    FROM crm_custom_field existing
    WHERE existing.entity_type = sf.entity_type
      AND existing.field_name = sf.field_name
)
  AND NOT EXISTS (
      SELECT 1
      FROM crm_custom_field existing
      WHERE existing.field_id = sf.field_id
  );

WITH system_fields (
    entity_type, field_name, field_label, field_type, column_name, column_type,
    default_value, placeholder, is_required, is_searchable, is_show_in_list, is_unique,
    options, validation_rules, sort_order
) AS (
    VALUES
        ('customer', 'companyName', '公司名称', 'text', 'company_name', 'VARCHAR(255)', NULL, '请输入公司名称', 1, 1, 1, 0, NULL, NULL, 10),
        ('customer', 'industry', '所属行业', 'text', 'industry', 'VARCHAR(100)', NULL, '请输入所属行业', 0, 1, 1, 0, NULL, NULL, 20),
        ('customer', 'stage', '商机阶段', 'select', 'stage', 'VARCHAR(50)', 'lead', '请选择商机阶段', 0, 1, 1, 0, '[{"value":"lead","label":"线索"},{"value":"qualified","label":"资格审核"},{"value":"proposal","label":"方案报价"},{"value":"negotiation","label":"谈判中"},{"value":"closed","label":"已成交"},{"value":"lost","label":"已流失"}]', NULL, 30),
        ('customer', 'level', '客户级别', 'select', 'level', 'VARCHAR(10)', NULL, '请选择客户级别', 0, 1, 1, 0, '[{"value":"A","label":"A 级客户"},{"value":"B","label":"B 级客户"},{"value":"C","label":"C 级客户"}]', NULL, 40),
        ('customer', 'source', '客户来源', 'text', 'source', 'VARCHAR(100)', NULL, '请输入客户来源', 0, 1, 1, 0, NULL, NULL, 50),
        ('customer', 'website', '公司网站', 'text', 'website', 'VARCHAR(255)', NULL, '请输入公司网站', 0, 1, 1, 0, NULL, NULL, 60),
        ('customer', 'quotation', '预计成交金额', 'number', 'quotation', 'DECIMAL(15,2)', NULL, '请输入预计成交金额', 0, 1, 1, 0, NULL, '{"min":0}', 70),
        ('customer', 'address', '客户地址', 'textarea', 'address', 'VARCHAR(500)', NULL, '请输入客户地址', 0, 1, 1, 0, NULL, NULL, 80),
        ('customer', 'nextFollowTime', '下次跟进时间', 'datetime', 'next_follow_time', 'TIMESTAMP', NULL, '请选择下次跟进时间', 0, 1, 1, 0, NULL, NULL, 90),
        ('customer', 'remark', '备注', 'textarea', 'remark', 'TEXT', NULL, '请输入备注', 0, 1, 1, 0, NULL, NULL, 100),
        ('customer', 'primaryContactName', '主要联系人', 'text', 'primary_contact_name', 'VARCHAR(100)', NULL, NULL, 0, 1, 1, 0, NULL, NULL, 110),
        ('customer', 'primaryContactPhone', '联系电话', 'text', 'primary_contact_phone', 'VARCHAR(50)', NULL, NULL, 0, 1, 1, 0, NULL, NULL, 120),
        ('contact', 'name', '姓名', 'text', 'name', 'VARCHAR(100)', NULL, '请输入姓名', 1, 1, 1, 0, NULL, NULL, 10),
        ('contact', 'position', '职位', 'text', 'position', 'VARCHAR(100)', NULL, '请输入职位', 0, 1, 1, 0, NULL, NULL, 20),
        ('contact', 'phone', '电话', 'text', 'phone', 'VARCHAR(50)', NULL, '请输入电话', 0, 1, 1, 0, NULL, NULL, 30),
        ('contact', 'email', '邮箱', 'text', 'email', 'VARCHAR(100)', NULL, '请输入邮箱', 0, 1, 1, 0, NULL, NULL, 40),
        ('contact', 'wechat', '微信', 'text', 'wechat', 'VARCHAR(100)', NULL, '请输入微信号', 0, 1, 1, 0, NULL, NULL, 50),
        ('contact', 'notes', '备注', 'textarea', 'notes', 'TEXT', NULL, '请输入备注', 0, 1, 1, 0, NULL, NULL, 60),
        ('contact', 'isPrimary', '主要联系人', 'checkbox', 'is_primary', 'SMALLINT', '0', NULL, 0, 1, 1, 0, NULL, NULL, 70),
        ('relation', 'name', '关系人', 'text', 'name', 'VARCHAR(100)', NULL, '请输入关系人姓名', 1, 1, 1, 0, NULL, NULL, 10),
        ('relation', 'relationType', '关系类型', 'select', 'relation_type', 'VARCHAR(50)', 'other', '请选择关系类型', 0, 1, 1, 0, '[{"value":"decision_maker","label":"决策人"},{"value":"influencer","label":"影响人"},{"value":"partner","label":"合作伙伴"},{"value":"customer_contact","label":"客户联系人"},{"value":"other","label":"其他"}]', NULL, 20),
        ('relation', 'phone', '电话', 'text', 'phone', 'VARCHAR(50)', NULL, '请输入电话', 0, 1, 1, 0, NULL, NULL, 30),
        ('relation', 'email', '邮箱', 'text', 'email', 'VARCHAR(100)', NULL, '请输入邮箱', 0, 1, 1, 0, NULL, NULL, 40),
        ('relation', 'wechat', '微信', 'text', 'wechat', 'VARCHAR(100)', NULL, '请输入微信号', 0, 1, 1, 0, NULL, NULL, 50),
        ('relation', 'company', '所属公司', 'text', 'company', 'VARCHAR(255)', NULL, '请输入所属公司', 0, 1, 1, 0, NULL, NULL, 60),
        ('relation', 'source', '来源', 'select', 'source', 'VARCHAR(50)', 'manual', '请选择来源', 0, 1, 1, 0, '[{"value":"manual","label":"手动创建"},{"value":"customer_contact","label":"客户联系人"},{"value":"other","label":"其他"}]', NULL, 70),
        ('relation', 'remark', '备注', 'textarea', 'remark', 'TEXT', NULL, '请输入备注', 0, 1, 1, 0, NULL, NULL, 80),
        ('product', 'productName', '产品名称', 'text', 'product_name', 'VARCHAR(255)', NULL, '请输入产品名称', 1, 1, 1, 0, NULL, NULL, 10),
        ('product', 'productCode', '产品编码', 'text', 'product_code', 'VARCHAR(100)', NULL, '请输入产品编码', 0, 1, 1, 1, NULL, NULL, 20),
        ('product', 'productType', '产品类型', 'select', 'product_type', 'VARCHAR(50)', 'goods', '请选择产品类型', 1, 1, 1, 0, '[{"value":"goods","label":"实物产品"},{"value":"service","label":"服务产品"}]', NULL, 30),
        ('product', 'unit', '单位', 'select', 'unit', 'VARCHAR(50)', NULL, '请选择单位', 0, 1, 1, 0, '[{"value":"piece","label":"件"},{"value":"set","label":"套"},{"value":"box","label":"盒"}]', NULL, 40),
        ('product', 'standardPrice', '标准价格', 'number', 'standard_price', 'NUMERIC(18,2)', NULL, '请输入标准价格', 0, 1, 1, 0, NULL, '{"min":0}', 50),
        ('product', 'costPrice', '成本价格', 'number', 'cost_price', 'NUMERIC(18,2)', NULL, '请输入成本价格', 0, 1, 1, 0, NULL, '{"min":0}', 60),
        ('product', 'status', '产品状态', 'select', 'status', 'VARCHAR(20)', 'active', '请选择产品状态', 1, 1, 1, 0, '[{"value":"active","label":"启用"},{"value":"inactive","label":"停用"}]', NULL, 70),
        ('product', 'description', '产品描述', 'textarea', 'description', 'TEXT', NULL, '请输入产品描述', 0, 1, 1, 0, NULL, NULL, 80)
)
UPDATE crm_custom_field cf
SET field_source = 'system',
    field_label = sf.field_label,
    field_type = sf.field_type,
    column_name = sf.column_name,
    column_type = sf.column_type,
    default_value = sf.default_value,
    placeholder = sf.placeholder,
    is_required = sf.is_required,
    is_searchable = sf.is_searchable,
    is_show_in_list = sf.is_show_in_list,
    is_unique = sf.is_unique,
    options = sf.options,
    validation_rules = sf.validation_rules,
    sort_order = sf.sort_order,
    status = 1,
    update_time = CURRENT_TIMESTAMP
FROM system_fields sf
WHERE cf.entity_type = sf.entity_type
  AND cf.field_name = sf.field_name
  AND (
      cf.field_source IS DISTINCT FROM 'system'
      OR cf.column_name IS DISTINCT FROM sf.column_name
      OR cf.column_type IS DISTINCT FROM sf.column_type
  );

INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type)
VALUES (2502, 2500, 'addressBook:detail', '详情', 5)
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO manager_role_menu (id, role_id, menu_id, data_scope, create_user_id, create_time)
SELECT r.role_id + 2502,
       r.role_id,
       2502,
       5,
       1,
       CURRENT_TIMESTAMP
FROM manager_role r
WHERE r.realm = 'super_admin'
  AND NOT EXISTS (
      SELECT 1
      FROM manager_role_menu rm
      WHERE rm.role_id = r.role_id
        AND rm.menu_id = 2502
  )
ON CONFLICT (id) DO NOTHING;
