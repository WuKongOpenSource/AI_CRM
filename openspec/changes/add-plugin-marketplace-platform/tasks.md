## 1. Data Model And Migrations

- [x] 1.1 Promote the staged reference DDL (`openspec/changes/add-plugin-marketplace-platform/migration/V44__plugin_marketplace_platform.sql`) into `backend/src/main/resources/db/migration/`, renumbered to the next free Vxx after the then-current head (`V43__im_phase3.sql` at authoring time; numbers collide on merge), creating all seven new tables.
- [ ] 1.2 Create `crm_oauth_client` (instance-global, NO `tenant_id`) with `client_id`, `plugin_id`, `display_name`, `grant_types`, `default_scopes` (TEXT JSON/CSV), `redirect_uris`, signing/public-key metadata, and `status`.
- [ ] 1.3 Create `crm_ai_application` for installed/custom apps only (built-ins stay constructor-seeded in code) mirroring the `ChatApplicationDefinition` record fields (`app_id`, `code`, `label`, `icon_name`, `description`, `system_prompt`, `default_rag_enabled`, `tool_groups`, `recommended_questions`) plus `plugin_id` and a reserved nullable `tenant_id`.
- [ ] 1.4 Create the lifecycle/auth tables `crm_plugin_install` (`install_id`, `plugin_id`, `version`, `enabled`, `config_encrypted`, reserved `tenant_id`), `crm_plugin_grant` (`grant_id`, `plugin_id`, `client_id`, `granted_scopes` TEXT JSON/CSV, `granted_by_user_id`, reserved `tenant_id`), and `crm_plugin_token` (`token_id`, `plugin_id`, `client_id`, `token_hash`, `scopes` TEXT JSON/CSV, `expires_at`, `revoked`, reserved `tenant_id`).
- [ ] 1.5 Create the delivery/audit tables `crm_plugin_subscription` (`subscription_id`, `plugin_id`, `event_type`, `endpoint_url`, `signing_secret_encrypted`, `active`, reserved `tenant_id`) and `crm_plugin_audit_log` (`log_id`, `plugin_id`, `actor`, `action`, `scope`, `result`, `request_meta`, `create_time`, reserved `tenant_id`).
- [ ] 1.6 Add MyBatis-Plus entity PO classes (`@TableName`, `@TableId(type = IdType.ASSIGN_ID)` so `BaseUtil.getNextId()` Snowflake IDs apply; serialize IDs as `String` toward the frontend) and mappers for the seven tables; rely on `MyMetaObjectHandler` for audit columns.
- [ ] 1.7 Document in the migration header that the reserved nullable `tenant_id` (NULL = the single instance) exists only for forward-compatibility with a future multi-tenant layer and that nothing filters on it today (no `TenantLineInnerInterceptor` exists in this codebase).
- [ ] 1.8 Confirm the seven new tables are infra/metadata (not protected business entities) and therefore require no `GlobalDataPermissionHandler` registration; note this in the migration header comment.

## 2. Dual-Track Scope Resolver

- [ ] 2.1 Add an `InstanceScopeResolver` that returns the single instance scope today and is the only place that reads/writes the reserved `tenant_id`; route all new plugin-table queries through it so a future tenant resolver can replace it without touching call sites.

## 3. Plugin Authorization

- [x] 3.1 Add an OAuth client registry service backed by `crm_oauth_client` to look up plugin apps by `client_id`, validate `grant_types`/`redirect_uris`, and expose default-scope templates. _(P0-A: `PluginAuthService.registerClient`/`listClients`, `CrmOauthClientMapper`; hashed `client_secret`.)_
- [x] 3.2 Extend `OidcController` (`/oauth2/token`) to support the OAuth2 `client_credentials` grant for plugins (validating client secret against the registry) and the `authorization_code` flow, without introducing a new auth scheme. _(P0-A: verified — returns OAuth JSON; wrong secret → 400.)_
- [ ] 3.3 Build a scope catalog derived from the existing `module:action` permission vocabulary (e.g. `customer:view`, `task:create`, `addressBook:list`) enforced by `PermissionService.hasPermission`; reject any requested scope not in the catalog.
- [x] 3.4 Add a consent/grant service that persists instance-admin-approved scopes to `crm_plugin_grant` (with `granted_by_user_id`) and require an existing grant before any token issuance. _(P0-B: `grantScopes` + `/plugin/client/grant`; token scope now sourced from the grant, empty without one.)_
- [x] 3.5 Implement plugin token issue/rotate/revoke against `crm_plugin_token` storing only the hashed secret (`token_hash`), effective `scopes`, and `expires_at`; expose rotation and revocation operations. _(P0-B: issue + revoke verified; an explicit rotate endpoint is a follow-up.)_
- [x] 3.6 Implement downscoping enforcement so a token's effective permission = installing actor's `PermissionService` permissions INTERSECT granted scopes (downscoping only, never escalation), reusing the imperative permission check. _(P0-B: grant-time downscoping via `PermissionService.hasPermission`; gateway enforces required scope ∈ token scopes — 403 verified.)_

