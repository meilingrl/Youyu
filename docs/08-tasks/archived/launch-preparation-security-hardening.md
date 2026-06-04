# Task: Launch Preparation Security Hardening

## Metadata

- ID: launch-preparation-security-hardening
- Status: active
- Owner: worker-security
- Track: cross-cutting
- Depends on: launch-preparation-l0-scope-freeze
- Priority: P0
- Planned date: 2026-06-04
- Completed date:

## Objective

Close the first launch-preparation security gaps that can be handled inside the repository, and document external production security prerequisites that cannot be completed locally.

## Background

The repository already has JWT default-secret fail-fast behavior outside dev/seed/test, a security-scanning workflow, and a staging profile. The launch roadmap still requires production-safe CORS, input-validation review, SQL-injection review evidence, permission regression evidence, sensitive-data masking review, and dependency/config scan evidence.

## Scope

- Audit and harden CORS behavior for staging/production-like profiles.
- Review JWT, mock-token, and payment mock exposure boundaries.
- Review dynamic SQL usage for parameterization and safe sort-field handling.
- Add or update security smoke documentation and executable HTTP/API checks where useful.
- Update security-scanning/runbook docs with concrete commands and limitations.
- Record any remaining external-production blockers such as HTTPS certificate provisioning.

## Out of Scope

- Obtaining HTTPS certificates or configuring a real cloud gateway.
- Replacing the authentication architecture.
- Adding broad new permission domains not already represented in the codebase.
- Changing production payment provider behavior beyond documenting current mock/sandbox boundaries.

## Files to Read

- `backend/src/main/java/com/youyu/backend/config/`
- `backend/src/main/java/com/youyu/backend/filter/`
- `backend/src/main/java/com/youyu/backend/common/auth/`
- `backend/src/main/java/com/youyu/backend/mapper/`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-staging.yml`
- `.github/workflows/security-scanning.yml`
- `.gitleaks.toml`
- `.gitleaksignore`
- `docs/04-standards/operations-and-deployment.md`
- `docs/04-standards/launch-foundation-security-scanning.md`

## Allowed Changes

- backend security/config classes and tests directly related to CORS, JWT/mock-token/payment exposure, SQL/sort safety, or sensitive response/log handling
- `backend/src/main/resources/application*.yml`
- `.github/workflows/security-scanning.yml`
- `docs/04-standards/launch-foundation-security-scanning.md`
- `docs/06-http/*security*.http` or relevant existing smoke files
- this task document

## Implementation Plan

1. Inspect current CORS, JWT guard, mock-token, and payment-gateway configuration.
2. Patch production-like defaults only where repository evidence shows a real gap.
3. Add focused tests or smoke commands for any changed behavior.
4. Review dynamic SQL and record remaining findings.
5. Update security documentation with exact commands, blockers, and deferred external work.

## Risks

- Over-tightening local dev CORS and breaking Vite proxy flows.
- Treating documentation-only HTTPS guidance as completed production HTTPS.
- Broad SQL refactors can destabilize unrelated mappers.

## Test Plan

- Backend: `cd backend; .\mvnw.cmd test`
- Frontend: not expected unless CORS docs require frontend changes.
- API validation: security smoke requests for auth-protected/admin endpoints where updated.
- Manual: confirm staging/prod-like configuration requires explicit allowed origins and secrets.

## Acceptance Criteria

- [x] Production-like CORS and secret behavior is explicit and testable.
- [x] Security scans have documented commands and known limitations.
- [x] No dev/test mock-token or mock-payment behavior is accidentally promoted to production-like profiles.
- [x] SQL-injection and permission-review findings are recorded with follow-up blockers where needed.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [x] relevant standards/runbook docs
- [x] task status and archive move

## Completion Notes

- Added explicit CORS configuration and startup validation. Production-like
  profiles must set explicit origins and cannot use wildcard CORS origins.
- Added mock-auth gating so local mock Bearer *** and header-auth are
  accepted only in dev/seed/test/default local modes.
- Added a mock-payment exposure guard and staging defaults that disable local
  mock payment.
- Added focused tests for CORS validation, mock-token boundaries, mock-payment
  fail-fast behavior, and mock gateway bean exposure.
- Added `docs/06-http/security-hardening.http` for CORS/auth/admin/payment
  boundary smoke evidence.
- SQL review found allowlisted product sort handling and fixed mapper sort
  clauses; no reviewed mapper path directly interpolates user-provided sort
  fields.
- Remaining external blockers: HTTPS certificate provisioning/reverse-proxy or
  gateway termination, production CORS domain values, deployment secret
  injection, and payment provider production approval.
- Main-agent integration fixed the CORS test-context issue by using
  `allowedOriginPatterns("*")` for empty local-safe CORS mappings while keeping
  production-like wildcard validation in `CorsProperties`.
- Post-integration backend `.\mvnw.cmd test` passed, 242 tests.
