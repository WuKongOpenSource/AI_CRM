## ADDED Requirements

### Requirement: Marketplace Client Connectivity
The Marketplace Client inside the instance SHALL connect to the centralized control-plane API to browse the catalog, fetch plugin version metadata, download plugin artifacts, and run license heartbeats.

#### Scenario: Browse the marketplace catalog
- **WHEN** an enabled Marketplace Client requests the plugin catalog from the configured control-plane endpoint
- **THEN** the instance SHALL return the available plugins with their listed versions without persisting any install state

#### Scenario: Fetch a specific version and download its artifact
- **WHEN** the operator selects a plugin version to install
- **THEN** the Marketplace Client SHALL fetch that version's metadata (including version, signature, signing key id, and content hash) and download the corresponding artifact before any install step runs

### Requirement: Mandatory Artifact Signature Verification
The instance SHALL verify each plugin version's signature against a pinned public key BEFORE install and SHALL refuse installation on verification failure.

#### Scenario: Signature verification fails
- **WHEN** a downloaded plugin artifact or manifest fails signature verification against the pinned public key (unsigned, tampered, or signed by an untrusted key)
- **THEN** the instance SHALL block the install, SHALL NOT apply any declarative extension or issue any plugin token, and SHALL surface a verification error

#### Scenario: Downgrade or replaced version is rejected
- **WHEN** an artifact presents a version older than or different from the version the operator pinned, or whose content hash does not match the signed version metadata
- **THEN** the instance SHALL refuse the install and SHALL report a version-integrity error

### Requirement: Plugin Version Immutability
Published plugin versions SHALL be immutable; a fix SHALL be published as a new version and SHALL NOT mutate an existing version.

#### Scenario: A fix is published
- **WHEN** a plugin author needs to correct a released version
- **THEN** the control-plane contract SHALL require a new version identifier and the instance SHALL treat the existing pinned version's signed content as unchanged

#### Scenario: Re-fetch of a pinned version is stable
- **WHEN** the instance re-fetches the metadata for an already-installed pinned version
- **THEN** the version, signature, and content hash SHALL be identical to what was verified at install time, otherwise the instance SHALL flag a tamper condition

### Requirement: License Verification With Offline Grace Period
License validity SHALL be checked online but SHALL tolerate transient marketplace unreachability via an offline grace period and SHALL NOT hard-fail installed plugins during a brief outage.

#### Scenario: Marketplace unreachable during heartbeat
- **WHEN** a scheduled license heartbeat cannot reach the control plane while a previously valid license is within the configured offline grace period
- **THEN** installed plugins SHALL keep running and the instance SHALL record the missed heartbeat and surface a non-fatal warning

#### Scenario: Grace period elapses without a successful check
- **WHEN** the offline grace period elapses with no successful online license verification
- **THEN** the instance SHALL mark the affected plugins as license-unverified and MAY disable them, while keeping the install records intact

#### Scenario: License is explicitly revoked online
- **WHEN** an online heartbeat returns an explicit revoked or expired license result
- **THEN** the instance SHALL disable the affected plugins and revoke their plugin tokens

### Requirement: Per-Instance Pinned Install Records
Installs SHALL be recorded in `crm_plugin_install` with an explicitly pinned version, scoped through `InstanceScopeResolver` and carrying the reserved nullable `tenant_id`.

#### Scenario: An operator installs a specific pinned version
- **WHEN** an admin installs a plugin at a chosen version
- **THEN** the instance SHALL write a `crm_plugin_install` row capturing the plugin id, the pinned version, the enabled flag, and the encrypted config, and SHALL NOT auto-upgrade that install to a newer version

#### Scenario: Install records resolve through the instance scope
- **WHEN** install records are queried
- **THEN** they SHALL be read through `InstanceScopeResolver` (the single instance today), with `tenant_id` reserved so a future tenant interceptor can scope them without schema change

### Requirement: Disableable Marketplace With Local-Manifest Install Path
The Marketplace Client SHALL be fully disableable and SHALL offer a local-manifest install path that never contacts the control plane, so open-source and air-gapped instances work with the marketplace off.

#### Scenario: Self-host with marketplace disabled installs a local signed manifest
- **WHEN** the Marketplace Client is disabled and an operator installs a plugin from a locally provided signed manifest
- **THEN** the instance SHALL verify the manifest signature against the pinned public key, apply the install with a pinned version, and SHALL NOT make any outbound call to the control plane

#### Scenario: Catalog browsing while disabled
- **WHEN** the Marketplace Client is disabled and a user opens the marketplace UI
- **THEN** the instance SHALL indicate that the catalog is unavailable and SHALL still permit the local-manifest install path

### Requirement: Externalized Marketplace Configuration
The marketplace endpoint, keys, and enable/disable switch SHALL be stored via `SystemConfigServiceImpl` and SHALL NOT be hardcoded under a static property.

#### Scenario: Configuring the marketplace endpoint
- **WHEN** an administrator sets or changes the marketplace endpoint, credentials, or enabled flag
- **THEN** the values SHALL be read through `SystemConfigServiceImpl` (Redis `system:config:*`, 30-min TTL), and the change SHALL take effect without a code change or redeploy

#### Scenario: No committed marketplace secrets
- **WHEN** the codebase is inspected for marketplace connectivity settings
- **THEN** no real endpoint URL or credential SHALL be hardcoded as a static property and any stored credential SHALL be encrypted via `SecretTextCipher`

### Requirement: Abstract Control-Plane API Contract
The control-plane API contract (catalog, version metadata, signed artifact, license check) SHALL be specified abstractly as a client interface, and building the SaaS itself SHALL be out of scope for this change.

#### Scenario: Contract is defined without the SaaS
- **WHEN** the instance integrates against the control plane
- **THEN** the catalog, version-metadata, signed-artifact, and license-check operations SHALL be defined as an abstract client interface that a separate closed-source SaaS implements, and this change SHALL NOT include the SaaS implementation

#### Scenario: Contract supports the pinned-key trust model
- **WHEN** the abstract contract describes version metadata
- **THEN** it SHALL include the fields required for signature verification (version, signature, signing key id, content hash) so the instance can enforce the pinned-public-key trust model independently of the SaaS
