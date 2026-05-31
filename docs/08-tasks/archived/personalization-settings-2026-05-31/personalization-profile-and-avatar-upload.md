# Task: Personalization Profile And Avatar Upload

## Metadata

- ID: personalization-profile-and-avatar-upload
- Status: done
- Owner: worker-or-main-agent
- Track: cross-cutting
- Depends on: `personalization-settings-parent`
- Priority: P1
- Planned date: 2026-05-31
- Completed date: 2026-05-31

## Objective

Allow users to edit their nickname and upload a real avatar image while preserving login ID immutability.

## Scope

- Backend profile update API for `nickname`.
- Backend multipart avatar upload API with validation:
  - max 10 MB
  - `image/jpeg`, `image/png`, `image/webp`
  - reject empty files and unsupported types
- Persist avatar URL to `users.avatar`.
- Serve uploaded avatars through a stable `/uploads/avatars/**` path.
- Frontend profile/settings entry for nickname editing and avatar upload.
- Keep auth session display in sync after successful profile update.

## Out of Scope

- Changing `username`, `loginId`, phone, password, or role.
- Cropping, image editing, moderation, CDN, object storage, or garbage collection of old avatar files.
- Email binding or verification.
- Theme changes.

## Files to Read

- `backend/src/main/java/com/youyu/backend/controller/user/UserController.java`
- `backend/src/main/java/com/youyu/backend/service/user/UserService.java`
- `backend/src/main/java/com/youyu/backend/service/user/impl/UserServiceImpl.java`
- `backend/src/main/java/com/youyu/backend/mapper/user/UserMapper.java`
- `backend/src/main/java/com/youyu/backend/mapper/user/impl/JdbcUserMapper.java`
- `backend/src/main/java/com/youyu/backend/config/WebMvcConfig.java`
- `backend/src/main/resources/application.yml`
- `frontend/src/api/modules/user.js`
- `frontend/src/stores/auth.js`
- `frontend/src/stores/market.js`
- `frontend/src/views/app/ProfileView.vue`
- `frontend/src/views/app/SettingsView.vue`
- `frontend/src/utils/market-normalizers.js`
- `docs/09-api-spec/user.md`
- `docs/06-http/auth.http`

## Allowed Changes

- `backend/src/main/java/com/youyu/backend/controller/user/**`
- `backend/src/main/java/com/youyu/backend/service/user/**`
- `backend/src/main/java/com/youyu/backend/mapper/user/**`
- `backend/src/main/java/com/youyu/backend/config/WebMvcConfig.java`
- `backend/src/main/resources/application*.yml`
- `backend/src/test/java/com/youyu/backend/user/**`
- `frontend/src/api/modules/user.js`
- `frontend/src/stores/auth.js`
- `frontend/src/stores/market.js`
- `frontend/src/views/app/ProfileView.vue`
- `frontend/src/views/app/SettingsView.vue`
- `frontend/src/utils/market-normalizers.js`
- related frontend tests
- `docs/09-api-spec/user.md`
- `docs/06-http/auth.http` or a new `docs/06-http/user.http`

## Implementation Plan

1. Add backend mapper/service/controller methods for profile update and avatar upload.
2. Add upload configuration with a safe local storage default and static resource serving.
3. Add backend tests for success and rejection cases.
4. Add frontend API/store methods and a scoped settings/profile editing UI.
5. Update API docs and HTTP examples.

## Risks

- Accidentally treating `username` as editable and breaking login identity.
- Trusting file extension instead of content type.
- Returning a filesystem path instead of a public URL.
- Leaving auth-store nickname stale after profile update.

## Test Plan

- Backend:
  - `mvnw.cmd test`
  - Add `UserProfileTest` cases for nickname update and avatar upload validation.
- Frontend:
  - `npm test`
  - `npm run build`
- API validation:
  - HTTP example for profile update and avatar upload.
- Manual:
  - Upload valid PNG/JPEG/WebP under 10 MB.
  - Upload invalid `.txt` or oversized image and verify rejection.

## Acceptance Criteria

- [x] User can update nickname and reload profile with the new nickname.
- [x] Login ID / username remains unchanged and cannot be patched through profile update.
- [x] User can upload a valid avatar image and later load it through the returned URL.
- [x] Oversized and unsupported avatar uploads are rejected with clear messages.
- [x] Auth header/profile displays stay consistent after update.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] `docs/09-api-spec/user.md`
- [ ] `docs/06-http/*`
- [ ] task status and archive move after review

## Completion Notes

- Implemented `PATCH /api/users/profile` for nickname-only edits.
- Implemented `POST /api/users/me/avatar` with multipart upload, 10 MB limit, JPEG/PNG/WebP validation, persisted `users.avatar`, and `/uploads/avatars/**` serving.
- Added profile UI avatar upload and nickname editing, with auth session sync.
- Verified with `mvnw.cmd test -Dtest=UserProfileTest`, `npm test -- --run`, and `npm run build`.
