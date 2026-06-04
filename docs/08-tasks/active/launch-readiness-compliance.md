# Task: Launch Readiness Compliance

## Metadata

- ID: launch-readiness-compliance
- Status: active
- Owner: Codex
- Track: cross-cutting
- Depends on: launch-preparation roadmap L2; privacy compliance wave 1 baseline
- Priority: P0
- Planned date: 2026-06-04
- Completed date:

## Objective

Close the remaining privacy, consent, user-rights, and launch-disclosure gaps needed before public demo or launch rehearsal.

## Background

Wave 1 added legal routes, cookie consent, registration agreement validation, consent logs, personal data export, and soft account closure. The latest readiness pass still requires compliance evidence, text alignment with implemented behavior, retention boundaries, user-rights smoke checks, and external filing decisions.

## Current State

Completed in earlier launch-preparation waves:

- Privacy policy, user agreement, and Cookie policy routes exist.
- Cookie consent banner exists and stores user choices.
- Registration requires user agreement and privacy-policy consent.
- Consent logs are persisted in `user_consent_logs`.
- User consent history, personal data export, and soft account closure endpoints exist.

Remaining work:

- Final legal-text review must confirm the pages do not overclaim current behavior.
- ICP/public-security filing, data-residency decisions, and legal review are external blockers.
- Retention and lifecycle rules for search logs, support records, reports, media, and deleted accounts need final signoff.

## Scope

- Review privacy policy, user agreement, and cookie policy for claims that exceed current implementation.
- Verify registration, cookie consent, consent history, data export, and account-closure request flows.
- Confirm retention and anonymization behavior for orders, reviews, reports, support records, search logs, and deleted accounts.
- Document ICP/public-security filing decision, data residency assumptions, and external legal-review blockers.
- Update API specs, HTTP checks, and UI entry points if behavior changed.

## Out of Scope

- Legal counsel approval.
- Completing ICP or public-security filing.
- Hard-deleting historical transaction records.
- New marketing consent, analytics consent, or preference-product features.

## Files to Read

- `docs/03-architecture/data-management-and-privacy.md`
- `docs/04-standards/operations-and-deployment.md`
- `docs/09-api-spec/user.md`
- `docs/06-http/auth.http`
- `docs/06-http/user.http` if present
- `frontend/src/views/legal/`
- `frontend/src/views/auth/RegisterView.vue`
- `frontend/src/views/app/PrivacyRightsView.vue`
- `backend/src/main/java/com/youyu/backend/controller/user/`
- `backend/src/main/java/com/youyu/backend/service/user/`
- `backend/src/main/resources/schema.sql`

## Allowed Changes

- Legal/compliance frontend copy and entry points.
- User consent, export, deletion, retention, and anonymization backend code/tests.
- User API specs and HTTP smoke requests.
- Compliance standards/runbook docs when needed.
- `CHANGELOG.md` and this task lifecycle record.

## Implementation Plan

1. Compare legal text and API behavior against implemented consent and user-rights flows.
2. Patch behavior or copy where the system overclaims or under-documents user rights.
3. Add focused tests for registration consent, export, consent history, and account closure where gaps exist.
4. Record external legal, filing, and data-residency blockers.

## Risks

- Compliance text can promise rights workflows that are not implemented.
- Account closure can conflict with transaction traceability.
- Filing timelines are external and cannot be solved by repository edits.

## Test Plan

- Backend: `cd backend; .\mvnw.cmd test`
- Frontend: `cd frontend; npm test`
- Frontend build: `cd frontend; npm run build` if UI copy or routing changes.
- API validation: registration, consent history, export, and account-closure smoke checks.
- Manual: verify legal links are reachable from registration, settings, and footer surfaces.

## Acceptance Criteria

- [ ] Legal copy matches actual data collection, retention, consent, export, and closure behavior.
- [x] Registration and cookie consent leave auditable evidence where required for logged-in/registered flows.
- [x] User data export and account-closure flows are implemented.
- [x] Filing/data-residency blockers are documented without marking them complete.
- [ ] API specs and smoke checks match implemented compliance behavior after final legal-text review.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] `docs/09-api-spec/user.md` if contracts change
- [ ] roadmap or standards docs if assumptions change
- [ ] task status and archive move

## Completion Notes

- 2026-06-04 sync: compliance implementation exists in legal frontend routes, `CookieConsent`, consent store/API calls, `UserController`, `UserServiceImpl`, and `user_consent_logs`.
- 2026-06-04 verification baseline: backend tests, frontend tests, and frontend build passed after closeout.
- 2026-06-04 remaining: legal copy review, retention signoff, filing/data-residency decision, and final API/smoke alignment stay open.
