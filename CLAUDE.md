# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

CampusMarket is a campus-focused vertical shopping platform. Monorepo: Vue 3 frontend + Spring Boot 3.3 backend + MySQL database.

**Product positioning**: A student-only marketplace emphasizing learning and dorm life products, with seller growth, campus credit tracking, identity verification, and transaction traceability.

## Build, Run, and Test Commands

### Backend (Spring Boot 3.3 + MySQL)

All commands run from `backend/`.

**Prerequisite**: MySQL 8+ running on `localhost:3306` with database `campus_market` created. Configure credentials via `MYSQL_PASSWORD` env var or edit `application.yml`.

**First-time setup** (create schema + seed demo data):
```bash
# Windows
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=seed

# macOS / Linux
./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
```

**Normal run** (schema-only, no seed data):
```bash
mvnw.cmd spring-boot:run
```

**Run all tests** (in-memory H2, no MySQL needed):
```bash
mvnw.cmd test
```

**Run a single test class**:
```bash
mvnw.cmd test -Dtest=CampusMarketBackendApplicationTests
```

**Run a single test method**:
```bash
mvnw.cmd test -Dtest=CampusMarketBackendApplicationTests#contextLoads
```

**Build JAR**:
```bash
mvnw.cmd clean package
```

**Server details**:
- URL: `http://localhost:8080`
- H2 console (test profile only): `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:testdb`, user: `sa`, empty password)

### Frontend (Vue 3 + Vite)

All commands run from `frontend/`.

```bash
npm ci                  # Install dependencies
npm run dev             # Dev server at http://localhost:5173, proxies /api to :8080
npm run build           # Production build
npm run preview         # Preview production build locally
npm test                # Vitest unit tests
npm run test:watch      # Vitest in watch mode
npm run test:e2e        # Playwright E2E tests (requires backend + frontend running)
npm run test:all        # Vitest + Playwright
```

### Performance Catalog

Regenerate synthetic product catalog (from repo root):
```bash
node scripts/generate-perf-catalog-sql.mjs 3500
```
Catalog loads only with profile `seed`, is idempotent, uses product IDs 10000+.

### CI Pipeline

GitHub Actions (`.github/workflows/ci.yml`) runs on push/PR to `master`:
1. **Backend Tests** — JDK 17, `./mvnw test -B`
2. **Frontend Tests + Build** — Node 22, `npm ci`, `npm test`, `npm run build`
3. **Playwright Smoke** — starts backend (seed profile) + frontend dev server, runs `npx playwright test`

## Architecture

### High-Level Design

Backend: Spring Boot REST API, layered by domain (controller -> service -> mapper -> entity).
Frontend: Vue 3 SPA with Pinia state, Axios HTTP, Vue Router 4.

```
backend/src/main/java/com/campusmarket/backend/
  controller/       HTTP endpoints, grouped by domain sub-package
  service/          Business logic (interface + impl per domain)
  mapper/           JDBC data access (interface + Jdbc*Mapper impl)
  entity/           Plain POJOs, no ORM annotations
  filter/           AuthContextFilter, AuthInterceptor, RequestTraceFilter
  common/           ApiResponse, ResultCode, AuthContextHolder, exceptions, enums
  config/           WebMvcConfig, AuthProperties, PasswordConfig

frontend/src/
  views/            Route-mapped pages (app/, admin/, auth/, system/)
  components/       Reusable UI (layout/, common/, search/)
  stores/           Pinia stores (auth, market, search, recommend, review, app)
  router/           Router config, guards, route modules
  api/              Axios client + domain modules
  utils/            Auth storage, localStorage helpers, data normalizers
  constants/        Navigation menus, insight metric definitions
  styles/           CSS custom properties + global styles
```

### Request Flow

Controller (validates input, calls service, returns `ApiResponse<T>`) -> Service (orchestrates logic, calls mappers) -> Mapper (JDBC via `JdbcTemplate`/`NamedParameterJdbcTemplate`, returns `Map<String, Object>`) -> Entity (plain POJOs).

**Cross-cutting**:
- **AuthContextFilter** — parses JWT from `Authorization: Bearer <token>`, sets `AuthContextHolder` (ThreadLocal)
- **AuthInterceptor** — checks `@LoginRequired` annotation, enforces role-based access
- **GlobalExceptionHandler** (`@RestControllerAdvice`) — maps exceptions to HTTP status codes
- **RequestTraceFilter** — attaches trace ID to every request

