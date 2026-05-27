# Task: Admin Support Console Implementation

## Metadata

- ID: admin-support-console-implementation
- Status: active
- Owner: unassigned
- Track: cross-cutting
- Depends on: `docs/02-requirements/admin-support-console-scope.md`; current `/admin/support` route; current admin/report/order/search API modules
- Priority: medium
- Planned date: 2026-05-27
- Completed date:

## Objective

Replace the reserved `/admin/support` placeholder with the v1 support context dashboard defined in `docs/02-requirements/admin-support-console-scope.md`, using only existing admin-owned APIs and frontend routes.

## Background / Current State

`frontend/src/views/admin/SupportView.vue` is currently a reserved-state page. It names support conversations, after-sales assistance, group governance, and abnormal messages, but it does not fetch live data or define ownership.

The support-console scope document narrows v1 to a context dashboard. Reports, orders/refunds, user/shop/product governance, and search governance already have owner pages and APIs. Chat and notifications exist for `USER` role flows, but they are not admin support data sources. Platform mediation is blocked because `docs/02-requirements/platform-mediation-scope.md` is absent.

## Problem Statement

The admin navigation exposes `/admin/support`, but the page remains too generic to guide operators. Implementing a full support system would duplicate report/order/chat responsibilities. The first safe implementation must show live context from existing owner modules while leaving all actions with those modules.

## Scope

- Update `frontend/src/views/admin/SupportView.vue` into a support context dashboard.
- Use existing APIs only:
  - `getAdminDashboard`
  - `getAdminReports`
  - `getAdminUsers`
  - `getAdminShops`
  - `getAdminProducts`
  - `getAdminReviewTasks`
  - `getAdminSearchLogs`
  - `getAdminOrderList`
- Show lane cards for:
  - report triage
  - order/refund assistance
  - user/shop/product governance context
  - search/risk signal context
  - blocked mediation
  - missing admin chat/group governance
- Link action buttons to existing owner routes:
  - `/admin/reports`
  - `/admin/orders`
  - `/admin/users`
  - `/admin/shops`
  - `/admin/products`
  - `/admin/review-tasks`
  - `/admin/hot-search`
- Include loading, empty, and error states using existing frontend conventions.
- Keep unavailable lanes visibly blocked or missing instead of showing fake data.

## Out of Scope

- Backend changes.
- Schema or seed changes.
- New `/api/admin/support/**` endpoints.
- New report, order, refund, chat, notification, or mediation behavior.
- Calling `/api/chat/**` or `/api/notifications/**` from the admin support page.
- Support tickets, assignment, SLA, internal notes, group governance, abnormal-message detection, and platform mediation.
- Changes to `CLAUDE.md`.

## Implementation Plan

1. Read `AGENTS.md`, `CLAUDE.md`, `docs/README.md`, `docs/04-standards/development-process.md`, `frontend/README.md`, and `docs/02-requirements/admin-support-console-scope.md`.
2. Read `frontend/src/views/admin/SupportView.vue`, `frontend/src/router/modules/admin.js`, `frontend/src/constants/navigation.js`, `frontend/src/api/modules/admin.js`, and `frontend/src/api/modules/order.js`.
3. Replace placeholder-only arrays in `SupportView.vue` with lane definitions that include owner module, route target, API data source, and blocked/missing state.
4. Fetch the existing admin data in parallel on mount:
   - dashboard snapshot
   - reports filtered to pending, first page
   - order list
   - users, shops, products, and review tasks first pages
   - search logs first page
5. Derive conservative summary counts only from returned data. If an API does not expose a count, label the value as a visible sample count or hide it.
6. Render context cards and queue previews with existing admin visual language. Use disabled or informational controls for missing lanes.
7. Keep all mutations out of the support page. Use router links/buttons to navigate to owner pages for action.
8. Add or update focused frontend tests only if the current test setup has a matching pattern for admin view data loading. Otherwise record manual verification.
9. Update `CHANGELOG.md` and this task's Completion Notes when complete. Archive only after head-Agent acceptance.

