## [2026-05-21] - Documentation cleanup and README overhaul post-migration

### docs
- `CLAUDE.md`: expanded backend directory tree to sub-package level; expanded frontend `components/` to reflect actual subdirectories (`explore/`, `shell/`, `trade/`); corrected `/app/*` route count from 16 to 19 and `/admin/*` from 9 to 10; added route guard detail (`public` meta, `?redirect=` behavior); expanded backend Key Conventions (exception types, new module checklist, `@LoginRequired` usage, `ApiResponse` return pattern); expanded frontend Key Conventions (normalization boundary, error-utils, Element Plus constraint, route meta shape); updated `docs/06-http/` and `docs/09-api-spec/` module lists to reflect actual files; added frontend unit test step to post-task checklist
- `backend/README.md`: rewritten from early scaffold description to current state — MySQL setup, full command reference, seed data layout, auth/mock token usage
- `frontend/README.md`: rewritten from scaffold stub to current directory structure, component inventory, and key conventions
- `tests/README.md`: replaced placeholder with actual test distribution (backend `src/test/`, frontend `__tests__/`, E2E `tests/e2e/`)
- `docs/README.md`: updated `09-api-spec` module list from 3 to 7 entries; removed deleted `ui-improvement-spec` from `03-architecture` description
- `migration-notes/README.md`: added new file explaining the directory is pre-migration historical archive with broken paths — not current reference material

### removed
- `docs/03-architecture/ui-improvement-spec.md`: deleted — file encoding was corrupted (GBK/UTF-8 mismatch), content unreadable by agents; described UI improvements that have since been implemented

### archived
- `docs/08-tasks/drafts/issue.md` → `archived/`: raw bug/issue notes, already decomposed into concrete active tasks
- `docs/08-tasks/active/execution-wave-plan-2026-05-20.md` → `archived/`: planning meta-task, wave breakdown already executed
- `docs/08-tasks/active/agent-prompts-wave-plan-2026-05-20.md` → `archived/`: agent dispatch prompts for the same wave, no longer active

---

## [2026-05-20] - Seller publish loading diagnosis and fix