**Response envelope**:
```json
{ "success": true, "code": "SUCCESS", "message": "OK", "data": {...}, "traceId": "uuid" }
```

### Backend Domain Modules

| Domain | Package | Key Endpoints |
|--------|---------|---------------|
| Auth | `auth/` | POST `/api/auth/register`, `/api/auth/login` |
| Admin Auth | `admin/` | POST `/api/admin/auth/login` |
| Users | `user/` | Profile, addresses, student verification, preferences, insight snapshots |
| Products | `product/` | CRUD, list with pagination/filter, my products, review tasks |
| Shops | `shop/` | Detail, insight snapshots, capability profiles |
| Orders | `order/` | Cart CRUD, order preview/submit, confirm receipt, refunds, admin fulfillment |
| Payments | `payment/` | Initiate payment, mock success callback |
| Reviews | `review/` | Submit product/shop reviews, pending reviews, review lists with summaries |
| Recommendations | `product/` | GET `/api/recommend/home` (popularity/personalized), `/api/recommend/also-bought/:id` |
| Search | `search/` | Hot keywords, search governance rules (admin CRUD), search log browser |
| Reports | `report/` | Submit reports, admin process |
| Favorites | `product/` | Toggle and list favorites |

### Frontend Stores and Routes

**Pinia stores** (Composition API):
- `auth.js` — login/logout, session management, localStorage persistence
- `market.js` — products, shops, favorites, profile, preferences, insight snapshots
- `search.js` — hot keywords, search history (localStorage, max 8)
- `recommend.js` — home recommendations, also-bought
- `review.js` — pending reviews, my reviews, product/shop review lists with summaries
- `app.js` — global keyword, sidebar collapsed state

**Routes**:
- `/login`, `/register` — public
- `/app/*` — 16 routes (home, products, cart, orders, reviews, favorites, profile, shop, seller, etc.)
- `/admin/*` — 9 routes (dashboard, users, verifications, products, review-tasks, shops, orders, reports, hot-search)

**Route guards** (`guards.js`): `requiresAuth` redirects to `/login`, role mismatch redirects to appropriate root.

### API Client Pattern

```javascript
// api/client.js — Axios instance, baseURL /api, auto-attaches Bearer token, unpacks response.data
// api/modules/product.js — domain endpoints
import service from '@/api/client'
export async function getProductList(params) { return service.get('/products', { params }) }
```

## Database

**Production/Dev**: MySQL 8+ at `localhost:3306/campus_market`. Schema in `schema.sql` uses `CREATE TABLE IF NOT EXISTS` (idempotent, no migration tool).

**Test**: H2 in-memory with MySQL compatibility mode. No MySQL required for tests.

**Schema** (22 tables):
- **Users**: `users`, `user_privilege_profiles`, `student_verifications`, `user_addresses`, `user_preferences`
- **Products**: `categories`, `products`, `product_media`, `product_digital_assets`, `product_review_tasks`
- **Shops**: `shops`, `shop_capability_profiles`
- **Orders**: `cart_items`, `orders`, `order_items`, `order_fulfillments`
- **Payments**: `payment_records`, `refund_records`
- **Reviews**: `reviews` (product), `shop_reviews`
- **Governance**: `reports`, `search_logs`, `search_governance_rules`, `digital_access_logs`

**Seed data** (profile `seed` only):
1. `schema.sql` — DDL
2. `seed/data.sql` — demo users (including `seedbuyer` accounts), shops, categories, products
3. `seed/seed-transactions.sql` — cart items, orders, fulfillments
4. `seed/data-perf-catalog.sql` — performance catalog (IDs 10000+), generated by `scripts/generate-perf-catalog-sql.mjs`

**Access layer**: Pure JDBC via Spring `JdbcTemplate` + `NamedParameterJdbcTemplate`. No ORM. Mappers return `Map<String, Object>`. Each domain has a `*Mapper` interface + `Jdbc*Mapper` implementation.

## Authentication

**Production flow**: JWT tokens via jjwt 0.12.6. `AuthController.register/login` returns `{ token, user, privilege }`. `AuthContextFilter` parses the Bearer token, `AuthInterceptor` enforces `@LoginRequired`.

**Test flow**: Mock tokens `Bearer mock-{userId}-{role}` still work in tests and local dev (recognized by `JwtAuthTokenServiceImpl`). Demo users: admin `admin`/`admin123`, users `zhangsan`/`user123`, seed buyers `seedbuyer`/`user123`.

