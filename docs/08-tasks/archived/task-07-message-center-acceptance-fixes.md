# Task 07: Message Center Acceptance Fixes

## Metadata

- **Task ID**: task-07
- **Priority**: P0 follow-up
- **Track**: cross-cutting
- **Status**: done
- **Created date**: 2026-05-26
- **Completed date**: 2026-05-26
- **Owner**: Head Agent

## Context

P0 message-center implementation passed automated tests, but manual acceptance found three gaps:

1. Unread/read red-dot behavior is hard to verify because seed data does not create enough visible unread scenarios.
2. Notifications are exposed as a top-level navigation module, which makes the navigation too large for a compact app shell.
3. Opening a conversation changes page position and uses animated thread transitions, making the message view feel unstable.

## Root Cause

- `seed-chat-data.sql` creates conversations and messages but leaves all unread counters at their defaults, so seeded users do not reliably see red-dot states.
- There is no notification seed data, so notification unread badges are not visible in the seed profile until a business event creates one.
- `appNavigation` includes `/app/notifications`, so the notification system appears as a full navigation section.
- `MessagesView.vue` calls `scrollIntoView({ behavior: 'smooth' })` on message load and applies thread/bubble animations, which can move the page viewport when switching conversations.

## Scope

- Add deterministic seed data for unread chat counts and unread/read notification states.
- Keep the notification route for deep links, but remove notifications from top-level navigation and surface it inside the message center.
- Make conversation switching keep the page position stable by scrolling only the thread container without smooth page scrolling.
- Add or update automated tests for unread count/read behavior where practical.

## Acceptance Criteria

- [x] Seed profile shows at least one unread chat red dot for `zhangsan` and one unread notification without requiring a new action.
- [x] Desktop and mobile top-level navigation do not show `Notifications` as a standalone main module.
- [x] Message center exposes notifications under the message UI.
- [x] Clicking a conversation does not scroll the page viewport.
- [x] Backend tests pass with explicit unread count and mark-read coverage.
- [x] Frontend tests and build pass.
- [x] Commit a version after verification.

## Completion Notes

- Added message-center seed data with deterministic unread chat counters and unread notifications.
- Removed notifications from top-level app navigation and changed the notification route nav key to the message center.
- Embedded notifications as a category inside `MessagesView`.
- Replaced page-level smooth `scrollIntoView` with direct thread-container scrolling and removed thread/bubble entrance animations.
- Added backend coverage for unread count and mark-read behavior.

## Verification

- `backend/.\\mvnw.cmd test` passed: 101 tests, 0 failures.
- `frontend/npm test` passed on rerun: 30 tests, 0 failures. The first cold run hit the existing 5s router test timeout.
- `frontend/npm run build` passed. Rollup emitted existing `@vueuse/core` pure-annotation warnings.
