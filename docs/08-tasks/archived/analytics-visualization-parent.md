# Task: Analytics Visualization Wave

## Metadata

- ID: analytics-visualization-parent
- Status: completed
- Owner: main-agent
- Track: cross-cutting
- Depends on: current roadmap truth, live user/shop insight APIs, admin dashboard baseline, blocked `preference-theme-capability-gap`
- Priority: high
- Planned date: 2026-06-02
- Completed date: 2026-06-02

## Objective

Run a governed delivery wave that upgrades the current user, shop, and admin analytics surfaces from metric-only or reserved-state presentation into clearer, more trustworthy visualization flows without mixing in payment/refund work that is still isolated on another branch.

## Background

Current repository truth shows:

- user insight snapshot is already backed by real aggregation in `UserServiceImpl.insightSnapshot()`
- shop insight snapshot is already backed by real aggregation in `ShopServiceImpl.insightSnapshot()`
- admin dashboard already has queue/governance/status breakdown data in `AdminServiceImpl.dashboard()`
- frontend user/shop/admin surfaces still rely heavily on metric cards, reserved panels, or lightweight list views rather than stronger visual explanation

The user explicitly decided to skip payment work in this round because payment changes are currently being developed on another branch and are not yet pushed or synchronized.

## Scope

- freeze the analytics-visualization wave boundaries before implementation
- upgrade user-side and shop-side insight presentation on top of existing real metrics
- deepen admin dashboard visualization where current backend data already exists or can be extended narrowly
- run a separate contract reconciliation task for touched analytics endpoints, HTTP assets, and API specs

## Out of Scope

- payment gateway, refund state machine, or any cross-branch payment merge work
- category-management delivery
- theme switching or broader personalization cleanup
- new event collection infrastructure beyond narrow backend additions required by the accepted visualization scope
- recommendation-system redesign

## Child Tasks

- [x] `analytics-visualization-execution-spec`
- [x] `analytics-visualization-user-shop-surfaces`
- [x] `analytics-visualization-admin-dashboard`
- [x] `analytics-visualization-contract-reconciliation`

## Locked Interfaces

- This wave does not own payment/refund feature changes.
- Existing user insight endpoint stays `GET /api/users/me/insight-snapshot`.
- Existing shop insight endpoint stays `GET /api/shops/{shopId}/insight-snapshot`.
- Existing admin dashboard endpoint stays `GET /api/admin/dashboard`.
- Visualization work must prefer existing APIs and narrow additive fields over broad backend rewrites.
- Do not add a new charting/UI library unless the main agent explicitly revisits that choice after repository evidence shows the existing stack is insufficient.

## Acceptance Criteria

- [x] Every child task is reviewed by the main agent before closeout.
- [x] User/shop/admin analytics surfaces become more explanatory without pretending unsupported data exists.
- [x] Any new or changed analytics fields are synchronized with `docs/06-http/` and `docs/09-api-spec/`.
- [x] Payment and refund work remains explicitly deferred in this wave.

## Completion Notes

- Added a shared `InsightBarList.vue` component for lightweight, dependency-free analytics visualization.
- Reworked `ProfileView.vue` to show purchase category and recent paid-order insight bars on top of the existing user snapshot endpoint.
- Upgraded `ShopView.vue` to explain monthly income and hot-product income while keeping unsupported capability areas explicit.
- Added backend admin sales aggregations and surfaced category sales share plus shop sales rankings in `DashboardView.vue`.
- Updated touched HTTP and API-spec documentation for the additive analytics fields and corrected shop metric semantics.
- Verification passed: backend full test suite (193 tests), final targeted backend analytics suite (55 tests), frontend unit suite (51 tests), frontend production build, and `git diff --check`.
