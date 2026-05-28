# Admin Support Console Scope

## 1. Purpose

This document defines the v1 boundary for `/admin/support`. The support console is an admin context surface that helps operators find existing governance records faster. It is not a new support-ticket system, mediation system, chat system, or persistence owner.

Runtime truth for this scope is the current backend controllers and API specs:

- `AdminController` owns dashboard, users, products, shops, reports, and search-governance admin endpoints.
- `AdminOrderController` owns admin order, fulfillment, and refund operations.
- `ReportController` owns user report submission.
- `ChatController` owns authenticated user-to-user chat endpoints.
- `NotificationController` owns authenticated user notification endpoints.
- `/admin/support` currently exists as a reserved frontend route and placeholder view only.

## 2. V1 Boundary

Support console v1 is limited to a support context dashboard:

- show static lane definitions and live summaries from existing admin-owned lists where available
- link operators to the existing admin reports, orders, users, shops, products, review tasks, and search governance pages
- expose no new mutation behavior
- create no support-ticket, mediation-case, group-chat, or admin-chat records
- avoid calling user-only chat or notification endpoints from an admin page

The first implementation slice is ready only because it can be built from existing admin APIs plus frontend routing. Any lane that requires new backend ownership remains explicitly missing.

## 3. Lane Ownership Matrix

| Lane | V1 status | Owner module | Existing reusable endpoints | Missing endpoints / gaps | Boundary |
|---|---|---|---|---|---|
| Support context dashboard | implementation-ready | frontend admin support view; reads admin/order/report/search APIs | `GET /api/admin/dashboard`, `GET /api/admin/reports`, `GET /api/admin/orders`, `GET /api/admin/users`, `GET /api/admin/shops`, `GET /api/admin/products`, `GET /api/admin/review-tasks`, `GET /api/admin/search/logs` | no `/api/admin/support/summary`; no dedicated SLA/assignment/status model | May summarize and deep-link existing records only. Must not create a new support data owner. |
| Report triage | reusable existing lane | report module plus `AdminController` report methods | `POST /api/reports`, `GET /api/admin/reports`, `PUT /api/admin/reports/{reportId}/process` | no report-to-mediation escalation endpoint; report status is not a support-ticket status | Reports remain accusation/governance records. Support console may link to `/admin/reports`, not duplicate report processing. |
| Order and refund assistance | reusable existing lane | order module plus `AdminOrderController` | `GET /api/admin/orders`, `GET /api/admin/orders/{orderId}`, `POST /api/admin/orders/{orderId}/ship`, `POST /api/admin/orders/{orderId}/offline/seller-confirm`, `POST /api/admin/orders/{orderId}/refunds/{refundId}/complete` | runtime controller exposes no admin order pagination or filtering; no assistance-note endpoint; no support case assignment | Support console may surface order/refund context and link to `/admin/orders`. Operational state changes stay in order admin views. |
| User/shop/product governance context | reusable existing lane | admin governance module | `GET /api/admin/users`, `GET /api/admin/users/{userId}`, `PUT /api/admin/users/{userId}/status`, `GET /api/admin/shops`, `GET /api/admin/shops/{shopId}`, `PUT /api/admin/shops/{shopId}/status`, `GET /api/admin/products`, `PUT /api/admin/products/{productId}/status`, `GET /api/admin/review-tasks`, `PUT /api/admin/review-tasks/{reviewTaskId}/review` | no support-specific case history by entity | Support console may link to current governance pages. It must not add parallel moderation actions. |
| Search/risk signal context | partial existing lane | search governance module via `AdminController` | `GET /api/admin/search/governance-rules`, `GET /api/admin/search/logs`, plus rule CRUD endpoints | no abnormal-message detection; no chat moderation hits; no risk queue | V1 may show search log/governance links only. It must not claim message-risk processing exists. |
| Buyer/seller chat visibility | missing for admin support | chat module | User endpoints exist under `/api/chat/**` for conversation list, messages, search, pin, mute, recall, auto-reply, quick replies, unread count | no admin role access; no cross-user conversation lookup; no admin participant model; no three-party support conversation | Admin support v1 must not call `/api/chat/**`. Chat remains peer-to-peer USER scope until a separate admin chat contract exists. |
| Notifications | not a support-console data source in v1 | notification module | User endpoints exist under `/api/notifications/**` for list, unread count, mark-read | no admin notification queue; no support notification composition; USER role only | V1 may mention notifications as user delivery infrastructure only. It must not display or mutate notifications. |
| Platform mediation | contract-defined, implementation pending | mediation module after implementation | none currently; planned contract is `docs/02-requirements/platform-mediation-scope.md` | mediation endpoints and `mediation_cases` do not exist until implementation | Mediation is excluded from support console v1 ownership. Support console may link to mediation after implementation, but must not own mediation UI state, endpoints, or schema. |
| Group governance | missing | no current owner | none | no group entity, group membership, group message, or group moderation endpoints | Excluded from v1. Keep it as a reserved lane only. |

