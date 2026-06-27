# WukongMarket ↔ AI CRM Instance — Integration Contract (v1)

> **Purpose.** This is the **single source of truth** that links two separate codebases:
> - **AI CRM instance** (this repo, open-source) — runs a *Marketplace Client* (P1-d, not yet built) that **consumes** this contract.
> - **WukongMarket** (new repo, closed-source SaaS) — the centralized control plane that **implements the server side** of this contract.
>
> Keep this file in **both** repos (or extract a shared `wukong-plugin-contract` JAR — see §9). Bump the version on any breaking change. Stack for both: **Java 21 / Spring Boot**.

---

## 0. Two planes & trust model

- **Data plane** = AI CRM instances (vendor cloud OR self-hosted). Each instance already has, today (verified this session): OAuth client registry, an Extension Gateway, declarative-manifest apply/reverse, dynamic AI-app registry, event→signed-webhook delivery, and a **local-manifest install path** (works fully offline, no marketplace).
- **Control plane** = WukongMarket: Registry, Distribution (CDN + **signing**), Licensing & Billing, Review/Moderation, Developer Portal, and the **public Marketplace API** below.
- **Trust anchor**: WukongMarket holds a **signing private key**; every plugin version is signed. Each instance **pins the public key** and verifies a signature **before** install. The instance never trusts unsigned/tampered artifacts.

## 1. Authority split (important — avoids a common mistake)

| Concern | Authority |
|---|---|
| Plugin catalog, versions, artifacts, signing, licensing, developer identity | **WukongMarket** |
| OAuth **token issuance**, scope **enforcement**, plugin API access, data access | **CRM instance** (its own OIDC provider, `OidcController`) |

WukongMarket **provisions** an OAuth client (allocates `client_id` + initial `client_secret` + publishes the manifest). The instance, on install, **registers** that client locally (`crm_oauth_client`, secret stored hashed) and **issues plugin tokens itself** via `grant_type=client_credentials`. WukongMarket never issues CRM access tokens.

---

## 2. Control-plane HTTP API (WukongMarket serves; instance calls)

- Base path: `/api/v1`. Base URL is configured per-instance (instance stores it via `SystemConfigServiceImpl`).
- Instance→marketplace auth: an **instance API key** in `Authorization: Bearer <instanceApiKey>` (issued when the instance/tenant registers with the marketplace). Public catalog endpoints MAY be anonymous.
- All responses JSON. Errors: `{ "error": "<code>", "message": "<text>" }` with appropriate HTTP status.

### 2.1 Catalog
```
GET /api/v1/catalog?category=&q=&page=1&size=20
200 → { "items": [ CatalogItem ], "page": 1, "size": 20, "total": 42 }
```
`CatalogItem`: `{ pluginId, name, summary, type, category, trustTier, iconUrl, latestVersion, rating, installCount, pricing: {model: "free|one_time|subscription", ...} }`  — `type`/`category`/`trustTier` enums in §11.

### 2.2 Plugin detail
```
GET /api/v1/plugins/{pluginId}
200 → { pluginId, name, description, author, type, category, trustTier, iconUrl, screenshots:[], versions:[ "1.0.0", "1.1.0" ], latestVersion, requestedScopes:[ "customer:view", ... ], pricing }
```

### 2.3 Version metadata (the trust-critical object)
```
GET /api/v1/plugins/{pluginId}/versions/{version}
200 → VersionMetadata
```
`VersionMetadata`:
```jsonc
{
  "pluginId": "acme.invoice-sync",
  "version": "1.1.0",
  "contentHash": "<sha256 hex of the artifact bytes>",
  "signature": "<base64 signature over contentHash>",
  "signingKeyId": "wm-key-2026",
  "signatureAlg": "SHA256withRSA",          // or Ed25519
  "manifestUrl": "/api/v1/plugins/acme.invoice-sync/versions/1.1.0/manifest",
  "artifactUrl": "/api/v1/plugins/acme.invoice-sync/versions/1.1.0/artifact",
  "sizeBytes": 12345,
  "publishedAt": "2026-06-22T10:00:00Z"
}
```

