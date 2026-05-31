# Task: Personalization Settings Delivery

## Metadata

- ID: personalization-settings-parent
- Status: done
- Owner: main-agent
- Track: cross-cutting
- Depends on: current user profile, settings center, preference API, address API
- Priority: P1/P2
- Planned date: 2026-05-31
- Completed date: 2026-05-31

## Objective

Deliver a controlled personalization wave covering editable nickname, real avatar upload, email-binding placeholder, and preference defaults that actually affect user flows.

## Background

The product direction is to continue the personalization track without doing the theme task first. The user decisions for this wave are:

- User clarified that profile name editing means nickname editing only, not changing `username` / login ID.
- Avatar must be a real image upload with file size and type validation.
- Email verification login is being developed separately; this wave only reserves an email-binding interface and frontend entry.
- Theme capability is not part of this wave. Focus first on default sort, address list management, default payment method, and related defaults.

## Scope

- Add governed backend and frontend support for editing public profile fields: nickname and avatar.
- Add real avatar upload with multipart request handling, MIME/type validation, size limit, persisted avatar URL, and serving path.
- Add an email-binding placeholder contract and frontend entry that records intent without sending verification mail.
- Make supported preference defaults affect real flows where practical:
  - default sort in product listing/search entry
  - default address in checkout/settings
  - default payment method in payment/checkout display
  - default fulfillment type where order preview or checkout already exposes that choice
- Update API specs, HTTP smoke files, tests, changelog, and task completion records.

## Out of Scope

- Changing `username`, `loginId`, or JWT identity semantics.
- Email verification code generation, SMTP delivery, email login completion, resend throttling, or verification-code persistence.
- Theme mode/theme color implementation or completion of `preference-theme-capability-gap`.
- Cloud object storage migration, CDN, image moderation, or historical avatar garbage collection beyond local replacement safety.
- Full privacy-compliance consent logs or account deletion.

## Child Tasks

- [x] `personalization-profile-and-avatar-upload`
- [x] `personalization-email-binding-placeholder`
- [x] `personalization-preferences-effective-defaults`
- [x] `personalization-settings-center-integration`

## Locked Interfaces

- Public profile update route: `PATCH /api/users/profile`
  - JSON body supports `nickname`; `username` is not accepted as an editable field.
  - Response returns the same public profile envelope shape used by `GET /api/users/profile`.
- Avatar upload route: `POST /api/users/me/avatar`
  - Request type: `multipart/form-data`, field name `file`.
  - Accepted image MIME types: `image/jpeg`, `image/png`, `image/webp`.
  - Maximum file size: 10 MB.
  - Response includes the updated public profile and avatar URL.
- Avatar storage:
  - Local dev/runtime storage under a configured upload root, defaulting to a repo-ignored local uploads directory.
  - Public serving path under `/uploads/avatars/**`.
  - No base64 avatars in database.
- Email placeholder route: `PUT /api/users/me/email`
  - JSON body: `email`.
  - Validates format and uniqueness.
  - Stores email as unverified/reserved only if the existing schema can represent this honestly; otherwise returns a placeholder response without claiming verification.
- Preference update route stays `PUT /api/users/me/preference`; do not add theme-related behavior in this wave.
- API envelope remains `ApiResponse<T>`.
- No `ALTER TABLE` or `DROP TABLE` in `schema.sql` unless a child task explicitly justifies additive DDL and keeps H2 tests passing.

## Main-Agent Launch Prompt

Execute the Personalization Settings Delivery wave in `E:\Dev\Projects\Youyu`.

Read `AGENTS.md`, `CLAUDE.md`, `docs/README.md`, `docs/04-standards/development-process.md`, `docs/05-roadmap/current/feature-roadmap.md`, this parent task, and all child tasks before editing. Confirm the base branch and worktree status. Prefer an isolated worktree and a `codex/personalization-settings` branch if executing implementation. Do not push unless explicitly requested.

Keep the main agent responsible for integration, API docs, HTTP smoke alignment, changelog, task completion, and final verification. If workers are dispatched later, assign each worker exactly one child task with the owned files listed in that task. Workers must not revert others' edits, must not commit, and must report changed files and checks.

Locked interfaces are the routes, file-size limit, MIME list, username boundary, and email placeholder boundary listed above.

Forbidden expansion: theme implementation, login-ID changes, SMTP/verification-code delivery, cloud storage migration, broad settings redesign, or unrelated address/payment refactors.

Final report must include child task status, verification commands and results, warnings, blockers, deferred work, and whether any branch/PR was created.

## Test Plan

- Backend:
  - `mvnw.cmd test`
  - user-profile focused tests for nickname update, avatar upload happy path, avatar size/type rejection, email placeholder uniqueness/format, and preference defaults.
- Frontend:
  - `npm test`
  - `npm run build`
  - focused store/view tests where existing test structure supports them.
- API validation:
  - Update `docs/09-api-spec/user.md`.
  - Update or split `docs/06-http/auth.http` / user smoke coverage for new user endpoints.
- Manual:
  - Login as demo user, edit nickname, upload valid avatar, reject invalid avatar, set default address/payment/sort, verify related flow prefill or ordering behavior.

## Acceptance Criteria

- [x] Every child task is reviewed before archival.
- [x] Nickname can be changed without changing login ID.
- [x] Avatar upload persists and reloads a served image URL; invalid type and oversized files are rejected clearly.
- [x] Email-binding placeholder is visible and honest: no UI claims that email verification or email login is complete.
- [x] Preference defaults included in this wave visibly affect at least one real user path or are removed/downgraded from UI.
- [x] API docs, HTTP examples, tests, changelog, and task records are aligned.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] `docs/09-api-spec/user.md`
- [x] relevant files in `docs/06-http/`
- [x] child task status and archive move after main-agent review

## Completion Notes

- Completed the personalization wave in `E:\Dev\Projects\Youyu-personalization-settings` on branch `codex/personalization-settings`.
- Follow-up cleanup raised the avatar limit to 10 MB, fixed browser multipart upload handling, and localized user-facing personalization copy.
- Deferred work remains: real email verification delivery/login, theme implementation, cloud avatar storage/CDN, moderation/cropping, and privacy/account lifecycle features.
