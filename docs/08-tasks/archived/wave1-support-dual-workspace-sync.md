# Task: Wave 1 Support Dual-Workspace Sync

## Metadata

- ID: wave1-support-dual-workspace-sync
- Status: completed
- Owner: worker-a
- Track: cross-cutting
- Depends on: `wave1-scope-lock-and-owner-sync`
- Priority: high
- Planned date: 2026-06-01
- Completed date: 2026-06-01

## Objective

Make `/admin/support` honestly and usefully serve both accepted support lanes: online customer-service chat and durable support-ticket handling.

## Background

The repository already has:

- user ticket UI: `/app/support`
- admin support-ticket APIs: `/api/admin/support/tickets/**`
- admin online-CS chat APIs: `/api/admin/support/chat/**`

But the admin support page is currently dominated by the online-chat console and does not clearly expose the ticket queue/detail workspace as a co-equal owner lane. This creates a user/admin continuity risk: users can open support tickets, but the admin support surface does not visibly center the corresponding handling path.

## Scope

- upgrade `/admin/support` into an explicit dual-workspace page
- expose a real support-ticket queue/detail path alongside the online-CS chat workspace
- keep context links to orders, reports, mediation, users, shops, and products where helpful
- preserve the route as a single admin entry while separating owner truth in the UI

## Out of Scope

- replacing the online-CS chat system
- replacing the support-ticket contract
- adding SLA automation or workload balancing
- turning admin support into a direct buyer/seller chat moderation surface

## Files to Read

- `frontend/src/views/admin/SupportView.vue`
- `frontend/src/api/modules/admin.js`
- `frontend/src/api/modules/support.js`
- `docs/02-requirements/admin-support-console-scope.md`
- `docs/07-decisions/2026-05-30-online-customer-service-on-chat.md`
- `docs/09-api-spec/support.md`
- `docs/09-api-spec/chat.md`

## Allowed Changes

- admin support frontend files
- directly related frontend tests
- directly related admin support HTTP/API docs if the accepted rendered behavior needs clarification

## Implementation Plan

1. Split the admin support surface into clearly labeled workspaces for online CS and ticket handling.
2. Ensure the ticket workspace reflects the real ticket queue/detail/status/message path rather than a placeholder.
3. Preserve or improve contextual navigation so ticket handling can reach owner pages when platform action is needed.

## Risks

- visually merging the two support lanes until they look like one shared backend
- adding ticket mutations through chat APIs or chat mutations through ticket APIs
- leaving user-created tickets without an obvious admin handling entry

## Test Plan

- Frontend:
  - run support/admin route-related tests if touched
  - run frontend build
- Manual:
  - verify `/admin/support` can reach both online-CS chat and ticket handling
  - verify ticket lane uses `/api/admin/support/tickets/**`
  - verify chat lane uses `/api/admin/support/chat/**`

## Acceptance Criteria

- [x] `/admin/support` exposes online CS and support tickets as distinct workspaces.
- [x] A user-created support-ticket flow has an obvious admin handling path on the same route.
- [x] The admin support surface does not obscure API ownership between chat and tickets.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [x] relevant files in `docs/09-api-spec/`
- [x] task status and archive move when complete

## Completion Notes

- Rebuilt `/admin/support` into explicit `chat` and `tickets` workspaces while keeping the accepted single route.
- Ticket handling now includes queue filters, detail, status updates, public replies, internal notes, and owner-context links.
