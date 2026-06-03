# Task: Launch Preparation Runtime Infrastructure

## Metadata

- ID: launch-preparation-runtime-infrastructure
- Status: completed
- Owner: worker-runtime
- Track: cross-cutting
- Depends on: launch-preparation-l0-scope-freeze
- Priority: P0
- Planned date: 2026-06-04
- Completed date: 2026-06-04

## Objective

Make the staging/runtime foundation deployable, observable, recoverable, and honest about remaining production blockers.

## Background

The repository already has a staging profile, Compose stack, and several launch-foundation standard documents. The launch roadmap still requires database connection pool production configuration, logging, health checks, backup/restore procedure, object-storage preparation, and data lifecycle documentation.

## Scope

- Review and improve staging/runtime datasource and Hikari configuration.
- Ensure health endpoints cover application and database health where practical.
- Review logging configuration for traceId and sensitive-field safety.
- Add or update backup/restore runbook scripts/docs without committing generated backups.
- Document object-storage migration boundary and rollback strategy for media assets.
- Document data lifecycle rules for search logs, old orders, deleted accounts, and media files.

## Out of Scope

- Running a real cloud backup service.
- Migrating product media to OSS in this wave.
- Introducing Redis or message queues unless needed for a documented minimal check.
- Declaring staging rehearsal production-ready.

## Files to Read

- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-staging.yml`
- `backend/src/main/java/com/youyu/backend/controller/HealthController.java`
- `backend/src/main/java/com/youyu/backend/filter/RequestTraceFilter.java`
- `docs/04-standards/launch-foundation-configuration.md`
- `docs/04-standards/launch-foundation-logging-and-health.md`
- `docs/04-standards/launch-foundation-backup-restore.md`
- `docs/03-architecture/data-management-and-privacy.md`

## Allowed Changes

- backend runtime configuration and narrowly related health/logging code/tests
- launch-foundation standards/runbook docs
- backup/restore helper scripts if they are safe, non-destructive by default, and do not embed secrets
- `.gitignore` only if needed to exclude generated backup/report artifacts
- this task document

## Implementation Plan

1. Inspect current staging datasource, health, trace logging, and backup docs.
2. Patch runtime config gaps that can be verified locally.
3. Add safe scripts or command docs for backup/restore rehearsal if missing.
4. Document object-storage and lifecycle boundaries as follow-up blockers where implementation exceeds this wave.
5. Run backend tests and any targeted health/config checks.

## Risks

- Backup scripts can become destructive if defaults are careless.
- Health checks can expose sensitive implementation details.
- Runtime docs can imply production readiness when only rehearsal has been verified.

## Test Plan

- Backend: `cd backend; .\mvnw.cmd test`
- Frontend: not expected.
- API validation: `/api/health` and actuator health if enabled.
- Manual: inspect backup/restore commands and confirm generated artifacts are ignored.

## Acceptance Criteria

- [x] Staging/runtime datasource and health behavior is documented and testable.
- [x] Logging can correlate requests with traceId without exposing secrets.
- [x] Backup/restore rehearsal procedure is executable in principle and preserves generated artifacts outside git.
- [x] Object-storage and data-lifecycle gaps are recorded as explicit follow-up work.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant standards/runbook docs
- [x] task status and archive move

## Completion Notes

- Added explicit staging Hikari pool settings with environment-variable overrides for pool size, timeouts, lifetime, leak detection, and startup fail-fast behavior.
- Kept default staging startup schema-only; `seed` remains opt-in and is not activated by `application-staging.yml`.
- Extended `/api/health` to include a database `UP`/`DOWN` probe without exposing JDBC URLs, usernames, hostnames, pool internals, SQL errors, or secrets.
- Kept Actuator exposure limited to health with `db` and `diskSpace` components and `show-details: never`.
- Updated launch-foundation configuration, logging/health, and backup/restore standards with validation commands, trace logging boundaries, object-storage migration boundaries, rollback expectations, and data lifecycle follow-up rules.
- Made backup pruning opt-in with `PRUNE_OLD_BACKUPS=true`; default backup creation is non-destructive and generated files remain under ignored `backups/mysql/`.
- Main-agent integration verified backend `.\mvnw.cmd test`, including `HealthEndpointTest`, after CORS and task-overlap fixes.
