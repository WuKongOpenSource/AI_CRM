-- ============================================
-- AI智能CRM系统 - PostgreSQL 数据库初始化脚本
-- 适用于 ParadeDB / PostgreSQL 17
-- ============================================

-- ============================================
-- 创建更新时间触发器函数
-- ============================================
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- 1. 客户表 (crm_customer)
-- ============================================
DROP TABLE IF EXISTS crm_customer CASCADE;
CREATE TABLE crm_customer (
    customer_id BIGINT NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    industry VARCHAR(100),
    stage VARCHAR(50) NOT NULL DEFAULT 'lead',
    owner_id BIGINT NOT NULL,
    level CHAR(1) DEFAULT 'C',
    source VARCHAR(100),
    address VARCHAR(500),
    website VARCHAR(255),
    quotation DECIMAL(15,2) DEFAULT 0,
    contract_amount DECIMAL(15,2) DEFAULT 0,
    revenue DECIMAL(15,2) DEFAULT 0,
    last_contact_time TIMESTAMP,
    next_follow_time TIMESTAMP,
    remark TEXT,
    status SMALLINT DEFAULT 1,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (customer_id)
);

CREATE INDEX idx_customer_owner_id ON crm_customer (owner_id);
CREATE INDEX idx_customer_stage ON crm_customer (stage);
CREATE INDEX idx_customer_level ON crm_customer (level);
CREATE INDEX idx_customer_create_time ON crm_customer (create_time);

CREATE TRIGGER trg_customer_update_time
    BEFORE UPDATE ON crm_customer
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_customer IS '客户表';
COMMENT ON COLUMN crm_customer.customer_id IS '客户ID';
COMMENT ON COLUMN crm_customer.company_name IS '公司名称';
COMMENT ON COLUMN crm_customer.industry IS '行业';
COMMENT ON COLUMN crm_customer.stage IS '阶段: lead, qualified, proposal, negotiation, closed, lost';
COMMENT ON COLUMN crm_customer.owner_id IS '负责人ID';
COMMENT ON COLUMN crm_customer.level IS '客户等级: A, B, C';
COMMENT ON COLUMN crm_customer.source IS '客户来源';
COMMENT ON COLUMN crm_customer.status IS '状态: 0-禁用, 1-正常';

-- ============================================
-- 2. 客户团队成员表 (crm_customer_team)
-- ============================================
DROP TABLE IF EXISTS crm_customer_team CASCADE;
CREATE TABLE crm_customer_team (
    id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(50) DEFAULT 'member',
    create_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (customer_id, user_id)
);

CREATE INDEX idx_customer_team_user_id ON crm_customer_team (user_id);

COMMENT ON TABLE crm_customer_team IS '客户团队成员表';

