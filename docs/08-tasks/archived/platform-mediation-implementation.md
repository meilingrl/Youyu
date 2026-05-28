# Task: Platform Mediation Implementation

## Metadata

- ID: platform-mediation-implementation
- Status: completed
- Owner: Codex head Agent / worker
- Track: cross-cutting
- Depends on: `docs/02-requirements/platform-mediation-scope.md`, accepted archived `platform-mediation-boundary-and-contract` task
- Priority: high
- Planned date: 2026-05-28
- Completed date: 2026-05-28

## Objective

Implement the v1 platform mediation workflow exactly as defined in `docs/02-requirements/platform-mediation-scope.md`.

The implementation must turn eligible order-backed reports into formal `mediation_cases`, provide admin list/detail/action surfaces, expose scoped read-only chat context, and record final write-once platform decisions.

## Background

The accepted product model is:

```text
user report -> admin escalation -> mediation_cases -> final platform decision
```

Support console is context/navigation only. Reports remain accusation/governance intake. Orders/refunds remain operational state owners. Chat remains buyer/seller participant messaging. Mediation owns the formal dispute case and decision record.

## Scope

- Add `mediation_cases` persistence and data access.
- Add admin-only report escalation from eligible order-backed reports:
  - `reports.target_type = order`
  - `reports.target_type = digital_order`
- Add admin mediation case list/detail/status/decision APIs.
- Add admin UI for report escalation, mediation list, mediation detail, status transition, and final decision recording.
- Include source report, related order/refund, participant, and scoped read-only chat context in case detail.
- Add seed data for an eligible report, an in-progress case, a resolved case, and at least one matching order-card chat message.
- Update API spec and HTTP smoke coverage.
- Add backend tests and frontend verification appropriate to the changed surface.

## Out of Scope

