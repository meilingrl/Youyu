# Task: Architecture And Performance Hardening

## Metadata

- ID: architecture-performance-hardening
- Status: in-progress (Wave 1 delivered; Slices D and F still open)
- Owner: meilingrl
- Track: cross-cutting
- Depends on: current marketplace transaction flow, admin governance baseline, hot-search P3, test foundation expansion
- Priority: high
- Planned date: 2026-05-17
- Completed date:

## Wave 1 Status (verified 2026-05-22)

- [x] Slice A: Admin pagination and filtering baseline — delivered under archived child task `admin-query-pagination-hardening`
- [x] Slice B: Review-path lookup hardening — delivered under archived child task `review-order-lookup-hardening`
- [x] Slice C: Order / payment / refund / report index hardening — delivered under archived child task `runtime-index-hardening`
- [ ] Slice D: Product search path improvement — **NOT STARTED**; `ProductServiceImpl.findPublicByFiltersPaged` still uses `LOWER(...) LIKE '%kw%'` against the products table with no specialized index strategy
- [x] Slice E: Frontend product-list request cleanup — delivered under archived child task `product-list-request-flow-hardening`
- [ ] Slice F: Configuration safety cleanup — **PARTIAL**; `application.yml` already gates DB password behind `${MYSQL_PASSWORD:...}` but the JWT secret default is still the literal `campusmarket-dev-secret-key-replace-in-production-min32` checked into the file with no env-var override path

## Children Tasks

Wave 2 splits Slices D and F into dispatchable child tasks following the wave-1 pattern:

- Slice D → `docs/08-tasks/active/product-search-path-hardening.md`
- Slice F → `docs/08-tasks/active/configuration-safety-hardening.md`

Once both children archive, this parent task can also be archived (or kept as draft if Wave 3 is planned).



## Objective

Create a structured remediation task for the hidden architecture and performance risks that have started to appear in the mid-stage codebase, so the project can continue adding features without silently accumulating response-time, scalability, and maintainability debt.

This task is not a broad rewrite. Its purpose is to identify and then execute a bounded set of high-leverage hardening changes on the most sensitive paths:

- admin list and dashboard data access
- order / payment / refund / report list scalability
- product search query path
- review submission lookup path
- frontend product-list request flow and metadata loading
- configuration safety around secrets and environment defaults

## Background

CampusMarket has already moved beyond early scaffolding. Core transaction flow, search governance, review, recommendation, report, and admin capabilities are present, and the repository has entered a post-MVP expansion stage. At this stage, the most important risks are no longer obvious syntax or missing-feature issues, but hidden structural issues that still work under small data volume while becoming increasingly expensive as data size and feature count grow.

The current codebase still retains several MVP-style implementation patterns that are acceptable during rapid feature landing, but are now risky for mid-stage evolution:

1. application-layer filtering over full-table result sets
2. list endpoints without database-side pagination
3. business validations implemented through table scans or N+1-style lookup paths
4. schema indexes that cover only part of the real runtime query patterns
5. frontend flows that issue extra requests to derive stable metadata from unstable page data
6. development defaults for secrets and credentials that are too loose for later deployment or collaboration scenarios

The point of this task is to convert the architecture review findings into an execution-ready governance and refactoring plan before these patterns spread into additional modules.

## Problem Summary

### 1. Admin data access is still MVP-style

Current admin service methods frequently call `findAll()` on mapper layers and then perform filtering, sorting, and summary calculation in Java streams. This is visible in dashboard assembly and in multiple admin list endpoints.

This pattern has several side effects:

- every admin page load scales with table size, not page size
- database pagination and filtering power is bypassed
- memory usage grows with total record count
- summary metrics and list data often require repeated full reads
- adding more admin modules will multiply the same cost pattern

Representative code paths:

- `backend/src/main/java/com/campusmarket/backend/service/admin/impl/AdminServiceImpl.java`
- `dashboard()`
- `listUsers()`
- `listProducts()`
- `listVerifications()`
- `listReviewTasks()`
- `listShops()`
- `listReports()`
- detail methods that again derive related data through repeated `findAll()` scans

### 2. Review submission contains an order-wide scan path

