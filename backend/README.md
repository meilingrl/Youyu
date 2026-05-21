# Backend

`backend/` is the Spring Boot scaffold for `CampusMarket`.

## Current goals

- Keep the project startable
- Maintain a clear layered structure:
  `controller / service / mapper / entity / config / filter / listener`
- Provide a unified API response shape and global exception handling
- Reserve a basic authentication scaffold and login interception
- Prepare standard module folders for:
  `auth / user / product / shop / order / payment / admin / report`

## Not in scope yet

- Full business implementation
- Real payment integration
- Final database ORM binding details

## Run locally

1. Enter `backend/`
2. **First time** (empty H2 file) or after you deleted `~/.campusmarket/h2/`, start **once** with demo + perf seed data: `mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=seed` (Windows) or `./mvnw spring-boot:run -Dspring-boot.run.profiles=seed` (macOS / Linux).
3. Normal daily runs (keep your DB changes): `mvnw.cmd spring-boot:run` or `./mvnw spring-boot:run` — only `schema.sql` runs; **no** bulk re-import of `seed/data.sql`.

## Basic verification

- Compile and test: `mvnw.cmd test`
- The project includes a minimal Spring Boot context test for CI verification

## Persistence

- Default datasource is **H2 on disk**: `jdbc:h2:file:${user.home}/.campusmarket/h2/campusmarket` (see `application.yml`). Data survives restarts; only delete that folder if you want a completely fresh file DB.
- On each start, Spring runs **`schema.sql` only** (`CREATE TABLE IF NOT EXISTS`, cheap and idempotent). Demo users and the large perf product catalog live in `src/main/resources/seed/` and are **not** loaded unless you enable profile **`seed`** (see Run locally above), so normal restarts do not replay thousands of `INSERT`s.
- Core business tables (users, products, orders, etc.) are accessed through **JDBC** (`JdbcTemplate` + `@Primary` mappers), not the old in-memory admin demo store.

## Seed data layout (profile `seed`)

- `seed/data.sql` — base users (including seed buyers `1010`–`1013`), shops (`4010`–`4011`), categories, core products (`3010`–`3014`), etc.
- `seed/seed-transactions.sql` — **phase 1** JDBC fixtures: `cart_items` (`9101`–`9110`) and `orders` + `order_items` + `order_fulfillments` (`8001`–`8010`), idempotent `DELETE` at top. No `payment_records` / `refund_records` yet.
- `seed/data-perf-catalog.sql` — large on-sale product list for list/API stress (ids `10000+`).

## Performance test catalog (SQL in repo)

- File `src/main/resources/seed/data-perf-catalog.sql` holds **hundreds of synthetic on-sale products** (reserved ids `10000+`) plus `product_media` rows. It starts with `DELETE` for that id range so re-runs with profile `seed` stay idempotent for that id block.
- With profile `seed`, Spring loads `data.sql`, then `seed-transactions.sql`, then `data-perf-catalog.sql` (see `application-seed.yml`).
- Regenerate or resize the file: from repo root run `node scripts/generate-perf-catalog-sql.mjs [count]` (default `3500`, max `9000`). Commit the updated `seed/data-perf-catalog.sql` when you change volume. The checked-in file is currently **4000** rows (ids `10000`–`13999`).

## Auth scaffold notes

- **Unified login**: `POST /api/auth/login` with JSON body `{ "loginId", "password" }`. The backend resolves **admin** vs **campus user** by `loginId` and returns `{ token, role, user }`. `POST /api/admin/auth/login` uses the same logic for compatibility.
- Mock login is supported with:
  `Authorization: Bearer mock-{userId}-{role}`
- Local debugging also supports:
  `X-User-Id` and `X-User-Role`
- `@LoginRequired` marks endpoints that require login or admin permission

Examples:

- User: `Authorization: Bearer mock-1001-USER`
- Admin: `Authorization: Bearer mock-9001-ADMIN`

Demo passwords (loaded only with profile `seed`, from `seed/data.sql`): admin `admin` / `admin123`; campus users `zhangsan` (or `20240001` / email) / `user123`. Extra seed buyers: `seedbuyer` … `seedbuyer4` / `user123` (users `1010`–`1013`; mock tokens `mock-1010-USER` … `mock-1013-USER`).
