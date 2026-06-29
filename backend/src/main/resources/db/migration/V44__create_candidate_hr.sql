CREATE TABLE IF NOT EXISTS crm_candidate (
    candidate_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(50) DEFAULT NULL,
    email VARCHAR(100) DEFAULT NULL,
    wechat VARCHAR(100) DEFAULT NULL,
    current_company VARCHAR(255) DEFAULT NULL,
    current_position VARCHAR(100) DEFAULT NULL,
    applied_position VARCHAR(150) DEFAULT NULL,
    stage VARCHAR(50) NOT NULL DEFAULT 'new',
    source VARCHAR(100) DEFAULT NULL,
    education VARCHAR(100) DEFAULT NULL,
    school VARCHAR(150) DEFAULT NULL,
    major VARCHAR(150) DEFAULT NULL,
    work_years NUMERIC(5,1) DEFAULT NULL,
    expected_city VARCHAR(100) DEFAULT NULL,
    expected_salary VARCHAR(100) DEFAULT NULL,
    skill_tags TEXT DEFAULT NULL,
    resume_summary TEXT DEFAULT NULL,
    ai_evaluation TEXT DEFAULT NULL,
    ai_parse_snapshot TEXT DEFAULT NULL,
    conflict_snapshot TEXT DEFAULT NULL,
    owner_id BIGINT DEFAULT NULL,
    remark TEXT DEFAULT NULL,
    last_contact_time TIMESTAMP DEFAULT NULL,
    next_step_time TIMESTAMP DEFAULT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    create_user_id BIGINT DEFAULT NULL,
    update_user_id BIGINT DEFAULT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (candidate_id)
);

CREATE INDEX IF NOT EXISTS idx_candidate_name ON crm_candidate (name);
CREATE INDEX IF NOT EXISTS idx_candidate_phone ON crm_candidate (phone);
CREATE INDEX IF NOT EXISTS idx_candidate_email ON crm_candidate (email);
CREATE INDEX IF NOT EXISTS idx_candidate_stage ON crm_candidate (stage);
CREATE INDEX IF NOT EXISTS idx_candidate_owner ON crm_candidate (owner_id);
CREATE INDEX IF NOT EXISTS idx_candidate_status ON crm_candidate (status);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_candidate_update_time') THEN
        CREATE TRIGGER trg_candidate_update_time
            BEFORE UPDATE ON crm_candidate
            FOR EACH ROW EXECUTE FUNCTION update_timestamp();
    END IF;
END $$;

ALTER TABLE crm_chat_session
    ADD COLUMN IF NOT EXISTS candidate_id BIGINT;

ALTER TABLE crm_schedule
    ADD COLUMN IF NOT EXISTS candidate_id BIGINT;

ALTER TABLE crm_task
    ADD COLUMN IF NOT EXISTS candidate_id BIGINT;

ALTER TABLE crm_knowledge
    ADD COLUMN IF NOT EXISTS candidate_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_chat_session_candidate ON crm_chat_session (candidate_id);
CREATE INDEX IF NOT EXISTS idx_schedule_candidate ON crm_schedule (candidate_id);
CREATE INDEX IF NOT EXISTS idx_task_candidate ON crm_task (candidate_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_candidate ON crm_knowledge (candidate_id);

INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type)
VALUES
    (2600, 0, 'candidate', '人力', 3),
    (2601, 2600, 'candidate:create', '新建候选人', 5),
    (2602, 2600, 'candidate:view', '查看候选人', 5),
    (2603, 2600, 'candidate:edit', '编辑候选人', 5),
    (2604, 2600, 'candidate:delete', '删除候选人', 5),
    (2605, 2600, 'candidate:import', '导入候选人', 5),
    (2606, 2600, 'candidate:export', '导出候选人', 5),
    (2607, 2600, 'candidate:change_stage', '变更阶段', 5)
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO manager_role_menu (id, role_id, menu_id, data_scope, create_time)
SELECT 2600000000000 + m.menu_id,
       r.role_id,
       m.menu_id,
       5,
       CURRENT_TIMESTAMP
FROM manager_role r
JOIN manager_menu m ON m.menu_id IN (2600, 2601, 2602, 2603, 2604, 2605, 2606, 2607)
WHERE r.realm = 'super_admin'
  AND NOT EXISTS (
      SELECT 1
      FROM manager_role_menu rm
      WHERE rm.role_id = r.role_id
        AND rm.menu_id = m.menu_id
  );

