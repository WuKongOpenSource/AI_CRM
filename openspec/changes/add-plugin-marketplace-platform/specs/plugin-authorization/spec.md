## ADDED Requirements

### Requirement: Reuse Existing OIDC Provider For Plugin Flows
The system SHALL support OAuth2 `client_credentials` and `authorization_code` flows for plugins by extending the existing OIDC provider (`OidcController`), and SHALL NOT introduce a new or parallel authentication scheme for plugins.

#### Scenario: Plugin requests a token via client_credentials
- **WHEN** a registered plugin posts valid `client_id`/`client_secret` with `grant_type=client_credentials` to `/oauth2/token`
- **THEN** `OidcController` SHALL issue a plugin token through the existing token path rather than a separate endpoint

#### Scenario: Plugin requests user-delegated access via authorization_code
- **WHEN** a plugin initiates `/oauth2/authorize` with `grant_type=authorization_code` against a user session
- **THEN** the existing OIDC authorize/token exchange SHALL be reused and SHALL NOT bypass the existing session/consent handling

### Requirement: OAuth Client Registry
Known plugin OAuth applications SHALL be recorded in an instance-global `crm_oauth_client` registry (no `tenant_id`), since plugin apps are catalog-level metadata rather than per-instance data.

#### Scenario: Token request from an unknown client
- **WHEN** a token is requested for a `client_id` that has no `crm_oauth_client` row or whose `status` is not active
- **THEN** the request SHALL be rejected and no plugin token SHALL be issued

#### Scenario: Client lookup is instance-global
- **WHEN** the OAuth flow resolves a plugin `client_id`
- **THEN** the `crm_oauth_client` read SHALL succeed without any tenant predicate, because the table is instance-global and carries no `tenant_id`

### Requirement: Scopes Reuse The module:action Permission Vocabulary
Plugin scopes SHALL be drawn from the existing `module:action` permission vocabulary enforced by `PermissionService.hasPermission`, and SHALL NOT define a separate plugin-only permission taxonomy.

#### Scenario: Requested scope maps to a known permission
- **WHEN** a plugin requests scopes such as `customer:view`, `task:create`, or `knowledge:view`
- **THEN** each scope SHALL be validated against the existing `module:action` vocabulary and SHALL be enforced via `PermissionService.hasPermission`

#### Scenario: Requested scope is not a valid permission token
- **WHEN** a plugin requests a scope that is not part of the `module:action` vocabulary
- **THEN** the scope SHALL be rejected during grant evaluation and SHALL NOT be persisted as granted

### Requirement: Downscoping Invariant
A plugin token's effective permission SHALL equal the installing actor's permissions INTERSECT the granted scopes, and a plugin SHALL NOT obtain any permission the installing actor does not hold (downscoping only, never escalation).

#### Scenario: Plugin requests a scope the installing admin lacks
- **WHEN** a plugin requests a scope (e.g. `customer:create`) that the installing admin does not have
- **THEN** that scope SHALL be excluded from the grant (denied/downscoped) and SHALL NOT be added to the plugin token's effective permissions

#### Scenario: Effective permission is the intersection
- **WHEN** a plugin token is evaluated against a `module:action` permission
- **THEN** access SHALL be allowed only if the permission is present in BOTH the installing actor's permissions AND the granted scopes

### Requirement: Instance-Admin Consent Grant On Install
Instance-admin consent on install SHALL be persisted in `crm_plugin_grant` before any plugin token is issued, recording the set of `module:action` scopes approved for the plugin. (When multi-tenancy is later added, the reserved `tenant_id` on this table makes consent per-tenant.)

#### Scenario: Admin approves requested scopes
- **WHEN** an admin approves a plugin's requested scopes during install
- **THEN** the approved scopes SHALL be written to `crm_plugin_grant` (with `granted_by_user_id`) and a token MAY then be issued

#### Scenario: Token issuance attempted without a grant
- **WHEN** a plugin token issuance is attempted with no active `crm_plugin_grant` for that plugin
- **THEN** issuance SHALL be refused until consent is recorded