`ReviewServiceImpl.submitProductReview()` currently resolves `orderItemId -> order` by scanning all orders and then scanning each order's items. The code itself already notes that this is a production-unfriendly placeholder.

This creates a hidden risk because review submission is a user-facing interactive path, not a low-frequency admin script. Under higher order volume, latency will grow sharply and unpredictably.

Representative code paths:

- `backend/src/main/java/com/campusmarket/backend/service/review/impl/ReviewServiceImpl.java`
- `findOrderByOrderItemId()`
- `backend/src/main/java/com/campusmarket/backend/service/transaction/support/TransactionDataStore.java`
- `listOrders()`
- `findOrderItems()`

### 3. Search query strategy is still limited for growth

The product list search path has already moved to server-side pagination, which is a good baseline, but the keyword matching strategy still relies on multiple `LOWER(...) LIKE '%keyword%'` conditions across several columns, plus a second count query. The current runtime schema has basic status and type indexes, but it does not yet reflect the real search workload.

Risks:

- low selectivity under broader catalogs
- poor index use for substring matching
- repeated scan cost for data query plus count query
- search performance becoming a blocker before the rest of the marketplace grows

Representative code paths:

- `backend/src/main/java/com/campusmarket/backend/mapper/product/impl/JdbcProductMapper.java`
- `findPublicByFiltersPaged()`
- `countPublicByFilters()`
- `backend/src/main/resources/schema.sql`

### 4. Order-related tables lack enough explicit support for real query paths

The business logic already depends heavily on access by:

- `buyer_user_id`
- `seller_user_id`
- `order_id`
- `submitted_at`
- `status`
- `payment_status`
- `refund_status`

But the runtime schema currently defines only a small subset of supporting indexes for these paths. Some foreign-key-backed relationships may receive implicit support depending on engine behavior, but the schema does not clearly express the intended indexing strategy for order, payment, refund, and report growth.

Risks:

- slower order list and detail queries as records accumulate
- slower refund/payment lookup under after-sales operations
- future debugging becoming harder because performance characteristics are implicit rather than deliberate

### 5. Frontend product list has an avoidable double-fetch pattern

`ProductListView.vue` currently primes category options by calling `marketStore.loadProducts()` once before the route-driven real search request. Categories are therefore derived from whatever product page happened to be loaded, instead of coming from a stable source of truth.

Risks:

- unnecessary extra network request on first load
- more loading time on list entry
- category metadata coupled to current product page contents
- store semantics becoming less predictable as more views reuse the same product state

Representative code paths:

- `frontend/src/views/app/ProductListView.vue`
- `ensureCategoryOptions()`
- `loadProductsByRoute()`
- `frontend/src/stores/market.js`

### 6. Configuration safety needs tightening

The backend configuration still contains permissive development defaults for the datasource password and JWT secret. This may be acceptable for a private local-only stage, but it is no longer a healthy baseline for a repository that is now structurally richer and closer to staged demonstration or deployment.

Risks:

- accidental reuse of insecure defaults in non-local contexts
- environment drift across collaborators
- weak secret-handling discipline spreading into later features

Representative file:

- `backend/src/main/resources/application.yml`

## Scope

### In Scope

- Replace full-read + Java-stream filtering patterns in high-value admin endpoints with SQL-backed filtering and pagination
- Introduce explicit page / pageSize contracts for admin list endpoints that currently return full datasets
- Add dedicated mapper queries for order-item-to-order lookup used by review submission
- Reduce obvious N+1 and full-table scan patterns on transaction and review paths
- Review and add missing runtime indexes for the main order / payment / refund / report / search access paths
- Improve product search query strategy within the current JDBC architecture
- Remove the product-list metadata bootstrap double-fetch on the frontend
- Tighten config defaults for secrets and credentials so insecure local fallbacks are not silently reused
- Update associated docs, HTTP examples, API specs, and task records as changes become concrete

### In Scope But Only As Small, Local Changes

- introducing small request DTOs or query objects for pagination parameters where needed
- adding focused mapper methods instead of broad service rewrites
- adding targeted schema indexes that match actual query paths
- splitting unstable store responsibilities if required to keep product metadata and product results separate

## Out of Scope

