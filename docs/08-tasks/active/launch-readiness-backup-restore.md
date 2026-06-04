# Task: Launch Readiness Backup Restore

## Metadata

- ID: launch-readiness-backup-restore
- Status: active
- Owner: Codex
- Track: cross-cutting
- Depends on: launch-preparation roadmap L3/L7; runtime infrastructure wave 1
- Priority: P0
- Planned date: 2026-06-04
- Completed date:

## Objective

Prove the launch candidate has a documented, non-destructive MySQL backup and restore rehearsal path.

## Background

Wave 1 documented backup helper defaults and kept generated backups out of git. Launch readiness still requires a restore drill to a temporary database, retention/encryption guidance, failure alerts, and evidence that the procedure does not depend on developer-only state.

## Current State

Completed in earlier launch-preparation waves:

- `scripts/backup-mysql.sh` and `scripts/restore-mysql.sh` exist.
- Restore is guarded to `youyu_restore_*` databases and refuses direct restore over `youyu`.
- `backups/mysql/` is ignored.
- The backup/restore runbook includes commands, safe defaults, verification checks, lifecycle notes, and an evidence template.
- The launch-foundation runbook records a 2026-05-30 restore rehearsal to a temporary database.

Remaining work:

- Production retention, encryption, off-site storage, alert owner, RPO, and RTO need environment-specific approval.
- A final release-candidate restore rehearsal should be recorded if the data baseline changes.

## Scope

- Verify backup command defaults are safe, non-destructive, and secret-free.
- Define backup schedule, retention, encryption, storage location, and alert requirements.
- Restore a backup into a temporary database or document environment blockers.
- Validate restored schema/data at a smoke level without mutating the source database.
- Document RPO/RTO assumptions and production managed-database responsibilities.

## Out of Scope

- Running backups against a real production database.
- Adding destructive prune behavior by default.
- Migrating to a managed backup service in this task.
- Object-storage media migration beyond documenting backup boundaries.

## Files to Read

- `docs/04-standards/launch-foundation-backup-restore.md`
- `docs/04-standards/operations-and-deployment.md`
- `docs/03-architecture/data-management-and-privacy.md`
- `scripts/`
- `compose.yml`
- `compose.demo.yml`
- `backend/src/main/resources/schema.sql`
- `backend/src/main/resources/seed/`

## Allowed Changes

- Safe backup/restore helper scripts and related docs.
- `.gitignore` only for generated backup or restore artifacts.
- Runtime/runbook docs that define backup, restore, retention, encryption, and alert procedures.
- `CHANGELOG.md` and this task lifecycle record.

## Implementation Plan

1. Inspect current backup helpers, ignored paths, and database startup paths.
2. Patch scripts or docs so backup and restore commands are explicit and non-destructive by default.
3. Run or dry-run a restore rehearsal to a temporary database where possible.
4. Record RPO/RTO, retention, encryption, storage, alerting, and ownership decisions.

## Risks

- Restore drills can overwrite local data if target database names are ambiguous.
- Backups can leak credentials or personal data if stored under the repo.
- Managed database backup settings are external and may remain blocked.

## Test Plan

- Backend: not expected unless health/runtime code changes.
- Frontend: not applicable.
- API validation: health check after restored database startup if a temporary restore is run.
- Manual: backup command, restore command to temporary DB, row-count/schema smoke checks, generated artifact cleanup verification.

## Acceptance Criteria

- [x] Backup and restore commands are documented with safe defaults and no committed secrets.
- [x] Restore rehearsal succeeded to a temporary database in the launch-foundation evidence record.
- [ ] Retention, encryption, storage, RPO, RTO, and alert ownership are approved for the selected environment.
- [x] Generated backup and restore artifacts remain outside git.
- [x] L7 launch checklist can reference the latest restore evidence.
- [ ] Final release-candidate restore evidence is refreshed if the database baseline changes.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] backup/restore and operations standards
- [ ] task status and archive move

## Completion Notes

- 2026-06-04 sync: foundation backup/restore scripts and runbook are complete for staging rehearsal.
- 2026-06-04 remaining: production backup policy choices, alert ownership, and any final environment-specific restore rehearsal stay open.