-- ============================================
-- 3. 客户标签表 (crm_customer_tag)
-- ============================================
DROP TABLE IF EXISTS crm_customer_tag CASCADE;
CREATE TABLE crm_customer_tag (
    id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    tag_name VARCHAR(100) NOT NULL,
    color VARCHAR(20) DEFAULT '#3b82f6',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_customer_tag_customer_id ON crm_customer_tag (customer_id);
CREATE INDEX idx_customer_tag_tag_name ON crm_customer_tag (tag_name);

COMMENT ON TABLE crm_customer_tag IS '客户标签表';

-- ============================================
-- 4. 联系人表 (crm_contact)
-- ============================================
DROP TABLE IF EXISTS crm_contact CASCADE;
CREATE TABLE crm_contact (
    contact_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    position VARCHAR(100),
    phone VARCHAR(50),
    email VARCHAR(100),
    wechat VARCHAR(100),
    is_primary SMALLINT DEFAULT 0,
    last_contact_time TIMESTAMP,
    notes TEXT,
    status SMALLINT DEFAULT 1,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (contact_id)
);

CREATE INDEX idx_contact_customer_id ON crm_contact (customer_id);
CREATE INDEX idx_contact_name ON crm_contact (name);

CREATE TRIGGER trg_contact_update_time
    BEFORE UPDATE ON crm_contact
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_contact IS '联系人表';
COMMENT ON COLUMN crm_contact.is_primary IS '是否主联系人: 0-否, 1-是';
COMMENT ON COLUMN crm_contact.status IS '状态: 0-禁用, 1-正常';

-- ============================================
-- 5. 联系人标签表 (crm_contact_tag)
-- ============================================
DROP TABLE IF EXISTS crm_contact_tag CASCADE;
CREATE TABLE crm_contact_tag (
    id BIGINT NOT NULL,
    contact_id BIGINT NOT NULL,
    tag_name VARCHAR(100) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_contact_tag_contact_id ON crm_contact_tag (contact_id);

COMMENT ON TABLE crm_contact_tag IS '联系人标签表';

-- ============================================
-- 6. 跟进记录表 (crm_follow_up)
-- ============================================
DROP TABLE IF EXISTS crm_follow_up CASCADE;
CREATE TABLE crm_follow_up (
    follow_up_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    contact_id BIGINT,
    type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    follow_time TIMESTAMP NOT NULL,
    next_follow_time TIMESTAMP,
    create_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follow_up_id)
);

CREATE INDEX idx_follow_up_customer_id ON crm_follow_up (customer_id);
CREATE INDEX idx_follow_up_follow_time ON crm_follow_up (follow_time);

COMMENT ON TABLE crm_follow_up IS '跟进记录表';
COMMENT ON COLUMN crm_follow_up.type IS '类型: call, meeting, email, visit';

-- ============================================
-- 7. 知识库项目表 (crm_knowledge)
-- ============================================
DROP TABLE IF EXISTS crm_knowledge CASCADE;
CREATE TABLE crm_knowledge (
    knowledge_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    file_path VARCHAR(500),
    file_size BIGINT DEFAULT 0,
    mime_type VARCHAR(100),
    customer_id BIGINT,
    summary TEXT,
    content_text TEXT,
    status SMALLINT DEFAULT 1,
    upload_user_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (knowledge_id)
);

CREATE INDEX idx_knowledge_customer_id ON crm_knowledge (customer_id);
CREATE INDEX idx_knowledge_type ON crm_knowledge (type);
CREATE INDEX idx_knowledge_upload_user_id ON crm_knowledge (upload_user_id);

CREATE TRIGGER trg_knowledge_update_time
    BEFORE UPDATE ON crm_knowledge
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_knowledge IS '知识库项目表';
COMMENT ON COLUMN crm_knowledge.type IS '类型: meeting, email, recording, document, proposal, contract';
COMMENT ON COLUMN crm_knowledge.status IS '状态: 0-处理中, 1-正常, 2-处理失败';

-- ParadeDB BM25 全文搜索索引 (中文支持)
-- 注意：需要先安装 pg_search 扩展
-- CREATE EXTENSION IF NOT EXISTS pg_search;
-- CREATE INDEX idx_knowledge_search ON crm_knowledge
-- USING bm25 (name, content_text)
-- WITH (
--   key_field = 'knowledge_id',
--   text_fields = '{
--     "name": {"tokenizer": {"type": "chinese_compatible"}},
--     "content_text": {"tokenizer": {"type": "chinese_compatible"}}
--   }'
-- );

-- ============================================
-- 8. 知识库标签表 (crm_knowledge_tag)
-- ============================================
DROP TABLE IF EXISTS crm_knowledge_tag CASCADE;
CREATE TABLE crm_knowledge_tag (
    id BIGINT NOT NULL,
    knowledge_id BIGINT NOT NULL,
    tag_name VARCHAR(100) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_knowledge_tag_knowledge_id ON crm_knowledge_tag (knowledge_id);

COMMENT ON TABLE crm_knowledge_tag IS '知识库标签表';

-- ============================================
-- 9. 任务表 (crm_task)
-- ============================================
DROP TABLE IF EXISTS crm_task CASCADE;
CREATE TABLE crm_task (
    task_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_date TIMESTAMP,
    priority VARCHAR(20) DEFAULT 'medium',
    status VARCHAR(20) DEFAULT 'pending',
    assigned_to BIGINT,
    customer_id BIGINT,
    generated_by_ai SMALLINT DEFAULT 0,
    ai_context TEXT,
    completed_time TIMESTAMP,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id)
);

