## Context

CRMFRANKLIN today has no third-party extension story. Every AI capability, route, sidebar module, and theme is hardcoded in source, and there is no machine-to-machine authentication for external apps. The relevant facts found during inspection of the actual code (not `CLAUDE.md`, which describes a richer commercial build):

- **AI tool wiring is hardcoded.** `DynamicChatClientProvider` (`backend/.../ai/DynamicChatClientProvider.java`) injects ~13 tool beans with `@Autowired` (`CustomerTools`, `TaskTools`, `ProjectTools`, `MailTools`, `CrmNoopTools`, etc.) and resolves them through `addToolsForGroup(...)`, a hardcoded `switch` over the `ChatApplicationRegistry.TOOL_GROUP_*` constants. There is no path to register a tool group at runtime.
- **AI providers are a static block.** `AiProviderRegistry` (`backend/.../ai/provider/AiProviderRegistry.java`) registers ~10 providers (`dashscope` default, `openai`, `deepseek`, `custom`, ...) in a `static { ... }` block; its `register(AiProviderDescriptor)` method is **private**. There is no public registration API and no DB-backed provider source.
- **Chat applications are constructor-seeded.** `ChatApplicationRegistry` (`backend/.../ai/app/ChatApplicationRegistry.java`) builds 7 `ChatApplicationDefinition` records (`general`, `crm`, `product`, `project`, `knowledge`, `address_book`, `relation`) in its constructor with a **private** `register(...)`. `ChatApplicationDefinition` is a Java record with 8 fields (`code`, `label`, `iconName`, `description`, `systemPrompt`, `defaultRagEnabled`, `toolGroups`, `recommendedQuestions`). There is no `crm_ai_application` table. The frontend reads apps from `GET /chat/app/options` (`ChatController`).
- **Frontend is fully static.** `frontend/src/router/index.ts` declares routes as a static `RouteRecordRaw[]` with `createWebHashHistory()`; no `router.addRoute` usage exists. The sidebar module list is a fixed array `['recent','customer','product','project','relation','addressBook']` in `src/utils/sidebarModuleOrder.ts`, with order (not membership) persisted via `userStore.updatePreferences({ sidebarModuleOrder })` → `POST /managerUser/preferences` (backed by `manager_user_ui_preferences`, migration **V21**). Theme is a fixed set of `--wk-*` CSS variables toggled by `useTheme.ts`. The chat store (`src/stores/chat.ts`) gates app codes through a hardcoded allow-list `Set`.
- **No plugin/app authentication exists.** Auth is UUID session tokens in Redis issued by `TokenService`, carried in the `Manager-Token` header. There is an OIDC provider — `OidcController` (`backend/.../controller/OidcController.java`) — exposing `/.well-known/openid-configuration`, `/oauth2/authorize`, `/oauth2/token`, `/oauth2/userinfo`, `/oauth2/jwks`, `/oauth2/minio-sso`, but it is wired only for the MinIO console SSO client (`OidcConfig`). There is no OAuth client registry table and no `client_credentials` flow.
- **Permissions are imperative `module:action` checks.** `PermissionService.hasPermission(String permission)` (`PermissionServiceImpl`, backed by `IManagerRoleService.auth(userId)` → a `module → {module:action → true}` JSON) is called imperatively in the service layer, not via `@PreAuthorize`. AI tools additionally carry `@AiToolPermission(value="customer:create", ...)` enforced by `AiToolPermissionAspect` (`@Around`).

> **Tenancy reality (load-bearing correction).** This branch is **single-tenant**. `MybatisPlusConfig` registers only `DataPermissionInterceptor` (→ `GlobalDataPermissionHandler`, row-level scoping by user/dept/role for `Customer`/`Contact`/`Task`/`FollowUp`/`Knowledge`) and `PaginationInnerInterceptor`. There is **no** `TenantLineInnerInterceptor`, **no** `TenantContextHolder`, **no** `crm_tenant` table, and `tenant_id` appears **zero** times in the schema. The only `tenant` references in Java are the Microsoft OAuth `tenant=common` string in `ExternalAuthServiceImpl`/`MailServiceImpl`. The multi-tenant isolation `CLAUDE.md` describes is part of the upstream commercial lineage and is not in this open-source cut.

