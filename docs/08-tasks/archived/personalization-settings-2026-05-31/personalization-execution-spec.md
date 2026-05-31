# Personalization Settings Execution Specification

## Summary

Deliver the personalization settings wave in an isolated worktree. This wave covers editable nickname, real avatar upload, an honest email-binding placeholder, and preference defaults that affect existing user flows. It intentionally does not implement theme personalization, email verification delivery, email login, or login ID changes.

Repository root for execution:

- Base repository: `E:\Dev\Projects\Youyu`
- Worktree: `E:\Dev\Projects\Youyu-personalization-settings`
- Branch: `codex/personalization-settings`

## Task Documents

| Task | Wave | Owner | Core delivery |
| --- | ---: | --- | --- |
| `personalization-settings-parent` | all | main agent | boundaries, locked interfaces, acceptance evidence |
| `personalization-profile-and-avatar-upload` | 1 | worker or main agent | nickname update, real avatar upload, validation, profile UI |
| `personalization-email-binding-placeholder` | 1 | worker or main agent | placeholder email binding endpoint and honest frontend entry |
| `personalization-preferences-effective-defaults` | 2 | worker or main agent | default sort/address/payment/fulfillment effects |
| `personalization-settings-center-integration` | final | main agent | settings navigation integration, docs, changelog, task closure |

## Locked Interfaces

- Work only in `E:\Dev\Projects\Youyu-personalization-settings`.
- Do not edit the base `master` worktree except to inspect it.
- Do not push unless the human explicitly requests it.
- Public profile update route: `PATCH /api/users/profile`.
- Nickname is editable; `username` / `loginId` is not editable.
- Avatar upload route: `POST /api/users/me/avatar`.
- Avatar request format: `multipart/form-data`, field name `file`.
- Avatar accepted MIME types: `image/jpeg`, `image/png`, `image/webp`.
- Avatar maximum size: 10 MB.
- Avatar response includes updated profile data and public avatar URL.
- Avatar storage is local for this wave and served under `/uploads/avatars/**`.
- Email placeholder route: `PUT /api/users/me/email`.
- Email placeholder body: `{ "email": "..." }`.
- Email placeholder validates format and uniqueness but does not claim email verification or email login is complete.
- Preference update route remains `PUT /api/users/me/preference`.
- Theme mode and theme color are not implemented in this wave.
- Response envelope remains `ApiResponse<T>`.

## Wave Order And Dependency Rationale

1. Wave 1: Profile/avatar and email placeholder can proceed independently because they touch separate backend service methods and mostly separate frontend UI surfaces.
2. Wave 2: Effective defaults should wait until profile/settings API state is stable, because checkout/product-list integration depends on the same market store and settings UI.
3. Final integration remains with the main agent to avoid duplicate settings entries, stale placeholders, or conflicting documentation.

## Explicitly Deferred

- Login ID / username change.
- SMTP, verification-code generation, resend limits, code expiration, or email login.
- Theme mode/theme color implementation.
- Cloud object storage, CDN, image moderation, avatar cropping, and historical avatar cleanup.
- Full privacy settings, consent logs, account deletion, or password reset.

## Test Plan

- Backend:
  - `mvnw.cmd test`
  - Add/extend user tests for profile update, avatar upload validation, email placeholder, and preference defaults if backend behavior changes.
- Frontend:
  - `npm test`
  - `npm run build`
- API validation:
  - Update `docs/09-api-spec/user.md`.
  - Update `docs/06-http/auth.http` or create `docs/06-http/user.http`.
- Manual:
  - Edit nickname and reload profile.
  - Upload valid JPEG/PNG/WebP under 10 MB.
  - Reject unsupported or oversized avatar upload.
  - Submit placeholder email and verify honest pending/unverified UI.
  - Set default sort/address/payment/fulfillment and verify real flow impact or honest fallback.

## Main-Agent Launch Prompt

```text
Execute the Personalization Settings Delivery wave in E:\Dev\Projects\Youyu-personalization-settings.

Use the orchestrate-agent-delivery workflow. Read repository governance before editing:
- AGENTS.md
- CLAUDE.md
- docs/README.md
- docs/04-standards/development-process.md
- docs/05-roadmap/current/stage-roadmap.md
- docs/05-roadmap/current/feature-roadmap.md
- docs/08-tasks/active/personalization-execution-spec.md
- docs/08-tasks/active/personalization-settings-parent.md
- all personalization child task docs

Confirm:
- the active worktree is E:\Dev\Projects\Youyu-personalization-settings
- branch is codex/personalization-settings
- base master worktree remains clean
- no implementation happens in E:\Dev\Projects\Youyu

Locked interfaces:
- PATCH /api/users/profile updates nickname only; username/loginId is immutable.
- POST /api/users/me/avatar accepts multipart/form-data field file.
- Avatar uploads accept image/jpeg, image/png, image/webp only.
- Avatar max size is 10 MB.
- Avatar URL is persisted in users.avatar and served under /uploads/avatars/**.
- PUT /api/users/me/email validates and reserves email binding intent only; do not implement verification-code delivery, SMTP, or email login.
- PUT /api/users/me/preference remains the preference update route.
- Theme mode and theme color are out of scope for this wave.
- All API responses keep the ApiResponse<T> envelope.

Execution:
1. Start with personalization-profile-and-avatar-upload.
2. Then implement personalization-email-binding-placeholder.
3. Then implement personalization-preferences-effective-defaults.
4. Finish with personalization-settings-center-integration.
5. Keep changelog, docs/09-api-spec/user.md, docs/06-http coverage, and task completion notes aligned.

Worker delegation is allowed only if the human explicitly authorizes sub-agents. If workers are used later, assign disjoint child tasks, repeat these locked interfaces, forbid scope expansion, and keep integration with the main agent.

Forbidden expansion:
- username/loginId changes
- theme implementation
- SMTP/email verification/login
- cloud avatar storage/CDN
- password reset/account deletion/privacy consent logs
- broad UI redesign

Verification:
- backend: mvnw.cmd test
- frontend: npm test
- frontend: npm run build
- git diff --check
- manual smoke for profile edit, avatar upload validation, email placeholder, default preference effects

Final report:
1. branch and worktree
2. child task status
3. changed files
4. verification commands and results
5. warnings and blockers
6. deferred work
7. push/PR status
```

## Worker Prompt Template

```text
Implement child task <task-id> in E:\Dev\Projects\Youyu-personalization-settings.

Ownership:
- You may edit only the files listed in the child task's Allowed Changes.
- Do not edit other child task files unless the main agent explicitly updates your scope.

Locked interfaces:
- username/loginId is immutable
- avatar upload: POST /api/users/me/avatar, multipart field file, jpeg/png/webp, max 10 MB
- email placeholder: PUT /api/users/me/email, no SMTP, no verification-code delivery, no email login claim
- preference route remains PUT /api/users/me/preference
- no theme implementation

Other agents may be editing concurrently. Do not revert others' edits. Do not commit. In your final response list changed files, checks run, findings, and blockers.
```
