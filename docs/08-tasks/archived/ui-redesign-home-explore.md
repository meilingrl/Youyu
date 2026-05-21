# Task Record: UI Redesign Home And Explore

## Metadata

- ID: ui-redesign-home-explore
- Status: archived
- Owner: Codex
- Track: feature
- Depends on: `ui-redesign-shell-navigation-foundation`
- Priority: high
- Completed date: 2026-05-18

## Delivered

- Reworked `frontend/src/views/app/HomeView.vue` into a lighter platform-introduction page with a stronger hero, explore-first CTA path, platform metrics, trust messaging, and buyer/seller onboarding
- Reworked `frontend/src/views/app/ProductListView.vue` into the main explore surface while preserving existing route-driven keyword, category, type, and pagination behavior
- Added shared explore UI components under `frontend/src/components/explore/` for the redesign shell, product cards, and featured shop presentation
- Upgraded `frontend/src/components/search/SearchSuggestInput.vue` and `frontend/src/components/search/HotSearchList.vue` so search, suggestions, and hot-keyword discovery feel like one coherent browsing surface
- Kept all data loading on the existing API modules, Pinia stores, and normalizers without introducing handwritten axios calls or backend contract changes

## Verification

- `npm test`
- `npm run build`
- Browser-based manual UI checks with mocked API responses for:
  - home desktop layout
  - explore desktop layout
  - search suggestion interaction
  - category filtering flow
  - featured shops rendering
  - empty state rendering
  - error state rendering
  - mobile overflow safety on home and explore

## Remaining Gaps

- Real visual richness still depends on future higher-quality product and shop imagery; this rollout stabilizes card ratio, crop, and surface hierarchy but does not solve source-asset quality
- Featured shop depth still depends on richer backend-facing shop profile data; current cards are intentionally derived from existing public product/shop data only

## Archive Note

This file records the completed home/explore redesign slice and should not be reused as a live implementation spec.
