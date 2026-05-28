# Task: Platform Mediation Implementation

## Metadata

- ID: platform-mediation-implementation
- Status: blocked
- Owner: unassigned
- Track: cross-cutting
- Depends on: `docs/02-requirements/platform-mediation-scope.md`, accepted mediation boundary task
- Priority: high
- Planned date: 2026-05-28
- Completed date:

## Objective

Implement the v1 platform mediation workflow after the mediation boundary and contract are accepted.

## Background

The product direction is report escalation into formal mediation cases. Support console provides context and navigation; mediation owns the official platform decision. Admins may inspect related chat context read-only, but they cannot join user conversations in v1.

## Scope

- Add the backend, frontend, data, API docs, and verification needed for v1 mediation.
- Support creating or escalating a mediation case from an eligible report.
- Support admin case list/detail, status flow, and final decision recording.
- Expose related report/order/refund/chat context according to the accepted scope document.
- Add minimum seed data required to exercise the mediation flow locally.

## Out of Scope

- Three-party chat or admin message sending.
- Automated decision logic.
- Appeals.
- Full support ticketing, assignment, SLA, or workload balancing.
- Multi-role permission implementation beyond existing admin-only protection unless the accepted scope explicitly requires it.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/05-roadmap/current/admin-module-goal-roadmap.md`
- `docs/02-requirements/chat-mvp-scope.md`
- `docs/02-requirements/platform-mediation-scope.md`
- `docs/02-requirements/admin-support-console-scope.md`
- `docs/08-tasks/active/platform-mediation-boundary-and-contract.md` or its archived accepted version
- backend report/order/refund/chat/admin files
- frontend admin report/order/support files
- `backend/src/main/resources/schema.sql`
- `docs/06-http/admin.http`
- `docs/09-api-spec/admin.md`

## Allowed Changes

- Scoped backend mediation/report/order/admin/chat-read context files.
- Scoped frontend admin mediation/report/support/order files.
- Database schema and seed data needed for mediation.
- Backend and frontend tests.
- `docs/06-http/admin.http`.
- `docs/09-api-spec/admin.md`.
- `CHANGELOG.md`.
- This task document lifecycle updates.

## Implementation Plan

1. Re-read the accepted mediation scope and confirm this task is unblocked.
2. Implement the minimum v1 data model and backend workflow.
3. Add admin UI surfaces for list/detail/action flow.
4. Add read-only related context without enabling admin chat participation.
5. Update API docs, HTTP smoke, tests, seed data, and completion notes.

## File Scope

Changes may cross backend, frontend, database, HTTP docs, API spec, tests, and seed assets. Do not modify unrelated buyer/seller flows except for status visibility explicitly required by the accepted mediation scope.

## API / Data Contract Impact

This task is expected to add or change admin mediation contracts. The implementation must update `docs/09-api-spec/admin.md` or create a dedicated mediation API spec if the accepted boundary requires it. HTTP smoke coverage must be added in `docs/06-http/`.

## Risks

- Implementing a broader ticketing system instead of mediation v1.
- Making admin chat writable.
- Creating final decisions without auditability.
- Missing idempotency and invalid-transition tests.

## Verification Plan

- Backend: run focused mediation/admin tests if added.
- Backend: run `.\mvnw.cmd test` from `backend/`.
- Frontend: run `npm test` from `frontend/`.
- Frontend: run `npm run build` from `frontend/`.
- API validation: run or manually verify updated `.http` mediation smoke requests.
- Manual: exercise seeded report escalation, case detail, decision, and completed status.

## Acceptance Criteria

- [ ] The task was started only after accepted mediation scope exists.
- [ ] Eligible reports can be escalated into mediation cases.
- [ ] Mediation cases have an admin list/detail/action path.
- [ ] Final decisions are recorded and cannot be overwritten accidentally.
- [ ] Related chat context is read-only and scoped.
- [ ] New/changed contracts are documented in API spec and HTTP files.
- [ ] Seed data supports a local mediation scenario.
- [ ] Backend tests pass.
- [ ] Frontend tests and build pass.
- [ ] `CHANGELOG.md` is updated.
- [ ] Completion notes are filled before archive.

## Sub-agent Instructions

- Treat the accepted scope document as binding.
- Do not invent out-of-scope ticketing or chat participation features.
- Stop and report if the accepted scope lacks a necessary decision.
- Return changed files, contract changes, migration/seed summary, verification results, and remaining risks.

## Feedback To Head Agent

Return:

- changed files;
- implemented mediation workflow summary;
- API and data contract changes;
- seed data added for mediation;
- verification commands and results;
- remaining risks or blocked acceptance criteria.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [x] `docs/09-api-spec/`
- [x] task status and archive move

## Completion Notes

(Filled in by implementing sub-agent and accepted by head Agent.)
