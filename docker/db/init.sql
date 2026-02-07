
CREATE DATABASE wk_ai_crm;


\c wk_ai_crm

-- ----------------------------
-- Table structure for crm_ai_agent
-- ----------------------------
DROP TABLE IF EXISTS "public"."crm_ai_agent";
CREATE TABLE "public"."crm_ai_agent" (
  "agent_id" int8 NOT NULL,
  "label" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "icon_name" varchar(100) COLLATE "pg_catalog"."default",
  "prompt" text COLLATE "pg_catalog"."default" NOT NULL,
  "persona" text COLLATE "pg_catalog"."default",
  "knowledge_base_types" varchar(500) COLLATE "pg_catalog"."default",
  "enabled" int2 DEFAULT 1,
  "sort_order" int4 DEFAULT 0,
  "category" varchar(50) COLLATE "pg_catalog"."default",
  "create_user_id" int8,
  "update_user_id" int8,
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."crm_ai_agent"."agent_id" IS '智能体ID';
COMMENT ON COLUMN "public"."crm_ai_agent"."label" IS '显示名称';
COMMENT ON COLUMN "public"."crm_ai_agent"."icon_name" IS '图标名称';
COMMENT ON COLUMN "public"."crm_ai_agent"."prompt" IS '系统提示词';
COMMENT ON COLUMN "public"."crm_ai_agent"."persona" IS '角色人设';
COMMENT ON COLUMN "public"."crm_ai_agent"."knowledge_base_types" IS '知识库类型(JSON数组)';
COMMENT ON COLUMN "public"."crm_ai_agent"."enabled" IS '是否启用: 0-否, 1-是';
COMMENT ON COLUMN "public"."crm_ai_agent"."sort_order" IS '排序';
COMMENT ON COLUMN "public"."crm_ai_agent"."category" IS '分类: default, custom';
COMMENT ON COLUMN "public"."crm_ai_agent"."create_user_id" IS '创建人ID';
COMMENT ON COLUMN "public"."crm_ai_agent"."update_user_id" IS '修改人ID';
COMMENT ON COLUMN "public"."crm_ai_agent"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."crm_ai_agent"."update_time" IS '修改时间';
COMMENT ON TABLE "public"."crm_ai_agent" IS 'AI智能体表';

-- ----------------------------
-- Records of crm_ai_agent
-- ----------------------------
INSERT INTO "public"."crm_ai_agent" VALUES (1, '创建新客户', 'UserPlus', '你是一个CRM助手，帮助用户创建新的客户信息。请引导用户提供公司名称、行业、联系人等必要信息。', '你是一个专业的CRM助手，擅长帮助销售人员高效地录入客户信息。你会主动询问必要的信息，并给出合理的建议。', NULL, 1, 1, 'default', 1, NULL, '2026-01-27 17:40:23', '2026-01-27 17:40:23');
INSERT INTO "public"."crm_ai_agent" VALUES (2, '上传会议记录', 'Upload', '你是一个CRM助手，帮助用户上传和整理会议记录。请协助用户将会议内容关联到正确的客户，并提取关键信息。', '你是一个专业的文档管理助手，擅长整理和归档会议记录。你会帮助用户提取会议要点，并建议后续行动。', '["meeting"]', 1, 2, 'default', 1, NULL, '2026-01-27 17:40:23', '2026-01-27 17:40:23');
INSERT INTO "public"."crm_ai_agent" VALUES (3, '查询客户状态', 'Search', '你是一个CRM助手，帮助用户查询和分析客户状态。你可以根据用户的问题，提供客户的详细信息、跟进历史等。', '你是一个专业的数据分析助手，擅长客户数据分析和状态查询。你会提供清晰的客户概况和跟进建议。', NULL, 1, 3, 'default', 1, NULL, '2026-01-27 17:40:23', '2026-01-27 17:40:23');
INSERT INTO "public"."crm_ai_agent" VALUES (4, '生成跟进任务', 'ListTodo', '你是一个CRM助手，帮助用户基于客户情况生成跟进任务。分析客户状态并建议合适的跟进行动和时间。', '你是一个专业的销售助手，擅长制定跟进计划和任务安排。你会根据客户的阶段和历史，给出针对性的跟进建议。', NULL, 1, 4, 'default', 1, NULL, '2026-01-27 17:40:23', '2026-01-27 17:40:23');

-- ----------------------------
-- Table structure for crm_chat_attachment
-- ----------------------------
DROP TABLE IF EXISTS "public"."crm_chat_attachment";
CREATE TABLE "public"."crm_chat_attachment" (
  "id" int8 NOT NULL,
  "message_id" int8 NOT NULL,
  "file_name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "file_path" varchar(500) COLLATE "pg_catalog"."default" NOT NULL,
  "file_size" int8,
  "mime_type" varchar(100) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."crm_chat_attachment"."id" IS '主键';
COMMENT ON COLUMN "public"."crm_chat_attachment"."message_id" IS '消息ID';
COMMENT ON COLUMN "public"."crm_chat_attachment"."file_name" IS '文件名';
COMMENT ON COLUMN "public"."crm_chat_attachment"."file_path" IS '文件路径';
COMMENT ON COLUMN "public"."crm_chat_attachment"."file_size" IS '文件大小';
COMMENT ON COLUMN "public"."crm_chat_attachment"."mime_type" IS 'MIME类型';
COMMENT ON COLUMN "public"."crm_chat_attachment"."create_time" IS '创建时间';
COMMENT ON TABLE "public"."crm_chat_attachment" IS '聊天消息附件表';

-- ----------------------------
-- Records of crm_chat_attachment
-- ----------------------------

-- ----------------------------
-- Table structure for crm_chat_message
-- ----------------------------
DROP TABLE IF EXISTS "public"."crm_chat_message";
CREATE TABLE "public"."crm_chat_message" (
  "message_id" int8 NOT NULL,
  "session_id" int8 NOT NULL,
  "role" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "content" text COLLATE "pg_catalog"."default" NOT NULL,
  "tokens_used" int4 DEFAULT 0,
  "model_name" varchar(100) COLLATE "pg_catalog"."default",
  "function_call" text COLLATE "pg_catalog"."default",
  "create_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."crm_chat_message"."message_id" IS '消息ID';
COMMENT ON COLUMN "public"."crm_chat_message"."session_id" IS '会话ID';
COMMENT ON COLUMN "public"."crm_chat_message"."role" IS '角色: user, assistant, system';
COMMENT ON COLUMN "public"."crm_chat_message"."content" IS '消息内容';
COMMENT ON COLUMN "public"."crm_chat_message"."tokens_used" IS '使用token数';
COMMENT ON COLUMN "public"."crm_chat_message"."model_name" IS '使用的模型';
COMMENT ON COLUMN "public"."crm_chat_message"."function_call" IS '函数调用(JSON)';
COMMENT ON COLUMN "public"."crm_chat_message"."create_time" IS '创建时间';
COMMENT ON TABLE "public"."crm_chat_message" IS '聊天消息表';

-- ----------------------------
-- Records of crm_chat_message
-- ----------------------------

-- ----------------------------
-- Table structure for crm_chat_session
-- ----------------------------
DROP TABLE IF EXISTS "public"."crm_chat_session";
CREATE TABLE "public"."crm_chat_session" (
  "session_id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "agent_id" int8,
  "customer_id" int8,
  "title" varchar(255) COLLATE "pg_catalog"."default",
  "status" int2 DEFAULT 1,
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."crm_chat_session"."session_id" IS '会话ID';
COMMENT ON COLUMN "public"."crm_chat_session"."user_id" IS '用户ID';
COMMENT ON COLUMN "public"."crm_chat_session"."agent_id" IS '智能体ID';
COMMENT ON COLUMN "public"."crm_chat_session"."customer_id" IS '关联客户ID';
COMMENT ON COLUMN "public"."crm_chat_session"."title" IS '会话标题';
COMMENT ON COLUMN "public"."crm_chat_session"."status" IS '状态: 0-已归档, 1-活跃';
COMMENT ON COLUMN "public"."crm_chat_session"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."crm_chat_session"."update_time" IS '修改时间';
COMMENT ON TABLE "public"."crm_chat_session" IS '会话表';

-- ----------------------------
-- Records of crm_chat_session
-- ----------------------------

-- ----------------------------
-- Table structure for crm_contact
-- ----------------------------
DROP TABLE IF EXISTS "public"."crm_contact";
CREATE TABLE "public"."crm_contact" (
  "contact_id" int8 NOT NULL,
  "customer_id" int8 NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "position" varchar(100) COLLATE "pg_catalog"."default",
  "phone" varchar(50) COLLATE "pg_catalog"."default",
  "email" varchar(100) COLLATE "pg_catalog"."default",
  "wechat" varchar(100) COLLATE "pg_catalog"."default",
  "is_primary" int2,
  "last_contact_time" timestamp(6),
  "notes" text COLLATE "pg_catalog"."default",
  "status" int2 DEFAULT 1,
  "create_user_id" int8,
  "update_user_id" int8,
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."crm_contact"."contact_id" IS '联系人ID';
COMMENT ON COLUMN "public"."crm_contact"."customer_id" IS '客户ID';
COMMENT ON COLUMN "public"."crm_contact"."name" IS '姓名';
COMMENT ON COLUMN "public"."crm_contact"."position" IS '职位';
COMMENT ON COLUMN "public"."crm_contact"."phone" IS '电话';
COMMENT ON COLUMN "public"."crm_contact"."email" IS '邮箱';
COMMENT ON COLUMN "public"."crm_contact"."wechat" IS '微信';
COMMENT ON COLUMN "public"."crm_contact"."is_primary" IS '是否主联系人: 0-否, 1-是';
COMMENT ON COLUMN "public"."crm_contact"."last_contact_time" IS '最后联系时间';
COMMENT ON COLUMN "public"."crm_contact"."notes" IS '备注';
COMMENT ON COLUMN "public"."crm_contact"."status" IS '状态: 0-禁用, 1-正常';
COMMENT ON COLUMN "public"."crm_contact"."create_user_id" IS '创建人ID';
COMMENT ON COLUMN "public"."crm_contact"."update_user_id" IS '修改人ID';
COMMENT ON COLUMN "public"."crm_contact"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."crm_contact"."update_time" IS '修改时间';
COMMENT ON TABLE "public"."crm_contact" IS '联系人表';

-- ----------------------------
-- Records of crm_contact
-- ----------------------------

-- ----------------------------
-- Table structure for crm_contact_tag
-- ----------------------------
DROP TABLE IF EXISTS "public"."crm_contact_tag";
CREATE TABLE "public"."crm_contact_tag" (
  "id" int8 NOT NULL,
  "contact_id" int8 NOT NULL,
  "tag_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "create_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."crm_contact_tag"."id" IS '主键';
COMMENT ON COLUMN "public"."crm_contact_tag"."contact_id" IS '联系人ID';
COMMENT ON COLUMN "public"."crm_contact_tag"."tag_name" IS '标签名称';
COMMENT ON COLUMN "public"."crm_contact_tag"."create_time" IS '创建时间';
COMMENT ON TABLE "public"."crm_contact_tag" IS '联系人标签表';

-- ----------------------------
-- Records of crm_contact_tag
-- ----------------------------

-- ----------------------------
-- Table structure for crm_custom_field
-- ----------------------------
DROP TABLE IF EXISTS "public"."crm_custom_field";
CREATE TABLE "public"."crm_custom_field" (
  "field_id" int8 NOT NULL,
  "entity_type" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "field_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "field_label" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "field_type" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "column_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "column_type" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "default_value" varchar(500) COLLATE "pg_catalog"."default",
  "placeholder" varchar(200) COLLATE "pg_catalog"."default",
  "is_required" int2 DEFAULT 0,
  "is_searchable" int2 DEFAULT 0,
  "is_show_in_list" int2 DEFAULT 1,
  "options" text COLLATE "pg_catalog"."default",
  "validation_rules" text COLLATE "pg_catalog"."default",
  "sort_order" int4 DEFAULT 0,
  "status" int2 DEFAULT 1,
  "create_user_id" int8,
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."crm_custom_field"."field_id" IS '字段ID';
COMMENT ON COLUMN "public"."crm_custom_field"."entity_type" IS '实体类型: customer, contact';
COMMENT ON COLUMN "public"."crm_custom_field"."field_name" IS '字段标识(英文，用于代码)';
COMMENT ON COLUMN "public"."crm_custom_field"."field_label" IS '字段显示标签(中文)';
COMMENT ON COLUMN "public"."crm_custom_field"."field_type" IS '字段类型: text, textarea, number, date, datetime, select, multiselect, checkbox';
COMMENT ON COLUMN "public"."crm_custom_field"."column_name" IS '实际数据库列名';
COMMENT ON COLUMN "public"."crm_custom_field"."column_type" IS '数据库列类型';
COMMENT ON COLUMN "public"."crm_custom_field"."default_value" IS '默认值';
COMMENT ON COLUMN "public"."crm_custom_field"."placeholder" IS '输入框占位提示';
COMMENT ON COLUMN "public"."crm_custom_field"."is_required" IS '是否必填: 0否 1是';
COMMENT ON COLUMN "public"."crm_custom_field"."is_searchable" IS '是否可搜索: 0否 1是';
COMMENT ON COLUMN "public"."crm_custom_field"."is_show_in_list" IS '是否在列表显示: 0否 1是';
COMMENT ON COLUMN "public"."crm_custom_field"."options" IS '选项列表(JSON数组): [{"value":"v1","label":"选项1"}]';
COMMENT ON COLUMN "public"."crm_custom_field"."validation_rules" IS '验证规则(JSON): {"min":0,"max":100,"pattern":""}';
COMMENT ON COLUMN "public"."crm_custom_field"."sort_order" IS '排序序号';
COMMENT ON COLUMN "public"."crm_custom_field"."status" IS '状态: 0禁用 1启用';
COMMENT ON COLUMN "public"."crm_custom_field"."create_user_id" IS '创建人';
COMMENT ON COLUMN "public"."crm_custom_field"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."crm_custom_field"."update_time" IS '更新时间';
COMMENT ON TABLE "public"."crm_custom_field" IS '自定义字段定义表';

-- ----------------------------
-- Records of crm_custom_field
-- ----------------------------

-- ----------------------------
-- Table structure for crm_customer
-- ----------------------------
DROP TABLE IF EXISTS "public"."crm_customer";
CREATE TABLE "public"."crm_customer" (
  "customer_id" int8 NOT NULL,
  "company_name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "industry" varchar(100) COLLATE "pg_catalog"."default",
  "stage" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "owner_id" int8 NOT NULL,
  "level" char(1) COLLATE "pg_catalog"."default",
  "source" varchar(100) COLLATE "pg_catalog"."default",
  "address" varchar(500) COLLATE "pg_catalog"."default",
  "website" varchar(255) COLLATE "pg_catalog"."default",
  "quotation" numeric(15,2) DEFAULT 0.00,
  "contract_amount" numeric(15,2) DEFAULT 0.00,
  "revenue" numeric(15,2) DEFAULT 0.00,
  "last_contact_time" timestamp(6),
  "next_follow_time" timestamp(6),
  "remark" text COLLATE "pg_catalog"."default",
  "status" int2 DEFAULT 1,
  "create_user_id" int8,
  "update_user_id" int8,
  "create_time" timestamp(6),
  "update_time" timestamp(6),
  "cf_cccc" varchar(500) COLLATE "pg_catalog"."default",
  "cf_contract__commit" text COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."crm_customer"."customer_id" IS '客户ID';
COMMENT ON COLUMN "public"."crm_customer"."company_name" IS '公司名称';
COMMENT ON COLUMN "public"."crm_customer"."industry" IS '行业';
COMMENT ON COLUMN "public"."crm_customer"."stage" IS '阶段: lead, qualified, proposal, negotiation, closed, lost';
COMMENT ON COLUMN "public"."crm_customer"."owner_id" IS '负责人ID';
COMMENT ON COLUMN "public"."crm_customer"."level" IS '客户等级: A, B, C';
COMMENT ON COLUMN "public"."crm_customer"."source" IS '客户来源';
COMMENT ON COLUMN "public"."crm_customer"."address" IS '地址';
COMMENT ON COLUMN "public"."crm_customer"."website" IS '网站';
COMMENT ON COLUMN "public"."crm_customer"."quotation" IS '报价金额';
COMMENT ON COLUMN "public"."crm_customer"."contract_amount" IS '合同金额';
COMMENT ON COLUMN "public"."crm_customer"."revenue" IS '收入金额';
COMMENT ON COLUMN "public"."crm_customer"."last_contact_time" IS '最后联系时间';
COMMENT ON COLUMN "public"."crm_customer"."next_follow_time" IS '下次跟进时间';
COMMENT ON COLUMN "public"."crm_customer"."remark" IS '备注';
COMMENT ON COLUMN "public"."crm_customer"."status" IS '状态: 0-禁用, 1-正常';
COMMENT ON COLUMN "public"."crm_customer"."create_user_id" IS '创建人ID';
COMMENT ON COLUMN "public"."crm_customer"."update_user_id" IS '修改人ID';
COMMENT ON COLUMN "public"."crm_customer"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."crm_customer"."update_time" IS '修改时间';
COMMENT ON COLUMN "public"."crm_customer"."cf_cccc" IS 'cccc';
COMMENT ON COLUMN "public"."crm_customer"."cf_contract__commit" IS '合同说明';
COMMENT ON TABLE "public"."crm_customer" IS '客户表';

-- ----------------------------
-- Records of crm_customer
-- ----------------------------

-- ----------------------------
-- Table structure for crm_customer_tag
-- ----------------------------
DROP TABLE IF EXISTS "public"."crm_customer_tag";
CREATE TABLE "public"."crm_customer_tag" (
  "id" int8 NOT NULL,
  "customer_id" int8 NOT NULL,
  "tag_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "create_time" timestamp(6),
  "color" varchar(20) COLLATE "pg_catalog"."default" DEFAULT '#3b82f6'::character varying
)
;
COMMENT ON COLUMN "public"."crm_customer_tag"."id" IS '主键';
COMMENT ON COLUMN "public"."crm_customer_tag"."customer_id" IS '客户ID';
COMMENT ON COLUMN "public"."crm_customer_tag"."tag_name" IS '标签名称';
COMMENT ON COLUMN "public"."crm_customer_tag"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."crm_customer_tag"."color" IS '标签颜色';
COMMENT ON TABLE "public"."crm_customer_tag" IS '客户标签表';

-- ----------------------------
-- Records of crm_customer_tag
-- ----------------------------

-- ----------------------------
-- Table structure for crm_customer_team
-- ----------------------------
DROP TABLE IF EXISTS "public"."crm_customer_team";
CREATE TABLE "public"."crm_customer_team" (
  "id" int8 NOT NULL,
  "customer_id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "role" varchar(50) COLLATE "pg_catalog"."default",
  "create_user_id" int8,
  "create_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."crm_customer_team"."id" IS '主键';
COMMENT ON COLUMN "public"."crm_customer_team"."customer_id" IS '客户ID';
COMMENT ON COLUMN "public"."crm_customer_team"."user_id" IS '用户ID';
COMMENT ON COLUMN "public"."crm_customer_team"."role" IS '角色: owner, member';
COMMENT ON COLUMN "public"."crm_customer_team"."create_user_id" IS '创建人ID';
COMMENT ON COLUMN "public"."crm_customer_team"."create_time" IS '创建时间';
COMMENT ON TABLE "public"."crm_customer_team" IS '客户团队成员表';

-- ----------------------------
-- Records of crm_customer_team
-- ----------------------------

-- ----------------------------
-- Table structure for crm_follow_up
-- ----------------------------
DROP TABLE IF EXISTS "public"."crm_follow_up";
CREATE TABLE "public"."crm_follow_up" (
  "follow_up_id" int8 NOT NULL,
  "customer_id" int8 NOT NULL,
  "contact_id" int8,
  "type" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "content" text COLLATE "pg_catalog"."default" NOT NULL,
  "follow_time" timestamp(6) NOT NULL,
  "next_follow_time" timestamp(6),
  "create_user_id" int8,
  "create_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."crm_follow_up"."follow_up_id" IS '跟进ID';
COMMENT ON COLUMN "public"."crm_follow_up"."customer_id" IS '客户ID';
COMMENT ON COLUMN "public"."crm_follow_up"."contact_id" IS '联系人ID';
COMMENT ON COLUMN "public"."crm_follow_up"."type" IS '类型: call, meeting, email, visit';
COMMENT ON COLUMN "public"."crm_follow_up"."content" IS '跟进内容';
COMMENT ON COLUMN "public"."crm_follow_up"."follow_time" IS '跟进时间';
COMMENT ON COLUMN "public"."crm_follow_up"."next_follow_time" IS '下次跟进时间';
COMMENT ON COLUMN "public"."crm_follow_up"."create_user_id" IS '创建人ID';
COMMENT ON COLUMN "public"."crm_follow_up"."create_time" IS '创建时间';
COMMENT ON TABLE "public"."crm_follow_up" IS '跟进记录表';

-- ----------------------------
-- Records of crm_follow_up
-- ----------------------------

-- ----------------------------
-- Table structure for crm_knowledge
-- ----------------------------
DROP TABLE IF EXISTS "public"."crm_knowledge";
CREATE TABLE "public"."crm_knowledge" (
  "knowledge_id" int8 NOT NULL,
  "name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "type" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "file_path" varchar(500) COLLATE "pg_catalog"."default",
  "file_size" int8,
  "mime_type" varchar(100) COLLATE "pg_catalog"."default",
  "customer_id" int8,
  "summary" text COLLATE "pg_catalog"."default",
  "content_text" text COLLATE "pg_catalog"."default",
  "status" int2 DEFAULT 1,
  "upload_user_id" int8 NOT NULL,
  "create_time" timestamp(6),
  "update_time" timestamp(6),
  "weknora_knowledge_id" varchar(100) COLLATE "pg_catalog"."default",
  "weknora_parse_status" varchar(20) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."crm_knowledge"."knowledge_id" IS '知识ID';
COMMENT ON COLUMN "public"."crm_knowledge"."name" IS '名称';
COMMENT ON COLUMN "public"."crm_knowledge"."type" IS '类型: meeting, email, recording, document, proposal, contract';
COMMENT ON COLUMN "public"."crm_knowledge"."file_path" IS '文件路径';
COMMENT ON COLUMN "public"."crm_knowledge"."file_size" IS '文件大小(字节)';
COMMENT ON COLUMN "public"."crm_knowledge"."mime_type" IS 'MIME类型';
COMMENT ON COLUMN "public"."crm_knowledge"."customer_id" IS '关联客户ID';
COMMENT ON COLUMN "public"."crm_knowledge"."summary" IS 'AI摘要';
COMMENT ON COLUMN "public"."crm_knowledge"."content_text" IS '文本内容(用于搜索)';
COMMENT ON COLUMN "public"."crm_knowledge"."status" IS '状态: 0-处理中, 1-正常, 2-处理失败';
COMMENT ON COLUMN "public"."crm_knowledge"."upload_user_id" IS '上传人ID';
COMMENT ON COLUMN "public"."crm_knowledge"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."crm_knowledge"."update_time" IS '修改时间';
COMMENT ON COLUMN "public"."crm_knowledge"."weknora_knowledge_id" IS 'WeKnora中的知识ID';
COMMENT ON COLUMN "public"."crm_knowledge"."weknora_parse_status" IS 'WeKnora解析状态';
COMMENT ON TABLE "public"."crm_knowledge" IS '知识库项目表';

-- ----------------------------
-- Records of crm_knowledge
-- ----------------------------

-- ----------------------------
-- Table structure for crm_knowledge_tag
-- ----------------------------
DROP TABLE IF EXISTS "public"."crm_knowledge_tag";
CREATE TABLE "public"."crm_knowledge_tag" (
  "id" int8 NOT NULL,
  "knowledge_id" int8 NOT NULL,
  "tag_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "create_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."crm_knowledge_tag"."id" IS '主键';
COMMENT ON COLUMN "public"."crm_knowledge_tag"."knowledge_id" IS '知识ID';
COMMENT ON COLUMN "public"."crm_knowledge_tag"."tag_name" IS '标签名称';
COMMENT ON COLUMN "public"."crm_knowledge_tag"."create_time" IS '创建时间';
COMMENT ON TABLE "public"."crm_knowledge_tag" IS '知识库标签表';

-- ----------------------------
-- Records of crm_knowledge_tag
-- ----------------------------

-- ----------------------------
-- Table structure for crm_operation_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."crm_operation_log";
CREATE TABLE "public"."crm_operation_log" (
  "log_id" int8 NOT NULL,
  "module" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "operation" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "target_id" int8,
  "target_type" varchar(50) COLLATE "pg_catalog"."default",
  "content" text COLLATE "pg_catalog"."default",
  "ip_address" varchar(50) COLLATE "pg_catalog"."default",
  "user_agent" varchar(500) COLLATE "pg_catalog"."default",
  "create_user_id" int8,
  "create_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."crm_operation_log"."log_id" IS '日志ID';
COMMENT ON COLUMN "public"."crm_operation_log"."module" IS '模块';
COMMENT ON COLUMN "public"."crm_operation_log"."operation" IS '操作类型';
COMMENT ON COLUMN "public"."crm_operation_log"."target_id" IS '目标ID';
COMMENT ON COLUMN "public"."crm_operation_log"."target_type" IS '目标类型';
COMMENT ON COLUMN "public"."crm_operation_log"."content" IS '操作内容';
COMMENT ON COLUMN "public"."crm_operation_log"."ip_address" IS 'IP地址';
COMMENT ON COLUMN "public"."crm_operation_log"."user_agent" IS 'User Agent';
COMMENT ON COLUMN "public"."crm_operation_log"."create_user_id" IS '操作人ID';
COMMENT ON COLUMN "public"."crm_operation_log"."create_time" IS '创建时间';
COMMENT ON TABLE "public"."crm_operation_log" IS '操作日志表';

-- ----------------------------
-- Records of crm_operation_log
-- ----------------------------

-- ----------------------------
-- Table structure for crm_system_config
-- ----------------------------
DROP TABLE IF EXISTS "public"."crm_system_config";
CREATE TABLE "public"."crm_system_config" (
  "config_id" int8 NOT NULL,
  "config_key" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "config_value" text COLLATE "pg_catalog"."default",
  "config_type" varchar(50) COLLATE "pg_catalog"."default",
  "description" varchar(500) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."crm_system_config"."config_id" IS '配置ID';
COMMENT ON COLUMN "public"."crm_system_config"."config_key" IS '配置键';
COMMENT ON COLUMN "public"."crm_system_config"."config_value" IS '配置值';
COMMENT ON COLUMN "public"."crm_system_config"."config_type" IS '配置类型';
COMMENT ON COLUMN "public"."crm_system_config"."description" IS '描述';
COMMENT ON COLUMN "public"."crm_system_config"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."crm_system_config"."update_time" IS '修改时间';
COMMENT ON TABLE "public"."crm_system_config" IS '系统配置表';

-- ----------------------------
-- Records of crm_system_config
-- ----------------------------
INSERT INTO "public"."crm_system_config" VALUES (1, 'ai_api_url', 'https://dashscope.aliyuncs.com/compatible-mode/', 'ai', 'AI API地址', '2026-01-27 17:40:23', '2026-01-27 17:40:23');
INSERT INTO "public"."crm_system_config" VALUES (2, 'ai_api_key', 'sk-sss', 'ai', 'AI API密钥', '2026-01-27 17:40:23', '2026-01-27 17:40:23');
INSERT INTO "public"."crm_system_config" VALUES (3, 'ai_model', 'qwen-max', 'ai', 'AI模型名称', '2026-01-27 17:40:23', '2026-01-27 17:40:23');
INSERT INTO "public"."crm_system_config" VALUES (4, 'ai_max_tokens', '2048', 'ai', '最大Token数', '2026-01-27 17:40:23', '2026-01-27 17:40:23');
INSERT INTO "public"."crm_system_config" VALUES (5, 'ai_temperature', '0.7', 'ai', 'AI温度参数', '2026-01-27 17:40:23', '2026-01-27 17:40:23');
INSERT INTO "public"."crm_system_config" VALUES (6, 'file_upload_path', '/uploads', 'file', '文件上传路径', '2026-01-27 17:40:23', '2026-01-27 17:40:23');
INSERT INTO "public"."crm_system_config" VALUES (7, 'file_max_size', '52428800', 'file', '文件最大大小(字节)', '2026-01-27 17:40:23', '2026-01-27 17:40:23');
INSERT INTO "public"."crm_system_config" VALUES (8, 'file_allowed_types', 'pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,mp3,mp4,jpg,png', 'file', '允许的文件类型', '2026-01-27 17:40:23', '2026-01-27 17:40:23');
INSERT INTO "public"."crm_system_config" VALUES (2017413716023709698, 'ai_provider', 'dashscope', 'ai', NULL, '2026-01-31 09:44:46', '2026-01-31 09:44:46');

-- ----------------------------
-- Table structure for crm_task
-- ----------------------------
DROP TABLE IF EXISTS "public"."crm_task";
CREATE TABLE "public"."crm_task" (
  "task_id" int8 NOT NULL,
  "title" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "description" text COLLATE "pg_catalog"."default",
  "due_date" timestamp(6),
  "priority" varchar(20) COLLATE "pg_catalog"."default",
  "status" varchar(20) COLLATE "pg_catalog"."default",
  "assigned_to" int8,
  "customer_id" int8,
  "generated_by_ai" int2 DEFAULT 0,
  "ai_context" text COLLATE "pg_catalog"."default",
  "completed_time" timestamp(6),
  "create_user_id" int8,
  "update_user_id" int8,
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."crm_task"."task_id" IS '任务ID';
COMMENT ON COLUMN "public"."crm_task"."title" IS '标题';
COMMENT ON COLUMN "public"."crm_task"."description" IS '描述';
COMMENT ON COLUMN "public"."crm_task"."due_date" IS '截止日期';
COMMENT ON COLUMN "public"."crm_task"."priority" IS '优先级: high, medium, low';
COMMENT ON COLUMN "public"."crm_task"."status" IS '状态: pending, in_progress, completed';
COMMENT ON COLUMN "public"."crm_task"."assigned_to" IS '指派人ID';
COMMENT ON COLUMN "public"."crm_task"."customer_id" IS '关联客户ID';
COMMENT ON COLUMN "public"."crm_task"."generated_by_ai" IS '是否AI生成: 0-否, 1-是';
COMMENT ON COLUMN "public"."crm_task"."ai_context" IS 'AI生成上下文';
COMMENT ON COLUMN "public"."crm_task"."completed_time" IS '完成时间';
COMMENT ON COLUMN "public"."crm_task"."create_user_id" IS '创建人ID';
COMMENT ON COLUMN "public"."crm_task"."update_user_id" IS '修改人ID';
COMMENT ON COLUMN "public"."crm_task"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."crm_task"."update_time" IS '修改时间';
COMMENT ON TABLE "public"."crm_task" IS '任务表';

-- ----------------------------
-- Records of crm_task
-- ----------------------------

-- ----------------------------
-- Table structure for manager_menu
-- ----------------------------
DROP TABLE IF EXISTS "public"."manager_menu";
CREATE TABLE "public"."manager_menu" (
  "menu_id" int8 NOT NULL,
  "parent_id" int8,
  "realm" varchar(64) COLLATE "pg_catalog"."default",
  "realm_name" varchar(64) COLLATE "pg_catalog"."default",
  "type" int2
)
;
COMMENT ON COLUMN "public"."manager_menu"."menu_id" IS '菜单ID';
COMMENT ON COLUMN "public"."manager_menu"."parent_id" IS '上级菜单ID';
COMMENT ON COLUMN "public"."manager_menu"."realm" IS '菜单标识';
COMMENT ON COLUMN "public"."manager_menu"."realm_name" IS '菜单名称';
COMMENT ON COLUMN "public"."manager_menu"."type" IS '类型 1 数据 2api 3 菜单 4 按钮 5 功能';
COMMENT ON TABLE "public"."manager_menu" IS '菜单表';

-- ----------------------------
-- Records of manager_menu
-- ----------------------------
INSERT INTO "public"."manager_menu" VALUES (1, 0, 'manager', '后台管理', 3);
INSERT INTO "public"."manager_menu" VALUES (2, 0, 'chain', '链管理', 3);
INSERT INTO "public"."manager_menu" VALUES (3, 0, 'company', '企业管理', 3);
INSERT INTO "public"."manager_menu" VALUES (4, 1, 'user', '用户管理', 3);
INSERT INTO "public"."manager_menu" VALUES (5, 1, 'role', '角色管理', 3);
INSERT INTO "public"."manager_menu" VALUES (7, 4, 'list', '查询用户列表', 4);
INSERT INTO "public"."manager_menu" VALUES (8, 4, 'add', '添加用户', 4);
INSERT INTO "public"."manager_menu" VALUES (9, 4, 'update', '修改用户', 4);
INSERT INTO "public"."manager_menu" VALUES (10, 4, 'delete', '删除用户', 4);
INSERT INTO "public"."manager_menu" VALUES (11, 4, 'read', '查看用户详情', 4);
INSERT INTO "public"."manager_menu" VALUES (12, 4, 'status', '设置用户状态', 4);
INSERT INTO "public"."manager_menu" VALUES (13, 4, 'password', '修改密码', 4);
INSERT INTO "public"."manager_menu" VALUES (15, 5, 'list', '查询角色列表', 4);
INSERT INTO "public"."manager_menu" VALUES (16, 5, 'add', '添加角色', 4);
INSERT INTO "public"."manager_menu" VALUES (17, 5, 'update', '修改角色', 4);
INSERT INTO "public"."manager_menu" VALUES (18, 5, 'read', '查看角色详情', 4);
INSERT INTO "public"."manager_menu" VALUES (19, 5, 'all', '全局角色查询', 4);
INSERT INTO "public"."manager_menu" VALUES (24, 5, 'auth', '角色权限', 4);
INSERT INTO "public"."manager_menu" VALUES (25, 6, 'list', '查询菜单列表', 4);
INSERT INTO "public"."manager_menu" VALUES (26, 2, 'list', '链实例', 3);
INSERT INTO "public"."manager_menu" VALUES (27, 2, 'wallet', '钱包管理', 3);
INSERT INTO "public"."manager_menu" VALUES (28, 26, 'list', '查询链列表', 4);
INSERT INTO "public"."manager_menu" VALUES (29, 26, 'add', '新建链实例', 4);
INSERT INTO "public"."manager_menu" VALUES (30, 26, 'update', '修改链信息', 4);
INSERT INTO "public"."manager_menu" VALUES (31, 26, 'delete', '删除链实例', 4);
INSERT INTO "public"."manager_menu" VALUES (32, 26, 'read', '查看链详情', 4);
INSERT INTO "public"."manager_menu" VALUES (33, 26, 'status', '设置链状态', 4);
INSERT INTO "public"."manager_menu" VALUES (34, 27, 'list', '查询钱包列表', 4);
INSERT INTO "public"."manager_menu" VALUES (35, 27, 'add', '新建钱包', 4);
INSERT INTO "public"."manager_menu" VALUES (36, 27, 'update', '修改钱包信息', 4);
INSERT INTO "public"."manager_menu" VALUES (37, 27, 'delete', '删除钱包', 4);
INSERT INTO "public"."manager_menu" VALUES (38, 27, 'read', '查看钱包详情', 4);
INSERT INTO "public"."manager_menu" VALUES (39, 3, 'list', '企业信息', 3);
INSERT INTO "public"."manager_menu" VALUES (40, 39, 'list', '查询企业列表', 4);
INSERT INTO "public"."manager_menu" VALUES (41, 39, 'add', '新建企业', 4);
INSERT INTO "public"."manager_menu" VALUES (42, 39, 'update', '修改企业信息', 4);
INSERT INTO "public"."manager_menu" VALUES (43, 39, 'delete', '删除企业', 4);
INSERT INTO "public"."manager_menu" VALUES (44, 39, 'read', '查看企业详情', 4);
INSERT INTO "public"."manager_menu" VALUES (45, 39, 'status', '设置企业状态', 4);
INSERT INTO "public"."manager_menu" VALUES (46, 3, 'access_control', '访问控制', 4);
INSERT INTO "public"."manager_menu" VALUES (47, 0, 'monitoring', '监控告警', 4);
INSERT INTO "public"."manager_menu" VALUES (48, 0, 'security', '安全与审计', 3);
INSERT INTO "public"."manager_menu" VALUES (49, 48, 'log', '审计日志', 4);
INSERT INTO "public"."manager_menu" VALUES (50, 48, 'content_audit', '内容安全管理', 4);

-- ----------------------------
-- Table structure for manager_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."manager_role";
CREATE TABLE "public"."manager_role" (
  "role_id" int8 NOT NULL,
  "role_name" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "realm" varchar(64) COLLATE "pg_catalog"."default",
  "description" varchar(255) COLLATE "pg_catalog"."default",
  "data_type" int2,
  "create_user_id" int8,
  "update_user_id" int8,
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."manager_role"."role_id" IS '角色ID';
COMMENT ON COLUMN "public"."manager_role"."role_name" IS '角色名称';
COMMENT ON COLUMN "public"."manager_role"."realm" IS '角色标识符';
COMMENT ON COLUMN "public"."manager_role"."description" IS '角色描述';
COMMENT ON COLUMN "public"."manager_role"."data_type" IS '数据权限 1、本人，2、本人及下属，3、本部门，4、本部门及下属部门，5、全部';
COMMENT ON COLUMN "public"."manager_role"."create_user_id" IS '创建人';
COMMENT ON COLUMN "public"."manager_role"."update_user_id" IS '修改人';
COMMENT ON COLUMN "public"."manager_role"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."manager_role"."update_time" IS '修改时间';
COMMENT ON TABLE "public"."manager_role" IS '角色表';

-- ----------------------------
-- Records of manager_role
-- ----------------------------
INSERT INTO "public"."manager_role" VALUES (1988211126550142978, '部分权限', NULL, '部分权限', NULL, 1, NULL, '2025-11-11 19:44:06', NULL);
INSERT INTO "public"."manager_role" VALUES (1993867842096762882, 'all', 'realm_ahbqju', '', NULL, 1, NULL, '2025-11-27 10:21:52', NULL);

-- ----------------------------
-- Table structure for manager_role_menu
-- ----------------------------
DROP TABLE IF EXISTS "public"."manager_role_menu";
CREATE TABLE "public"."manager_role_menu" (
  "id" int8 NOT NULL,
  "role_id" int8 NOT NULL,
  "menu_id" int8 NOT NULL,
  "create_user_id" int8,
  "update_user_id" int8,
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."manager_role_menu"."id" IS '主键';
COMMENT ON COLUMN "public"."manager_role_menu"."role_id" IS '角色ID';
COMMENT ON COLUMN "public"."manager_role_menu"."menu_id" IS '菜单ID';
COMMENT ON COLUMN "public"."manager_role_menu"."create_user_id" IS '创建人';
COMMENT ON COLUMN "public"."manager_role_menu"."update_user_id" IS '修改人';
COMMENT ON COLUMN "public"."manager_role_menu"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."manager_role_menu"."update_time" IS '修改时间';
COMMENT ON TABLE "public"."manager_role_menu" IS '角色菜单对应关系表';

-- ----------------------------
-- Records of manager_role_menu
-- ----------------------------
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476097, 1988211126550142978, 4, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476098, 1988211126550142978, 7, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476099, 1988211126550142978, 8, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476100, 1988211126550142978, 9, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476101, 1988211126550142978, 10, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476102, 1988211126550142978, 11, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476103, 1988211126550142978, 12, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476104, 1988211126550142978, 13, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476105, 1988211126550142978, 15, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476106, 1988211126550142978, 16, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476107, 1988211126550142978, 2, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476108, 1988211126550142978, 26, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476109, 1988211126550142978, 28, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476110, 1988211126550142978, 29, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476111, 1988211126550142978, 30, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476112, 1988211126550142978, 31, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476113, 1988211126550142978, 32, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476114, 1988211126550142978, 33, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476115, 1988211126550142978, 27, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476116, 1988211126550142978, 34, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476117, 1988211126550142978, 35, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476118, 1988211126550142978, 36, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476119, 1988211126550142978, 37, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1991111197545476120, 1988211126550142978, 38, 1, NULL, '2025-11-19 19:47:57', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142786, 1993867842096762882, 1, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142787, 1993867842096762882, 4, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142788, 1993867842096762882, 7, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142789, 1993867842096762882, 8, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142790, 1993867842096762882, 9, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142791, 1993867842096762882, 10, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142792, 1993867842096762882, 11, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142793, 1993867842096762882, 12, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142794, 1993867842096762882, 13, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142795, 1993867842096762882, 5, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142796, 1993867842096762882, 15, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142797, 1993867842096762882, 16, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142798, 1993867842096762882, 17, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142799, 1993867842096762882, 18, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142800, 1993867842096762882, 19, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142801, 1993867842096762882, 24, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142802, 1993867842096762882, 2, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142803, 1993867842096762882, 26, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142804, 1993867842096762882, 28, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142805, 1993867842096762882, 29, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142806, 1993867842096762882, 30, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142807, 1993867842096762882, 31, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142808, 1993867842096762882, 32, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142809, 1993867842096762882, 33, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142810, 1993867842096762882, 27, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142811, 1993867842096762882, 34, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142812, 1993867842096762882, 35, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142813, 1993867842096762882, 36, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142814, 1993867842096762882, 37, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142815, 1993867842096762882, 38, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142816, 1993867842096762882, 3, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142817, 1993867842096762882, 39, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142818, 1993867842096762882, 40, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142819, 1993867842096762882, 41, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142820, 1993867842096762882, 42, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142821, 1993867842096762882, 43, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142822, 1993867842096762882, 44, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142823, 1993867842096762882, 45, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142824, 1993867842096762882, 46, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142825, 1993867842096762882, 47, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142826, 1993867842096762882, 48, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142827, 1993867842096762882, 49, 1, NULL, '2025-11-27 10:22:03', NULL);
INSERT INTO "public"."manager_role_menu" VALUES (1993867884673142828, 1993867842096762882, 50, 1, NULL, '2025-11-27 10:22:03', NULL);

-- ----------------------------
-- Table structure for manager_user
-- ----------------------------
DROP TABLE IF EXISTS "public"."manager_user";
CREATE TABLE "public"."manager_user" (
  "user_id" int8 NOT NULL,
  "username" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "password" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "salt" varchar(64) COLLATE "pg_catalog"."default",
  "img" varchar(255) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6) NOT NULL,
  "realname" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "num" varchar(64) COLLATE "pg_catalog"."default",
  "mobile" varchar(32) COLLATE "pg_catalog"."default",
  "email" varchar(128) COLLATE "pg_catalog"."default",
  "sex" int2 DEFAULT 0,
  "dept_id" int4,
  "post" varchar(64) COLLATE "pg_catalog"."default",
  "status" int2 DEFAULT 2,
  "parent_id" int8
)
;
COMMENT ON COLUMN "public"."manager_user"."user_id" IS '主键';
COMMENT ON COLUMN "public"."manager_user"."username" IS '用户名';
COMMENT ON COLUMN "public"."manager_user"."password" IS '密码';
COMMENT ON COLUMN "public"."manager_user"."salt" IS '安全符';
COMMENT ON COLUMN "public"."manager_user"."img" IS '头像';
COMMENT ON COLUMN "public"."manager_user"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."manager_user"."realname" IS '真实姓名';
COMMENT ON COLUMN "public"."manager_user"."num" IS '员工编号';
COMMENT ON COLUMN "public"."manager_user"."mobile" IS '手机号';
COMMENT ON COLUMN "public"."manager_user"."email" IS '邮箱';
COMMENT ON COLUMN "public"."manager_user"."sex" IS '0 未选择 1 男 2 女';
COMMENT ON COLUMN "public"."manager_user"."dept_id" IS '部门';
COMMENT ON COLUMN "public"."manager_user"."post" IS '岗位';
COMMENT ON COLUMN "public"."manager_user"."status" IS '状态,0禁用,1正常,2未激活';
COMMENT ON COLUMN "public"."manager_user"."parent_id" IS '直属上级ID';
COMMENT ON TABLE "public"."manager_user" IS '用户表';

-- ----------------------------
-- Records of manager_user
-- ----------------------------
INSERT INTO "public"."manager_user" VALUES (1, 'admin', '$2a$10$Kgbyz2XIQQ8xxF8fzcG1uO3lMaJ3x/.qwJNezZ909Ov/Vj8SVXEKO', 'aa1bb1b0f5fa40c5b83e22f1de0500c9', NULL, '2025-11-08 14:54:01', 'admin', 'A021', '18888888888', NULL, 0, NULL, NULL, 1, 0);
INSERT INTO "public"."manager_user" VALUES (1988224371763707906, '123', '$2a$10$k6OGyDK6U8/jla4VI7z1JejxRDfCtjvarXoyulaXJhlm7zZx6fO.q', '142a2b2f780345b19dd5c8242be970dc', NULL, '2025-11-11 20:36:44', '123', NULL, '123', '3213', 0, NULL, NULL, 1, NULL);
INSERT INTO "public"."manager_user" VALUES (1988854418885259264, 'wwww', '$2a$10$7NDB3Ml7ZGKJtDO6NqSF4e30RxthyA3os.940/H3BDo2qE..GZCDS', '96b90870534e40e383f1ef106a8166a6', NULL, '2025-11-13 14:20:19', '1111', NULL, '1111', '', 0, NULL, NULL, 1, NULL);
INSERT INTO "public"."manager_user" VALUES (1988863766139215872, '权限账号', '$2a$10$9Qj0qkSZWgp6cNLx5h2YO.d1Ar/m3QJayrnyDffw15PvFJBapZ8N6', '84bd4777d7cc48598b7c8ca3fd9512bf', NULL, '2025-11-13 14:57:28', '权', NULL, '', '', 0, NULL, NULL, 1, NULL);
INSERT INTO "public"."manager_user" VALUES (1991112035846250496, 'test', '$2a$10$ywH9VXmPXdoNSLadaBs5.u.riGw75Dj3D.yKGZc7wmoGGRj4lARWW', '6f1941a61e0148598aed060a3f170697', NULL, '2025-11-19 19:51:17', 'est', NULL, '', '', 0, NULL, NULL, 1, NULL);
INSERT INTO "public"."manager_user" VALUES (1993867768534880256, 'y', '$2a$10$YVJfQTzKxWQcZXrwcIuIze35hnQrP5gY0Sazka9Z9IiF08j.DO6Zi', '5850b8c036f94df28f5e3438a8515172', NULL, '2025-11-27 10:21:35', 'y', NULL, '', '', 0, NULL, NULL, 1, NULL);

-- ----------------------------
-- Table structure for manager_user_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."manager_user_role";
CREATE TABLE "public"."manager_user_role" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "role_id" int8 NOT NULL,
  "create_user_id" int8,
  "update_user_id" int8,
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."manager_user_role"."id" IS '主键';
COMMENT ON COLUMN "public"."manager_user_role"."user_id" IS '用户ID';
COMMENT ON COLUMN "public"."manager_user_role"."role_id" IS '角色ID';
COMMENT ON COLUMN "public"."manager_user_role"."create_user_id" IS '创建人';
COMMENT ON COLUMN "public"."manager_user_role"."update_user_id" IS '修改人';
COMMENT ON COLUMN "public"."manager_user_role"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."manager_user_role"."update_time" IS '修改时间';
COMMENT ON TABLE "public"."manager_user_role" IS '用户与角色对应表';

-- ----------------------------
-- Records of manager_user_role
-- ----------------------------
INSERT INTO "public"."manager_user_role" VALUES (1988854418858078209, 1988854418885259264, 1988211126550142978, 1, NULL, '2025-11-13 14:20:19', NULL);
INSERT INTO "public"."manager_user_role" VALUES (1988854876620218369, 1988224371763707906, 1988211126550142978, NULL, 1, NULL, '2025-11-13 14:22:08');
INSERT INTO "public"."manager_user_role" VALUES (1988854920857542658, 1, 1988211126550142978, NULL, 1, NULL, '2025-11-13 14:22:19');
INSERT INTO "public"."manager_user_role" VALUES (1988863766112002050, 1988863766139215872, 1988211126550142978, 1, NULL, '2025-11-13 14:57:28', NULL);
INSERT INTO "public"."manager_user_role" VALUES (1991112035814875138, 1991112035846250496, 1988211126550142978, 1, NULL, '2025-11-19 19:51:17', NULL);
INSERT INTO "public"."manager_user_role" VALUES (1993867933473869825, 1993867768534880256, 1993867842096762882, NULL, 1, NULL, '2025-11-27 10:22:14');

-- ----------------------------
-- Indexes structure for table crm_ai_agent
-- ----------------------------
CREATE INDEX "idx_category" ON "public"."crm_ai_agent" USING btree (
  "category" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_enabled" ON "public"."crm_ai_agent" USING btree (
  "enabled" "pg_catalog"."int2_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table crm_ai_agent
-- ----------------------------
ALTER TABLE "public"."crm_ai_agent" ADD CONSTRAINT "crm_ai_agent_pkey" PRIMARY KEY ("agent_id");

-- ----------------------------
-- Indexes structure for table crm_chat_attachment
-- ----------------------------
CREATE INDEX "idx_message_id" ON "public"."crm_chat_attachment" USING btree (
  "message_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table crm_chat_attachment
-- ----------------------------
ALTER TABLE "public"."crm_chat_attachment" ADD CONSTRAINT "crm_chat_attachment_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table crm_chat_message
-- ----------------------------
CREATE INDEX "idx_create_time" ON "public"."crm_chat_message" USING btree (
  "create_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_session_id" ON "public"."crm_chat_message" USING btree (
  "session_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table crm_chat_message
-- ----------------------------
ALTER TABLE "public"."crm_chat_message" ADD CONSTRAINT "crm_chat_message_pkey" PRIMARY KEY ("message_id");

-- ----------------------------
-- Indexes structure for table crm_chat_session
-- ----------------------------
CREATE INDEX "idx_agent_id" ON "public"."crm_chat_session" USING btree (
  "agent_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_customer_id" ON "public"."crm_chat_session" USING btree (
  "customer_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_id" ON "public"."crm_chat_session" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table crm_chat_session
-- ----------------------------
ALTER TABLE "public"."crm_chat_session" ADD CONSTRAINT "crm_chat_session_pkey" PRIMARY KEY ("session_id");

-- ----------------------------
-- Indexes structure for table crm_contact
-- ----------------------------
CREATE INDEX "idx_name" ON "public"."crm_contact" USING btree (
  "name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table crm_contact
-- ----------------------------
ALTER TABLE "public"."crm_contact" ADD CONSTRAINT "crm_contact_pkey" PRIMARY KEY ("contact_id");

-- ----------------------------
-- Indexes structure for table crm_contact_tag
-- ----------------------------
CREATE INDEX "idx_contact_id" ON "public"."crm_contact_tag" USING btree (
  "contact_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table crm_contact_tag
-- ----------------------------
ALTER TABLE "public"."crm_contact_tag" ADD CONSTRAINT "crm_contact_tag_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table crm_custom_field
-- ----------------------------
CREATE INDEX "idx_entity_type" ON "public"."crm_custom_field" USING btree (
  "entity_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_status" ON "public"."crm_custom_field" USING btree (
  "status" "pg_catalog"."int2_ops" ASC NULLS LAST
);
CREATE UNIQUE INDEX "uk_entity_column" ON "public"."crm_custom_field" USING btree (
  "entity_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "column_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE UNIQUE INDEX "uk_entity_field" ON "public"."crm_custom_field" USING btree (
  "entity_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "field_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table crm_custom_field
-- ----------------------------
ALTER TABLE "public"."crm_custom_field" ADD CONSTRAINT "crm_custom_field_pkey" PRIMARY KEY ("field_id");

-- ----------------------------
-- Indexes structure for table crm_customer
-- ----------------------------
CREATE INDEX "idx_level" ON "public"."crm_customer" USING btree (
  "level" COLLATE "pg_catalog"."default" "pg_catalog"."bpchar_ops" ASC NULLS LAST
);
CREATE INDEX "idx_owner_id" ON "public"."crm_customer" USING btree (
  "owner_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_stage" ON "public"."crm_customer" USING btree (
  "stage" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table crm_customer
-- ----------------------------
ALTER TABLE "public"."crm_customer" ADD CONSTRAINT "crm_customer_pkey" PRIMARY KEY ("customer_id");

-- ----------------------------
-- Indexes structure for table crm_customer_tag
-- ----------------------------
CREATE INDEX "idx_tag_name" ON "public"."crm_customer_tag" USING btree (
  "tag_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table crm_customer_tag
-- ----------------------------
ALTER TABLE "public"."crm_customer_tag" ADD CONSTRAINT "crm_customer_tag_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table crm_customer_team
-- ----------------------------
CREATE UNIQUE INDEX "uk_customer_user" ON "public"."crm_customer_team" USING btree (
  "customer_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table crm_customer_team
-- ----------------------------
ALTER TABLE "public"."crm_customer_team" ADD CONSTRAINT "crm_customer_team_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table crm_follow_up
-- ----------------------------
CREATE INDEX "idx_follow_time" ON "public"."crm_follow_up" USING btree (
  "follow_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table crm_follow_up
-- ----------------------------
ALTER TABLE "public"."crm_follow_up" ADD CONSTRAINT "crm_follow_up_pkey" PRIMARY KEY ("follow_up_id");

-- ----------------------------
-- Indexes structure for table crm_knowledge
-- ----------------------------
CREATE INDEX "ft_content" ON "public"."crm_knowledge" USING btree (
  "name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "content_text" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_type" ON "public"."crm_knowledge" USING btree (
  "type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_upload_user_id" ON "public"."crm_knowledge" USING btree (
  "upload_user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table crm_knowledge
-- ----------------------------
ALTER TABLE "public"."crm_knowledge" ADD CONSTRAINT "crm_knowledge_pkey" PRIMARY KEY ("knowledge_id");

-- ----------------------------
-- Indexes structure for table crm_knowledge_tag
-- ----------------------------
CREATE INDEX "idx_knowledge_id" ON "public"."crm_knowledge_tag" USING btree (
  "knowledge_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table crm_knowledge_tag
-- ----------------------------
ALTER TABLE "public"."crm_knowledge_tag" ADD CONSTRAINT "crm_knowledge_tag_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table crm_operation_log
-- ----------------------------
CREATE INDEX "idx_module" ON "public"."crm_operation_log" USING btree (
  "module" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_target" ON "public"."crm_operation_log" USING btree (
  "target_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "target_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table crm_operation_log
-- ----------------------------
ALTER TABLE "public"."crm_operation_log" ADD CONSTRAINT "crm_operation_log_pkey" PRIMARY KEY ("log_id");

-- ----------------------------
-- Indexes structure for table crm_system_config
-- ----------------------------
CREATE UNIQUE INDEX "uk_config_key" ON "public"."crm_system_config" USING btree (
  "config_key" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table crm_system_config
-- ----------------------------
ALTER TABLE "public"."crm_system_config" ADD CONSTRAINT "crm_system_config_pkey" PRIMARY KEY ("config_id");

-- ----------------------------
-- Indexes structure for table crm_task
-- ----------------------------
CREATE INDEX "idx_assigned_to" ON "public"."crm_task" USING btree (
  "assigned_to" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_due_date" ON "public"."crm_task" USING btree (
  "due_date" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table crm_task
-- ----------------------------
ALTER TABLE "public"."crm_task" ADD CONSTRAINT "crm_task_pkey" PRIMARY KEY ("task_id");

-- ----------------------------
-- Primary Key structure for table manager_menu
-- ----------------------------
ALTER TABLE "public"."manager_menu" ADD CONSTRAINT "manager_menu_pkey" PRIMARY KEY ("menu_id");

-- ----------------------------
-- Primary Key structure for table manager_role
-- ----------------------------
ALTER TABLE "public"."manager_role" ADD CONSTRAINT "manager_role_pkey" PRIMARY KEY ("role_id");

-- ----------------------------
-- Indexes structure for table manager_role_menu
-- ----------------------------
CREATE INDEX "idx_menu_id" ON "public"."manager_role_menu" USING btree (
  "menu_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_role_id" ON "public"."manager_role_menu" USING btree (
  "role_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table manager_role_menu
-- ----------------------------
ALTER TABLE "public"."manager_role_menu" ADD CONSTRAINT "manager_role_menu_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table manager_user
-- ----------------------------
CREATE INDEX "username" ON "public"."manager_user" USING btree (
  "username" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table manager_user
-- ----------------------------
ALTER TABLE "public"."manager_user" ADD CONSTRAINT "manager_user_pkey" PRIMARY KEY ("user_id");

-- ----------------------------
-- Primary Key structure for table manager_user_role
-- ----------------------------
ALTER TABLE "public"."manager_user_role" ADD CONSTRAINT "manager_user_role_pkey" PRIMARY KEY ("id");
