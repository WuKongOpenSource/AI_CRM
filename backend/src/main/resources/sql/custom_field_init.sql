-- ============================================
-- 自定义字段功能 - 数据库初始化脚本
-- ============================================

-- 使用数据库
USE wk_ai_crm;

-- ============================================
-- 自定义字段定义表 (crm_custom_field)
-- ============================================
DROP TABLE IF EXISTS `crm_custom_field`;
CREATE TABLE `crm_custom_field` (
    `field_id` BIGINT NOT NULL COMMENT '字段ID',
    `entity_type` VARCHAR(50) NOT NULL COMMENT '实体类型: customer, contact',
    `field_name` VARCHAR(100) NOT NULL COMMENT '字段标识(英文，用于代码)',
    `field_label` VARCHAR(100) NOT NULL COMMENT '字段显示标签(中文)',
    `field_type` VARCHAR(50) NOT NULL COMMENT '字段类型: text, textarea, number, date, datetime, select, multiselect, checkbox',
    `column_name` VARCHAR(100) NOT NULL COMMENT '实际数据库列名',
    `column_type` VARCHAR(100) NOT NULL COMMENT '数据库列类型',
    `default_value` VARCHAR(500) DEFAULT NULL COMMENT '默认值',
    `placeholder` VARCHAR(200) DEFAULT NULL COMMENT '输入框占位提示',
    `is_required` TINYINT DEFAULT 0 COMMENT '是否必填: 0否 1是',
    `is_searchable` TINYINT DEFAULT 0 COMMENT '是否可搜索: 0否 1是',
    `is_show_in_list` TINYINT DEFAULT 1 COMMENT '是否在列表显示: 0否 1是',
    `options` TEXT DEFAULT NULL COMMENT '选项列表(JSON数组): [{"value":"v1","label":"选项1"}]',
    `validation_rules` TEXT DEFAULT NULL COMMENT '验证规则(JSON): {"min":0,"max":100,"pattern":""}',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`field_id`),
    UNIQUE KEY `uk_entity_field` (`entity_type`, `field_name`),
    UNIQUE KEY `uk_entity_column` (`entity_type`, `column_name`),
    KEY `idx_entity_type` (`entity_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自定义字段定义表';

-- ============================================
-- 示例数据（可选，用于测试）
-- ============================================
-- 以下是示例，实际添加字段时会自动ALTER TABLE添加对应列
-- INSERT INTO `crm_custom_field` VALUES
-- (1, 'customer', 'contractType', '合同类型', 'select', 'cf_contract_type', 'VARCHAR(100)', NULL, '请选择合同类型', 0, 1, 1, '[{"value":"new","label":"新签"},{"value":"renew","label":"续签"},{"value":"expand","label":"增购"}]', NULL, 1, 1, 1, NOW(), NOW()),
-- (2, 'customer', 'annualRevenue', '年营收', 'number', 'cf_annual_revenue', 'DECIMAL(15,2)', NULL, '请输入年营收金额', 0, 1, 1, NULL, '{"min":0}', 2, 1, 1, NOW(), NOW()),
-- (3, 'contact', 'birthday', '生日', 'date', 'cf_birthday', 'DATE', NULL, '请选择生日', 0, 0, 1, NULL, NULL, 1, 1, 1, NOW(), NOW());