- full ORM migration
- search-engine migration to Elasticsearch or similar infrastructure
- broad frontend redesign
- chat / mediation / recommendation feature expansion
- wholesale replacement of the existing controller -> service -> mapper layering
- changing course-project repository structure or build tools
- introducing distributed cache, message queue, or microservice decomposition

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/04-standards/development-process.md`
- `docs/05-roadmap/current/stage-roadmap.md`
- `docs/05-roadmap/current/feature-roadmap.md`
- `backend/README.md`
- `frontend/README.md`
- `backend/src/main/resources/schema.sql`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/campusmarket/backend/service/admin/impl/AdminServiceImpl.java`
- `backend/src/main/java/com/campusmarket/backend/service/review/impl/ReviewServiceImpl.java`
- `backend/src/main/java/com/campusmarket/backend/service/transaction/support/TransactionDataStore.java`
- `backend/src/main/java/com/campusmarket/backend/mapper/product/impl/JdbcProductMapper.java`
- `backend/src/main/java/com/campusmarket/backend/controller/admin/AdminController.java`
- `backend/src/main/java/com/campusmarket/backend/controller/order/OrderController.java`
- `frontend/src/views/app/ProductListView.vue`
- `frontend/src/stores/market.js`
- related mapper interfaces and JDBC implementations for user, report, shop, review task, search log, order, payment, and refund access

## Allowed Changes

- backend admin service / mapper / controller modules relevant to pagination and filtering
- backend review and transaction support modules relevant to review-path lookup performance
- backend runtime schema and related tests
- frontend product-list and related store modules
- docs in `docs/06-http/`, `docs/08-tasks/`, and `docs/09-api-spec/`
- `CHANGELOG.md`

## Architecture Principles For This Task

1. Prefer query-path hardening over aesthetic refactor.
2. Keep the existing layered architecture.
3. Push filtering, sorting, counting, and pagination into SQL where practical.
4. Do not introduce generalized abstraction that is larger than the real problem.
5. Optimize the most expensive paths first:
   - admin list endpoints
   - order/review lookup paths
   - product search list path
6. Preserve current API semantics unless there is a strong reason to revise them.
7. Any response-shape changes must be synchronized with API spec and HTTP smoke assets.

## Suggested Implementation Slices

### Slice A: Admin pagination and filtering baseline

Goal:
Move admin list endpoints from full-read in-memory filtering to database-backed pagination and filtering.

Minimum target modules:

- users
- verifications
- products
- review tasks
- shops
- reports

Expected outputs:

- `page`, `pageSize`, `total`, `items` contract on admin list endpoints
- mapper methods with query conditions pushed into SQL
- summary metrics kept either as separate aggregate queries or clearly bounded derived data
- frontend admin pages updated to consume paginated responses

### Slice B: Review-path lookup hardening

Goal:
Remove the order-wide scan used during product review submission.

Expected outputs:

- direct query for locating the owning order and buyer by `order_item_id`
- no full-order scan inside review submission
- regression tests for valid, foreign-user, and missing-order-item cases

### Slice C: Order / payment / refund / report index hardening

Goal:
Make schema intent match actual runtime read patterns.

Expected outputs:

- index review document or explicit index additions in `schema.sql`
- indexes for order ownership and time-based listing
- indexes for payment/refund/report lookup paths used by current services
- smoke verification that existing behavior is preserved

### Slice D: Product search path improvement

Goal:
Improve scalability of current search behavior without changing the overall architecture.

Possible bounded directions:

- add supporting composite indexes for public listing filters
- normalize query conditions more deliberately
- keep current substring behavior where required, but prepare an explainable staged path toward better search semantics
- if search semantics are tightened, document the exact behavioral tradeoff

### Slice E: Frontend product-list request cleanup

Goal:
Remove the category-bootstrap double request and decouple stable metadata from current-page result data.

Expected outputs:

- product list page performs one intentional initial request, not two
- category source is stable and predictable
- search route behavior remains unchanged from the user's point of view

### Slice F: Configuration safety cleanup

Goal:
Remove insecure defaults from the repo baseline while keeping local development usable.

Expected outputs:

- environment-variable-first configuration
- safer fallback behavior or explicit startup requirement
- docs updated with the new local setup expectation

## Implementation Plan

1. Baseline the current runtime query paths.
   - inventory admin, order, review, report, and search endpoints
   - map each endpoint to current SQL shape, page contract, and expected data volume

