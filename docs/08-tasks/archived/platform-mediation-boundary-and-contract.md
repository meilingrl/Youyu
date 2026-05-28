# Task: Platform Mediation Boundary And Contract

## Metadata

- ID: platform-mediation-boundary-and-contract
- Status: completed
- Owner: unassigned
- Track: cross-cutting
- Depends on: `docs/02-requirements/chat-mvp-scope.md`, current report/order/refund/chat/admin baselines
- Priority: high
- Planned date: 2026-05-28
- Completed date: 2026-05-28

## Objective

Define the v1 platform mediation boundary and contract before implementation. This task must produce requirements and implementation constraints, not business code.

## Background

Support console v1 is only a context dashboard. Platform mediation is the remaining core admin capability needed to turn report/order disputes into formal platform decisions. The accepted product model is report escalation into a new `mediation_cases` domain. Admins may inspect related chat context read-only in v1, but they may not join or send messages in buyer/seller chat.

## Scope

- Define how reports can escalate into mediation cases.
- Define what belongs to `reports`, `orders`, `refund_records`, chat context, support console, and `mediation_cases`.
- Define mediation case statuses and decision categories at requirements level.
- Define admin surfaces and API/data contract expectations at a high level.
- Produce or update the implementation task for mediation code work if needed.

## Out of Scope

- Backend implementation.
- Frontend implementation.
- Database migration or schema edits.
- Three-party chat, admin message sending, automated decision logic, appeals, SLA assignment, or a full ticketing system.
- Buyer-facing mediation UI beyond status visibility assumptions.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/05-roadmap/current/admin-module-goal-roadmap.md`
- `docs/02-requirements/chat-mvp-scope.md`
- `docs/02-requirements/admin-support-console-scope.md`
- `docs/05-roadmap/current/feature-roadmap.md`
- `docs/08-tasks/archived/admin-support-console-contract-definition.md`
- `docs/08-tasks/archived/admin-support-console-implementation.md`
- `backend/src/main/resources/schema.sql` reports, orders, refunds, chat tables
- report, admin, order, refund, and chat backend controllers/services
- admin report/order/support frontend views
- `docs/06-http/admin.http`
- `docs/09-api-spec/admin.md`

## Allowed Changes

- `docs/02-requirements/platform-mediation-scope.md`.
- `docs/08-tasks/active/platform-mediation-implementation.md` updates if this boundary changes implementation expectations.
- `docs/05-roadmap/current/feature-roadmap.md`.
- `docs/05-roadmap/current/admin-module-goal-roadmap.md` if sequencing or status changes.
- `CHANGELOG.md`.
- This task document lifecycle updates.

## Implementation Plan

1. Stop immediately if `docs/02-requirements/chat-mvp-scope.md` is missing.
2. Read report/order/refund/chat/support contracts and current runtime code.
3. Define v1 mediation as report escalation into `mediation_cases`.
4. Define the allowed read-only chat context relationship.
5. Define requirements-level API, data, admin surface, seed, and testing expectations for implementation.
6. Update or validate the mediation implementation task.

## File Scope

This is a documentation and contract task. It must not edit source code, schema, or seed data.

## API / Data Contract Impact

Expected contract direction:

- `mediation_cases` is a new domain for formal platform dispute handling.
- A mediation case should reference the source report and related order where applicable.
- Reports remain reports and should not become the final decision store.
- Admin read-only chat context must be constrained to related order/report conversations.

The exact endpoint and schema shape should be specified enough for implementation, but code-level details remain for the implementation task.

## Risks

- Confusing support triage with formal mediation.
- Making chat admin-writable in v1.
- Coupling final decisions into report processing too tightly.
- Producing an implementation task that lacks testable acceptance criteria.

## Verification Plan

- Documentation review: `docs/02-requirements/platform-mediation-scope.md` exists.
- Documentation review: mediation implementation task has clear boundaries and acceptance criteria.
- Run `git diff --check`.
- No backend/frontend test is required unless code is changed, which is out of scope.

## Acceptance Criteria

- [x] Task remains blocked if `docs/02-requirements/chat-mvp-scope.md` is absent.
- [x] `platform-mediation-scope.md` clearly separates reports, support, chat, orders/refunds, and mediation.
- [x] The chosen model is `mediation_cases` created from report escalation.
- [x] Admin chat access is read-only and scoped to related dispute context.
- [x] The implementation task is ready for a sub-agent without needing product decisions.
- [x] No code, schema, or seed files are changed.
- [x] `CHANGELOG.md` is updated.
- [x] `git diff --check` passes.
- [x] Completion notes are filled before archive.

## Sub-agent Instructions

- Do not write implementation code.
- Do not modify `schema.sql`.
- Do not create a ticketing system.
- If chat scope is missing or ambiguous, report the blocker instead of guessing.
- Return created/updated docs, unresolved contract risks, and whether implementation can be dispatched.

## Feedback To Head Agent

Return:

- created or updated requirement/task docs;
- blocker status for chat scope;
- final mediation boundary decisions;
- implementation task readiness;
- unresolved contract risks.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/` if no API changes are expected
- [x] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes

Completed on 2026-05-28.

- Confirmed `docs/02-requirements/chat-mvp-scope.md` exists, so the prior chat-scope blocker is cleared.
- Created `docs/02-requirements/platform-mediation-scope.md`.
- Defined mediation v1 as order-backed report escalation into `mediation_cases`.
- Kept `reports` as accusation/governance intake and `mediation_cases` as the formal dispute/decision owner.
- Kept `/admin/support` as context/navigation only.
- Kept chat visibility read-only, mediation-scoped, and outside user-owned `/api/chat/**` admin calls.
- Defined v1 mediation statuses: `opened`, `evidence_review`, `decision_pending`, `resolved`, `cancelled`.
- Defined decision categories: `refund_full_to_buyer`, `refund_rejected_release_to_seller`, `order_completion_required`, `platform_governance_action`, `no_action_invalid_or_duplicate`.
- Rewrote `docs/08-tasks/active/platform-mediation-implementation.md` with explicit data, API, admin UI, seed, testing, and acceptance criteria.
- Updated current roadmaps and `CHANGELOG.md`.
- No backend, frontend, schema, or seed files were changed.

Verification:

- `git diff --check` - passed.

Residual documentation notes:

- During head Agent review, `docs/02-requirements/admin-support-console-scope.md` and `docs/05-roadmap/current/open-questions.md` were aligned with the accepted chat and mediation scope documents so they no longer describe the mediation scope as absent or unresolved.
