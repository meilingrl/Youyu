# Task: Launch Preparation Integration Verification

## Metadata

- ID: launch-preparation-integration-verification
- Status: completed
- Owner: main-agent
- Track: cross-cutting
- Depends on: launch-preparation-security-hardening; launch-preparation-privacy-compliance; launch-preparation-runtime-infrastructure; launch-preparation-container-deployment
- Priority: P0
- Planned date: 2026-06-04
- Completed date: 2026-06-04

## Objective

Review, integrate, verify, document, and archive the first launch-preparation wave after worker slices report completion.

## Background

Launch-preparation work spans shared configuration, legal UX, runtime infrastructure, deployment docs, and security evidence. Worker summaries are not proof of completion; main-agent review is required before task archival or publishing.

## Scope

- Review each worker diff for scope, secrets, locked-interface compliance, and documentation accuracy.
- Resolve narrow overlaps and integration defects.
- Run required backend/frontend checks.
- Exercise launch-specific checks where practical: config validation, health endpoints, Docker Compose config, scan commands, legal/consent UI.
- Update changelog, task completion notes, roadmap status notes if assumptions changed, API specs, and HTTP samples.
- Archive completed tasks and leave blocked/deferred items visible.

## Out of Scope

- Accepting worker output without diff review.
- Pushing or opening a PR unless the human asks.
- Declaring public production readiness from local or CI-only checks.

## Files to Read

- all `docs/08-tasks/active/launch-preparation-*.md`
- `CHANGELOG.md`
- changed files from every worker
- changed `docs/04-standards/*`
- changed `docs/06-http/*.http`
- changed `docs/09-api-spec/*.md`

## Allowed Changes

- narrow integration fixes in files already touched by accepted child tasks
- `CHANGELOG.md`
- `docs/08-tasks/active/launch-preparation-*.md`
- move completed task docs to `docs/08-tasks/archived/`
- matching API spec, HTTP smoke, and standards docs where workers missed required updates

## Implementation Plan

1. Review worker diffs and changed-file lists.
2. Confirm scope and locked interfaces.
3. Run automated checks and launch-specific validations.
4. Patch narrow integration issues.
5. Update documentation and completion notes.
6. Archive accepted child tasks and parent task if the first wave is complete.
7. Commit the verified wave and report blockers/deferred work.

## Risks

- Docker or external scan tools may not be installed locally.
- Worker changes can overlap in shared config or docs.
- Compliance and security evidence can be overstated if not clearly scoped to local/staging.

## Test Plan

- Backend: `cd backend; .\mvnw.cmd test`
- Frontend: `cd frontend; npm test`
- Frontend build: `cd frontend; npm run build`
- Diff hygiene: `git diff --check`
- Docker: `docker compose config` and optional `docker compose up -d --build` when available.
- Security/configuration: repository scan commands where available.
- Manual: legal/consent and health/deployment documentation review.

## Acceptance Criteria

- [x] Every worker slice has reviewed diffs and completion notes.
- [x] Automated checks pass or failures are documented with root cause.
- [x] Launch-specific validation evidence is recorded.
- [x] API docs, HTTP samples, and standards docs match changed behavior.
- [x] `CHANGELOG.md` has a prepend entry for the launch-preparation wave.
- [x] Completed task docs are archived; blockers/deferred work remain active or clearly recorded.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/04-standards/`
- [x] relevant files in `docs/06-http/`
- [x] relevant files in `docs/09-api-spec/`
- [x] task status and archive move

## Completion Notes

- Reviewed and integrated the security, privacy compliance, runtime infrastructure, and container deployment worker slices.
- Patched the CORS integration issue where empty local-safe mappings let Spring fall back to `allowedOrigins("*")` while credentials were enabled.
- Localized the new legal, Cookie, registration-consent, and privacy-rights UI copy to match the existing Chinese frontend language.
- Updated the existing backend main-chain registration test to include the now-required `agreedToTerms` contract.
- Verified backend `.\mvnw.cmd test` passed, 242 tests.
- Verified frontend `npm test` passed, 63 tests.
- Verified frontend `npm run build` passed, with existing Rollup/chunk-size warnings.
- Verified `docker compose config` passed with temporary example `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, and `APP_JWT_SECRET` values.
- Verified `git diff --check` passed.
- Full Docker build/start rehearsal remains blocked because Docker Desktop Linux engine is not running in this environment.