2. Prioritize the highest-cost scans.
   - fix user-facing or frequently-used expensive paths before polishing lower-frequency flows
   - review submission scan and admin list full-reads are likely first

3. Introduce paginated mapper methods.
   - add SQL-backed filters, ordering, count queries, and summary queries where needed
   - avoid mixing unrelated refactor into the same slice

4. Tighten schema support.
   - add explicit indexes that match the read patterns already present in services
   - keep schema changes additive and non-destructive

5. Simplify frontend request flow.
   - separate stable category metadata from current result data
   - remove unnecessary bootstrap fetches while preserving route-driven behavior

6. Lock down config defaults.
   - shift secrets and credentials toward environment-driven usage
   - document the expected local setup

7. Verify and document.
   - run focused backend and frontend tests
   - update `.http`, API spec, task status, and changelog entries

## Risks

- Scope creep into broad repository refactor instead of targeted hardening
- API response shape drift during admin pagination rollout
- Under-indexing or over-indexing without checking actual query paths
- Changing search behavior accidentally while trying to improve performance
- Frontend list state regressions if store responsibilities are changed too aggressively
- Schema updates that are safe in H2 tests but behave differently in MySQL
- Hidden client assumptions in admin pages that currently rely on full arrays

## Test Plan

- Backend:
  - add focused service/controller tests for paginated admin endpoints
  - add review submission tests covering direct order-item lookup
  - add regression tests for order, refund, report, and search list behavior after query changes
  - if schema indexes are added, ensure all existing tests still pass under the configured test database

- Frontend:
  - add or update tests for product-list initial load behavior and route-driven filtering
  - verify admin pages consume paginated responses correctly
  - check loading, empty, and error states do not regress

- API validation:
  - update relevant files in `docs/06-http/`
  - update `docs/09-api-spec/` entries for any endpoint contract change, especially admin list pagination

- Manual:
  - open admin list pages with filters and verify page switching
  - submit product and shop reviews through normal user flow
  - exercise product list route filters and search suggestions
  - validate order, refund, and report list/detail flows after index/query changes

## Acceptance Criteria

- [ ] Admin list endpoints no longer depend on full-table read + Java-side filtering for their main list data
- [ ] Product review submission no longer scans all orders to resolve `orderItemId`
- [ ] Order / payment / refund / report runtime query paths have explicit schema support aligned with actual usage
- [ ] Product list page no longer performs an avoidable metadata bootstrap request before the real search request
- [ ] Search/list performance changes preserve explainable behavior and documented API semantics
- [ ] Insecure default credential/secret handling is tightened and documented
- [ ] Tests and documentation remain aligned with the hardened implementation

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [ ] task status and archive move

## Completion Notes

Draft created from a mid-stage architecture and performance review on 2026-05-17.

Initial evidence used to open this task:

- admin service methods rely heavily on `findAll()` + in-memory filtering and summary derivation
- review submission uses a full-order scan placeholder to resolve `orderItemId`
- product search uses multi-column substring matching with limited schema support for future scale
- order / payment / refund / report paths are more mature than their explicit indexing strategy
- product-list frontend flow performs an avoidable extra request to derive category metadata
- backend config still contains permissive local defaults for password and JWT secret

This task should be split into smaller active execution slices once the human decides the desired rollout order.

## Parallel Execution Decomposition

The following active tasks were split out as wave-1 execution slices with non-overlapping primary write scopes:

1. `../active/admin-query-pagination-hardening.md`
   - owns admin controller/service/query modernization
   - does not own schema or storefront files

2. `../active/review-order-lookup-hardening.md`
   - owns review submission lookup-path cleanup
   - does not own schema or frontend files

3. `../active/runtime-index-hardening.md`
   - owns `schema.sql` and runtime index strategy
   - other parallel tasks must not write schema directly

4. `../active/product-list-request-flow-hardening.md`
   - owns storefront product-list request-flow cleanup
   - does not own backend or admin files

### Wave-1 Coordination Rule

If a child task discovers a requirement that crosses into another child task's owned write scope, it must:

1. record the dependency in its notes
2. avoid editing the foreign-owned file
3. hand the issue back through coordination rather than silently broadening scope