### 2.4 Manifest & artifact
```
GET /api/v1/plugins/{pluginId}/versions/{version}/manifest   → PluginManifest (v1, §3)
GET /api/v1/plugins/{pluginId}/versions/{version}/artifact    → bytes (or 302 to CDN)
```

### 2.5 Install bundle (issued when a tenant installs)
```
POST /api/v1/plugins/{pluginId}/install   { instanceId, version }   (Bearer instanceApiKey)
200 → InstallBundle
```
`InstallBundle`:
```jsonc
{
  "pluginId": "acme.invoice-sync",
  "version": "1.1.0",
  "clientId": "plg_acme_xxx",
  "clientSecret": "<one-time plaintext; instance stores only its hash>",
  "requestedScopes": ["customer:view", "task:create"],
  "endpoints": { "baseUrl": "https://plugin.acme.example" },   // out-of-process backend (P2)
  "versionMetadata": { ...VersionMetadata... },
  "licenseKey": "<license key for this instance+plugin, or null for free>"
}
```

### 2.6 License heartbeat
```
POST /api/v1/licenses/heartbeat   { instanceId, pluginId, version, licenseKey }   (Bearer instanceApiKey)
200 → { "valid": true, "status": "active|expired|revoked", "graceSeconds": 604800, "checkedAt": "..." }
```

> **Developer/publish endpoints** (upload a version, manage listing, see installs/revenue) are **internal to WukongMarket** (Developer Portal) and are NOT called by the instance. Define them freely in the new repo.

---

## 3. Plugin manifest schema v1 (BYTE-COMPATIBLE with the instance parser)

This MUST match the instance's `com.kakarote.ai_crm.entity.BO.PluginManifest` exactly. The instance ignores unknown keys (Jackson `fail-on-unknown=false`).

```jsonc
{
  "schemaVersion": "v1",                       // REQUIRED; instance rejects anything != "v1"
  "plugin": { "id": "acme.invoice-sync", "name": "Invoice Sync", "version": "1.1.0" },
  "requestedScopes": ["customer:view", "task:create"],   // module:action vocabulary (§3.1)
  "endpoints": { "baseUrl": "https://plugin.acme.example", "health": "/healthz" },  // P2 out-of-process

  "customFields": [
    { "entity": "customer",          // one of: customer | contact | relation | product
      "label": "Invoice ID",         // REQUIRED display label
      "type": "text",                // text|textarea|number|date|datetime|select|multiselect|checkbox
      "defaultValue": null, "placeholder": null,
      "required": false, "searchable": false, "showInList": true, "unique": false }
  ],

  "aiApplications": [
    { "code": "acme_invoice",        // must NOT collide with built-ins (§3.2) — collisions are ignored
      "label": "Invoice Assistant", "iconName": "doc", "description": "...",
      "systemPrompt": "...", "defaultRagEnabled": false,
      "toolGroups": ["knowledge"],   // built-in tool groups load tools; custom groups are inert until P1-b follow-up
      "recommendedQuestions": ["...", "..."] }
  ],

  "aiProviders": [],                  // reserved (P1-b follow-up)
  "themeTokens": { "name": "acme-dark", "variables": { "--wk-primary-rgb": "12 80 200" } },  // reserved (partial)
  "eventSubscriptions": [ { "event": "customer.created", "endpoint": "/hooks/customer-created" } ],
  "uiExtensions": []                  // reserved (P4, iframe sandbox)
}
```

### 3.1 Scope vocabulary
Scopes are the CRM's existing `module:action` permission strings (enforced by `PermissionService.hasPermission`). Examples: `customer:view`, `customer:create`, `task:create`, `knowledge:view`, `addressBook:list`. **Downscoping invariant** (enforced by the instance): a plugin's effective permission = installing admin's permissions ∩ granted scopes. WukongMarket should let developers *declare* `requestedScopes`; actual grant happens at install-time consent in the instance.

