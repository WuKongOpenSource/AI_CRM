-- ============================================
-- 自定义字段功能 - PostgreSQL 数据库初始化脚本
-- 适用于 ParadeDB / PostgreSQL 17
-- ============================================

-- ============================================
-- 自定义字段定义表 (crm_custom_field)
-- ============================================
DROP TABLE IF EXISTS crm_custom_field CASCADE;
CREATE TABLE crm_custom_field (
    field_id BIGINT NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    field_label VARCHAR(100) NOT NULL,
    field_type VARCHAR(50) NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    column_type VARCHAR(100) NOT NULL,
    default_value VARCHAR(500) DEFAULT NULL,
    placeholder VARCHAR(200) DEFAULT NULL,
    is_required SMALLINT DEFAULT 0,
    is_searchable SMALLINT DEFAULT 0,
    is_show_in_list SMALLINT DEFAULT 1,
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

CREATE INDEX idx_custom_field_entity_type ON crm_custom_field (entity_type);
CREATE INDEX idx_custom_field_status ON crm_custom_field (status);

CREATE TRIGGER trg_custom_field_update_time
    BEFORE UPDATE ON crm_custom_field
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_custom_field IS '自定义字段定义表';
COMMENT ON COLUMN crm_custom_field.field_id IS '字段ID';
COMMENT ON COLUMN crm_custom_field.entity_type IS '实体类型: customer, contact';
COMMENT ON COLUMN crm_custom_field.field_name IS '字段标识(英文，用于代码)';
COMMENT ON COLUMN crm_custom_field.field_label IS '字段显示标签(中文)';
COMMENT ON COLUMN crm_custom_field.field_type IS '字段类型: text, textarea, number, date, datetime, select, multiselect, checkbox';
COMMENT ON COLUMN crm_custom_field.column_name IS '实际数据库列名';
COMMENT ON COLUMN crm_custom_field.column_type IS '数据库列类型';
COMMENT ON COLUMN crm_custom_field.is_required IS '是否必填: 0否 1是';
COMMENT ON COLUMN crm_custom_field.is_searchable IS '是否可搜索: 0否 1是';
COMMENT ON COLUMN crm_custom_field.is_show_in_list IS '是否在列表显示: 0否 1是';
COMMENT ON COLUMN crm_custom_field.options IS '选项列表(JSON数组): [{"value":"v1","label":"选项1"}]';
COMMENT ON COLUMN crm_custom_field.validation_rules IS '验证规则(JSON): {"min":0,"max":100,"pattern":""}';
COMMENT ON COLUMN crm_custom_field.status IS '状态: 0禁用 1启用';

-- ============================================
-- 示例数据（可选，用于测试）
-- ============================================
-- 以下是示例，实际添加字段时会自动ALTER TABLE添加对应列
-- INSERT INTO crm_custom_field VALUES
-- (1, 'customer', 'contractType', '合同类型', 'select', 'cf_contract_type', 'VARCHAR(100)', NULL, '请选择合同类型', 0, 1, 1, '[{"value":"new","label":"新签"},{"value":"renew","label":"续签"},{"value":"expand","label":"增购"}]', NULL, 1, 1, 1, NOW(), NOW()),
-- (2, 'customer', 'annualRevenue', '年营收', 'number', 'cf_annual_revenue', 'DECIMAL(15,2)', NULL, '请输入年营收金额', 0, 1, 1, NULL, '{"min":0}', 2, 1, 1, NOW(), NOW()),
-- (3, 'contact', 'birthday', '生日', 'date', 'cf_birthday', 'DATE', NULL, '请选择生日', 0, 0, 1, NULL, NULL, 1, 1, 1, NOW(), NOW());