## 4. Reusable Endpoint Details

### Admin dashboard

- Method/path: `GET /api/admin/dashboard`
- Auth: admin role
- Response dependency: aggregated admin overview snapshot
- Use in v1: top-level support context metrics if the current response already contains useful counts
- Limitation: no support-specific metric contract

### Reports

- Method/path: `GET /api/admin/reports`
- Auth: admin role
- Query: `keyword`, `status`, `targetType`, `page`, `pageSize`
- Response dependency: paginated report list
- Use in v1: pending report queue preview and link to `/admin/reports`
- Limitation: processing stays in the report owner page

- Method/path: `PUT /api/admin/reports/{reportId}/process`
- Auth: admin role
- Use in v1: not called from support dashboard
- Limitation: mutation remains in report processing view

### Admin orders and refunds

- Method/path: `GET /api/admin/orders`
- Auth: admin role
- Response dependency: list of admin order records
- Use in v1: order/refund context preview and link to `/admin/orders`
- Limitation: current controller does not accept status, page, or pageSize query parameters even if HTTP samples include a status-filter example.

- Method/path: `GET /api/admin/orders/{orderId}`
- Auth: admin role
- Use in v1: deep-link target only unless the future page adds a detail drawer
- Limitation: no support-note or assistance-state field

- Method/path: `POST /api/admin/orders/{orderId}/refunds/{refundId}/complete`
- Auth: admin role
- Use in v1: not called from support dashboard
- Limitation: refund completion remains an order operation

### Governance context

- Method/path: `GET /api/admin/users`, `GET /api/admin/shops`, `GET /api/admin/products`, `GET /api/admin/review-tasks`
- Auth: admin role
- Use in v1: context cards and deep links only
- Limitation: support console must not duplicate status mutations from owner pages

### Search governance context

- Method/path: `GET /api/admin/search/logs`
- Auth: admin role
- Use in v1: risk-signal context link or small recent-search-log preview
- Limitation: search logs are not abnormal-message detections

## 5. Missing Endpoint Inventory

The following endpoints do not exist and must not be assumed by v1:

- `GET /api/admin/support/summary`
- `GET /api/admin/support/queues`
- `GET /api/admin/support/conversations`
- `GET /api/admin/support/conversations/{id}/messages`
- `POST /api/admin/support/conversations/{id}/messages`
- `POST /api/admin/support/cases`
- `POST /api/admin/reports/{reportId}/escalate-to-mediation`
- `GET /api/admin/mediation-cases`
- `POST /api/admin/mediation-cases/{id}/decision`
- any group governance endpoint
- any abnormal chat-message detection endpoint
- any support assignment, SLA, disposition, or internal-note endpoint

Adding any of these requires a separate implementation task with backend contract, tests, API spec updates, and HTTP smoke coverage.

## 6. Non-Goals

- No support-ticket persistence.
- No `AdminDataStore` or persistent in-memory business store.
- No platform mediation implementation.
- No buyer-facing mediation UI.
- No admin participation in buyer/seller chat.
- No group-chat or group-governance implementation.
- No abnormal-message detection or automated risk scoring.
- No new order, report, refund, chat, notification, schema, or seed behavior.

## 7. Implementation-Ready Slice

The only implementation-ready v1 slice is:

**Admin support context dashboard**

It may replace the reserved `/admin/support` placeholder with a live dashboard that:

- fetches existing admin report, order, dashboard, governance, and search-log data through existing frontend API modules
- renders lane cards that clearly label their owner module and state
- links each lane to the existing owner page for action
- shows unavailable lanes as blocked or missing without fake counts
- keeps all mutations on the owner pages

This slice is implementation-ready because it needs no backend, schema, seed, API contract, or product-decision change. It remains separate from mediation: `docs/02-requirements/platform-mediation-scope.md` now defines mediation ownership, and support console must not become that owner.

## 8. Future Scope Triggers

Create a separate task before expanding support console beyond v1 if any of the following becomes required:

- admin-visible chat transcripts or admin message sending
- support assignment, SLA, internal notes, or disposition workflow
- support-console ownership of mediation escalation from reports or orders
- group governance
- abnormal-message detection
- notification composition or admin notification queues
- a dedicated `/api/admin/support/**` backend module
