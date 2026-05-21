# Task: UI Redesign Product And Shop Detail

## Metadata

- ID: ui-redesign-product-shop-detail
- Status: completed
- Owner:
- Track: feature
- Depends on: `ui-redesign-shell-navigation-foundation`
- Priority: high
- Planned date:
- Completed date: 2026-05-17

## Objective

重构商品详情页和店铺详情页，使商品详情更像 Etsy 式交易决策页，使店铺详情成为店主对外主体和店铺信用展示页。

## Background

新的信息架构确认：商品详情和店铺详情是探索页之后的核心承接页面。店主不进入系统后台，店铺是店主的对外主体，店主个人信息应被包含在店铺主体内。

## Scope

- 商品详情页信息层级。
- 店铺详情页信息层级。
- 商品图、价格、交易方式、保障、卖家/店铺入口。
- 店铺介绍、店铺商品、信用、评价、联系店主、粉丝群/优惠群入口占位。
- 商品详情到消息、交易、店铺详情的入口。

## Out of Scope

- 首页和探索页重构。
- 购物车、订单、售后页重构。
- 真实聊天后端实现。
- 店主后台管理页。
- 管理员后台页面。

## Files to Read

- `../../03-architecture/ui-ux-constitution.md`
- `../../03-architecture/frontend-information-architecture.md`
- `../../../frontend/src/views/app/ProductDetailView.vue`
- `../../../frontend/src/views/app/ShopView.vue`
- `../../../frontend/src/components/common/RatingSummary.vue`
- `../../../frontend/src/components/common/ReviewList.vue`
- `../../../frontend/src/components/common/ReviewForm.vue`
- `../../../frontend/src/stores/market.js`
- `../../../frontend/src/stores/review.js`
- `../../../frontend/src/stores/recommend.js`
- relevant product, shop, review, favorite API modules

## Allowed Changes

- `frontend/src/views/app/ProductDetailView.vue`
- `frontend/src/views/app/ShopView.vue`
- detail-specific components under `frontend/src/components/`
- minimal store changes for presentation state if needed
- related docs and changelog

## Implementation Plan

1. Redesign product detail around purchase decision: image, title, price, trust, seller/shop, CTA, details, reviews, recommendations.
2. Redesign shop detail around shop as external identity: shop hero, owner signal, trust metrics, products, reviews, contact/follow/group entry points.
3. Use Aesop-style spacing for static detail sections and Etsy-style commerce hierarchy for decision areas.
4. Add contact/message entry points as UI affordances, even if final message implementation is handled by another task.
5. Verify desktop and mobile detail layouts.

## Risks

- Contact/chat entry points may outpace actual message feature readiness.
- Store or API shapes may not provide all desired shop identity data yet.
- Product detail can become too editorial and hide purchase actions.
- Shop page can become too heavy if product list, reviews, and profile all compete.

## Test Plan

- Backend: not required unless endpoint needs are discovered.
- Frontend: run `npm run test`.
- API validation: not required unless endpoint usage changes.
- Manual: verify product detail, shop detail, favorite/add-to-cart/contact entry, reviews, recommendations, loading/error/empty states, desktop/mobile.

## Acceptance Criteria

- [ ] Product detail clearly supports purchase decisions.
- [ ] Shop detail clearly acts as the seller's public-facing subject.
- [ ] Store owner personal information is included only as part of shop identity where applicable.
- [ ] Contact and trust entry points are visible and understandable.
- [ ] Desktop and mobile layouts are both usable.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes

- Rebuilt `frontend/src/views/app/ProductDetailView.vue` into a purchase-decision page with media-first layout, visible seller/shop entry, add-to-cart/buy/favorite/contact actions, product description, review hierarchy, and related recommendations.
- Rebuilt `frontend/src/views/app/ShopView.vue` into a shop-subject page focused on store introduction, owner-facing public identity, trust and trade metrics, public goods, reviews, and reserved group/follow entry points without pretending backend support exists.
- Kept API usage on the existing product, shop, review, favorite, recommend, and order modules; centralized new display compatibility in `market-normalizers.js` and `market.js`.
- Replaced unresolved `v-loading` usage in both detail pages with explicit skeleton loading states after self-check surfaced runtime directive warnings during manual browser inspection.
- `npm run test` passed.
- `npm run build` passed.
- Manual browser checks covered `/app/products/1` and `/app/shops/1` in frontend-only local runtime; because the backend API was offline, validation focused on safe error-state rendering rather than live detail data.
