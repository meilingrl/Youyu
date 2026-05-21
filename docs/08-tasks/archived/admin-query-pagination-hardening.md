# Task: Admin Query Pagination Hardening

## Metadata

- ID: admin-query-pagination-hardening
- Status: archived
- Owner: meilingrl
- Track: cross-cutting
- Depends on: architecture-performance-hardening
- Priority: high
- Planned date: 2026-05-17
- Completed date: 2026-05-17

## Objective

Replace the current admin-side full-table reads and Java-side filtering with SQL-backed filtering, sorting, counting, and pagination, while keeping the admin interaction model understandable and keeping scope tightly limited to the admin query layer.

This task is intended to be executed in parallel with other hardening tasks. It is therefore deliberately scoped to the admin module query path and must not expand into unrelated schema, review, search, or storefront refactors.

## Background

The current admin implementation has reached a point where several list and dashboard endpoints still rely on:

- mapper `findAll()` methods
- service-layer `stream().filter(...)`
- in-memory sorting and summary calculation

This was acceptable during small-data MVP delivery, but it is now a structural bottleneck:

- list latency grows with total table size rather than requested page size
- memory pressure rises as more records accumulate
- frontends cannot scale naturally into pagination
- every additional admin capability risks copying the same anti-pattern

Representative current evidence includes:

- `backend/src/main/java/com/campusmarket/backend/service/admin/impl/AdminServiceImpl.java`
- dashboard metric assembly through repeated full-list reads
- list endpoints for users, verifications, products, review tasks, shops, and reports
- admin frontend list pages that currently consume whole arrays without a shared pagination contract

## Scope

### In Scope

- backend admin list endpoints:
  - `/api/admin/users`
  - `/api/admin/verifications`
  - `/api/admin/products`
  - `/api/admin/review-tasks`
  - `/api/admin/shops`
  - `/api/admin/reports`
- backend admin dashboard query behavior if and only if a bounded optimization can be made without touching unrelated domains
- mapper-level query methods for filtering, sorting, paging, and total count
- admin frontend pages that must adapt to paginated response structures
- related HTTP smoke assets and admin API spec documentation

### In Scope But Only If Needed To Support Pagination

- small request-parameter additions such as `page` and `pageSize`
- dedicated response wrapper fields such as `items`, `total`, `page`, and `pageSize`
- localized frontend pagination controls and loading state adjustments

## Out of Scope

- any schema/index work in `backend/src/main/resources/schema.sql`
- product search behavior changes
- storefront product list page changes
- review submission flow changes
- order, payment, refund, or report domain business-rule redesign
- broad admin UI redesign unrelated to paginated data consumption

## Files to Read

- `docs/04-standards/development-process.md`
- `docs/05-roadmap/current/stage-roadmap.md`
- `backend/README.md`
- `frontend/README.md`
- `backend/src/main/java/com/campusmarket/backend/controller/admin/AdminController.java`
- `backend/src/main/java/com/campusmarket/backend/service/admin/AdminService.java`
- `backend/src/main/java/com/campusmarket/backend/service/admin/impl/AdminServiceImpl.java`
- relevant mapper interfaces and JDBC implementations:
  - `UserMapper`
  - `StudentVerificationMapper`
  - `ProductMapper`
  - `ProductReviewTaskMapper`
  - `ShopMapper`
  - `ReportMapper`
- relevant admin frontend pages under `frontend/src/views/admin/`
- `docs/06-http/admin.http`
- `docs/09-api-spec/admin.md` if updated during this task

## Allowed Changes

- `backend/src/main/java/com/campusmarket/backend/controller/admin/**`
- `backend/src/main/java/com/campusmarket/backend/service/admin/**`
- admin-related mapper interfaces and JDBC implementations
- `frontend/src/views/admin/**`
- admin-related API client modules under `frontend/src/api/`
- `docs/06-http/admin.http`
- `docs/09-api-spec/admin.md`
- `CHANGELOG.md`

## Parallelization Boundary

This task is designed to be parallel-safe with:

- `review-order-lookup-hardening`
- `runtime-index-hardening`
- `product-list-request-flow-hardening`

### Do Not Touch In This Task

- `backend/src/main/resources/schema.sql`
- `backend/src/main/java/com/campusmarket/backend/service/review/**`
- `backend/src/main/java/com/campusmarket/backend/service/transaction/**`
- `backend/src/main/java/com/campusmarket/backend/mapper/product/impl/JdbcProductMapper.java` except where strictly required for admin-only product listing support
- `frontend/src/views/app/**`
- `frontend/src/stores/market.js`

If a necessary change would require these files, stop and open a coordination note instead of silently expanding scope.

## Required Response Contract Direction

Admin list endpoints should converge on a stable structure like:

```json
{
  "items": [],
  "total": 0,
  "page": 1,
  "pageSize": 10,
  "summary": {}
}
```

Notes:

- `summary` may stay if it remains useful to the page
- `summary` should not force a second full-table in-memory scan
- endpoint semantics should stay explainable and consistent

## Implementation Plan

1. Inventory each admin endpoint's current behavior.
   - identify filters
   - identify current sorting rules
   - identify whether summary values are page-local or global

2. Define the pagination contract.
   - normalize `page` and `pageSize`
   - cap `pageSize`
   - document default values

3. Push list filtering and sorting into mapper SQL.
   - add paged query methods
   - add matching count methods
   - avoid building one giant generic abstraction if explicit methods stay clearer

4. Rework the admin service layer.
   - remove `findAll().stream().filter(...)` list logic from the main list path
   - keep service code focused on orchestration and response shaping

5. Update the frontend admin pages.
   - consume paged responses
   - add or adapt `el-pagination` where needed
   - preserve loading, empty, and error states

6. Update docs and verification assets.
   - update `docs/06-http/admin.http`
   - update `docs/09-api-spec/admin.md` if the contract is now formalized or changed

## Risks

- inconsistent pagination contract across admin modules
- accidentally mixing page-local counts with global counts in `summary`
- hidden frontend assumptions that still expect arrays instead of `{ items, total }`
- expanding into schema/index work and colliding with another task
- modifying too many mappers at once without preserving current semantics

## Test Plan

- Backend:
  - add or update controller/service tests for each paginated admin endpoint
  - verify filters, page boundaries, totals, and empty states
- Frontend:
  - verify admin page pagination renders and page switching works
  - verify query and filter interaction remains stable
- API validation:
  - update `docs/06-http/admin.http`
  - validate representative user, product, shop, and report list requests
- Manual:
  - open each admin list page
  - change filters
  - navigate between pages
  - verify totals and summaries remain believable

## Acceptance Criteria

- [x] Main admin list endpoints no longer rely on full-table in-memory filtering for list assembly
- [x] Each target endpoint supports explicit page and pageSize behavior
- [x] Frontend admin pages correctly render paginated data and preserve loading/empty/error behavior
- [x] Response contracts are documented and smoke assets are updated
- [x] No changes were made to schema, review-path logic, or storefront product-list flow

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes

This task is one parallel execution slice split from `architecture-performance-hardening.md`.

It exists specifically to keep admin-query hardening isolated from:

- schema/index hardening
- review/order lookup hardening
- storefront request-flow cleanup

Any proposed change that would cross that boundary should be documented and reassigned instead of merged into this task.

Completed outcome:

- Migrated 6 target admin list endpoints to SQL-backed paging and filtering
- Added page/pageSize request parameters and `{ items, total, page, pageSize }` response structure
- Updated admin frontend list pages to consume paged responses and render pagination controls
- Updated `docs/06-http/admin.http` and `docs/09-api-spec/admin.md`
