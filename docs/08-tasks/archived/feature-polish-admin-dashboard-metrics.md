# Task: Admin Dashboard Metrics and Charts

## Metadata

- ID: feature-polish-admin-dashboard-metrics
- Status: completed
- Owner: worker-wave-2
- Track: feature
- Depends on: feature-polish-closeout-parent
- Priority: P1
- Planned date: 2026-06-03
- Completed date: 2026-06-03

## Objective

Complete the admin dashboard with trustworthy core metrics and chart-ready data for users, orders, sales, today's orders/sales, sales trend, hot products, and order status.

## Background

The frontend already has `frontend/src/views/admin/DashboardView.vue`, and the admin API wrapper exposes `getAdminDashboard()` for `/api/admin/dashboard`. This task should improve that contract rather than adding a duplicate dashboard endpoint.

## Scope

- Backend dashboard response must include:
  - total users
  - total orders
  - total sales amount
  - today's order count
  - today's sales amount
  - sales trend
  - hot product ranking
  - order status statistics
- Frontend dashboard must render metric cards and data charts/tables in a way that works with loading, empty, and error states.
- Keep chart implementation consistent with the current frontend dependency set.
- Clarify whether totals include paid/completed only or all orders.

## Out of Scope

- Operational monitoring dashboards.
- Real-time websocket dashboards.
- External BI tooling.
- Replacing admin layout/navigation.

## Files to Read

- `frontend/src/views/admin/DashboardView.vue`
- `frontend/src/api/modules/admin.js`
- `frontend/src/constants/insightMetrics.js`
- `backend/src/main/java/com/youyu/backend/controller/admin/AdminController.java`
- `backend/src/main/java/com/youyu/backend/service/admin/AdminService.java`
- `backend/src/main/java/com/youyu/backend/service/admin/impl/AdminServiceImpl.java`
- `backend/src/main/java/com/youyu/backend/mapper/order/OrderMapper.java`
- `backend/src/main/java/com/youyu/backend/mapper/product/ProductMapper.java`
- `backend/src/main/resources/schema.sql`
- `docs/09-api-spec/admin.md`
- `docs/06-http/admin.http`

## Allowed Changes

- Admin dashboard frontend and admin API wrapper.
- Admin service/mapper files needed to aggregate dashboard metrics.
- Focused backend tests for metric aggregation.
- `docs/09-api-spec/admin.md`
- `docs/06-http/admin.http`

## Implementation Plan

1. Inspect current `/api/admin/dashboard` payload.
2. Define metric semantics before implementation, especially sales amount and today's date boundary.
3. Add backend aggregation with stable field names.
4. Update frontend dashboard cards/charts using existing style conventions.
5. Add tests for empty data and seeded data where practical.
6. Update admin API spec and HTTP examples.

## Risks

- Sales totals can be incorrect if unpaid/refunded/canceled orders are included unintentionally.
- Timezone handling for "today" must match backend/runtime expectation.
- Chart UI can become misleading if the dataset is empty.

## Test Plan

- Backend: `cd backend; .\mvnw.cmd test`
- Frontend: `cd frontend; npm test`
- Frontend build: `cd frontend; npm run build`
- API validation: `/api/admin/dashboard`.
- Manual: admin dashboard with seed data and empty/test-like data.

## Acceptance Criteria

- [ ] Dashboard shows all requested core metrics.
- [ ] Dashboard renders sales trend, hot product ranking, and order status statistics.
- [ ] Backend metric semantics are documented.
- [ ] Empty/loading/error states are visible and polished.
- [ ] API spec and HTTP samples match the response shape.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] `docs/06-http/admin.http`
- [ ] `docs/09-api-spec/admin.md`
- [ ] task status and archive move

## Completion Notes

- Expanded `/api/admin/dashboard` with total sales, today's order/sales counters, seven-day trend data, hot products, and fuller order-status breakdowns.
- Corrected hot-product aggregation to use real `order_items` snapshots and aligned sales semantics with paid-not-refunded orders so the dashboard copy matches backend behavior.
