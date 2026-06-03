# Task: Buyer Payment Experience Upgrade

## Metadata

- ID: F2-payment-frontend-experience
- Status: completed
- Owner: worker-frontend-payment
- Track: feature
- Depends on: `payment-upgrade-gateway-foundation.md`
- Priority: P1
- Planned date: 2026-05-31
- Completed date: 2026-05-31

## Objective

Update the buyer payment page for payment-method selection and explicit
processing, success, failure, cancellation, timeout, and retry feedback without
exposing internal mock/test wording.

## Scope

- Reuse the existing payment API module and trade shell.
- Show available payment methods returned by the backend.
- Render clear buyer-facing payment states and retry actions.
- Preserve mobile primary-action behavior.
- Add or update focused frontend tests.

## Out of Scope

- New payment-provider business rules.
- Order-detail redesign.
- Admin refund UI redesign.
- Customer-service and unrelated trade-center changes.

## Allowed Changes

- `frontend/src/api/modules/payment.js`
- `frontend/src/views/app/PaymentView.vue`
- payment-focused helpers under `frontend/src/components/trade/`
- payment-focused frontend tests

## Locked Interfaces

- Reuse `/app/payments/:orderId`.
- Reuse existing trade shell components.
- Do not expose `mock`, `test`, `sandbox`, or internal gateway implementation
  wording in buyer-facing text.
- Keep loading, empty, error, illegal-state, and mobile states explicit.

## Acceptance Criteria

- [x] Buyer can choose an available payment method.
- [x] Processing, success, failure, cancellation, timeout, and retry states are
  understandable.
- [x] Mobile primary action remains usable.
- [x] Internal implementation wording is absent from buyer-facing text.
- [x] Changed-file list stays within ownership.

## Completion Notes

- Buyer payment methods now come from backend gateway discovery.
- Added explicit attempt states, retry actions, mobile continuity, local
  Alipay QR image rendering, and payment-entry recovery after page refresh.
- Verified by `npm test -- --run` (44 passing) and `npm run build` on
  2026-05-31.
