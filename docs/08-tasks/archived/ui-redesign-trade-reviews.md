# Task: UI Redesign Trade And Reviews

## Metadata

- ID: ui-redesign-trade-reviews
- Status: completed
- Owner:
- Track: feature
- Depends on: `ui-redesign-shell-navigation-foundation`
- Priority: high
- Planned date:
- Completed date: 2026-05-17

## Objective

将购物车、订单、支付、售后、举报、待评价和我的评价收束为清晰的交易中心体验，强化交易闭环和售后反馈。

## Background

新的页面结构确认交易作为前台一级能力。购物车、订单、售后、评价都属于交易域，而不是分散在多个一级导航中。

## Scope

- 交易中心入口页或交易域导航。
- 购物车体验。
- 结算体验。
- 支付状态体验。
- 订单列表与订单详情。
- 售后、退款、举报入口。
- 待评价与我的评价。

## Out of Scope

- 首页和探索页。
- 商品详情和店铺详情视觉重构。
- 消息中心真实实现。
- 后台客服管理。
- 后端交易模型大改。

## Files to Read

- `../../03-architecture/ui-ux-constitution.md`
- `../../03-architecture/frontend-information-architecture.md`
- `../../02-requirements/order-lifecycle-and-fulfillment.md`
- `../../02-requirements/communication-and-after-sales-boundary.md`
- `../../../frontend/src/views/app/CartView.vue`
- `../../../frontend/src/views/app/CheckoutView.vue`
- `../../../frontend/src/views/app/PaymentView.vue`
- `../../../frontend/src/views/app/OrdersView.vue`
- `../../../frontend/src/views/app/PendingReviewsView.vue`
- `../../../frontend/src/views/app/MyReviewsView.vue`
- `../../../frontend/src/stores/market.js`
- `../../../frontend/src/stores/review.js`
- relevant order, payment, review, report API modules

## Allowed Changes

- `frontend/src/views/app/CartView.vue`
- `frontend/src/views/app/CheckoutView.vue`
- `frontend/src/views/app/PaymentView.vue`
- `frontend/src/views/app/OrdersView.vue`
- `frontend/src/views/app/PendingReviewsView.vue`
- `frontend/src/views/app/MyReviewsView.vue`
- new trade-specific components under `frontend/src/components/`
- minimal store changes for UI state if needed
- related docs and changelog

## Implementation Plan

1. Define a trade-domain landing or internal navigation pattern.
2. Make cart, checkout, payment, orders, after-sales, and reviews feel like one continuous flow.
3. Improve status hierarchy for pending payment, paid, fulfilled, refund, completed, and review states.
4. Keep feedback explicit for loading, success, failure, empty, illegal-state, and duplicate-submit cases.
5. Keep mobile transaction flows clear and one-path-at-a-time.
6. Verify the full flow from cart to order to review.

## Risks

- Transaction pages have higher regression risk than purely visual pages.
- Current order detail is drawer-based; a future `/app/orders/:id` route may need separate planning.
- UI simplification must not hide important state or after-sales actions.
- Parallel message-center work may overlap with communication entry points.

## Test Plan

- Backend: run focused order/payment/report/review tests if backend behavior changes.
- Frontend: run `npm run test`.
- API validation: update `docs/06-http/order.http`, `review.http`, or `report.http` only if endpoint usage changes.
- Manual: verify cart -> checkout -> payment -> orders -> refund/report -> review flow on desktop and mobile.

## Acceptance Criteria

- [x] Trade is understandable as one coherent domain.
- [x] Cart, checkout, payment, orders, after-sales, and reviews share consistent state feedback.
- [x] High-risk actions have loading and duplicate-submit protection.
- [x] Empty and error states guide the user forward.
- [x] Mobile transaction flow remains clear and usable.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes

- Added a trade-domain component layer under `frontend/src/components/trade/` so cart, checkout, payment, orders, pending reviews, and my reviews now share one transaction shell, shared status language, and clearer next-step guidance.
- Kept legacy trade routes working and did not modify backend API contracts, transaction state machine semantics, or database schema.
- Preserved explicit loading, duplicate-submit protection, illegal-state handling, refund/report/review entry points, and empty/error states across the trade-domain pages.
- Verification completed with `npm test` and `npm run build`.
- Backend tests were not run because this rollout did not change backend code or API contracts.
