# Task: Admin Permission Assignment

## Metadata

- ID: feature-polish-admin-permission-assignment
- Status: completed
- Owner: worker-wave-2
- Track: cross-cutting
- Depends on: feature-polish-closeout-parent
- Priority: P1
- Planned date: 2026-06-03
- Completed date: 2026-06-03

## Objective

Allow privileged administrators to assign administrator roles/permissions safely, using the existing admin permission model and audit conventions.

## Background

The codebase already has:

- `AdminPermission`
- `AdminPermissionPolicy`
- admin roles such as `ADMIN`, `SUPER_ADMIN`, `SUPPORT_AGENT`, `REVIEWER`, `OPERATOR`, and `ORDER_ADMIN`
- `@LoginRequired(permissions = ...)` enforcement

The missing capability is a managed admin UI/API path to assign administrator authority.

## Scope

- Define who can assign roles. Proposed boundary: only `SUPER_ADMIN` or existing all-permission admin can assign admin roles.
- Add backend role-assignment endpoint(s) with explicit permission checks.
- Prevent privilege escalation by lower-role admins.
- Record admin audit logs for permission/role assignment.
- Add admin user-management UI for viewing and changing admin role/permission assignment.
- Keep static permission mapping unless a dynamic per-permission model is explicitly approved.

## Out of Scope

- Replacing role-based access control with a full dynamic ACL system.
- Adding organization/team hierarchy.
- Letting ordinary users self-request admin roles.
- Removing existing role checks.

## Files to Read

- `backend/src/main/java/com/youyu/backend/common/auth/AdminPermission.java`
- `backend/src/main/java/com/youyu/backend/common/auth/AdminPermissionPolicy.java`
- `backend/src/main/java/com/youyu/backend/common/auth/UserRole.java`
- `backend/src/main/java/com/youyu/backend/common/auth/LoginRequired.java`
- `backend/src/main/java/com/youyu/backend/filter/AuthInterceptor.java`
- `backend/src/main/java/com/youyu/backend/controller/admin/AdminController.java`
- `backend/src/main/java/com/youyu/backend/service/admin/AdminService.java`
- `backend/src/main/java/com/youyu/backend/service/admin/impl/AdminServiceImpl.java`
- `backend/src/main/java/com/youyu/backend/mapper/user/UserMapper.java`
- `backend/src/main/java/com/youyu/backend/mapper/audit/AdminAuditLogMapper.java`
- `frontend/src/views/admin/UserManageView.vue`
- `frontend/src/api/modules/admin.js`
- `docs/09-api-spec/admin.md`
- `docs/06-http/admin.http`

## Allowed Changes

- Admin auth/permission files listed above.
- Admin user-management frontend files listed above.
- Add a request DTO for role assignment if needed.
- Add backend tests for allowed/forbidden role assignment.
- `docs/09-api-spec/admin.md`
- `docs/06-http/admin.http`

## Implementation Plan

1. Confirm current role storage in users and current admin list/detail responses.
2. Define the smallest role-assignment contract, including allowed target roles and forbidden self-demotion/escalation cases.
3. Implement backend endpoint with `@LoginRequired` permission enforcement and service-level guard.
4. Record audit logs with actor, target, old role, new role, and reason.
5. Add admin UI controls only for actors allowed to manage roles.
6. Update API spec and HTTP smoke examples.

## Risks

- Security regression if `ADMIN` can create `SUPER_ADMIN` without an explicit policy.
- Existing admin sessions may cache role data; verify session refresh behavior.
- UI must not be the only enforcement layer.

## Test Plan

- Backend: `cd backend; .\mvnw.cmd test`
- Frontend: `cd frontend; npm test`
- Frontend build: `cd frontend; npm run build`
- API validation: allowed role assignment, forbidden lower-role assignment, audit-log visibility.
- Manual: login as superadmin and lower-role admin; verify controls and backend denial match.

## Acceptance Criteria

- [ ] Admin role/permission assignment has backend enforcement.
- [ ] Lower-privilege admins cannot escalate themselves or others.
- [ ] Assignment is audited.
- [ ] UI shows available actions according to current actor capability.
- [ ] API spec and HTTP samples document the contract.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] `docs/06-http/admin.http`
- [ ] `docs/09-api-spec/admin.md`
- [ ] ADR if dynamic permissions are introduced instead of static role mapping
- [ ] task status and archive move

## Completion Notes

- Added `PUT /api/admin/users/{userId}/role` with `ADMIN_ROLE_ASSIGN` protection, normalized role validation, self-change blocking, and last-full-access-admin protection.
- Updated admin user management so privileged operators can open a detail drawer, inspect role state, and submit audited role changes from the frontend.
