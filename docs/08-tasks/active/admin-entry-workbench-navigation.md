# Task: Admin Entry Workbench Navigation

## Metadata

- ID: admin-entry-workbench-navigation
- Status: active
- Owner: unassigned
- Track: cross-cutting
- Depends on: current admin frontend baseline
- Priority: high
- Planned date: 2026-05-28
- Completed date:

## Objective

Make the admin experience feel like a dedicated workbench from login onward. Admin users should land in the admin workspace, see a clearer left navigation and dashboard entry, and not be guided through the buyer/seller application shell as their primary experience.

## Background

The current admin module already has an `/admin/*` layout and pages. The next product direction treats the current admin as the future highest-authority administrator. Multi-role pages are not part of this slice, but the admin entry and navigation should stop feeling like an extension of the regular user app.

## Scope

- Review current login redirect and role-based navigation behavior.
- Ensure admin login and authenticated admin default routing land on `/admin/dashboard`.
- Simplify the admin dashboard and navigation labels so they read as a work queue and governance console.
- Keep one runtime admin role for this task.
- Preserve ordinary user routing and buyer/seller flows.

## Out of Scope

- Multi-role permission implementation.
- New mediation, ticketing, audit-log, or observability backend features.
- Rebuilding all admin pages.
- Removing legitimate debug or explicit links unless the task confirms they break admin workflow.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/05-roadmap/current/admin-module-goal-roadmap.md`
- `frontend/README.md`
- `frontend/src/router/index.js`
- `frontend/src/router/guards.js`
- `frontend/src/router/modules/admin.js`
- `frontend/src/views/auth/LoginView.vue`
- `frontend/src/layouts/AdminLayout.vue`
- `frontend/src/components/layout/AdminSidebar.vue`
- `frontend/src/views/admin/DashboardView.vue`

## Allowed Changes

- Scoped frontend routing, login redirect, admin layout, admin navigation, and dashboard files.
- Relevant frontend tests if the current test structure supports them.
- `CHANGELOG.md`.
- This task document lifecycle updates.

## Implementation Plan

1. Identify the current admin login and redirect behavior from code.
2. Adjust the admin entry flow so admin users default to `/admin/dashboard`.
3. Simplify admin navigation and dashboard language around governance queues and workbench tasks.
4. Keep ordinary `/app/*` behavior intact for non-admin users.
5. Verify the routing and build behavior.

## File Scope

The implementation should stay in frontend auth/router/layout/dashboard surfaces unless a test file needs updating. Do not modify backend, database, API specs, or unrelated app views.

## API / Data Contract Impact

No API or database contract change is expected.

If the implementation discovers backend role data is insufficient for routing, stop and report the gap instead of inventing a new auth contract in this task.

## Risks

- Breaking ordinary user redirect behavior.
- Accidentally making admin-only pages available to normal users.
- Overbuilding the dashboard before real observability data exists.

## Verification Plan

- Frontend: run `npm test` from `frontend/`.
- Frontend: run `npm run build` from `frontend/`.
- Manual or automated smoke:
  - admin login or seeded admin session lands at `/admin/dashboard`;
  - ordinary user login still lands in the user app;
  - direct non-admin access to `/admin/*` remains blocked or redirected;
  - admin left navigation renders without layout overlap.

## Acceptance Criteria

- [ ] Admin login/default authenticated entry lands in `/admin/dashboard`.
- [ ] Non-admin login/default entry remains in the normal app.
- [ ] Admin layout is the primary admin experience and does not require visiting buyer/seller screens.
- [ ] Admin navigation labels and grouping are clearer for governance work.
- [ ] No backend, schema, or API contract changes are introduced.
- [ ] `frontend\npm test` passes.
- [ ] `frontend\npm run build` passes.
- [ ] `CHANGELOG.md` is updated.
- [ ] Completion notes are filled before archive.

## Sub-agent Instructions

- Do not start role-permission implementation in this task.
- Do not create new admin APIs.
- Keep changes narrow and explain any redirect behavior change in the final report.
- Return changed files, test results, screenshots or smoke notes if available, and any unresolved routing concerns.

## Feedback To Head Agent

Return:

- changed files;
- admin and non-admin redirect behavior after the change;
- verification commands and results;
- smoke notes for admin navigation;
- unresolved routing risks.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/` if no API changes are expected
- [ ] roadmap or standards docs if behavior decisions change
- [x] task status and archive move

## Completion Notes

(Filled in by implementing sub-agent and accepted by head Agent.)
