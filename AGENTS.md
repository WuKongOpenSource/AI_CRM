# AGENTS.md

## 项目边界
- 根目录没有统一的构建或测试运行器；请在你正在修改的子项目内运行命令。
- `backend/` 是主要的 AI CRM Spring Boot 应用：使用 Java 21，入口为 `com.kakarote.ai_crm.ManagerApplication`，默认端口为 `8088`。
- `frontend/` 是 Vue 3/Vite 应用：入口为 `src/main.ts`，使用 hash router、Element Plus、Pinia 和 Tailwind。
- `sync_data/` 是独立的 Spring Boot HTTP 服务，用于将旧 WK CRM MySQL 数据同步到 AI CRM PostgreSQL，默认端口为 `10456`；它不使用 backend 的 Services、认证或租户拦截器，也不会在启动时自动同步。
- `mcp_server/` 是独立的 stdio MCP 服务器，会调用 backend HTTP API；它有意不调用也不暴露 `sync_data`。
- `CLAUDE.md` 是面向架构的综合参考（仓库结构、命令、认证与会话、AI 计费、第三方集成、身份联合/OIDC、业务模块、前端架构、卫星子项目等），与本文件互补；但在把其中说法复制到这里前，请先对照可执行配置进行验证。

## 命令
- Backend (`backend/`)：`mvn compile`、`mvn test`、`mvn test -Dtest=ClassName`、`mvn test -Dtest=ClassName#method`、`mvn spring-boot:run`、`mvn clean install`。
- Frontend (`frontend/`)：`npm install`、`npm run dev`、`npm run build` (`vue-tsc -b && vite build`)、`npm run lint`（会自动修复）。
- Sync service (`sync_data/`)：`mvn -DskipTests package`；运行方式为 `java -jar target\sync-data-1.0.0.jar --server.port=10456`。
- MCP server (`mcp_server/`)：`npm install`、`npm run build`、`npm start`；认证调用需要设置 `AICRM_TOKEN`，`AICRM_BASE_URL` 默认是 `http://127.0.0.1:8088`，`AICRM_TOKEN_HEADER` 默认是 `Manager-Token`。
- 本地测试时，优先直接启动相关本地子项目（`mvn spring-boot:run`、`npm run dev`、sync service 的 Maven/JAR 命令，或 MCP 的 npm 脚本）。除非用户明确要求，或需要验证打包后的 Docker 部署，否则不要用 Docker 作为普通本地测试启动方式。
- Docker (`docker/`)：`docker-compose up -d` 会在 `8088` 启动 CRM，同时启动 WeKnora、PostgreSQL、Redis 和 MinIO。CRM/frontend 使用预构建镜像，不使用本地 `backend/` 或 `frontend/` 源码；`docreader` 同时配置了 `image` 和 `build`。

## 运行时配置
- Backend 默认配置位于 `backend/src/main/resources/application.yml`；Flyway 已启用，并且该文件指向已部署/测试用的 PG、Redis、MinIO、WeKnora 默认值，所以本地运行前请使用环境变量或本地 Spring profile 覆盖配置。
- 没有 `application-test.yml`；任何加载 Spring 上下文的测试都会使用 `application.yml`，除非测试自己覆盖属性。
- Frontend 开发环境目前设置 `VITE_API_BASE_URL=/crmapi`，因此 CRM 请求走 `/crmapi` Vite 代理；同步请求使用 `VITE_SYNC_API_BASE_URL=/syncapi`，默认代理到 `127.0.0.1:10456`。
- `frontend/vite.config.ts` 和 `frontend/vite.config.js` 同时存在；Vite 会先找到 `.js`，所以修改 Vite 配置时要保持两者同步，或有意移除重复文件。
- 认证 token 请求头和 localStorage key 都是 `Manager-Token`。
- Frontend 从 `frontend/src` 导入时使用 `@`；TypeScript 对未使用的局部变量/参数要求严格，ESLint 会移除未使用的 import。

