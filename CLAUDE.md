·# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Read AGENTS.md first

[AGENTS.md](AGENTS.md) is the authoritative, frequently-updated reference for commands, runtime config, and the many non-obvious gotchas (tenant interceptors, Flyway versioning conflicts, frontend `Result` unwrapping, AI tool conventions, ignored test files, etc.). This file is the orientation layer — the cross-module big picture and the commands you reach for most. When the two disagree, AGENTS.md wins; verify either against the actual config before relying on it.

## Repository shape

A monorepo of **four independently-built subprojects with no root build runner**. Always `cd` into the subproject before running commands.

| Dir | What | Stack | Port |
| --- | --- | --- | --- |
| `backend/` | Main AI CRM app (entry `com.kakarote.ai_crm.ManagerApplication`) | Java 21, Spring Boot 3.3, MyBatis-Plus, PostgreSQL, Redis, MinIO | 8088 |
| `frontend/` | SPA (entry `src/main.ts`) | Vue 3, Vite, TypeScript, Element Plus, Pinia, Tailwind | 5173 (dev) |
| `sync_data/` | One-way migrator: legacy WK CRM MySQL → AI CRM PostgreSQL. Standalone; **no** backend services/auth/tenant interceptors; does not sync on startup | Spring Boot | 10456 |
| `mcp_server/` | stdio MCP server that calls the backend HTTP API. Deliberately does **not** touch `sync_data` | Node/TypeScript | — |

`docker/docker-compose.yaml` runs CRM + WeKnora + Postgres + Redis + MinIO from **pre-built images** (not local `backend/`/`frontend/` source). For normal local work, run subprojects directly — do not use Docker as the default test harness.

## Common commands

```bash
# backend/
mvn spring-boot:run                 # run (port 8088, API docs at /doc.html)
mvn test                            # all tests
mvn test -Dtest=ClassName#method    # single test
mvn clean install

# frontend/  (imports use @ -> src/; TS is strict on unused vars)
npm run dev
npm run lint                        # auto-fixes
npm run build                       # vue-tsc -b && vite build — NOT a default verification step; see AGENTS.md

# sync_data/
mvn -DskipTests package && java -jar target/sync-data-1.0.0.jar --server.port=10456

# mcp_server/   (needs AICRM_TOKEN; AICRM_BASE_URL defaults to http://127.0.0.1:8088)
npm run build && npm start
```

There is no `application-test.yml`; any test that loads the Spring context uses `application.yml`, which points at deployed/shared infra — override via env vars or a local profile before running locally.

## Backend architecture

Layering is conventional `controller → service (impl) → mapper (MyBatis XML)`, but two cross-cutting systems shape almost every change:

**Multi-tenancy is automatic and pervasive.** `JwtAuthenticationTokenFilter` populates `TenantContextHolder`; `TenantLineInnerInterceptor` rewrites queries with a `tenant_id` predicate (running *before* data-permission and pagination); `MyMetaObjectHandler` stamps `tenantId` on insert. Consequences you must respect:
- Code paths without a JWT request context (registration, async tasks) **must** set the tenant context manually and clear it in `finally`.
- A small set of global tables are tenant-exempt (menus, `crm_tenant`, billing/pricing config, access/error logs, external-auth tables); cross-tenant queries use `@InterceptorIgnore(tenantLine = "true")`. See AGENTS.md for the exact list.
- Row-level **data permission** SQL lives only in `GlobalDataPermissionHandler` and currently maps just `Customer`/`Contact`/`Task`/`FollowUp`/`Knowledge` mappers — extend it when adding a protected business mapper.

**The AI chat subsystem** (`backend/src/main/java/com/kakarote/ai_crm/ai/`) is the product's core:
- `ManagerApplication` **excludes Spring AI auto-configuration on purpose.** Get chat clients from `DynamicChatClientProvider` — never inject an auto-configured Spring AI client. Clients are built per-tenant from runtime config.
- `/chat/send` is **SSE**: `ChatServiceImpl` saves the user message, assembles history + RAG + attachments, streams chunks, then persists the assistant (or error) message.
- `AiContextHolder` + `ContextPropagationConfig` carry user/tenant context across Reactor / Spring AI tool threads — reuse the existing clear methods when adding terminal paths.
- LLM tools live in `ai/tools/` (params are `String` by design, since tool-call args arrive as strings). Keep the split: concrete execution time → `ScheduleTools.createSchedule`; deadline-only/no-time → `TaskTools.createTask`.
- Providers/model capabilities register in `AiProviderRegistry` (default provider `dashscope`; tool/vision support inferred from model-name keywords). `ai/app/` defines selectable chat applications via `ChatApplicationRegistry`.

