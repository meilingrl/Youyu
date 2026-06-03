# Task: Search Engine Upgrade With Meilisearch

## Metadata

- ID: search-engine-upgrade-meilisearch
- Status: completed
- Owner: worker-search
- Track: cross-cutting
- Depends on: feature-polish-explore-filter-search-sort
- Priority: P0
- Planned date: 2026-06-03
- Completed date: 2026-06-03

## Objective

Upgrade product search from SQL-only fuzzy substring matching to a Meilisearch-backed product search index while keeping MySQL as the business source of truth.

## Background

The current product list contract supports `keyword`, `categoryId`, `productType`, `sort`, `page`, and `pageSize`. `keyword` currently uses fuzzy substring matching across product title, subtitle, description, and category name.

That behavior is acceptable for small data, but it cannot provide mature search behavior such as typo tolerance, better relevance ranking, scalable suggestions, or future synonym/pinyin support. The intended architecture is:

`MySQL = source of truth`

`Meilisearch = product search index`

Search requests should query Meilisearch first when it is configured and healthy, receive ordered product IDs, then fetch full product-card data from MySQL. If Meilisearch is disabled or unavailable in local/test mode, the existing MySQL search path must continue to work.

## Scope

- Add a backend Meilisearch integration for public product search.
- Keep the existing `GET /api/products` response shape stable for the frontend.
- Keep MySQL as the only source of truth for product detail, price, stock, visibility, order, cart, and payment logic.
- Index only search-safe product fields needed for matching, filtering, and sorting.
- Add configuration flags and environment variables for Meilisearch host/key/index behavior.
- Add a reindex path suitable for local/dev/admin operation.
- Add fallback behavior when Meilisearch is disabled or unreachable.
- Update product/search API docs and HTTP validation samples.

## Out of Scope

- Replacing MySQL product storage.
- Searching orders, users, chats, reports, or admin audit logs through Meilisearch.
- Elasticsearch/OpenSearch migration.
- Production synonym, pinyin, semantic search, or personalized ranking.
- Making Meilisearch required for backend startup in test/local mode.
- Storing full business payloads or sensitive user/order data in Meilisearch.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/04-standards/development-process.md`
- `docs/09-api-spec/product.md`
- `docs/09-api-spec/search.md`
- `docs/06-http/product.http`
- `frontend/src/views/app/ProductListView.vue`
- `frontend/src/stores/market.js`
- `frontend/src/stores/search.js`
- `frontend/src/api/modules/product.js`
- `backend/pom.xml`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-test.yml`
- `backend/src/main/java/com/youyu/backend/controller/product/ProductController.java`
- `backend/src/main/java/com/youyu/backend/controller/search/SearchController.java`
- `backend/src/main/java/com/youyu/backend/service/product/ProductService.java`
- `backend/src/main/java/com/youyu/backend/service/product/impl/ProductServiceImpl.java`
- `backend/src/main/java/com/youyu/backend/mapper/product/ProductMapper.java`
- `backend/src/main/java/com/youyu/backend/mapper/product/impl/JdbcProductMapper.java`
- `backend/src/main/java/com/youyu/backend/service/search/SearchService.java`
- `backend/src/main/java/com/youyu/backend/service/search/impl/SearchServiceImpl.java`

## Allowed Changes

- Backend product/search service, mapper, controller, config, and test files directly needed for the Meilisearch integration.
- Backend dependency/config files for a Meilisearch Java client or narrow HTTP client integration.
- Add new backend package/classes under `service/search`, `config`, or a clearly named search integration package.
- `frontend/src/views/app/ProductListView.vue`, `frontend/src/stores/market.js`, `frontend/src/stores/search.js`, and product API wrappers only if the existing API contract needs a small display or parameter adjustment.
- `docs/09-api-spec/product.md`
- `docs/09-api-spec/search.md`
- `docs/06-http/product.http`
- Focused backend/frontend tests for product search behavior.

Do not modify order, payment, logistics, map, admin export, or unrelated UI files.

## Implementation Plan

