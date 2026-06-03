# Task: Feature Polish Closeout Orchestration

## Metadata

- ID: feature-polish-closeout-parent
- Status: active
- Owner: main-agent
- Track: cross-cutting
- Depends on: clean review of current uncommitted docs/frontend work before implementation
- Priority: P0
- Planned date: 2026-06-03
- Completed date:

## Objective

Coordinate the final feature-polish work for messages, explore, trade, admin users, admin dashboard, and admin export without letting one agent make a risky broad implementation pass.

This task is owned by the main Agent. It defines task boundaries, worker dispatch order, review gates, and final acceptance. It does not authorize this Agent to implement product code directly.

## Background

The requested closeout items are:

- Messages page: chat conversations stretch the page; the chat area needs a fixed, usable height with internal scrolling or an equivalent stable layout.
- Explore page: category selection is not effective, fuzzy search is missing, the search bar is shaking again, and sorting must support price, sales, and newest time.
- Trade page: support review image upload, cart select-all / invert-selection, usable refund flow, and logistics / map connection.
- Admin users: support administrator permission assignment.
- Admin dashboard: show core metrics and charts for users, orders, sales, today's orders/sales, sales trend, hot products, and order status.
- Admin export: provide data export capability.

Current repository evidence:

- `frontend/src/views/app/MessagesView.vue` and `frontend/src/components/chat/*` own the user chat surface.
- `frontend/src/views/app/ProductListView.vue`, `frontend/src/components/explore/ExploreSearchShell.vue`, `frontend/src/stores/market.js`, `frontend/src/stores/search.js`, and `frontend/src/api/modules/product.js` own the explore surface.
- `backend/src/main/java/com/youyu/backend/controller/product/ProductController.java` currently accepts product list filters for `keyword`, `categoryId`, `productType`, `page`, and `pageSize`; explicit sort parameters need contract work.
- `frontend/src/views/app/CartView.vue`, `frontend/src/views/app/TradeView.vue`, `frontend/src/views/app/PendingReviewsView.vue`, `frontend/src/components/common/ReviewForm.vue`, `frontend/src/api/modules/order.js`, and `frontend/src/api/modules/review.js` own the trade/review buyer surface.
- `backend/src/main/java/com/youyu/backend/controller/order/OrderController.java` exposes `POST /api/orders/{orderId}/refunds`; the implementation must be verified before changing UI assumptions.
- `backend/src/main/java/com/youyu/backend/common/auth/AdminPermission.java` and `AdminPermissionPolicy.java` already define a static permission model; admin assignment needs a deliberate contract and audit boundary.
- `frontend/src/views/admin/DashboardView.vue` and `frontend/src/api/modules/admin.js` already call `/api/admin/dashboard`.

## Scope

- Keep this as a governed multi-agent delivery program.
- Prefer simple, low-risk frontend-only fixes first.
- Move cross-module API and schema work into later waves.
- Require main-agent diff review before archiving any child task.
- Require API spec and `.http` updates for every new or changed endpoint.
- Require changelog and task archival only after verified integration.

## Out of Scope

- Replacing the current Vue/Spring/JDBC architecture.
- Introducing a new UI library.
- Reintroducing `AdminDataStore`.
- Building a real third-party logistics provider or production map provider unless the task explicitly adds a documented adapter boundary and fallback.
- Silent schema rewrites, destructive migrations, or `ALTER TABLE` / `DROP TABLE` changes in `schema.sql` outside a task-approved additive plan.
- Treating placeholder/demo UI as production-complete without stating the limitation.

## Child Tasks

- [ ] `feature-polish-messages-chat-window-layout`
- [ ] `feature-polish-explore-filter-search-sort`
- [ ] `feature-polish-cart-selection-review-images`
- [ ] `feature-polish-refund-logistics-map-reconciliation`
- [ ] `feature-polish-admin-permission-assignment`
- [ ] `feature-polish-admin-dashboard-metrics`
- [ ] `feature-polish-admin-data-export`
- [ ] `feature-polish-integration-verification`

## Wave Plan