## Backend 注意事项
- 持久化 ID 是 Snowflake `BIGINT`；Java 中使用 `Long`，当前端/API ID 有精度风险时按字符串处理。
- 租户隔离是自动完成的：`TenantLineInnerInterceptor` 先于数据权限和分页运行，`JwtAuthenticationTokenFilter` 会设置/清理 `TenantContextHolder`，`MyMetaObjectHandler` 会在插入时填充 `tenantId`。
- 在没有 JWT 请求上下文的代码中，例如注册或异步任务，必须显式设置租户上下文，并在 `finally` 中清理。
- 只有在有意执行跨租户查询时才使用 `@InterceptorIgnore(tenantLine = "true")`。忽略租户的表包括 `manager_menu`、`manager_role_menu`、`crm_tenant`、`crm_custom_field_pool`、`crm_ai_model_pricing`、`crm_ai_billing_config`、`crm_access_log`、`crm_error_log`、`crm_external_auth_identity` 和 `crm_external_tenant_binding`。
- 数据权限 SQL 集中在 `GlobalDataPermissionHandler` 中，目前只映射 `CustomerMapper`、`ContactMapper`、`TaskMapper`、`FollowUpMapper` 和 `KnowledgeMapper`；添加受保护的业务 mapper 时需要同步更新。
- `manager_user_role` 没有 `del_flag`；查询该表时不要添加 `del_flag = 0`。
- MyBatis XML 查询如果选择了关联或计算出来的列，必须返回 VO，而不是 PO，否则额外字段会被丢弃。
- `ManagerApplication` 排除了 Spring AI 自动配置；请使用 `DynamicChatClientProvider`，不要注入自动配置的 Spring AI client。
- 避免新增 Service 到 Service 的循环依赖；已有修复方式使用 Mapper 注入或 `@Lazy`。

## Schema 和配置数据
- `db/migration/V1__baseline.sql` 是 Flyway baseline stub；基础 schema 位于 `backend/src/main/resources/sql/crm_init_postgres.sql`，增量 schema 变更应放入新的 `backend/src/main/resources/db/migration/V*.sql` 文件。
- 合并代码后，Flyway 版本文件可能与其他分支冲突或版本号重复；如果遇到冲突，需要修改本次提交代码引入的 DB 版本，改为当前最高版本之后的下一个可用版本，并同步更新迁移文件名。
- 手动租户初始化 SQL 仍位于 `backend/src/main/resources/sql/saas_tenant_migration.sql`；不要假设它已经在每个数据库中执行过。
- 租户级动态配置保存在 `crm_system_config`，并在 Redis 中缓存 30 分钟；请通过 `SystemConfigServiceImpl` 路径更新，以便 Redis key 和租户 ChatClient 刷新。
- WeKnora 租户 API key 和知识库 ID 保存在 `crm_tenant`，并由 `WeKnoraClient.getOrCreateTenantContext` 懒创建，不要硬编码在 `weknora.*` 中。
- `sync_data` DSN 来自 `SYNC_OLD_*` 和 `SYNC_TARGET_*`；`--sync.truncate-before-sync=true` 只会删除记录在 `sync_mapping` 中的目标行。

## AI 聊天和工具
- `/chat/send` 是 SSE；`ChatServiceImpl` 会保存用户消息，构建历史/RAG/附件，流式输出 chunk，然后保存助手消息或错误消息。
- `AiContextHolder` 与 `ContextPropagationConfig` 会把用户和租户上下文带入 Reactor/Spring AI 工具线程；添加终止路径时请使用现有 clear 方法。
- AI 工具类位于 `backend/src/main/java/com/kakarote/ai_crm/ai/tools`；工具参数有意使用 `String`，因为 LLM 工具调用传入的是字符串。
- 保持任务/日程的拆分：有具体执行时间 -> `ScheduleTools.createSchedule`；只有截止时间或没有时间 -> `TaskTools.createTask`。
- AI provider 和模型能力注册在 `AiProviderRegistry` 中；默认 provider 是 `dashscope`，工具/视觉支持根据模型名关键字推断。

## Frontend 注意事项
- `utils/request.ts` 只在 `code === 0` 时解包 backend `Result`；业务 `302` 表示未登录，`401` 表示无权限。
- 使用共享的 `useResponsive()` 单例处理移动端/平板状态；现有 dialog 和 drawer 已遵循移动端尺寸/fullscreen 模式。
- 所有 tips/tooltips 都应匹配侧边栏展开/折叠 tooltip 样式：黑色背景、白色文字、8px 圆角、6px 12px 内边距、13px medium 字体，以及紧凑阴影。Element Plus tooltip 优先使用共享的侧边栏风格 popper class/pattern，而不是默认浅色 tooltip。
- `src/vite-env.d.ts` 会直接导入 `@vue-office/*/lib/v3/index.js`，因为这些包可能缺少 `lib/index.js`；除非是在修复该依赖，否则保留这些路径。
- `npm run build` 可能会输出较大的 Element Plus chunk 警告；除非任务是 bundle size 相关，否则把它视为既有情况。

## 测试和生成文件
- 完成修改后的默认验证步骤不要运行 `npm run build`；只有在用户明确要求，或任务确实需要生产构建检查时才运行。
- 已有 backend 和 `sync_data` 测试，但根目录 `.gitignore` 有宽泛的 `test` 规则；新增 backend 或 `sync_data` 测试文件时，提交前可能需要添加显式 unignore 规则。
- 不要编辑被忽略/生成的输出：`frontend/dist`、`mcp_server/dist`、任何 `target/`，以及 frontend 的 `*.tsbuildinfo`。
