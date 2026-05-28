# Task: Admin Role Permission Model

## Metadata

- ID: admin-role-permission-model
- Status: completed
- Owner: Codex
- Track: cross-cutting
- Depends on: admin module roadmap role model, current auth baseline
- Priority: medium
- Planned date: 2026-05-28
- Completed date: 2026-05-28

## Objective

Define and implement a secure five-role admin permission model so future backend staff see and can operate only the admin capabilities assigned to their role.

## Background

The current backend has coarse `USER` and `ADMIN` roles. The product direction keeps the current admin as the future `super_admin` and introduces specialist roles later. This task must not stop at frontend route splitting; backend authorization and tests are mandatory.

## Scope

- Implement or document the minimum role/permission storage needed for five admin roles.
- Enforce permissions on backend admin endpoints.
- Align frontend admin routes and menu visibility with backend permissions.
- Preserve `super_admin` access to all current admin capabilities.
- Add role seed accounts for local verification.

## Out of Scope

- Fine-grained per-record ownership permissions.
- Full staff invitation or HR management workflow.
- External identity provider integration.
- Rewriting all auth flows beyond what this permission model requires.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/05-roadmap/current/admin-module-goal-roadmap.md`
- `backend/src/main/java/com/youyu/backend/common/auth/UserRole.java`
- `backend/src/main/java/com/youyu/backend/common/auth/LoginRequired.java`
- `backend/src/main/java/com/youyu/backend/filter/AuthInterceptor.java`
- backend admin controllers
- frontend admin router and sidebar files
- `backend/src/main/resources/schema.sql`
- seed assets currently used by the project

## Allowed Changes

- Backend auth, role, permission, schema, seed, and tests.
- Frontend admin route/menu guards and related tests.
- API docs and HTTP samples if auth behavior or seed tokens change.
- `CHANGELOG.md`.
- This task document lifecycle updates.

## Implementation Plan

1. Confirm the current auth model and seed login behavior.
2. Implement the five-role model: `super_admin`, `support_agent`, `reviewer`, `operator`, `order_admin`.
3. Protect backend admin endpoints by capability, not only by frontend route visibility.
4. Align frontend navigation and route access with backend capabilities.
5. Add seed accounts and tests for allowed and forbidden access.

## File Scope

This is a cross-cutting security task. Keep changes limited to auth/permission, admin route/menu, seed, docs, and tests.

## API / Data Contract Impact

Auth and authorization semantics will change. Update API docs where role requirements are stated. If response envelopes or login payload roles change, document that explicitly.

## Risks

- Frontend-only permission implementation.
- Breaking existing admin tests by renaming `ADMIN` without compatibility handling.
- Leaving endpoints unprotected because menu links are hidden.
- Adding too many roles or permissions beyond the agreed five-role minimum.

## Verification Plan

- Backend: tests for each role accessing allowed and forbidden representative endpoints.
- Backend: run `.\mvnw.cmd test` from `backend/`.
- Frontend: tests for route/menu behavior if testable.
- Frontend: run `npm test` and `npm run build` from `frontend/`.
- Manual: verify seed accounts land on appropriate admin surfaces.

## Acceptance Criteria

- [x] Five admin roles are represented consistently in data/auth code.
- [x] `super_admin` keeps full admin access.
- [x] Specialist roles have restricted backend access.
- [x] Frontend admin navigation reflects role permissions.
- [x] Unauthorized role access is rejected by backend tests.
- [x] Seed data includes representative admin staff accounts.
- [x] Required tests/build pass.
- [x] API/auth docs are updated.
- [x] `CHANGELOG.md` is updated.
- [x] Completion notes are filled before archive.

## Sub-agent Instructions

- Do not implement only frontend hiding.
- Preserve a compatibility path for current admin login unless the task is explicitly updated.
- Return the role-permission matrix, changed files, tests, and any endpoint coverage gaps.

## Feedback To Head Agent

Return:

- final role-permission matrix;
- backend endpoints covered by permission tests;
- frontend route/menu behavior changed;
- changed files;
- verification commands and results;
- remaining endpoints needing permission coverage.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/` if auth/API samples change
- [x] `docs/09-api-spec/` if role requirements change
- [x] task status and archive move

## Head Agent Notes

- 2026-05-28: Unblocked after `admin-audit-log-foundation` was completed and committed. Current auth baseline is `LoginRequired` plus `UserRole`, and the five-role model is defined in `docs/05-roadmap/current/admin-module-goal-roadmap.md`.

## Completion Notes

- Added backend role/capability model:
  - legacy `ADMIN`: full access compatibility role
  - `SUPER_ADMIN`: full access
  - `SUPPORT_AGENT`: dashboard, support context, user/product/shop context, reports, search logs, order read, mediation handling except final decisions
  - `REVIEWER`: dashboard, verification review, product context/review, shop context/manage
  - `OPERATOR`: dashboard, product context, search governance, search logs
  - `ORDER_ADMIN`: dashboard, order read/manage, mediation handling except final decisions
- Extended `@LoginRequired` with backend `AdminPermission` checks and annotated admin governance, order, mediation, search, and audit endpoints.
- Preserved legacy `ADMIN` mock tokens and login behavior while adding underscore-role mock tokens such as `mock-9103-REVIEWER`.
- Added frontend admin permission helpers, route guard enforcement, and sidebar filtering so visible navigation matches backend capabilities.
- Added seed staff accounts: `superadmin`, `supportagent`, `reviewer`, `operator`, and `orderadmin`, all using seed password `admin123`.
- Updated `docs/09-api-spec/admin.md`, `docs/09-api-spec/auth.md`, `docs/09-api-spec/order.md`, `docs/09-api-spec/mediation.md`, `docs/06-http/admin.http`, `docs/06-http/auth.http`, `backend/README.md`, and `CHANGELOG.md`.
- Verification:
  - `backend/ .\mvnw.cmd test` passed, 141 tests.
  - `frontend/ npm test` passed, 38 tests.
  - `frontend/ npm run build` passed.
- Remaining endpoint coverage gaps:
  - Tests cover representative allowed/forbidden endpoints for every role, but not every endpoint/role pair exhaustively.
  - No UI page for managing staff roles; this remains out of scope for the current task.
