# Task: Wave 0 Favorites Closure

## Metadata

- ID: wave0-favorites-closure
- Status: completed
- Owner: worker-a
- Track: cross-cutting
- Depends on: `wave0-scope-lock-and-contract-boundary`
- Priority: high
- Planned date: 2026-06-01
- Completed date: 2026-06-01

## Objective

Close the existing favorites user path with a real backend contract and frontend integration that match the frozen Wave 0 interface.

## Background

The frontend already exposes favorites entry points and store logic, and `docs/06-http/product.http` already promises favorites endpoints. The backend contract is currently incomplete, and the frontend favorite API module currently drifts from the documented route shape.

## Scope

- implement favorites backend support for the frozen REST contract
- align frontend favorite API usage and store behavior to that contract
- support favorites list rendering and product-detail favorite toggle flow
- update only favorites-related HTTP/API docs required by the implementation

## Out of Scope

- category APIs or category-derived favorite analytics
- recommendation personalization based on favorites
- shop-level favorites or follow system
- changing unrelated product listing filters

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `frontend/src/api/modules/favorite.js`
- `frontend/src/stores/market.js`
- `frontend/src/views/app/FavoritesView.vue`
- `frontend/src/views/app/ProductDetailView.vue`
- `docs/06-http/product.http`
- relevant backend product/user schema and controller/service/mapper files

## Allowed Changes

- favorites-related backend controller/service/mapper/entity/schema seed files
- `frontend/src/api/modules/favorite.js`
- `frontend/src/stores/market.js`
- favorites-related frontend tests
- favorites-related HTTP/API spec files

## Implementation Plan

1. Implement the frozen favorites contract in the backend.
2. Align frontend API calls and store behavior to the frozen contract.
3. Verify favorites list and toggle behavior end to end.

## Risks

- introducing a toggle-only contract that drifts from the frozen REST routes
- modifying unrelated product or category logic while wiring favorites

## Test Plan

- Backend:
  - add or update favorites endpoint tests
- Frontend:
  - run relevant store/view tests and build if touched
- API validation:
  - update favorites HTTP examples and formal API documentation
- Manual:
  - verify add favorite, remove favorite, and favorites list flow

## Acceptance Criteria

- [x] `GET /api/favorites` returns the current user's favorite products or IDs consistently with the chosen response shape
- [x] `POST /api/favorites` adds a product to favorites idempotently
- [x] `DELETE /api/favorites/{productId}` removes a product from favorites idempotently
- [x] Frontend favorites entry points work against the real backend contract

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes
- Added `FavoriteController` with the frozen Wave 0 REST contract and user-role protection.
- Added persistent favorites storage via `product_favorites` plus idempotent counter updates.
- Aligned `frontend/src/api/modules/favorite.js` and `frontend/src/stores/market.js` to the real list/add/remove flow.
- Added backend integration coverage for add/list/remove, idempotency, unauthorized access, and invalid `productId` payloads.
