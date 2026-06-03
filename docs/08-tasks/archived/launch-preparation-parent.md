# Task: Launch Preparation Program

## Metadata

- ID: launch-preparation-parent
- Status: active
- Owner: main-agent
- Track: cross-cutting
- Depends on: feature-polish closeout accepted; launch-preparation roadmap L0
- Priority: P0
- Planned date: 2026-06-04
- Completed date:

## Objective

Coordinate the transition from feature closeout into launch preparation, keeping security, compliance, runtime infrastructure, deployment, monitoring, backup, and release verification separate from new product feature expansion.

## Background

`docs/05-roadmap/current/stage-roadmap.md` declares that F0-F6 feature work and the payment upgrade are complete within the current repository scope. `docs/05-roadmap/current/launch-preparation-roadmap.md` now owns the L0-L7 launch-preparation route.

Repository evidence also shows that several launch-foundation assets already exist:

- `application-staging.yml`
- root `compose.yml` and `compose.demo.yml`
- backend and frontend containerization assets
- security-scanning workflow
- launch-foundation standards under `docs/04-standards/`

This program must consolidate that baseline and fill the remaining launch-preparation gaps without reopening the completed feature roadmap.

## Scope

- Establish L0 scope freeze and a single active task set for launch preparation.
- Reconcile stale active feature-polish task records with archived completed records.
- Execute the first P0 launch-preparation wave:
  - L1 security hardening and scan evidence
  - L2 privacy compliance and user-rights baseline
  - L3 runtime infrastructure, health, logging, backup, and lifecycle baseline
  - L5 containerization/deployment path and rollback documentation
- Keep L4 performance and L6 monitoring as visible follow-up work unless low-risk evidence collection is needed for P0 acceptance.
- Keep all worker scopes disjoint and require main-agent review before archival.

## Out of Scope

- New product features, marketing expansion, notification expansion, or personalization expansion.
- Real production launch approval.
- Production secret provisioning, certificate issuance, ICP filing, or cloud account operations.
- Destructive database migration or schema rewrite.
- Replacing the current Vue/Spring/JDBC architecture.
- Reintroducing `AdminDataStore`.

## Child Tasks

- [x] `launch-preparation-l0-scope-freeze`
- [x] `launch-preparation-security-hardening`
- [x] `launch-preparation-privacy-compliance`
- [x] `launch-preparation-runtime-infrastructure`
- [x] `launch-preparation-container-deployment`
- [x] `launch-preparation-integration-verification`

## Locked Interfaces

- Keep API responses wrapped in `ApiResponse<T>`.
- Keep backend runtime on Spring Boot 3.3, Java 17, JDBC mapper style, and MySQL/H2 test compatibility.
- Keep frontend runtime on Vue 3, Vite, Pinia, Vue Router, Axios, and Element Plus.
- Keep dev/test mock token behavior limited to dev/test modes.
- `staging` is a production-like rehearsal profile, not production approval.
- Default staging startup must not activate `seed`; demo data stays behind `compose.demo.yml`.
- Secrets, generated credentials, certificates, reports, backups, and local `.env` files must not be committed.
- No worker may archive its own task; main-agent review and verification are required.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/04-standards/development-process.md`
- `docs/05-roadmap/current/stage-roadmap.md`
- `docs/05-roadmap/current/launch-preparation-roadmap.md`
- `docs/04-standards/operations-and-deployment.md`
- `docs/03-architecture/data-management-and-privacy.md`
- `docs/03-architecture/performance-and-scalability.md`
- `frontend/README.md`
- `backend/README.md`
- each child task before dispatch

## Allowed Changes

- `docs/08-tasks/active/launch-preparation-*.md`
- `docs/08-tasks/archived/launch-preparation-*.md`
- stale active `feature-polish-*.md` records only as part of L0 lifecycle reconciliation
- launch-preparation-owned backend/frontend/config/docs files listed by child tasks
- `docs/06-http/*.http`, `docs/09-api-spec/*.md`, and `CHANGELOG.md` where changed behavior or contracts require updates

## Implementation Plan

1. Complete L0 scope freeze and task lifecycle cleanup.
2. Dispatch L1, L2, L3, and L5 workers with disjoint ownership.
3. Review each worker diff for scope, locked-interface compliance, secrets, and runtime defaults.
4. Patch narrow integration issues in the main worktree.
5. Run backend/frontend checks plus launch-specific scans or rehearsal commands where practical.
6. Update changelog, roadmap status notes, task completion notes, and archive accepted tasks.

## Risks

- Stale active feature-polish docs can confuse worker scope if not reconciled first.
- Security, compliance, deployment, and runtime infrastructure touch shared configuration; ownership must remain explicit.
- Some P0 roadmap items require external production decisions or credentials and must be recorded as blockers rather than faked.
- Local staging rehearsal cannot prove production readiness.

## Test Plan

- Backend: `cd backend; .\mvnw.cmd test`
- Frontend: `cd frontend; npm test`
- Frontend build: `cd frontend; npm run build`
- Diff hygiene: `git diff --check`
- Security/configuration: run available repository scan workflows or local equivalents where practical.
- Deployment rehearsal: run Docker Compose checks when Docker is available.
- Manual: verify legal links, consent flows, health endpoints, and launch runbook commands where implemented.

## Acceptance Criteria

- [ ] L0 documents confirm the project is ready for launch-preparation execution, not production launch.
- [ ] Each P0 launch-preparation direction has exactly one active task document.
- [ ] Stale feature-polish active records no longer appear as current execution tasks.
- [ ] Worker-owned changes are reviewed and verified before archival.
- [ ] All changed contracts and operator docs are updated.
- [ ] Blockers, deferred items, and external-production prerequisites remain explicit.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] relevant files in `docs/04-standards/`
- [ ] relevant files in `docs/06-http/`
- [ ] relevant files in `docs/09-api-spec/`
- [ ] roadmap status note if launch-preparation stage assumptions change
- [ ] task status and archive move

## Completion Notes

- L0 completed on 2026-06-04. The repository is ready for launch-preparation development and worker dispatch, but not ready for public production launch.
- Wave 1 completed on 2026-06-04. Security, privacy compliance, runtime infrastructure, and container deployment rehearsal baselines were integrated and verified.
- Public production launch remains blocked by external deployment work: HTTPS/certificate provisioning, real production CORS origins, production secret injection, payment-provider production approval, Docker engine/startup rehearsal, backup restore drill, monitoring/alerting ownership, and L7 release acceptance.
