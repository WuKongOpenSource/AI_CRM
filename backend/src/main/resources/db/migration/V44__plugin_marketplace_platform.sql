-- ============================================
-- V44: Plugin marketplace platform foundation (change: add-plugin-marketplace-platform).
--
-- Seven instance-side tables for the Open Core plugin foundation:
--   OAuth client registry, installed AI applications, and per-instance plugin
--   install / grant / token / subscription / audit records.
--
-- TENANCY: this codebase is single-tenant (no TenantLineInnerInterceptor exists).
--   Every operational table EXCEPT crm_oauth_client carries a RESERVED nullable
--   `tenant_id` (NULL = the single instance). Nothing filters on it today; it
--   exists only so a future multi-tenant layer (see change add-multi-tenancy-
--   foundation) can scope these tables without an ALTER. crm_oauth_client is
--   instance-global (a plugin-app catalog), so it has no tenant_id.
--
-- These are infra/metadata tables, not protected business entities, so they need
--   no GlobalDataPermissionHandler registration.
-- IDs are Snowflake BIGINT (BaseUtil.getNextId); audit columns are stamped by
--   MyMetaObjectHandler (createTime/updateTime/createUserId).
-- ============================================

-- -------------------------------------------------------------------
-- 1) crm_oauth_client — instance-global registry of known plugin OAuth apps
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS crm_oauth_client (
    id                  BIGINT NOT NULL,
    client_id           VARCHAR(64)  NOT NULL,
    plugin_id           VARCHAR(100) NOT NULL,
    display_name        VARCHAR(150),
    client_secret_hash  VARCHAR(255),
    grant_types         VARCHAR(200) NOT NULL DEFAULT 'client_credentials',
    default_scopes      TEXT,
    redirect_uris       TEXT,
    signing_key_id      VARCHAR(100),
    status              SMALLINT     NOT NULL DEFAULT 1,
    create_user_id      BIGINT,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_oauth_client_client_id ON crm_oauth_client (client_id);
CREATE INDEX IF NOT EXISTS idx_oauth_client_plugin ON crm_oauth_client (plugin_id);
COMMENT ON TABLE  crm_oauth_client                    IS '插件 OAuth 应用注册表（实例全局，无 tenant_id）';
COMMENT ON COLUMN crm_oauth_client.client_id          IS 'OAuth client_id（全局唯一）';
COMMENT ON COLUMN crm_oauth_client.plugin_id          IS '所属插件标识，如 acme.invoice-sync';
COMMENT ON COLUMN crm_oauth_client.client_secret_hash IS 'client_secret 的哈希，绝不存明文';
COMMENT ON COLUMN crm_oauth_client.grant_types        IS '允许的授权类型，CSV：client_credentials,authorization_code';
COMMENT ON COLUMN crm_oauth_client.default_scopes     IS '默认申请 scope（JSON/CSV，module:action 词表）';
COMMENT ON COLUMN crm_oauth_client.signing_key_id     IS '该插件 artifact 验签所用密钥 id（公钥固定在系统配置）';
COMMENT ON COLUMN crm_oauth_client.status             IS '0=停用 1=启用';

-- -------------------------------------------------------------------
-- 2) crm_ai_application — installed / custom chat apps (built-ins stay in code)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS crm_ai_application (
    app_id               BIGINT NOT NULL,
    plugin_id            VARCHAR(100),
    code                 VARCHAR(64)  NOT NULL,
    label                VARCHAR(100) NOT NULL,
    icon_name            VARCHAR(64),
    description          TEXT,
    system_prompt        TEXT,
    default_rag_enabled  SMALLINT     NOT NULL DEFAULT 0,
    tool_groups          TEXT,
    recommended_questions TEXT,
    status               SMALLINT     NOT NULL DEFAULT 1,
    tenant_id            BIGINT,
    create_user_id       BIGINT,
    create_time          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (app_id)
);
-- Uniqueness is per-instance today: a single-column unique index on `code`.
-- It is intentionally NOT (tenant_id, code) yet — tenant_id is NULL on every row
-- now, and PostgreSQL treats NULLs as distinct in a unique index, so a composite
-- would NOT enforce uniqueness in single-tenant mode. The add-multi-tenancy-
-- foundation change backfills tenant_id, then drops this and recreates it as a
-- (tenant_id, code) composite.
CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_application_code ON crm_ai_application (code);
CREATE INDEX IF NOT EXISTS idx_ai_application_plugin ON crm_ai_application (plugin_id);
COMMENT ON TABLE  crm_ai_application                      IS '已安装/自定义 AI 应用（内置应用仍由 ChatApplicationRegistry 构造函数提供，不入库）';
COMMENT ON COLUMN crm_ai_application.code                 IS '应用编码，与内置编码冲突时内置优先';
COMMENT ON COLUMN crm_ai_application.tool_groups          IS '关联工具组（JSON/CSV）';
COMMENT ON COLUMN crm_ai_application.recommended_questions IS '推荐问题（JSON 数组）';
COMMENT ON COLUMN crm_ai_application.tenant_id            IS '预留：NULL=单实例；未来多租户用';