WITH candidate_fields (
    field_id, entity_type, field_name, field_label, field_type, column_name, column_type,
    default_value, placeholder, is_required, is_searchable, is_show_in_list, is_unique,
    options, validation_rules, sort_order
) AS (
    VALUES
        (370000000000401::BIGINT, 'candidate', 'name', '姓名', 'text', 'name', 'VARCHAR(100)', NULL, '请输入候选人姓名', 1, 1, 1, 0, NULL, NULL, 10),
        (370000000000402::BIGINT, 'candidate', 'phone', '手机号', 'text', 'phone', 'VARCHAR(50)', NULL, '请输入手机号', 0, 1, 1, 1, NULL, NULL, 20),
        (370000000000403::BIGINT, 'candidate', 'email', '邮箱', 'text', 'email', 'VARCHAR(100)', NULL, '请输入邮箱', 0, 1, 1, 1, NULL, NULL, 30),
        (370000000000404::BIGINT, 'candidate', 'appliedPosition', '应聘岗位', 'text', 'applied_position', 'VARCHAR(150)', NULL, '请输入应聘岗位', 0, 1, 1, 0, NULL, NULL, 40),
        (370000000000405::BIGINT, 'candidate', 'stage', '候选人阶段', 'select', 'stage', 'VARCHAR(50)', 'new', '请选择候选人阶段', 0, 1, 1, 0, '[{"value":"new","label":"新候选人"},{"value":"screening","label":"初筛"},{"value":"interviewing","label":"面试中"},{"value":"offer","label":"Offer"},{"value":"hired","label":"已入职"},{"value":"rejected","label":"已淘汰"}]', NULL, 50),
        (370000000000406::BIGINT, 'candidate', 'currentCompany', '当前公司', 'text', 'current_company', 'VARCHAR(255)', NULL, '请输入当前公司', 0, 1, 1, 0, NULL, NULL, 60),
        (370000000000407::BIGINT, 'candidate', 'currentPosition', '当前职位', 'text', 'current_position', 'VARCHAR(100)', NULL, '请输入当前职位', 0, 1, 1, 0, NULL, NULL, 70),
        (370000000000408::BIGINT, 'candidate', 'education', '学历', 'text', 'education', 'VARCHAR(100)', NULL, '请输入学历', 0, 1, 1, 0, NULL, NULL, 80),
        (370000000000409::BIGINT, 'candidate', 'school', '学校', 'text', 'school', 'VARCHAR(150)', NULL, '请输入学校', 0, 1, 1, 0, NULL, NULL, 90),
        (370000000000410::BIGINT, 'candidate', 'major', '专业', 'text', 'major', 'VARCHAR(150)', NULL, '请输入专业', 0, 1, 0, 0, NULL, NULL, 100),
        (370000000000411::BIGINT, 'candidate', 'workYears', '工作年限', 'number', 'work_years', 'NUMERIC(5,1)', NULL, '请输入工作年限', 0, 1, 1, 0, NULL, '{"min":0}', 110),
        (370000000000412::BIGINT, 'candidate', 'expectedCity', '期望城市', 'text', 'expected_city', 'VARCHAR(100)', NULL, '请输入期望城市', 0, 1, 1, 0, NULL, NULL, 120),
        (370000000000413::BIGINT, 'candidate', 'expectedSalary', '期望薪资', 'text', 'expected_salary', 'VARCHAR(100)', NULL, '请输入期望薪资', 0, 1, 1, 0, NULL, NULL, 130),
        (370000000000414::BIGINT, 'candidate', 'skillTags', '技能标签', 'textarea', 'skill_tags', 'TEXT', NULL, '请输入技能标签', 0, 1, 1, 0, NULL, NULL, 140),
        (370000000000415::BIGINT, 'candidate', 'resumeSummary', '简历摘要', 'textarea', 'resume_summary', 'TEXT', NULL, NULL, 0, 1, 0, 0, NULL, NULL, 150),
        (370000000000416::BIGINT, 'candidate', 'aiEvaluation', 'AI评估', 'textarea', 'ai_evaluation', 'TEXT', NULL, NULL, 0, 1, 0, 0, NULL, NULL, 160),
        (370000000000417::BIGINT, 'candidate', 'ownerId', '负责人', 'number', 'owner_id', 'BIGINT', NULL, NULL, 0, 1, 1, 0, NULL, NULL, 170),
        (370000000000418::BIGINT, 'candidate', 'source', '来源', 'text', 'source', 'VARCHAR(100)', NULL, '请输入来源', 0, 1, 1, 0, NULL, NULL, 180),
        (370000000000419::BIGINT, 'candidate', 'nextStepTime', '下一步时间', 'datetime', 'next_step_time', 'TIMESTAMP', NULL, '请选择下一步时间', 0, 1, 1, 0, NULL, NULL, 190),
        (370000000000420::BIGINT, 'candidate', 'remark', '备注', 'textarea', 'remark', 'TEXT', NULL, '请输入备注', 0, 1, 0, 0, NULL, NULL, 200)
)
INSERT INTO crm_custom_field (
    field_id, entity_type, field_name, field_label, field_type, field_source, column_name, column_type,
    default_value, placeholder, is_required, is_searchable, is_show_in_list, is_unique,
    options, validation_rules, sort_order, status, create_user_id, create_time, update_time
)
SELECT field_id, entity_type, field_name, field_label, field_type, 'system', column_name, column_type,
       default_value, placeholder, is_required, is_searchable, is_show_in_list, is_unique,
       options, validation_rules, sort_order, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM candidate_fields cf
WHERE NOT EXISTS (
    SELECT 1
    FROM crm_custom_field existing
    WHERE existing.entity_type = cf.entity_type
      AND existing.field_name = cf.field_name
)
  AND NOT EXISTS (
      SELECT 1
      FROM crm_custom_field existing
      WHERE existing.field_id = cf.field_id
  );
