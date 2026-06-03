# Task: Launch Preparation Container Deployment

## Metadata

- ID: launch-preparation-container-deployment
- Status: completed
- Owner: worker-deploy
- Track: cross-cutting
- Depends on: launch-preparation-l0-scope-freeze
- Priority: P0
- Planned date: 2026-06-04
- Completed date: 2026-06-04

## Objective

Verify and complete the containerized staging rehearsal path, deployment documentation, and rollback boundaries for launch preparation.

## Background

The repository already contains root Compose files, backend Dockerfile, frontend Docker/Nginx assets, and launch-foundation containerization documentation. The launch roadmap still requires a repeatable new-machine deployment path, production environment variable boundaries, CI/CD deployment notes, database initialization boundaries, and rollback procedure.

## Scope

- Review backend and frontend Dockerfiles plus frontend Nginx configuration.
- Verify Compose defaults keep staging and seed/demo behavior separated.
- Ensure environment-variable documentation matches actual Compose/application behavior.
- Add or update rollback/deployment runbook steps.
- Improve CI/CD deployment documentation without pushing or provisioning a real deployment target.
- Run Docker Compose rehearsal if Docker is available; otherwise document the blocker.

## Out of Scope

- Pushing images to a registry.
- Deploying to a real server or cloud.
- Issuing TLS certificates.
- Adding Kubernetes unless already required by repository docs.
- Running destructive `docker compose down -v` except as a documented manual option.

## Files to Read

- `compose.yml`
- `compose.demo.yml`
- `.env.example`
- `backend/Dockerfile`
- `frontend/Dockerfile`
- `frontend/nginx.conf` or `frontend/nginx/*`
- `docs/04-standards/launch-foundation-containerization.md`
- `docs/04-standards/launch-foundation-runbook.md`
- `.github/workflows/ci.yml`

## Allowed Changes

- `compose.yml`
- `compose.demo.yml`
- `.env.example`
- backend/frontend Docker and Nginx files
- launch-foundation deployment/runbook docs
- CI workflow documentation only unless a safe build/test job change is required
- this task document

## Implementation Plan

1. Inspect container and Compose assets for mismatch with documentation.
2. Patch configuration or docs so default staging and demo overlay behavior are clear and reproducible.
3. Add rollback and health-check steps to the runbook where missing.
4. Run or attempt a Compose config/build rehearsal when Docker is available.
5. Record external deployment blockers.

## Risks

- Compose health checks may depend on actuator behavior owned by runtime infrastructure.
- CI/CD docs can imply a deployment exists when the repo only has test CI.
- Local machine Docker availability may block full rehearsal.

## Test Plan

- Backend: not expected unless Docker changes affect build context.
- Frontend: `cd frontend; npm run build` if Docker/Nginx behavior changes frontend assets.
- API validation: not expected.
- Manual: `docker compose config`; `docker compose up -d --build` when Docker is available.

## Acceptance Criteria

- [ ] Container/Compose behavior matches documentation.
- [ ] Default staging startup does not load demo seed data.
- [ ] Demo overlay behavior is explicit and opt-in.
- [ ] Deployment and rollback steps are executable as a staging rehearsal guide.
- [ ] Missing external production deployment pieces are listed as blockers.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] relevant standards/runbook docs
- [ ] task status and archive move

## Completion Notes

- Updated Compose so the staging backend container receives the optional SMTP,
  Meilisearch, Amap, and logistics tracking environment variables already
  documented in `.env.example`; defaults remain disabled or empty.
- Added frontend Docker build args for optional public Amap browser keys, while
  preserving the default no-map build.
- Confirmed `docker compose config` resolves default startup to
  `SPRING_PROFILES_ACTIVE=staging` and the demo overlay to
  `SPRING_PROFILES_ACTIVE=staging,seed`; default staging does not activate
  `seed`.
- Updated the containerization guide and runbook to state that `staging` is a
  production-like rehearsal profile, not production approval; demo data remains
  explicitly opt-in through `compose.demo.yml`.
- Added executable rehearsal checks, non-destructive rollback boundaries,
  CI/CD deployment notes, and external production blockers.
- Docker CLI and Compose are installed, but full `docker compose build` /
  `docker compose up` rehearsal was blocked because the Docker Desktop Linux
  engine was not running (`npipe:////./pipe/dockerDesktopLinuxEngine` missing).
- Main-agent integration updated `CHANGELOG.md`.
- Main-agent integration verified `docker compose config` with temporary
  example `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, and `APP_JWT_SECRET` values.
  Full build/start remains blocked until the Docker Desktop Linux engine is
  running.
