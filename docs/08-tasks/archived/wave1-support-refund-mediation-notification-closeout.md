# Task: Wave 1 Support Refund Mediation Notification Closeout

## Metadata

- ID: wave1-support-refund-mediation-notification-closeout
- Status: completed
- Owner: worker-b
- Track: cross-cutting
- Depends on: `wave1-support-dual-workspace-sync`, `wave1-refund-assistance-and-mediation-handoff`
- Priority: medium
- Planned date: 2026-06-01
- Completed date: 2026-06-01

## Objective

Use the existing in-app notification lane to cover the most important support, refund, and mediation events introduced or clarified by Wave 1.

## Background

The repository already has a user notification center, but Wave 1 still needs message delivery continuity when support or after-sales state changes. The user chose to include only bounded in-app notification closeout, not preferences, templates, or admin notification orchestration.

## Scope

- add or align notification events for key support-ticket, refund, and mediation changes
- ensure user-visible notification links land on real owner pages
- keep notification work limited to the current in-app/user-facing notification surface

## Out of Scope

- notification preferences
- admin notification inboxes or operator notifications
- reusable template management
- email/SMS/push delivery

## Files to Read

- `frontend/src/views/app/NotificationsView.vue`
- `frontend/src/stores/notification.js`
- `frontend/src/api/modules/notification.js`
- relevant backend notification files and event producers
- support/refund/mediation user flows touched in Wave 1

## Allowed Changes

- existing notification backend/frontend files
- directly related producer files in support/order/mediation modules
- related tests
- directly related notification HTTP/API docs

## Implementation Plan

1. Identify the minimum Wave 1 state changes that should generate user notifications.
2. Emit notifications through the existing notification lane only.
3. Ensure action links route users into the real support/refund/mediation owner pages.

## Risks

- trying to solve global notification design inside a bounded Wave 1 closeout
- creating events for states that still lack a usable user-facing target page
- drifting notification semantics between support, refund, and mediation modules

## Test Plan

- Backend:
  - run notification-related tests if added
- Frontend:
  - run touched tests and frontend build
- Manual:
  - verify notification list entries appear for the accepted Wave 1 events
  - verify `actionUrl` targets are real and useful

## Acceptance Criteria

- [x] Key Wave 1 support/refund/mediation events reach the existing notification center.
- [x] Notifications land on real user-facing owner pages.
- [x] Notification closeout stays bounded to the current in-app notification system.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [x] relevant files in `docs/09-api-spec/`
- [x] task status and archive move when complete

## Completion Notes

- Added bounded `support_ticket` and `mediation_update` notifications through the existing user notification center only.
- Notification action links now land on real support-ticket and order detail pages.