-- -------------------------------------------------------------------
-- 3) crm_plugin_install — per-instance install record (version-pinned)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS crm_plugin_install (
    install_id        BIGINT NOT NULL,
    plugin_id         VARCHAR(100) NOT NULL,
    version           VARCHAR(50)  NOT NULL,
    enabled           SMALLINT     NOT NULL DEFAULT 1,
    config_encrypted  TEXT,
    install_source    VARCHAR(20)  NOT NULL DEFAULT 'marketplace',
    tenant_id         BIGINT,
    create_user_id    BIGINT,
    create_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (install_id)
);
-- Single-column unique now (see note on uq_ai_application_code re: NULL tenant_id);
-- add-multi-tenancy-foundation recreates this as (tenant_id, plugin_id).
CREATE UNIQUE INDEX IF NOT EXISTS uq_plugin_install_plugin ON crm_plugin_install (plugin_id);
COMMENT ON TABLE  crm_plugin_install                   IS '插件安装记录（按实例，预留 tenant_id）';
COMMENT ON COLUMN crm_plugin_install.version           IS '已固定安装的版本，不自动升级';
COMMENT ON COLUMN crm_plugin_install.config_encrypted  IS '插件配置，SecretTextCipher 加密存储';
COMMENT ON COLUMN crm_plugin_install.install_source    IS 'marketplace / local_manifest';
COMMENT ON COLUMN crm_plugin_install.tenant_id         IS '预留：NULL=单实例；未来多租户用';

-- -------------------------------------------------------------------
-- 4) crm_plugin_grant — consent record (granted module:action scopes)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS crm_plugin_grant (
    grant_id           BIGINT NOT NULL,
    plugin_id          VARCHAR(100) NOT NULL,
    client_id          VARCHAR(64)  NOT NULL,
    granted_scopes     TEXT         NOT NULL,
    granted_by_user_id BIGINT,
    create_user_id     BIGINT,
    tenant_id          BIGINT,
    create_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (grant_id)
);
-- Single-column unique now (see note on uq_ai_application_code re: NULL tenant_id);
-- add-multi-tenancy-foundation recreates this as (tenant_id, plugin_id).
CREATE UNIQUE INDEX IF NOT EXISTS uq_plugin_grant_plugin ON crm_plugin_grant (plugin_id);
COMMENT ON TABLE  crm_plugin_grant                IS '插件授权/同意记录（安装时管理员批准的 scope）';
COMMENT ON COLUMN crm_plugin_grant.granted_scopes IS '已批准 scope（JSON/CSV，module:action）';
COMMENT ON COLUMN crm_plugin_grant.tenant_id      IS '预留：NULL=单实例；未来多租户用';

-- -------------------------------------------------------------------
-- 5) crm_plugin_token — issued plugin tokens (hashed secret only)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS crm_plugin_token (
    token_id     BIGINT NOT NULL,
    plugin_id    VARCHAR(100) NOT NULL,
    client_id    VARCHAR(64)  NOT NULL,
    token_hash   VARCHAR(255) NOT NULL,
    scopes       TEXT,
    expires_at   TIMESTAMP,
    revoked      SMALLINT     NOT NULL DEFAULT 0,
    tenant_id    BIGINT,
    create_time  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (token_id)
);
CREATE INDEX IF NOT EXISTS idx_plugin_token_hash ON crm_plugin_token (token_hash);
CREATE INDEX IF NOT EXISTS idx_plugin_token_plugin ON crm_plugin_token (plugin_id, revoked);
COMMENT ON TABLE  crm_plugin_token            IS '已签发插件令牌（仅存哈希）';
COMMENT ON COLUMN crm_plugin_token.token_hash IS '令牌哈希，绝不存明文';
COMMENT ON COLUMN crm_plugin_token.scopes     IS '有效 scope = 安装者权限 ∩ 已授权 scope';
COMMENT ON COLUMN crm_plugin_token.revoked    IS '0=有效 1=已吊销（停用/卸载时置 1）';
COMMENT ON COLUMN crm_plugin_token.tenant_id  IS '预留：NULL=单实例；未来多租户用';

