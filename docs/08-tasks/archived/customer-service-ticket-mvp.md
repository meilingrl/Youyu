# Task: Customer Service Ticket MVP

## Metadata

- ID: customer-service-ticket-mvp
- Status: archived
- Owner: multi-agent
- Track: cross-cutting
- Depends on: `docs/02-requirements/customer-service-ticket-scope.md`
- Priority: high
- Planned date: 2026-05-30
- Completed date: 2026-05-30

## Objective

Implement the first durable customer-service feature: user-created support tickets plus an admin support-ticket queue.

## Background

`/admin/support` previously acted as a context dashboard only. The next step is a bounded customer-service MVP that persists support tickets and lets support agents triage, reply, add internal notes, and change ticket status.

This task must not turn support into real-time chat, mediation, or order/refund ownership.

## Scope

- Add support-ticket schema and seed examples.
- Add user support-ticket REST endpoints under `/api/support/tickets`.
- Add admin support-ticket REST endpoints under `/api/admin/support/tickets`.
- Add user `/app/support` page for creating and following tickets.
- Upgrade admin `/admin/support` into a ticket queue and detail workspace while preserving context links.
- Add backend tests, HTTP smoke examples, and API specs.

## Out of Scope

- WebSocket, SSE, realtime chat, or admin participation in `/api/chat/**`.
- Mediation creation, mediation decisions, or buyer/seller chat transcript access.
- Mutating order, refund, report, product, shop, or user status from the support-ticket flow.
- Notification delivery, SLA timers, auto-assignment, workload balancing, and group governance.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/02-requirements/customer-service-ticket-scope.md`
- `docs/02-requirements/admin-support-console-scope.md`
- `docs/02-requirements/chat-mvp-scope.md`
- `docs/02-requirements/platform-mediation-scope.md`
- `frontend/README.md`
- `backend/README.md`
- `backend/src/main/resources/schema.sql`
- `backend/src/main/resources/seed/data.sql`
- `backend/src/main/java/com/youyu/backend/controller/admin/AdminController.java`
- `frontend/src/views/admin/SupportView.vue`
- `frontend/src/router/modules/app.js`
- `frontend/src/router/modules/admin.js`

## Allowed Changes

- `backend/src/main/resources/schema.sql`
- `backend/src/main/resources/seed/data.sql`
- New backend support domain files under:
  - `backend/src/main/java/com/youyu/backend/controller/support/`
  - `backend/src/main/java/com/youyu/backend/service/support/`
  - `backend/src/main/java/com/youyu/backend/mapper/support/`
- Existing backend admin controller/service files only for `/api/admin/support/tickets` integration if a dedicated admin support controller is not used.
- Backend tests under `backend/src/test/java/com/youyu/backend/support/`
- `frontend/src/api/modules/support.js`
- `frontend/src/api/modules/admin.js`
- `frontend/src/api/index.js`
- `frontend/src/router/modules/app.js`
- `frontend/src/views/app/SupportTicketsView.vue`
- `frontend/src/views/admin/SupportView.vue`
- Related shared frontend styles only if needed for the support pages.
- `docs/06-http/support.http`
- `docs/06-http/admin.http`
- `docs/09-api-spec/support.md`
- `docs/09-api-spec/admin.md`
- `docs/05-roadmap/current/admin-module-goal-roadmap.md`
- `CHANGELOG.md`
- This task document and prompt docs under `docs/08-tasks/active/`

Exact SQL authorized for `schema.sql` is the SQL block in `docs/02-requirements/customer-service-ticket-scope.md`.

## Implementation Plan

1. Implement backend persistence, service validation, controllers, seed data, and backend tests.
2. Implement frontend API wrappers, `/app/support`, and `/admin/support` ticket workflow.
3. Update API specs, HTTP smoke files, changelog, and roadmap notes.
4. Run backend tests and frontend test/build verification.
5. Fill completion notes and archive this task after acceptance.

## Risks

- Accidentally reusing `/api/chat/**` as an admin support surface.
- Letting support-ticket status mutate order/report/mediation state.
- Colliding with existing uncommitted admin-workbench UX changes.
- Schema additions must stay additive and H2-compatible.

## Test Plan

- Backend: `.\mvnw.cmd test`
- Frontend: `npm test`; `npm run build`
- API validation: review `docs/06-http/support.http` and admin support examples.
- Manual: check `/app/support` and `/admin/support` route behavior in a local browser if a dev server is started.

## Acceptance Criteria

- [x] Users can create, list, view, and reply to their own support tickets.
- [x] Users cannot view or reply to another user's support ticket.
- [x] Admin support users can list, view, assign/status-change, publicly reply, and add internal notes.
- [x] Invalid status transitions and closed-ticket replies are rejected.
- [x] `/app/support` is user-facing and does not imply realtime chat.
- [x] `/admin/support` is a ticket queue/detail workspace with context links.
- [x] No support-ticket UI or backend flow calls `/api/chat/**`.
- [x] API specs, HTTP smoke files, changelog, and roadmap notes are updated.
- [x] `docs/09-api-spec/support.md` matches the final controller DTOs and response shapes.
- [x] `docs/06-http/support.http` and the admin support-ticket examples in `docs/06-http/admin.http` run against seeded data or have their seed assumptions documented.
- [x] `docs/02-requirements/admin-support-console-scope.md` clearly separates support tickets from the existing context dashboard and implemented mediation v1.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] `docs/06-http/support.http`
- [x] `docs/06-http/admin.http`
- [x] `docs/09-api-spec/support.md`
- [x] `docs/09-api-spec/admin.md`
- [x] `docs/02-requirements/admin-support-console-scope.md`
- [x] roadmap notes if the support baseline changes
- [x] task status and archive move

## Completion Notes

Documentation worker note, 2026-05-30:

- Added draft support-ticket API spec and HTTP smoke coverage for the MVP contract.
- Updated admin API/spec references, support-console scope drift, roadmap baseline, and changelog draft.
- Final acceptance compared the support spec and HTTP smoke files against the completed backend controllers, response shapes, and seeded ticket IDs.

Completed 2026-05-30:

- Added `support_tickets` and `support_ticket_messages` persistence with seed records `6201` and `6202`.
- Added user `/api/support/tickets` create/list/detail/reply endpoints and admin `/api/admin/support/tickets` queue/detail/status/message endpoints.
- Added `ADMIN_SUPPORT_TICKETS_HANDLE` and granted it to legacy admin, super admin, and support agent roles.
- Added `/app/support` for asynchronous user support tickets and rebuilt `/admin/support` as a ticket queue/detail workspace.
- Verified no support-ticket UI or backend flow calls `/api/chat/**`.
- Verification passed:
  - `backend`: `.\mvnw.cmd test` — 154 tests passed.
  - `frontend`: `npm test` — 39 tests passed.
  - `frontend`: `npm run build` — passed; only third-party `@vueuse/core` Rollup annotation warnings.
  - repo: `git diff --check` — passed.
