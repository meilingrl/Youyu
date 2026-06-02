# Task: Wave 1 Refund Assistance And Mediation Handoff

## Metadata

- ID: wave1-refund-assistance-and-mediation-handoff
- Status: completed
- Owner: worker-b
- Track: cross-cutting
- Depends on: `wave1-scope-lock-and-owner-sync`
- Priority: high
- Planned date: 2026-06-01
- Completed date: 2026-06-01

## Objective

Deepen refund and after-sales visibility so both users and admins can follow progress, evidence, and mediation escalation without dragging Wave 1 into payment-gateway redesign.

## Background

Current order/refund flows already exist, and formal mediation already exists, but the roadmap still calls out after-sales depth, refund negotiation detail, visible progress, and better handoff into mediation. The user explicitly wants backend/admin handling to stay in sync with any user-visible capability.

## Scope

- make refund progress and assistance history more legible across user and admin order/support surfaces
- add or clarify evidence / reason / progress fields where the current flow is too shallow
- support report-backed escalation into the existing mediation API from Wave 1 user/admin contexts where appropriate
- keep owner truth in the order and mediation modules

## Out of Scope

- payment-provider integration or callback redesign
- direct creation of mediation cases without the existing report-backed path
- final mediation-decision workflow redesign
- notification preference/template expansion

## Files to Read

- `backend/src/main/java/com/youyu/backend/controller/order/OrderController.java`
- `backend/src/main/java/com/youyu/backend/controller/order/AdminOrderController.java`
- `backend/src/main/java/com/youyu/backend/controller/mediation/AdminMediationController.java`
- `docs/09-api-spec/order.md`
- `docs/09-api-spec/mediation.md`
- order/refund-related frontend user/admin views

## Allowed Changes

- order/refund/mediation related backend files
- directly related user/admin order/support frontend files
- related tests
- directly related HTTP/API docs

## Implementation Plan

1. Audit the current refund records returned to user/admin detail views.
2. Add the minimum state/history/evidence depth needed for a believable after-sales trail.
3. Wire report-backed mediation escalation from the accepted contexts without creating a new owner path.

## Risks

- letting refund UI imply a deeper payment reconciliation contract than actually exists
- inventing a second mediation-entry contract that bypasses the report lane
- exposing a user-side refund/escalation action without a matching admin handling path

## Test Plan

- Backend:
  - run order/refund/mediation related tests
- Frontend:
  - run touched tests and frontend build
- Manual:
  - verify a refund-visible user path has matching admin context
  - verify mediation escalation still lands in the existing case list/detail flow

## Acceptance Criteria

- [x] User-visible refund progress has a corresponding admin-side handling or review path.
- [x] Refund assistance data is more traceable without implying a payment-gateway upgrade.
- [x] Wave 1 mediation handoff reuses the existing report-backed mediation contract.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [x] relevant files in `docs/09-api-spec/`
- [x] task status and archive move when complete

## Completion Notes

- Buyer/admin order detail responses now expose refund-rule guidance, linked reports, mediation summary, and aggregated after-sales status.
- Admin order management now links directly into support-ticket, report, and mediation handling lanes for the same order context.
