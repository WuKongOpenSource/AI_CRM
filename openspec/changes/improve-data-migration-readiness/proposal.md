## Why

The current data migration feature has a useful foundation for binding an AI CRM tenant to a WK CRM company and running an idempotent full sync, but it is still closer to an operator tool than a production-ready migration workflow. Before relying on it for tenant onboarding or real customer cutover, the feature needs clearer safety controls, asynchronous job handling, migration completeness, validation, and recovery behavior.

## What Changes

- Harden runtime configuration so database endpoints and passwords are not shipped as real defaults, and migration service access is explicit.
- Convert full sync triggering from a long blocking HTTP request into an observable asynchronous job lifecycle with polling, cancellation-safe status, and clear failure states.
- Replace simulated frontend progress with backend job/module progress, module messages, and actionable error summaries.
- Expand migration coverage planning for currently omitted WK CRM modules such as business opportunities, contracts, receivables, products, invoices, and approvals when target models exist.
- Add preflight checks for source schema compatibility, target schema compatibility, row counts, duplicate-risk checks, and dry-run reporting visible from the UI.
- Make incremental sync either explicitly disabled as unavailable or implement a real event application path for supported tables; do not present the reserved MQ option as working sync.
- Add repeat-run, retry, and partial failure handling rules so operators can safely rerun a company migration without data loss or unclear state.
- Add focused automated tests around mapping idempotency, company scoping, module statistics, frontend sync flow state, and incremental event status.

## Capabilities

### New Capabilities
- `data-migration-readiness`: Production-readiness requirements for tenant-bound WK CRM to AI CRM migration, including configuration safety, full-sync job lifecycle, progress/error visibility, validation, module coverage, incremental-state behavior, and retry semantics.

### Modified Capabilities

None.

## Impact

- Affected backend service: `sync_data/src/main/java/com/kakarote/syncdata/**`.
- Affected frontend UI/API: `frontend/src/views/sync/SyncDataView.vue`, `frontend/src/api/syncData.ts`, and `/syncapi` proxy assumptions.
- Affected configuration: `sync_data/src/main/resources/application.yml`, environment variable requirements, and deployment/runtime documentation.
- Affected metadata tables: `sync_full_job`, `sync_job_module`, `sync_job_error`, `sync_company_binding`, `sync_mapping`, and `sync_incremental_event_log`.
- Verification impact: adds backend unit/integration tests and frontend build/type checks for migration flow behavior.
