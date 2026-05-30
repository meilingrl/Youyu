# Task: Launch Foundation Backup Restore

## Metadata

- ID: launch-foundation-backup-restore
- Status: completed
- Owner: worker-c
- Track: cross-cutting
- Depends on: launch-foundation-scope-and-environments
- Priority: high
- Planned date: 2026-05-30
- Completed date: 2026-05-30

## Objective

Provide Linux Shell full-backup and guarded restore rehearsal tooling for MySQL.

## Scope

- timestamped gzip backup script with 7-day retention default
- restore script that rejects target database `youyu`
- rehearsal instructions and evidence template

## Out of Scope

- incremental backups, cloud RDS automation, Windows PowerShell scripts

## Allowed Changes

- `scripts/backup-mysql.sh`
- `scripts/restore-mysql.sh`
- backup-focused documentation under `docs/04-standards/`

## Acceptance Criteria

- [ ] Backup output is timestamped and compressed.
- [ ] Restore requires an explicit temporary target database.
- [ ] Restore rejects `youyu`.

## Completion Notes

Added compressed timestamped backup and guarded restore scripts. A live backup
was restored to `youyu_restore_foundation`; restoring over `youyu` was rejected.