## File Scope

### Must Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/04-standards/development-process.md`
- `frontend/README.md`
- `docs/02-requirements/admin-support-console-scope.md`
- `docs/09-api-spec/admin.md`
- `docs/09-api-spec/order.md`
- `docs/09-api-spec/report.md`
- `docs/09-api-spec/search.md`
- `frontend/src/views/admin/SupportView.vue`
- `frontend/src/router/modules/admin.js`
- `frontend/src/constants/navigation.js`
- `frontend/src/api/modules/admin.js`
- `frontend/src/api/modules/order.js`

### Allowed Changes

- `frontend/src/views/admin/SupportView.vue`
- frontend test files directly covering `SupportView.vue`, if an existing pattern is present
- `CHANGELOG.md`
- `docs/08-tasks/active/admin-support-console-implementation.md`

### Not Allowed

- `backend/src/**`
- `backend/src/main/resources/schema.sql`
- seed SQL files
- `frontend/src/api/modules/chat.js`
- `frontend/src/api/modules/notification.js`
- `CLAUDE.md`
- existing ADR rewrites
- archived task rewrites

## API / Data Contract Impact

No runtime API contract changes are allowed.

The page may consume existing endpoints only:

- `GET /api/admin/dashboard`
- `GET /api/admin/reports`
- `GET /api/admin/users`
- `GET /api/admin/shops`
- `GET /api/admin/products`
- `GET /api/admin/review-tasks`
- `GET /api/admin/search/logs`
- `GET /api/admin/orders`

Do not add request parameters to `GET /api/admin/orders`; current runtime controller ignores filters and pagination. Do not call user-role chat or notification endpoints from the admin page.

## Risks

- Accidentally presenting missing lanes as live capabilities.
- Treating reports as support tickets.
- Treating order/refund operations as support-console-owned actions.
- Calling user-only chat or notification endpoints with an admin token.
- Creating fake metrics when an endpoint returns only a sample list.
- Reopening mediation decisions before platform mediation scope exists.

## Verification Plan

- Frontend unit tests: run `npm test` from `frontend/` if tests are added or affected.
- Frontend build: run `npm run build` from `frontend/`.
- Manual UI smoke: run the frontend against a seeded backend if practical and open `/admin/support` as admin.
- Documentation check:
  - confirm no backend, schema, seed, chat API, or notification API files changed
  - confirm unavailable lanes are labeled blocked/missing
  - confirm support dashboard actions navigate to owner pages

## Acceptance Criteria

- [ ] `/admin/support` renders live support context from existing admin endpoints.
- [ ] The page labels each lane with its owner module and v1 state.
- [ ] Report, order/refund, governance, and search lanes link to their owner pages.
- [ ] Chat, notification, group governance, abnormal-message detection, and mediation are not implemented as live support-console features.
- [ ] No `/api/admin/support/**` endpoint is introduced.
- [ ] No backend, schema, or seed file is modified.
- [ ] No user-only `/api/chat/**` or `/api/notifications/**` endpoint is called from the admin support page.
- [ ] Loading, empty, and error states are visible.
- [ ] `CHANGELOG.md` is prepended with the completed implementation entry.
- [ ] Completion Notes record verification commands and any residual limitations.

## Sub-agent Instructions

You are not alone in the codebase. Do not revert edits made by others. Work only within this task's File Scope.

Implement the frontend-only support context dashboard described here and in `docs/02-requirements/admin-support-console-scope.md`. Keep all backend contracts unchanged. Do not add support-ticket, mediation, admin-chat, group-governance, notification, schema, seed, or `AdminDataStore` logic.

If implementation requires a new backend endpoint or product decision, stop and report the blocker instead of widening the task.

## Completion Notes

(Filled in after implementation delivery and head-Agent acceptance.)
