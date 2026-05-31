# Task: Personalization Email Binding Placeholder

## Metadata

- ID: personalization-email-binding-placeholder
- Status: done
- Owner: worker-or-main-agent
- Track: cross-cutting
- Depends on: `personalization-settings-parent`
- Priority: P2
- Planned date: 2026-05-31
- Completed date: 2026-05-31

## Objective

Reserve an honest email-binding interface and frontend entry while email verification login is developed separately.

## Scope

- Add a backend endpoint for email binding intent.
- Validate email format and uniqueness against existing `users.email`.
- Return clear placeholder status indicating verification is not completed in this wave.
- Add a frontend settings/security entry for current email and binding placeholder state.
- Keep UI copy honest: no claim that verification email was sent or that email login is enabled.

## Out of Scope

- Verification-code generation, persistence, expiration, resend throttling, SMTP, and email delivery.
- Email login or passwordless login.
- Campus-email student verification changes.
- Account recovery or password reset.

## Files to Read

- `backend/src/main/java/com/youyu/backend/controller/user/UserController.java`
- `backend/src/main/java/com/youyu/backend/service/user/UserService.java`
- `backend/src/main/java/com/youyu/backend/service/user/impl/UserServiceImpl.java`
- `backend/src/main/java/com/youyu/backend/mapper/user/UserMapper.java`
- `backend/src/main/java/com/youyu/backend/mapper/user/impl/JdbcUserMapper.java`
- `backend/src/main/resources/schema.sql`
- `frontend/src/api/modules/user.js`
- `frontend/src/stores/market.js`
- `frontend/src/views/app/SettingsView.vue`
- `frontend/src/router/modules/app.js`
- `docs/09-api-spec/user.md`

## Allowed Changes

- `backend/src/main/java/com/youyu/backend/controller/user/**`
- `backend/src/main/java/com/youyu/backend/service/user/**`
- `backend/src/main/java/com/youyu/backend/mapper/user/**`
- `backend/src/test/java/com/youyu/backend/user/**`
- `frontend/src/api/modules/user.js`
- `frontend/src/stores/market.js`
- `frontend/src/views/app/SettingsView.vue`
- `frontend/src/views/app/SettingsSecurityView.vue`
- `frontend/src/router/modules/app.js`
- related frontend tests
- `docs/09-api-spec/user.md`
- `docs/06-http/*`

## Implementation Plan

1. Define placeholder response semantics for `PUT /api/users/me/email`.
2. Add backend validation for blank, invalid format, and duplicate email.
3. Add security/settings UI entry that can submit the placeholder binding request.
4. Document deferred verification-login behavior explicitly.

## Risks

- Misleading users into believing email verification or email login is complete.
- Creating schema drift for verification state without a full verification design.
- Confusing `users.email` with `student_verifications.campus_email`.

## Test Plan

- Backend:
  - `mvnw.cmd test`
  - Add tests for invalid email, duplicate email, and successful placeholder update.
- Frontend:
  - `npm test`
  - `npm run build`
- API validation:
  - HTTP example for the placeholder endpoint.
- Manual:
  - Submit valid email and verify the UI displays pending/unverified placeholder language.

## Acceptance Criteria

- [x] Endpoint exists and validates email format and uniqueness.
- [x] Frontend exposes email binding as a reserved/pending capability without claiming verification is complete.
- [x] Existing campus-email verification flow is not changed.
- [x] API spec clearly marks verification-code delivery as deferred.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] `docs/09-api-spec/user.md`
- [ ] `docs/06-http/*`
- [ ] task status and archive move after review

## Completion Notes

- Implemented `PUT /api/users/me/email` as a placeholder response with `verificationEnabled=false` and `emailLoginEnabled=false`.
- The endpoint validates format and uniqueness against other users but does not send codes or persist a verified binding state.
- Added `/app/settings/security` frontend entry.
