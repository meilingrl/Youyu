# Task: API Spec Standardization Follow-up (Recommend & Shop)

## Metadata

- ID: docs-api-spec-standardization-follow-up
- Status: archived
- Owner: unassigned
- Track: cross-cutting
- Depends on: current module specs in `docs/09-api-spec/`
- Priority: medium
- Planned date: 2026-05-23
- Completed date: 2026-05-23

## Objective

Add formal API specs for the two modules still missing from `docs/09-api-spec/`: **`recommend`** and **`shop`**.

## Background

`docs/09-api-spec/` currently covers: `admin`, `auth`, `order`, `product`, `report`, `review`, `search`, `user`. The repository exposes additional public/authenticated endpoints under `recommend/` and `shop/` controllers with no formal contract document. The `.http` collection has `recommend.http`, and the public shop examples currently live inside `product.http`; there is no dedicated `shop.http`.

## Pre-flight Verification (must complete before writing)

1. `ls docs/09-api-spec/` — confirm `recommend.md` and `shop.md` are both absent
2. `ls docs/06-http/` — confirm `recommend.http` is present and check whether `shop.http` exists (it likely does not)
3. Read `docs/09-api-spec/API_SPEC_TEMPLATE.md`
4. Read `docs/09-api-spec/product.md` and `docs/09-api-spec/search.md` as the closest style references (similar in being public-leaning modules)
5. `grep -n "^\s*@\(Get\|Post\|Put\|Delete\|Patch\)Mapping\|@LoginRequired" backend/src/main/java/com/youyu/backend/controller/product/RecommendController.java`
6. `grep -n "^\s*@\(Get\|Post\|Put\|Delete\|Patch\)Mapping\|@LoginRequired" backend/src/main/java/com/youyu/backend/controller/shop/ShopController.java`
7. Read each service implementation to capture response field shapes:
   - `backend/src/main/java/com/youyu/backend/service/recommend/impl/RecommendServiceImpl.java`
   - `backend/src/main/java/com/youyu/backend/service/shop/impl/ShopServiceImpl.java`

## Files to Read

- `AGENTS.md`, `CLAUDE.md`, `docs/README.md`
- `docs/04-standards/development-process.md`
- `docs/09-api-spec/README.md`
- `docs/09-api-spec/API_SPEC_TEMPLATE.md`
- `docs/09-api-spec/product.md`, `docs/09-api-spec/search.md`, `docs/09-api-spec/user.md`
- `docs/06-http/recommend.http`
- `docs/06-http/shop.http` (if present; create from scratch if not)
- Controllers, services, and mappers for `recommend` and `shop` (read only; no edits)
- `backend/src/main/java/com/youyu/backend/common/api/ResultCode.java`

## In Scope

Produce three or four artifacts depending on `shop.http` existence:

1. `docs/09-api-spec/recommend.md` (new)
2. `docs/09-api-spec/shop.md` (new)
3. `docs/06-http/shop.http` (new — only if not already present)
4. Update `docs/09-api-spec/README.md` module index to include `recommend` and `shop`

## Out of Scope

- already completed specs for `report`, `review`, `search`, `admin`, `auth`, `order`, `product`, and `user`
- Backend code changes
- Endpoint redesign
- Adding OpenAPI / Swagger tooling
- Refactoring existing specs

## Hard Limits

- **Do not** edit any file under `backend/src/`
- **Do not** add Swagger / OpenAPI plugins or dependencies
- **Do not** change the response envelope or error semantics across modules
- **Do not** combine `recommend` and `shop` into one spec file — each gets its own module spec
- **Do not** rewrite `recommend.http` unless drift is found; the task is additive

## Allowed Changes

- `docs/09-api-spec/recommend.md` (new)
- `docs/09-api-spec/shop.md` (new)
- `docs/06-http/shop.http` (new, if not present)
- `docs/06-http/recommend.http` (drift fix only)
- `docs/09-api-spec/README.md` (module index update)
- `CHANGELOG.md`
- `docs/08-tasks/active/api-spec-standardization-follow-up.md` → move to `archived/` after both modules done

## Implementation Steps

