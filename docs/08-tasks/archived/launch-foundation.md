# Task: Launch Foundation

## Metadata

- ID: launch-foundation
- Status: completed
- Owner: main-agent
- Track: cross-cutting
- Depends on: current local CI baseline, JWT secret guard baseline
- Priority: high
- Planned date: 2026-05-30
- Completed date: 2026-05-30

## Objective

Build a reproducible staging rehearsal baseline without presenting the current mock-payment system as production-ready.

## Background

The project is in a feature-closeout plus launch-preparation stage. This task covers low-conflict foundation work that can proceed in parallel with product completion.

## Scope

- staging configuration and environment documentation
- Docker Compose, backend/frontend images, Nginx proxy, optional HTTPS template
- MySQL full backup and guarded restore rehearsal
- dependency and secret scan CI reporting
- k6 performance baseline scripts
- minimal Actuator health, DB health, MDC traceId, console-first logs
- integration runbook and rehearsal evidence

## Out of Scope

- Redis, rate limiting, OSS, Flyway, Prometheus, Grafana
- remote deployment workflows
- privacy business features, payment upgrades, security final acceptance, L7 release acceptance

## Child Tasks

- [x] `launch-foundation-scope-and-environments`
- [x] `launch-foundation-production-config`
- [x] `launch-foundation-containerization`
- [x] `launch-foundation-backup-restore`
- [x] `launch-foundation-security-scanning`
- [x] `launch-foundation-performance-baseline`
- [x] `launch-foundation-logging-and-health`
- [x] `launch-foundation-runbook-and-integration`

## Locked Interfaces

- Default: `docker compose up -d`, staging profile, schema only.
- Demo: `docker compose -f compose.yml -f compose.demo.yml up -d`, staging plus seed profiles.
- Existing `schema.sql` remains Spring Boot initialized; no Flyway.
- Frontend uses Nginx same-origin `/api` proxy.
- `/api/health` stays compatible; `/actuator/health` is added with minimum exposure.
- Logs stay console-first; traceId enters and leaves MDC per request.
- Backup retention defaults to 7 days; restore rejects the `youyu` database target.
- CI blocks secret leaks; dependency findings are recorded but do not block this wave.

## Risks

- Mock payment remains a production blocker.
- A foundation task must not drift into product feature work.
- Compose verification depends on Docker availability on the execution host.

## Acceptance Criteria

- [x] Every child task is main-agent reviewed before archival.
- [x] Default and demo Compose modes are documented and verified when Docker is available.
- [x] Backup restore, scanning and k6 smoke commands are reproducible.
- [x] Deferred work and production blockers are explicit.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes

Completed the staging rehearsal foundation in the isolated
`codex/launch-foundation` worktree. The result is buildable, startable,
health-checkable, backup-restorable, and scan-ready without claiming production
release readiness. See `docs/04-standards/launch-foundation-runbook.md`.
