# Task: Launch Foundation Containerization

## Metadata

- ID: launch-foundation-containerization
- Status: completed
- Owner: worker-b
- Track: cross-cutting
- Depends on: launch-foundation-scope-and-environments
- Priority: high
- Planned date: 2026-05-30
- Completed date: 2026-05-30

## Objective

Add a single-machine staging rehearsal stack with explicit demo seed overlay.

## Scope

- backend and frontend Dockerfiles
- root `compose.yml` and `compose.demo.yml`
- Nginx SPA fallback and `/api` proxy
- optional HTTPS Nginx template and certificate mount documentation
- container ignore files as needed

## Out of Scope

- application configuration ownership
- Actuator implementation, remote deployment, certificates

## Allowed Changes

- `backend/Dockerfile`, `backend/.dockerignore`
- `frontend/Dockerfile`, `frontend/.dockerignore`
- `frontend/nginx/`
- `compose.yml`, `compose.demo.yml`
- containerization-focused documentation

## Acceptance Criteria

- [ ] Default Compose uses staging and does not enable seed.
- [ ] Demo overlay enables staging and seed explicitly.
- [ ] Nginx serves the SPA and proxies `/api`.
- [ ] HTTPS support is a non-default template.

## Completion Notes

Added images, Compose files, Nginx proxy, and HTTPS template. Default and demo
stacks were built and started locally. Added optional `BACKEND_PORT` so a
rehearsal can avoid an occupied host port while keeping `8080` as the default.