### 3.2 Reserved AI-application codes (built-ins; do not let devs use)
`general, crm, product, project, knowledge, address_book, relation` — the instance keeps the built-in on conflict (verified). The Developer Portal should reject these codes at publish time.

---

## 4. Artifact signing & trust

1. WukongMarket computes `contentHash = sha256(artifactBytes)`, signs it with the **private key** → `signature` (+ `signingKeyId`, `signatureAlg`).
2. The instance, before install: downloads artifact → recomputes sha256 → checks it equals `contentHash` → verifies `signature` over `contentHash` with the **pinned public key** for `signingKeyId`. Any mismatch ⇒ **refuse install**.
3. **Versions are immutable.** A fix = a new version. Re-fetch of a pinned version must return identical `contentHash`/`signature`.
4. **Algorithm**: recommend `SHA256withRSA` (the instance already uses RSA in `OidcConfig`, so reuse the JCA primitives) **or** Ed25519. Pick one and fix it for v1.
5. **Key distribution / rotation**: the instance pins public key(s) via `SystemConfigServiceImpl` (`marketplace.signing.publicKeys` = list of `{keyId, pem}`). Support multiple keyIds for rotation; never auto-trust a key delivered in-band.

---

## 5. Install / OAuth provisioning flow (end to end)

```
Developer → WukongMarket: publish plugin version (manifest + artifact)  → marketplace signs it
Tenant admin (in CRM UI) → browse catalog (via instance Marketplace Client → §2.1/2.2)
Tenant admin → install vX → instance calls POST /install (§2.5) → InstallBundle
Instance:
  1. verify signature over contentHash (pinned key)            [§4]
  2. register OAuth client: crm_oauth_client(clientId, hash(clientSecret), pluginId, grant_types, default_scopes=requestedScopes)
  3. apply manifest declaratively (customFields, aiApplications, ...) [existing PluginManifestService]
  4. admin consents to scopes → crm_plugin_grant (downscoped to admin's perms)
  5. register event subscriptions → crm_plugin_subscription
  6. record crm_plugin_install (pinned version, encrypted config + applied refs)
Plugin (out-of-process) → POST /oauth2/token grant_type=client_credentials (client_id/secret) → plugin token (scopes from grant)
Plugin → /plugin/gateway/** with Bearer plugin token → runs as installing actor, data-permission applies
```

The instance pieces in steps 2–6 **already exist and are verified** (`registerClient`/`applyManifest`/`grantScopes`/`createSubscription`/gateway). P1-d only adds the Marketplace Client (steps: browse, call /install, verify signature, schedule heartbeats) + a "register-from-bundle" path that reuses those.

---

## 6. Licensing

- License key per `(instanceId, pluginId)`. Free plugins: no key / always active.
- Instance heartbeats periodically (§2.6). **Offline grace**: within `graceSeconds` of the last successful check, installed plugins keep running even if the marketplace is unreachable (do NOT hard-fail on transient outage). After grace elapses with no success → mark license-unverified, MAY disable. Explicit `revoked`/`expired` from a successful call → disable + revoke plugin tokens.

---

## 7. Versioning

- Contract version is **v1**, encoded in the API path (`/api/v1`) and `manifest.schemaVersion`. Any breaking change ⇒ `/api/v2` + `schemaVersion: "v2"`; the instance rejects unknown schema versions.

---

## 8. WukongMarket build outline (server side)

- **M0** Scaffold Spring Boot service (separate repo) + Postgres + object storage (artifacts) + the shared contract lib (§9).
- **M1** Registry + public catalog API (§2.1–2.4) + plugin/version/manifest storage.
- **M2** Signing service (private key in a KMS/secret store) + artifact distribution/CDN + immutable versions (§4).
- **M3** Developer Portal (publish, version, manage; reject reserved codes; manifest v1 validation byte-compatible with §3).
- **M4** Install bundle + OAuth client provisioning (§2.5/§5) + Licensing & heartbeat (§2.6/§6).
- **M5** Review/Moderation (submission scanning: scope-overreach, manifest lint, artifact/dep scan) + Billing.
- **M6** Instance/tenant registration → issue instance API keys.