1. Complete pre-flight; record endpoint inventories for both modules.
2. Draft `docs/09-api-spec/recommend.md`:
   - Module overview (1 paragraph): describes hybrid cold-start / personalized / also-bought strategy as exposed
   - Per-endpoint sections matching `API_SPEC_TEMPLATE.md` (method, path, auth, request, response data shape, errors, `.http` line range)
3. Audit `docs/06-http/recommend.http` against the inventory; patch any method/path/auth drift in place.
4. Inventory `ShopController` endpoints. Decide whether `shop.http` already exists; if not, create it with one block per endpoint following the format of `recommend.http`.
5. Draft `docs/09-api-spec/shop.md` per template.
6. Update `docs/09-api-spec/README.md` to list `recommend` and `shop` (alphabetical position).
7. Prepend `CHANGELOG.md` block describing both new specs.
8. Move this task from `active/` to `archived/` with `Status: archived`, `Completed date`, `Delivered` section.

## Test Plan

- Backend: not required
- Frontend: not required
- API validation:
  - For each module, every path in the new spec must be reachable from the corresponding controller (`grep` verification)
  - For each module, every `.http` block must correspond to an endpoint listed in the spec
- Manual: read each new spec end-to-end; each endpoint section answers what a frontend dev needs to know to make the call

## Acceptance Criteria

- [x] `docs/09-api-spec/recommend.md` exists and follows the template
- [x] `docs/09-api-spec/shop.md` exists and follows the template
- [x] Every endpoint in `RecommendController` is documented in `recommend.md`
- [x] Every endpoint in `ShopController` is documented in `shop.md`
- [x] Each endpoint section names: method, path, auth (role if applicable), request fields, response data shape, error codes, `.http` line range
- [x] `docs/06-http/shop.http` exists (created if absent)
- [x] `docs/06-http/recommend.http` has no controller drift (patched if needed)
- [x] `docs/09-api-spec/README.md` lists `recommend` and `shop`
- [x] `CHANGELOG.md` block added
- [x] This task moved to `archived/`

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/` (recommend.http audit; shop.http possibly new)
- [x] `docs/09-api-spec/README.md`
- [x] task status and archive move

## Final Report Format

```markdown
## Return Report — docs-api-spec-standardization-follow-up

### A. Branch & Commit
- Branch: <name>
- Commit SHA: <sha>
- Files changed (paste `git diff --stat`)

### B. Pre-flight Findings
- recommend.md absent: <confirmed yes/no>
- shop.md absent: <confirmed yes/no>
- shop.http present before this task: <yes/no>
- RecommendController endpoints:
  - <METHOD PATH — auth>
  - ...
- ShopController endpoints:
  - <METHOD PATH — auth>
  - ...
- Recommend.http drift found: <list or "none">

### C. Implementation Walkthrough
- Step 2 → recommend.md drafted (lines: <N>, endpoints documented: <N>)
- Step 3 → recommend.http drift fix: <list of patched lines or "none">
- Step 4 → shop.http created from scratch: <yes/no>, line count: <N>
- Step 5 → shop.md drafted (lines: <N>, endpoints documented: <N>)
- Step 6 → README.md module index updated (paste resulting lines listing recommend and shop)
- Step 7 → CHANGELOG block added (paste)
- Step 8 → task moved to archived

### D. Test Plan Results
- Recommend: spec endpoints vs controller endpoints — <match? evidence>
- Shop: spec endpoints vs controller endpoints — <match? evidence>
- Recommend.http blocks vs spec endpoints — <match? evidence>
- Shop.http blocks vs spec endpoints — <match? evidence>

### E. Acceptance Criteria Check
- [x/✗] one per criterion with evidence

### F. Deviations from Spec
- "none" or specific deviation with reason

### G. Out-of-scope Findings
- "none" or specific items (e.g., drift found in unrelated specs — do not fix here, just report)

