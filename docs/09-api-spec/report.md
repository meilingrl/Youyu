# API Spec: report

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/youyu/backend/controller/report/ReportController.java`
  - controller: `backend/src/main/java/com/youyu/backend/controller/admin/AdminController.java` (report-related methods)
  - service impl: `backend/src/main/java/com/youyu/backend/service/report/impl/ReportServiceImpl.java`
  - mapper impl: `backend/src/main/java/com/youyu/backend/mapper/report/impl/JdbcReportMapper.java`
  - shared response / error handling: `backend/src/main/java/com/youyu/backend/common/api/ApiResponse.java`
  - shared response / error handling: `backend/src/main/java/com/youyu/backend/controller/advice/GlobalExceptionHandler.java`
  - request sample: `docs/06-http/report.http`
  - related task: `docs/08-tasks/archived/api-spec-report-module-standardization.md`
- Last updated: 2026-05-22

## Scope

This document covers report submission and governance endpoints across two controllers:

- User-side report submission under `/api/reports` (`ReportController`)
- Admin-side report listing and processing under `/api/admin/reports` (`AdminController`)

It does not cover other admin governance endpoints (users, shops, products, search) documented in `admin.md`, mediation escalation/case endpoints documented in `mediation.md`, or any future credit-risk workflows.

## Authentication And Roles

- `ReportController` uses class-level `@LoginRequired` (any authenticated user). The submit endpoint additionally narrows to `@LoginRequired(roles = {UserRole.USER})` to block admin accounts from submitting reports.
- Admin report endpoints in `AdminController` use class-level `@LoginRequired(roles = {UserRole.ADMIN})`.
- Current project samples use Bearer tokens such as `mock-1001-USER` for user endpoints and `mock-9001-ADMIN` for admin endpoints.

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
| `400` | `BAD_REQUEST` | Invalid request parameters, missing required fields, or validation failure |
| `401` | `UNAUTHORIZED` | Missing token, invalid token, or not logged in |
| `403` | `FORBIDDEN` | Authenticated but role is not permitted for this endpoint |
| `404` | `NOT_FOUND` | Referenced resource does not exist |
| `500` | `INTERNAL_SERVER_ERROR` | Unhandled or server-side failure |

### `BUSINESS_ERROR`

- Current backend behavior returns `200 OK` with `success=false`, `code="BUSINESS_ERROR"`, and a business-facing message.
- Callers must not assume every failure is represented by a non-2xx HTTP status.

## Endpoints

### `GET /api/reports/skeleton`

#### Purpose

Return a developer-facing module readiness snapshot for the report domain. Used to verify that the report module is wired and the backend is accepting connections.

#### Request

No query parameters, path parameters, or body.

#### Response

- `data` is the module info object returned by `ReportService.moduleInfo()`.

| Field | Type | Notes |
|---|---|---|
| `module` | string | Always `"report"` |
| `status` | string | Always `"ready"` |
| `next` | string | Human-readable note about planned workflow extensions |

#### Error Cases

- `401`: missing token or invalid token

---

### `POST /api/reports`

#### Purpose

Submit a new report for a target entity (product, shop, user, order, digital product, or digital order). The report is created with status `pending` and awaits admin processing.

#### Request

##### Body

| Field | Required | Type | Notes |
|---|---|---|---|
| `targetType` | yes | string | One of: `product`, `order`, `shop`, `user`, `digital_product`, `digital_order` |
| `targetId` | yes | number | Positive integer ID of the reported entity |
| `reason` | yes | string | Short reason code or label; max 64 characters. Also accepted as `reasonType` |
| `content` | yes | string | Detailed description of the issue; max 1000 characters |
| `targetLabel` | no | string | Human-readable label for the reported entity (for display in admin view); max 255 characters |

**Notes:**
- The service accepts `reason` or `reasonType` as the reason field name; `reasonType` takes precedence if both are provided.
- The authenticated caller's user ID is resolved server-side from the JWT; no `reporterUserId` field is accepted in the request body.

#### Response

- `data` is a single-key object wrapping the created report record.

| Field | Type | Notes |
|---|---|---|
| `report` | object | The newly created report; see **Report Object** in Shared Types |

#### Error Cases

- `400`: missing or blank `targetType`, `targetId`, `reason`, or `content`; `targetType` not in the supported set; `targetId` is not a positive number; `reason` or `content` exceeds length limit; `targetLabel` exceeds 255 characters
- `401`: missing token or invalid token; caller's user ID cannot be resolved from token
- `403`: caller's role is not `USER` (admin accounts cannot submit reports)
- `404`: caller's user account does not exist (resolved by `UserMapper`)
- `500`: report record was not created after insert

**`BUSINESS_ERROR` cases:** invalid `targetType` value and missing required fields return `200 OK` with `success=false` and code `BAD_REQUEST`.

---

### `GET /api/admin/reports`

#### Purpose

List user-submitted reports for admin governance processing with optional filtering and pagination.

#### Request

##### Query

| Field | Required | Type | Notes |
|---|---|---|---|
| `keyword` | no | string | Default empty string; matches against `target_label`, `reporter_name`, and `content` (case-insensitive) |
| `status` | no | string | Default empty string; filter by report status (e.g., `pending`, `resolved`, `dismissed`) |
| `targetType` | no | string | Default empty string; filter by target entity type |
| `page` | no | number | Default `1` |
| `pageSize` | no | number | Default `10`, max `100` |

#### Response

- `data` is the paginated report-list result returned by `AdminService.listReports(...)`.

| Field | Type | Notes |
|---|---|---|
| `data.items` | array | Current page of report objects; each item has the Report Object shape (see Shared Types) |
| `data.total` | number | Total matching records |
| `data.page` | number | Current page number |
| `data.pageSize` | number | Current page size |

#### Error Cases

- `401`: missing token or invalid token
- `403`: current user is not an admin

---

### `PUT /api/admin/reports/{reportId}/process`

#### Purpose

Process a pending report and record a governance resolution. Updates the report status and optionally records a resolution note and the processing admin's identity.

#### Request

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `reportId` | yes | number | ID of the report to process |

##### Body

| Field | Required | Type | Notes |
|---|---|---|---|
| `status` | yes | string | Processing outcome; current flow expects values such as `resolved` or `dismissed` |
| `resolution` | no | string | Optional resolution note visible to internal records |

#### Response

- `data` is the updated report result returned by `AdminService.processReport(...)`.

| Field | Type | Notes |
|---|---|---|
| `data` | object | Updated report record; see **Report Object** in Shared Types |

#### Error Cases

- `400`: missing or blank `status`
- `401`: missing token or invalid token
- `403`: current user is not an admin
- `404`: report with given `reportId` does not exist

## Shared Types / Enumerations

### Report Object

Fields returned by `JdbcReportMapper.normalizeReport(...)`, which normalizes database column names to camelCase:

| Field | Type | Notes |
|---|---|---|
| `id` | number | Report primary key |
| `reporterUserId` | number | ID of the user who submitted the report |
| `reporterName` | string | Display name of the reporter at time of submission |
| `targetType` | string | Type of the reported entity; one of: `product`, `order`, `shop`, `user`, `digital_product`, `digital_order` |
| `targetId` | number | ID of the reported entity |
| `targetLabel` | string | Display label for the reported entity; empty string if not provided |
| `reasonType` | string | Reason code or short label provided by the reporter |
| `content` | string | Detailed description of the issue |
| `status` | string | Processing state: `pending` on creation; updated to `resolved`, `dismissed`, or another admin-defined value on processing |
| `submittedAt` | string | Submission timestamp formatted as `yyyy-MM-dd HH:mm`; empty string if null |
| `processedAt` | string | Processing timestamp formatted as `yyyy-MM-dd HH:mm`; empty string if not yet processed |
| `processedBy` | string | Admin user ID or identifier who processed the report; empty string if not yet processed |
| `resolution` | string | Resolution note recorded by admin; empty string if not yet processed |

### Report `status` Values

| Value | Meaning |
|---|---|
| `pending` | Newly submitted; not yet reviewed by admin |
| `resolved` | Admin reviewed and took action |
| `dismissed` | Admin reviewed and dismissed without action |

Other values may be recorded by admin; the service does not enforce an enum on processing status.

### Supported `targetType` Values

| Value | Meaning |
|---|---|
| `product` | Physical or listing product |
| `order` | Buyer-placed order |
| `shop` | Seller shop |
| `user` | Platform user account |
| `digital_product` | Digital downloadable product |
| `digital_order` | Order for a digital product |

## HTTP Asset Mapping

- Primary validation file: `docs/06-http/report.http`
- Note: `GET /api/reports/skeleton` is not included in `report.http` because it is a developer probe endpoint; all user-facing and admin-facing paths are covered by the `.http` file.

## Known Drift Or Follow-Up Notes

- No controller/spec drift found during the pre-flight audit for this task. The paths, methods, auth roles, and request body fields in `report.http` match current controller behavior.
- The `GET /api/reports/skeleton` endpoint is documented here but has no corresponding `.http` sample. This is intentional; it is a developer probe not intended for frontend integration.
- Admin report processing accepts `status` as a free-form string; the service does not currently validate against a closed enum. A formal enum enforcement is noted in the `platform-mediation-boundary-definition` task.
