# Task: Admin Data Export

## Metadata

- ID: feature-polish-admin-data-export
- Status: completed
- Owner: worker-wave-3
- Track: cross-cutting
- Depends on: feature-polish-admin-permission-assignment, feature-polish-admin-dashboard-metrics
- Priority: P2
- Planned date: 2026-06-03
- Completed date: 2026-06-03

## Objective

Provide administrator data export capability with explicit permissions, safe data boundaries, and a documented export format.

## Background

The user requested backend data export. This overlaps with privacy/security concerns, so it should happen after admin permission boundaries are clear.

## Scope

- Define exportable datasets for the first version. Proposed first version:
  - users summary
  - orders summary
  - products summary
  - reviews/report summaries if already supported by admin list APIs
- Provide CSV export first unless the main Agent approves another format.
- Add admin UI entry points for export.
- Enforce admin permission checks.
- Exclude secrets and high-risk PII by default.
- Document field list, filters, and security boundary.

## Out of Scope

- Full database dump.
- Exporting passwords, password hashes, JWTs, verification codes, SMTP config, payment secrets, or raw audit security details.
- Scheduled exports.
- Async export jobs for very large datasets unless required by current performance evidence.

## Files to Read

- `frontend/src/views/admin/UserManageView.vue`
- `frontend/src/views/admin/OrderManageView.vue`
- `frontend/src/views/admin/ProductManageView.vue`
- `frontend/src/views/admin/DashboardView.vue`
- `frontend/src/api/modules/admin.js`
- `backend/src/main/java/com/youyu/backend/common/auth/AdminPermission.java`
- `backend/src/main/java/com/youyu/backend/controller/admin/AdminController.java`
- `backend/src/main/java/com/youyu/backend/service/admin/AdminService.java`
- `backend/src/main/java/com/youyu/backend/service/admin/impl/AdminServiceImpl.java`
- relevant admin mappers for each exported dataset
- `docs/09-api-spec/admin.md`
- `docs/06-http/admin.http`
- `docs/03-architecture/data-management-and-privacy.md`

## Allowed Changes

- Admin API/controller/service/mapper export code.
- Admin frontend export buttons/actions.
- New utility for CSV serialization if no existing helper fits.
- Tests for permission and CSV field output.
- `docs/09-api-spec/admin.md`
- `docs/06-http/admin.http`
- privacy/data-management docs only if export policy changes.

## Implementation Plan

1. Lock export datasets and fields with the main Agent before coding.
2. Add backend export endpoint(s) with permission guard and safe field allowlists.
3. Return CSV with correct content type and filename.
4. Add frontend export action with loading/error feedback.
5. Add tests for forbidden roles and excluded sensitive fields.
6. Document the contract and data boundary.

## Risks

- Export can leak PII if fields are not allowlisted.
- Large exports can block request threads; keep first version bounded or document async follow-up.
- Browser download behavior needs manual verification.

## Test Plan

- Backend: `cd backend; .\mvnw.cmd test`
- Frontend: `cd frontend; npm test`
- Frontend build: `cd frontend; npm run build`
- API validation: export endpoint with allowed and forbidden admin roles.
- Manual: download CSV, inspect headers/fields, verify sensitive fields are absent.

## Acceptance Criteria

- [ ] Export is permission-protected.
- [ ] Exported fields are allowlisted and documented.
- [ ] Sensitive values are not present in exported files.
- [ ] Frontend provides clear export action and error state.
- [ ] API spec and HTTP samples cover export.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] `docs/06-http/admin.http`
- [ ] `docs/09-api-spec/admin.md`
- [ ] `docs/03-architecture/data-management-and-privacy.md` if export policy changes
- [ ] task status and archive move

## Completion Notes

- Added permission-protected CSV exports for `users`, `orders`, and `products`, with explicit allowlisted fields and browser-download handling in the frontend.
- Verified that sensitive values such as passwords and password hashes are excluded from exported datasets and admin list/detail responses.
