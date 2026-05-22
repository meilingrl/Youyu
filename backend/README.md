# Backend

Spring Boot 3.3 backend for CampusMarket. Layered by domain: `controller / service / mapper / entity / config / filter / listener`.

## Run locally

**Prerequisite**: MySQL 8+ on `localhost:3306`, database `campus_market` created. Set credentials via `MYSQL_PASSWORD` env var or edit `application.yml`.

### Environment variables

| Name | Required | Default | Description |
|------|----------|---------|-------------|
| `MYSQL_PASSWORD` | local: optional, deploy: yes | `yinkaixin123` | MySQL password for the `campus_market` schema |
| `APP_JWT_SECRET` | local: optional, deploy: **yes** | committed dev secret | JWT signing secret (≥32 chars); falls back to a dev default when running with profile `dev` / `seed` / `test` |

**Production checklist**: `APP_JWT_SECRET` MUST be exported (≥32 chars) before booting any non-dev/seed/test profile. The application fails fast at startup if the committed dev default is detected under any other active profile.

**First-time setup** (schema + seed data):
```bash
# Windows
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=seed

# macOS / Linux
./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
```

**Normal run**:
```bash
mvnw.cmd spring-boot:run
```

Server runs at `http://localhost:8080`.

## Tests

Tests use H2 in-memory — no MySQL required.

```bash
mvnw.cmd test                                          # all tests
mvnw.cmd test -Dtest=CampusMarketBackendApplicationTests  # single class
```

H2 console (test profile only): `http://localhost:8080/h2-console`  
JDBC URL: `jdbc:h2:mem:testdb`, user: `sa`, password: (empty)

## Build

```bash
mvnw.cmd clean package
```

## Seed data (profile `seed`)

Loaded in order:
1. `src/main/resources/schema.sql` — DDL (`CREATE TABLE IF NOT EXISTS`, idempotent)
2. `src/main/resources/seed/data.sql` — demo users, shops, categories, products
3. `src/main/resources/seed/seed-transactions.sql` — cart items, orders, fulfillments
4. `src/main/resources/seed/data-perf-catalog.sql` — 4000 synthetic products (IDs 10000+)

Demo credentials (seed only):
- Admin: `admin` / `admin123`
- User: `zhangsan` / `user123`
- Seed buyers: `seedbuyer`–`seedbuyer4` / `user123` (user IDs 1010–1013)

## Auth

**JWT**: `POST /api/auth/login` with `{ "loginId", "password" }` returns `{ token, user, privilege }`.

**Mock tokens** (dev/test only):
```
Authorization: Bearer mock-{userId}-{role}
# e.g. Bearer mock-1001-USER  or  Bearer mock-9001-ADMIN
```

`@LoginRequired` marks endpoints requiring authentication. Read current user via `AuthContextHolder.getUser()`.

## Architecture

See `CLAUDE.md` at the repository root for the full domain module map, request flow, and coding conventions.
