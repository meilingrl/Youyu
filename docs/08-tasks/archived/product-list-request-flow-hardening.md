# Task: Product List Request Flow Hardening

## Metadata

- ID: product-list-request-flow-hardening
- Status: archived
- Owner: meilingrl
- Track: feature
- Depends on: architecture-performance-hardening
- Priority: medium
- Planned date: 2026-05-17
- Completed date: 2026-05-17

## Objective

Remove avoidable extra requests and unstable metadata coupling from the storefront product-list page while preserving the current route-driven search experience, suggestion behavior, and existing UI semantics.

## Background

The current storefront product-list page uses an initial bootstrap request to derive category options before the real route-driven request runs. This makes the page work, but it introduces a subtle architectural issue:

- category metadata is being inferred from whichever product page happened to load
- the first screen may issue two requests instead of one
- the global store mixes stable metadata with page-scoped result data

At the current stage, this is not the most severe backend scalability problem, but it is a good candidate for a small, isolated cleanup with immediate UX and maintainability benefit.

This task intentionally stays frontend-focused so it can run in parallel with backend hardening work.

## Scope

### In Scope

- `frontend/src/views/app/ProductListView.vue`
- `frontend/src/stores/market.js`
- any directly related storefront API or utility code required to support a cleaner load path
- route-driven search/list behavior for:
  - keyword
  - category
  - product type
  - page
- loading, empty, and error state behavior on the product list page

### In Scope But Only If Needed

- introducing a clearer source of category metadata if one already exists or can be derived without broad backend work
- small store refactors that separate stable metadata from page-scoped results

## Out of Scope

- backend search semantics changes
- product search SQL changes
- schema/index changes
- admin pages
- hot-search ranking redesign
- broad visual redesign of the storefront

## Files to Read

- `frontend/README.md`
- `frontend/src/views/app/ProductListView.vue`
- `frontend/src/stores/market.js`
- `frontend/src/stores/search.js`
- related product API module files under `frontend/src/api/`
- any existing tests covering market/search stores or product list route behavior

## Allowed Changes

- `frontend/src/views/app/ProductListView.vue`
- `frontend/src/stores/market.js`
- directly related storefront API or utility files if necessary
- focused frontend tests for product list/store behavior
- `CHANGELOG.md`

## Parallelization Boundary

This task is designed to be parallel-safe with:

- `admin-query-pagination-hardening`
- `review-order-lookup-hardening`
- `runtime-index-hardening`

### Do Not Touch In This Task

- `backend/**`
- `docs/06-http/admin.http`
- admin frontend pages
- review/order backend services

If this task appears to require a backend category endpoint or search-contract change, stop and open a coordination note instead of silently expanding scope.

## Required Behavioral Constraints

- the page must remain route-driven
- keyword submission and suggestion selection must still update the route
- search history behavior must remain intact
- empty/loading/error states must remain explicit
- mobile usability must not regress

## Implementation Plan

1. Trace the current request sequence.
   - confirm where the bootstrap fetch happens
   - identify which data are truly page-scoped and which are stable metadata

2. Define a cleaner metadata strategy.
   - avoid deriving stable category options from the current visible result set if possible
   - keep the solution small and consistent with existing architecture

3. Remove the avoidable double-fetch path.
   - first page entry should perform one intentional load flow
   - route changes should still trigger predictable result refresh

4. Simplify state responsibilities.
   - if needed, separate category metadata from current product result state
   - do not broaden this into a general store rewrite

5. Verify route and UI behavior.
   - keyword change
   - chip filters
   - page switching
   - clear filters
   - suggestion selection

## Risks

- accidentally changing route synchronization behavior
- breaking category-chip rendering on first load
- causing stale product/store state across other storefront pages
- expanding into backend search work and colliding with another task

## Test Plan

- Backend:
  - no backend changes expected
- Frontend:
  - add or update tests for product-list initial load and route-driven refresh
  - verify only the intended request sequence occurs
- API validation:
  - no API contract changes expected unless explicitly coordinated
- Manual:
  - open product list page directly
  - refresh with query params present
  - switch category and product type chips
  - paginate
  - submit and clear keyword filters

## Acceptance Criteria

- [x] Product list page no longer performs an avoidable bootstrap fetch before the real route-driven request
- [x] Category metadata is no longer coupled to whichever page of product results happened to load first
- [x] Existing route-driven list behavior remains intact
- [x] No backend, schema, or admin files were modified in this task

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes

This task is intentionally storefront-only. It exists as a safe parallel slice so one agent can improve the user-facing request flow without touching backend query, schema, or admin code.

Completed outcome:

- Removed the initial bootstrap fetch so product list first load now issues one intentional route-driven request
- Decoupled storefront category chips from the current result set by making `DEFAULT_CATEGORIES` the stable category source
- Preserved route-driven filtering, suggestion selection, and existing build/test compatibility