**IDs** are Snowflake `BIGINT` → Java `Long`; serialize as `String` toward the frontend/API where precision is at risk.

## 认证与会话内幕（Auth & session internals）

`## Backend architecture` 已说明每请求由 `JwtAuthenticationTokenFilter` 设置 `TenantContextHolder`。下面是会话、RBAC 与数据权限的完整机制（源码在 `config/security/`、`service/impl/DataPermissionServiceImpl.java`、`common/auth/DataPermissionHolder.java`）。

- **令牌与会话**：登录令牌是 UUID（`IdUtil.fastUUID()`），`LoginUser` 序列化为 JSON 存入 Redis，TTL = 配置项 `token.expireTime`（分钟）。请求头与前端 localStorage key 均为 `Manager-Token`。见 `config/security/service/TokenService.java`。
- **单点登录（默认）**：`token.multi-login-enabled` 默认 `false`。同一用户重新登录会写 kickout 标记并删除旧令牌，旧令牌的后续请求随即失效；置 `true` 放开多端并发登录。
- **每请求生命周期**：校验令牌 → 写入 Spring Security 上下文与 `TenantContextHolder` → 在 `finally` 中**同时**清理 `TenantContextHolder` 与 `DataPermissionHolder`（线程池复用时防 ThreadLocal 泄漏）。无请求上下文的代码（注册、异步任务）手动设租户时务必照此清理。
- **RBAC**：表为 `manager_user` / `manager_role` / `manager_user_role` / `manager_role_menu` / `manager_menu` / `manager_dept`。权限判定是**命令式**的 `PermissionService.hasPermission("module")` 或 `hasPermission("module:action")`，**不是** Spring `@PreAuthorize` 注解——新增受控接口要在业务层显式调用。
- **数据权限（行级）**：`DataPermissionService` 按角色解析 5 级范围（本人 / 本人+下属 / 本部门 / 本部门+子部门 / 全部），每请求懒加载进 `DataPermissionHolder`；WHERE 片段由 `GlobalDataPermissionHandler` 注入，**仅覆盖** `Customer`/`Contact`/`Task`/`FollowUp`/`Knowledge` 五个 mapper（新增受保护业务 mapper 需同步登记）。
- **叠加顺序**：同一次查询上，Spring Security（认证/接口权限）→ `TenantLineInnerInterceptor`（租户隔离）→ 数据权限（行级范围）依次生效。验证码用 AjCaptcha + Redis。

## AI 计费与额度（Billing & credits）

所有 AI 操作（对话、音频转写、知识检索、各类 AI 抽取）都经 `AiQuotaService`（`service/impl/AiQuotaService.java`）**两阶段计量**：

1. **调用前** `ensureQuotaAvailable(...)`：按**估算** token 预检额度，不足直接抛异常中止。
2. **调用后** `consumeResolvedTokens(...)`：按模型返回的**真实** usage 扣减，并向 `crm_ai_credit_record` 写一条不可变流水。

- **额度公式**：`credits = ceil(total_tokens × model_credit_multiplier / tokens_per_credit)`，每次至少 1 credit。
- **token 估算**（无 usage 时，`estimateTokens`）：CJK 字符按 1 token/字，其余字符按 `ceil(字符数 / 4)` 折算（约 0.25 token/字），历史每条消息额外 +4。
- **双桶余额扣减**（`CrmTenantServiceImpl.consumeCredits` → `CreditConsumeResult`）：**先扣赠送额度** `gift_credit_*`、不足再扣购买额度 `purchased_credit_*`（均在 `crm_tenant`，租户豁免表）。
- **是否计费看模型来源**：用户自带 key 的自定义模型（`model_source=custom`）**不计费**；系统模型一律计费。

| 表 / 服务 | 作用 |
| --- | --- |
| `crm_ai_billing_config`（`AiBillingConfigService`） | 全局单行，`tokens_per_credit` 默认 800 |
| `crm_ai_model_pricing`（`AiModelPricingService`） | 按 (provider, model) 的 `credit_multiplier` 倍率 |
| `crm_ai_credit_record`（`AiCreditRecordService`） | 扣减流水：prompt/completion tokens、倍率、gift/purchased 用量、前后余额、引用对象 |
| `crm_token_purchase_order`（`ITokenPurchaseService`） | 充值下单与微信/支付宝回调 |

## Schema & dynamic config