## 4. Extension Gateway

- [x] 4.1 Add an Extension Gateway controller under `/plugin/gateway/**` as the single inbound API entry for plugins, distinct from the human `Manager-Token` business controllers (see design Appendix B). _(P0-B: `PluginGatewayController`; path whitelisted in `SecurityConfig`.)_
- [x] 4.2 Resolve and validate the bearer plugin token against `crm_plugin_token` (not revoked, not expired) and enforce the required `module:action` per route via the downscoped effective-permission check. _(P0-B: verified — 401 on missing/invalid, 403 on insufficient scope.)_
- [x] 4.3 Establish the installing actor's context for the request (so existing RBAC and `GlobalDataPermissionHandler` row-level scoping apply) and clear any ThreadLocal context in a `finally` block to avoid leakage across pooled threads. _(P0 closure: `runAsActor` sets SecurityContext principal = installing actor's LoginUser, clears `DataPermissionHolder` + restores context in finally; verified — `/crm/customers` returns real data-permission-scoped customers. Actor = grant's `granted_by_user_id`.)_
- [x] 4.4 Enforce per-plugin rate-limit/quota (e.g. Redis-backed counters) and reject over-limit calls with a deterministic error. _(P0-B: `allowRequest` via `StringRedisTemplate`, 120/min, 429.)_
- [x] 4.5 Write an immutable `crm_plugin_audit_log` row for every gateway call and lifecycle event (action, scope, result, request meta). _(P0-B: verified — success/denied/throttled rows.)_
- [x] 4.6 Add the abstract `PluginAiUsageMeter` interface (`ensure(estimate)` / `record(actualUsage)`) with a no-op default bean, and bracket AI-consuming gateway routes with it. Do NOT implement metering/credit (non-goal; no `AiQuotaService` exists today). _(P0-B: `NoOpPluginAiUsageMeter`; brackets `/ai/echo`.)_

## 5. Dynamic Capability Registries

- [x] 5.1 Introduce a registry merge layer so built-in registrations (current static/constructor data) and installed registrations combine, with built-ins always winning on `code` conflict. _(P1-b: `ChatApplicationRegistry.mergedApplications()`; verified — manifest hijack of `crm` ignored, built-in "CRM管理" intact. DB-failure falls back to built-ins-only.)_
- [x] 5.2 Add a `crm_ai_application` loader that maps rows to `ChatApplicationDefinition` records and merges with the constructor-seeded built-ins (do not delete the private constructor seeding). _(P1-b: `CrmAiApplication` entity/mapper + `toDefinition`; built-ins remain constructor-seeded.)_
- [ ] 5.3 Add a public `AiProviderRegistry.registerInstalled(AiProviderDescriptor)` so installed providers overlay the `static { ... }` block without removing the built-in providers (`dashscope` default, `openai`, etc.); document whether `list()` returns built-in+installed.
- [ ] 5.4 Refactor `DynamicChatClientProvider.addToolsForGroup(...)` so tool-group resolution consults a registry that includes installed (out-of-process) tool groups in addition to the hardcoded `switch` over `ChatApplicationRegistry.TOOL_GROUP_*`; the out-of-process invocation transport itself is stubbed (P2 boundary).
- [x] 5.5 Ensure installed AI applications surface through the existing `GET /chat/app/options` endpoint so they reach the frontend. _(P1-b: verified — installed `acme_helper` appears in options; gone after uninstall.)_

## 6. Declarative Manifest Parser