### Requirement: Plugin Token Lifecycle
Plugin tokens SHALL be issued, rotated, and revoked in `crm_plugin_token` storing only hashed secrets, and SHALL be revoked on plugin disable or uninstall.

#### Scenario: Token is stored hashed
- **WHEN** a plugin token is issued
- **THEN** only a hash of the secret SHALL be persisted in `crm_plugin_token.token_hash` and the plaintext secret SHALL NOT be stored

#### Scenario: Revoked token is rejected
- **WHEN** a request presents a token whose `crm_plugin_token` row is marked revoked or is expired
- **THEN** the request SHALL be rejected and the rejection SHALL be auditable

#### Scenario: Disable or uninstall revokes tokens
- **WHEN** a plugin is disabled or uninstalled
- **THEN** all of that plugin's `crm_plugin_token` rows SHALL be revoked

### Requirement: Extension Gateway Single Inbound Entry
The Extension Gateway SHALL be the single inbound API entry for plugin calls (base path `/plugin/gateway/**`, `Authorization: Bearer <plugin-token>`), validating token and scope, enforcing per-plugin rate-limit/quota, and writing a `crm_plugin_audit_log` entry for each call and lifecycle event. Plugins SHALL NOT call the human `Manager-Token` business controllers directly.

#### Scenario: Scoped resource call requires the matching permission
- **WHEN** a plugin calls `GET /plugin/gateway/crm/customers`
- **THEN** the gateway SHALL require `customer:view` in the token's effective permissions and SHALL reject the call otherwise

#### Scenario: Call exceeding granted scope is blocked and audited
- **WHEN** a plugin call targets an operation requiring a `module:action` not within the token's effective permissions
- **THEN** the gateway SHALL reject the call and SHALL write a `crm_plugin_audit_log` entry recording the plugin, action, scope, and a failure result

#### Scenario: Plugin exceeds its rate limit or quota
- **WHEN** a plugin exceeds its configured per-plugin rate-limit or quota
- **THEN** the gateway SHALL throttle or reject the call and SHALL record the throttled outcome in `crm_plugin_audit_log`

#### Scenario: Successful call is audited
- **WHEN** a plugin call passes token and scope validation
- **THEN** the gateway SHALL execute the call and SHALL write a `crm_plugin_audit_log` entry with a success result

### Requirement: Abstract AI-Usage Metering Hook
The gateway SHALL bracket AI-consuming plugin calls with an abstract `PluginAiUsageMeter` hook (`ensure` before, `record` after). This change SHALL ship only a no-op default implementation; actual metering/credit accounting is a non-goal because no `AiQuotaService`/credit system exists in this codebase.

#### Scenario: AI-consuming call invokes the hook with the no-op meter
- **WHEN** an AI-consuming plugin call is made and the default no-op `PluginAiUsageMeter` is active
- **THEN** the gateway SHALL invoke `ensure` before and `record` after the call, and the no-op meter SHALL permit the call and account nothing

#### Scenario: A future meter can block or account without gateway changes
- **WHEN** a future change supplies a non-no-op `PluginAiUsageMeter` bean
- **THEN** it SHALL be able to fail `ensure` (blocking the call pre-invocation) or account usage in `record` without any modification to the gateway call sites

### Requirement: Actor Scoping Preserved Through The Gateway
Plugin data access through the gateway SHALL run as the installing actor so that the existing RBAC (`PermissionService`) and row-level data-permission (`GlobalDataPermissionHandler` via `DataPermissionInterceptor`) checks apply automatically; a plugin SHALL NOT see data the installing actor cannot see.

#### Scenario: Plugin read honors row-level data permission
- **WHEN** a plugin reads `Customer`/`Contact`/`Task`/`FollowUp`/`Knowledge` data via the gateway
- **THEN** the query SHALL be constrained by `GlobalDataPermissionHandler` to the installing actor's data scope (self / self+subordinates / dept / dept+sub / all) and SHALL NOT return rows outside it

#### Scenario: Request context is established and cleared
- **WHEN** a plugin request is authenticated at the gateway
- **THEN** the installing actor's context SHALL be established for the request (so RBAC and data-permission resolve correctly) and SHALL be cleared in a `finally` block after the request completes
