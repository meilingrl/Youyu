# Task: Order And After-Sales UX Hardening

## Metadata

- ID: order-after-sales-ux-hardening
- Status: completed
- Owner: meilingrl
- Track: cross-cutting
- Depends on: current order, payment, refund, and report baseline
- Priority: high
- Planned date: 2026-05-16
- Completed date: 2026-05-16

## Objective

Harden the order, payment, refund, report, and order-detail interaction flow so the current non-chat communication and after-sales experience is clear, robust, and consistent across desktop and mobile layouts.

## Background

Current repository guidance explicitly avoids building a full IM system in this stage. Instead, order remarks, refund requests, reports, and related governance actions must carry the communication and after-sales experience. These flows therefore need stronger UX feedback, clearer information structure, and basic duplicate-submission protection.

This task builds a safer product floor without entering the separate chat or mediation roadmap.

## Scope

- Order detail communication-related information structure
- Refund initiation and feedback flow
- Report / governance entry feedback and status visibility
- Payment result and order-state feedback continuity
- Basic duplicate-submit protection and illegal-state guards
- Responsive readability and operability for key order / after-sales pages

## Out of Scope

- Full chat / IM system
- New mediation workflow or dispute center
- Broad redesign of the entire marketplace UI
- Search, recommendation, or unrelated admin features

## Files to Read

- `../../04-standards/development-process.md`
- `../../05-roadmap/current/stage-roadmap.md`
- `../../02-requirements/communication-and-after-sales-boundary.md`
- `../../02-requirements/order-lifecycle-and-fulfillment.md`
- `../../02-requirements/non-functional-requirements.md`
- `../../../frontend/README.md`
- `../../../backend/README.md`
- relevant frontend order / payment / report views
- relevant backend order / payment / report controllers and services

## Allowed Changes

- frontend order, payment, checkout, report, and related shared UI modules
- backend order, payment, report, and validation-related modules
- relevant files in `../../06-http/`
- related task and changelog documents

## Implementation Plan

1. Audit current user flow across checkout, payment, orders, refund, and report entry points.
2. Identify interaction gaps: missing feedback, unclear status, repeat-submit risk, illegal-state behavior, and poor mobile readability.
3. Improve frontend state handling for loading, success, empty, error, and invalid-operation cases.
4. Tighten backend validation and error messaging where current flow allows ambiguous or duplicated actions.
5. Restructure order-detail and related views so remarks, refund, report, and fulfillment information are clearly separated.
6. Verify the hardened flow on both desktop and mobile viewport assumptions.

## Risks

- Scope creep into chat or mediation features
- Inconsistent frontend copy if multiple views are updated separately
- Backend validation changes surfacing previously hidden client assumptions
- UX hardening becoming a silent visual redesign rather than behavior-focused improvement

## Test Plan

- Backend:
  - run focused order / payment / report tests
  - verify duplicate-submit and illegal-state cases
- Frontend:
  - run unit tests for touched stores or interaction logic
  - verify loading and error states do not regress
- API validation:
  - update relevant request examples in `docs/06-http/`
- Manual:
  - test checkout -> pay -> order view -> refund/report flow
  - test mobile-width readability and button accessibility on key pages

## Acceptance Criteria

- [ ] Order and after-sales entry points always provide explicit feedback
- [ ] Duplicate refund/report/payment-adjacent submissions have basic protection
- [ ] Illegal state transitions show clear user-facing guidance
- [ ] Order detail information is structured clearly without mixing notes, refund, and governance records
- [ ] Key pages remain usable on both desktop and mobile widths

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [ ] task status and archive move

## Completion Notes

- Backend: added duplicate refund check (pending refund guard) in `OrderServiceImpl.applyRefund`; added duplicate payment guard and already-success check in `PaymentServiceImpl`
- Frontend OrdersView: added `actionLoading` flag on all action buttons with `:loading`/`:disabled`; added buyer-side report submission dialog; added `buyerNote` display; added payment records section; made drawer width responsive (100% on mobile, 680px on desktop); added refund reason client-side validation; added mobile responsive CSS
- Frontend PaymentView: added `submitting` guard; buttons disable when not `pending_payment`; added post-payment navigation link and paid-state notice
- Frontend CheckoutView: added `submitting` guard; added client-side offline time format validation and address selection validation
- Frontend Admin OrderManageView: added `actionLoading` on admin action buttons; made drawer responsive; added `buyerNote` and payment records sections
- Smoke tests: added duplicate refund, duplicate payment, and duplicate mock-success test cases to `order.http`
- All tests pass: backend 37/37, frontend 25/25
