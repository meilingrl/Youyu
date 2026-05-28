# Platform Mediation Scope

## 1. Purpose

This document defines the v1 product and contract boundary for platform mediation.

Mediation v1 is a formal admin decision workflow for order-backed disputes. It starts from an eligible user report and creates a durable `mediation_cases` record. It is not a support-ticket system, buyer/seller chat extension, or replacement for existing report/order/refund modules.

Runtime truth reviewed for this boundary:

- `docs/02-requirements/chat-mvp-scope.md`
- `docs/02-requirements/admin-support-console-scope.md`
- `backend/src/main/resources/schema.sql`
- `AdminController`, `ReportController`, `AdminOrderController`, `ChatController`
- `AdminServiceImpl`, `ReportServiceImpl`, `OrderServiceImpl`, `ChatServiceImpl`
- admin report/order/support frontend views
- `docs/06-http/admin.http`
- `docs/09-api-spec/admin.md`, `docs/09-api-spec/report.md`, `docs/09-api-spec/order.md`

## 2. V1 Model

The v1 model is:

```text
user report -> admin escalation -> mediation_cases -> final platform decision
```

Only order-backed reports are eligible for mediation v1:

- `reports.target_type = order`
- `reports.target_type = digital_order`

For v1, `reports.target_id` is the disputed `orders.id`. Product, shop, and user reports remain ordinary governance reports unless a later task defines a safe order-linking contract.

Escalation must be idempotent by `source_report_id`: the same report must not create multiple active or historical mediation cases. If a case already exists for a report, the escalation endpoint returns the existing case.

## 3. Ownership Boundaries

| Area | Owns | Does not own |
|---|---|---|
| `reports` | User accusation intake, reporter identity snapshot, reported target, reason/content, lightweight admin triage status. | Formal dispute state, final mediation decision, refund decision record, evidence review state. |
| Support console | Context gathering and navigation to owner pages. | Mediation cases, support tickets, assignment, SLA, internal notes, chat participation. |
| Chat MVP | Buyer/seller conversations and participant-owned chat state under `/api/chat/**`. | Admin chat browsing, admin message sending, mediation state, report/order/refund mutation. |
| Orders/refunds | Order lifecycle, fulfillment, buyer refund request, refund completion, order/refund state transitions. | Formal platform dispute decision rationale. |
| `mediation_cases` | Formal dispute case state, source report link, related order link, evidence references, final decision category/summary, decision actor/time. | General report queue, support tickets, buyer/seller message persistence, full audit-log platform. |

## 4. Report Escalation Rules

An admin may escalate a report when all conditions are true:

1. The current user has admin role.
2. The source report exists.
3. `targetType` is `order` or `digital_order`.
4. `targetId` resolves to an existing order.
5. No mediation case already exists for that source report, unless the endpoint is returning the existing case idempotently.
6. The report is not already terminal from the mediation perspective.

Recommended report status interaction:

- On escalation, move the report to `processing` if it is still `pending`.
- Store a report resolution note such as `Escalated to mediation case <caseNo>`.
- On final mediation decision, mark the source report `resolved` and record a short resolution summary.
- The final decision details remain in `mediation_cases`; `reports.resolution` is only a pointer/summary.

Non-order reports must continue through the existing report processing flow.

## 5. Mediation Statuses

Mediation v1 uses this closed status set:

| Status | Meaning | Allowed next statuses |
|---|---|---|
| `opened` | Case created from an eligible report; context has not been reviewed. | `evidence_review`, `cancelled` |
| `evidence_review` | Admin is reviewing report, order/refund, and scoped chat context. | `decision_pending`, `cancelled` |
| `decision_pending` | Evidence review is complete and the case is ready for a final decision. | `resolved`, `cancelled` |
| `resolved` | Final platform decision has been recorded. | none |
| `cancelled` | Case was closed before decision because it was invalid, duplicate, or no longer applicable. | none |

Implementation must reject invalid transitions and must not allow `resolved` or `cancelled` cases to be reopened in v1.

## 6. Decision Categories

Mediation v1 uses this closed decision category set when resolving a case:

| Category | Meaning | Expected enforcement boundary |
|---|---|---|
| `refund_full_to_buyer` | Platform decides the buyer should receive a full refund. | Order/refund module performs or records the refund state change. |
| `refund_rejected_release_to_seller` | Platform rejects refund/dispute and releases the order outcome to the seller side. | Order/refund module remains or moves to the non-refund outcome allowed by its state machine. |
| `order_completion_required` | Platform decides the order should be completed or fulfillment evidence is sufficient. | Order module performs only supported order transitions. |
| `platform_governance_action` | Platform decision requires user/product/shop/report governance action rather than direct refund completion. | Existing governance owner modules perform those mutations. |
| `no_action_invalid_or_duplicate` | Case is invalid, duplicate, or lacks sufficient dispute basis. | No order/refund mutation is required. |

Each final decision requires:

- `decisionCategory`
- `decisionSummary`
- `decidedByAdminUserId`
- `decidedAt`

Final decisions are write-once in v1. A second decision request for the same case must be rejected unless a future appeal/reopen contract is created.

## 7. Minimum Data Contract

The implementation task should add a durable `mediation_cases` table. Minimum fields:

| Field | Requirement |
|---|---|
| `id` | Primary key. |
| `case_no` | Stable human-readable case number, unique. |
| `source_report_id` | Required, unique, references `reports.id`. |
| `related_order_id` | Required for v1, references `orders.id`. |
| `buyer_user_id` | Required, copied from related order. |
| `seller_user_id` | Required, copied from related order. |
| `reporter_user_id` | Required, copied from source report. |
| `status` | Required closed enum from this document. |
| `decision_category` | Nullable until resolved; required when `status=resolved`. |
| `decision_summary` | Nullable until resolved; required when `status=resolved`. |
| `enforcement_summary` | Optional short text describing the order/refund/governance action taken or expected. |
| `decided_by_admin_user_id` | Nullable until resolved. |
| `decided_at` | Nullable until resolved. |
| `created_by_admin_user_id` | Admin who escalated the report. |
| `created_at`, `updated_at` | Required timestamps. |

Optional but recommended for implementation quality:

- `cancel_reason` for `cancelled` cases.
- `last_status_changed_at` for list sorting.
- a compact `context_snapshot` JSON/text field only if the implementation needs immutable display snapshots; it must not replace live source records.

Do not create support-ticket, SLA, assignment, or mediation chat-message tables in v1.

## 8. Minimum API Contract

The implementation task should add admin-only endpoints under `/api/admin`.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/admin/reports/{reportId}/escalate-to-mediation` | Create or return the mediation case for an eligible report. |
| `GET` | `/api/admin/mediation-cases` | Paginated case list with `status`, `decisionCategory`, `reportId`, `orderId`, and `keyword` filters. |
| `GET` | `/api/admin/mediation-cases/{caseId}` | Case detail with source report, related order/refund context, participants, and scoped read-only chat context. |
| `PUT` | `/api/admin/mediation-cases/{caseId}/status` | Move a case through non-final status transitions or cancel it. |
| `POST` | `/api/admin/mediation-cases/{caseId}/decision` | Record the final write-once decision and transition to `resolved`. |

Response shape follows the existing `ApiResponse<T>` envelope.

Minimum request fields:

- Escalation: optional `escalationReason`.
- Status update: `status`, optional `cancelReason` when cancelling.
- Decision: `decisionCategory`, `decisionSummary`, optional `enforcementSummary`.

Minimum response fields for list items:

- `id`, `caseNo`, `status`, `decisionCategory`, `sourceReportId`, `relatedOrderId`
- `reporterUserId`, `buyerUserId`, `sellerUserId`
- source report label/reason summary
- order number/status/payment status/refund presence summary
- `createdAt`, `updatedAt`, `decidedAt`

## 9. Read-Only Chat Visibility

Admin chat visibility in mediation v1 is read-only and scoped to the case.

Allowed:

- The mediation case detail may include a `chatContext` block.
- `chatContext` may show buyer/seller messages only when the conversation is directly tied to the related order/report context.
- The safest v1 query is messages with `chat_messages.order_id = mediation_cases.related_order_id`.
- If implementation also includes buyer/seller conversations linked by the disputed order product or shop, it must require both participants to match the order buyer/seller and must cap returned messages.

Forbidden:

- No admin send endpoint.
- No admin participation in buyer/seller conversations.
- No calls from admin UI to user-owned `/api/chat/**` endpoints.
- No mark-read, pin, mute, soft-delete, recall, auto-reply, quick-reply, unread-count, or other chat state mutation.
- No global chat browsing or cross-user conversation search.

If no scoped chat messages exist, the API returns an empty `chatContext.items` array. It must not broaden the search silently.

## 10. Admin Surface Expectations

Minimum v1 admin UI:

- Report management page: show an "escalate to mediation" action only for eligible order-backed reports.
- Mediation case list page: filter by status and decision category; surface source report and order summary.
- Mediation case detail page: show source report, order/refund context, participants, scoped read-only chat context, status transition controls, and final decision form.
- Support console: may link to mediation once implemented, but must remain context/navigation only.
- Order admin detail: may link to a related mediation case, but order/refund mutations stay in the order owner flow or in clearly documented mediation enforcement actions.

Buyer-facing UI is out of scope for v1 except optional read-only status visibility if the implementation task explicitly adds it. Buyers and sellers must not receive mediation action forms in v1.

## 11. Seed Expectations

The implementation task should add minimum seed data for local verification:

- one order-backed report that is eligible for escalation and has no mediation case yet;
- one existing mediation case in `evidence_review` or `decision_pending`;
- one resolved mediation case with a final decision category;
- at least one buyer/seller chat message with `order_id` matching a seeded mediation order, so read-only chat context can be validated.

Seed data must remain small and idempotent.

## 12. Testing Expectations

Backend tests must cover:

- admin-only access for escalation and case endpoints;
- eligible report escalation creates a case and updates report triage state;
- escalation is idempotent by source report;
- non-order reports cannot be escalated in v1;
- missing report/order returns the correct error;
- case list/detail returns report, order/refund, participant, and chat context;
- valid and invalid status transitions;
- final decision is write-once;
- chat context is read-only and does not mutate chat participant state.

Frontend tests or build verification must cover:

- report page eligibility display;
- mediation list/detail rendering;
- final decision form state and terminal-case disabled behavior;
- no admin UI calls to `/api/chat/**`.

Documentation and smoke coverage must update:

- `docs/09-api-spec/admin.md` or a dedicated mediation spec linked from `admin.md`;
- `docs/06-http/admin.http` with escalation, list, detail, status, and decision examples;
- `CHANGELOG.md`.

## 13. Explicit Non-Goals

- No support-ticket system.
- No assignment, SLA, workload balancing, or internal support notes.
- No three-party chat.
- No admin message sending.
- No automated decision logic.
- No appeals, reopen flow, or decision overwrite.
- No global chat moderation queue.
- No role-permission model beyond current admin-only protection in this slice.
