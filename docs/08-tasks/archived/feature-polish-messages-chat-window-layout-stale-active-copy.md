# Task: Messages Chat Window Layout

## Metadata

- ID: feature-polish-messages-chat-window-layout
- Status: active
- Owner: worker-wave-1
- Track: feature
- Depends on: feature-polish-closeout-parent
- Priority: P0
- Planned date: 2026-06-03
- Completed date:

## Objective

Prevent the messages page chat conversation from stretching the whole page indefinitely. The selected conversation panel should keep a stable viewport-appropriate height and scroll internally.

## Background

The reported problem is layout behavior, not a new chat feature. Fix the visible page structure first, preserving the existing chat APIs and message model.

## Scope

- Make the messages page stable on desktop and mobile.
- Give the active message list an internal scroll region.
- Preserve existing conversation selection, sending, image/message-card behavior, quick replies, and empty/loading/error states.
- Ensure long conversations, long message text, and media cards do not resize the whole route.

## Out of Scope

- New chat backend endpoints.
- Rewriting chat store/API modules.
- Adding real-time websocket behavior.
- Changing support-console admin behavior.

## Files to Read

- `frontend/src/views/app/MessagesView.vue`
- `frontend/src/stores/chat.js`
- `frontend/src/api/modules/chat.js`
- `frontend/src/components/chat/ImageUploader.vue`
- `frontend/src/components/chat/ProductCardMessage.vue`
- `frontend/src/components/chat/OrderCardMessage.vue`
- `frontend/src/components/chat/QuickReplyPanel.vue`

## Allowed Changes

- `frontend/src/views/app/MessagesView.vue`
- `frontend/src/components/chat/*.vue` only when needed for local overflow/media sizing
- `frontend/src/stores/__tests__/` or nearby frontend tests if a focused layout/state test exists or is added

## Implementation Plan

1. Reproduce or inspect the current DOM/CSS path where the conversation list grows beyond the page.
2. Add stable layout constraints using viewport-aware height, min-height: 0 on flex/grid parents, and internal overflow scrolling.
3. Verify empty, loading, selected, no-selected, long-message, and image-message states.
4. Keep route and API behavior unchanged.

## Risks

- Missing `min-height: 0` on a flex ancestor can make overflow rules appear ineffective.
- Mobile bottom navigation can be covered if fixed heights ignore safe spacing.

## Test Plan

- Frontend: `cd frontend; npm test`
- Frontend build: `cd frontend; npm run build`
- Manual: open `/app/messages`, select a long conversation, confirm the page shell stays fixed while the message list scrolls.
- Manual: verify mobile viewport does not hide composer/actions behind the bottom nav.

## Acceptance Criteria

- [ ] Long conversations no longer push the full page downward.
- [ ] The visible message list scrolls inside the conversation panel.
- [ ] Message composer remains reachable.
- [ ] Empty/loading/error states still render.
- [ ] No chat endpoint or data contract changes are introduced.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] task status and archive move

## Completion Notes

