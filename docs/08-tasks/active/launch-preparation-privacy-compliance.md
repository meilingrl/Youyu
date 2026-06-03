# Task: Launch Preparation Privacy Compliance

## Metadata

- ID: launch-preparation-privacy-compliance
- Status: active
- Owner: worker-privacy
- Track: cross-cutting
- Depends on: launch-preparation-l0-scope-freeze
- Priority: P0
- Planned date: 2026-06-04
- Completed date:

## Objective

Establish the user-facing legal-document, consent, and user-rights baseline required for public demo or launch rehearsal, without overclaiming production legal approval.

## Background

The launch roadmap requires privacy policy, user agreement, cookie policy, cookie consent, registration agreement validation, consent logs, personal data export, account deletion, and legal-entry links. Existing architecture guidance describes these as compliance capabilities, separate from product preference features.

## Scope

- Add or verify legal document routes/pages and navigation entries.
- Add cookie consent banner/preferences using existing frontend patterns.
- Require explicit agreement in registration and enforce it server-side.
- Add consent logging/history if not already implemented.
- Add personal data export and account deletion/request baseline when feasible within current schema and JDBC style.
- Update API specs and HTTP smoke requests for changed or added compliance endpoints.

## Out of Scope

- Legal counsel approval of policy text.
- Real regulatory filing such as ICP or public-security filing.
- Hard deletion of historical order records.
- Marketing-consent or analytics-product expansion.
- Large destructive schema migration.

## Files to Read

- `docs/03-architecture/data-management-and-privacy.md`
- `frontend/src/router/`
- `frontend/src/views/auth/RegisterView.vue`
- `frontend/src/layouts/AppLayout.vue`
- `frontend/src/components/layout/`
- `frontend/src/stores/`
- `frontend/src/api/modules/user.js`
- `backend/src/main/java/com/youyu/backend/controller/auth/`
- `backend/src/main/java/com/youyu/backend/controller/user/`
- `backend/src/main/java/com/youyu/backend/service/user/`
- `backend/src/main/java/com/youyu/backend/mapper/user/`
- `backend/src/main/resources/schema.sql`
- `docs/09-api-spec/user.md`

## Allowed Changes

- frontend legal views, consent components, routes, stores, user API wrappers, registration UI
- backend auth/user consent/data-export/account-deletion controllers, services, mappers, DTOs, tests
- additive schema changes in `backend/src/main/resources/schema.sql` only when needed and compatible with H2/MySQL tests
- `docs/09-api-spec/user.md`
- relevant `docs/06-http/*.http`
- this task document

## Implementation Plan

1. Inspect whether legal pages, consent UI, and user-rights endpoints already exist.
2. Implement the smallest coherent baseline for missing P0 compliance capabilities.
3. Keep legal copy scoped to current behavior and avoid promises the system cannot fulfill.
4. Add backend tests for consent logging, registration agreement validation, export, or deletion behavior when implemented.
5. Update API docs and HTTP smoke requests.

## Risks

- Compliance copy can overpromise rights workflows not yet implemented.
- Account deletion can conflict with order-retention requirements.
- Schema additions must remain idempotent and test-compatible.

## Test Plan

- Backend: `cd backend; .\mvnw.cmd test`
- Frontend: `cd frontend; npm test`
- Frontend build: `cd frontend; npm run build`
- API validation: user compliance HTTP smoke requests.
- Manual: visit legal pages, register with/without agreement, review cookie consent behavior.

## Acceptance Criteria

- [ ] Legal-document entries are reachable from user-facing surfaces.
- [ ] Registration agreement consent is validated client-side and server-side.
- [ ] Cookie consent is visible and persists user preference without storing unnecessary sensitive data.
- [ ] User consent history, export, and deletion/request limitations are explicit.
- [ ] API docs and smoke samples match implemented behavior.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] `docs/09-api-spec/user.md`
- [ ] task status and archive move

## Completion Notes
