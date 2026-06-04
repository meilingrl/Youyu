# Backend

Spring Boot 3.3 backend for Youyu. Layered by domain: `controller / service / mapper / entity / config / filter / listener`.

## Run locally

**Prerequisite**: MySQL 8+ on `localhost:3306`, database `youyu` created. Set credentials via `MYSQL_PASSWORD` env var or edit `application.yml`.

### Environment variables

| Name | Required | Default | Description |
|------|----------|---------|-------------|
| `MYSQL_PASSWORD` | local: optional, deploy: yes | `yinkaixin123` | MySQL password for the `youyu` schema |
| `APP_JWT_SECRET` | local: optional, deploy: **yes** | committed dev secret | JWT signing secret (≥32 chars); falls back to a dev default when running with profile `dev` / `seed` / `test` |
| `YOUYU_CORS_ALLOWED_ORIGINS` | staging/prod: **yes** | empty | Comma-separated exact frontend origins allowed by CORS, for example `https://www.example.com` |
| `YOUYU_CORS_ALLOWED_ORIGIN_PATTERNS` | staging/prod: optional | empty | Comma-separated CORS origin patterns; avoid this unless exact origins are not sufficient |
| `APP_MAIL_HOST` | runtime email delivery: **yes** | none | SMTP server host |
| `APP_MAIL_PORT` | runtime email delivery: **yes** | none | SMTP server port |
| `APP_MAIL_USERNAME` | runtime email delivery: **yes** | none | SMTP login username |
| `APP_MAIL_PASSWORD` | runtime email delivery: **yes** | none | SMTP login password or provider authorization code |
| `APP_MAIL_FROM` | runtime email delivery: **yes** | none | Verified sender address |
| `APP_MAIL_SSL_ENABLED` | runtime email delivery: **yes** | none | Set `true` when the provider requires SMTP SSL |
| `YOUYU_AMAP_ENABLED` | logistics map WebService: optional | `false` | Enables backend Amap map-provider readiness metadata |
| `YOUYU_AMAP_WEB_SERVICE_KEY` | logistics map WebService: yes when enabled | none | Server-side Amap WebService key; keep private |
| `YOUYU_LOGISTICS_TRACKING_ENABLED` | real logistics tracking: optional | `false` | Enables outbound logistics provider requests |
| `YOUYU_LOGISTICS_TRACKING_PROVIDER` | real logistics tracking: optional | `disabled` | Supported value for real adapter: `kdniao` |
| `KDNIAO_BUSINESS_ID` | Kdniao tracking: yes when enabled | none | Kdniao EBusinessID |
| `KDNIAO_APP_KEY` | Kdniao tracking: yes when enabled | none | Kdniao app key; keep private |
| `KDNIAO_ENDPOINT` | Kdniao tracking: optional | Kdniao API URL | Override only for approved provider endpoints |
| `YOUYU_LOGISTICS_TRACKING_TIMEOUT_SECONDS` | real logistics tracking: optional | `5` | Provider request timeout |
| `YOUYU_REDIS_CACHE_ENABLED` | launch cache rehearsal: optional | `false` | Enables Redis-backed caches for hot search and recommendation candidates |
| `YOUYU_REDIS_HEALTH_ENABLED` | launch cache rehearsal: optional | `false` | Enables Actuator Redis health when Redis is required for a rehearsal |
| `REDIS_HOST` | launch cache rehearsal: optional | `localhost` | Redis host for the backend process |
| `REDIS_PORT` | launch cache rehearsal: optional | `6379` | Redis port for the backend process |
| `REDIS_PASSWORD` | launch cache rehearsal: optional | empty | Redis password, if configured |
| `REDIS_MAXMEMORY` | Compose Redis rehearsal: optional | `256mb` | Redis container memory ceiling for cache keys |
| `REDIS_MAXMEMORY_POLICY` | Compose Redis rehearsal: optional | `allkeys-lru` | Redis eviction policy for cache-only data |
| `YOUYU_REDIS_HOT_SEARCH_TTL` | launch cache rehearsal: optional | `1h` | Hot-search ranking cache TTL |
| `YOUYU_REDIS_HOME_RECOMMEND_TTL` | launch cache rehearsal: optional | `15m` | Home recommendation cache TTL |
| `YOUYU_REDIS_ALSO_BOUGHT_TTL` | launch cache rehearsal: optional | `15m` | Also-bought recommendation cache TTL |

**Production checklist**: `APP_JWT_SECRET` MUST be exported (≥32 chars) before booting any non-dev/seed/test profile. The application fails fast at startup if the committed dev default is detected under any other active profile.

