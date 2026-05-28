# Task: Message Center UI Polish

## Metadata

- ID: message-center-ui-polish
- Status: archived
- Owner: Codex
- Track: feature
- Depends on: message center P2 completion
- Priority: P3
- Planned date: 2026-05-27
- Completed date: 2026-05-27

## Objective

Improve the message-center interaction quality without changing the backend chat API contract.

## Background

The message center already supported P2 behavior, but the acceptance pass identified UI/UX gaps in conversation list clarity, conversation actions, composer tools, quick replies, search date filtering, notification copy/data, recall affordance, and message timestamps.

## Scope

- Conversation list readability and swipe actions.
- Composer emoji/sticker entry and image-message caption behavior.
- Scenario-scoped quick replies with custom create/delete controls.
- Message search date filtering as a floating multi-select/range calendar.
- Notification UI localization and backend seed examples.
- Message recall interaction and hover timestamps.

## Out of Scope

- WebSocket transport.
- File/object storage for real image uploads.
- New backend endpoints or schema changes.
- Full E2E browser automation.

## Files to Read

- `frontend/src/views/app/MessagesView.vue`
- `frontend/src/components/chat/MessageSearch.vue`
- `frontend/src/components/chat/QuickReplyPanel.vue`
- `frontend/src/views/app/NotificationsView.vue`
- `backend/src/main/resources/seed/seed-chat-data.sql`
- `docs/05-roadmap/current/message-center-roadmap.md`

## Allowed Changes

- Message-center frontend components and view styles.
- Message-center seed notification and quick-reply examples.
- Changelog and message-center roadmap updates.
- Archived task record.

## Implementation Plan

1. Replace the menu-only conversation actions with swipe-friendly conversation rows.
2. Add emoji/sticker composer tools and keep image messages free of file-name captions.
3. Restrict quick-reply presets to the active conversation scenario and expose custom reply CRUD.
4. Convert message search date filtering to a compact floating calendar with multi-select/range behavior.
5. Localize notification UI copy and seed realistic Chinese notification examples.
6. Improve recall and timestamp visibility through a bubble hover action rail.

## Risks

- Hover-only affordances can be difficult on touch devices; mobile fallback positions the action rail above the bubble.
- Sticker sending currently uses generated data-image payloads and remains a lightweight demo path.
- Seed updates require running the backend with the `seed` profile before acceptance data appears in an existing local database.

## Test Plan

- Backend: `backend/.\\mvnw.cmd test "-Dtest=ChatControllerTest,QuickReplyControllerTest"`
- Frontend: `frontend/npm test`
- API validation: Existing chat controller tests cover chat and quick-reply contracts.
- Manual: Refresh `/app/messages` as `zhangsan`, inspect conversation actions, emoji panel, search date filter, notification list, recall affordance, and hover timestamps.

## Acceptance Criteria

- [x] Conversation rows are clearer and support swipe pin/mute/delete actions.
- [x] Uploaded images do not render the local file name as a caption.
- [x] Emoji/sticker panel stays within the composer width.
- [x] Date search opens as a floating calendar and supports multi-select/range selection.
- [x] Quick replies show current-scenario presets and custom user replies only.
- [x] Notifications render meaningful Chinese seed data from the backend.
- [x] Recall is reachable from a stable hover action rail.
- [x] Message bubbles show timestamps on hover.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [x] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes

Completed on 2026-05-27. No endpoint contract changes were made, so HTTP smoke files and API specs did not need updates.
