# Launch Foundation Containerization Rehearsal Guide

This guide describes the single-machine staging rehearsal stack. It is not a
production deployment workflow.

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

`DB_HOST` and the internal `DB_PORT` are fixed by Compose as `db` and `3306`.
The host-side database port can be changed with `DB_PORT`; the frontend HTTP
port can be changed with `HTTP_PORT`; the host-side backend port can be changed
with `BACKEND_PORT`. Their defaults remain `3306`, `80`, and `8080`.

## Staging Rehearsal

The default startup activates only the `staging` Spring profile:

```bash
docker compose up -d --build
```

The backend initializes the schema through the existing Spring SQL initializer.
It does not load demo seed data.

## Explicit Demo Seed Overlay

Demo data is opt-in. Use the overlay only for local rehearsal, manual demos, or
smoke validation:

```bash
docker compose -f compose.yml -f compose.demo.yml up -d --build
```

The overlay activates `staging,seed`, so the backend initializes both the
schema and demo records. Do not use this overlay for a real production
deployment.

## Health Checks

The backend container health check targets `/actuator/health`. The endpoint is
introduced by the separate logging-and-health foundation task. Until that task
lands, the backend container is expected to remain unhealthy.

The existing `/api/health` endpoint remains available for application smoke
checks.

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

## Useful Commands

```bash
docker compose ps
docker compose logs -f backend
docker compose down
docker compose down -v  # destructive: also removes the local rehearsal DB
```
