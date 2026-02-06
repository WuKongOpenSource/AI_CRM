-- ============================================
-- AI智能CRM系统 - 数据库初始化脚本
-- ============================================

-- 使用数据库
USE wk_ai_crm;

-- ============================================
-- 1. 客户表 (crm_customer)
-- ============================================
DROP TABLE IF EXISTS `crm_customer`;
CREATE TABLE `crm_customer` (
    `customer_id` BIGINT NOT NULL COMMENT '客户ID',
    `company_name` VARCHAR(255) NOT NULL COMMENT '公司名称',
    `industry` VARCHAR(100) COMMENT '行业',
    `stage` VARCHAR(50) NOT NULL DEFAULT 'lead' COMMENT '阶段: lead, qualified, proposal, negotiation, closed, lost',
    `owner_id` BIGINT NOT NULL COMMENT '负责人ID',
    `level` CHAR(1) DEFAULT 'C' COMMENT '客户等级: A, B, C',
    `source` VARCHAR(100) COMMENT '客户来源',
    `address` VARCHAR(500) COMMENT '地址',
    `website` VARCHAR(255) COMMENT '网站',
    `quotation` DECIMAL(15,2) DEFAULT 0 COMMENT '报价金额',
    `contract_amount` DECIMAL(15,2) DEFAULT 0 COMMENT '合同金额',
    `revenue` DECIMAL(15,2) DEFAULT 0 COMMENT '收入金额',
    `last_contact_time` DATETIME COMMENT '最后联系时间',
    `next_follow_time` DATETIME COMMENT '下次跟进时间',
    `remark` TEXT COMMENT '备注',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    `create_user_id` BIGINT COMMENT '创建人ID',
    `update_user_id` BIGINT COMMENT '修改人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`customer_id`),
    INDEX `idx_owner_id` (`owner_id`),
    INDEX `idx_stage` (`stage`),
    INDEX `idx_level` (`level`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户表';

-- ============================================
-- 2. 客户团队成员表 (crm_customer_team)
-- ============================================
DROP TABLE IF EXISTS `crm_customer_team`;
CREATE TABLE `crm_customer_team` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `customer_id` BIGINT NOT NULL COMMENT '客户ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` VARCHAR(50) DEFAULT 'member' COMMENT '角色: owner, member',
    `create_user_id` BIGINT COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_customer_user` (`customer_id`, `user_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户团队成员表';

-- ============================================
-- 3. 客户标签表 (crm_customer_tag)
-- ============================================
DROP TABLE IF EXISTS `crm_customer_tag`;
CREATE TABLE `crm_customer_tag` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `customer_id` BIGINT NOT NULL COMMENT '客户ID',
    `tag_name` VARCHAR(100) NOT NULL COMMENT '标签名称',
    `color` VARCHAR(20) DEFAULT '#3b82f6' COMMENT '标签颜色',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_customer_id` (`customer_id`),
    INDEX `idx_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户标签表';

-- ============================================
-- 4. 联系人表 (crm_contact)
-- ============================================
DROP TABLE IF EXISTS `crm_contact`;
CREATE TABLE `crm_contact` (
    `contact_id` BIGINT NOT NULL COMMENT '联系人ID',
    `customer_id` BIGINT NOT NULL COMMENT '客户ID',
    `name` VARCHAR(100) NOT NULL COMMENT '姓名',
    `position` VARCHAR(100) COMMENT '职位',
    `phone` VARCHAR(50) COMMENT '电话',
    `email` VARCHAR(100) COMMENT '邮箱',
    `wechat` VARCHAR(100) COMMENT '微信',
    `is_primary` TINYINT DEFAULT 0 COMMENT '是否主联系人: 0-否, 1-是',
    `last_contact_time` DATETIME COMMENT '最后联系时间',
    `notes` TEXT COMMENT '备注',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    `create_user_id` BIGINT COMMENT '创建人ID',
    `update_user_id` BIGINT COMMENT '修改人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`contact_id`),
    INDEX `idx_customer_id` (`customer_id`),
    INDEX `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='联系人表';

-- ============================================
-- 5. 联系人标签表 (crm_contact_tag)
-- ============================================
DROP TABLE IF EXISTS `crm_contact_tag`;
CREATE TABLE `crm_contact_tag` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `contact_id` BIGINT NOT NULL COMMENT '联系人ID',
    `tag_name` VARCHAR(100) NOT NULL COMMENT '标签名称',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_contact_id` (`contact_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='联系人标签表';

-- ============================================
-- 6. 跟进记录表 (crm_follow_up)
-- ============================================
DROP TABLE IF EXISTS `crm_follow_up`;
CREATE TABLE `crm_follow_up` (
    `follow_up_id` BIGINT NOT NULL COMMENT '跟进ID',
    `customer_id` BIGINT NOT NULL COMMENT '客户ID',
    `contact_id` BIGINT COMMENT '联系人ID',
    `type` VARCHAR(50) NOT NULL COMMENT '类型: call, meeting, email, visit',
    `content` TEXT NOT NULL COMMENT '跟进内容',
    `follow_time` DATETIME NOT NULL COMMENT '跟进时间',
    `next_follow_time` DATETIME COMMENT '下次跟进时间',
    `create_user_id` BIGINT COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`follow_up_id`),
    INDEX `idx_customer_id` (`customer_id`),
    INDEX `idx_follow_time` (`follow_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='跟进记录表';

-- ============================================
-- 7. 知识库项目表 (crm_knowledge)
-- ============================================
DROP TABLE IF EXISTS `crm_knowledge`;
CREATE TABLE `crm_knowledge` (
    `knowledge_id` BIGINT NOT NULL COMMENT '知识ID',
    `name` VARCHAR(255) NOT NULL COMMENT '名称',
    `type` VARCHAR(50) NOT NULL COMMENT '类型: meeting, email, recording, document, proposal, contract',
    `file_path` VARCHAR(500) COMMENT '文件路径',
    `file_size` BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    `mime_type` VARCHAR(100) COMMENT 'MIME类型',
    `customer_id` BIGINT COMMENT '关联客户ID',
    `summary` TEXT COMMENT 'AI摘要',
    `content_text` LONGTEXT COMMENT '文本内容(用于搜索)',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-处理中, 1-正常, 2-处理失败',
    `upload_user_id` BIGINT NOT NULL COMMENT '上传人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`knowledge_id`),
    INDEX `idx_customer_id` (`customer_id`),
    INDEX `idx_type` (`type`),
    INDEX `idx_upload_user_id` (`upload_user_id`),
    FULLTEXT INDEX `ft_content` (`name`, `content_text`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库项目表';

-- ============================================
-- 8. 知识库标签表 (crm_knowledge_tag)
-- ============================================
DROP TABLE IF EXISTS `crm_knowledge_tag`;
CREATE TABLE `crm_knowledge_tag` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `knowledge_id` BIGINT NOT NULL COMMENT '知识ID',
    `tag_name` VARCHAR(100) NOT NULL COMMENT '标签名称',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_knowledge_id` (`knowledge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库标签表';

-- ============================================
-- 9. 任务表 (crm_task)
-- ============================================
DROP TABLE IF EXISTS `crm_task`;
CREATE TABLE `crm_task` (
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `title` VARCHAR(255) NOT NULL COMMENT '标题',
    `description` TEXT COMMENT '描述',
    `due_date` DATETIME COMMENT '截止日期',
    `priority` VARCHAR(20) DEFAULT 'medium' COMMENT '优先级: high, medium, low',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending, in_progress, completed',
    `assigned_to` BIGINT COMMENT '指派人ID',
    `customer_id` BIGINT COMMENT '关联客户ID',
    `generated_by_ai` TINYINT DEFAULT 0 COMMENT '是否AI生成: 0-否, 1-是',
    `ai_context` TEXT COMMENT 'AI生成上下文',
    `completed_time` DATETIME COMMENT '完成时间',
    `create_user_id` BIGINT COMMENT '创建人ID',
    `update_user_id` BIGINT COMMENT '修改人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`task_id`),
    INDEX `idx_assigned_to` (`assigned_to`),
    INDEX `idx_customer_id` (`customer_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_due_date` (`due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- ============================================
-- 10. AI智能体表 (crm_ai_agent)
-- ============================================
DROP TABLE IF EXISTS `crm_ai_agent`;
CREATE TABLE `crm_ai_agent` (
    `agent_id` BIGINT NOT NULL COMMENT '智能体ID',
    `label` VARCHAR(100) NOT NULL COMMENT '显示名称',
    `icon_name` VARCHAR(100) COMMENT '图标名称',
    `prompt` TEXT NOT NULL COMMENT '系统提示词',
    `persona` TEXT COMMENT '角色人设',
    `knowledge_base_types` VARCHAR(500) COMMENT '知识库类型(JSON数组)',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用: 0-否, 1-是',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `category` VARCHAR(50) DEFAULT 'custom' COMMENT '分类: default, custom',
    `create_user_id` BIGINT COMMENT '创建人ID',
    `update_user_id` BIGINT COMMENT '修改人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`agent_id`),
    INDEX `idx_enabled` (`enabled`),
    INDEX `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI智能体表';

-- ============================================
-- 11. 会话表 (crm_chat_session)
-- ============================================
DROP TABLE IF EXISTS `crm_chat_session`;
CREATE TABLE `crm_chat_session` (
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `agent_id` BIGINT COMMENT '智能体ID',
    `customer_id` BIGINT COMMENT '关联客户ID',
    `title` VARCHAR(255) COMMENT '会话标题',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-已归档, 1-活跃',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`session_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_agent_id` (`agent_id`),
    INDEX `idx_customer_id` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- ============================================
-- 12. 聊天消息表 (crm_chat_message)
-- ============================================
DROP TABLE IF EXISTS `crm_chat_message`;
CREATE TABLE `crm_chat_message` (
    `message_id` BIGINT NOT NULL COMMENT '消息ID',
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色: user, assistant, system',
    `content` LONGTEXT NOT NULL COMMENT '消息内容',
    `tokens_used` INT DEFAULT 0 COMMENT '使用token数',
    `model_name` VARCHAR(100) COMMENT '使用的模型',
    `function_call` TEXT COMMENT '函数调用(JSON)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`message_id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- ============================================
-- 13. 消息附件表 (crm_chat_attachment)
-- ============================================
DROP TABLE IF EXISTS `crm_chat_attachment`;
CREATE TABLE `crm_chat_attachment` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `message_id` BIGINT NOT NULL COMMENT '消息ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
    `file_size` BIGINT DEFAULT 0 COMMENT '文件大小',
    `mime_type` VARCHAR(100) COMMENT 'MIME类型',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_message_id` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息附件表';

-- ============================================
-- 14. 系统配置表 (crm_system_config)
-- ============================================
DROP TABLE IF EXISTS `crm_system_config`;
CREATE TABLE `crm_system_config` (
    `config_id` BIGINT NOT NULL COMMENT '配置ID',
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` TEXT COMMENT '配置值',
    `config_type` VARCHAR(50) COMMENT '配置类型',
    `description` VARCHAR(500) COMMENT '描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`config_id`),
    UNIQUE INDEX `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ============================================
-- 15. 操作日志表 (crm_operation_log)
-- ============================================
DROP TABLE IF EXISTS `crm_operation_log`;
CREATE TABLE `crm_operation_log` (
    `log_id` BIGINT NOT NULL COMMENT '日志ID',
    `module` VARCHAR(50) NOT NULL COMMENT '模块',
    `operation` VARCHAR(50) NOT NULL COMMENT '操作类型',
    `target_id` BIGINT COMMENT '目标ID',
    `target_type` VARCHAR(50) COMMENT '目标类型',
    `content` TEXT COMMENT '操作内容',
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `user_agent` VARCHAR(500) COMMENT 'User Agent',
    `create_user_id` BIGINT COMMENT '操作人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`log_id`),
    INDEX `idx_module` (`module`),
    INDEX `idx_target` (`target_type`, `target_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ============================================
-- 初始化演示数据
-- ============================================

-- 插入默认AI智能体
INSERT INTO `crm_ai_agent` (`agent_id`, `label`, `icon_name`, `prompt`, `persona`, `knowledge_base_types`, `enabled`, `sort_order`, `category`, `create_user_id`, `create_time`) VALUES
(1, '创建新客户', 'UserPlus', '你是一个CRM助手，帮助用户创建新的客户信息。请引导用户提供公司名称、行业、联系人等必要信息。', '你是一个专业的CRM助手，擅长帮助销售人员高效地录入客户信息。你会主动询问必要的信息，并给出合理的建议。', NULL, 1, 1, 'default', 1, NOW()),
(2, '上传会议记录', 'Upload', '你是一个CRM助手，帮助用户上传和整理会议记录。请协助用户将会议内容关联到正确的客户，并提取关键信息。', '你是一个专业的文档管理助手，擅长整理和归档会议记录。你会帮助用户提取会议要点，并建议后续行动。', '["meeting"]', 1, 2, 'default', 1, NOW()),
(3, '查询客户状态', 'Search', '你是一个CRM助手，帮助用户查询和分析客户状态。你可以根据用户的问题，提供客户的详细信息、跟进历史等。', '你是一个专业的数据分析助手，擅长客户数据分析和状态查询。你会提供清晰的客户概况和跟进建议。', NULL, 1, 3, 'default', 1, NOW()),
(4, '生成跟进任务', 'ListTodo', '你是一个CRM助手，帮助用户基于客户情况生成跟进任务。分析客户状态并建议合适的跟进行动和时间。', '你是一个专业的销售助手，擅长制定跟进计划和任务安排。你会根据客户的阶段和历史，给出针对性的跟进建议。', NULL, 1, 4, 'default', 1, NOW());

-- 插入示例客户数据
INSERT INTO `crm_customer` (`customer_id`, `company_name`, `industry`, `stage`, `owner_id`, `level`, `source`, `address`, `website`, `quotation`, `contract_amount`, `revenue`, `last_contact_time`, `next_follow_time`, `remark`, `status`, `create_user_id`, `create_time`) VALUES
(1001, '北京科技有限公司', '科技互联网', 'negotiation', 1, 'A', '官网咨询', '北京市海淀区中关村大街1号', 'https://www.bjtech.com', 500000.00, 0, 0, '2024-01-20 10:30:00', '2024-01-27 14:00:00', '大型互联网公司，对AI产品有强烈需求', 1, 1, NOW()),
(1002, '上海金融集团', '金融服务', 'proposal', 1, 'A', '行业展会', '上海市浦东新区陆家嘴金融中心', 'https://www.shfinance.com', 800000.00, 0, 0, '2024-01-18 15:00:00', '2024-01-25 10:00:00', '金融行业头部客户，决策周期较长', 1, 1, NOW()),
(1003, '广州制造业股份公司', '制造业', 'qualified', 1, 'B', '销售拜访', '广州市番禺区工业园区', 'https://www.gzmfg.com', 300000.00, 0, 0, '2024-01-15 09:00:00', '2024-01-28 09:00:00', '传统制造业转型，需要数字化解决方案', 1, 1, NOW()),
(1004, '深圳教育科技公司', '教育培训', 'lead', 1, 'B', '线上推广', '深圳市南山区科技园', 'https://www.szedu.com', 0, 0, 0, NULL, '2024-01-30 14:00:00', '新线索，需要初步沟通了解需求', 1, 1, NOW()),
(1005, '杭州电商平台', '零售电商', 'closed', 1, 'A', '客户转介绍', '杭州市西湖区互联网小镇', 'https://www.hzecom.com', 600000.00, 580000.00, 580000.00, '2024-01-10 16:00:00', NULL, '已成交客户，后续考虑追加销售', 1, 1, NOW()),
(1006, '成都医疗健康公司', '医疗健康', 'lost', 1, 'C', '官网咨询', '成都市高新区天府软件园', 'https://www.cdhealth.com', 200000.00, 0, 0, '2024-01-05 11:00:00', NULL, '预算不足，暂时搁置', 1, 1, NOW());

-- 插入客户标签
INSERT INTO `crm_customer_tag` (`id`, `customer_id`, `tag_name`, `create_time`) VALUES
(1, 1001, 'AI需求', NOW()),
(2, 1001, '大客户', NOW()),
(3, 1002, '金融行业', NOW()),
(4, 1002, '决策周期长', NOW()),
(5, 1003, '数字化转型', NOW()),
(6, 1005, 'VIP客户', NOW()),
(7, 1005, '可追加销售', NOW());

-- 插入联系人数据
INSERT INTO `crm_contact` (`contact_id`, `customer_id`, `name`, `position`, `phone`, `email`, `wechat`, `is_primary`, `last_contact_time`, `notes`, `status`, `create_user_id`, `create_time`) VALUES
(2001, 1001, '张明', '技术总监', '13800138001', 'zhangming@bjtech.com', 'zhangming_bj', 1, '2024-01-20 10:30:00', '技术决策人，对产品功能很关注', 1, 1, NOW()),
(2002, 1001, '李华', '采购经理', '13800138002', 'lihua@bjtech.com', 'lihua_bj', 0, '2024-01-19 14:00:00', '负责采购流程', 1, 1, NOW()),
(2003, 1002, '王芳', '副总裁', '13900139001', 'wangfang@shfinance.com', 'wangfang_sh', 1, '2024-01-18 15:00:00', '最终决策人', 1, 1, NOW()),
(2004, 1002, '陈强', 'IT总监', '13900139002', 'chenqiang@shfinance.com', 'chenqiang_sh', 0, '2024-01-17 10:00:00', '技术评估负责人', 1, 1, NOW()),
(2005, 1003, '刘伟', '厂长', '13700137001', 'liuwei@gzmfg.com', 'liuwei_gz', 1, '2024-01-15 09:00:00', '生产和运营负责人', 1, 1, NOW()),
(2006, 1004, '赵敏', 'CEO', '13600136001', 'zhaomin@szedu.com', 'zhaomin_sz', 1, NULL, '创始人，对教育科技很感兴趣', 1, 1, NOW()),
(2007, 1005, '孙丽', '运营总监', '13500135001', 'sunli@hzecom.com', 'sunli_hz', 1, '2024-01-10 16:00:00', '项目对接人', 1, 1, NOW());

-- 插入联系人标签
INSERT INTO `crm_contact_tag` (`id`, `contact_id`, `tag_name`, `create_time`) VALUES
(1, 2001, '技术决策人', NOW()),
(2, 2003, '高管', NOW()),
(3, 2003, '最终决策人', NOW()),
(4, 2006, '创始人', NOW());

-- 插入跟进记录
INSERT INTO `crm_follow_up` (`follow_up_id`, `customer_id`, `contact_id`, `type`, `content`, `follow_time`, `next_follow_time`, `create_user_id`, `create_time`) VALUES
(3001, 1001, 2001, 'meeting', '与张明总监进行了产品演示，对方对AI功能非常感兴趣，计划下周安排技术团队深入沟通。', '2024-01-20 10:30:00', '2024-01-27 14:00:00', 1, NOW()),
(3002, 1001, 2002, 'call', '与李华确认了采购流程，需要准备商务报价单。', '2024-01-19 14:00:00', '2024-01-22 10:00:00', 1, NOW()),
(3003, 1002, 2003, 'meeting', '高层会议，王芳副总对方案整体认可，但需要等待预算审批。', '2024-01-18 15:00:00', '2024-01-25 10:00:00', 1, NOW()),
(3004, 1002, 2004, 'email', '发送了技术方案文档，等待技术评估反馈。', '2024-01-17 10:00:00', '2024-01-20 10:00:00', 1, NOW()),
(3005, 1003, 2005, 'visit', '现场拜访，了解了工厂的生产流程和痛点，初步确认合作意向。', '2024-01-15 09:00:00', '2024-01-28 09:00:00', 1, NOW()),
(3006, 1005, 2007, 'call', '项目验收沟通，客户对交付成果满意，考虑后续追加采购。', '2024-01-10 16:00:00', NULL, 1, NOW());

-- 插入知识库数据
INSERT INTO `crm_knowledge` (`knowledge_id`, `name`, `type`, `file_path`, `file_size`, `mime_type`, `customer_id`, `summary`, `content_text`, `status`, `upload_user_id`, `create_time`) VALUES
(4001, '北京科技产品演示会议记录.docx', 'meeting', '/uploads/2024/01/meeting_bjtech_20240120.docx', 52480, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 1001, 'AI产品演示会议记录，客户对智能分析和自动化功能表现出浓厚兴趣，计划安排技术团队深入测试。', '会议时间：2024年1月20日\n参会人：张明、李华、我方销售团队\n会议内容：产品功能演示、技术架构介绍、商务初步沟通', 1, 1, NOW()),
(4002, '上海金融技术方案.pdf', 'proposal', '/uploads/2024/01/proposal_shfinance_v1.pdf', 1048576, 'application/pdf', 1002, '为上海金融集团定制的AI解决方案，包含数据分析、风控预警、智能报表等模块。', '上海金融集团AI解决方案\n1. 项目背景\n2. 解决方案\n3. 实施计划\n4. 商务报价', 1, 1, NOW()),
(4003, '广州制造现场调研录音.mp3', 'recording', '/uploads/2024/01/visit_gzmfg_20240115.mp3', 15728640, 'audio/mpeg', 1003, '工厂现场调研录音，记录了生产线自动化需求和MES系统升级诉求。', NULL, 1, 1, NOW()),
(4004, '杭州电商项目合同.pdf', 'contract', '/uploads/2024/01/contract_hzecom_2024.pdf', 524288, 'application/pdf', 1005, '与杭州电商平台签订的年度服务合同，合同金额58万元，服务期1年。', '服务合同\n甲方：杭州电商平台\n乙方：我司\n合同金额：580,000元\n服务期限：2024年1月1日至2024年12月31日', 1, 1, NOW());

-- 插入知识库标签
INSERT INTO `crm_knowledge_tag` (`id`, `knowledge_id`, `tag_name`, `create_time`) VALUES
(1, 4001, '产品演示', NOW()),
(2, 4001, 'AI功能', NOW()),
(3, 4002, '技术方案', NOW()),
(4, 4002, '金融行业', NOW()),
(5, 4003, '现场调研', NOW()),
(6, 4004, '正式合同', NOW());

-- 插入任务数据
INSERT INTO `crm_task` (`task_id`, `title`, `description`, `due_date`, `priority`, `status`, `assigned_to`, `customer_id`, `generated_by_ai`, `ai_context`, `completed_time`, `create_user_id`, `create_time`) VALUES
(5001, '准备北京科技技术交流会议', '准备技术交流材料，包括产品架构图、性能测试报告、集成方案等', '2024-01-27 14:00:00', 'high', 'pending', 1, 1001, 1, '基于会议跟进记录自动生成', NULL, 1, NOW()),
(5002, '发送上海金融报价单', '根据技术方案准备正式报价单，需要包含分期付款方案', '2024-01-25 10:00:00', 'high', 'in_progress', 1, 1002, 0, NULL, NULL, 1, NOW()),
(5003, '跟进广州制造需求确认', '确认MES系统升级的具体需求范围和预算情况', '2024-01-28 09:00:00', 'medium', 'pending', 1, 1003, 1, '基于现场调研自动生成', NULL, 1, NOW()),
(5004, '深圳教育初次拜访', '安排初次拜访，了解客户的教育科技需求和预算', '2024-01-30 14:00:00', 'medium', 'pending', 1, 1004, 0, NULL, NULL, 1, NOW()),
(5005, '杭州电商续约沟通', '与客户沟通续约事宜，了解追加采购需求', '2024-02-15 10:00:00', 'low', 'pending', 1, 1005, 1, '基于客户到期时间自动生成', NULL, 1, NOW()),
(5006, '更新CRM系统客户信息', '完善本周新增客户的详细信息', '2024-01-26 18:00:00', 'low', 'completed', 1, NULL, 0, NULL, '2024-01-25 17:30:00', 1, NOW());

-- 插入系统配置
INSERT INTO `crm_system_config` (`config_id`, `config_key`, `config_value`, `config_type`, `description`, `create_time`) VALUES
(1, 'ai_api_url', 'https://api.openai.com/v1', 'ai', 'AI API地址', NOW()),
(2, 'ai_api_key', '', 'ai', 'AI API密钥', NOW()),
(3, 'ai_model', 'gpt-3.5-turbo', 'ai', 'AI模型名称', NOW()),
(4, 'ai_max_tokens', '2000', 'ai', '最大Token数', NOW()),
(5, 'ai_temperature', '0.7', 'ai', 'AI温度参数', NOW()),
(6, 'file_upload_path', '/uploads', 'file', '文件上传路径', NOW()),
(7, 'file_max_size', '52428800', 'file', '文件最大大小(字节)', NOW()),
(8, 'file_allowed_types', 'pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,mp3,mp4,jpg,png', 'file', '允许的文件类型', NOW());

-- 插入客户团队成员
INSERT INTO `crm_customer_team` (`id`, `customer_id`, `user_id`, `role`, `create_user_id`, `create_time`) VALUES
(1, 1001, 1, 'owner', 1, NOW()),
(2, 1002, 1, 'owner', 1, NOW()),
(3, 1003, 1, 'owner', 1, NOW()),
(4, 1004, 1, 'owner', 1, NOW()),
(5, 1005, 1, 'owner', 1, NOW()),
(6, 1006, 1, 'owner', 1, NOW());

-- 插入默认会话
INSERT INTO `crm_chat_session` (`session_id`, `user_id`, `agent_id`, `customer_id`, `title`, `status`, `create_time`) VALUES
(6001, 1, 1, NULL, '新客户录入', 1, NOW()),
(6002, 1, 3, 1001, '北京科技客户分析', 1, NOW());

-- 插入示例聊天消息
INSERT INTO `crm_chat_message` (`message_id`, `session_id`, `role`, `content`, `tokens_used`, `model_name`, `create_time`) VALUES
(7001, 6001, 'user', '我想创建一个新客户', 10, NULL, NOW()),
(7002, 6001, 'assistant', '好的，我来帮您创建新客户。请告诉我以下信息：\n\n1. **公司名称**（必填）\n2. **所属行业**（如：科技互联网、金融服务、制造业等）\n3. **客户等级**（A/B/C）\n4. **主要联系人姓名和电话**\n\n您可以一次性告诉我，也可以逐步提供。', 120, 'gpt-3.5-turbo', NOW()),
(7003, 6002, 'user', '帮我分析一下北京科技这个客户的情况', 15, NULL, NOW()),
(7004, 6002, 'assistant', '好的，我来为您分析**北京科技有限公司**的情况：\n\n## 基本信息\n- **行业**：科技互联网\n- **客户等级**：A级（重点客户）\n- **当前阶段**：谈判中\n- **预计金额**：50万元\n\n## 最近动态\n- 最后联系时间：2024年1月20日\n- 最近进行了产品演示，技术总监张明对AI功能很感兴趣\n\n## 待办事项\n- 下次跟进：2024年1月27日（准备技术交流会议）\n\n## 建议\n1. 重点准备技术架构和性能相关资料\n2. 关注采购流程，提前准备商务报价\n3. 保持与技术决策人的沟通频率\n\n需要我帮您做什么进一步的操作吗？', 280, 'gpt-3.5-turbo', NOW());