- Base schema: `backend/src/main/resources/sql/crm_init_postgres.sql`. Incremental changes go in **new** `backend/src/main/resources/db/migration/V*.sql` Flyway files (current head is V43). Merges frequently collide on version numbers — renumber your migration to the next free version after the current max and rename the file accordingly.
- Tenant-level dynamic settings live in `crm_system_config`, cached in Redis for 30 min — always update through `SystemConfigServiceImpl` so the Redis key and the tenant ChatClient both refresh.
- WeKnora per-tenant API keys / knowledge-base IDs live in `crm_tenant`, lazily created by `WeKnoraClient.getOrCreateTenantContext` — do not hardcode them under `weknora.*`.

## 第三方集成：企业微信 / 腾讯会议 / 邮箱

三个集成共享一套模式，先掌握模式再看差异：

- **共同模式**：凭证按租户加密入库（`SecretTextCipher`）；增量同步用游标表（`*_sync_cursor`）；webhook 回调用 AES-GCM 解密；OAuth state/ticket 存 Redis（短 TTL）；外部实体（外部联系人/会议/邮件）可绑定到 CRM `customer`。

| 集成 | 入口 / 关键类 | 主要表 | 备注 |
| --- | --- | --- | --- |
| 企业微信 | `WecomOpenPlatformService`、`WecomController` / `WecomOpenController`、`WecomCallbackCryptoService`、`WecomTokenService`；配置 `WecomOpenPlatformProperties` | `crm_wecom_*`（V37/V41） | 第三方应用授权流；支持"直接安装"——首次授权无绑定时自动建租户+用户 |
| 腾讯会议 | `TencentMeetingController` / `CustomerTencentMeetingController`、`TencentMeetingOAuthService`（用户级 OAuth）、`TencentMeetingSyncServiceImpl`（游标增量）、`TencentMeetingWebhookService` | `crm_tencent_meeting_*`（含 participant/recording/transcript_segment，V38–V40） | 会议可绑 `customer`，并 AI 生成纪要/待办 |
| 邮箱 | `MailController`、`MailServiceImpl`；配置 `MailIntegrationProperties` / `CloudMailProperties` | `crm_mail_*`（V28） | IMAP/SMTP/OAuth 账号；`body_sync_mode` = summary/full/metadata；AI 抽取 action items 与回复截止时间 |

## 身份联合与 OIDC

- **外部登录（External Auth）**：`ExternalAuthController` + `ExternalAuthServiceImpl` 支持多 provider 登录。回调后把 `ExternalLoginTicket` 存 Redis，客户端再用 ticket 换 JWT（避免在 redirect URL 暴露用户数据）；首次登录且无绑定时由 `RegistrationService` 自动建租户。两张表都是**租户豁免**：`crm_external_auth_identity`（`(provider, subject)` 全局唯一 → tenant+user）、`crm_external_tenant_binding`（外部企业 → CRM 租户）。
- **CRM 自身作为 OIDC Provider**：`OidcController` 暴露 `/.well-known/openid-configuration`、`/oauth2/authorize|token|userinfo|jwks`，以及给 MinIO 控制台用的 `/oauth2/minio-sso`。session 可走 Cookie 或 URL 参数 `session_token`（跨域时用，MinIO 即如此）；issuer 按请求动态计算（适配反代）；不发 refresh token。配置见 `OidcConfig`。

## 业务模块：自定义字段 / 项目 / 关系

- **自定义字段 / 动态 schema**：`CustomFieldServiceImpl` + `DynamicSchemaServiceImpl` 在运行时真实改表——`addColumn` 执行 `ALTER TABLE ... ADD COLUMN`、`dropColumn` 执行 `DROP COLUMN`，用 `information_schema.columns` 判断列是否存在。**字段创建后不能改类型**（需删除重建）；`is_unique` 会建唯一索引；全部按租户隔离。元数据表 `crm_custom_field`、选项池 `crm_custom_field_pool`（**租户豁免**）、排序 `crm_custom_field_sort`。
- **项目（看板）**：结构为 project → lane（看板列）→ task（`ProjectServiceImpl` / `ProjectController`，V34/V35/V42）。**双模 task**：`crm_task` 新增 `project_id` / `lane_id` / `ai_source_text`——带 `project_id` 的是看板任务，不带则是独立任务，同一套 service 同时处理。成员权限存 `crm_project_member.permissions`（JSON），**独立于全局 RBAC**；AI 指令由 `ProjectAiCommandParser` 解析（期望 LLM 返回 JSON）；项目对话上下文在 `crm_project_chat_*`。
- **关系（Relation）**：`RelationServiceImpl` / `RelationController`（V30）。轻量的"人"（决策人/影响人等），区别于正式 `contact`：可来源于 customer/contact（`source_customer_id` / `source_contact_id`），可被多个 task/schedule/follow_up/knowledge/chat_session 关联；`company` 是文本字段而非外键。