CREATE INDEX idx_task_assigned_to ON crm_task (assigned_to);
CREATE INDEX idx_task_customer_id ON crm_task (customer_id);
CREATE INDEX idx_task_status ON crm_task (status);
CREATE INDEX idx_task_due_date ON crm_task (due_date);

CREATE TRIGGER trg_task_update_time
    BEFORE UPDATE ON crm_task
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_task IS '任务表';
COMMENT ON COLUMN crm_task.priority IS '优先级: high, medium, low';
COMMENT ON COLUMN crm_task.status IS '状态: pending, in_progress, completed';
COMMENT ON COLUMN crm_task.generated_by_ai IS '是否AI生成: 0-否, 1-是';

-- ============================================
-- 10. AI智能体表 (crm_ai_agent)
-- ============================================
DROP TABLE IF EXISTS crm_ai_agent CASCADE;
CREATE TABLE crm_ai_agent (
    agent_id BIGINT NOT NULL,
    label VARCHAR(100) NOT NULL,
    icon_name VARCHAR(100),
    prompt TEXT NOT NULL,
    persona TEXT,
    knowledge_base_types VARCHAR(500),
    enabled SMALLINT DEFAULT 1,
    sort_order INT DEFAULT 0,
    category VARCHAR(50) DEFAULT 'custom',
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (agent_id)
);

CREATE INDEX idx_ai_agent_enabled ON crm_ai_agent (enabled);
CREATE INDEX idx_ai_agent_category ON crm_ai_agent (category);

CREATE TRIGGER trg_ai_agent_update_time
    BEFORE UPDATE ON crm_ai_agent
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_ai_agent IS 'AI智能体表';
COMMENT ON COLUMN crm_ai_agent.enabled IS '是否启用: 0-否, 1-是';
COMMENT ON COLUMN crm_ai_agent.category IS '分类: default, custom';

-- ============================================
-- 11. 会话表 (crm_chat_session)
-- ============================================
DROP TABLE IF EXISTS crm_chat_session CASCADE;
CREATE TABLE crm_chat_session (
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    agent_id BIGINT,
    customer_id BIGINT,
    title VARCHAR(255),
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (session_id)
);

CREATE INDEX idx_chat_session_user_id ON crm_chat_session (user_id);
CREATE INDEX idx_chat_session_agent_id ON crm_chat_session (agent_id);
CREATE INDEX idx_chat_session_customer_id ON crm_chat_session (customer_id);

CREATE TRIGGER trg_chat_session_update_time
    BEFORE UPDATE ON crm_chat_session
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_chat_session IS '会话表';
COMMENT ON COLUMN crm_chat_session.status IS '状态: 0-已归档, 1-活跃';

-- ============================================
-- 12. 聊天消息表 (crm_chat_message)
-- ============================================
DROP TABLE IF EXISTS crm_chat_message CASCADE;
CREATE TABLE crm_chat_message (
    message_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    tokens_used INT DEFAULT 0,
    model_name VARCHAR(100),
    function_call TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id)
);

CREATE INDEX idx_chat_message_session_id ON crm_chat_message (session_id);
CREATE INDEX idx_chat_message_create_time ON crm_chat_message (create_time);

COMMENT ON TABLE crm_chat_message IS '聊天消息表';
COMMENT ON COLUMN crm_chat_message.role IS '角色: user, assistant, system';

-- ============================================
-- 13. 消息附件表 (crm_chat_attachment)
-- ============================================
DROP TABLE IF EXISTS crm_chat_attachment CASCADE;
CREATE TABLE crm_chat_attachment (
    id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT DEFAULT 0,
    mime_type VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_chat_attachment_message_id ON crm_chat_attachment (message_id);

COMMENT ON TABLE crm_chat_attachment IS '聊天消息附件表';

-- ============================================
-- 14. 系统配置表 (crm_system_config)
-- ============================================
DROP TABLE IF EXISTS crm_system_config CASCADE;
CREATE TABLE crm_system_config (
    config_id BIGINT NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    config_type VARCHAR(50),
    description VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (config_id),
    UNIQUE (config_key)
);

CREATE TRIGGER trg_system_config_update_time
    BEFORE UPDATE ON crm_system_config
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE crm_system_config IS '系统配置表';

-- ============================================
-- 15. 操作日志表 (crm_operation_log)
-- ============================================
DROP TABLE IF EXISTS crm_operation_log CASCADE;
CREATE TABLE crm_operation_log (
    log_id BIGINT NOT NULL,
    module VARCHAR(50) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    target_id BIGINT,
    target_type VARCHAR(50),
    content TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    create_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id)
);

