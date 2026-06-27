## ADDED Requirements

### Requirement: Dynamic AI Chat Application Registry
The AI chat application registry SHALL merge its constructor-seeded built-in `ChatApplicationDefinition` entries (which remain defined in code, not in the database) with installed applications loaded from a new `crm_ai_application` table via a `loadInstalled(...)` path, so installed chat applications appear without any code change, with built-ins winning on `code` conflict.

#### Scenario: Installed application is loaded from the database
- **WHEN** an enabled plugin has applied a `crm_ai_application` row whose `code` does not collide with a built-in
- **THEN** `ChatApplicationRegistry` SHALL expose that application alongside the built-in apps and it SHALL be returned by the `GET /chat/app/options` endpoint

#### Scenario: Installed application code collides with a built-in
- **WHEN** a `crm_ai_application` row uses a `code` already registered by the constructor (e.g. `general`, `crm`)
- **THEN** the registry SHALL keep the built-in definition and SHALL NOT let the installed row override it

#### Scenario: Built-ins require no seed data
- **WHEN** a clean instance starts with an empty `crm_ai_application` table
- **THEN** all built-in applications SHALL still be available from the constructor seeding, so the instance never depends on migration seed rows for built-ins

### Requirement: Instance-Scoped Installed Records With Reserved Tenant Seam
Installed records (e.g. `crm_ai_application`, `crm_plugin_install`) SHALL be scoped through a single `InstanceScopeResolver` and SHALL carry a reserved nullable `tenant_id` column that defaults to the single instance, so that a future multi-tenant layer can scope them without schema change while nothing filters on `tenant_id` today.

#### Scenario: Records resolve to the single instance today
- **WHEN** installed records are read or written on this single-tenant codebase
- **THEN** access SHALL go through `InstanceScopeResolver` returning the instance scope, and `tenant_id` SHALL remain NULL (the single-instance sentinel) without any query rewriting

#### Scenario: Forward-compatible with multi-tenancy
- **WHEN** a future change introduces a tenant-scoping interceptor
- **THEN** the reserved `tenant_id` column and the `InstanceScopeResolver` seam SHALL allow per-tenant scoping without altering the table structure or the call sites

### Requirement: Dynamic AI Provider And Tool-Group Registration
`AiProviderRegistry` SHALL gain a public `registerInstalled(AiProviderDescriptor)` path that overlays installed provider descriptors onto the static built-in block without removing built-ins, and `DynamicChatClientProvider` tool-group resolution SHALL include installed out-of-process tool groups in addition to the hardcoded `addToolsForGroup` switch.

#### Scenario: Installed provider descriptor is registered
- **WHEN** an enabled plugin contributes an `AiProviderDescriptor` for a new provider code
- **THEN** `AiProviderRegistry` SHALL include it in lookups while the built-in providers (e.g. `dashscope`, `openai`) remain registered

#### Scenario: Installed tool group is resolved for a chat application
- **WHEN** a chat application references a tool group contributed by an installed plugin rather than a hardcoded group
- **THEN** `DynamicChatClientProvider` SHALL resolve that group to the installed out-of-process tool invocation path in addition to the built-in tool beans

### Requirement: Versioned Extension API And Manifest Schema
The Extension API and plugin manifest schema SHALL be versioned as `v1` with documented backward-compatibility guarantees so that a manifest declaring an unsupported or unknown schema version is rejected rather than partially applied.

#### Scenario: Manifest declares a supported schema version
- **WHEN** a signed manifest declares `schemaVersion: "v1"` with fields recognized by the parser
- **THEN** the parser SHALL accept and apply it, ignoring unknown non-critical keys with a warning

#### Scenario: Manifest declares an unsupported schema version
- **WHEN** a manifest declares a schema version the instance does not support
- **THEN** the parser SHALL reject the manifest with a version-incompatibility error and SHALL NOT apply any declarative extension from it

### Requirement: Signed Declarative Manifest Application
A signed declarative manifest parser SHALL apply custom fields via `CustomFieldServiceImpl.addField` (backed by `DynamicSchemaServiceImpl.addColumn`), theme CSS-variable token packs, `crm_ai_application` rows, and AI provider descriptors, idempotently, executing zero third-party code.

#### Scenario: Manifest applied twice is idempotent
- **WHEN** the same signed manifest is applied a second time to an instance that already has its extensions
- **THEN** existing custom-field columns SHALL be skipped via `DynamicSchemaServiceImpl.columnExists`, existing `crm_ai_application` rows SHALL NOT be duplicated, and the operation SHALL succeed without error

#### Scenario: Manifest references an unsupported field type
- **WHEN** a manifest declares a custom field whose type is not a valid `CustomFieldServiceImpl` field type or violates the column-name pattern
- **THEN** the parser SHALL abort the apply, SHALL roll back any partially applied changes in the same transaction, and SHALL report a validation error

#### Scenario: Custom-field cap is exceeded
- **WHEN** applying a manifest would push an entity past the existing 50-field cap
- **THEN** the parser SHALL refuse to add further fields and SHALL surface the existing cap error rather than silently dropping fields

