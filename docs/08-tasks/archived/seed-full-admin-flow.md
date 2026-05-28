# Task: Seed Full Admin Flow

## Metadata

- ID: seed-full-admin-flow
- Status: completed
- Owner: Codex
- Track: cross-cutting
- Depends on: admin entry, mediation implementation, audit log foundation, role permission model
- Priority: medium
- Planned date: 2026-05-28
- Completed date: 2026-05-28

## Objective

Complete local/demo seed data so the full admin workflow can be operated and verified without manually fabricating records.

## Background

The admin module needs end-to-end verification data for users, sellers, products, reviews, orders, refunds, reports, mediation, audit logs, and role-specific staff accounts. Seed data should be realistic enough to validate flows while remaining small and maintainable.

## Scope

- Audit current seed/demo data sources.
- Add or adjust data for every core admin queue.
- Include role-specific admin staff accounts after role permissions exist.
- Include at least one full dispute path from user report to mediation outcome after mediation exists.
- Document the local verification walkthrough.

## Out of Scope

- Large production-like datasets.
- Randomized data generation unless the repo already uses it.
- Performance/load testing.
- Business logic shortcuts that bypass normal validation.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/05-roadmap/current/admin-module-goal-roadmap.md`
- database README and seed assets
- `backend/src/main/resources/schema.sql`
- backend test fixtures
- admin HTTP smoke files
- relevant active/archived admin tasks

## Allowed Changes

- Database seed/demo assets.
- Backend fixture/test seed helpers.
- HTTP smoke files and local verification docs.
- Tests that rely on seeded admin flows.
- `CHANGELOG.md`.
- This task document lifecycle updates.

## Implementation Plan

1. Inventory current seed coverage for admin queues.
2. Define a small canonical demo dataset.
3. Add missing records for admin staff, pending review, reports, dispute orders, mediation, and audit examples.
4. Update HTTP/manual verification docs.
5. Run tests and smoke flows that use the seed data.

## File Scope

Expected changes are seed/demo/test/docs files. Do not change production business logic to make seed data fit.

## API / Data Contract Impact

No API contract change is expected. If seeded tokens, users, or sample IDs in HTTP files change, update the affected docs.

## Risks

- Seed data drifting from schema or validation rules.
- Adding too much data and making tests brittle.
- Masking missing features with manually inserted impossible states.

## Verification Plan

- Backend: run `.\mvnw.cmd test` from `backend/`.
- Frontend: run `npm test` and `npm run build` if frontend verification depends on seeded labels/routes.
- API validation: run or manually verify admin HTTP smoke files that use seeded data.
- Manual: follow the documented full admin flow from login to queue handling.

## Acceptance Criteria

- [x] Seed data covers the current admin workbench and all implemented core queues.
- [x] Seed data includes role-specific admin accounts after role implementation.
- [x] Seed data includes a full dispute/mediation scenario after mediation implementation.
- [x] Seed records are compatible with schema and business validation.
- [x] Verification walkthrough is documented.
- [x] Required tests and smoke checks pass.
- [x] `CHANGELOG.md` is updated.
- [x] Completion notes are filled before archive.

## Sub-agent Instructions

- Do not create impossible states just to satisfy UI screenshots.
- Keep the dataset minimal and named clearly.
- Return seeded scenarios, sample credentials, changed files, verification commands, and known remaining gaps.

## Feedback To Head Agent

Return:

- seeded scenarios and sample credentials;
- changed seed/docs/test files;
- full-flow verification steps;
- verification commands and results;
- remaining data gaps.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [x] roadmap or standards docs if applicable (not required)
- [x] task status and archive move

## Completion Notes

- Current seed coverage includes:
  - dashboard queues for pending verification, product review, shop review, reports, orders, and mediation;
  - role-specific staff accounts: `superadmin`, `supportagent`, `reviewer`, `operator`, `orderadmin`, all using password `admin123`;
  - order-backed report scenarios `6010`, `6011`, `6012`;
  - active mediation case `70001` and resolved mediation case `70002`;
  - read-only order chat context for mediation via order `8008`;
  - seeded admin audit logs `92001` through `92006`.
- Added `docs/06-http/admin-full-flow.http` as the local walkthrough from dashboard to queues, order/report/mediation context, audit logs, and role permission smoke checks.
- Updated `CHANGELOG.md`.
- Verification:
  - `backend/ .\mvnw.cmd test` passed.
  - `frontend/ npm test` passed.
  - `frontend/ npm run build` passed.
  - `git diff --check` passed.

## Head Agent Notes

- 2026-05-28: Unblocked after `admin-role-permission-model` was completed and committed. All listed dependencies are now archived.
