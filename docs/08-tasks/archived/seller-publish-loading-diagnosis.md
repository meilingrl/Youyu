# Task: Seller Publish Loading Diagnosis

## Metadata

- ID: seller-publish-loading-diagnosis
- Status: archived
- Owner: unassigned
- Track: feature
- Depends on: current seller-management frontend/backend baseline
- Priority: high
- Planned date: 2026-05-20
- Completed date: 2026-05-20

## Objective

Find the real cause of the seller publish page getting stuck in loading state and apply the smallest reliable fix.

## Background

The issue backlog explicitly reports that entering the seller publish page hangs on loading every time. This needs diagnosis first, not a speculative UI rewrite.

## Scope

- diagnose the loading hang on the seller publish page
- separate frontend-state causes from backend/API causes with evidence
- fix the smallest root cause in scope

## Out of Scope

- redesigning seller publish UX
- expanding seller capabilities
- unrelated seller-products cleanup

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `frontend/README.md`
- `backend/README.md`
- `frontend/src/views/app/SellerPublishView.vue`
- `frontend/src/views/app/SellerProductsView.vue`
- related seller product API/store/router files
- relevant backend product/shop endpoints if the hang is API-driven

## Allowed Changes

- seller publish frontend files
- the minimum backend/api files needed if the root cause is server-side
- relevant docs/http/api-spec files only if the runtime contract changes
- `CHANGELOG.md`
- task lifecycle files under `docs/08-tasks/`

## Implementation Plan

1. Trace the loading lifecycle and identify where it stops resolving.
2. Confirm the true failing dependency or state path.
3. Fix the minimum root cause and verify the page no longer hangs.

## Risks

- masking the problem with timeout hacks
- treating a backend failure as a pure frontend issue or vice versa

## Test Plan

- Backend:
  - run focused tests if backend files are touched
- Frontend:
  - run relevant tests/build if frontend files are touched
- API validation:
  - update docs only if a contract bug is fixed
- Manual:
  - verify the seller publish page loads under the intended user state

## Acceptance Criteria

- [x] The true cause of the loading hang is identified with evidence
- [x] The smallest reliable fix is applied
- [x] The seller publish page no longer hangs on entry
- [x] Documentation is updated only if scope requires it

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes

### Diagnosis

Three separate issues converged to cause the seller publish page to appear stuck:

1. **Backend wouldn't compile** (server-side root cause). `JdbcReviewMapper` was missing the `findOrderContextByOrderItemId(Long)` implementation declared in the `ReviewMapper` interface. This prevented the entire backend from starting — all API endpoints were unavailable. `SellerPublishView.onMounted` called `marketStore.loadMyShop()` → `GET /api/shops/mine`, which failed with a connection error. The `.catch(() => {})` swallowed the error silently, but no real data reached the page.

2. **Categories never loaded from API** (frontend data dependency gap). `loadProducts()` in the market store did not update `categories` — it only set `products`, `searchTotal`, `searchPage`, `searchPageSize`. The sole place `categories.value` was set was inside `loadProductDetail()`, which is not called from the publish page.

3. **Stale DEFAULT_CATEGORIES guard** (frontend state bug). `SellerPublishView.onMounted` guarded the `loadProducts()` call with `if (!marketStore.categories.length)`. Since `categories` starts with 4 hardcoded `DEFAULT_CATEGORIES`, the condition was always false — `loadProducts()` was never called from this page, and categories permanently showed the hardcoded defaults regardless of actual database categories.

**Frontend vs. backend separation**: `SellerPublishView.vue` itself has no `loading` ref, no `v-loading` directive, and no conditional rendering on loading state — it renders unconditionally on mount. The page could not "hang on loading" from its own component code. The symptom was the page rendering with non-functional defaults while all API calls silently failed.

### Changes

| File | Change |
|------|--------|
| `backend/…/mapper/review/impl/JdbcReviewMapper.java` | Added missing `findOrderContextByOrderItemId` implementation (JOINs `order_items` with `orders` to resolve buyerUserId, orderStatus, productId) |
| `frontend/src/stores/market.js` | `loadProducts()` now derives `categories` from loaded products via `categoriesFromProducts()` after each successful fetch |
| `frontend/src/views/app/SellerPublishView.vue` | Removed the `!marketStore.categories.length` guard in `onMounted` — `loadProducts()` always runs to refresh categories from the API |
| `frontend/src/stores/__tests__/market.test.js` | Updated test assertion: `loadProducts` now populates categories from loaded data instead of preserving hardcoded defaults |

### Verification

- Backend: `mvnw.cmd test` — **80 tests, 0 failures, 0 errors**
- Frontend: `npm test` — **30 tests, 0 failures** (7 test files)
- Frontend: `npm run build` — **successful production build**
- No API contract changes — no `docs/06-http/` or `docs/09-api-spec/` updates needed
