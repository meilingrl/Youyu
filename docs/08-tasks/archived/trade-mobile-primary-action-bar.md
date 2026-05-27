# Task: Trade Mobile Primary Action Bar

## Metadata

- ID: trade-mobile-primary-action-bar
- Status: completed
- Owner: worker-5
- Track: feature
- Depends on: current trade UI baseline
- Priority: high
- Planned date: 2026-05-27
- Completed date: 2026-05-27

## Objective

Make the key mobile actions in cart, checkout, and payment flows stable and always easy to reach by introducing a consistent mobile primary-action pattern.

## Background

The current trade pages are responsive, but their primary actions live inside normal page content or top-page actions. On mobile, users may need to scroll to find "checkout", "submit order", or "pay". Transaction flows should keep the current amount/status and the next action visible at a stable bottom position.

Order-detail mobile action behavior belongs to `order-detail-dedicated-experience`; this task intentionally avoids order-detail files to keep parallel work safe.

## Scope

- Add a reusable mobile-only primary action component or local pattern for transaction pages.
- Apply it to:
  - `CartView.vue`
  - `CheckoutView.vue`
  - `PaymentView.vue`
- Show concise state next to the main action, such as selected item count and amount, payable amount, or order payment state.
- Ensure disabled/loading states match existing duplicate-submit protections.
- Add enough bottom padding so fixed mobile bars do not cover page content.
- Keep desktop layout visually unchanged except for harmless shared CSS/component additions.

## Out of Scope

- `OrdersView.vue`
- New `OrderDetailView.vue`
- Trade dashboard status aggregation.
- Backend changes.
- API spec work.
- Message-center behavior.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `frontend/README.md`
- `frontend/src/views/app/CartView.vue`
- `frontend/src/views/app/CheckoutView.vue`
- `frontend/src/views/app/PaymentView.vue`
- `frontend/src/components/trade/TradePageShell.vue`
- `frontend/src/components/trade/trade-meta.js`
- `frontend/src/styles/variables.css`
- `frontend/src/styles/index.css`

## Allowed Changes

- `frontend/src/views/app/CartView.vue`
- `frontend/src/views/app/CheckoutView.vue`
- `frontend/src/views/app/PaymentView.vue`
- New focused component under `frontend/src/components/trade/`, for example `TradeMobileActionBar.vue`
- Focused CSS additions in `frontend/src/styles/index.css` or page-scoped styles if needed
- `docs/08-tasks/active/trade-mobile-primary-action-bar.md`
- `CHANGELOG.md` when the task is completed

## Implementation Plan

1. Identify current primary action and amount/status data in cart, checkout, and payment pages.
2. Create a mobile-only bottom action component that accepts label, helper/status text, amount or count, loading, disabled, and click handler.
3. Wire cart to show selected count, selected amount, and "go checkout".
4. Wire checkout to show payable amount and "submit order".
5. Wire payment to show order payment state/payable amount and "pay" only when the order is payable.
6. Add responsive padding and verify buttons do not overlap content or mobile navigation.

## Risks

- Fixed bottom bars can conflict with existing mobile bottom navigation.
- Duplicating top and bottom actions can create inconsistent disabled/loading behavior.
- Overly broad CSS can affect unrelated pages.

## Test Plan

- Backend:
  - Not required.
- Frontend:
  - Run `npm test`.
  - Run `npm run build`.
- API validation:
  - Not required.
- Manual:
  - Verify cart mobile width with no selection and with selected items.
  - Verify checkout mobile width with address/offline validation and submit loading.
  - Verify payment mobile width for payable and already-paid states.
  - Verify desktop pages still look consistent.

## Acceptance Criteria

- [x] Cart, checkout, and payment have a stable mobile primary action area.
- [x] The mobile action reflects existing disabled and loading logic.
- [x] The mobile action displays relevant amount/status context.
- [x] Fixed action UI does not cover content or conflict with mobile bottom navigation.
- [x] No order list/detail, backend, API spec, or message-center files are changed.

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
- exact mobile action behavior on cart, checkout, and payment
- test commands and exit codes
- acceptance-criteria checklist
- any viewport or mobile-navigation overlap risk that remains

## Completion Notes

- Added `TradeMobileActionBar.vue` as a mobile-only fixed action bar positioned above the existing mobile bottom navigation, with a spacer to keep page content scrollable above the fixed UI.
- Cart now shows selected settlement amount, selected item helper text, and a disabled `去结算` action until at least one item is selected.
- Checkout now shows payable amount, item count plus fulfillment type, and a submit action sharing the existing `submitting || !preview` guard.
- Payment now shows payable amount plus payment/order status. It shows `模拟支付成功` only while the order is payable; otherwise the mobile action becomes `返回订单`.
- Head-agent verification after installing frontend dependencies: `npm test` passed with 7 test files and 30 tests; `npm run build` passed.