| Wave | Task | Rationale |
| --- | --- | --- |
| 1 | messages chat window layout | Simple frontend layout fix with low conflict. |
| 1 | explore filter/search/sort | User-visible issue, touches existing explore files; avoid parallel edits to `ProductListView.vue` with other frontend workers. |
| 1 | cart selection + review images | Cart selection can be local UI/store work; review images may need API contract but can start with existing upload patterns. |
| 2 | refund + logistics/map reconciliation | Must verify current backend behavior and decide adapter/fallback boundary before UI claims completion. |
| 2 | admin permission assignment | Security-sensitive; must use existing static permission model and audit conventions. |
| 2 | admin dashboard metrics/charts | Backend aggregation plus frontend display; can run after admin contract review. |
| 3 | admin data export | Cross-cutting and potentially sensitive; should follow permission/dashboard/export policy decisions. |
| final | integration verification | Main Agent reviews all diffs, runs checks, updates docs, changelog, and archives tasks. |

## Locked Interfaces

- Keep API response envelope as `ApiResponse<T>`.
- Keep frontend API wrappers under `frontend/src/api/modules/`.
- Normalize backend snake_case / mixed responses at store or API boundary, not deep inside page templates.
- Use Element Plus and existing common components for UI states.
- Every backend endpoint requiring authentication must use `@LoginRequired`; admin mutations must use `AdminPermission` where available.
- Preserve mock token support for dev/test only.
- Product list sort keys must be documented before implementation. Proposed keys: `price_asc`, `price_desc`, `sales_desc`, `newest`.
- Any map/logistics work must expose a graceful fallback when no provider key is configured.
- Export endpoints must require admin permission and must not export plaintext credentials, tokens, verification codes, or excessive PII.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/04-standards/development-process.md`
- `docs/05-roadmap/current/stage-roadmap.md`
- `docs/05-roadmap/current/launch-preparation-roadmap.md`
- `frontend/README.md`
- `backend/README.md`
- each child task before dispatch

## Allowed Changes

- `docs/08-tasks/active/feature-polish-*.md`
- Child-task-owned frontend/backend/API/test files only after that child task is dispatched.
- `docs/06-http/*.http`, `docs/09-api-spec/*.md`, and `CHANGELOG.md` during integration when contracts or substantive behavior change.

## Implementation Plan

1. Main Agent checks current branch, uncommitted changes, and active task list before dispatch.
2. Main Agent creates or uses a topic branch/worktree for implementation because the current tree already contains unrelated local changes.
3. Dispatch Wave 1 workers first; do not let multiple workers edit the same view/component at the same time.
4. Review Wave 1 diffs, run frontend checks, and only then dispatch Wave 2.
5. For Wave 2/3, require backend contract review before frontend claims the feature works.
6. Main Agent integrates all accepted work, resolves overlaps, updates docs/API specs/HTTP samples/changelog, and archives tasks.

## Risks

- Current working tree is dirty; implementation should not start until the main Agent separates or preserves existing changes.
- Explore work may overlap with existing uncommitted `ProductListView.vue` edits.
- Refund and admin-permission work can alter business/security behavior and needs backend tests.
- Logistics/map and export can easily become fake UI if provider and security boundaries are not locked.
- Dashboard metrics can be misleading if computed from mock-only or seed-only data.

## Test Plan

- Backend: `cd backend; .\mvnw.cmd test`
- Frontend: `cd frontend; npm test`
- Frontend build: `cd frontend; npm run build`
- E2E smoke when backend/frontend are running: `cd frontend; npm run test:e2e`
- API validation: update and run relevant `.http` requests manually or through the repo's accepted HTTP workflow.
- Manual: verify messages, explore, cart/review/refund, admin users, dashboard, and export paths with appropriate user/admin roles.

## Acceptance Criteria

- [ ] Each requested user issue maps to a child task with explicit owner and acceptance criteria.
- [ ] Simple Wave 1 tasks complete before difficult Wave 2/3 tasks are dispatched.
- [ ] No worker archives its own task without main-agent review.
- [ ] New or changed endpoint contracts are reflected in `docs/09-api-spec/` and `docs/06-http/`.
- [ ] Final integration verifies backend tests, frontend tests, frontend build, and relevant manual/browser flows.
- [ ] Deferred provider-backed capabilities remain clearly labeled instead of presented as production-complete.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] relevant files in `docs/09-api-spec/`
- [ ] task status and archive move

## Completion Notes