### fix
- Backend: Added missing `findOrderContextByOrderItemId` implementation in `JdbcReviewMapper` (backend wouldn't compile — root cause of all API unavailability)
- Frontend (market store): `loadProducts()` now derives `categories` from loaded product data via `categoriesFromProducts()`, keeping categories in sync with the API
- Frontend (SellerPublishView): Removed stale `!marketStore.categories.length` guard that prevented `loadProducts()` from ever running on the publish page (guard was always false due to hardcoded DEFAULT_CATEGORIES)

### diagnosis
- `SellerPublishView.vue` has no component-level loading state — it renders unconditionally; the "loading hang" was the page showing stale defaults while API calls silently failed
- Separated three converging causes: backend compilation error (server-side), `loadProducts` not updating categories (data gap), and guard preventing API call (frontend state)

---

## [2026-05-19] - Comment completeness sprint (W1-W8)

### docs
- Added 48 WHY-type comments across 5 backend service/mapper files: ReviewServiceImpl (denormalized rating cache, defensive JDBC unwrap, MVP distribution TODO), JdbcReviewMapper (anti-join semantics, COALESCE NULL handling, LEFT JOIN graceful degradation), PaymentServiceImpl (state machine constraints, idempotency guards, dual-state sync, fulfillment branching), OrderServiceImpl (25 locations — order lifecycle state machine, address snapshot, offline double-confirm, digital asset access timeline, action builder contract, refund policy, ensureProductPurchasable conditions), TransactionDataStore (PersistentMap auto-persist trap, serial number format, seed data dev-only warning, silent JSON degradation)
- Added 50 JSDoc blocks across 8 frontend files: all exported functions in stores (market 24, review 8, recommend 2, auth 4) and utils (market-normalizers 4, auth 4, storage 3, error-utils 1) documented with @param / @returns / @sideEffects
- Added `docs/08-tasks/active/comment-completeness-sprint.md` — parallel work item spec for the sprint

### principle
- Comments explain WHY, not WHAT — design intent, non-obvious constraints, business rules, SQL gotchas
- Self-documenting code left as-is; only non-obvious behavior documented
- Pure comment changes, zero logic modifications

---

## [2026-05-19] - UI follow-ups and brand asset organization

### fix
- Restored UTF-8 Chinese copy in `ProfileView.vue` (encoding corruption replaced labels with `?`)
- Fixed Mojibake navigation labels in `CHANGELOG.md` (首页 / 探索 / 消息 / 交易 / 我的)

### docs
- Added the shell navigation foundation draft and organized brand asset guidance under `resources/assets/brand/`

---

## [2026-05-19] - UI/UX copy polish: replace meta-descriptive text with user-facing content

### changed
- HomeView: hero title, description, section titles, trust items, guide cards all rewritten from design-document language to natural product copy
- TradeView: rewritten metrics, description, and overview cards from implementation jargon to user-relevant navigation
- OrdersView: metrics helper text and page description updated to user language
- CartView: page description and empty state text updated
- MessagesView: placeholder conversations rewritten with realistic chat content; group descriptions, input area hints, and section titles humanized
- ProfileView: completely rewritten to a clean identity + stats + quick-links structure without reserved-metric components
- SettingsView: developer badges ("已迁移结构", "复用偏好页", "待后端字段") replaced with user-friendly labels; alert descriptions humanized
- PreferenceSettingsView: three PageSection descriptions rewritten
- ProductDetailView: three PageSection descriptions and ElMessage texts updated
- ShopView: six PageSection descriptions, button labels ("预留" removed), formatMoney/formatMetric fallback changed from "待接入" to "--"
- SellerProductsView: page description rewritten
- SellerPublishView: page description rewritten
- PendingReviewsView: page description rewritten

### principle
- All user-visible text must serve the user, not explain design decisions
- "待接入" replaced with "--" for data placeholders
- "(预留)" removed from disabled button labels
- Developer-facing status tags in admin views intentionally kept as-is

---

## [2026-05-19] - Code quality cleanup sprint (W1-W9)

### fix
- Removed dead code: `toKeywordSet()` in SearchServiceImpl, `boolOrNull()` in JdbcSearchGovernanceMapper
- Fixed `bool()` null default from `true` to `false` in JdbcSearchGovernanceMapper (NULL is_active no longer silently enables governance rules)
- Unified `update()` method contract in JdbcSearchGovernanceMapper to accept only camelCase keys
- Replaced SQL `.formatted()` anti-pattern with safe string concatenation in JdbcRecommendMapper
- Removed 5 redundant response field aliases from JdbcRecommendMapper.toApiMap() (price, cover, coverUrl, type, publishedAt)
- Added try/catch isolation to `recordKeywordSearch` so DB failures don't block search responses
- Added `@Transactional` to create/update/delete governance rule methods in SearchServiceImpl

### refactor
- Extracted shared `MapperTypeConverters` utility class (string, toInt, toLong, toDouble, first) — eliminates duplicated helpers across 4 Mapper files
- Merged duplicate query method pairs in JdbcSearchLogMapper (findRecentDailyAggregates, findTopKeywordsForRecentWindow) into shared internal methods
- Extracted `createAsyncAction` helper for frontend Pinia stores — unified loading/error pattern across search, review, and recommend stores
- Unified all frontend error messages to Chinese

### docs
- Added `docs/04-standards/code-quality-standards.md` — coding standards covering comments, Mapper patterns, Service resilience, Store async patterns, dead code, SQL safety, boolean defaults, API response fields, and review checklist
- Added `docs/08-tasks/drafts/code-quality-cleanup-sprint.md` — detailed work item breakdown for the cleanup sprint

### test
- Frontend store tests compatible with refactored async pattern (exported names preserved)

## [2026-05-18] - Home and explore UI/UX redesign

### changed
- Reworked `HomeView` into a light platform-introduction entry with a focused hero, explore-first CTA path, platform metrics, trust guidance, hot-search preview, and buyer/seller onboarding instead of a dominant full product list
- Reworked `ProductListView` into the main explore surface for both `/app/explore` and the compatible `/app/products` route, keeping the existing route-driven keyword, category, product-type, and pagination flow intact
- Added shared explore UI components for the redesign: `ExploreSearchShell`, `ExploreProductCard`, and `FeaturedShopsSection`
- Upgraded `SearchSuggestInput` and `HotSearchList` presentation so search suggestions, focus elevation, and hot-keyword chips feel like one warm shared surface rather than a plain form control
- Kept all data access on the existing API modules and Pinia stores; no backend API contract changes were introduced

### test
- Ran `npm test`
- Ran `npm run build`
- Performed browser-based manual UI checks with mocked API responses for home, explore, search suggestions, category filtering, featured shops, empty state, error state, and mobile overflow safety

### deferred
- Explore and home visual quality still depends on future richer real product images; the current redesign stabilizes image containers and crop behavior so imperfect source images remain usable
- Featured shop richness still depends on future backend/shop profile depth; for now it is compatibly derived from existing public product and shop fields without adding new endpoints
# Changelog

All notable changes to this project are documented here. AI agents prepend a new block at the top after each completed iteration.

## [2026-05-17] - Profile settings and shop identity redesign

### feat
- Rebuilt `ProfileView` as an identity-and-summary page that separates personal identity, trade/favorite/review summaries, non-owner growth guidance, and owner-facing shop management from account settings
- Connected the frontend to the existing `GET /api/shops/mine` endpoint so owner-state handling uses current backend shop data instead of long-term frontend hardcoding
- Reworked `SettingsView` into a real settings center with sections for preferences, addresses, security, notifications, privacy, and logout
- Refreshed `PreferenceSettingsView`, `VerificationView`, `SellerProductsView`, and `SellerPublishView` so they align with the new `/app/me`, `/app/settings`, and `/app/shop/manage/*` information architecture

### refactor
- Added `normalizeShop()` plus address/verification compatibility cleanup in `market-normalizers.js`
- Extended `marketStore` with `ownedShop` state and `loadMyShop()` to share shop-owner context across profile, settings, and seller-management pages
- Switched primary seller-entry navigation inside the updated views from legacy `/app/seller/*` paths to `/app/shop/manage/*` while keeping router compatibility intact

### test
- Ran `npm run test`
- Ran `npm run build`
- Manually verified with browser automation against local frontend/backend that `/app/me`, `/app/settings`, `/app/settings/preferences`, `/app/verification`, `/app/shop/manage/products`, and `/app/shop/manage/publish` render with the expected new role-specific headings and actions

## [2026-05-17] - Messages center and admin support entry first-pass UI

### changed
- Rebuilt `frontend/src/views/app/MessagesView.vue` into a responsive message-center shell with trade, shop, support, and group categories, conversation detail placeholders, disabled composer state, and mobile list/detail switching
- Added future deep-link conventions from product, shop, and order flows into `/app/messages` using explicit route query context instead of pretending real chat already exists
- Added `frontend/src/views/admin/SupportView.vue` and `/admin/support` as the first restrained admin entry for customer support, after-sales assistance, group governance, and abnormal-message handling

### docs
- Archived `docs/08-tasks/drafts/ui-redesign-messages-support.md` as `docs/08-tasks/archived/ui-redesign-messages-support.md` with completion notes and follow-up backend/API requirements

### test
- Pending in this run: `npm run test`
- Pending in this run: `npm run build`
- Pending in this run: manual browser verification for `/app/messages` and `/admin/support`

## [2026-05-17] - Trade and reviews UI UX redesign

### changed
- Consolidated the cart, checkout, payment, orders, pending reviews, and my reviews pages into a clearer trade-center experience while keeping legacy routes such as `/app/cart`, `/app/orders`, and `/app/reviews/*` available
- Added trade-specific shared UI pieces for transaction navigation, status pills, order cards, review cards, and transaction metric strips so the trade flow now reads as one continuous loop instead of isolated pages
- Reworked high-risk transaction screens to keep explicit loading, duplicate-submit protection, empty/error feedback, illegal-state messaging, and visible payment, refund, report, receipt-confirmation, and review actions

### docs
- Archived `docs/08-tasks/drafts/ui-redesign-trade-reviews.md` as a completed trade-domain task record with implementation and verification notes

### test
- Ran `npm test`
- Ran `npm run build`

## [2026-05-17] - Product and shop detail UI UX redesign

### changed
- Rebuilt `ProductDetailView` into a stronger purchase-decision page with gallery-first product media, a sticky decision column, clearer transaction facts, visible seller and shop entry, and primary actions for buy, cart, favorite, and contact
- Rebuilt `ShopView` so the shop acts as the seller's public-facing subject, emphasizing shop introduction, selected owner identity, trust and trade signals, public goods, reviews, and explicitly reserved follow or group entry points
- Extended frontend product and shop compatibility shaping in `market-normalizers.js` and `market.js` so new presentation fields stay centralized instead of scattering raw backend fields across multiple detail templates
- Replaced unresolved `v-loading` usage in both detail pages with explicit skeleton loading states so runtime loading behavior no longer depends on a missing global directive

### docs
- Archived `docs/08-tasks/drafts/ui-redesign-product-shop-detail.md` as a completed task record with implementation notes and validation blockers

### test
- Ran `npm run test`
- Ran `npm run build`
- Manually verified public detail routes `/app/products/1` and `/app/shops/1` under frontend-only local runtime, confirming error-state rendering while the backend API was offline
- Confirmed that contact/message entry remains route-based and backend-safe; unauthenticated message access still stays behind route auth
## [2026-05-17] - UI redesign shell navigation foundation

### changed
- Added the warm CampusMarket design-token baseline with terracotta/orange primary colors, warm paper surfaces, glass, shadow, radius, and motion variables
- Updated the app shell header, mobile drawer, and mobile bottom navigation to match the 首页 / 探索 / 消息 / 交易 / 我的 information architecture
- Preserved legacy app route compatibility while introducing `/app/explore`, `/app/trade`, `/app/messages`, `/app/me`, and `/app/settings`

### test
- Ran `npm run test`
- Ran `npm run build`

## [2026-05-17] - Frontend redesign safety standard

### docs
- Added `docs/04-standards/frontend-redesign-safety.md` to control UI redesign risks around API modules, stores, normalizers, route compatibility, chat placeholders, shop-owner identity, transaction flows, and admin support scope
- Updated `docs/README.md` to list the frontend redesign safety standard under repository standards

## [2026-05-17] - Admin query pagination hardening

### refactor
- Replaced admin-side `findAll()` -> `stream().filter().sorted()` in-memory list assembly with SQL-backed filtering, sorting, counting, and pagination across all 6 target admin list endpoints (`/api/admin/users`, `/verifications`, `/products`, `/review-tasks`, `/shops`, `/reports`)
- Added `findXxxPaged` and `countXxx` methods to UserMapper, StudentVerificationMapper, ProductMapper, ProductReviewTaskMapper, ShopMapper, ReportMapper and their JDBC implementations
- Optimized admin dashboard to use COUNT queries instead of `findAll()` for metric cards and todo counts

### changed
- Admin list endpoints now accept `page` (default 1) and `pageSize` (default 10, max 100) query parameters
- Response contract changed from `{ items, summary }` to `{ items, total, page, pageSize }` for all 6 list endpoints
- Frontend admin pages now render `el-pagination` with page switching, size selection, and total display
- Removed in-memory summary tag breakdowns from admin list pages (replaced by pagination total)

### docs
- Updated `docs/06-http/admin.http` with pagination query examples
- Updated `docs/09-api-spec/admin.md` with page/pageSize parameters and response fields

## [2026-05-17] - Review order lookup hardening

### refactor
- Replaced full-order scan in `ReviewServiceImpl.submitProductReview()` with a single bounded JOIN query (`order_items` + `orders`) that resolves `orderItemId` -> `{orderId, buyerUserId, orderStatus, productId}` in one lookup
- Removed `findOrderByOrderItemId()` private method that iterated `listOrders()` -> `findOrderItems()` across all orders

### feat
- Added `ReviewMapper.findOrderContextByOrderItemId(Long)` for direct order-item -> order context resolution
- Implemented in `JdbcReviewMapper` as `SELECT o.id, o.buyer_user_id, o.order_status, oi.product_id FROM order_items oi JOIN orders o ON oi.order_id = o.id WHERE oi.id = ?`

### test
- Added `submitProductReviewNonexistentOrderItemReturnsNotFound` to verify 404 on non-existent order item
- Added `submitProductReviewForNonCompletedOrderFails` to verify BUSINESS_ERROR when the order is not completed

### handoff
- The direct lookup query `WHERE oi.id = ?` on `order_items.id` uses the PK index, so no additional index is needed for the query itself
- The JOIN on `orders.id` is also PK-covered, so no extra index is needed there either
- If future features need bulk order-item -> product resolution, consider `idx_order_items_order_product ON order_items(order_id, product_id)`, but the current single-item lookup is already optimal

## [2026-05-17] - Runtime index hardening

### feat
- Added `idx_orders_buyer ON orders(buyer_user_id, submitted_at, id)` to support buyer order listing (`WHERE buyer_user_id = ? ORDER BY submitted_at DESC, id DESC`)
- Added `idx_orders_shop_status ON orders(shop_id, order_status, payment_status, completed_at)` to support shop monthly insight aggregations over completed and paid orders
- Added `idx_order_items_order ON order_items(order_id, id)` to support order detail expansion (`WHERE order_id = ? ORDER BY id`)
- Added `idx_order_items_product ON order_items(product_id)` to support recommendation and hot-product joins on `product_id`
- Added `idx_payment_records_order ON payment_records(order_id, initiated_at, id)` to support order-scoped payment lookups (`WHERE order_id = ? ORDER BY initiated_at, id`)
- Added `idx_refund_records_order ON refund_records(order_id, applied_at, id)` to support order-scoped refund lookups (`WHERE order_id = ? ORDER BY applied_at, id`)
- Added `idx_reports_submitted ON reports(submitted_at)` to support report listing sort (`ORDER BY submitted_at DESC`)
- Added `idx_reports_status_submitted ON reports(status, submitted_at)` to support SQL-backed admin report pagination with status filtering and submitted-time ordering
- Added `idx_student_verifications_status_submitted ON student_verifications(verification_status, submitted_at, id)` to support SQL-backed admin verification pagination with status filtering and reverse submitted-time ordering
- Added `idx_student_verifications_user_submitted ON student_verifications(user_id, submitted_at, id)` to support the latest-verification correlated subquery used in SQL-backed admin user listing
- All indexes are additive and schema-compatible with both H2 and MySQL; `spring.sql.init.continue-on-error=true` keeps repeated initialization idempotent

### deferred
- No `idx_orders_seller` was added because no seller-scoped order listing query exists in the current code
- No dedicated `users(status, created_at)` style index was added because the current admin-user query cost is driven more by latest-verification lookup than by low-cardinality status filtering
- No `reports(target_type, submitted_at)` style index was added because target-type filtering is optional and less central than status-backed pagination
- No extra product / shop / review-task sort indexes were added because existing filter indexes plus limited page windows are sufficient for the current SQL-backed admin queries
- No search_logs index changes were needed because existing `idx_search_logs_created_at` and `idx_search_logs_normalized_keyword` already cover the current aggregate and prefix-filter patterns
- No cart_items indexes were added because that table is adjacent but outside this hardening slice

## [2026-05-17] - Product list request flow hardening

### fix
- Removed the avoidable bootstrap `loadProducts()` call in `ProductListView`; initial page load now performs a single route-driven request instead of fetching an extra unfiltered product page first
- Removed the `preserveCategories` workaround from `market.js` `setProducts()` / `loadProducts()`, since the old double-fetch pattern is gone

### refactor
- `setProducts()` in `market.js` now updates only product result state and no longer mutates category metadata as a side effect
- `DEFAULT_CATEGORIES` in the market store now act as the stable category-chip source for the product list, fully decoupling storefront filter chips from the currently loaded result set while preserving the single-request route-driven flow

## [2026-05-17] - Hardening task wave decomposition

### docs
- Added `docs/08-tasks/active/admin-query-pagination-hardening.md` as the admin-query modernization slice with explicit pagination contract, write-scope limits, and parallelization constraints
- Added `docs/08-tasks/active/review-order-lookup-hardening.md` as the focused review-path lookup task to remove full-order scan behavior without mixing in schema or frontend work
- Added `docs/08-tasks/active/runtime-index-hardening.md` as the schema-owned indexing slice for current runtime query paths
- Added `docs/08-tasks/active/product-list-request-flow-hardening.md` as the storefront-only request-flow cleanup slice
- Updated `docs/08-tasks/drafts/architecture-performance-hardening.md` with wave-1 decomposition and cross-task ownership rules so the hardening work can be distributed to parallel child agents safely

## [2026-05-17] - Architecture and performance hardening draft

### docs
- Added `docs/08-tasks/drafts/architecture-performance-hardening.md` to capture the mid-stage architecture and performance review as an execution-ready cross-cutting hardening task
- Documented the main hidden risks around admin full-table reads, review-path order scans, product search scalability, order-related indexing gaps, frontend product-list double fetch, and configuration safety defaults
- Split the proposed remediation into bounded implementation slices so later execution can stay targeted instead of turning into an unfocused broad refactor

## [2026-05-17] - UI/UX constitution draft

### docs
- Added `docs/03-architecture/ui-ux-constitution.md` to capture the redesigned CampusMarket visual direction, interaction rules, motion system, responsive principles, and AI execution constraints
- Added `docs/03-architecture/frontend-information-architecture.md` to define the new front-office page structure, route distribution, navigation principles, shop-owner identity model, messages entry, trade entry, and admin support area
- Added parallel UI redesign draft tasks for shell/navigation, home/explore, product/shop detail, trade/reviews, messages/support, and profile/settings/shop identity work
- Updated `docs/README.md` so the architecture document area explicitly includes UI/UX constitution guidance

## [2026-05-16] - test-foundation-expansion

### feat
- Added `BackendTestBase` shared test base class with common flow helpers (`addToCart`, `createOrder`, `initiateAndPay`) and search governance utilities
- Added `AdminGovernanceTest` (21 tests) covering admin dashboard, user management, verification review, product/shop management, search governance CRUD, and auth guards
- Added `CartEdgeCaseTest` (6 tests) covering quantity merge, update, remove, not-found, empty cart, and auth checks
- Added `PaymentEdgeCaseTest` (6 tests) covering non-existent order, cross-user payment, double initiation, bogus payment, gateway info, and auth checks
- Added `UserProfileTest` (8 tests) covering profile, verification status, addresses CRUD, default address, logout, and auth/me
- Added `app.test.js` store test (5 tests) covering keyword state, `hasKeyword` computed, and `collapsed` toggle
- Added E2E specs: `admin-governance.spec.js` (18 tests), `offline-order.spec.js` (1 full-flow test), `cart-edge-cases.spec.js` (4 tests)
- Updated `playwright.config.js` with three new project entries for new E2E specs

### refactor
- Extracted common test helpers from `CampusMarketBackendApplicationTests` into `BackendTestBase` to reduce duplication

### test
- Backend: 58 -> 78 tests (+20 new test methods, 0 failures)
- Frontend: 6 -> 7 test files (30 tests, all passing)
- E2E: 1 -> 4 spec files (21 -> 42 total tests)

### feat
- Order detail drawer: buyer-side report submission dialog with reason selection and description
- Order detail drawer: buyerNote display and payment records section (buyer + admin views)
- Responsive drawer width: 100% on mobile (<768px), fixed width on desktop (buyer 680px, admin 720px)

### fix
- Duplicate-submit protection: action buttons disabled with loading state during async operations
- Backend: duplicate refund rejected when pending/in-progress refund exists
- Backend: duplicate payment creation rejected for orders with active payment
- Backend: already-success payment rejects repeat mock-success calls
- Checkout: client-side validation for offline meet time format and required fields
- Payment: buttons disabled when order not in `pending_payment` status; post-payment navigation added

### docs
- Smoke tests: added duplicate refund, duplicate payment, duplicate mock-success cases to `order.http`
- Task spec: order-after-sales-ux-hardening moved from drafts to active, completion notes added

Format per block:
```
## [YYYY-MM-DD] - <iteration id or short description>

### feat
- ...

### fix
- ...

### refactor
- ...

### docs
- ...
```

## [2026-05-16] - Admin governance UI consistency

### feat
- Added `EmptyState` and `ErrorBlock` rendering to `ListPageShell` — all 8 admin list pages now show empty/error feedback instead of silent blank tables
- Created `frontend/src/utils/error-utils.js` — shared `resolveErrorMessage` utility, eliminating 8 copy-pasted copies

### refactor
- Refactored `OrderManageView` to use `ListPageShell` with summary/filters/table slots, matching the pattern used by all other admin list pages
- All 9 admin pages now import `resolveErrorMessage` from the shared utility instead of inline definitions

## [2026-05-16] - Reserved-state realism upgrade

### feat
- Created `ReservedMetricCard.vue` — reusable component that replaces blunt `待接入` text with metric label, scope definition, data source, and muted status badge; uses dashed-border muted card style to visually distinguish reserved metrics from live data
- Created `ReservedPanel.vue` — reusable component that replaces plain `placeholder-block` divs with structured icon + title + description layout for reserved content areas
- Updated `ShopView.vue` — 4 insight metric cards and hot-products panel now render `ReservedMetricCard` / `ReservedPanel` when `shopInsightReserved` is true, showing metric definitions and data sources instead of bare `待接入` text
- Updated `ProfileView.vue` — 4 user insight metric cards and 2 content panels (recent browses, favorite preferences) now render the new components when `userInsightReserved` is true
- Added `viewCountSummary` and `favoriteCountSummary` definitions to `shopInsightMetricDefinitions`

### refactor
- No backend changes; live data branches in both views remain unchanged; `el-alert` banners and status badges preserved

## [2026-05-16] - Tighten formal API specification workflow

### docs
- Updated `docs/09-api-spec/README.md` to separate runtime truth, formal spec, and `.http` validation layers, and to define the current hand-written maintenance workflow
- Expanded `docs/09-api-spec/API_SPEC_TEMPLATE.md` with fixed sections for response envelope, `ResultCode`/HTTP mapping, `BUSINESS_ERROR` semantics, field tables, HTTP asset mapping, and known drift tracking
- Added `docs/09-api-spec/user.md` and `docs/09-api-spec/admin.md` as new formal API spec samples aligned to current controllers and request DTOs
- Corrected direct method/path drift in `docs/06-http/product.http`, `docs/06-http/order.http`, and `docs/06-http/admin.http`
- Added `docs/08-tasks/drafts/api-spec-standardization-follow-up.md` to guide the next round of module-by-module API spec expansion

## [2026-05-16] - Hot search enhancement P3: suggestions and aggregation optimization

### feat
- Added `GET /api/search/suggest` for prefix-based search suggestions derived from recent `search_logs`
- Extended the frontend search store and search inputs on Home and Product List pages with debounced suggestion loading, local error fallback, and selection-driven navigation
- Kept search suggestions under the existing search governance model so hidden and blocked terms are filtered consistently with the hot-search ranking

### refactor
- Optimized hot-search aggregation by moving representative-keyword selection and daily aggregation into `SearchLogMapper`, reducing Java-side regrouping while keeping `/api/search/hot` response semantics unchanged
- Consolidated active governance-rule loading into one snapshot read before applying hide, block, and pin behavior

### test
- Added backend regression coverage for suggestion behavior, governed filtering, pinned ordering, and hot-search recency weighting
- Extended frontend search-store tests for suggestion loading, blank-input clearing, and stale-response protection
- Added a search-suggestion API smoke case to `frontend/e2e/smoke.spec.js`

### docs
- Updated `docs/06-http/search.http` with suggestion and governance validation examples
- Added `docs/09-api-spec/search.md` and linked it from the API spec index
- Archived the P3 task record with completion notes and verification summary

## [2026-05-16] - Frontend bundle optimization first pass

### refactor
- Replaced global `ElementPlus` registration in `frontend/src/main.js` with a local on-demand plugin that registers only the Element Plus components currently used by the project
- Removed the global `element-plus/dist/index.css` import and switched to component-level Element Plus style imports
- Lazy-loaded `AppLayout` and `AdminLayout` from the router so admin shell code is no longer bundled into the main entry by default
- Updated frontend views to consume shared `ElMessage` and `ElMessageBox` exports from the local plugin instead of importing services directly from `element-plus`

### docs
- Archived `docs/08-tasks/archived/frontend-bundle-optimization.md` as the first recorded delivery slice for this cross-cutting governance item
- Updated `docs/05-roadmap/current/feature-roadmap.md` to reflect that frontend bundle governance has moved from not-started to in-progress with a completed first pass

## [2026-05-16] - Add first formal API specifications

### docs
- Added `docs/09-api-spec/auth.md`, `product.md`, and `order.md` as the first formal API contract documents based on current controllers and request samples
- Expanded `docs/09-api-spec/API_SPEC_TEMPLATE.md` into a directly usable module-spec template instead of a minimal placeholder
- Updated `docs/09-api-spec/README.md` and `docs/README.md` so the API specification area now has concrete module entry points

## [2026-05-16] - Documentation system restructure and AGENTS introduction

### docs
- Reorganized `docs/` into `01-product` through `08-tasks`, separating product, requirements, architecture, standards, roadmap, API, decisions, and task records
- Replaced the old `docs/ROADMAP.md` semantics with `docs/04-standards/development-process.md` and added `docs/05-roadmap/stage-roadmap.md` plus `feature-roadmap.md`
- Added `CampusMarket/AGENTS.md` as the repository-wide AI execution guide without moving `CLAUDE.md`
- Introduced `docs/08-tasks/README.md`, `TASK_TEMPLATE.md`, archived task records, and draft-task storage for ongoing work such as hot-search P3
- Updated `CLAUDE.md`, `README.md`, `docs/README.md`, and related references to the new document paths

## [2026-05-16] - Roadmap folder cleanup

### docs
- Refined `docs/05-roadmap/` into `current/` and `archived/` so active roadmap files are separated from historical MVP planning artifacts
- Moved `stage-roadmap.md`, `feature-roadmap.md`, and `open-questions.md` into `docs/05-roadmap/current/`
- Moved `mvp-scope.md` and `mvp-database-cut.md` into `docs/05-roadmap/archived/`
- Added `docs/05-roadmap/README.md` and updated references across standards, task docs, AGENTS, and the docs index

## [2026-05-16] - Separate HTTP collections from API specifications

### docs
- Renamed `docs/06-api/` to `docs/06-http/` because the existing `.http` files are request collections rather than formal API specifications
- Added `docs/09-api-spec/` with a README and template for future real API contract documents
- Updated repository guidance and task/standards references so HTTP validation assets and formal API specs are no longer conflated

## [2026-05-16] - Tighten current roadmap to match actual project state

### docs
- Rewrote `docs/05-roadmap/current/stage-roadmap.md` to focus only on the current phase, next priorities, and active risks
- Rewrote `docs/05-roadmap/current/feature-roadmap.md` to track only still-relevant feature lines instead of repeating completed history
- Rewrote `docs/05-roadmap/current/open-questions.md` to keep only decisions that still affect upcoming work
- Clarified in `docs/05-roadmap/README.md` that completed history belongs in `archived/`, not `current/`

## [2026-05-15] - Hot search enhancement P2: admin governance and search pagination

### feat
- Added `search_governance_rules` table with SENSITIVE_WORD, STOP_WORD, HIDE_KEYWORD, and PIN_KEYWORD rule types
- Added admin CRUD endpoints for governance rules (`/api/admin/search/governance-rules`)
- Public hot-search ranking (`GET /api/search/hot`) now automatically filters blocked/hidden terms and applies pin ordering
- Added paginated search log browser endpoint (`/api/admin/search/logs`) for admin review
- Product search (`GET /api/products`) now supports server-side pagination via `page` and `pageSize` params, returning `{ items, total, page, pageSize }`
- ProductListView replaced IntersectionObserver infinite-scroll with `<el-pagination>` for page-number pagination
- Added admin HotSearchGovernView with rule CRUD, enable/disable toggle, and search log browser dialog

### test
- Backend integration tests: governance rules CRUD, HIDE_KEYWORD filtering on hot-search output (with cleanup), product search pagination metadata, and search log browser pagination
- Updated existing product-list tests and Playwright e2e smoke tests for the new paginated response shape

### docs
- Updated `docs/http/search.http` with paginated search, governance CRUD, and log browser examples

## [2026-05-15] - Product & shop review system

### feat
- Added `reviews` and `shop_reviews` tables with UNIQUE constraints preventing duplicate reviews
- Added `POST /api/reviews/products` and `POST /api/reviews/shops` for authenticated buyer review submission
- Added `GET /api/reviews/pending` listing completed-but-unreviewed order items
- Added `GET /api/reviews/mine` returning user's product and shop reviews
- Added `GET /api/products/{id}/reviews` and `GET /api/products/{id}/review-summary` public endpoints
- Added `GET /api/shops/{id}/reviews` and `GET /api/shops/{id}/review-summary` public endpoints
- Rating aggregates (`rating_score`, `review_count`) update synchronously on review submission
- Backend integration tests covering success, duplicate rejection, score validation, auth, ownership, and public access (12 tests)
- Frontend: `ReviewForm`, `ReviewList`, `RatingSummary` reusable components
- Frontend: `PendingReviewsView` and `MyReviewsView` pages with routing
- Frontend: `ProductDetailView` and `ShopView` now display review summaries and lists
- Frontend: Pinia `review` store with Vitest tests (5 tests)
- Added `docs/http/review.http` with endpoint examples

## [2026-05-15] - Product recommendations: cold-start, personalized, and also-bought

### feat
- Added `GET /api/recommend/home` public endpoint with hybrid cold-start/personalized recommendation
- Added `GET /api/recommend/also-bought/{productId}` public endpoint for co-purchase recommendations
- Cold-start strategy: popularity score = `view_count * 0.6 + sold_count * 10 * 0.4`
- Personalized strategy: fetches hot products from user's purchased categories, falls back to cold-start on empty history
- Also-bought strategy: `order_items` self-join counting co-occurrence in completed/paid orders
- Frontend HomeView now uses `recommendStore.homeRecommendList` instead of client-side product slicing
- Frontend ProductDetailView now shows "also-bought" recommendation section
- New `RecommendMapper` / `JdbcRecommendMapper` with three deterministic SQL queries
- New `RecommendService` / `RecommendServiceImpl` with hybrid strategy orchestration
- New `RecommendController` serving two public GET endpoints under `/api/recommend`

### test
- Added 5 backend integration tests: public cold-start, unavailable product exclusion, personalized for logged-in user, also-bought structure validation, also-bought empty result handling
- Added 4 frontend store tests: home recommend success/failure, also-bought success/failure
- Added `docs/http/recommend.http` with 8 request scenarios

### docs
- Created `docs/dev/tasks/RECOMMEND_FEATURE_DESIGN.md` as feature design document

## [2026-05-15] - Hot search enhancement P1: Chinese UI, encoding repair, filter exposure, and smoke tests

### feat
- Restored hot-search related UI copy to project-standard Chinese across HomeView, ProductListView, HotSearchList, search store, and market store fallback categories
- Exposed product type filter (digital/physical/service) in ProductListView alongside existing keyword and category filters
- Product type filter participates in route-query state so combined filter URLs remain shareable

### fix
- Repaired `CHANGELOG.md` encoding: replaced corrupted em dash sequences (`鈥?` → `- `) and cleaned up old entries

### test
- Added Playwright API smoke tests for hot search endpoint, keyword/category/productType filtering, combined filter query, and blank keyword handling
- Updated `docs/http/search.http` with productType and combined filter examples

### docs
- `HOT_SEARCH_ENHANCEMENT_P1.md` status updated to reflect completed delivery

## [2026-05-15] - Hot search enhancement planning documents

### docs
- Rewrote `docs/dev/新功能规划.md` as a clean UTF-8 master planning document for next-stage feature work
- Clarified that hot search v1 is complete and that enhancement work is split into P1, P2, and P3 task specs
- Added `HOT_SEARCH_ENHANCEMENT_OVERVIEW.md`, `HOT_SEARCH_ENHANCEMENT_P1.md`, `HOT_SEARCH_ENHANCEMENT_P2.md`, and `HOT_SEARCH_ENHANCEMENT_P3.md`
- Marked the archived role of `HOT_SEARCH_FEATURE_TASK.md` as the completed baseline task
- Updated `CURRENT_STAGE_WORK_PLAN.md` to unify completion markers and next-step pointers for completed and planned workstreams

## [2026-05-13] - MVP closure: MySQL full migration and automated smoke/CI pipeline

### feat
- Full MySQL 8 migration: replaced H2 runtime with `mysql-connector-j`, updated datasource config
- Schema adapted for MySQL: `CLOB` to `TEXT`, `CREATE INDEX IF NOT EXISTS` to conditional-safe MySQL equivalents, and `DROP INDEX IF EXISTS` to migration-safe handling
- Seed data adapted for MySQL: `MERGE INTO` replaced with `INSERT ... ON DUPLICATE KEY UPDATE`
- Java mappers adapted for MySQL-compatible date handling and upsert behavior
- Added Playwright smoke test suite covering public browse, digital purchase, logistics, admin review, report governance, error cases, and browser happy path
- Added GitHub Actions CI workflow and `test:e2e`, `test:e2e:ui`, and `test:all` npm scripts

### test
- Playwright API-level smoke tests run against the real backend with seed tokens
- Vitest excludes `e2e/**` from unit-test collection
- Backend tests continue to use in-memory H2 for fast CI execution

### refactor
- `JdbcSearchLogMapper.findRecentAggregates` computes the cutoff date in Java instead of dialect-specific SQL date arithmetic

## [2026-05-13] - Phase 3 feature 1: hot search and server-side keyword logging

### feat
- Added `search_logs` persistence with normalized keyword, optional user id, result count, and created time for product-search analytics
- Extended public `GET /api/products` to support backend keyword and category filtering while preserving the existing public browsing route
- Added public `GET /api/search/hot` with a 7-day weighted hot-keyword ranking
- Added a dedicated frontend search store, recent-search history, and a reusable hot-search list component
- Home page now shows hot keywords and supports direct keyword jumps into the product list
- Product list search is now route-query driven so keyword/category filters can be refreshed or shared reproducibly

### test
- Added backend integration coverage for keyword search, hot-search ranking, and public category filtering
- Added frontend Vitest coverage for hot-search loading and recent-search history behavior

### docs
- Refined `docs/dev/tasks/CURRENT_STAGE_WORK_PLAN.md` with a concrete hot-search task definition and recorded the completed delivery slice
- Added `docs/dev/tasks/HOT_SEARCH_FEATURE_TASK.md`
- Added `docs/http/search.http`

## [2026-05-13] - MVP closure: governance, insights, delivery, and tests

### feat
- Added user-side report submission through `POST /api/reports`, persisted through JDBC and visible in the existing admin report workflow
- Added persistent `user_preferences` storage with JDBC read/upsert support for `GET` / `PUT /api/users/me/preference`
- User insight snapshot now uses real completed-order aggregation for total spend, purchased item count, recent purchases, and category preference summary
- Shop insight snapshot now uses real completed-order and product aggregation for monthly sales, monthly orders, hot products, views, favorites, and repeat buyers
- Added `digital_access_logs` table and JDBC mapper for digital asset access audit persistence
- Added `accessDigitalAsset` to `OrderService`, validating buyer ownership, digital order type, receipt completion, and `full_access` status before granting access
- Added `GET /api/orders/{orderId}/assets/{assetId}/access` endpoint with server-side authorization
- Order detail now includes `digitalAccessLogs` array showing full access audit trail
- Added lightweight frontend report API module and authorized digital asset access API call

### fix
- Full digital assets now require backend-authorized access through the new endpoint; preview assets remain visible in order detail without access logging
- Digital asset section in `OrdersView` now shows an authorized "access resource" action instead of exposing raw full-asset URLs
- Frontend auth store now normalizes session roles to lowercase so `USER` / `ADMIN` API payloads work with route guards

### refactor
- Replaced user preference process-memory storage with mapper-backed database persistence
- Centralized digital asset access through the order service instead of deriving access behavior in the frontend

### test
- Added backend integration coverage for preference default read, update, and second read consistency
- Added backend integration coverage for report submit, invalid report target, admin visibility, and admin processing
- Added backend integration test `digitalAssetAccessGovernanceEnforcesOwnershipAndLogsAccess`, covering pre-receipt block, non-buyer 403, post-receipt access, and access log visibility
- Added backend integration coverage for real user and shop insight aggregation
- Added frontend Vitest coverage for auth store, market store product loading, and router guard access control

### docs
- Added `docs/dev/tasks/CURRENT_STAGE_WORK_PLAN.md` for AI-agent execution planning
- Updated roadmap and engineering docs to align the current stage as MVP closure on H2 file database + JDBC, with MySQL/MyBatis kept as course and migration direction
- Added `docs/http/report.http`
- Updated `docs/http/auth.http` and `docs/http/order.http` for preference and digital asset access flows
## [2026-05-21] - Repository bootstrap hardening for Youyu

### docs
- Rewrote the repository root `README.md` as a proper standalone project entry for `Youyu`, including stack overview, quick start, repository structure, documentation entry points, and migration notes from the older course workspace

### ci
- Hardened `.github/workflows/ci.yml` with manual trigger support, workflow concurrency control, explicit read-only permissions, job timeouts, healthier service readiness checks, and failure artifact uploads for Playwright runtime logs

### chore
- Expanded root `.gitignore` to cover common local build outputs, editor files, OS files, Vite artifacts, and local environment overrides
- Added `.editorconfig` for cross-editor formatting consistency
- Added `.gitattributes` for safer line-ending behavior across Windows and Linux
- Added `.github/pull_request_template.md` to standardize summary, testing, and documentation expectations for protected-branch development

---