- **No AI billing exists.** There is no `AiQuotaService`, no credit/billing class, and no `crm_ai_credit_record`. The `CLAUDE.md` billing section is aspirational for this repo.
- **No business-layer domain event bus.** `ApplicationEventPublisher` is used only inside the IM module; `customer.created`/`task.updated`-style domain events do not exist.
- **No `RegistrationService` class.** Auto-provisioning logic today lives inside `ExternalAuthServiceImpl`; plugin lifecycle in this change assumes the instance/user already exists and does not provision tenants.
- **Reusable building blocks exist.** Declarative data extensions can reuse `CustomFieldServiceImpl.addField(CustomFieldAddBO)` / `deleteField(Long)` (50-field cap, pooled `field_*` columns via `CustomFieldPoolServiceImpl`) and `DynamicSchemaServiceImpl.addColumn/dropColumn/columnExists` (real `ALTER TABLE`, idempotent). Secrets encrypt via `SecretTextCipher.encrypt/decrypt`. Dynamic config goes through `SystemConfigServiceImpl.getConfigValue/updateConfig` (Redis `system:config:*`, 30-min TTL). IDs come from `BaseUtil.getNextId()` (Hutool Snowflake); audit columns stamped by `MyMetaObjectHandler`.

The current `frontend` and `backend` builds pass. This change is **additive capability**, not a bug fix: it introduces the extension framework, machine-to-machine auth, and a marketplace client without altering existing user-facing behavior.

## Goals / Non-Goals

**Goals:**

- Establish an **Open Core** extension framework inside the open-source instance plus the abstract contract for a separate, centralized **control plane** (marketplace SaaS), without building the SaaS in this change.
- Make AI capability registration **dynamic** by introducing a registry layer that merges built-in registrations (the current static/constructor data) with installed registrations, including a new `crm_ai_application` table for installed/custom apps.
- Add a **declarative manifest** parser that applies data-model / theme-token / AI-application / AI-provider extensions by calling existing services, executing zero third-party code, idempotent, and fully reversible on uninstall.
- Add a **domain event bus** (`ApplicationEventPublisher` + `@TransactionalEventListener` AFTER_COMMIT) plus a signed-webhook dispatcher with retries/backoff and per-plugin subscription records, so out-of-process automation plugins can subscribe to events.
- Reuse the existing **OIDC provider** (`OidcController`) to authenticate plugins via OAuth2 `client_credentials` (+ `authorization_code`), backed by a new OAuth client registry, instance-admin consent grants, and revocable hashed-secret plugin tokens.
- Enforce a **downscoping** authorization model: plugin effective permission = installing actor's `module:action` permissions ∩ granted scopes; never escalation.
- Introduce an **Extension Gateway** as the single inbound API entry for plugins: validates token + scope, applies per-plugin rate-limit/quota, writes an audit log, and invokes an abstract AI-usage meter hook.
- Add a **Marketplace Client** that browses the catalog, fetches versions, downloads/verifies signed artifacts, and runs license heartbeats — **fully disableable** with a local-manifest install path for air-gapped/self-host.
- Adopt a **dual-track tenancy** design: ship single-tenant (per-instance) now, but reserve a nullable `tenant_id` column on operational tables plus an `InstanceScopeResolver` seam so multi-tenancy can be added later without schema rework.
- Define a **stable, versioned Extension API + manifest schema (v1)** with backward-compatibility guarantees, and lay the contracts (event bus, gateway, OAuth, manifest, signing) so deferred phases slot in without rework.

**Non-Goals:**

- Building the centralized control-plane SaaS (Registry, Distribution/CDN, Licensing & Billing, Review & Moderation, Developer Portal) — specified abstractly only; its code lives in a separate closed-source repo.
- Implementing **multi-tenancy** itself (a tenant interceptor/holder, `crm_tenant`, tenant-scoped query rewriting). This change only *reserves the seam*; it does not add the isolation layer `CLAUDE.md` describes.
- Implementing **AI metering/billing**. No `AiQuotaService`/credit system exists; this change ships only an abstract no-op `PluginAiUsageMeter` hook. Real metering, pricing, and credit accounting are a future change.
- Out-of-process AI-skill / automation **runtime** (the actual hosted plugin services) — this change defines the gateway/event/auth contracts they will use, but ships no runtime (P2, future change).
- UI extension **GA** — full-page and widget plugins via iframe sandbox + postMessage SDK: this change DEFINES the host contract but does not ship production UI plugins (P4, future).
- Executing any third-party **code inside the JVM or database**; backend extension code is always out-of-process.
- Changing existing auth (`Manager-Token` sessions) or data-permission coverage beyond what a new protected plugin mapper would require.