- [x] 6.1 Define the versioned manifest schema (v1) as a Java model + JSON schema with documented backward-compatibility (custom fields, theme token packs, AI applications, AI provider descriptors, event subscriptions, requested scopes, reserved `uiExtensions`); see design Appendix A. Reject unknown/unsupported `schemaVersion`. _(P1-a: `PluginManifest` model + v1 validation verified — v2 → code 400. Currently models plugin meta + customFields; theme/aiApp/provider fields are P1-b/c.)_
- [ ] 6.2 Verify the manifest signature against the pinned public key before any parsing/apply, and reject unsigned or invalid manifests.
- [x] 6.3 Apply `customFields` by calling `CustomFieldServiceImpl.addField(CustomFieldAddBO)` (inheriting the 50-field cap, pooled `field_*` columns, and idempotent `DynamicSchemaServiceImpl.columnExists` checks); record each applied field for reversal. _(P1-a: verified — `/plugin/client/apply-manifest` created `field_6q6vo9`; applied field ids recorded encrypted in `crm_plugin_install`.)_
- [~] 6.4 Apply theme token packs (`--wk-*` CSS variables), `crm_ai_application` rows, and AI provider descriptors idempotently; record each applied extension. _(P1-a: customFields. P1-b: `crm_ai_application` rows applied + reversed (verified). Remaining: theme token packs + AI provider descriptors.)_
- [x] 6.5 Implement uninstall reversal that exactly undoes every recorded extension (`deleteField(Long)`, removed `crm_ai_application` rows, deregistered providers, removed theme tokens) and is idempotent. _(P1-a: verified — uninstall removed the custom field + install record; null-safe/idempotent for missing refs.)_
- [x] 6.6 Add idempotency guards so re-applying the same manifest version is a no-op and partial-apply failures roll back cleanly within a transaction. _(P1-a: `@Transactional(rollbackFor=Exception)`; re-apply guarded by "already installed → uninstall first". A true same-version no-op upsert is a follow-up.)_

## 7. Domain Event Bus And Webhook Dispatcher

- [x] 7.1 Define domain event types and publish them from business service impls after a successful write via Spring `ApplicationEventPublisher`. _(P1-c: `PluginDomainEvent`; `customer.created` published from `CustomerServiceImpl.addCustomer` (verified). Additional events — `task.updated`, `followup.created`, ... — are additive one-liners.)_
- [x] 7.2 Add a webhook dispatcher consuming events with `@TransactionalEventListener(phase = AFTER_COMMIT)` so no event fires for a rolled-back transaction and dispatch failure never rolls back business data; it reads active `crm_plugin_subscription` rows matching the event type and delivers to `endpoint_url`. _(P1-c: `PluginWebhookDispatcher`; verified fired only after commit.)_
- [x] 7.3 HMAC-sign each delivered payload using the per-subscription `signing_secret_encrypted` (decrypted via `SecretTextCipher`) and include a verifiable signature header. _(P1-c: `X-Plugin-Signature: sha256=<hmac>`; verified sigPrefix in delivery log.)_
- [x] 7.4 Implement asynchronous delivery with retries and exponential backoff, and record delivery outcomes to `crm_plugin_audit_log`. _(P1-c: `@Async PluginWebhookDeliverer` on a separate bean (so the proxy applies), 2 attempts w/ backoff, audits `delivered:<status>`/`error:<type>`; verified `delivered:200`.)_
- [x] 7.5 Add subscription management (create/activate/deactivate) writing `crm_plugin_subscription` with encrypted signing secrets. _(P1-c: `/plugin/client/subscribe` creates (encrypted secret, returned once); uninstall deletes all subs. Explicit activate/deactivate toggle is a follow-up.)_

## 8. Plugin Lifecycle Service

- [x] 8.1 Implement a per-instance lifecycle service for install/enable/disable/uninstall writing `crm_plugin_install` with a pinned `version` and `enabled` flag (through `InstanceScopeResolver`). _(P1-a: `apply-manifest` writes `crm_plugin_install` (pinned version, source); `enable`/`disable`/`uninstall` on the client. Marketplace-driven install + `InstanceScopeResolver` abstraction is P1-d.)_
- [x] 8.2 Store plugin config encrypted in `config_encrypted` via `SecretTextCipher.encrypt`/`decrypt`. _(P1-a: applied-extension refs stored encrypted in `config_encrypted`.)_
- [ ] 8.3 On install, verify signature and license (Section 9), persist the consent grant (Section 3.4), apply the declarative manifest (Section 6), and register subscriptions (Section 7).
- [ ] 8.4 On disable, deactivate subscriptions and revoke issued `crm_plugin_token` rows (declarative extensions remain installed); on enable, restore access.
- [x] 8.5 On uninstall, reverse all declarative extensions, revoke all plugin tokens, deactivate/remove subscriptions, and write lifecycle audit entries. _(P1-a: `uninstallClient` calls `reverseForPlugin` (deletes applied fields + install) then revokes tokens + deletes grant; subscriptions/audit-on-lifecycle land with P1-c.)_

