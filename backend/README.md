# Backend

Spring Boot 3.3 backend for Youyu. Layered by domain: `controller / service / mapper / entity / config / filter / listener`.

## Run locally

**Prerequisite**: MySQL 8+ on `localhost:3306`, database `youyu` created. Set credentials via `MYSQL_PASSWORD` env var or edit `application.yml`.

### Environment variables

| Name | Required | Default | Description |
|------|----------|---------|-------------|
| `MYSQL_PASSWORD` | local: optional, deploy: yes | `yinkaixin123` | MySQL password for the `youyu` schema |
| `APP_JWT_SECRET` | local: optional, deploy: **yes** | committed dev secret | JWT signing secret (≥32 chars); falls back to a dev default when running with profile `dev` / `seed` / `test` |
| `APP_MAIL_HOST` | runtime email delivery: **yes** | none | SMTP server host |
| `APP_MAIL_PORT` | runtime email delivery: **yes** | none | SMTP server port |
| `APP_MAIL_USERNAME` | runtime email delivery: **yes** | none | SMTP login username |
| `APP_MAIL_PASSWORD` | runtime email delivery: **yes** | none | SMTP login password or provider authorization code |
| `APP_MAIL_FROM` | runtime email delivery: **yes** | none | Verified sender address |
| `APP_MAIL_SSL_ENABLED` | runtime email delivery: **yes** | none | Set `true` when the provider requires SMTP SSL |

**Production checklist**: `APP_JWT_SECRET` MUST be exported (≥32 chars) before booting any non-dev/seed/test profile. The application fails fast at startup if the committed dev default is detected under any other active profile.

The public registration and password-reset email-code endpoints require SMTP
delivery. The `test` profile uses a deterministic fake sender and never opens a
network connection. Do not commit provider credentials, recipient addresses, or
verification codes.

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
