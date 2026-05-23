# Task: Home Quick Entry Card Buttons

## Metadata

- ID: home-quick-entry-card-buttons
- Status: archived
- Owner: unassigned
- Track: feature
- Depends on: none
- Priority: medium
- Planned date: 2026-05-24
- Completed date: 2026-05-24

## Objective

Make the three homepage quick-entry cards act as the navigation controls themselves, so users can click the whole card instead of only the small action text.

## Scope

- `frontend/src/views/app/HomeView.vue`
- `CHANGELOG.md`
- This archived task record

## Delivered

1. Replaced the quick-entry `article` plus nested action button pattern with one button per card.
2. Kept the existing card visual treatment while expanding the clickable area to the full card.
3. Added a visible keyboard focus state for the card buttons.

## Out of Scope

- Route or navigation target changes.
- Backend, store, API, or data changes.
- Homepage platform-data Canvas changes.

## Test Plan

- Frontend unit tests: `npm test`
- Frontend production build: `npm run build`
- Browser smoke check:
  - Quick-entry cards render as three full-card buttons.
  - Clicking the exploration card navigates to `/app/explore`.
  - There are no nested button controls in the quick-entry cards.

## Acceptance Criteria

- [x] The three quick-entry cards are full-card buttons.
- [x] The visible card copy and action text remain available.
- [x] Keyboard focus is visible.
- [x] No route target changes are introduced.
- [x] `CHANGELOG.md` is updated.

## Completion Notes

- Verified by frontend unit tests, production build, and a Playwright smoke check against local preview.
