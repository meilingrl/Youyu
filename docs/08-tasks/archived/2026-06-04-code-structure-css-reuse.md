# Task: Code Structure And CSS Reuse Optimization

## Metadata

- ID: 2026-06-04-code-structure-css-reuse
- Status: archived
- Owner: Codex
- Track: cross-cutting
- Depends on: none
- Priority: P1
- Planned date: 2026-06-04
- Completed date: 2026-06-04

## Objective

Reduce oversized frontend and backend files while preserving existing behavior, response shapes, routing, and visual results.

## Background

The repository contains several large frontend views and backend service classes. This task focuses on low-risk structural cleanup only: style extraction, reusable CSS utilities, and moving CSV export assembly out of the admin service implementation.

## Scope

- Move scoped CSS from the message center and admin support SFCs into adjacent CSS files.
- Add reusable global surface/card utility classes for repeated support-admin panel styling.
- Move admin support view option/status metadata into a sidecar module.
- Extract admin dashboard and CSV export assembly into dedicated builder components.
- Keep business logic, API contracts, and rendered UI semantics unchanged.

## Out of Scope

- Feature behavior changes.
- Endpoint or response shape changes.
- Dashboard payload refactoring.
- Broad design restyling.
- Database changes.

## Files Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/04-standards/development-process.md`
- `docs/05-roadmap/current/stage-roadmap.md`
- `docs/05-roadmap/current/launch-preparation-roadmap.md`
- `frontend/README.md`
- `backend/README.md`
- `database/README.md`

## Changes

- Added `frontend/src/views/app/MessagesView.css` and switched `MessagesView.vue` to `style scoped src`.
- Added `frontend/src/views/admin/SupportView.css` and switched `SupportView.vue` to `style scoped src`.
- Added `frontend/src/views/admin/support-view-options.js` for admin support view constants and status metadata.
- Added `ui-surface-panel` and `ui-stack-card` utilities in `frontend/src/styles/index.css`, then reused them in the admin support view.
- Added `AdminDashboardBuilder` and delegated admin dashboard response assembly from `AdminServiceImpl`.
- Added `AdminCsvExportBuilder` and delegated admin CSV export generation from `AdminServiceImpl`.

## Risks

- SFC external style loading could regress if Vite/Vue did not preserve scoped processing.
- Constructor injection changes could fail Spring context creation.

## Test Plan

- Backend: `.\mvnw.cmd test`
- Frontend: `npm test`
- Frontend build: `npm run build`
- API validation: not required, no endpoint contract changes.
- Manual: not required for this structure-only cleanup.

## Acceptance Criteria

- [x] Existing backend tests pass.
- [x] Existing frontend tests pass.
- [x] Frontend production build passes.
- [x] Message/support SFC line counts are reduced without changing runtime logic.
- [x] `AdminServiceImpl` is reduced below 800 lines by moving dashboard/export assembly into focused builders.
- [x] Admin CSV export behavior remains delegated through the existing `AdminService` API.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/` - not applicable.
- [ ] roadmap or standards docs if applicable - not applicable.
- [x] task status and archive move

## Completion Notes

The optimization intentionally avoided changing chat-flow behavior, API contracts, and dashboard field structure. The completed changes are structural and verified by the existing backend/frontend test suites plus frontend production build.