1. Inspect current product list SQL behavior and lock the current API response shape before changing implementation.
2. Add Meilisearch configuration with an explicit enable flag, host, API key, and product index name. Default test/local behavior must not require a running Meilisearch service.
3. Define indexed product document fields:
   - `productId`
   - `title`
   - `subtitle`
   - `description`
   - `categoryId`
   - `categoryName`
   - `productType`
   - `status`
   - `salePrice`
   - `favoriteCount`
   - `viewCount`
   - `createdAt`
   - seller/shop identifiers only if already public in product cards
4. Implement indexing/reindexing:
   - update index after product publish/update/status/delete
   - provide a full reindex method/endpoint or startup-safe admin operation
   - never index passwords, student IDs, addresses, orders, or payment data
5. Implement search execution:
   - query Meilisearch when enabled and keyword/filter/sort criteria require product search
   - use Meilisearch filters and sort allowlists
   - fetch complete product cards from MySQL by ordered product IDs
   - preserve pagination metadata
6. Keep a MySQL fallback for disabled/unhealthy Meilisearch and document fallback behavior.
7. Update docs, `.http` samples, and focused tests.

## Risks

- Product search can return stale results if indexing is not updated after product mutations.
- Fetching MySQL rows by ID can lose Meilisearch relevance order unless ordering is restored explicitly.
- Raw sort/filter values must stay allowlisted.
- Search index fields can accidentally include sensitive data if the document builder is too broad.
- Tests must not depend on an external Meilisearch service unless explicitly marked integration-only.

## Test Plan

- Backend: `cd backend; .\mvnw.cmd test`
- Frontend: `cd frontend; npm test`
- Frontend build: `cd frontend; npm run build`
- API validation:
  - product list with keyword typo/partial term
  - category filter
  - product type filter
  - `newest`, `price_asc`, `price_desc`, `sales_desc`
  - disabled Meilisearch fallback
- Manual:
  - start backend with Meilisearch disabled and verify existing product search still works
  - start backend with Meilisearch enabled and verify `/app/explore` search returns ordered product cards from MySQL
  - publish/update/off-sale a product and verify index behavior or reindex recovery

## Acceptance Criteria

- [x] `GET /api/products` keeps its existing response shape and remains usable without Meilisearch.
- [x] When enabled, product keyword search queries Meilisearch and then fetches full product data from MySQL.
- [x] MySQL remains the source of truth for product detail, price, stock, visibility, order, cart, and payment behavior.
- [x] Indexed fields are limited to search-safe public product fields.
- [x] Sort/filter values are allowlisted and do not interpolate raw user input into SQL or search filters.
- [x] Product publish/update/status/delete either updates the index or has a documented full-reindex recovery path.
- [x] Tests cover fallback behavior and ordered MySQL hydration from search result IDs.
- [x] Product/search API docs and product `.http` examples are updated.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] `docs/06-http/product.http`
- [x] `docs/09-api-spec/product.md`
- [x] `docs/09-api-spec/search.md` if search-domain endpoints or semantics change
- [x] task status and archive move

## Completion Notes

- Added optional Meilisearch-backed product search behind `youyu.search.meilisearch.enabled`, defaulting disabled for local/test-safe fallback.
- Kept `GET /api/products` response shape stable while hydrating full product-card rows from MySQL by ordered search-result product IDs.
- Added admin full-reindex endpoint `POST /api/admin/search/products/reindex`.
- Added product index sync/removal for seller publish/update/status/delete, admin status/review changes, and favorite-count changes.
- Updated product/search API docs, product HTTP smoke samples, and changelog.
- Verification:
  - `cd backend; .\mvnw.cmd test -Dtest=ProductSearchIntegrationTest`
  - `cd backend; .\mvnw.cmd test -Dtest=FavoritesIntegrationTest`
  - Main-agent verification: `cd backend; .\mvnw.cmd test` passed, 226 tests.
  - Main-agent verification: `cd frontend; npm test` passed, 63 tests.
  - Main-agent verification: `cd frontend; npm run build` passed.