## 9. The clean link: a shared `wukong-plugin-contract` JAR (recommended)

Because both are Java, extract a tiny **dependency-free** module both repos depend on, so the schema/DTOs/signature scheme cannot drift:

- `PluginManifest` (+ nested `PluginMeta`, `ManifestCustomField`, `ManifestAiApplication`, ...) — the v1 model.
- Control-plane DTOs: `CatalogItem`, `VersionMetadata`, `InstallBundle`, `LicenseHeartbeatRequest/Response`.
- `SignatureVerifier` (verify) + `SignatureSigner` interface (marketplace impls with the private key).
- Constants: header names (`X-Plugin-Signature`, `X-Plugin-Event`), reserved app codes, contract version.

Publish to a private Maven repo (or git submodule). **Migration note:** the CRM currently has its own `PluginManifest` under `entity/BO`; when the shared lib lands, switch the CRM to the lib's class (or keep both in lockstep). Until then, **this document is the source of truth** and both sides must match it.

## 10. What the CRM instance still owes (P1-d, in THIS repo, later)

A `MarketplaceClient` (HTTP client for §2 + §6), signature verification (§4), a "register-from-install-bundle" path (§5 steps 2–6 reuse existing services), and a heartbeat scheduler — all **disableable**, endpoint+keys via `SystemConfigServiceImpl`. The offline `local-manifest` install path already exists and must keep working with the marketplace turned off.

---

## 11. Taxonomy (`type` × `category`)

Two **orthogonal** axes, both enums in the contract. `type` = the extension *form* (drives install mechanism + trust UX); `category` = the *functional domain* (drives browsing). The marketplace stores both on every plugin; the instance MAY use `type`/`trustTier` to choose the install/consent flow.

### 11.1 `type` — extension form (the structural "几大类")

| `type` | 形态 | install / runtime | `trustTier` |
|---|---|---|---|
| `ai_app` | AI 应用 / 助手 | declarative (`crm_ai_application` row) | `declarative` |
| `ai_skill` | AI 技能 / 工具(可被 LLM 调用) | out-of-process (gateway + OAuth scopes) | `connected` |
| `ai_provider` | AI 模型接入 | config (provider descriptor) | `declarative` |
| `integration` | 集成 / 连接器 | out-of-process (webhook + gateway) | `connected` |
| `automation` | 自动化 / 工作流 | out-of-process (event subscriptions) | `connected` |
| `data_extension` | 数据扩展(字段/字典/实体) | declarative (custom fields / schema) | `declarative` |
| `ui_module` | 界面 / 页面模块 | iframe sandbox | `sandboxed` |
| `widget` | 挂件 / 组件 | iframe sandbox | `sandboxed` |
| `theme` | 主题 / 皮肤 | declarative (theme tokens) | `declarative` |

### 11.2 `trustTier` — derived from `type` (drives the install UX)

- `declarative` — no code execution (data/model/theme/ai-app rows only). Instant install, confirm-only.
- `connected` — runs out-of-process; calls the instance via the Extension Gateway with `module:action` scopes. **Requires the scope-consent step.**
- `sandboxed` — frontend UI in an iframe; highest review bar; declares both a UI surface and any scopes.

### 11.3 `category` — functional domain (browsing)

`sales` · `enrichment` · `marketing` · `finance` · `communication` · `knowledge` · `analytics` · `productivity` · `industry` · `admin_security`

### 11.4 Consistency rule

A plugin's `type` MUST be consistent with what its manifest actually declares (e.g. `data_extension` ⇒ `customFields`; `ai_app` ⇒ `aiApplications`; `automation`/`integration` ⇒ `eventSubscriptions`/`requestedScopes`). The Developer Portal validates this at publish time; the instance enforces scopes/signing regardless of declared `type`.
