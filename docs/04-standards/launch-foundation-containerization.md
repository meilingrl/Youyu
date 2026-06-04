# Launch Foundation Containerization Rehearsal Guide

This guide describes the single-machine staging rehearsal stack. It is not a
production deployment workflow, and the `staging` profile is not production
approval.

## Services

The root `compose.yml` starts:

- `db`: MySQL 8.4 with a persistent named volume
- `backend`: Spring Boot application built from `backend/Dockerfile`
- `frontend`: Vite production build served by Nginx

Nginx serves the SPA, falls back to `index.html` for client-side routes, and
proxies same-origin `/api/` requests to the backend container.

## Required Environment Variables

Export these variables or place them in an untracked root `.env` file:

```dotenv
DB_NAME=youyu
DB_USERNAME=youyu
DB_PASSWORD=replace-with-a-staging-password
MYSQL_ROOT_PASSWORD=replace-with-a-root-password
APP_JWT_SECRET=replace-with-a-random-secret-at-least-32-characters
```

Do not commit `.env`, generated credentials, certificates, reports, backups, or
other environment-specific secrets.

`DB_HOST` and the internal `DB_PORT` are fixed by Compose as `db` and `3306`.
The host-side database port can be changed with `DB_PORT`; the frontend HTTP
port can be changed with `HTTP_PORT`; the host-side backend port can be changed
with `BACKEND_PORT`. Their rehearsal defaults are `13306`, `18080`, and
`18081`, avoiding common local-development ports while remaining configurable.

Optional integrations are disabled by default. Compose forwards the optional
Alipay sandbox, SMTP, Meilisearch, Amap, and logistics tracking variables from
`.env.example` into the relevant build or runtime container. Leaving those
values empty must preserve the default mock, fallback, or disabled behavior.

## Staging Rehearsal

The default startup activates only the `staging` Spring profile:

```bash
docker compose config
docker compose up -d --build
```

The backend initializes the schema through the existing Spring SQL initializer.
It does not load demo seed data.

## Explicit Demo Seed Overlay

Demo data is opt-in. Use the overlay only for local rehearsal, manual demos, or
smoke validation:

```bash
docker compose -f compose.yml -f compose.demo.yml config
docker compose -f compose.yml -f compose.demo.yml up -d --build
```

The overlay activates `staging,seed`, so the backend initializes both the
schema and demo records. Do not use this overlay for a real production
deployment.

## Health Checks

The backend container health check targets `/actuator/health`. It reports
application readiness and database readiness through the minimal Actuator
health endpoint. Only health is exposed.

The existing `/api/health` endpoint remains available for application smoke
checks.

Expected checks after startup:

```bash
docker compose ps
curl http://localhost:18080/api/health
curl http://localhost:18081/actuator/health
```

The first `curl` validates the frontend container, Nginx `/api/` proxy, and
backend application health route. The second validates the backend container
health target directly.

## CI/CD Boundary

The current GitHub Actions workflow is CI only: backend tests, frontend unit
tests/build, and Playwright smoke. It does not push images, deploy to a server,
or provision cloud infrastructure. A future deployment workflow must add image
registry credentials, deployment host/cluster credentials, environment secret
injection, HTTPS certificate handling, post-deploy health checks, and rollback
approval outside this repository's current committed CI.

## Optional HTTPS Template

HTTP remains the default for local staging rehearsal. For a self-hosted HTTPS
rehearsal:

1. Copy `frontend/nginx/https.conf.template` to a local untracked Nginx config.
2. Replace `youyu.example.com` with the rehearsal domain.
3. Bind-mount the config to `/etc/nginx/conf.d/default.conf`.
4. Bind-mount certificate files to `/etc/nginx/certs/fullchain.pem` and
   `/etc/nginx/certs/privkey.pem`.
5. Publish container port `443` in an environment-specific Compose overlay.

Certificates, domains, and environment-specific mounts are intentionally not
committed. A real production deployment must use HTTPS and must replace the
current mock payment gateway.

## Rollback Boundary

For this single-machine rehearsal, rollback means returning to a previously
verified Git revision and rebuilding local containers without deleting data:

```bash
docker compose build --pull
docker compose up -d --force-recreate
docker compose ps
curl http://localhost:18080/api/health
```

If the new revision cannot become healthy, return to the prior revision and run
the same commands. Database rollback is not automatic; restore from a validated
backup into a separate restore database first, then promote only after manual
approval. `docker compose down -v` is a destructive local reset, not a normal
rollback step.

## Useful Commands

```bash
docker compose ps
docker compose logs -f backend
docker compose down
docker compose down -v  # destructive: also removes the local rehearsal DB
```