-- -------------------------------------------------------------------
-- 6) crm_plugin_subscription — event -> plugin webhook subscriptions
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS crm_plugin_subscription (
    subscription_id          BIGINT NOT NULL,
    plugin_id                VARCHAR(100) NOT NULL,
    event_type               VARCHAR(64)  NOT NULL,
    endpoint_url             VARCHAR(500) NOT NULL,
    signing_secret_encrypted TEXT,
    active                   SMALLINT     NOT NULL DEFAULT 1,
    tenant_id                BIGINT,
    create_time              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (subscription_id)
);
CREATE INDEX IF NOT EXISTS idx_plugin_sub_event ON crm_plugin_subscription (event_type, active);
CREATE INDEX IF NOT EXISTS idx_plugin_sub_plugin ON crm_plugin_subscription (plugin_id);
COMMENT ON TABLE  crm_plugin_subscription                          IS '插件事件订阅（领域事件 -> webhook 端点）';
COMMENT ON COLUMN crm_plugin_subscription.event_type               IS '订阅的事件类型，如 customer.created';
COMMENT ON COLUMN crm_plugin_subscription.signing_secret_encrypted IS 'HMAC 签名密钥，SecretTextCipher 加密';
COMMENT ON COLUMN crm_plugin_subscription.active                   IS '0=停用 1=启用';
COMMENT ON COLUMN crm_plugin_subscription.tenant_id                IS '预留：NULL=单实例；未来多租户用';

-- -------------------------------------------------------------------
-- 7) crm_plugin_audit_log — immutable audit of gateway/lifecycle activity
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS crm_plugin_audit_log (
    log_id        BIGINT NOT NULL,
    plugin_id     VARCHAR(100),
    actor         VARCHAR(100),
    action        VARCHAR(64),
    scope         VARCHAR(100),
    result        VARCHAR(20),
    request_meta  TEXT,
    tenant_id     BIGINT,
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id)
);
CREATE INDEX IF NOT EXISTS idx_plugin_audit_plugin_time ON crm_plugin_audit_log (plugin_id, create_time);
COMMENT ON TABLE  crm_plugin_audit_log              IS '插件网关调用与生命周期事件的不可变审计';
COMMENT ON COLUMN crm_plugin_audit_log.action       IS 'api_call / install / uninstall / enable / disable / webhook_delivery ...';
COMMENT ON COLUMN crm_plugin_audit_log.result       IS 'success / denied / throttled / error';
COMMENT ON COLUMN crm_plugin_audit_log.request_meta IS '请求摘要（方法、路由、参数指纹等）';
COMMENT ON COLUMN crm_plugin_audit_log.tenant_id    IS '预留：NULL=单实例；未来多租户用';

-- -------------------------------------------------------------------
-- update_time triggers — match the repo convention (update_timestamp() from
-- V9_1; used by crm_init_postgres.sql and V10+). Only the five MUTABLE tables
-- get a trigger. crm_plugin_token (effectively immutable apart from `revoked`,
-- no update_time column) and crm_plugin_audit_log (append-only) are excluded.
-- -------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_oauth_client_update_time ON crm_oauth_client;
CREATE TRIGGER trg_oauth_client_update_time
    BEFORE UPDATE ON crm_oauth_client
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

DROP TRIGGER IF EXISTS trg_ai_application_update_time ON crm_ai_application;
CREATE TRIGGER trg_ai_application_update_time
    BEFORE UPDATE ON crm_ai_application
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

DROP TRIGGER IF EXISTS trg_plugin_install_update_time ON crm_plugin_install;
CREATE TRIGGER trg_plugin_install_update_time
    BEFORE UPDATE ON crm_plugin_install
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

DROP TRIGGER IF EXISTS trg_plugin_grant_update_time ON crm_plugin_grant;
CREATE TRIGGER trg_plugin_grant_update_time
    BEFORE UPDATE ON crm_plugin_grant
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

DROP TRIGGER IF EXISTS trg_plugin_subscription_update_time ON crm_plugin_subscription;
CREATE TRIGGER trg_plugin_subscription_update_time
    BEFORE UPDATE ON crm_plugin_subscription
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();
