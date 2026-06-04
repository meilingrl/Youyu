# Task: Launch Readiness Deployment Runbook

## Metadata

- ID: launch-readiness-deployment-runbook
- Status: active
- Owner: Codex
- Track: cross-cutting
- Depends on: launch-preparation roadmap L5; container deployment wave 1; runtime infrastructure wave 1
- Priority: P0
- Planned date: 2026-06-04
- Completed date:

## Objective

Produce and verify a repeatable deployment, rollback, and operator handoff path for the launch candidate.

## Background

The repository has Docker/Compose support and staging runtime configuration, but launch readiness still depends on an executable runbook, environment-variable inventory, CI/CD or manual release path, health checks, rollback steps, and known external production blockers.

## Current State

Completed in earlier launch-preparation waves:

- Backend and frontend Dockerfiles exist.
- `compose.yml` and `compose.demo.yml` define staging and demo rehearsal paths.
- `.env.example` documents required and optional runtime variables with placeholders.
- `application-staging.yml` separates production-like runtime settings from local defaults.
- `launch-foundation-runbook.md` documents start, health check, logs, backup/restore, scanning, performance smoke, and rollback rehearsal steps.
- CI validates backend tests, frontend tests/build, and Playwright smoke; security-scanning workflow exists.

Remaining work:

- Registry publishing, hosted deployment, TLS certificate issuance, secret-manager integration, and production rollback automation are external or future tasks.
- A fresh release-candidate deployment rehearsal should be recorded after final environment selection.

## Scope

- Verify backend and frontend images build from a clean checkout.
- Confirm Compose or equivalent staging deployment starts without local-only paths.
- Document required and optional environment variables for staging, demo, and production-like modes.
- Define deployment, health-check, rollback, redeploy, and log-inspection procedures.
- Record CI/CD gaps and external hosting, domain, HTTPS, registry, and secret-manager prerequisites.

## Out of Scope

- Purchasing hosting, domains, certificates, or registry services.
- Deploying to a real production cloud environment unless the human provides access.
- Large infrastructure-as-code migration.
- Database backup drill details owned by the backup/restore task.

## Files to Read

- `docs/04-standards/operations-and-deployment.md`
- `docs/04-standards/launch-foundation-runbook.md`
- `docs/04-standards/launch-foundation-containerization.md`
- `docs/04-standards/launch-foundation-configuration.md`
- `compose.yml`
- `compose.demo.yml`
- `.env.example`
- `backend/Dockerfile`
- `frontend/Dockerfile`
- `frontend/nginx/default.conf`
- `.github/workflows/`

## Allowed Changes

- Dockerfiles, Compose files, Nginx config, environment examples, and CI/CD workflow files directly needed for deployment readiness.
- Launch runbook and deployment standards.
- Focused docs or smoke scripts for release and rollback rehearsal.
- `CHANGELOG.md` and this task lifecycle record.

## Implementation Plan

1. Build and inspect backend/frontend images and Compose configuration.
2. Reconcile environment-variable docs with actual runtime requirements.
3. Write or refine release, health-check, rollback, and redeploy runbook steps.
4. Run a local or staging-like rehearsal where available and record blockers.

## Risks

- Docker Desktop or external registry access may be unavailable locally.
- Demo seed settings can be mistaken for production defaults.
- Rollback docs can overstate database rollback safety.

## Test Plan

- Backend: `cd backend; .\mvnw.cmd test` if runtime code changes.
- Frontend: `cd frontend; npm test` and `npm run build` if frontend deployment config changes.
- API validation: health endpoint and smoke path after Compose startup.
- Manual: `docker compose config`, image build, startup, health check, log inspection, rollback rehearsal where environment allows.

## Acceptance Criteria

- [x] A clean checkout has documented runbook steps to build and start the staging rehearsal.
- [x] Required secrets and environment variables are listed without committed secret values.
- [x] Deployment, health check, rollback, and redeploy procedures are documented for the current Compose rehearsal path.
- [x] Demo-only seed behavior is clearly separated from production-like deployment.
- [x] CI/CD or manual release gaps are assigned as blockers or deferred items.
- [ ] Final release-candidate deployment evidence is recorded for the selected real environment.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] deployment/runbook standards
- [ ] relevant smoke docs if deployment endpoints or ports change
- [ ] task status and archive move

## Completion Notes

- 2026-06-04 sync: repository Compose entry is `compose.yml`; Nginx config path is `frontend/nginx/default.conf`.
- 2026-06-04 sync: deployment foundation is documented and CI exists, but real hosted deployment, registry, TLS, and approval gates remain external blockers.
