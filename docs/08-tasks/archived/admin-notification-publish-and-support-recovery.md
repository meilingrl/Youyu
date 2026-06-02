# Task: Admin notification publishing and support recovery

## Metadata

- ID: admin-notification-publish-and-support-recovery
- Status: archived
- Owner: Codex
- Track: cross-cutting
- Depends on: notification Wave 1, admin support dual workspace
- Priority: P1
- Planned date: 2026-06-02
- Completed date: 2026-06-02

## Objective

Restore the admin support page, add an admin system-notification publishing entry, and correct the user notification-center copy.

## Scope

- Fix the `/admin/support` runtime initialization failure without changing its dual-workspace architecture.
- Add an admin-only broadcast endpoint for system notifications.
- Add `/admin/notifications` as a dedicated admin publishing page.
- Update the user notification center so supported notification types and copy are accurate.

## Out of Scope

- Payment or refund workflow changes.
- Scheduled notifications, templates, targeting segments, or external push delivery.
- Reworking unrelated in-progress visual changes in the current worktree.

## Test Plan

- Backend: notification publishing permission, persistence, and audit tests.
- Frontend: unit tests and production build.
- API validation: admin notification HTTP sample.
- Manual: open `/admin/support`, publish one system notification, and verify it appears in the user message-center notification lane.

## Acceptance Criteria

- [x] `/admin/support` renders its online-CS and ticket lanes.
- [x] `ADMIN` and `SUPER_ADMIN` can broadcast one system notification to active ordinary users.
- [x] Specialist admin roles cannot publish system notifications.
- [x] `/admin/notifications` provides a clear publishing form.
- [x] User notification-center copy is accurate and notification types render correctly.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [x] `docs/09-api-spec/notification.md`
- [x] task status and archive move

## Completion Notes

- Fixed `/admin/support` by removing the setup-time dependency on later-defined
  ticket option constants.
- Added admin system-notification publishing for all active ordinary users.
- Added `SYSTEM_NOTIFICATION_PUBLISH` audit records with aggregate
  `targetType=NOTIFICATION` and `targetId=0`.
- Updated the user notification lane and message-center empty-state icons.
- Verified the full backend suite, frontend suite, production build, diff
  hygiene, and the three affected browser paths.