CREATE INDEX idx_operation_log_module ON crm_operation_log (module);
CREATE INDEX idx_operation_log_target ON crm_operation_log (target_type, target_id);
CREATE INDEX idx_operation_log_create_time ON crm_operation_log (create_time);

COMMENT ON TABLE crm_operation_log IS '操作日志表';

-- ============================================
-- 初始化演示数据
-- ============================================

-- 插入默认AI智能体
INSERT INTO crm_ai_agent (agent_id, label, icon_name, prompt, persona, knowledge_base_types, enabled, sort_order, category, create_user_id, create_time) VALUES
(1, '创建新客户', 'UserPlus', '你是一个CRM助手，帮助用户创建新的客户信息。请引导用户提供公司名称、行业、联系人等必要信息。', '你是一个专业的CRM助手，擅长帮助销售人员高效地录入客户信息。你会主动询问必要的信息，并给出合理的建议。', NULL, 1, 1, 'default', 1, NOW()),
(2, '上传会议记录', 'Upload', '你是一个CRM助手，帮助用户上传和整理会议记录。请协助用户将会议内容关联到正确的客户，并提取关键信息。', '你是一个专业的文档管理助手，擅长整理和归档会议记录。你会帮助用户提取会议要点，并建议后续行动。', '["meeting"]', 1, 2, 'default', 1, NOW()),
(3, '查询客户状态', 'Search', '你是一个CRM助手，帮助用户查询和分析客户状态。你可以根据用户的问题，提供客户的详细信息、跟进历史等。', '你是一个专业的数据分析助手，擅长客户数据分析和状态查询。你会提供清晰的客户概况和跟进建议。', NULL, 1, 3, 'default', 1, NOW()),
(4, '生成跟进任务', 'ListTodo', '你是一个CRM助手，帮助用户基于客户情况生成跟进任务。分析客户状态并建议合适的跟进行动和时间。', '你是一个专业的销售助手，擅长制定跟进计划和任务安排。你会根据客户的阶段和历史，给出针对性的跟进建议。', NULL, 1, 4, 'default', 1, NOW());

-- 插入示例客户数据
INSERT INTO crm_customer (customer_id, company_name, industry, stage, owner_id, level, source, address, website, quotation, contract_amount, revenue, last_contact_time, next_follow_time, remark, status, create_user_id, create_time) VALUES
(1001, '北京科技有限公司', '科技互联网', 'negotiation', 1, 'A', '官网咨询', '北京市海淀区中关村大街1号', 'https://www.bjtech.com', 500000.00, 0, 0, '2024-01-20 10:30:00', '2024-01-27 14:00:00', '大型互联网公司，对AI产品有强烈需求', 1, 1, NOW()),
(1002, '上海金融集团', '金融服务', 'proposal', 1, 'A', '行业展会', '上海市浦东新区陆家嘴金融中心', 'https://www.shfinance.com', 800000.00, 0, 0, '2024-01-18 15:00:00', '2024-01-25 10:00:00', '金融行业头部客户，决策周期较长', 1, 1, NOW()),
(1003, '广州制造业股份公司', '制造业', 'qualified', 1, 'B', '销售拜访', '广州市番禺区工业园区', 'https://www.gzmfg.com', 300000.00, 0, 0, '2024-01-15 09:00:00', '2024-01-28 09:00:00', '传统制造业转型，需要数字化解决方案', 1, 1, NOW()),
(1004, '深圳教育科技公司', '教育培训', 'lead', 1, 'B', '线上推广', '深圳市南山区科技园', 'https://www.szedu.com', 0, 0, 0, NULL, '2024-01-30 14:00:00', '新线索，需要初步沟通了解需求', 1, 1, NOW()),
(1005, '杭州电商平台', '零售电商', 'closed', 1, 'A', '客户转介绍', '杭州市西湖区互联网小镇', 'https://www.hzecom.com', 600000.00, 580000.00, 580000.00, '2024-01-10 16:00:00', NULL, '已成交客户，后续考虑追加销售', 1, 1, NOW()),
(1006, '成都医疗健康公司', '医疗健康', 'lost', 1, 'C', '官网咨询', '成都市高新区天府软件园', 'https://www.cdhealth.com', 200000.00, 0, 0, '2024-01-05 11:00:00', NULL, '预算不足，暂时搁置', 1, 1, NOW());

