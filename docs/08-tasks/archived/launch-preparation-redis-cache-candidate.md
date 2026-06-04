# Task: Launch Preparation Redis Cache Candidate

## Metadata

- ID: launch-preparation-redis-cache-candidate
- Status: archived
- Owner: meilingrl
- Track: cross-cutting
- Depends on: launch-preparation-roadmap L4 performance/cache candidate
- Priority: medium
- Planned date: 2026-06-04
- Completed date: 2026-06-04

## Objective

Introduce Redis as an optional launch-performance cache candidate for high-read public endpoints without making local development, tests, or staging startup depend on Redis availability.

## Background

The launch-preparation roadmap identifies Redis candidates for hot search, recommendations, product detail, categories, and configuration. This slice covers the lowest-risk read-heavy candidates already present in the current runtime: hot search, home recommendations, and also-bought recommendations.

## Scope

- Add Redis client dependencies and environment-driven Redis configuration.
- Add explicit cache helper with TTLs, key prefixing, read/write fallback, and prefix eviction.
- Cache `/api/search/hot`, `/api/recommend/home`, and `/api/recommend/also-bought/{productId}` when enabled.
- Evict hot-search cache after search-governance mutations.
- Evict recommendation caches after product publish/update/status/delete mutations.
- Evict recommendation caches after order completion and product-review submission.
- Add Compose Redis service and optional environment switches.
- Add Redis password, memory ceiling, and LRU eviction configuration for Compose rehearsal.
- Keep the Compose demo stack directly runnable with staging security requirements, including CORS environment forwarding and frontend Docker build compatibility.
- Document cache behavior and staging rehearsal expectations.

## Out of Scope

- Redis as a mandatory production dependency.
- Distributed sessions or token blacklist.
- Rate limiting.
- Product detail/category/config caches.
- Load-test acceptance claim.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/05-roadmap/current/launch-preparation-roadmap.md`
- `docs/03-architecture/performance-and-scalability.md`
- `backend/README.md`
- `backend/src/main/java/com/youyu/backend/service/search/impl/SearchServiceImpl.java`
- `backend/src/main/java/com/youyu/backend/service/product/impl/RecommendServiceImpl.java`
- `backend/src/main/java/com/youyu/backend/service/product/impl/ProductServiceImpl.java`

## Allowed Changes

- Backend cache configuration and cache helper.
- Search/recommend/product service cache integration.
- Backend tests covering cache behavior.
- Compose and environment examples for Redis rehearsal.
- Minimal Compose demo compatibility fixes required to start the stack.
- Launch runbook, API specs, task record, and changelog.

## Implementation Plan

1. Add Redis client dependencies and typed cache properties.
2. Implement explicit Redis cache support with fallback behavior.
3. Wire hot-search and recommendation cache reads/writes.
4. Wire cache eviction after governance and product mutations.
5. Add Compose and documentation support.
6. Verify with targeted cache tests and full backend tests.

## Risks

- Redis outage must not break public endpoints while cache is optional.
- Governance changes must not be hidden behind stale hot-search cache.
- Recommendation cache invalidation is broad by prefix; acceptable for this launch candidate but may be refined later.

## Test Plan

- Backend: targeted cache tests for hit, miss, TTL write, and invalidation behavior.
- Backend: full `.\mvnw.cmd test`.
- API validation: existing `docs/06-http/search.http` and `docs/06-http/recommend.http` remain contract-valid; response shapes unchanged.
- Manual: enable `YOUYU_REDIS_CACHE_ENABLED=true`, call hot-search and recommendation endpoints twice, and compare behavior with Redis disabled.

## Acceptance Criteria

- [x] Redis cache is disabled by default.
- [x] Hot search and recommendation endpoints preserve existing response contracts.
- [x] Redis read/write failures fall back to existing service paths.
- [x] Governance mutations evict hot-search cache.
- [x] Product mutations evict recommendation caches.
- [x] Order completion and product-review submission evict recommendation caches.
- [x] Compose and environment docs expose Redis rehearsal switches.
- [x] Redis Compose rehearsal supports password, memory ceiling, and LRU eviction policy.
- [x] Compose demo stack starts with Redis cache and Redis health enabled.
- [x] Tests cover cache hit/miss and invalidation behavior.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] `backend/README.md`
- [x] `docs/04-standards/launch-foundation-runbook.md`
- [x] `docs/09-api-spec/recommend.md`
- [x] `docs/09-api-spec/search.md`
- [x] task status and archive move

## Completion Notes

Implemented as an optional launch-performance candidate. The cache covers hot search, home recommendations, and also-bought recommendations with configurable TTLs. Redis is available in Compose for rehearsal parity, but backend cache usage and Redis health remain opt-in through environment variables. Redis remains cache-only; MySQL is still the durable backup source.

Compose rehearsal was run with `YOUYU_REDIS_CACHE_ENABLED=true`, `YOUYU_REDIS_HEALTH_ENABLED=true`, and a local Redis password. The stack started successfully after adding local CORS origin forwarding for staging profile and fixing the frontend Alpine/Rollup Docker build dependency. Actuator reported Redis `UP`, the cache candidate endpoints returned success, Redis keys were created, and Redis config confirmed AOF, `256mb` maxmemory, and `allkeys-lru`.
