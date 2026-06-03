# Launch Foundation Configuration

This document defines the configuration boundary for the launch-foundation staging exercise. It does not claim that the application is ready for public production traffic.

## Environment Purpose

The `staging` Spring profile is a production-like rehearsal profile for local or isolated test environments. It keeps the current Spring Boot schema initialization path while requiring deployment credentials to come from environment variables.

Local development remains unchanged:

- no active profile: existing local defaults remain available
- `seed` profile: demo data remains opt-in for local development and tests
- `staging` profile: environment variables are required and only `schema.sql` is loaded by default

The default staging startup must not activate `seed`. A demo startup may explicitly combine `staging,seed` when stable sample users and products are needed for a rehearsal.

## Required Variables

Use the repository root `.env.example` as a name-only template. Create a local `.env` file with generated values and do not commit it.

| Variable | Purpose |
| --- | --- |
| `DB_HOST` | MySQL host name |
| `DB_PORT` | MySQL port |
| `DB_NAME` | MySQL schema name |
| `DB_USERNAME` | Application database user |
| `DB_PASSWORD` | Application database password |
| `MYSQL_ROOT_PASSWORD` | MySQL container root password for environment bootstrap and backup operations |
| `APP_JWT_SECRET` | JWT signing secret; use a generated value of at least 32 characters |

The application staging profile consumes the database variables and `APP_JWT_SECRET`. `MYSQL_ROOT_PASSWORD` is reserved for environment bootstrap and operational scripts; the application must not use the MySQL root account.

Optional staging runtime variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_POOL_MAX_SIZE` | `10` | Maximum Hikari pool connections |
| `DB_POOL_MIN_IDLE` | `2` | Minimum idle Hikari connections |
| `DB_CONNECTION_TIMEOUT_MS` | `30000` | Maximum wait for a pooled connection |
| `DB_VALIDATION_TIMEOUT_MS` | `5000` | Maximum wait for connection validation |
| `DB_IDLE_TIMEOUT_MS` | `600000` | Idle connection retirement window |
| `DB_MAX_LIFETIME_MS` | `1800000` | Maximum connection lifetime; keep below the MySQL/network idle close window |
| `DB_LEAK_DETECTION_THRESHOLD_MS` | `0` | Disabled by default; enable only during focused investigation |
| `DB_INITIALIZATION_FAIL_TIMEOUT_MS` | `1` | Fail startup quickly when the staging database is unavailable |

## Startup Modes

Default staging rehearsal:

```bash
docker compose up -d
```

Expected database behavior: initialize tables through `classpath:schema.sql`; do not insert demo users, products, or transactions.

Explicit demo rehearsal:

```bash
docker compose -f compose.yml -f compose.demo.yml up -d
```

Expected database behavior: combine the `staging` and `seed` Spring profiles so the existing demo SQL files are loaded intentionally.

The Compose files are owned by the containerization task. They must pass the required variables into the backend and keep demo seed loading behind the explicit overlay.

## Safety Boundaries

- Do not commit `.env`, generated secrets, credentials, tokens, or certificates.
- Do not activate `seed` in staging by default.
- Do not load demo data in a real production environment.
- Do not use the committed development JWT fallback with `staging`; startup must fail when `APP_JWT_SECRET` is absent.
- Do not treat staging as production-ready. The current payment gateway is `MOCK`, which blocks real production use.

## Runtime Validation

After the backend starts with the `staging` profile:

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/actuator/health
```

Expected behavior:

- `/api/health` returns only service status, database status, and the trace ID envelope.
- `/actuator/health` exposes aggregate `health`, `db`, and `diskSpace` component statuses without detailed connection properties.
- `/actuator/metrics` and `/actuator/prometheus` remain unavailable during the launch-foundation rehearsal.
