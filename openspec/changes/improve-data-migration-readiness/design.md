## Context

`sync_data` is an independent Spring Boot service that reads WK CRM MySQL data and writes directly to the AI CRM PostgreSQL schema. It already supports tenant/company binding, `sync_mapping` idempotency, metadata tables for full jobs/modules/errors, and a frontend entry page under `/sync`.

Current gaps found during inspection:

- Full sync is executed inside `POST /sync/bindings/{bindingId}/full-sync`, so the request blocks until the migration finishes.
- Frontend progress is simulated locally and only loads job details after the blocking request returns.
- Incremental sync records received events, but it does not apply insert/update/delete changes to target business tables.
- Runtime defaults include concrete database hosts and passwords in `application.yml`.
- The implemented module set is limited to tenants, departments, roles, users, customers, contacts, custom fields, follow-ups, schedules, and tasks.
- There are no source/target preflight results exposed to the UI before starting a migration.
- There is no automated test coverage under `sync_data/src/test/java`, although the module has `spring-boot-starter-test`.

Both `mvn -q -DskipTests package` in `sync_data` and `npm run build` in `frontend` currently pass, so the change is about readiness and behavior rather than restoring a broken build.

## Goals / Non-Goals

**Goals:**

- Make full migration jobs observable and safe to run from the frontend.
- Give operators accurate preflight, progress, module, and error information.
- Prevent unsafe default connections and accidental production writes.
- Clarify what is migrated, what is explicitly unsupported, and how unsupported modules are surfaced.
- Establish retry/rerun behavior based on existing `sync_mapping` semantics.
- Make incremental sync status honest: unavailable/reserved unless target-table mutation is implemented.
- Add automated tests for the highest-risk migration behavior.

**Non-Goals:**

- Rebuilding the migration service into the main backend service.
- Implementing every omitted WK CRM business module in the first pass.
- Implementing bidirectional sync.
- Replacing the current direct SQL migration strategy with calls into main backend business Services.

## Decisions

1. Use asynchronous full-sync jobs instead of blocking HTTP.

   The start endpoint will create/mark a job and return immediately with a `jobId`. A backend executor will run the migration, update `sync_full_job`, `sync_job_module`, and `sync_company_binding`, and expose status through existing job query endpoints.

   Alternative considered: keep a long request and raise timeout limits. This keeps the UI simple but leaves browser, proxy, and service restarts unable to report reliable state.

2. Make backend module statistics the source of truth for progress.

   The frontend will poll job and module endpoints and derive progress from module totals/success/fail/status. Simulated progress can remain only as a visual placeholder before the first real module update arrives.

   Alternative considered: keep fixed UI progress ranges. This looks smooth but reports false completion and hides modules with failures.

3. Add a preflight endpoint before binding/full sync.

   The service will inspect required source tables/columns, target tables/columns, row counts, binding conflicts, incremental availability, and destructive flags such as `truncate-before-sync`. The frontend will present blocking errors and warnings before starting.

   Alternative considered: fail during migration. That creates partial jobs and makes preventable setup problems harder to understand.

4. Treat incremental sync as a declared mode with capability status.

   Until event-to-table mutation exists, the UI must label incremental as "reserved/unavailable" rather than offering it as an enabled sync option. If implemented later, event handling must route by source table, reuse `sync_mapping`, apply upsert/delete, update checkpoints, and record success/failure per event.

   Alternative considered: keep storing MQ topic/group only. That is useful as metadata but misleading when shown beside working full sync.

5. Remove real connection defaults from committed configuration.

   `application.yml` will keep non-sensitive placeholders or localhost-safe examples, while real values must come from environment variables or deployment secrets.

   Alternative considered: keep defaults for convenience. This is risky because the migration service performs direct writes and can target real databases.

6. Keep mapping-based idempotency as the rerun model.

   Reruns should update existing mapped target rows. Optional cleanup must remain scoped by binding/company and must clearly report what will be deleted. Partial module retry should reuse mappings and update module/job stats.

   Alternative considered: delete-and-reload by default. That increases data-loss risk and can remove manually repaired target data.

## Risks / Trade-offs

- Async execution can leave a job in `running` after process termination -> On startup or job query, mark stale running jobs as `failed` or `unknown_interrupted` with a clear message.
- Direct SQL writes can drift from main backend validations -> Add target schema compatibility checks and migration-specific tests for required fields and enum conversions.
- Row counts can be large -> Use lightweight count queries and paginated module reads; avoid loading full source datasets during preflight.
- Dynamic custom-field columns can grow unbounded -> Continue scoping column names by company/field, and add preflight warnings for target column count/identifier limits.
- Omitted modules can create business expectation gaps -> Show a coverage matrix in UI/docs and mark unsupported modules as skipped, not silently absent.
- Removing committed defaults may break local convenience -> Provide `.env`/README examples without real credentials.

## Migration Plan

1. Introduce preflight and async job APIs while keeping existing query endpoints compatible.
2. Update the frontend flow to call preflight, start jobs, poll status, and render module/error details.
3. Sanitize committed sync service configuration and document required environment variables.
4. Add tests for backend mapping/job behavior and frontend flow state.
5. Decide separately which omitted WK CRM modules have target models ready enough to implement.

Rollback strategy: keep existing metadata tables backward compatible. If async launch must be rolled back, the old blocking endpoint behavior can be restored while preserving job/module/error records.

## Open Questions

- Which target AI CRM models are authoritative for business opportunities, contracts, receivables, products, invoices, and approvals?
- Should migration service access be limited by network/deployment boundary only, or should it require an application-level admin token?
- What is the expected maximum migration duration and data volume for a single `company_id`?
- Should partial retry be module-level only, or should operators be able to retry selected failed rows?