- Three-party chat or admin message sending.
- Admin calls to user-owned `/api/chat/**`.
- Support tickets, assignment, SLA, workload balancing, or internal support notes.
- Automated decision logic.
- Appeals, reopen flow, or final decision overwrite.
- Global chat browsing or chat moderation queues.
- Buyer/seller mediation action forms.
- Role-permission implementation beyond current admin-only protection.
- Reintroducing `AdminDataStore`.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/05-roadmap/current/admin-module-goal-roadmap.md`
- `docs/02-requirements/chat-mvp-scope.md`
- `docs/02-requirements/platform-mediation-scope.md`
- `docs/02-requirements/admin-support-console-scope.md`
- `docs/08-tasks/archived/platform-mediation-boundary-and-contract.md`
- `backend/src/main/resources/schema.sql`
- report/admin/order/refund/chat backend controller, service, mapper files
- admin report/order/support frontend views and admin route/navigation files
- `docs/06-http/admin.http`
- `docs/09-api-spec/admin.md`
- `docs/09-api-spec/report.md`
- `docs/09-api-spec/order.md`

## Allowed Changes

- Backend mediation module files:
  - controller/service/mapper/entity or DTO files needed for mediation.
  - report escalation integration in admin/report service/controller.
  - order/refund read context integration as needed.
  - chat read-context mapper/service methods only when scoped under mediation/admin behavior.
- Frontend admin mediation files:
  - admin routes/navigation.
  - report page escalation action.
  - mediation list/detail views and API module functions.
  - support/order links only where needed for navigation context.
- Database schema and seed data needed for `mediation_cases`.
- Backend and frontend tests.
- `docs/06-http/admin.http`.
- `docs/09-api-spec/admin.md` or a dedicated mediation API spec linked from `admin.md`.
- `CHANGELOG.md`.
- This task document lifecycle updates.

## Not Allowed

- Do not add `/api/admin/support/**`.
- Do not add admin send/chat participation endpoints.
- Do not mutate chat read, unread, pin, mute, delete, recall, auto-reply, or quick-reply state from mediation.
- Do not create support-ticket, assignment, SLA, or internal-note tables.
- Do not add buyer/seller action forms for mediation v1.
- Do not broaden product/shop/user reports into mediation unless `platform-mediation-scope.md` is updated and accepted first.
- Do not edit archived tasks except to read them as history.

## Required Data Contract

Add a durable `mediation_cases` table with at least:

- `id`
- `case_no` unique
- `source_report_id` required and unique
- `related_order_id` required for v1
- `buyer_user_id`
- `seller_user_id`
- `reporter_user_id`
- `status`
- `decision_category`
- `decision_summary`
- `enforcement_summary`
- `decided_by_admin_user_id`
- `decided_at`
- `created_by_admin_user_id`
- `created_at`
- `updated_at`

Use the closed status set from the scope document:

- `opened`
- `evidence_review`
- `decision_pending`
- `resolved`
- `cancelled`

Use the closed decision category set from the scope document:

- `refund_full_to_buyer`
- `refund_rejected_release_to_seller`
- `order_completion_required`
- `platform_governance_action`
- `no_action_invalid_or_duplicate`

## Required API Contract

Add admin-only endpoints:

| Method | Path | Required behavior |
|---|---|---|
| `POST` | `/api/admin/reports/{reportId}/escalate-to-mediation` | Create or return the case for an eligible order-backed report. |
| `GET` | `/api/admin/mediation-cases` | Paginated list with `status`, `decisionCategory`, `reportId`, `orderId`, and `keyword` filters. |
| `GET` | `/api/admin/mediation-cases/{caseId}` | Detail with case, source report, order/refund context, participants, and scoped read-only chat context. |
| `PUT` | `/api/admin/mediation-cases/{caseId}/status` | Enforce allowed non-final status transitions or cancellation. |
| `POST` | `/api/admin/mediation-cases/{caseId}/decision` | Record a final write-once decision and transition to `resolved`. |

Response envelope must remain `ApiResponse<T>`.

Escalation requirements:

- Reject non-order-backed reports.
- Validate that the related order exists.
- Idempotently return the existing case when one already exists for `source_report_id`.
- Move source report to `processing` when escalating from `pending`.

Decision requirements:

- Require `decisionCategory` and `decisionSummary`.
- Reject unsupported decision categories.
- Reject decisions for terminal cases.
- Reject a second decision for the same case.
- Mark source report `resolved` with a short resolution summary after final decision.

## Read-Only Chat Context Rules

Case detail may include a `chatContext` block only through mediation/admin code.

Minimum safe behavior:

- Return messages where `chat_messages.order_id = mediation_cases.related_order_id`.
- Include conversation and sender summaries needed for display.
- Cap returned chat messages.
- Return an empty list when no scoped messages exist.

Forbidden behavior:

- no admin message send;
- no admin participant insertion;
- no `/api/chat/**` calls from admin mediation UI;
- no mark-read, unread, pin, mute, soft-delete, recall, auto-reply, or quick-reply mutation;
- no global cross-user chat search.

## Admin UI Requirements

- Add a mediation case route, preferably `/admin/mediation`.
- Add route/navigation labels consistent with the existing admin workbench.
- In `ReportManageView`, show escalation only for eligible `order` and `digital_order` reports.
- Mediation list must show case number, status, decision category, source report summary, order summary, participants, and timestamps.
- Mediation detail must show:
  - case metadata and status;
  - source report;
  - related order/refund context;
  - buyer/seller/reporter summary;
  - read-only chat context;
  - allowed status actions;
  - final decision form disabled for terminal cases.
- Support console may link to mediation after implementation, but it must not own mediation state or mutations.

## Seed Requirements

Add small idempotent seed coverage:

- one eligible order-backed report with no case yet;
- one mediation case in `evidence_review` or `decision_pending`;
- one resolved mediation case with a final decision category;
- one buyer/seller chat message with `order_id` matching a seeded mediation order.

## Verification Plan

- Backend focused tests for mediation escalation, list/detail, status transitions, final decision, authorization, idempotency, and chat read-only context.
- Backend full test suite: `backend\\mvnw.cmd test`.
- Frontend unit tests if matching admin view/API patterns exist.
- Frontend build: `frontend\\npm run build`.
- API smoke: update and manually run or review the new `docs/06-http/admin.http` mediation requests.
- Static diff hygiene: `git diff --check`.

## Acceptance Criteria

- [x] The task starts only after `docs/02-requirements/platform-mediation-scope.md` exists and the boundary task is archived as accepted.
- [x] Eligible order-backed reports can be escalated into `mediation_cases`.
- [x] Escalation is idempotent by `source_report_id`.
- [x] Non-order reports cannot be escalated in v1.
- [x] Mediation cases have admin list/detail/status/decision APIs.
- [x] Mediation cases have admin list/detail UI.
- [x] Final decisions are write-once and cannot be overwritten accidentally.
- [x] Source report triage state is updated on escalation and final decision without making reports the decision store.
- [x] Related order/refund context is shown without moving order/refund ownership into mediation.
- [x] Related chat context is read-only, scoped, and does not call `/api/chat/**` from admin UI.
- [x] New/changed API contracts are documented.
- [x] HTTP smoke requests cover escalation, list, detail, status update, and decision.
- [x] Seed data supports a local mediation scenario.
- [x] Backend tests pass.
- [x] Frontend tests/build pass as applicable.
- [x] `CHANGELOG.md` is updated.
- [x] Completion notes are filled before archive.

## Sub-agent Instructions

You are not alone in the codebase. Do not revert edits made by others. Treat `docs/02-requirements/platform-mediation-scope.md` as binding.

Implement only mediation v1. Do not invent ticketing, assignment, SLA, admin chat participation, global chat browsing, appeals, or broader report eligibility. If the implementation needs a product decision not present in the scope document, stop and report the blocker instead of widening the task.

## Feedback To Head Agent

Return:

- changed files;
- implemented mediation workflow summary;
- API and data contract changes;
- seed data added for mediation;
- verification commands and results;
- remaining risks or blocked acceptance criteria.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [x] `docs/09-api-spec/`
- [x] task status and archive move

## Completion Notes

- Accepted on 2026-05-28 after worker implementation and head Agent review.
- Added `mediation_cases` schema, backend mediation controller/service/mapper/entity files, and admin-only endpoints for report escalation, case list/detail, status transitions, and final write-once decisions.
- Escalation is limited to `order` and `digital_order` reports, validates the related order, is idempotent by `source_report_id`, and moves pending reports to `processing`.
- Final decisions require `decisionCategory` and `decisionSummary`, require `decision_pending`, reject terminal cases, and mark the source report `resolved` without making reports the decision store.
- Case detail includes source report, order/refund context, participants, and mediation-scoped read-only chat messages selected by `chat_messages.order_id = related_order_id`.
- Added `/admin/mediation` and `/admin/mediation/:id` admin UI, report-page escalation action for eligible reports, admin navigation, API wrappers, and route guard coverage.
- Added seed coverage for one eligible report without a case, one active case, one resolved case, and one order-card chat message. Head Agent review added cleanup for seeded chat messages before reseeding orders so the seed profile remains rerunnable under foreign keys.
- Added `docs/09-api-spec/mediation.md`, linked it from admin/report/API README docs, and added admin HTTP smoke examples for escalation, list, detail, status, and decision.
- Verification passed: `backend\\.\\mvnw.cmd test` (131 tests), `frontend\\npm test` (35 tests), `frontend\\npm run build`, and `git diff --check`.
- HTTP smoke examples were updated but not manually executed against a running seed-profile server; automated backend tests cover the endpoint behavior.
