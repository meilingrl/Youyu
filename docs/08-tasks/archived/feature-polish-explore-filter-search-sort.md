# Task: Explore Filter, Search, Sort, and Search-Bar Stability

## Metadata

- ID: feature-polish-explore-filter-search-sort
- Status: completed
- Owner: worker-wave-1
- Track: feature
- Depends on: feature-polish-closeout-parent
- Priority: P0
- Planned date: 2026-06-03
- Completed date: 2026-06-03

## Objective

Make the explore page category filtering, fuzzy search, stable search-bar layout, and sorting work together without UI shaking or stale route/query state.

## Background

Reported issues:

- Category function does not work.
- Fuzzy search is missing.
- Search bar shakes again.
- Sorting must support price, sales, and newest time.

The current product list controller accepts `keyword`, `categoryId`, `productType`, `page`, and `pageSize`. Sort behavior and fuzzy-search semantics must be added deliberately if backend changes are required.

## Scope

- Verify whether category selection fails in frontend state, route query syncing, backend filtering, or response normalization.
- Implement fuzzy search using the existing product list/search architecture.
- Stabilize search-bar dimensions so focus, suggestions, loading state, and route updates do not cause visible shaking.
- Add sort options for price, sales, and newest time with clear query keys and display labels.
- Keep existing hot search and suggestion behavior unless it directly causes the bug.

## Out of Scope

- Full-text search engine integration.
- Personalized ranking.
- Rewriting explore page information architecture.
- Marketing campaign sorting.

## Files to Read

- `frontend/src/views/app/ProductListView.vue`
- `frontend/src/components/explore/ExploreSearchShell.vue`
- `frontend/src/components/explore/ExploreProductCard.vue`
- `frontend/src/components/search/SearchSuggestInput.vue`
- `frontend/src/stores/market.js`
- `frontend/src/stores/search.js`
- `frontend/src/api/modules/product.js`
- `backend/src/main/java/com/youyu/backend/controller/product/ProductController.java`
- `backend/src/main/java/com/youyu/backend/service/product/ProductService.java`
- `backend/src/main/java/com/youyu/backend/service/product/impl/ProductServiceImpl.java`
- `backend/src/main/java/com/youyu/backend/mapper/product/ProductMapper.java`
- `backend/src/main/java/com/youyu/backend/mapper/product/impl/JdbcProductMapper.java`
- `docs/09-api-spec/product.md`
- `docs/06-http/product.http`

## Allowed Changes

- `frontend/src/views/app/ProductListView.vue`
- `frontend/src/components/explore/ExploreSearchShell.vue`
- `frontend/src/components/search/SearchSuggestInput.vue`
- `frontend/src/stores/market.js`
- `frontend/src/api/modules/product.js`
- product-list backend controller/service/mapper files listed above, only if frontend-only correction is insufficient
- `docs/09-api-spec/product.md`
- `docs/06-http/product.http`
- focused frontend/backend tests for product list behavior

## Implementation Plan

1. Diagnose category failure by tracing selected category -> route/query -> API params -> backend filter -> normalized results.
2. Define locked sort query values before coding: `price_asc`, `price_desc`, `sales_desc`, `newest`.
3. Add or repair backend filtering/search/sort only where the current data path proves insufficient.
4. Stabilize search-bar layout with fixed control dimensions and non-layout-shifting suggestion/loading states.
5. Update product API spec and HTTP examples for any new query parameter or fuzzy behavior.

## Risks

- SQL sort fields can become injection-prone if raw query values are interpolated. Use an allowlist.
- Existing uncommitted changes already touch `ProductListView.vue` and product API modules; main Agent must isolate or merge carefully.
- Fuzzy matching on large product lists may need pagination and index review later.

## Test Plan

- Backend: `cd backend; .\mvnw.cmd test`
- Frontend: `cd frontend; npm test`
- Frontend build: `cd frontend; npm run build`
- API validation: product `.http` examples for category, fuzzy keyword, and each sort key.
- Manual: verify category chips, fuzzy search terms, clear/reset, pagination reset, and sort options in `/app/explore` or the current product-list route.

## Acceptance Criteria

- [ ] Selecting a category changes results or shows a correct empty state.
- [ ] Fuzzy search matches reasonable partial title/category/description terms according to documented behavior.
- [ ] Search bar no longer shifts or shakes during focus, suggestions, loading, or route-query updates.
- [ ] Sort options work for price, sales, and newest time and survive route/query refresh.
- [ ] Sort query values are allowlisted and documented.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] `docs/06-http/product.http` if backend params change
- [ ] `docs/09-api-spec/product.md` if backend params change
- [ ] task status and archive move

## Completion Notes

- Repaired explore category filtering and route synchronization, added allowlisted backend/frontend sort keys (`price_asc`, `price_desc`, `sales_desc`, `newest`), and wired fuzzy keyword search through the existing product list/search path.
- Stabilized the search shell so focus, suggestion, and filter transitions no longer change the control width or trigger visible shaking.