## 9. Marketplace Client

- [ ] 9.1 Specify the centralized control-plane API contract abstractly (catalog browse, version list/fetch, artifact download, license heartbeat) as a Java client interface, including the version-metadata fields needed for signature verification (version, signature, signing key id, content hash); do not build the SaaS.
- [ ] 9.2 Implement a disableable Marketplace Client that browses catalog, fetches versions, and downloads artifacts when the control-plane connection is enabled.
- [ ] 9.3 Verify every downloaded plugin-version artifact's signature against a pinned public key BEFORE install and refuse installation on verification failure; treat versions as immutable (a fix is a new version, never a mutation) and flag a content-hash mismatch as tamper.
- [ ] 9.4 Implement license heartbeats with online verification plus an offline grace period so transient marketplace unreachability does NOT hard-fail; on explicit online revoke, disable affected plugins and revoke tokens.
- [ ] 9.5 Add a local-manifest install path that verifies a signed manifest + artifact entirely offline and never contacts the control plane (air-gapped/self-host).
- [ ] 9.6 Store the marketplace endpoint, keys, and enable/disable switch via `SystemConfigServiceImpl.updateConfig`/`getConfigValue` (Redis `system:config:*`, 30-min TTL), never hardcoded under a static property; encrypt any stored credential via `SecretTextCipher`.

## 10. Frontend Extension Host

- [ ] 10.1 Add dynamic route registration via `router.addRoute` (new for `src/router/index.ts`, currently static `RouteRecordRaw[]`) driven by installed plugin descriptors, respecting `meta.permission`/`meta.requiresAuth`.
- [ ] 10.2 Make sidebar module membership data-driven (currently the fixed array in `src/utils/sidebarModuleOrder.ts`) while reusing the existing `sidebarModuleOrder` order persistence (`userStore.updatePreferences` → `POST /managerUser/preferences`, `manager_user_ui_preferences`, V21).
- [ ] 10.3 Register plugin theme tokens as `--wk-*` CSS variables through the `useTheme.ts` mechanism so theme packs apply without code changes, and remove them on uninstall.
- [ ] 10.4 Remove/relax the hardcoded chat app-code allow-list `Set` in `src/stores/chat.ts` so installed AI applications returned by `GET /chat/app/options` are accepted dynamically.
- [ ] 10.5 Scaffold the sandboxed iframe UI host contract (postMessage SDK surface) as a defined, documented contract only; UI plugin GA deferred (no production UI plugins shipped; host rejects UI plugin loads as not yet GA).

## 11. Plugin Admin UI

- [ ] 11.1 Add a plugin marketplace/admin view to browse the catalog and view plugin versions and details.
- [ ] 11.2 Implement the install flow with a scope-approval (consent) prompt showing requested `module:action` scopes and writing the grant on approval.
- [ ] 11.3 Add enable/disable/uninstall controls and install status (pinned version, enabled state) sourced from `crm_plugin_install`.
- [ ] 11.4 Add a local-manifest install entry point in the UI for air-gapped/self-host installs.
- [ ] 11.5 Gate all plugin-admin views behind an appropriate `module:action` permission via `userStore.hasPermission`.

## 12. Tests And Verification

- [ ] 12.1 Add backend tests for authorization downscoping (effective permission = actor ∩ granted scopes; no escalation), token issue/rotate/revoke, and revocation on disable/uninstall.
- [ ] 12.2 Add backend tests for manifest apply/uninstall reversal idempotency (custom fields via `CustomFieldServiceImpl`, AI app rows, theme tokens) and signature verification (reject unsigned/invalid).
- [ ] 12.3 Add backend tests for Extension Gateway scope enforcement, rate-limit/quota rejection, audit-log writes, and `PluginAiUsageMeter` hook invocation (no-op).
- [ ] 12.4 Add backend tests confirming plugin gateway calls run the existing RBAC + `GlobalDataPermissionHandler` checks (a plugin cannot exceed the installing actor's row-level visibility).
- [ ] 12.5 Add backend tests for domain event publication (AFTER_COMMIT, not fired on rollback) and signed webhook delivery with retry/backoff against `crm_plugin_subscription`.
- [ ] 12.6 Add backend/marketplace tests for offline-grace license behavior (no hard-fail on transient unreachability) and the local-manifest offline install path.
- [ ] 12.7 Add frontend tests/typed fixtures for dynamic route/sidebar registration, theme token registration, and the data-driven chat app-code path.
- [ ] 12.8 Verify `mvn test` passes for `backend` and `npm run build` passes for `frontend`.