### Requirement: Reversible Uninstall Of Declarative Extensions
Uninstall SHALL fully reverse every declarative extension applied by a plugin — deleting custom fields via `CustomFieldServiceImpl.deleteField`, removing the plugin's `crm_ai_application` rows, unregistering its provider descriptors and theme token packs — and SHALL revoke the plugin's tokens.

#### Scenario: Uninstall reverses a custom field
- **WHEN** a plugin that added a custom field is uninstalled
- **THEN** the corresponding field metadata SHALL be removed via `CustomFieldServiceImpl.deleteField`, the column SHALL be dropped via `DynamicSchemaServiceImpl.dropColumn`, and the field SHALL no longer appear for that entity

#### Scenario: Uninstall revokes plugin tokens
- **WHEN** a plugin is uninstalled
- **THEN** all of its `crm_plugin_token` rows SHALL be marked revoked so subsequent gateway calls with those tokens fail

#### Scenario: Uninstall records the lifecycle event
- **WHEN** uninstall completes its reversals
- **THEN** the system SHALL record the lifecycle event in `crm_plugin_audit_log`

### Requirement: Domain Events Published After Commit
Domain events such as `customer.created` and `task.updated` SHALL be published from business services via Spring `ApplicationEventPublisher` and consumed for delivery with `@TransactionalEventListener(phase = AFTER_COMMIT)`, so an event is never delivered for a rolled-back transaction and a delivery failure never rolls back the business write.

#### Scenario: Event fires only after the business transaction commits
- **WHEN** a business write publishes a domain event but its transaction subsequently rolls back
- **THEN** no webhook delivery SHALL occur for that event

#### Scenario: Delivery failure does not affect business data
- **WHEN** the webhook dispatcher fails to deliver an after-commit event
- **THEN** the business write SHALL remain committed and the failure SHALL be retried/recorded out of band

### Requirement: Signed Webhook Delivery To Subscribed Plugins
Published domain events SHALL be delivered to subscribed plugin endpoints recorded in `crm_plugin_subscription` with HMAC-signed payloads, retries, and backoff.

#### Scenario: Subscribed plugin receives a signed event
- **WHEN** a domain event is published and an active `crm_plugin_subscription` row matches its event type
- **THEN** the dispatcher SHALL POST the event payload to the subscription `endpoint_url` with an HMAC signature derived from the per-subscription signing secret (decrypted via `SecretTextCipher`)

#### Scenario: Delivery fails transiently
- **WHEN** a webhook delivery returns a retryable failure
- **THEN** the dispatcher SHALL retry with backoff up to the configured limit and SHALL record the final delivery outcome in `crm_plugin_audit_log`

#### Scenario: Disabled plugin stops receiving events
- **WHEN** a plugin is disabled or its subscription is marked inactive
- **THEN** the dispatcher SHALL NOT deliver further events to that plugin's endpoint

### Requirement: Per-Instance Plugin Lifecycle With Encrypted Config
The platform SHALL support per-instance install, enable, disable, and uninstall recorded in `crm_plugin_install`, with plugin configuration stored encrypted via `SecretTextCipher`.

#### Scenario: Plugin config is stored encrypted
- **WHEN** a plugin is installed with configuration values
- **THEN** the config SHALL be persisted in `crm_plugin_install.config_encrypted` using `SecretTextCipher.encrypt` and SHALL NOT be stored in plaintext

#### Scenario: Disable suspends a plugin without uninstalling
- **WHEN** a plugin is disabled
- **THEN** its `crm_plugin_install.enabled` flag SHALL be cleared, its declarative extensions SHALL remain installed, but its tokens SHALL be revoked and it SHALL stop receiving events and gateway access

#### Scenario: Lifecycle action runs without a request context
- **WHEN** a lifecycle action executes on a background thread without a request context
- **THEN** the code SHALL resolve scope via `InstanceScopeResolver` and clear any ThreadLocal context in a `finally` block to avoid leakage across pooled threads

### Requirement: Frontend Extension Host
The frontend extension host SHALL support dynamic route registration via `router.addRoute` gated by `meta.permission`, data-driven sidebar modules reusing the existing `sidebarModuleOrder` persistence (`POST /managerUser/preferences`, `manager_user_ui_preferences`), and CSS-variable theme token registration, while the iframe UI host contract is defined but UI plugin GA is deferred.

#### Scenario: Installed page registers a permission-gated route
- **WHEN** an enabled plugin contributes a frontend page with a declared `meta.permission`
- **THEN** the host SHALL register the route via `router.addRoute`, and `router.beforeEach` SHALL allow navigation only when `userStore.hasPermission(...)` passes, otherwise redirect to `/chat`

#### Scenario: Data-driven sidebar module appears
- **WHEN** an enabled plugin contributes a sidebar module
- **THEN** it SHALL be added to the sidebar module set and its position SHALL persist through the existing `sidebarModuleOrder` preference saved to `manager_user_ui_preferences`

#### Scenario: Theme token pack registers CSS variables
- **WHEN** a plugin theme token pack is applied
- **THEN** the host SHALL register the declared CSS variables (e.g. `--wk-primary-rgb`) and remove them on uninstall

#### Scenario: Iframe UI host contract is defined but inert
- **WHEN** this change is delivered
- **THEN** the iframe UI host postMessage contract SHALL be documented as `v1` but no production UI plugin SHALL be loadable, and the host SHALL reject UI plugin loads as not yet generally available
