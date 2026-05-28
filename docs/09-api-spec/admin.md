# API Spec: admin

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/youyu/backend/controller/admin/AdminController.java`
  - request DTO: `backend/src/main/java/com/youyu/backend/controller/admin/dto/UpdateUserStatusRequest.java`
  - request DTO: `backend/src/main/java/com/youyu/backend/controller/admin/dto/ReviewVerificationRequest.java`
  - shared response / error handling: `backend/src/main/java/com/youyu/backend/common/api/ApiResponse.java`
  - shared response / error handling: `backend/src/main/java/com/youyu/backend/controller/advice/GlobalExceptionHandler.java`
  - audit mapper: `backend/src/main/java/com/youyu/backend/mapper/audit/AdminAuditLogMapper.java`
  - request sample: `docs/06-http/admin.http`
  - related task: `docs/08-tasks/drafts/api-spec-standardization-follow-up.md`
- Last updated: 2026-05-28

## Scope

This document covers governance and operational endpoints under `/api/admin`:

- dashboard
- users
- student verifications
- products
- review tasks
- shops
- reports
- mediation case escalation and admin mediation APIs
- search governance rules and search logs
- admin audit logs

It does not cover admin login under `/api/admin/auth` or admin order operations under `/api/admin/orders`.

Formal mediation APIs under `/api/admin/reports/{reportId}/escalate-to-mediation`
and `/api/admin/mediation-cases/**` are documented in `docs/09-api-spec/mediation.md`.

`/admin/support` is a frontend admin route, not an admin API namespace. The v1 support console scope reuses existing admin/report/order/search endpoints for context and does not introduce `/api/admin/support/**`; see `docs/02-requirements/admin-support-console-scope.md`.

## Authentication And Roles

- All endpoints in this module require an admin staff role and, for protected actions, a backend `AdminPermission`.
- Legacy `ADMIN` remains a full-access compatibility role and is treated like `SUPER_ADMIN`.
- Supported admin staff roles are `SUPER_ADMIN`, `SUPPORT_AGENT`, `REVIEWER`, `OPERATOR`, and `ORDER_ADMIN`.
- Current project samples use Bearer tokens such as `mock-9001-ADMIN`, `mock-9101-SUPER_ADMIN`, and `mock-9103-REVIEWER`.
- Frontend navigation mirrors these capabilities, but backend permission checks are the source of enforcement.

### Admin Permission Matrix

| Role | Capabilities |
|---|---|
| `ADMIN` / `SUPER_ADMIN` | All admin permissions including audit logs and final mediation decisions |
| `SUPPORT_AGENT` | Dashboard, support context, user/product/shop context, reports, search logs, order read, mediation handling except final decisions |
| `REVIEWER` | Dashboard, student verification, product context, product review tasks, shop context and shop review/status handling |
| `OPERATOR` | Dashboard, product context, search governance, search logs |
| `ORDER_ADMIN` | Dashboard, order read/manage, mediation handling except final decisions |

## Response Envelope

All endpoints in this module use the unified response envelope:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Request succeeded",
  "data": {},
  "traceId": "..."
}
```

## Error Semantics

### HTTP Status And `ResultCode`

| HTTP status | `ResultCode` | Meaning |
|---|---|---|
| `400` | `BAD_REQUEST` | Invalid request parameters or validation failure |
| `401` | `UNAUTHORIZED` | Missing token or invalid token |
| `403` | `FORBIDDEN` | Logged-in user is not an admin |
| `404` | `NOT_FOUND` | Requested admin-side resource does not exist |
| `500` | `INTERNAL_SERVER_ERROR` | Unhandled or server-side failure |

### `BUSINESS_ERROR`

- Current backend behavior returns `200 OK` with `success=false`, `code="BUSINESS_ERROR"`, and a business-facing message.
- Governance flows that reject an invalid state transition may still respond through this envelope rather than a non-2xx status.

## Endpoints

### `GET /api/admin/dashboard`

#### Purpose

Return the admin dashboard observability snapshot for pending work, governance signals,
order status, and mediation status.

#### Request

No query parameters, path parameters, or body.

#### Response

- `data` is the dashboard object returned by `AdminService.dashboard()`.

| Field | Type | Notes |
|---|---|---|
| `data.summary` | object | High-level totals for users, products, shops, reports, orders, and mediation cases |
| `data.queueMetrics` | array | Stable pending-work metrics. Each item has `id`, `label`, `value`, `severity`, `available`, `source`, `description`, and `target.path` |
| `data.governanceSignals` | array | Stable non-primary queue signals with the same metric item shape as `queueMetrics` |
| `data.statusBreakdowns.orders` | array | Real order counts by selected `orders.order_status` values |
| `data.statusBreakdowns.mediation` | array | Real mediation counts by selected `mediation_cases.status` values |
| `data.unavailableMetrics` | array | Metrics intentionally not shown as live data because no reliable source exists yet |
| `data.cards` | array | Legacy dashboard card shape retained for compatibility |
| `data.shortcuts` | array | Admin route shortcuts |
| `data.todo` | object | Legacy pending counts retained for compatibility |

#### Metric Item Shape

| Field | Type | Notes |
|---|---|---|
| `id` | string | Stable machine-readable identifier, e.g. `pending_verifications` |
| `label` | string | Human-readable metric label |
| `value` | number or null | Real count when `available=true`; `null` for unavailable metrics |
| `severity` | string | UI hint such as `danger`, `warning`, `info`, or `muted` |
| `available` | boolean | `true` only when the backend has a trustworthy data source |
| `source` | string | Data source/filter expression used for the metric |
| `description` | string | Short operational meaning |
| `target.path` | string or null | Owning admin route when the metric is actionable |

Current live queue metrics:

| `id` | Data source | Target |
|---|---|---|
| `pending_verifications` | `student_verifications.verification_status = pending_review` | `/admin/verifications` |
| `pending_review_tasks` | `product_review_tasks.review_status = pending_review` | `/admin/review-tasks` |
| `pending_reports` | `reports.status = pending` | `/admin/reports` |
| `pending_shops` | `shops.review_status = pending_review` | `/admin/shops` |
| `pending_order_fulfillment` | `orders.order_status = pending_fulfillment` | `/admin/orders` |
| `refunding_orders` | `orders.order_status = refunding` | `/admin/orders` |
| `active_mediation_cases` | `mediation_cases.status IN (opened, evidence_review, decision_pending)` | `/admin/mediation` |

#### Error Cases

- `401`: missing token or invalid token
- `403`: current user is not an admin

### `GET /api/admin/users`

#### Purpose

List users from the admin view with optional filtering.

#### Request

##### Query

| Field | Required | Type | Notes |
|---|---|---|---|
| `keyword` | no | string | Default empty string |
| `status` | no | string | Default empty string |
| `verificationStatus` | no | string | Default empty string |
| `page` | no | number | Default `1` |
| `pageSize` | no | number | Default `10`, max `100` |

#### Response

- `data` is the paginated user-list result returned by `AdminService.listUsers(...)`.

| Field | Type | Notes |
|---|---|---|
| `data.items` | array | Current page of users |
| `data.total` | number | Total matching records |
| `data.page` | number | Current page number |
| `data.pageSize` | number | Current page size |

#### Error Cases

- `401`: missing token or invalid token
- `403`: current user is not an admin

### `GET /api/admin/users/{userId}`

#### Purpose

Return admin-side detail for a specific user.

#### Request

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `userId` | yes | number | Target user ID |

#### Response

- `data` is the user detail object returned by `AdminService.userDetail(...)`.

| Field | Type | Notes |
|---|---|---|
| `data` | object | Detailed user and governance context |

#### Error Cases

- `401`: missing token or invalid token
- `403`: current user is not an admin
- `404`: user does not exist

### `PUT /api/admin/users/{userId}/status`

#### Purpose

Update admin-managed user availability state.

#### Request

This endpoint uses `UpdateUserStatusRequest` with `@Valid`.

| Field | Required | Type | Notes |
|---|---|---|---|
| `status` | yes | string | One of `active`, `disabled`, `locked` |
| `restrictionReason` | no | string | Max 255 |

#### Response

- `data` is the updated user status result returned by `AdminService.updateUserStatus(...)`.

| Field | Type | Notes |
|---|---|---|
| `data` | object | Updated user governance result |

#### Error Cases

- `400`: validation failure or unsupported status
- `401`: missing token or invalid token
- `403`: current user is not an admin
- `404`: user does not exist

### `GET /api/admin/verifications`

#### Purpose

List student verification records for admin review.

#### Request

##### Query

| Field | Required | Type | Notes |
|---|---|---|---|
| `keyword` | no | string | Default empty string |
| `status` | no | string | Default empty string |
| `page` | no | number | Default `1` |
| `pageSize` | no | number | Default `10`, max `100` |

#### Response

- `data` is the paginated verification-list result returned by `AdminService.listVerifications(...)`.

| Field | Type | Notes |
|---|---|---|
| `data.items` | array | Current page of verifications |
| `data.total` | number | Total matching records |
| `data.page` | number | Current page number |
| `data.pageSize` | number | Current page size |

#### Error Cases

- `401`: missing token or invalid token
- `403`: current user is not an admin

### `PUT /api/admin/verifications/{verificationId}/review`

#### Purpose

Approve or reject a student verification request.

#### Request

This endpoint uses `ReviewVerificationRequest` with `@Valid`.

| Field | Required | Type | Notes |
|---|---|---|---|
| `action` | yes | string | `@NotBlank`, current flow expects values such as `approve` or `reject` |
| `rejectReason` | no | string | Max 255; required by business rules when rejecting |
| `reviewNote` | no | string | Max 255 |

#### Response

- `data` is the updated verification-review result returned by `AdminService.reviewVerification(...)`.

#### Error Cases

- `400`: validation failure, unsupported action, or missing reject reason when rejecting
- `401`: missing token or invalid token
- `403`: current user is not an admin
- `404`: verification or related resource does not exist

### `GET /api/admin/products`

#### Purpose

List products from the admin governance view.

#### Request

##### Query

| Field | Required | Type | Notes |
|---|---|---|---|
| `keyword` | no | string | Default empty string |
| `status` | no | string | Default empty string |
| `reviewStatus` | no | string | Default empty string |
| `productType` | no | string | Default empty string |
| `page` | no | number | Default `1` |
| `pageSize` | no | number | Default `10`, max `100` |

#### Response

- `data` is the paginated product-list result returned by `AdminService.listProducts(...)`.

| Field | Type | Notes |
|---|---|---|
| `data.items` | array | Current page of products |
| `data.total` | number | Total matching records |
| `data.page` | number | Current page number |
| `data.pageSize` | number | Current page size |

#### Error Cases

- `401`: missing token or invalid token
- `403`: current user is not an admin

### `PUT /api/admin/products/{productId}/status`

#### Purpose

Update admin-managed product state.

#### Request

| Field | Required | Type | Notes |
|---|---|---|---|
| `status` | yes | string | One of `draft`, `on_sale`, `off_sale`, `closed` |

#### Response

- `data` is the updated product result returned by `AdminService.updateProductStatus(...)`.

#### Error Cases

- `400`: missing or unsupported status
- `401`: missing token or invalid token
- `403`: current user is not an admin
- `404`: product does not exist

### `GET /api/admin/review-tasks`

#### Purpose

List product review tasks that require admin review.

#### Request

##### Query

| Field | Required | Type | Notes |
|---|---|---|---|
| `keyword` | no | string | Default empty string |
| `status` | no | string | Default empty string |
| `page` | no | number | Default `1` |
| `pageSize` | no | number | Default `10`, max `100` |

#### Response

- `data` is the paginated review-task list returned by `AdminService.listReviewTasks(...)`.

| Field | Type | Notes |
|---|---|---|
| `data.items` | array | Current page of review tasks |
| `data.total` | number | Total matching records |
| `data.page` | number | Current page number |
| `data.pageSize` | number | Current page size |

#### Error Cases

- `401`: missing token or invalid token
- `403`: current user is not an admin

### `PUT /api/admin/review-tasks/{reviewTaskId}/review`

#### Purpose

Approve or reject a product review task.

#### Request

This endpoint currently accepts a map-style payload.

| Field | Required | Type | Notes |
|---|---|---|---|
| `action` | yes | string | Expected values currently include `approve` and `reject` |
| `rejectReason` | no | string | Required by business rules when rejecting |
| `reviewNote` | no | string | Optional reviewer note |

#### Response

- `data` is the updated review-task result returned by `AdminService.reviewTask(...)`.

#### Error Cases

- `400`: unsupported action or missing reject reason when rejecting
- `401`: missing token or invalid token
- `403`: current user is not an admin
- `404`: review task does not exist

### `GET /api/admin/shops`

#### Purpose

List shops from the admin governance view.

#### Request

##### Query

| Field | Required | Type | Notes |
|---|---|---|---|
| `keyword` | no | string | Default empty string |
| `status` | no | string | Default empty string |
| `reviewStatus` | no | string | Default empty string |
| `page` | no | number | Default `1` |
| `pageSize` | no | number | Default `10`, max `100` |

#### Response

- `data` is the paginated shop-list result returned by `AdminService.listShops(...)`.

| Field | Type | Notes |
|---|---|---|
| `data.items` | array | Current page of shops |
| `data.total` | number | Total matching records |
| `data.page` | number | Current page number |
| `data.pageSize` | number | Current page size |

#### Error Cases

- `401`: missing token or invalid token
- `403`: current user is not an admin

### `GET /api/admin/shops/{shopId}`

#### Purpose

Return admin-side detail for a specific shop.

#### Request

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `shopId` | yes | number | Target shop ID |

#### Response

- `data` is the shop detail object returned by `AdminService.shopDetail(...)`.

| Field | Type | Notes |
|---|---|---|
| `data` | object | Detailed shop and governance context |

#### Error Cases

- `401`: missing token or invalid token
- `403`: current user is not an admin
- `404`: shop does not exist

### `PUT /api/admin/shops/{shopId}/status`

#### Purpose

Update admin-managed shop status and review outcome.

#### Request

This endpoint currently accepts a map-style payload.

| Field | Required | Type | Notes |
|---|---|---|---|
| `status` | yes when `reviewStatus` is omitted | string | One of `active`, `inactive`, `disabled`; may be omitted when `reviewStatus` supplies an approval/rejection transition |
| `reviewStatus` | no | string | One of `pending_review`, `approved`, `rejected` |
| `rejectReason` | no | string | Required by business rules in rejection flows |

Valid `status` / `reviewStatus` combinations:

| Flow | Accepted payload |
|---|---|
| Approve shop | `reviewStatus=approved` with omitted status or `status=active` |
| Reject shop | `reviewStatus=rejected` with omitted status or `status=inactive`; `rejectReason` required |
| Keep pending review | `reviewStatus=pending_review` with omitted status or `status=inactive` |
| Disable / enable availability only | omit `reviewStatus`, set `status` to `active`, `inactive`, or `disabled` |

#### Response

- `data` is the updated shop result returned by `AdminService.updateShopStatus(...)`.

#### Error Cases

- `400`: missing or invalid status / reviewStatus combination
- `401`: missing token or invalid token
- `403`: current user is not an admin
- `404`: shop does not exist

### `GET /api/admin/reports`

#### Purpose

List user-submitted reports for governance processing.

#### Request

##### Query

| Field | Required | Type | Notes |
|---|---|---|---|
| `keyword` | no | string | Default empty string |
| `status` | no | string | Default empty string |
| `targetType` | no | string | Default empty string |
| `page` | no | number | Default `1` |
| `pageSize` | no | number | Default `10`, max `100` |

#### Response

- `data` is the paginated report-list result returned by `AdminService.listReports(...)`.

| Field | Type | Notes |
|---|---|---|
| `data.items` | array | Current page of reports |
| `data.total` | number | Total matching records |
| `data.page` | number | Current page number |
| `data.pageSize` | number | Current page size |

#### Error Cases

- `401`: missing token or invalid token
- `403`: current user is not an admin

### `PUT /api/admin/reports/{reportId}/process`

#### Purpose

Process a report and record a governance resolution.

#### Request

This endpoint currently accepts a map-style payload.

| Field | Required | Type | Notes |
|---|---|---|---|
| `status` | yes | string | One of `pending`, `processing`, `resolved`, `rejected` |
| `resolution` | no | string | Resolution note |

#### Response

- `data` is the updated report result returned by `AdminService.processReport(...)`.

#### Error Cases

- `400`: missing or invalid processing status
- `401`: missing token or invalid token
- `403`: current user is not an admin
- `404`: report does not exist

### `GET /api/admin/search/governance-rules`

#### Purpose

List current search governance rules.

#### Request

No query parameters, path parameters, or body.

#### Response

- `data` is a list returned by `AdminService.listSearchGovernanceRules()`.

| Field | Type | Notes |
|---|---|---|
| `data` | array | Governance-rule list |

#### Error Cases

- `401`: missing token or invalid token
- `403`: current user is not an admin

### `POST /api/admin/search/governance-rules`

#### Purpose

Create a search governance rule.

#### Request

This endpoint currently accepts a map-style payload.

| Field | Required | Type | Notes |
|---|---|---|---|
| `ruleType` | yes | string | Example values used in current `.http`: `SENSITIVE_WORD`, `HIDE_KEYWORD` |
| `keyword` | yes | string | Governance keyword target |
| `isActive` | no | boolean | Optional activation flag if supported by service |

#### Response

- `data` is the created rule object returned by `AdminService.createSearchGovernanceRule(...)`.

#### Error Cases

- `400`: invalid rule type or invalid keyword
- `401`: missing token or invalid token
- `403`: current user is not an admin

### `PUT /api/admin/search/governance-rules/{ruleId}`

#### Purpose

Update an existing search governance rule.

#### Request

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `ruleId` | yes | number | Governance rule ID |

##### Body

| Field | Required | Type | Notes |
|---|---|---|---|
| `keyword` | no | string | Optional replacement keyword |
| `isActive` | no | boolean | Optional activation toggle |
| `ruleType` | no | string | Optional depending on service support |

#### Response

- `data` is the updated rule object returned by `AdminService.updateSearchGovernanceRule(...)`.

#### Error Cases

- `400`: invalid field values
- `401`: missing token or invalid token
- `403`: current user is not an admin
- `404`: rule does not exist

### `DELETE /api/admin/search/governance-rules/{ruleId}`

#### Purpose

Delete a search governance rule.

#### Request

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `ruleId` | yes | number | Governance rule ID |

#### Response

- `data.deleted`: boolean

| Field | Type | Notes |
|---|---|---|
| `deleted` | boolean | Current controller returns `true` when deletion succeeds |

#### Error Cases

- `401`: missing token or invalid token
- `403`: current user is not an admin
- `404`: rule does not exist

### `GET /api/admin/search/logs`

#### Purpose

Browse paginated search logs from the admin view.

#### Request

##### Query

| Field | Required | Type | Notes |
|---|---|---|---|
| `page` | no | number | Default `1` |
| `pageSize` | no | number | Default `10` |

#### Response

- `data` is the paginated search-log result returned by `AdminService.listSearchLogs(page, pageSize)`.

| Field | Type | Notes |
|---|---|---|
| `items` | array | Current page of logs |
| `total` | number | Total matched rows |
| `page` | number | Current page number |
| `pageSize` | number | Current page size |

#### Error Cases

- `400`: invalid pagination values
- `401`: missing token or invalid token
- `403`: current user is not an admin

### `GET /api/admin/audit-logs`

#### Purpose

Browse durable admin operation logs for sensitive backend actions.

#### Request

##### Query

| Field | Required | Type | Notes |
|---|---|---|---|
| `action` | no | string | Exact action filter, e.g. `USER_STATUS_UPDATE`; default empty |
| `targetType` | no | string | Exact target type filter, e.g. `USER`; default empty |
| `page` | no | number | Default `1` |
| `pageSize` | no | number | Default `10`, max `100` |

#### Response

- `data` is the paginated audit-log result returned by `AdminService.listAuditLogs(...)`.

| Field | Type | Notes |
|---|---|---|
| `items` | array | Current page of audit records |
| `total` | number | Total matched rows |
| `page` | number | Current page number |
| `pageSize` | number | Current page size |

#### Audit Record Shape

| Field | Type | Notes |
|---|---|---|
| `id` | number | Audit log ID |
| `operatorUserId` | number | Admin user ID from the authenticated request |
| `operatorRole` | string | Current role label, e.g. `ADMIN`, `SUPER_ADMIN`, or specialist staff role |
| `action` | string | Stable action identifier |
| `targetType` | string | Stable target category |
| `targetId` | number | Mutated target ID |
| `summary` | string | Short action summary, capped by backend storage |
| `createdAt` | string | Audit event creation time |

Current v1 audited actions:

| Action | Target type | Source endpoint examples |
|---|---|---|
| `USER_STATUS_UPDATE` | `USER` | `PUT /api/admin/users/{userId}/status` |
| `PRODUCT_STATUS_UPDATE` | `PRODUCT` | `PUT /api/admin/products/{productId}/status` |
| `STUDENT_VERIFICATION_REVIEW` | `STUDENT_VERIFICATION` | `PUT /api/admin/verifications/{verificationId}/review` |
| `PRODUCT_REVIEW_TASK_REVIEW` | `PRODUCT_REVIEW_TASK` | `PUT /api/admin/review-tasks/{reviewTaskId}/review` |
| `SHOP_STATUS_UPDATE` | `SHOP` | `PUT /api/admin/shops/{shopId}/status` |
| `REPORT_PROCESS` | `REPORT` | `PUT /api/admin/reports/{reportId}/process` |
| `SEARCH_GOVERNANCE_RULE_CREATE` | `SEARCH_GOVERNANCE_RULE` | `POST /api/admin/search/governance-rules` |
| `SEARCH_GOVERNANCE_RULE_UPDATE` | `SEARCH_GOVERNANCE_RULE` | `PUT /api/admin/search/governance-rules/{ruleId}` |
| `SEARCH_GOVERNANCE_RULE_DELETE` | `SEARCH_GOVERNANCE_RULE` | `DELETE /api/admin/search/governance-rules/{ruleId}` |

#### Error Cases

- `401`: missing token or invalid token
- `403`: current user is not an admin

## Shared Types / Enumerations

- User `status`: `active`, `disabled`, `locked`
- Verification review `action`: `approve`, `reject`
- Product `status`: `draft`, `on_sale`, `off_sale`, `closed`
- Review-task `action`: `approve`, `reject`
- Shop `status`: `active`, `inactive`, `disabled`
- Shop `reviewStatus`: `pending_review`, `approved`, `rejected`
- Report `status`: `pending`, `processing`, `resolved`, `rejected`
- Search governance `ruleType`: current examples use `SENSITIVE_WORD` and `HIDE_KEYWORD`
- Several admin mutation endpoints still accept map-style payloads rather than dedicated DTOs

## HTTP Asset Mapping

- Primary validation file: `docs/06-http/admin.http`
- Additional related files: `docs/06-http/search.http` for search-governance examples

## Known Drift Or Follow-Up Notes

- No known controller/spec drift is being left unresolved in this module as part of this iteration.
- Admin order operations live in a separate controller and should be documented separately if the project later expands admin formal coverage beyond governance endpoints.