**Password hashing**: bcrypt via `spring-security-crypto` (`PasswordConfig.java` provides `BCryptPasswordEncoder`).

**JWT config** (`application.yml`):
- Secret: `app.jwt.secret` (dev default: `campusmarket-dev-secret-key-replace-in-production-min32`)
- Expiration: `app.jwt.expiration-hours` (72h)

## Key Conventions

### Backend

- Services are interfaces with `@Service` + `@Transactional` implementations
- Constructor injection preferred
- Throw `BusinessException(resultCode, message)` for domain errors
- No DTO layer — controllers accept `Map<String, Object>` or request DTOs, data flows mapper -> service -> controller -> response
- Mapper pattern: `XxxMapper` interface + `JdbcXxxMapper` implementation (no `@Repository`, pure JDBC)

### Frontend

- Store pattern: `ref()` state, `async function` actions, `computed` for derived state, try/catch/finally for loading flags
- API modules: thin wrappers around the Axios client instance
- Data normalization: `utils/market-normalizers.js` transforms snake_case API responses to camelCase client shape
- Component convention: `ListPageShell.vue` for list views, `FormPageShell.vue` for forms, `EmptyState.vue`/`ErrorBlock.vue`/`SkeletonCard.vue` for loading/error states

## Document System

```
docs/
  01-product/          Product positioning, business scenarios, user model
  02-requirements/     Functional and non-functional requirements, domain model
  03-architecture/     Tech stack, repo structure, implementation strategies
  04-standards/        Development process, testing, contribution, course constraints
  05-roadmap/          Stage/feature roadmap, open questions (current/), MVP scope (archived/)
  06-http/             Executable smoke-test request collections (.http files)
  07-decisions/        Architecture Decision Records (immutable)
  08-tasks/            Task specs: drafts/ -> active/ -> archived/
  09-api-spec/         Formal API specs (auth, order, product) with template
```

Reading order for agents: `AGENTS.md` -> `CLAUDE.md` -> `docs/README.md` -> relevant roadmap/task docs.

## AI Agent Protocol

### Before starting a task

1. Read `AGENTS.md` first, then this file, then `docs/README.md`.
2. Find your task spec in `docs/08-tasks/active/` or `docs/08-tasks/drafts/`. If none exists, ask the human.
3. Read every file listed in the task spec's "Files to read" section.
4. Run `mvnw.cmd test` (backend) before making changes. If it fails before your changes, report it.
5. Identify the exact files to modify. Any file not in the task spec's "Changes" section is out of scope.

### During implementation

- One logical change at a time. Don't batch unrelated edits.
- If out-of-scope changes become necessary, stop and flag it.
- Never modify `schema.sql` DDL or seed files unless the task spec includes the exact SQL.
- Never change test assertions without instruction. If a change breaks a test, fix the root cause.
- Never run `git push` unless explicitly asked.

### Post-task checklist

1. `mvnw.cmd test` — zero failures, zero errors
2. Prepend a block to `CHANGELOG.md`
3. If an endpoint was added or its contract changed, update `docs/09-api-spec/`
4. Update `docs/06-http/*.http` smoke tests to cover new or changed endpoints
5. If a significant design decision was made, create an ADR in `docs/07-decisions/`

### Hard limits

- Do not use `ALTER TABLE` or `DROP TABLE` in `schema.sql`
- Do not commit with `--no-verify`
- Do not delete files not listed in the task spec

## Document Maintenance

| Document | Maintained by | Update when |
|----------|--------------|-------------|
| `CLAUDE.md` | Human (agents may update links) | Architecture, build commands, conventions change |
| `AGENTS.md` | Human | Cross-agent protocol or doc structure changes |
| `docs/README.md` | Human or agent | Document structure or reading order changes |
| `docs/04-standards/*.md` | Human or agent | Process, testing, or governance rules change |
| `docs/05-roadmap/*.md` | Human or agent | Stage status, feature order, planning assumptions change |
| `docs/06-http/*.http` | Agent (per task) | Smoke tests need to cover new or changed endpoints |
| `docs/07-decisions/*.md` | Agent (per task) | Significant design decision; never edit after creation |
| `docs/08-tasks/` | Human or agent | Task lifecycle (create, activate, complete, archive) |
| `docs/09-api-spec/` | Agent (per task) | Endpoint contract or response shape changes |
| `CHANGELOG.md` | Agent (per task) | Every completed task prepends one block |
