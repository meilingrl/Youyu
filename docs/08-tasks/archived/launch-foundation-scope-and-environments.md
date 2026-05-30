# Task: Launch Foundation Scope And Environments

## Metadata

- ID: launch-foundation-scope-and-environments
- Status: completed
- Owner: main-agent
- Track: cross-cutting
- Depends on: none
- Priority: high
- Planned date: 2026-05-30
- Completed date: 2026-05-30

## Objective

Freeze the first launch-foundation wave and distinguish local development, staging rehearsal, explicit demo seed, and deferred production work.

## Scope

- document environment boundaries and production blockers
- define staging rehearsal acceptance and deferred items
- preserve existing CI seed flow

## Out of Scope

- implementation owned by sibling child tasks
- remote production deployment

## Allowed Changes

- `docs/08-tasks/active/launch-foundation*.md`
- launch foundation runbook documentation

## Acceptance Criteria

- [ ] Default staging rehearsal and explicit demo seed modes are unambiguous.
- [ ] Mock payment is listed as a real-production blocker.
- [ ] Deferred items match the parent task.

## Completion Notes

Locked the schema-only staging default, explicit `staging,seed` demo overlay,
mock-payment production blocker, required environment variables, and deferred
capabilities before worker implementation.
