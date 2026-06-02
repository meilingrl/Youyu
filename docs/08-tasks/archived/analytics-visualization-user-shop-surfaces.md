# Task: Analytics Visualization For User And Shop Surfaces

## Metadata

- ID: analytics-visualization-user-shop-surfaces
- Status: completed
- Owner: unassigned
- Track: feature
- Depends on: `analytics-visualization-parent`, `analytics-visualization-execution-spec`
- Priority: high
- Planned date: 2026-06-02
- Completed date: 2026-06-02

## Objective

Turn the current profile and shop analytics surfaces into clearer visual summaries based on real data that already exists, without pretending unsupported metrics or social features are ready.

## Background

Current code already loads:

- `marketStore.loadUserInsightSnapshot()` into `ProfileView.vue`
- `marketStore.loadShopInsightSnapshot()` into `ShopView.vue`

But the presentation is still limited:

- profile shows only a few summary cards and no visual explanation of category preference or recent activity structure
- shop view still contains reserved/placeholder-style analytics framing in several sections

## Scope

- improve profile analytics presentation using the existing user insight snapshot
- improve shop analytics presentation using the existing shop insight snapshot
- add or refine shared visualization/presentation components as needed
- allow narrow additive backend fields only if required to support the accepted UI truth cleanly

## Out of Scope

- payment/refund analytics
- fake social/community metrics
- full recommendation redesign
- unrelated profile/settings cleanup
- backend event collection architecture

## Files to Read

- `frontend/README.md`
- `frontend/src/views/app/ProfileView.vue`
- `frontend/src/views/app/ShopView.vue`
- `frontend/src/components/common/ReservedMetricCard.vue`
- `frontend/src/components/common/ReservedPanel.vue`
- `frontend/src/constants/insightMetrics.js`
- `frontend/src/stores/market.js`
- `backend/src/main/java/com/youyu/backend/service/user/impl/UserServiceImpl.java`
- `backend/src/main/java/com/youyu/backend/service/shop/impl/ShopServiceImpl.java`

## Allowed Changes

- scoped frontend user/shop analytics files
- related shared frontend components
- narrow backend user/shop insight response shaping if needed
- related frontend tests

## Implementation Plan

1. Audit which user/shop analytics fields are already real and stable.
2. Replace weak reserved-state framing where real visualization is now possible.
3. Keep clearly unsupported capabilities labeled honestly rather than implied.

## Risks

- adding flashy visuals that are not backed by real metrics
- making user/shop analytics inconsistent with the underlying API semantics

## Test Plan

- Backend: run targeted tests only if user/shop insight response shape changes
- Frontend: run touched tests and `npm run build`
- API validation: update touched analytics examples/specs if response shape changes
- Manual: verify profile and shop pages show real visual summaries without false promises

## Acceptance Criteria

- [x] Profile analytics meaningfully visualizes existing real insight data.
- [x] Shop analytics uses real insight data where available and keeps unsupported areas explicit.
- [x] No unsupported social or recommendation capability is implied as complete.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes

- Added `frontend/src/components/common/InsightBarList.vue` as a reusable horizontal-bar summary component.
- Rebuilt `frontend/src/views/app/ProfileView.vue` around user spending, including category subtotal and recent paid-order bars.
- Extended `frontend/src/views/app/ShopView.vue` with monthly shop-income and hot-product-income summaries while preserving existing reserved/disabled capability boundaries.
- Added category `spendAmount` and hot-product `salesAmount` fields, and corrected the existing monthly-order and repeat-buyer aggregation semantics.
