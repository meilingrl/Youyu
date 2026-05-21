# Task Record: UI Redesign Messages And Support Entry

## Metadata

- ID: ui-redesign-messages-support
- Status: completed
- Owner: Codex
- Track: feature
- Depends on: `ui-redesign-shell-navigation-foundation`
- Priority: medium
- Planned date:
- Completed date: 2026-05-17

## Objective

Build the first-pass information architecture and UI shell for the storefront message center and the admin support entry, while keeping all chat and support behavior explicitly in placeholder mode until a real backend exists.

## Background

The redesigned frontend information architecture promotes messages to a first-level user capability instead of leaving communication scattered inside product, shop, or order details. The admin side also needs a clear `/admin/support` entry so later customer-service, after-sales assistance, group governance, and abnormal-message handling have an agreed home.

Current scope must stay honest:

- no real message submission
- no WebSocket
- no backend message model
- no database changes
- no mock data mixed into production stores

## Scope

- storefront `/app/messages` responsive shell
- visible conversation categories for trade, shops, support, and groups
- conversation list and conversation detail placeholder states
- mobile list/detail switching
- future deep-link conventions from product, shop, and order pages into the message center
- admin `/admin/support` first-pass governance entry

## Out of Scope

- realtime chat backend
- WebSocket or long connection transport
- message tables or schema work
- real send-message APIs
- full support governance or moderation workflow
- full group-chat permissions and business rules

## Files Read

- `../../03-architecture/ui-ux-constitution.md`
- `../../03-architecture/frontend-information-architecture.md`
- `../../04-standards/frontend-redesign-safety.md`
- `../../02-requirements/communication-and-after-sales-boundary.md`
- `../../../frontend/src/router/modules/app.js`
- `../../../frontend/src/router/modules/admin.js`
- `../../../frontend/src/constants/navigation.js`
- `../../../frontend/src/layouts/AppLayout.vue`
- `../../../frontend/src/layouts/AdminLayout.vue`

## Changes

- Rebuilt `frontend/src/views/app/MessagesView.vue` into a responsive message-center shell with:
  - category switching for trade, shop, support, and group communication
  - placeholder conversation list and detail view
  - explicit disabled composer state
  - mobile list/detail switching
  - visible entry-context convention display from route query parameters
- Added `frontend/src/views/admin/SupportView.vue` as the first admin support and governance entry.
- Added `/admin/support` route and admin navigation entry.
- Updated product, shop, and order flows to start using message-entry route conventions instead of pretending real chat already exists.

## Risks Addressed

- Prevented the UI from implying that a complete chat backend already exists.
- Kept placeholder data page-local instead of storing it in production Pinia state.
- Limited admin support scope to a governance entry rather than expanding into a full support system.

## Test Plan

- Frontend: `npm run test`
- Frontend build: `npm run build` if possible
- Manual:
  - verify `/app/messages`
  - verify category empty state and disabled composer
  - verify mobile list/detail switching
  - verify `/admin/support`

## Acceptance Criteria

- [x] Messages exists as a clear first-level front-office capability.
- [x] Conversation categories are visible and understandable.
- [x] UI does not pretend unavailable backend actions are complete.
- [x] Admin support entry exists and stays clear and restrained.
- [x] Future backend/API needs are listed in completion notes.

## Documentation Updates

- [x] `CHANGELOG.md`
- [x] task status and archive move
- [ ] `docs/06-http/` not updated because no backend endpoint was introduced
- [ ] roadmap / standards docs not updated because the architecture direction was already documented

## Completion Notes

Frontend outcome:

- `/app/messages` now shows a real shell instead of a single placeholder block.
- Storefront entry conventions are visible through route query context rather than hidden assumptions.
- `/admin/support` now exists as the first landing page for future support and message governance work.

Future backend / API requirements discovered during implementation:

1. Conversation list API with pagination, category, status, unread count, last-message summary, and related entry context.
2. Conversation detail API with message timeline, system events, read state, attachments, and disabled-state reasons.
3. Message send API with duplicate-submit protection, attachment handling, and role-aware validation.
4. Order-linked support context model so after-sales messages can reference orders, refunds, reports, and platform intervention without inventing a separate full after-sales entity too early.
5. Admin support APIs for queue listing, assignee / status transitions, abnormal-message signals, and future group-governance actions.
