## ADDED Requirements

### Requirement: Safe Runtime Configuration
The migration service SHALL NOT ship committed configuration that can connect to real source or target databases using real credentials by default.

#### Scenario: Missing deployment database configuration
- **WHEN** the migration service starts without required source or target database configuration
- **THEN** the service SHALL fail fast or expose the migration API as unavailable with a clear configuration error

#### Scenario: Local development configuration
- **WHEN** a developer runs the migration service locally
- **THEN** the service SHALL use explicitly provided environment variables or documented local-only sample values

### Requirement: Preflight Migration Validation
The system SHALL provide a preflight validation step before binding or starting a full migration for a company.

#### Scenario: Source schema is incompatible
- **WHEN** required source tables or columns are missing for the selected company migration
- **THEN** preflight SHALL return blocking errors and SHALL prevent full sync from starting

#### Scenario: Target schema has compatibility warnings
- **WHEN** target tables exist but optional compatibility columns or dynamic custom-field constraints need attention
- **THEN** preflight SHALL return warnings that are visible before the operator starts migration

#### Scenario: Migration scope is previewed
- **WHEN** preflight succeeds for a selected `company_id`
- **THEN** the response SHALL include row counts and module coverage status for supported, skipped, and unavailable modules

### Requirement: Asynchronous Full Sync Job Lifecycle
Starting a full migration SHALL create or update a job and return control to the client without waiting for all rows to finish processing.

#### Scenario: Full sync starts successfully
- **WHEN** the operator starts full sync for a valid binding
- **THEN** the API SHALL return a `jobId`, the binding SHALL be marked `running`, and the job SHALL be queryable immediately

#### Scenario: Full sync completes with row failures
- **WHEN** one or more rows fail but the job finishes all modules
- **THEN** the job SHALL end as `completed_with_errors`, the binding SHALL reflect that status, and row errors SHALL be queryable

#### Scenario: Full sync is interrupted
- **WHEN** the migration process stops while a job is running
- **THEN** the system SHALL later expose the job as failed or interrupted instead of leaving it indefinitely running

### Requirement: Backend-Sourced Progress And Errors
The frontend SHALL display migration progress, module state, totals, failures, and error summaries from backend job/module/error APIs rather than from fixed local simulation alone.

#### Scenario: Module progress updates
- **WHEN** backend module records change during migration
- **THEN** the frontend SHALL update the visible module progress and summary counts from those records

#### Scenario: Errors are available
- **WHEN** a job has row-level errors
- **THEN** the frontend SHALL show an actionable error count and a way to inspect recent module/source/error details

### Requirement: Honest Incremental Sync State
The system SHALL distinguish reserved incremental metadata from working incremental data application.

#### Scenario: Incremental data application is not implemented
- **WHEN** the operator views migration settings
- **THEN** the UI SHALL label incremental sync as unavailable or reserved and SHALL NOT imply that binlog/MQ changes will mutate target business tables

#### Scenario: Incremental event is received in reserved mode
- **WHEN** an incremental event is posted or consumed while data application is unavailable
- **THEN** the system SHALL record the event status as reserved/received and SHALL NOT mark it as successfully applied

### Requirement: Migration Coverage Matrix
The migration workflow SHALL expose which WK CRM modules are supported, skipped, or unavailable for the current AI CRM target model.

#### Scenario: Unsupported module exists in source data
- **WHEN** source data contains an unsupported module such as business opportunities, contracts, receivables, products, invoices, or approvals
- **THEN** preflight or job summary SHALL report that module as not migrated instead of silently omitting it

### Requirement: Safe Rerun And Retry
The migration system SHALL support rerunning a company migration without creating duplicate target records and without deleting unmapped manual data by default.

#### Scenario: Full sync is rerun
- **WHEN** the operator reruns full sync for the same binding with existing `sync_mapping` records
- **THEN** the migration SHALL reuse mapped target IDs and update target rows idempotently

#### Scenario: Cleanup is enabled
- **WHEN** cleanup before sync is explicitly enabled
- **THEN** deletion SHALL be scoped to the selected company mapping and SHALL be reported before execution

### Requirement: Migration Test Coverage
The project SHALL include automated tests for the highest-risk data migration behavior.

#### Scenario: Backend migration behavior is tested
- **WHEN** backend tests run
- **THEN** they SHALL cover mapping idempotency, company scoping, job status transitions, module statistics, and partial failure recording

#### Scenario: Frontend migration flow is tested or type-checked
- **WHEN** frontend verification runs
- **THEN** it SHALL validate the sync flow state transitions, API type assumptions, and build/type correctness for the migration page