## Decisions

1. **Adopt an Open Core + centralized control-plane split (two planes).**

   The data plane is the open-source CRMFRANKLIN instance (vendor cloud or self-hosted). It ships the in-core extension framework and an open-source Marketplace Client. The control plane is a separate, closed-source SaaS owning the Registry (catalog/versions/search/ratings), Distribution (CDN + artifact signing), Licensing & Billing, Review & Moderation, and Developer Portal. This change implements only the instance side and specifies the control-plane API abstractly.

   Alternative considered: a single monolith that hosts both the marketplace and the runtime in-process. Rejected because it couples third-party code lifecycle to the core build, forces self-host operators to run marketplace infrastructure they do not want, and contradicts the existing `mcp_server` "pure adapter, no in-process third-party logic" precedent.

2. **Use a hybrid plugin runtime: out-of-process for code, declarative manifest for data/model/theme, iframe for UI.**

   Where plugin code runs is decided per plugin type. (a) Backend capabilities that execute logic — AI skills/tools, automation actions, integration connectors — run as **out-of-process independent services**; the instance calls them through the Extension Gateway (OAuth2 `client_credentials` + scopes) or pushes signed event webhooks to them. Third-party code never enters the JVM or DB, mirroring `mcp_server`. (b) Data-model, AI-application, AI-provider, and theme extensions are pure **declarative JSON manifest** with zero code execution, translated into calls on existing services. (c) Frontend pages/widgets use an **iframe sandbox + postMessage SDK**; this change defines the host contract only.

   Alternative considered: an in-JVM plugin classloader (OSGi-style) for backend plugins. Rejected: it would let untrusted code touch MyBatis interceptors and the database directly, defeating data-permission safety and supply-chain integrity, and would tie plugin compatibility to the exact Spring/Java version.

