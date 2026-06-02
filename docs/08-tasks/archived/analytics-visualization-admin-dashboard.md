# Task: Analytics Visualization For Admin Dashboard

## Metadata

- ID: analytics-visualization-admin-dashboard
- Status: completed
- Owner: unassigned
- Track: feature
- Depends on: `analytics-visualization-parent`, `analytics-visualization-execution-spec`
- Priority: high
- Planned date: 2026-06-02
- Completed date: 2026-06-02

## Objective

Upgrade the admin dashboard from a counts-first workbench into a more visual, still-operational analytics surface using real dashboard data and only narrow backend additions where they directly improve explanation.

## Background

`AdminServiceImpl.dashboard()` already returns:

- summary counts
- queue metrics
- governance signals
- status breakdowns
- a small unavailable-metrics section

The frontend dashboard currently renders these as stat cards and rows, which is operationally usable but not yet a convincing visualization layer.

## Scope

- improve visualization of admin dashboard summary and status breakdown data
- add narrow backend dashboard aggregation support only if the existing payload is insufficient
- preserve the dashboard's operational role as a workbench, not a vanity BI page

## Out of Scope

- global analytics platform design
- new telemetry ingestion pipelines
- unrelated admin module redesign
- payment/refund branch work

## Files to Read

- `frontend/src/views/admin/DashboardView.vue`
- `frontend/src/api/modules/admin.js`
- `backend/src/main/java/com/youyu/backend/service/admin/impl/AdminServiceImpl.java`
- `docs/09-api-spec/admin.md`

## Allowed Changes

- scoped admin dashboard frontend files
- narrow backend admin dashboard aggregation files
- related tests

## Implementation Plan

1. Identify the most valuable dashboard breakdowns to visualize more clearly.
2. Improve frontend presentation while preserving route-linked operational actions.
3. Add backend dashboard fields only if they materially improve the accepted visualization.

## Risks

- sacrificing operational clarity for decorative charts
- expanding the dashboard into broad analytics work that belongs to a later phase

## Test Plan

- Backend: run targeted admin dashboard tests if payload changes
- Frontend: run touched tests and `npm run build`
- API validation: update admin dashboard API/HTTP docs if payload changes
- Manual: verify the dashboard remains actionable and navigable after visualization changes

## Acceptance Criteria

- [x] Admin dashboard presents key queue/governance/status data more visually.
- [x] Dashboard links to operational pages remain intact.
- [x] Any new dashboard data is narrow, explicit, and documented.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes

- Kept the existing `GET /api/admin/dashboard` contract and route-linked operational cards intact.
- Added `salesAnalytics.categorySales` and `salesAnalytics.shopRankings` to `GET /api/admin/dashboard`.
- Added a transaction-data overview section in `frontend/src/views/admin/DashboardView.vue` for category sales share and shop sales rankings.
