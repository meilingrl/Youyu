# Task: Admin Dashboard Observability

## Metadata

- ID: admin-dashboard-observability
- Status: completed
- Owner: Codex
- Track: cross-cutting
- Depends on: admin entry workbench, current admin dashboard endpoint, mediation contract if mediation counts are included
- Priority: medium
- Planned date: 2026-05-28
- Completed date: 2026-05-28

## Objective

Upgrade the admin dashboard from a static entry page into a task-oriented workbench that shows real pending work, review queues, dispute status, and operational signals.

## Background

The admin module already has many governance pages. The dashboard should help administrators understand what needs attention: pending verification, pending product review, reports, disputes, abnormal orders, and processing progress. The first observability slice should prioritize trustworthy counts and links over decorative charts.

## Scope

- Audit current admin dashboard data.
- Define and render task cards backed by existing or newly documented admin data.
- Add charts only where the data source is real and the visualization improves scanning.
- Link each metric to the owning admin page.
- Mark unavailable metrics clearly instead of faking counts.

## Out of Scope

- Complex BI system.
- Large visual redesign unrelated to task observability.
- Fake chart data presented as real.
- Role-based dashboard personalization.
- Mediation implementation if not already available.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/05-roadmap/current/admin-module-goal-roadmap.md`
- `docs/09-api-spec/admin.md`
- `docs/06-http/admin.http`
- backend admin dashboard service/controller files
- frontend `DashboardView.vue`
- current admin list pages for target links

## Allowed Changes

- Scoped backend admin dashboard data aggregation if needed.
- Scoped frontend dashboard view/components.
- API spec and HTTP docs if dashboard contract changes.
- Tests related to dashboard aggregation/rendering.
- `CHANGELOG.md`.
- This task document lifecycle updates.

## Implementation Plan

1. Inventory dashboard metrics available from current code.
2. Select a small set of real pending-work metrics and links.
3. Extend the dashboard contract only where necessary.
4. Render a clear task-oriented dashboard.
5. Verify data, routing links, and build/tests.

## File Scope

Expected scope is admin dashboard backend/frontend and related tests/docs. Do not change unrelated governance flows to force a metric.

## API / Data Contract Impact

If the dashboard response changes, update `docs/09-api-spec/admin.md` and `docs/06-http/admin.http`.

## Risks

- Adding misleading metrics without reliable data.
- Over-optimizing visuals before the workflows are stable.
- Creating duplicate counting logic inconsistent with owner modules.

## Verification Plan

- Backend: run focused dashboard tests if backend changes.
- Backend: run `.\mvnw.cmd test` from `backend/` if backend changes.
- Frontend: run `npm test` from `frontend/`.
- Frontend: run `npm run build` from `frontend/`.
- Manual: verify each dashboard card links to its owner page and displays real or explicitly unavailable data.

## Acceptance Criteria

- [x] Dashboard shows pending-work signals for available admin queues.
- [x] Every metric links to a responsible admin page or is explicitly marked unavailable.
- [x] No fake operational data is presented as real.
- [x] API docs and HTTP samples are updated if the dashboard contract changes.
- [x] Required tests/build pass.
- [x] `CHANGELOG.md` is updated.
- [x] Completion notes are filled before archive.

## Sub-agent Instructions

- Prefer a small truthful dashboard over a broad decorative one.
- Do not invent mediation metrics before mediation exists.
- Return metric list, data source for each metric, changed files, verification results, and unavailable metrics.

## Feedback To Head Agent

Return:

- dashboard metrics implemented and their data sources;
- unavailable metrics intentionally left out;
- changed files;
- verification commands and results;
- follow-up metrics or contract gaps.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/` if dashboard API changes
- [x] `docs/09-api-spec/` if dashboard API changes
- [x] task status and archive move

## Completion Notes

- Confirmed mediation implementation is present and used `mediation_cases` counts for dashboard metrics.
- Extended `/api/admin/dashboard` with stable `summary`, `queueMetrics`, `governanceSignals`, `statusBreakdowns`, and `unavailableMetrics` fields while retaining legacy `cards`, `shortcuts`, and `todo`.
- Implemented dashboard metrics from real backend sources:
  - `student_verifications.verification_status = pending_review`
  - `product_review_tasks.review_status = pending_review`
  - `reports.status = pending` / `processing`
  - `shops.review_status = pending_review`
  - `orders.order_status = pending_fulfillment`, `pending_receipt`, `refunding`
  - `mediation_cases.status = opened`, `evidence_review`, `decision_pending`
  - `users.status = disabled`, `student_verifications.risk_flag = true`, `products.review_status = rejected`
- Marked audit-log and role-permission alert metrics unavailable instead of inventing data.
- Updated `docs/09-api-spec/admin.md`, `docs/06-http/admin.http`, `CHANGELOG.md`, and backend dashboard test coverage.
- Verification:
  - `backend/ .\mvnw.cmd test` passed.
  - `frontend/ npm test` passed.
  - `frontend/ npm run build` passed.