## Frontend notes

- `utils/request.ts` unwraps the backend `Result` only on `code === 0`. Business `302` = not logged in, `401` = no permission.
- Auth token header and localStorage key are both `Manager-Token`.
- Use the shared `useResponsive()` singleton for mobile/tablet state. Two Vite configs exist (`vite.config.ts` **and** `vite.config.js`) and `.js` wins — keep them in sync. Dev proxies: `/crmapi` → backend, `/syncapi` → sync service.

## 前端架构补充（Frontend deep-dive）

- **路由静态、权限动态**：`router/index.ts` 用 `createWebHashHistory`，路由是静态 `RouteRecordRaw`，每项带 `meta.permission`（如 `customer:view`、`addressBook:list`）与 `meta.requiresAuth`。`router.beforeEach`：无 token → `/login?redirect=<path>`；首次进入拉 `userStore.fetchUserInfo()`（并行 `/auth/userInfo` 与 `/managerRole/auth`）；`userStore.hasPermission("module:action")` 分层校验，拒绝则回 `/chat`。
- **Chat store 是前端核心**（`stores/chat.ts`，体量很大）：支持**并发流**（每个 session 一个 `AbortController`，`streamingTasks` 按 sessionId 索引）；内置 6 个 app context（`general` / `crm` / `project` / `knowledge` / `address_book` / `relation`）；任意实体都能开带上下文的会话（`openCustomerChat` / `openEmployeeChat` / `openRelationChat` / `openProjectChat` / `openProjectTaskChat`）；草稿、所选模型、所选 app 均持久化到 localStorage。
- **SSE 消费**：`api/chat.ts` 的 `sendMessageStream` 用原生 `fetch()` + `response.body.getReader()` + `TextDecoder` 处理 `/chat/send` 的 `text/event-stream`（**不是** Axios），按 `\n\n` 切事件、解析 `data:` 行。
- **侧边栏模块排序**：`MainLayout.vue` 支持拖拽排序，持久化到用户偏好 `sidebarModuleOrder`（后端 `manager_user_ui_preferences`，V43）。
- **API 约定**：`api/*.ts` 统一用 `@/utils/request` 的 `get/post/put/del/upload/download`（响应已按 `code` 解包，见 `## Frontend notes`）；可选请求传 `{ silentError: true }` 抑制错误提示。

## Spec-driven changes

`openspec/` (schema `spec-driven`) holds change proposals under `openspec/changes/`. Project-specific agent skills live in `.codex/skills/` and `docs/superpowers/`.

## 卫星子项目：sync_data 与 mcp_server

- **`sync_data` 不只是单向迁移**（修正上文 "one-way migrator" 的简化说法）：
  - **全量同步**：`FullSyncService` + `MappingRepository`。`sync_mapping` 表把来源 `(system+table+company_id+id)` 映射到目标 `(table+id)`，新目标 ID 由 `SnowflakeIdGenerator` 分配并批量 `getOrCreateTargetIds`。`--sync.truncate-before-sync=true` **只删** `sync_mapping` 记录过的目标行。PG JDBC 自动追加 `reWriteBatchedInserts=true`（见 `config/DataSourceConfig.java`）。
  - **增量双向同步**：`incremental/` + `mq/` 经 **RocketMQ**，`SyncDirection` = `CRM_TO_AICRM` / `AICRM_TO_CRM`，`IncrementalSyncController` 收发事件，按公司绑定 opt-in。
  - DSN 来自 `SYNC_OLD_*` / `SYNC_TARGET_*`；故意绕过 backend 的 service/租户/权限，直连 JDBC 读写。
- **`mcp_server`**：单文件 `src/index.ts`（约 285 行），stdio transport，暴露 **9 个只读工具**——`global_search`、`list_customers`、`get_customer_detail`、`list_customer_contacts`、`list_customer_followups`、`list_my_tasks`、`list_my_schedules`、`search_knowledge`、`get_knowledge_detail`，全部转调 backend REST。鉴权用环境变量 `AICRM_TOKEN` 经 `Manager-Token` 头透传，backend 返回 `code !== 0` 即抛错；入参用 Zod 校验。定位是**纯适配层**：无直连 DB、无业务逻辑、暂无写操作（约定见 `mcp_server/MCP_DEVELOPMENT_STANDARD.md`）。
