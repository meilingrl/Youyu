# Task: Order Detail Dedicated Experience

## Metadata

- ID: order-detail-dedicated-experience
- Status: completed
- Owner: worker-2
- Track: feature
- Depends on: current order list/detail/payment/refund/report baseline
- Priority: high
- Planned date: 2026-05-27
- Completed date: 2026-05-27

## Objective

Add a stable order-detail experience that supports direct links, clearer information hierarchy, and future after-sales or mediation entry points without overloading the order list page.

## Background

`OrdersView.vue` previously opened order detail in an `el-drawer`. This was usable but weak for deep linking, refresh behavior, messages/notifications jumping to a specific order, and future after-sales expansion.

This task introduced a dedicated detail route while keeping the existing order list route functional.

## Scope

- Add an authenticated user route for `/app/orders/:orderId`.
- Create a dedicated order detail view using the existing `getOrderDetail`, `cancelOrder`, `confirmReceipt`, `buyerConfirmOffline`, `applyRefund`, `accessDigitalAsset`, and `submitReport` flows.
- Move or duplicate the necessary buyer-side order detail UI into a maintainable dedicated view.
- Update the order list so "view detail" navigates to the dedicated route.
- Support returning to `/app/orders`.
- Preserve current buyer-side available-action behavior driven by `availableActions`.
- Include mobile-friendly primary actions inside the dedicated detail view.

## Out of Scope

- Admin order detail redesign.
- Backend order state-machine changes.
- New mediation workflow.
- Message-center implementation changes.
- Cart, checkout, or payment-page mobile action bars.
- Broad visual redesign unrelated to detail readability and actions.

## Files Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `frontend/README.md`
- `frontend/src/views/app/OrdersView.vue`
- `frontend/src/router/modules/app.js`
- `frontend/src/api/modules/order.js`
- `frontend/src/api/modules/report.js`
- `frontend/src/components/trade/TradeOrderCard.vue`
- `frontend/src/components/trade/TradeStatusTag.vue`
- `frontend/src/components/trade/trade-meta.js`
- `docs/09-api-spec/order.md`

## Changes

- Added `frontend/src/views/app/OrderDetailView.vue`.
- Updated `frontend/src/router/modules/app.js` with `/app/orders/:orderId`.
- Updated `frontend/src/views/app/OrdersView.vue` so detail navigation uses the new route and legacy `/app/orders?orderId=` links redirect to it.
- Updated `CHANGELOG.md`.
- No backend, schema, cart, checkout, payment, message-center, API spec, or HTTP smoke files changed.

## Test Plan

- Backend:
  - Not required because no backend code changed.
- Frontend:
  - `npm test`
  - `npm run build`
- API validation:
  - No update required because endpoint usage did not change.
- Manual:
  - Navigate from `/app/orders` to `/app/orders/:orderId`.
  - Refresh the detail page and verify it reloads the same order.
  - Verify pay, cancel, confirm receipt, refund, report, and digital asset actions where available.
  - Verify mobile layout and bottom/primary action behavior.

## Acceptance Criteria

- [x] `/app/orders/:orderId` exists and loads the correct buyer-owned order detail.
- [x] The order list navigates to the dedicated detail route.
- [x] Existing order actions still call the same APIs and honor `availableActions`.
- [x] Refreshing a detail URL does not lose context.
- [x] Detail view is usable on mobile without hiding critical actions.
- [x] No backend, schema, cart, checkout, payment, or message-center files are changed.

## Documentation Updates Required

- [x] task document created
- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/` only if endpoint usage changes: not required
- [x] roadmap or standards docs if applicable: not required
- [x] task status and archive move

## Completion Notes

- Route behavior: `/app/orders/:orderId` loads `OrderDetailView.vue`, fetches detail from `getOrderDetail(route.params.orderId)`, and reloads after buyer actions.
- List behavior: `OrdersView.vue` remains the order list and routes "查看详情" to `app-order-detail`.
- Legacy compatibility: `/app/orders?orderId=<id>` is preserved through a `router.replace` redirect to `/app/orders/<id>`.
- Action semantics: pay, cancel, confirm receipt, offline confirm, and refund buttons are rendered only when `availableActions` contains their respective action key.
- Digital asset access, report submission, and message entry points use the same existing API/navigation flows as the previous drawer implementation.
- Head-agent verification after installing frontend dependencies: `npm test` passed with 7 test files and 30 tests; `npm run build` passed.
