# API Spec: mediation

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/youyu/backend/controller/mediation/AdminMediationController.java`
  - service: `backend/src/main/java/com/youyu/backend/service/mediation/impl/MediationServiceImpl.java`
  - mapper: `backend/src/main/java/com/youyu/backend/mapper/mediation/impl/JdbcMediationMapper.java`
  - requirements: `docs/02-requirements/platform-mediation-scope.md`
  - smoke requests: `docs/06-http/admin.http`
- Last updated: 2026-05-28

## Scope

Mediation v1 is an admin-only formal dispute workflow:

```text
eligible order-backed report -> mediation_cases -> final write-once platform decision
```

It does not add ticketing, assignment, SLA, appeals, admin chat participation, or global chat browsing.

## Authentication

All endpoints require admin role access through the existing `@LoginRequired(roles = {UserRole.ADMIN})` protection.

## Data Contract

`mediation_cases` stores the durable case state:

| Field | Notes |
|---|---|
| `caseNo` | Unique human-readable case number |
| `sourceReportId` | Required and unique; escalation is idempotent by this field |
| `relatedOrderId` | Required for v1 |
| `buyerUserId`, `sellerUserId`, `reporterUserId` | Copied from related order and source report |
| `status` | One of `opened`, `evidence_review`, `decision_pending`, `resolved`, `cancelled` |
| `decisionCategory` | Nullable until final decision |
| `decisionSummary`, `enforcementSummary` | Final decision text |
| `decidedByAdminUserId`, `decidedAt` | Final decision actor/time |

Decision categories:

- `refund_full_to_buyer`
- `refund_rejected_release_to_seller`
- `order_completion_required`
- `platform_governance_action`
- `no_action_invalid_or_duplicate`

## Endpoints

### `POST /api/admin/reports/{reportId}/escalate-to-mediation`

Creates or returns the mediation case for an eligible order-backed report.

Request body:

| Field | Required | Type |
|---|---|---|
| `escalationReason` | no | string |

Behavior:

- accepts only `reports.target_type` of `order` or `digital_order`;
- validates `reports.target_id` resolves to an order;
- returns an existing case when `source_report_id` was already escalated;
- moves a pending source report to `processing`.

### `GET /api/admin/mediation-cases`

Returns paginated case list.

Query fields:

| Field | Required | Notes |
|---|---|---|
| `status` | no | Closed mediation status set |
| `decisionCategory` | no | Closed decision category set |
| `reportId` | no | Source report ID |
| `orderId` | no | Related order ID |
| `keyword` | no | Case/report/order keyword |
| `page`, `pageSize` | no | Defaults `1`, `10`; max page size `100` |

### `GET /api/admin/mediation-cases/{caseId}`

Returns detail with:

- `case`
- `sourceReport`
- `order`
- `orderItems`
- `refunds`
- `participants`
- `chatContext`

`chatContext.items` is read-only and scoped to `chat_messages.order_id = relatedOrderId`. The mediation UI must not call `/api/chat/**`.

### `PUT /api/admin/mediation-cases/{caseId}/status`

Updates non-final status or cancels a case.

Request body:

| Field | Required | Notes |
|---|---|---|
| `status` | yes | `evidence_review`, `decision_pending`, or `cancelled` according to the current status |
| `cancelReason` | no | Used when cancelling |

Invalid transitions and terminal reopen attempts return `BAD_REQUEST`.

### `POST /api/admin/mediation-cases/{caseId}/decision`

Records the final write-once platform decision and transitions the case to `resolved`.

Request body:

| Field | Required | Notes |
|---|---|---|
| `decisionCategory` | yes | Closed decision category set |
| `decisionSummary` | yes | Non-blank |
| `enforcementSummary` | no | Short owner-module enforcement note |

Behavior:

- requires case status `decision_pending`;
- rejects terminal cases;
- rejects a second decision;
- marks the source report `resolved` with a summary pointer.
