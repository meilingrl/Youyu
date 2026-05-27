# Task: Trade Center Live Status Dashboard

## Metadata

- ID: trade-center-live-status-dashboard
- Status: completed
- Owner: worker-1
- Track: feature
- Depends on: current cart, order, review API baseline
- Priority: high
- Planned date: 2026-05-27
- Completed date: 2026-05-27

## Objective

Turn `/app/trade` from a mostly static navigation page into a real trade dashboard that summarizes the current user's actionable transaction state.

## Background

The trade center currently gives users entry cards for cart, orders, and reviews. It does not answer the more important question: "What trade actions need my attention right now?"

This task should aggregate existing API data only. It must not add backend endpoints unless the implementation proves the current APIs cannot support the minimum dashboard.

## Scope

- Load current cart, order list, and pending-review data on `TradeView`.
- Show real counts for cart items, selected cart items, pending payment, pending receipt, refund in progress, completed orders, and pending reviews.
- Add a prioritized action area, for example pay now, confirm receipt, review pending items, or return to cart.
- Preserve quick navigation to cart, orders, pending reviews, and my reviews.
- Add explicit loading, error, retry, and empty-state behavior.
- Keep desktop and mobile layouts readable without depending on a separate implementation task.

## Out of Scope

- New backend aggregation endpoint.
- Reworking order detail behavior.
- Changing review eligibility rules.
- Changing message or notification behavior.
- Editing `OrdersView.vue`, `CartView.vue`, `CheckoutView.vue`, `PaymentView.vue`, or `frontend/src/stores/review.js`.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `frontend/README.md`
- `frontend/src/views/app/TradeView.vue`
- `frontend/src/components/trade/trade-meta.js`
- `frontend/src/components/trade/TradeMetricStrip.vue`
- `frontend/src/components/trade/TradePageShell.vue`
- `frontend/src/api/modules/order.js`
- `frontend/src/api/modules/review.js`

## Allowed Changes

- `frontend/src/views/app/TradeView.vue`
- New trade-dashboard components under `frontend/src/components/trade/`
- Focused changes to `frontend/src/components/trade/trade-meta.js` only if new status helper text is needed
- `docs/08-tasks/active/trade-center-live-status-dashboard.md`
- `CHANGELOG.md` when the task is completed

## Implementation Plan

1. Replace static metric values in `TradeView` with data loaded from `getCart()`, `getOrderList()`, and `getPendingReviewItems()`.
2. Derive actionable buckets from existing frontend order status values: `pending_payment`, `pending_receipt`, `refund_in_progress`, and `completed`.
3. Render a prioritized "next actions" section that links directly to the relevant existing routes.
4. Render quick-entry cards below the live summary so the page still works as a trade-domain hub.
5. Add loading and partial-failure handling; one failed source must not make the whole dashboard blank if other data loaded successfully.

## Risks

- The dashboard can become noisy if it repeats everything from the order list.
- Calling three APIs on page load may expose inconsistent loading behavior.
- Parallel tasks may touch shared trade components; keep component changes local and avoid broad style rewrites.

## Test Plan

- Backend:
  - Not required unless backend APIs are changed.
- Frontend:
  - Run `npm test`.
  - Run `npm run build`.
- API validation:
  - No update required unless endpoint usage or contracts change.
- Manual:
  - Verify `/app/trade` with empty cart/no orders.
  - Verify `/app/trade` with cart items and pending payment orders.
  - Verify pending receipt, refund in progress, completed, and pending review counts.
  - Verify mobile layout at narrow width.

## Acceptance Criteria

- [x] `/app/trade` displays real user-specific trade counts from existing APIs.
- [x] The page clearly highlights at least one next action when actionable data exists.
- [x] Loading, partial error, retry, and all-empty states are visible and understandable.
- [x] Existing navigation to cart, orders, pending reviews, and my reviews still works.
- [x] No backend endpoint, schema, or message-center file is changed.

## Documentation Updates Required

- [x] task document created
- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/` only if endpoint usage changes
- [x] roadmap or standards docs if applicable
- [x] task status and archive move

## Required Agent Feedback

When finished, report:

- branch and commit or uncommitted status
- files changed
- API sources used for each dashboard count
- test commands and exit codes
- acceptance-criteria checklist
- any missing data that would require a future backend aggregation endpoint

## Completion Notes

- Reworked `frontend/src/views/app/TradeView.vue` into a live dashboard backed by `getCart()`, `getOrderList()`, and `getPendingReviewItems()`.
- Dashboard counts now cover cart items, selected cart items, pending payment, pending receipt, refund in progress, completed orders, and pending reviews.
- Added prioritized next-action cards for payment, receipt confirmation, refund follow-up, reviews, and cart return.
- Added per-source loading, partial error, retry, full-failure, and all-empty rendering.
- No backend endpoint, schema, message-center, cart, checkout, payment, orders, or review-store files were changed by this task.
- Head-agent verification after installing frontend dependencies: `npm test` passed with 7 test files and 30 tests; `npm run build` passed.
