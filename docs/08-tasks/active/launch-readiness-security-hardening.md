# Task: Launch Readiness Security Hardening

## Metadata

- ID: launch-readiness-security-hardening
- Status: active
- Owner: Codex
- Track: cross-cutting
- Depends on: launch-preparation roadmap L1; first launch-preparation wave closeout
- Priority: P0
- Planned date: 2026-06-04
- Completed date:

## Objective

Finish launch-blocking security hardening and produce reproducible evidence for a staging or public demo release.

## Background

Wave 1 added production-like CORS validation, mock-auth/payment guards, and security smoke requests. The remaining launch-readiness work must verify external HTTPS, production secret injection, input validation coverage, dependency/config scans, permission regression, and sensitive-data handling.

## Current State

Completed in earlier launch-preparation waves:

- JWT dev-secret fail-fast guard exists for production-like profiles.
- Production-like CORS validation rejects missing or wildcard origins.
- Mock auth and mock payment are gated away from production-like profiles.
- Security smoke collection and CI secret/dependency scan workflows exist.
- Backend permission and main transaction-path tests currently pass.

Remaining work:

- Final HTTPS/certificate evidence is external to the repository.
- High-risk `Map<String,Object>` mutation endpoints still need gradual typed DTO and Bean Validation hardening.
- Dependency/config scan outputs need a final release-candidate record.
- `localStorage` token strategy remains a security decision rather than a completed migration.

## Scope

- Verify HTTPS termination requirements and HTTP-to-HTTPS redirect runbook.
- Audit JWT, SMTP, payment, database, Redis, and admin secrets for production injection and rotation steps.
- Close high-risk input-validation gaps in auth, product, order, payment, report, support, and admin mutations.
- Re-run dependency, secret, configuration, SQL-injection, and permission checks with recorded evidence.
- Confirm sensitive data is not exposed in logs, API errors, admin views, or exported smoke evidence.

## Out of Scope

- Purchasing certificates, domains, or cloud secret-manager services.
- Replacing the authentication model.
- Adding new product roles or permission domains.
- Declaring legal or regulatory compliance approval.

## Files to Read

- `docs/05-roadmap/current/launch-preparation-roadmap.md`
- `docs/04-standards/operations-and-deployment.md`
- `docs/04-standards/launch-foundation-security-scanning.md`
- `docs/06-http/security-hardening.http`
- `backend/src/main/java/com/youyu/backend/config/`
- `backend/src/main/java/com/youyu/backend/controller/`
- `backend/src/main/java/com/youyu/backend/filter/`
- `backend/src/main/resources/application*.yml`

## Allowed Changes

- Backend security/configuration, validation DTOs, permission guards, and focused tests.
- Security smoke requests and launch security standards/runbook docs.
- Environment example files when needed for secure configuration.
- `CHANGELOG.md` and this task lifecycle record.

## Implementation Plan

1. Inventory remaining launch-blocking security checks from roadmap and wave 1 completion notes.
2. Patch only verified repository gaps in validation, config guards, permissions, or data masking.
3. Run and record dependency, secret, configuration, SQL, permission, and CORS checks.
4. Document external blockers separately from repository-complete controls.

## Risks

- Over-tightening local dev settings can break seed/demo workflows.
- Scan output can contain sensitive paths or tokens if copied carelessly.
- Permission fixes can regress admin support workflows.

## Test Plan

- Backend: `cd backend; .\mvnw.cmd test`
- Frontend: not expected unless security UI behavior changes.
- API validation: run `docs/06-http/security-hardening.http` and any updated auth/admin/payment smoke requests.
- Manual: verify staging/prod-like startup fails without required secrets and explicit CORS origins.

## Acceptance Criteria

- [x] Production-like runtime cannot start with dev JWT secrets, wildcard CORS, or mock payment/auth exposure.
- [ ] High-risk mutation inputs have server-side validation or documented blockers.
- [x] Permission regression evidence covers user/admin boundary cases through backend test coverage.
- [ ] Security scan commands and release-candidate results are recorded without leaking secrets.
- [x] External HTTPS/certificate blockers are explicit in the launch-foundation runbook.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] security/runbook standards if applicable
- [ ] task status and archive move

## Completion Notes

- 2026-06-04 sync: `.\mvnw.cmd test` passed with 255 tests after Redis/cache closeout.
- 2026-06-04 sync: configuration guards are implemented in `JwtSecretGuard`, `CorsProperties`, `MockPaymentExposureGuard`, and mock auth boundary logic.
- 2026-06-04 remaining: DTO validation migration, final scan artifacts, HTTPS certificate evidence, and token-storage decision stay open for this active task.
