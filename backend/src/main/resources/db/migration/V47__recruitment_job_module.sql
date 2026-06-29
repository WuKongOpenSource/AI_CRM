CREATE TABLE IF NOT EXISTS crm_recruitment_job (
    recruitment_job_id BIGINT PRIMARY KEY,
    job_name VARCHAR(150) NOT NULL,
    department VARCHAR(100),
    headcount INTEGER,
    work_years NUMERIC(6, 1),
    education VARCHAR(100),
    city VARCHAR(100),
    salary_range VARCHAR(100),
    skill_tags VARCHAR(500),
    responsibilities TEXT,
    requirements TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'open',
    owner_id BIGINT,
    remark TEXT,
    del_flag INTEGER NOT NULL DEFAULT 0,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_crm_recruitment_job_status
    ON crm_recruitment_job(status, del_flag);

CREATE INDEX IF NOT EXISTS idx_crm_recruitment_job_owner
    ON crm_recruitment_job(owner_id);

ALTER TABLE crm_candidate
    ADD COLUMN IF NOT EXISTS recruitment_job_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_crm_candidate_recruitment_job
    ON crm_candidate(recruitment_job_id);

INSERT INTO crm_recruitment_job (
    recruitment_job_id, job_name, department, headcount, work_years, education, city,
    salary_range, skill_tags, responsibilities, requirements, status, del_flag,
    create_user_id, update_user_id, create_time, update_time
) VALUES
(
    380000000000001, 'Java开发工程师', '研发部', 2, 3, '本科及以上', '上海',
    '20k-35k', 'Java, Spring Boot, PostgreSQL, Redis',
    '负责核心业务系统后端开发、接口设计、性能优化和线上问题排查。',
    '熟悉 Java 和 Spring Boot，有数据库建模经验，具备良好的问题定位和沟通能力。',
    'open', 0, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    380000000000002, '产品经理', '产品部', 1, 3, '本科及以上', '上海',
    '18k-30k', '需求分析, 原型设计, CRM, AI产品',
    '负责 CRM 与 AI 助手相关产品规划、需求拆解、验收和跨团队推进。',
    '具备 B 端产品经验，能沉淀业务流程并推动研发、设计、运营协作落地。',
    'open', 0, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (recruitment_job_id) DO NOTHING;
