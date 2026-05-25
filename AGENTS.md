# AGENTS.md

## Project Boundaries
- No root build or test runner exists; run commands inside the subproject you are changing.
- `backend/` is the main AI CRM Spring Boot app: Java 21, entrypoint `com.kakarote.ai_crm.ManagerApplication`, default port `8088`.
- `frontend/` is the Vue 3/Vite app: entry `src/main.ts`, hash router, Element Plus, Pinia, Tailwind.
- `sync_data/` is a separate Spring Boot HTTP service for old WK CRM MySQL -> AI CRM PostgreSQL sync, default port `10456`; it does not use backend Services/auth/tenant interceptors and does not sync on startup.
- `mcp_server/` is a standalone stdio MCP server that calls backend HTTP APIs; it intentionally does not call or expose `sync_data`.
- `CLAUDE.md` has more historical detail, but verify claims against executable config before copying them here.

## Commands
- Backend (`backend/`): `mvn compile`, `mvn test`, `mvn test -Dtest=ClassName`, `mvn test -Dtest=ClassName#method`, `mvn spring-boot:run`, `mvn clean install`.
- Frontend (`frontend/`): `npm install`, `npm run dev`, `npm run build` (`vue-tsc -b && vite build`), `npm run lint` (auto-fixes).
- Sync service (`sync_data/`): `mvn -DskipTests package`; run with `java -jar target\sync-data-1.0.0.jar --server.port=10456`.
- MCP server (`mcp_server/`): `npm install`, `npm run build`, `npm start`; set `AICRM_TOKEN` for authenticated calls, with `AICRM_BASE_URL` defaulting to `http://127.0.0.1:8088` and `AICRM_TOKEN_HEADER` to `Manager-Token`.
- Docker (`docker/`): `docker-compose up -d` uses prebuilt images, not local `backend/` or `frontend/` source; it starts CRM on `8088` plus WeKnora, PostgreSQL, Redis, and MinIO.

## Runtime Config
- Backend defaults live in `backend/src/main/resources/application.yml`; Flyway is enabled and the file points at deployed/test PG/Redis/MinIO/WeKnora defaults, so use env vars or a local Spring profile override before running locally.
- There is no `application-test.yml`; any Spring test that loads the context uses `application.yml` unless the test overrides properties.
- Frontend dev env currently sets `VITE_API_BASE_URL=http://127.0.0.1:8088`, so CRM requests bypass the `/crmapi` Vite proxy; sync requests use `VITE_SYNC_API_BASE_URL=/syncapi` and proxy to `127.0.0.1:10456` by default.
- `frontend/vite.config.ts` and `frontend/vite.config.js` both exist; Vite finds `.js` first, so keep them in sync or remove the duplicate deliberately when changing Vite config.
- Auth token header and localStorage key are both `Manager-Token`.
- Use `@` for frontend imports from `frontend/src`; TypeScript is strict with unused locals/parameters, and ESLint removes unused imports.

## Backend Gotchas
- Persisted IDs are Snowflake `BIGINT`; use Java `Long` and treat frontend/API IDs as strings when precision matters.
- Tenant isolation is automatic: `TenantLineInnerInterceptor` runs before data permission and pagination, `JwtAuthenticationTokenFilter` sets/clears `TenantContextHolder`, and `MyMetaObjectHandler` fills `tenantId` on insert.
- Code that runs without JWT request context, such as registration or async work, must set tenant context explicitly and clear it in `finally`.
- Only use `@InterceptorIgnore(tenantLine = "true")` for deliberate cross-tenant queries. Tenant-ignored tables are `manager_menu`, `manager_role_menu`, `crm_tenant`, `crm_custom_field_pool`, and `crm_ai_model_pricing`.
- Data permission SQL is centralized in `GlobalDataPermissionHandler` and currently maps only `CustomerMapper`, `ContactMapper`, `TaskMapper`, `FollowUpMapper`, and `KnowledgeMapper`; update it when adding a protected business mapper.
- `manager_user_role` has no `del_flag`; do not add `del_flag = 0` to queries touching it.
- MyBatis XML queries that select joined or computed columns must return a VO, not a PO, or the extra fields are dropped.
- `ManagerApplication` excludes Spring AI auto-configuration; use `DynamicChatClientProvider` instead of injecting auto-configured Spring AI clients.
- Avoid new Service-to-Service cycles; existing fixes use Mapper injection or `@Lazy`.

## Schema And Config Data
- `db/migration/V1__baseline.sql` is a Flyway baseline stub; base schema lives in `backend/src/main/resources/sql/crm_init_postgres.sql`, and incremental schema changes belong in new `backend/src/main/resources/db/migration/V*.sql` files.
- Manual tenant bootstrap SQL still exists at `backend/src/main/resources/sql/saas_tenant_migration.sql`; do not assume it has run in every database.
- Tenant-scoped dynamic settings live in `crm_system_config` and are cached in Redis for 30 minutes; update through `SystemConfigServiceImpl` paths so Redis keys and tenant ChatClients refresh.
- WeKnora tenant API key and knowledge base ID are stored on `crm_tenant` and lazily created by `WeKnoraClient.getOrCreateTenantContext`, not hard-coded in `weknora.*`.
- `sync_data` DSNs come from `SYNC_OLD_*` and `SYNC_TARGET_*`; `--sync.truncate-before-sync=true` only deletes target rows recorded in `sync_mapping`.

## AI Chat And Tools
- `/chat/send` is SSE; `ChatServiceImpl` saves the user message, builds history/RAG/attachments, streams chunks, then saves the assistant or error message.
- `AiContextHolder` plus `ContextPropagationConfig` carries user and tenant context into Reactor/Spring AI tool threads; use the existing clear methods when adding terminal paths.
- AI tool classes live in `backend/src/main/java/com/kakarote/ai_crm/ai/tools`; tool parameters are intentionally `String` because LLM tool calls pass strings.
- Preserve the task/schedule split: specific execution time -> `ScheduleTools.createSchedule`; only a deadline or no time -> `TaskTools.createTask`.
- AI providers and model capabilities are registered in `AiProviderRegistry`; default provider is `dashscope`, with tool/vision support inferred from model-name keywords.

## Frontend Notes
- `utils/request.ts` unwraps backend `Result` only when `code === 0`; business `302` means not logged in and `401` means no permission.
- Use the shared `useResponsive()` singleton for mobile/tablet state; existing dialogs and drawers already follow mobile size/fullscreen patterns.
- All tips/tooltips should match the expand/collapse sidebar tooltip style: black background, white text, 8px radius, 6px 12px padding, 13px medium text, and compact shadow. For Element Plus tooltips, prefer the shared sidebar-like popper class/pattern instead of the default light tooltip.
- `src/vite-env.d.ts` imports `@vue-office/*/lib/v3/index.js` directly because those packages may miss `lib/index.js`; preserve those paths unless fixing that dependency.
- `npm run build` may emit a large Element Plus chunk warning; treat it as pre-existing unless the task is bundle-size work.

## Tests And Generated Files
- Do not run `npm run build` as the default post-task verification step after finishing changes; only run it when the user explicitly asks or when the task specifically requires a production build check.
- There are currently no backend or sync test files; `backend/.gitignore` ignores `src/test/*`, so adjust or verify ignore rules before adding backend tests you expect to commit.
- Do not edit ignored/generated outputs: `frontend/dist`, `mcp_server/dist`, any `target/`, and frontend `*.tsbuildinfo`.