-- 插入客户标签
INSERT INTO crm_customer_tag (id, customer_id, tag_name, create_time) VALUES
(1, 1001, 'AI需求', NOW()),
(2, 1001, '大客户', NOW()),
(3, 1002, '金融行业', NOW()),
(4, 1002, '决策周期长', NOW()),
(5, 1003, '数字化转型', NOW()),
(6, 1005, 'VIP客户', NOW()),
(7, 1005, '可追加销售', NOW());

-- 插入联系人数据
INSERT INTO crm_contact (contact_id, customer_id, name, position, phone, email, wechat, is_primary, last_contact_time, notes, status, create_user_id, create_time) VALUES
(2001, 1001, '张明', '技术总监', '13800138001', 'zhangming@bjtech.com', 'zhangming_bj', 1, '2024-01-20 10:30:00', '技术决策人，对产品功能很关注', 1, 1, NOW()),
(2002, 1001, '李华', '采购经理', '13800138002', 'lihua@bjtech.com', 'lihua_bj', 0, '2024-01-19 14:00:00', '负责采购流程', 1, 1, NOW()),
(2003, 1002, '王芳', '副总裁', '13900139001', 'wangfang@shfinance.com', 'wangfang_sh', 1, '2024-01-18 15:00:00', '最终决策人', 1, 1, NOW()),
(2004, 1002, '陈强', 'IT总监', '13900139002', 'chenqiang@shfinance.com', 'chenqiang_sh', 0, '2024-01-17 10:00:00', '技术评估负责人', 1, 1, NOW()),
(2005, 1003, '刘伟', '厂长', '13700137001', 'liuwei@gzmfg.com', 'liuwei_gz', 1, '2024-01-15 09:00:00', '生产和运营负责人', 1, 1, NOW()),
(2006, 1004, '赵敏', 'CEO', '13600136001', 'zhaomin@szedu.com', 'zhaomin_sz', 1, NULL, '创始人，对教育科技很感兴趣', 1, 1, NOW()),
(2007, 1005, '孙丽', '运营总监', '13500135001', 'sunli@hzecom.com', 'sunli_hz', 1, '2024-01-10 16:00:00', '项目对接人', 1, 1, NOW());

-- 插入联系人标签
INSERT INTO crm_contact_tag (id, contact_id, tag_name, create_time) VALUES
(1, 2001, '技术决策人', NOW()),
(2, 2003, '高管', NOW()),
(3, 2003, '最终决策人', NOW()),
(4, 2006, '创始人', NOW());

-- 插入跟进记录
INSERT INTO crm_follow_up (follow_up_id, customer_id, contact_id, type, content, follow_time, next_follow_time, create_user_id, create_time) VALUES
(3001, 1001, 2001, 'meeting', '与张明总监进行了产品演示，对方对AI功能非常感兴趣，计划下周安排技术团队深入沟通。', '2024-01-20 10:30:00', '2024-01-27 14:00:00', 1, NOW()),
(3002, 1001, 2002, 'call', '与李华确认了采购流程，需要准备商务报价单。', '2024-01-19 14:00:00', '2024-01-22 10:00:00', 1, NOW()),
(3003, 1002, 2003, 'meeting', '高层会议，王芳副总对方案整体认可，但需要等待预算审批。', '2024-01-18 15:00:00', '2024-01-25 10:00:00', 1, NOW()),
(3004, 1002, 2004, 'email', '发送了技术方案文档，等待技术评估反馈。', '2024-01-17 10:00:00', '2024-01-20 10:00:00', 1, NOW()),
(3005, 1003, 2005, 'visit', '现场拜访，了解了工厂的生产流程和痛点，初步确认合作意向。', '2024-01-15 09:00:00', '2024-01-28 09:00:00', 1, NOW()),
(3006, 1005, 2007, 'call', '项目验收沟通，客户对交付成果满意，考虑后续追加采购。', '2024-01-10 16:00:00', NULL, 1, NOW());

