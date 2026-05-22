# Task: Explore Infinite Scroll And Bookmark Rail

## Metadata

- ID: explore-infinite-scroll-bookmark-ux
- Status: archived
- Owner: unassigned
- Track: feature
- Depends on: archived `explore-page-ux-redesign`, current `ProductListView.vue`, existing product list API pagination
- Priority: high
- Planned date: 2026-05-22
- Completed date: 2026-05-22

## Objective

Replace the explore page's explicit pagination with an infinite browsing flow that lazy-loads more product pages as the user scrolls, and add a desktop-side custom progress/bookmark rail that lets the user double-click to save a reading position and click the saved marker to return there.

The implementation should preserve the current filter/search UX, avoid backend or API contract changes, and keep the data-layer change surface as small as possible.

## Scope

### In scope

- `frontend/src/views/app/ProductListView.vue`
  - remove visible pagination control
  - accumulate paged API results locally for infinite loading
  - keep existing route-driven filter/query behavior for keyword/category/type
  - add loading-more state, end-of-list state, and sentinel-triggered fetch
  - add a sticky side rail for progress + bookmark restore
- `CHANGELOG.md`
- this task document lifecycle

### Out of scope

- backend changes
- API module changes
- store contract redesign unless absolutely required
- changing `ExploreSearchShell.vue`
- changing `ExploreProductCard.vue`
- changing routing structure

## Constraints

- Prefer keeping `marketStore.loadProducts()` unchanged and handle page accumulation in the view.
- Do not add new npm packages.
- Do not introduce fake data.
- Keep loading/error/empty states intact.
- Mobile should remain usable; the side rail may be hidden on smaller screens if needed.

## Implementation Notes

1. Use local view state to accumulate loaded product pages.
2. Keep filters query-driven, but treat page advancement as an internal browsing concern instead of URL state.
3. Use an `IntersectionObserver` sentinel for lazy loading the next page.
4. Add a sticky/fixed desktop rail that shows:
   - browse progress
   - saved bookmark marker when one exists
   - hint text for double-click save / click restore
5. Persist the bookmark locally so it survives navigation back into the page.

## Test Plan

```bash
cd frontend && npm test
cd frontend && npm run build
```

## Completion Notes

- Replaced explore-page pagination with a view-local infinite loading flow that reuses the existing paged API and accumulates cards client-side.
- Added a sticky desktop bookmark rail that supports double-click save and click-to-restore for long browsing sessions, including restoring deeper positions by loading intermediate pages first.
- Verified the slice with `cd frontend && npm test`, `cd frontend && npm run build`, and a browser check against `http://127.0.0.1:4173/app/explore`.