The public registration and password-reset email-code endpoints require SMTP
delivery. The `test` profile uses a deterministic fake sender and never opens a
network connection. Do not commit provider credentials, recipient addresses, or
verification codes.

Logistics tracking is disabled by default and automated tests do not require
provider credentials or network access. Enabling Kdniao only adds provider-backed
event lookup; Amap map rendering remains a separate map-provider concern and
must not be treated as live courier GPS.

Redis caching is disabled by default. When `YOUYU_REDIS_CACHE_ENABLED=true`, hot
search, home recommendation, and also-bought responses are cached with TTLs and
fall back to MySQL/JDBC paths if Redis reads or writes fail. Enable
`YOUYU_REDIS_HEALTH_ENABLED=true` only when Redis availability should affect
Actuator health for a specific staging or launch-performance rehearsal.

For staging or production-like rehearsals, set a non-empty `REDIS_PASSWORD`,
keep Redis private to the application network, and use a bounded memory policy
such as `REDIS_MAXMEMORY=256mb` with
`REDIS_MAXMEMORY_POLICY=allkeys-lru`. Redis currently stores cache-only data, so
MySQL remains the backup source of truth.

**First-time setup** (schema + seed data):
```bash
# Windows
$env:SPRING_PROFILES_ACTIVE="seed"
mvnw.cmd spring-boot:run

# macOS / Linux
SPRING_PROFILES_ACTIVE=seed ./mvnw spring-boot:run
```

**Normal run**:
```bash
$env:SPRING_PROFILES_ACTIVE=""
mvnw.cmd spring-boot:run
```

Server runs at `http://localhost:8080`.

## Tests

Tests use H2 in-memory — no MySQL required.

```bash
mvnw.cmd test                                          # all tests
mvnw.cmd test -Dtest=YouyuBackendApplicationTests  # single class
```

H2 console (test profile only): `http://localhost:8080/h2-console`
JDBC URL: `jdbc:h2:mem:testdb`, user: `sa`, password: (empty)

## Build

```bash
mvnw.cmd clean package
```

## Seed data (profile `seed`)

Current demo-seed note:
- `application-seed.yml` now loads `seed/demo-expansion.sql` instead of the older `data-realistic-products.sql`.
- The expansion file provides the large demo catalog, fuller buyer lifecycles, denser admin dashboards, and support/message data used for the final demo overlay.
- Demo buyer login accounts remain `seedbuyer`-`seedbuyer4` / `user123` for compatibility, but their display-facing names and shops have been cleaned for presentation.
- For a fresh containerized demo reset, run `.\scripts\run-demo-fresh.ps1 -Detach`.

Loaded in order (see `application-seed.yml`):
1. `src/main/resources/schema.sql` — DDL (`CREATE TABLE IF NOT EXISTS`, idempotent)
2. `src/main/resources/seed/data.sql` — demo users, shops, categories, products
3. `src/main/resources/seed/seed-transactions.sql` — cart items, orders, fulfillments
4. `src/main/resources/seed/seed-zhangsan-spend.sql` — completed paid buyer orders for `zhangsan` (profile spend charts)
5. `src/main/resources/seed/data-realistic-products.sql` — additional product catalog samples
6. `src/main/resources/seed/seed-chat-data.sql` — message-center and chat demo data

Demo credentials (seed only):
- Admin: `admin` / `admin123`
- Admin staff: `superadmin`, `supportagent`, `reviewer`, `operator`, `orderadmin` / `admin123`
- User: `zhangsan` / `user123`
- Seed buyers: `seedbuyer`–`seedbuyer4` / `user123` (user IDs 1010–1013)

## Auth

**JWT**: `POST /api/auth/login` with `{ "loginId", "password" }` returns `{ token, user, privilege }`.

**Registration**: request an email code with `POST /api/auth/email-codes`, then
submit `POST /api/auth/register`. Registration returns the new user identity
without a JWT. Password recovery uses the same email-code endpoint with purpose
`reset_password`, followed by `POST /api/auth/password-reset`.

**Mock tokens** (dev/test only):
```
Authorization: Bearer mock-{userId}-{role}
# e.g. Bearer mock-1001-USER, Bearer mock-9001-ADMIN, or Bearer mock-9103-REVIEWER
```

`@LoginRequired` marks endpoints requiring authentication. Read current user via `AuthContextHolder.getUser()`.

## Architecture

See `CLAUDE.md` at the repository root for the full domain module map, request flow, and coding conventions.
