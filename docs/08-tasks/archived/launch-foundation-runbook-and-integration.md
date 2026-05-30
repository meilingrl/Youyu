# Task: Launch Foundation Runbook And Integration

## Metadata

- ID: launch-foundation-runbook-and-integration
- Status: completed
- Owner: main-agent
- Track: cross-cutting
- Depends on: all launch-foundation worker tasks
- Priority: high
- Planned date: 2026-05-30
- Completed date: 2026-05-30

## Objective

Integrate the launch foundation slices, run rehearsal checks and publish an honest operations runbook.

## Scope

- review and integrate worker diffs
- execute available test and rehearsal checks
- document startup, demo seed, backup restore, scanning, k6, rollback and blockers
- update changelog and task lifecycle records

## Out of Scope

- deferred capabilities listed in the parent task

## Allowed Changes

- launch foundation docs and runbook
- `CHANGELOG.md`
- task lifecycle files
- narrow integration fixes required to make approved child slices work together

## Acceptance Criteria

- [ ] Worker diffs are reviewed for scope.
- [ ] Verification evidence is recorded, including unavailable host capabilities.
- [ ] Runbook is executable and mock payment is a production blocker.
- [ ] Child tasks are archived only after integration review.

## Completion Notes

Reviewed all worker slices, performed the local rehearsal, added the staging
runbook, updated the changelog, and recorded production blockers and deferred
work honestly.