-- 插入知识库数据
INSERT INTO crm_knowledge (knowledge_id, name, type, file_path, file_size, mime_type, customer_id, summary, content_text, status, upload_user_id, create_time) VALUES
(4001, '北京科技产品演示会议记录.docx', 'meeting', '/uploads/2024/01/meeting_bjtech_20240120.docx', 52480, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 1001, 'AI产品演示会议记录，客户对智能分析和自动化功能表现出浓厚兴趣，计划安排技术团队深入测试。', '会议时间：2024年1月20日
参会人：张明、李华、我方销售团队
会议内容：产品功能演示、技术架构介绍、商务初步沟通', 1, 1, NOW()),
(4002, '上海金融技术方案.pdf', 'proposal', '/uploads/2024/01/proposal_shfinance_v1.pdf', 1048576, 'application/pdf', 1002, '为上海金融集团定制的AI解决方案，包含数据分析、风控预警、智能报表等模块。', '上海金融集团AI解决方案
1. 项目背景
2. 解决方案
3. 实施计划
4. 商务报价', 1, 1, NOW()),
(4003, '广州制造现场调研录音.mp3', 'recording', '/uploads/2024/01/visit_gzmfg_20240115.mp3', 15728640, 'audio/mpeg', 1003, '工厂现场调研录音，记录了生产线自动化需求和MES系统升级诉求。', NULL, 1, 1, NOW()),
(4004, '杭州电商项目合同.pdf', 'contract', '/uploads/2024/01/contract_hzecom_2024.pdf', 524288, 'application/pdf', 1005, '与杭州电商平台签订的年度服务合同，合同金额58万元，服务期1年。', '服务合同
甲方：杭州电商平台
乙方：我司
合同金额：580,000元
服务期限：2024年1月1日至2024年12月31日', 1, 1, NOW());

-- 插入知识库标签
INSERT INTO crm_knowledge_tag (id, knowledge_id, tag_name, create_time) VALUES
(1, 4001, '产品演示', NOW()),
(2, 4001, 'AI功能', NOW()),
(3, 4002, '技术方案', NOW()),
(4, 4002, '金融行业', NOW()),
(5, 4003, '现场调研', NOW()),
(6, 4004, '正式合同', NOW());

-- 插入任务数据
INSERT INTO crm_task (task_id, title, description, due_date, priority, status, assigned_to, customer_id, generated_by_ai, ai_context, completed_time, create_user_id, create_time) VALUES
(5001, '准备北京科技技术交流会议', '准备技术交流材料，包括产品架构图、性能测试报告、集成方案等', '2024-01-27 14:00:00', 'high', 'pending', 1, 1001, 1, '基于会议跟进记录自动生成', NULL, 1, NOW()),
(5002, '发送上海金融报价单', '根据技术方案准备正式报价单，需要包含分期付款方案', '2024-01-25 10:00:00', 'high', 'in_progress', 1, 1002, 0, NULL, NULL, 1, NOW()),
(5003, '跟进广州制造需求确认', '确认MES系统升级的具体需求范围和预算情况', '2024-01-28 09:00:00', 'medium', 'pending', 1, 1003, 1, '基于现场调研自动生成', NULL, 1, NOW()),
(5004, '深圳教育初次拜访', '安排初次拜访，了解客户的教育科技需求和预算', '2024-01-30 14:00:00', 'medium', 'pending', 1, 1004, 0, NULL, NULL, 1, NOW()),
(5005, '杭州电商续约沟通', '与客户沟通续约事宜，了解追加采购需求', '2024-02-15 10:00:00', 'low', 'pending', 1, 1005, 1, '基于客户到期时间自动生成', NULL, 1, NOW()),
(5006, '更新CRM系统客户信息', '完善本周新增客户的详细信息', '2024-01-26 18:00:00', 'low', 'completed', 1, NULL, 0, NULL, '2024-01-25 17:30:00', 1, NOW());

-- 插入系统配置
INSERT INTO crm_system_config (config_id, config_key, config_value, config_type, description, create_time) VALUES
(1, 'ai_api_url', 'https://api.openai.com/v1', 'ai', 'AI API地址', NOW()),
(2, 'ai_api_key', '', 'ai', 'AI API密钥', NOW()),
(3, 'ai_model', 'gpt-3.5-turbo', 'ai', 'AI模型名称', NOW()),
(4, 'ai_max_tokens', '2000', 'ai', '最大Token数', NOW()),
(5, 'ai_temperature', '0.7', 'ai', 'AI温度参数', NOW()),
(6, 'file_upload_path', '/uploads', 'file', '文件上传路径', NOW()),
(7, 'file_max_size', '52428800', 'file', '文件最大大小(字节)', NOW()),
(8, 'file_allowed_types', 'pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,mp3,mp4,jpg,png', 'file', '允许的文件类型', NOW()),
-- WeKnora 知识库服务配置
(101, 'weknora_enabled', 'false', 'weknora', 'WeKnora是否启用', NOW()),
(102, 'weknora_base_url', '', 'weknora', 'WeKnora API地址', NOW()),
(103, 'weknora_api_key', '', 'weknora', 'WeKnora API密钥', NOW()),
(104, 'weknora_knowledge_base_id', '', 'weknora', '默认知识库ID', NOW()),
(105, 'weknora_match_count', '5', 'weknora', '搜索最大结果数', NOW()),
(106, 'weknora_vector_threshold', '0.5', 'weknora', '向量相似度阈值', NOW()),
(107, 'weknora_auto_rag_enabled', 'true', 'weknora', '是否启用自动RAG', NOW());

-- 插入客户团队成员
INSERT INTO crm_customer_team (id, customer_id, user_id, role, create_user_id, create_time) VALUES
(1, 1001, 1, 'owner', 1, NOW()),
(2, 1002, 1, 'owner', 1, NOW()),
(3, 1003, 1, 'owner', 1, NOW()),
(4, 1004, 1, 'owner', 1, NOW()),
(5, 1005, 1, 'owner', 1, NOW()),
(6, 1006, 1, 'owner', 1, NOW());

-- 插入默认会话
INSERT INTO crm_chat_session (session_id, user_id, agent_id, customer_id, title, status, create_time) VALUES
(6001, 1, 1, NULL, '新客户录入', 1, NOW()),
(6002, 1, 3, 1001, '北京科技客户分析', 1, NOW());

-- 插入示例聊天消息
INSERT INTO crm_chat_message (message_id, session_id, role, content, tokens_used, model_name, create_time) VALUES
(7001, 6001, 'user', '我想创建一个新客户', 10, NULL, NOW()),
(7002, 6001, 'assistant', '好的，我来帮您创建新客户。请告诉我以下信息：

1. **公司名称**（必填）
2. **所属行业**（如：科技互联网、金融服务、制造业等）
3. **客户等级**（A/B/C）
4. **主要联系人姓名和电话**

您可以一次性告诉我，也可以逐步提供。', 120, 'gpt-3.5-turbo', NOW()),
(7003, 6002, 'user', '帮我分析一下北京科技这个客户的情况', 15, NULL, NOW()),
(7004, 6002, 'assistant', '好的，我来为您分析**北京科技有限公司**的情况：

## 基本信息
- **行业**：科技互联网
- **客户等级**：A级（重点客户）
- **当前阶段**：谈判中
- **预计金额**：50万元

## 最近动态
- 最后联系时间：2024年1月20日
- 最近进行了产品演示，技术总监张明对AI功能很感兴趣

## 待办事项
- 下次跟进：2024年1月27日（准备技术交流会议）

## 建议
1. 重点准备技术架构和性能相关资料
2. 关注采购流程，提前准备商务报价
3. 保持与技术决策人的沟通频率

需要我帮您做什么进一步的操作吗？', 280, 'gpt-3.5-turbo', NOW());