### H. Open Questions / Blockers
- "none" or single question
```

## Completion Notes

## Delivered

- Created `docs/09-api-spec/recommend.md` (191 lines), documenting 2 endpoints:
  - `GET /api/recommend/home`
  - `GET /api/recommend/also-bought/{productId}`
- Created `docs/09-api-spec/shop.md` (391 lines), documenting 7 `ShopController` endpoints:
  - `GET /api/shops/skeleton`
  - `GET /api/shops/mine`
  - `POST /api/shops/applications`
  - `GET /api/shops/{shopId}`
  - `GET /api/shops/{shopId}/insight-snapshot`
  - `GET /api/shops/{shopId}/reviews`
  - `GET /api/shops/{shopId}/review-summary`
- Created `docs/06-http/shop.http` (67 lines).
- Audited `docs/06-http/recommend.http`; no method/path/auth drift found against `RecommendController`.
- Updated `docs/09-api-spec/README.md` and `docs/README.md` module indexes.
- Updated current roadmap wording so formal API spec coverage is treated as current-module complete, with future maintenance deferred until UI/UX decisions create endpoint changes.
- Prepended a `CHANGELOG.md` entry.

## Return Report — docs-api-spec-standardization-follow-up

### A. Branch & Commit
- Branch: `feat/ux-airbnb-search-shell`
- Commit SHA: `9fbc678`
- Files changed: see final `git diff --stat`; this task intentionally touched API/docs/task/changelog files only.

### B. Pre-flight Findings
- `recommend.md` absent: confirmed yes
- `shop.md` absent: confirmed yes
- `shop.http` present before this task: no
- RecommendController endpoints:
  - `GET /api/recommend/home` — public, optional Bearer token can personalize
  - `GET /api/recommend/also-bought/{productId}` — public
- ShopController endpoints:
  - `GET /api/shops/skeleton` — public
  - `GET /api/shops/mine` — USER role required
  - `POST /api/shops/applications` — USER role required
  - `GET /api/shops/{shopId}` — public
  - `GET /api/shops/{shopId}/insight-snapshot` — public
  - `GET /api/shops/{shopId}/reviews` — public
  - `GET /api/shops/{shopId}/review-summary` — public
- Recommend.http drift found: none

### C. Implementation Walkthrough
- Step 2 -> `recommend.md` drafted (lines: 191, endpoints documented: 2)
- Step 3 -> `recommend.http` drift fix: none
- Step 4 -> `shop.http` created from scratch: yes, line count: 67
- Step 5 -> `shop.md` drafted (lines: 391, endpoints documented: 7)
- Step 6 -> README module indexes updated to list `recommend.md` and `shop.md`
- Step 7 -> CHANGELOG block added
- Step 8 -> task moved to archived

### D. Test Plan Results
- Recommend: spec endpoints vs controller endpoints — match (`/home`, `/also-bought/{productId}`)
- Shop: spec endpoints vs controller endpoints — match (`/skeleton`, `/mine`, `/applications`, `/{shopId}`, `/{shopId}/insight-snapshot`, `/{shopId}/reviews`, `/{shopId}/review-summary`)
- Recommend.http blocks vs spec endpoints — match; all request paths map to the two controller endpoints
- Shop.http blocks vs spec endpoints — match; all request paths map to the seven documented endpoints

### E. Acceptance Criteria Check
- [x] `docs/09-api-spec/recommend.md` exists and follows the template
- [x] `docs/09-api-spec/shop.md` exists and follows the template
- [x] Every endpoint in `RecommendController` is documented in `recommend.md`
- [x] Every endpoint in `ShopController` is documented in `shop.md`
- [x] Each endpoint section names method, path, auth, request fields, response data shape, error cases, and `.http` mapping
- [x] `docs/06-http/shop.http` exists
- [x] `docs/06-http/recommend.http` has no controller drift
- [x] `docs/09-api-spec/README.md` lists `recommend` and `shop`
- [x] `CHANGELOG.md` block added
- [x] This task moved to `archived/`

### F. Deviations from Spec
- Added `docs/README.md` module-index update so the top-level documentation index does not become stale.
- Updated current roadmap wording because completing this task changes API-spec status from in-progress to current-module complete.

### G. Out-of-scope Findings
- `docs/06-http/product.http` still contains a legacy `GET /api/shops/{shopId}/products` example. Current runtime does not expose that route; shop products are returned inside `GET /api/shops/{shopId}`. This task recorded the drift in `shop.md` but did not edit `product.http` because it was outside the allowed write scope.

### H. Open Questions / Blockers
- none