3. **Reuse `OidcController` for plugin OAuth instead of inventing a new auth scheme.**

   The existing OIDC provider already exposes `/.well-known/openid-configuration`, `/oauth2/authorize`, `/oauth2/token`, `/oauth2/userinfo`, and `/oauth2/jwks`. This change extends it to support OAuth2 `client_credentials` (machine-to-machine) and `authorization_code` (interactive) flows for plugins, backed by a new `crm_oauth_client` registry. Plugin app secrets are validated against the registry; issued plugin tokens are recorded in `crm_plugin_token` (hashed secret only).

   Alternative considered: a bespoke API-key header per plugin (like `mcp_server`'s single `AICRM_TOKEN`). Rejected: API keys lack scoping, rotation, consent, and standard revocation semantics, and would duplicate an OAuth surface the instance already serves.

4. **Model plugin scopes on the existing `module:action` permission vocabulary, with strict downscoping.**

   A plugin requests scopes drawn from the same vocabulary `PermissionService.hasPermission("module:action")` already enforces (e.g. `customer:view`, `task:create`). On install, an instance admin consents to the requested scopes (`crm_plugin_grant`). At call time the plugin token's **effective permission = installing actor's permissions ∩ granted scopes** — downscoping only, never escalation. Enforcement reuses the existing imperative `PermissionService` check inside the gateway rather than `@PreAuthorize`.

   Alternative considered: a separate plugin-only permission taxonomy. Rejected: it would create two divergent permission models, double the admin mental load, and risk a plugin obtaining rights the installing user never had.

5. **Route all plugin inbound traffic through a single Extension Gateway that scopes, meters, and audits.**

   The Extension Gateway (base path `/plugin/gateway/**`, Bearer plugin token) is the only inbound API entry for plugins. For every request it validates the plugin token + scope, enforces per-plugin rate-limit/quota, writes `crm_plugin_audit_log`, and — because the call runs as the installing actor — applies the existing RBAC + row-level data-permission (`GlobalDataPermissionHandler`) checks so plugin data access stays scoped. For AI-consuming calls it invokes an **abstract `PluginAiUsageMeter` hook** (no-op default). See *Appendix B: Extension Gateway route contract*.

   Alternative considered: let plugins call existing business controllers directly with a session-equivalent token. Rejected: those controllers assume a human `Manager-Token` session, perform no per-plugin scope/audit, and would scatter enforcement logic across dozens of endpoints.

6. **AI metering is an abstract hook, not an implementation (non-goal).**

   No `AiQuotaService`/credit system exists in this codebase. The gateway depends on a small `PluginAiUsageMeter` interface (`ensure(estimate)` / `record(actualUsage)`) with a no-op default bean. This is the seam where a future metering change (or the commercial build's quota service) plugs in without re-touching the gateway. This change ships no metering, pricing, or credit accounting.

   Alternative considered: build the credit/quota system now to match `CLAUDE.md`. Rejected: it roughly doubles the scope, and the open-source core has no billing model to meter against yet; a no-op seam keeps the gateway honest and forward-compatible.

7. **Introduce a merge-based dynamic capability registry layer (built-in + installed).**

   Rather than replacing the current static data, a registry layer merges built-in registrations with installed ones. `ChatApplicationRegistry` keeps its constructor-seeded built-ins and additionally loads installed apps from a new `crm_ai_application` table via a new `loadInstalled(List<ChatApplicationDefinition>)` merge path (built-ins are **not** seeded into the table; they remain in code and win on `code` conflict). `AiProviderRegistry` gains a public `registerInstalled(AiProviderDescriptor)` so installed descriptors overlay the static block. `DynamicChatClientProvider.addToolsForGroup` resolves tool groups from a registry that includes installed (out-of-process) tool groups in addition to the hardcoded `switch`.

   Alternative considered: migrate all built-ins into the DB and delete the static definitions. Rejected: it makes a clean checkout depend on seed data, complicates upgrades, and risks a bricked instance if the seed migration regresses.

8. **Apply declarative data-model extensions by reusing `CustomFieldServiceImpl` / `DynamicSchemaServiceImpl`.**

   The manifest parser translates a signed manifest's `customFields` into `CustomFieldServiceImpl.addField(CustomFieldAddBO)` (and `deleteField` on uninstall), inheriting the 50-field cap, pooled `field_*` columns, idempotent `columnExists` checks, and the existing scoping. Theme token packs register `--wk-*` CSS variables; AI applications insert `crm_ai_application` rows; AI providers register descriptors. Every applied extension is recorded so uninstall reverses it exactly. No new business tables are created for fields or themes.

   Alternative considered: a parallel plugin-owned schema-mutation path. Rejected: it would duplicate the careful pool/validation logic in `DynamicSchemaServiceImpl` and risk uncoordinated `ALTER TABLE`s outside the existing safeguards.

9. **Deliver automation via a domain event bus + signed webhooks to out-of-process plugins.**

   Business services publish domain events (e.g. `customer.created`, `task.updated`) through Spring `ApplicationEventPublisher`, consumed by a dispatcher annotated `@TransactionalEventListener(phase = AFTER_COMMIT)` so events fire only after the business transaction commits and a dispatch failure never rolls it back. The dispatcher reads `crm_plugin_subscription` rows and delivers subscribed events to plugin endpoints with HMAC-signed payloads, retries, and exponential backoff. Plugins act on events out-of-process and call back through the Extension Gateway. See *Appendix C: domain event publication points*.

   Alternative considered: synchronous in-process listener beans contributed by plugins. Rejected: a slow or failing third-party listener would block or break core transactions, and it would require running plugin code in the JVM.

10. **Verify signed, immutable artifacts before install; treat licenses with an offline grace period.**

    Every plugin version published by the marketplace is signed; the instance verifies the signature against a pinned public key **before** installing, and versions are immutable (a fix is a new version, never a mutation). License validity is checked online but tolerates transient marketplace unreachability via an offline grace period — the instance does **not** hard-fail when the control plane is briefly unreachable. Installs are pinned in `crm_plugin_install` with an explicit version.

    Alternative considered: trust-on-download without signature verification and hard online license checks. Rejected: it opens a supply-chain attack vector and makes the product unusable during any marketplace outage.

11. **Make the Marketplace Client pluggable and fully disableable for self-host / air-gapped use.**

    The Marketplace Client is open-source and lives in the instance, but the entire control-plane connection can be turned off. A local-manifest install path lets self-hosted/air-gapped operators install a verified manifest + artifact without ever contacting the SaaS. The marketplace endpoint and keys are stored via `SystemConfigServiceImpl`, never hardcoded under a static property.

    Alternative considered: mandatory always-on marketplace connectivity. Rejected: it would make the open-source core unusable in disconnected/regulated deployments and contradicts the Open Core promise.

12. **Adopt dual-track tenancy: single-tenant now, reserved seam for later.**

    Because the codebase is single-tenant, plugins install **per-instance** and "consent" is given by an instance admin holding the right `module:action`. To avoid a painful migration if multi-tenancy is added later, every operational table (`crm_ai_application`, `crm_plugin_install`, `crm_plugin_grant`, `crm_plugin_token`, `crm_plugin_subscription`, `crm_plugin_audit_log`) carries a **reserved nullable `tenant_id BIGINT`** (NULL = the single instance), and all access goes through an `InstanceScopeResolver` that returns the instance scope today. When the `CLAUDE.md`-style tenant layer lands, the resolver switches to a tenant resolver and the interceptor scopes these tables — no schema change. `crm_oauth_client` stays instance-global (it is a plugin-app catalog, not tenant data).

    Alternative considered: hard single-tenant with no reserved column. Rejected: retrofitting `tenant_id` onto live install/token/audit tables later is a high-risk data migration; a nullable reserved column is nearly free now.

13. **Phase the work: ship P0 (auth + gateway + lifecycle) and P1 (declarative plugins + dynamic registries + marketplace client) now; defer P2/P3/P4.**

    The foundation lays the durable contracts — event bus, Extension Gateway, OAuth/OIDC client model, signed manifest schema v1, artifact signing — so later phases attach without rework. Deferred: the out-of-process AI-skill/automation **runtime** (P2), billing productization (P3), and UI sandbox plugin **GA** (P4). See *Appendix D: deferred-phase contract boundaries*.

    Alternative considered: ship all four phases together. Rejected: it multiplies surface area and risk, delays the foundation other teams depend on, and forces premature decisions on the UI sandbox and billing before the core contracts are proven.

## Risks / Trade-offs

- **Out-of-process latency.** Routing AI tools/automation through the gateway and webhooks adds network hops versus in-JVM calls. Trade-off accepted for isolation and supply-chain safety; mitigate with per-plugin timeouts and async webhook delivery.
- **Supply-chain trust.** A compromised marketplace or leaked signing key could push malicious artifacts. Mitigated by pinned-public-key verification before install, immutable versions, and submission scanning in the (separate) control plane; the signing key's custody/rotation is itself a risk to govern.
- **Downscoping correctness is security-critical.** A bug that computes effective permission as union (not intersection) of actor ∩ grant would silently escalate plugin access. Must be covered by explicit tests (task 12.1).
- **Reserved `tenant_id` is inert until multi-tenancy lands.** Until `add-multi-tenancy-foundation` ships, `InstanceScopeResolver` returns a single instance and nothing enforces `tenant_id`; a premature assumption that plugin data is tenant-isolated would be wrong.
- **No-op AI meter can mask cost.** Shipping `PluginAiUsageMeter` as a no-op means AI-consuming plugins incur unmetered model spend until a real meter lands. Acceptable for the open-source core (no billing model), but vendor-cloud operators must supply a real meter before monetizing.
- **Registry merge regressions.** Refactoring `DynamicChatClientProvider`/`AiProviderRegistry`/`ChatApplicationRegistry` touches the live chat path; a merge bug could drop built-in apps/providers. Mitigate by keeping built-ins constructor-seeded (never DB-dependent) and testing built-in availability on an empty install.
- **Frontend dynamic routes/sidebar** widen the attack surface for a future UI plugin; this is why UI plugin GA is deferred and the iframe host is inert in this change.

## Migration Plan

1. **Schema first** — ship the `V44` migration (seven additive tables, `IF NOT EXISTS`, no changes to existing tables); safe to deploy ahead of code.
2. **Auth + gateway (P0)** — `crm_oauth_client` registry, `OidcController` `client_credentials`/`authorization_code`, scope catalog, consent/grant, token issue/rotate/revoke, downscoping, Extension Gateway with audit + no-op meter. Plugins cannot do anything useful until this lands, so it precedes everything else.
3. **Registries + manifest + lifecycle + events (P1)** — dynamic registry merge, signed manifest parser (reusing `CustomFieldServiceImpl`), event bus + webhook dispatcher, lifecycle service.
4. **Marketplace client** — connectivity, signature verification, offline-grace license, local-manifest path; endpoint via `SystemConfigServiceImpl`.
5. **Frontend host + admin UI** — dynamic routes/sidebar/theme, data-driven chat apps, plugin admin views.
6. **No data backfill required** — all tables are new and empty. Rollback is dropping the seven tables (no existing data touched). The `InstanceScopeResolver` seam is activated later by `add-multi-tenancy-foundation`.

## Open Questions

- Out-of-process tool invocation transport for installed tool groups (HTTP vs gRPC) — deferred to P2; this change only registers the named group.
- Should the Extension Gateway expose a generic CRM proxy (`/plugin/gateway/crm/*`) or an explicit per-resource allowlist? Leaning allowlist for tighter scope mapping; to be finalized in implementation.
- Webhook delivery durability — in-process retry/backoff now vs a durable queue later if volume grows.
- Per-plugin rate-limit/quota defaults and where they are configured (global vs per-install).
- Whether theme token packs may override arbitrary `--wk-*` variables or only a curated subset (to prevent UI breakage).

## Appendix A: Manifest schema v1 (example)

A plugin manifest is a signed JSON document. Backend logic is referenced by `endpoints` (out-of-process URLs), never embedded as code.

```jsonc
{
  "schemaVersion": "v1",
  "plugin": { "id": "acme.invoice-sync", "name": "Invoice Sync", "version": "1.4.0" },
  "requestedScopes": ["customer:view", "task:create"],
  "endpoints": { "baseUrl": "https://plugin.acme.example", "health": "/healthz" },
  "customFields": [
    { "entity": "customer", "key": "acme_invoice_id", "label": "Invoice ID", "type": "text", "unique": false }
  ],
  "aiApplications": [
    { "code": "acme_invoice", "label": "Invoice Assistant", "iconName": "doc",
      "systemPrompt": "...", "defaultRagEnabled": false,
      "toolGroups": ["acme_invoice_tools"], "recommendedQuestions": ["..."] }
  ],
  "aiProviders": [],
  "themeTokens": { "name": "acme-dark", "variables": { "--wk-primary-rgb": "12 80 200" } },
  "eventSubscriptions": [
    { "event": "customer.created", "endpoint": "/hooks/customer-created" }
  ],
  "uiExtensions": []   // reserved; iframe host contract defined, GA deferred (P4)
}
```

Backward-compatibility: `schemaVersion` is mandatory; unknown top-level keys are ignored with a warning, but an unknown/unsupported `schemaVersion` is rejected without applying anything. A `v2` migration path is out of scope.

## Appendix B: Extension Gateway route contract

- Base path: `/plugin/gateway/**`. Authentication: `Authorization: Bearer <plugin-token>`.
- Scoped CRM access is namespaced, e.g. `GET /plugin/gateway/crm/customers?limit=10` requires `customer:view` in the token's effective permissions; `POST /plugin/gateway/crm/tasks` requires `task:create`.
- Subscription management: `POST /plugin/gateway/subscriptions` (create), `DELETE /plugin/gateway/subscriptions/{id}` (deactivate).
- Every call resolves the installing actor, recomputes effective permission (actor ∩ grant), runs the existing `PermissionService`/data-permission checks, writes a `crm_plugin_audit_log` row, and (for AI-consuming routes) brackets the call with `PluginAiUsageMeter.ensure(...)` / `record(...)`.
- The gateway never exposes the human `Manager-Token` controllers to plugins.

## Appendix C: Domain event publication points

- Events are published from business service implementations *after* a successful write, via `applicationEventPublisher.publishEvent(new CustomerCreatedEvent(id, scope))`.
- The dispatcher consumes them with `@TransactionalEventListener(phase = AFTER_COMMIT)`, so no event is delivered for a rolled-back transaction and a dispatch failure cannot roll back business data.
- First-wave events: `customer.created`, `customer.updated`, `task.created`, `task.updated`, `followup.created`. The publication call sites (`CustomerServiceImpl`, `TaskServiceImpl`, `FollowUpServiceImpl`, ...) are enumerated as task 6.1; adding an event to a new service is additive.

## Appendix D: Deferred-phase contract boundaries

- **P2 — out-of-process AI-skill/automation runtime.** The gateway + `client_credentials` auth + event webhooks are the contract; the actual hosted plugin services and the invocation transport (HTTP/gRPC) for installed tool groups are deferred. `DynamicChatClientProvider` resolves an installed tool group to a *named out-of-process invocation* whose wire call is stubbed in this change.
- **P3 — billing.** `PluginAiUsageMeter` is the attribution seam; pricing, credit balances, and purchase orders are deferred.
- **P4 — UI plugins.** The iframe + postMessage host contract is defined (Appendix A `uiExtensions`, frontend host scaffold) but no production UI plugin is loadable; the host rejects UI plugin loads as not yet GA.
