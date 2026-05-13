## 1. Configuration And Safety

> Deferred by user request for this apply pass.

- [ ] 1.1 Replace committed real database endpoints/passwords in `sync_data/src/main/resources/application.yml` with safe placeholders or localhost-only examples.
- [ ] 1.2 Add startup/runtime validation that reports missing source and target database configuration clearly.
- [ ] 1.3 Document required sync service environment variables and safe local run examples.

## 2. Preflight Validation

- [x] 2.1 Add a backend preflight service that checks source required tables/columns, target required tables/columns, selected binding conflicts, and destructive sync flags.
- [x] 2.2 Add a preflight API endpoint returning blocking errors, warnings, source row counts, and module coverage status.
- [x] 2.3 Update the frontend sync page to run preflight before bind/full sync and display blocking errors/warnings.

## 3. Asynchronous Full Sync Jobs

- [x] 3.1 Refactor full-sync start API to return `jobId` immediately and run migration in a backend executor.
- [x] 3.2 Ensure job and binding status transitions cover `running`, `completed`, `completed_with_errors`, `failed`, and interrupted/stale running jobs.
- [x] 3.3 Keep existing job/module/error query APIs compatible while adding any status fields needed by the frontend.

## 4. Frontend Progress And Error Visibility

- [x] 4.1 Replace fixed local progress completion with polling from job/module/error APIs.
- [x] 4.2 Render module-level totals, success/failure counts, status messages, and recent row errors.
- [x] 4.3 Keep the sync page resilient across refresh by hydrating current binding/job state from backend data.

## 5. Incremental Sync State

- [x] 5.1 Change frontend copy and controls so reserved incremental sync is not presented as a working data-sync feature.
- [x] 5.2 Update incremental event handling status/messages so received events are clearly recorded as reserved/unapplied unless real table mutation is implemented.
- [x] 5.3 Add a backend capability flag or status field describing whether incremental application is available.

## 6. Coverage, Rerun, And Retry Semantics

- [x] 6.1 Add a migration coverage matrix for supported, skipped, and unavailable WK CRM modules.
- [x] 6.2 Surface unsupported source modules such as opportunities, contracts, receivables, products, invoices, and approvals in preflight/job summaries.
- [x] 6.3 Verify reruns reuse `sync_mapping` IDs and cleanup remains scoped to the selected company mapping.
- [x] 6.4 Add operator-facing messaging for rerun, cleanup, and partial-failure retry behavior.

## 7. Tests And Verification

- [x] 7.1 Add backend tests for mapping idempotency, company scoping, job status transitions, module statistics, and row-level error recording.
- [x] 7.2 Add tests or typed fixtures for frontend sync flow state transitions and API response assumptions.
- [x] 7.3 Verify `mvn -q test` or targeted backend tests pass for `sync_data`.
- [x] 7.4 Verify `npm run build` passes for `frontend`.
