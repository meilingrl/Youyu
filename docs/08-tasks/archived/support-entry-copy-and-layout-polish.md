# Task: Support Entry Copy And Layout Polish

## Metadata

- ID: support-entry-copy-and-layout-polish
- Status: done
- Owner: Codex
- Track: cross-cutting
- Depends on: `wave1-support-and-aftersales-parent` (archived)
- Priority: high
- Planned date: 2026-06-01
- Completed date: 2026-06-02

## Objective

Close the remaining user/admin support frontend gaps by adding an obvious user-side support-ticket entry from the message center, upgrading support-facing copy to launch-ready wording, and normalizing `/app/support` page spacing with the rest of the app shell.

## Background

Wave 1 established the support-ticket and admin dual-workspace backend/frontend structure, but three frontend issues remain:

- `/app/support` exists without a clear entry inside the user message center
- support-related user/admin copy still exposes implementation-facing wording and draft-stage language
- `/app/support` does not use the standard app shell container, so page spacing is visibly tighter than neighboring views

The user explicitly wants both user-side and admin-side handling continuity preserved.

## Scope

- add a clear support-ticket interaction point from the user message center
- polish user/admin support copy to launch-ready product wording
- normalize `/app/support` shell spacing and related layout details

## Out of Scope

- backend support-ticket or chat contract changes
- notification-system redesign
- broader message-center IA redesign outside the support entry path
- unrelated copy rewrites in other modules

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/04-standards/development-process.md`
- `frontend/README.md`
- `frontend/src/views/app/MessagesView.vue`
- `frontend/src/views/app/SupportTicketsView.vue`
- `frontend/src/views/admin/SupportView.vue`
- `frontend/src/styles/index.css`
- `frontend/src/router/modules/app.js`
- `CHANGELOG.md`

## Allowed Changes

- scoped frontend files for user/admin support entry, copy, and layout
- related route metadata if support titles need polishing
- `CHANGELOG.md`
- task lifecycle files under `docs/08-tasks/`

## Implementation Plan

1. Add a visible support-ticket entry within the user message-center support lane.
2. Replace implementation-facing support copy with launch-ready user/admin wording.
3. Align `/app/support` with the shared shell container and verify spacing on desktop/mobile.

## Risks

- blending support chat and support-ticket ownership instead of presenting them as parallel lanes
- over-broad copy rewrites outside the requested support surfaces
- layout tweaks that unintentionally break mobile stacking

## Test Plan

- Backend:
  - no backend changes expected
- Frontend:
  - run `npm run build`
- API validation:
  - not required unless route or contract wording changes require doc updates
- Manual:
  - verify a user can discover support tickets from `/app/messages`
  - verify `/app/support` spacing matches nearby shell pages
  - verify `/admin/support` and `/app/support` copy contains no draft-stage or implementation-facing wording

## Acceptance Criteria

- [x] The message center exposes a clear user-facing path into `/app/support`
- [x] `/app/support` and `/admin/support` use launch-ready wording without draft-stage language
- [x] `/app/support` spacing matches the app shell standard on desktop and mobile

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes

- Replaced the half-merged message-center implementation with a single route-aware flow so the general messages hub opens on trade conversations by default and only enters support when explicitly requested.
- Restored working navigation between `/app/messages` and `/app/support`, including support-lane actions for online customer service and support tickets.
- Rewrote the affected support/message UI text and helper panels to remove garbled strings and make the wording launch-ready.
- Kept `/app/support` on the shared shell spacing standard with roomier side/detail gutters on desktop and mobile.
