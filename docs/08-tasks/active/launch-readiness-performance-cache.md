# Task: Launch Readiness Performance Cache

## Metadata

- ID: launch-readiness-performance-cache
- Status: active
- Owner: Codex
- Track: cross-cutting
- Depends on: launch-preparation roadmap L4; Redis cache candidate; deployment/runbook task
- Priority: P1
- Planned date: 2026-06-04
- Completed date:

## Objective

Establish launch performance evidence, cache behavior, and degradation controls for expected demo or early-production traffic.

## Background

The Redis cache candidate covers hot search and recommendation endpoints. The launch roadmap still requires performance baselines, database index review, cache invalidation evidence, HTTP cache headers, rate limiting candidates, pressure-test scripts, and an acceptance target of 500 concurrent users, p95 under 500 ms, and error rate under 1%.

## Current State

Completed in earlier launch-preparation waves:

- k6 smoke script exists for `/api/health`, product list, hot search, and home recommendations.
- The launch-foundation runbook records a lightweight 2-VU smoke with 0 failures and p95 19.8 ms.
- Redis cache candidate exists for hot search, home recommendations, and also-bought recommendations.
- Redis is optional and cache-only; MySQL/JDBC remains the source of truth.
- 2026-06-04 closeout replaced Redis prefix eviction `KEYS` usage with `SCAN` and added focused tests.
- 2026-06-04 closeout split ECharts/ZRender chunks and removed the frontend large vendor chunk warning.

Remaining work:

- Formal 500-concurrency capacity testing has not been run.
- Query-plan/index review for high-traffic endpoints needs recorded evidence.
- HTTP cache and rate-limiting decisions remain open.
- Redis production dependency decision requires staging cache rehearsal evidence.

## Scope

- Define and run reproducible baseline tests for product list, search, detail, order, admin list, hot search, recommendation, and payment-adjacent flows.
- Review high-frequency query plans and pagination stability.
- Validate Redis cache hit/miss/fallback/invalidation behavior for implemented candidates.
- Identify next cache candidates for product detail, categories, and configuration without making Redis mandatory.
- Add HTTP cache headers for safe static/public read-only surfaces if missing.
- Define rate-limiting candidates for login, register, search, payment, support, and admin high-risk operations.

## Out of Scope

- Making Redis a durable data store.
- Full distributed rate limiting unless explicitly selected during implementation.
- Rewriting search or recommendation algorithms.
- Declaring capacity success without reproducible pressure-test evidence.

## Files to Read

- `docs/03-architecture/performance-and-scalability.md`
- `docs/04-standards/launch-foundation-performance-baseline.md`
- `docs/05-roadmap/current/launch-preparation-roadmap.md`
- `backend/src/main/java/com/youyu/backend/service/search/impl/SearchServiceImpl.java`
- `backend/src/main/java/com/youyu/backend/service/product/impl/RecommendServiceImpl.java`
- `backend/src/main/java/com/youyu/backend/service/product/impl/ProductServiceImpl.java`
- `backend/src/main/java/com/youyu/backend/mapper/`
- `frontend/src/api/`
- `scripts/`

## Allowed Changes

- Focused backend performance, cache, pagination, HTTP-cache, or rate-limit code/tests.
- Performance scripts and reproducible result documentation.
- Redis/cache configuration docs and API notes when behavior changes without changing response shapes.
- `CHANGELOG.md` and this task lifecycle record.

## Implementation Plan

1. Inventory current performance scripts, cache support, and high-traffic endpoints.
2. Run baseline tests and record p95/error-rate evidence.
3. Patch low-risk query, cache, HTTP-cache, or rate-limit gaps with focused tests.
4. Re-run performance checks and document degradation behavior and unresolved blockers.

## Risks

- Local hardware results may not represent production capacity.
- Cache invalidation mistakes can serve stale governance or product data.
- Rate limiting can block legitimate demo/test traffic if defaults are too aggressive.

## Test Plan

- Backend: targeted cache/performance tests plus `cd backend; .\mvnw.cmd test`.
- Frontend: `cd frontend; npm test` and build only if client caching or API behavior changes.
- API validation: search, recommend, product list/detail, order preview/submit, and admin list smoke paths.
- Manual: pressure-test script run with documented environment, concurrency, p95, throughput, and error rate.

## Acceptance Criteria

- [x] Lightweight baseline smoke evidence is recorded with commands and environment.
- [x] Implemented caches preserve response contracts and have fallback/invalidation coverage in backend tests.
- [ ] High-frequency queries have documented index/query-plan review.
- [ ] HTTP-cache and rate-limit decisions are documented or implemented with safe defaults.
- [ ] 500-concurrency launch target status is recorded as pass, blocked, or deferred with concrete evidence.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] performance/cache standards and API docs if behavior changes
- [ ] task status and archive move

## Completion Notes

- 2026-06-04 sync: `RedisCacheSupport.evictByPrefix` now uses `SCAN` and batched deletes instead of Redis `KEYS`.
- 2026-06-04 verification: `.\mvnw.cmd test` passed with 255 tests; frontend tests and build passed; Vite large-chunk warning is gone.
- 2026-06-04 remaining: formal capacity test, query-plan review, HTTP cache/rate-limit decisions, and Redis production acceptance evidence stay open.
