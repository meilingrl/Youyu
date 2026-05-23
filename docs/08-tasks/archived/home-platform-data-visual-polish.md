# Task: Home Platform Data Visual Polish

## Metadata

- ID: home-platform-data-visual-polish
- Status: archived
- Owner: unassigned
- Track: feature
- Depends on: archived `home-platform-data-interaction-refinement`
- Priority: medium
- Planned date: 2026-05-23
- Completed date: 2026-05-23

## Objective

Refine the homepage platform-data Canvas after visual feedback: remove accidental-looking static particles, soften the section boundary, remove repeated baseline lines, and make the first student-network scene more open and natural.

## Scope

- `frontend/src/components/home/HomeStatsNetwork.vue`
- `frontend/src/views/app/HomeView.vue`
- `CHANGELOG.md`
- This archived task record

## Delivered

1. Removed separate static anchor particles from the Canvas scenes.
2. Removed the product scene's fixed endpoint rows and horizontal guide-line treatment.
3. Made the Canvas background rely on the parent section gradient, with only subtle warm glow layers inside the Canvas.
4. Added shared section and stage gradients so the platform-data area transitions into the page instead of reading as a hard-edged block.
5. Reworked the student network from a tight center/ring origin into a loose inner cloud expanding toward outer campus-network endpoints.
6. Loosened the transition cluster so theme switching gathers particles into a wider cloud instead of a tight center circle.
7. Tuned the region scene hemisphere grid so the globe silhouette remains readable without extra static point clutter.
8. Followed the latest visual feedback by making the student scene use straight segments, removing fixed shop endpoint/throat particles, restoring the product endpoint rows, and rebuilding the region scene as a left-hub hemisphere fan.

## Out of Scope

- Backend statistics API.
- New frontend dependencies.
- Route, store, or API changes.
- Pause, mode switch, or extra control UI.

## Test Plan

- Frontend unit tests: `npm test`
- Frontend production build: `npm run build`
- Browser checks:
  - Desktop student scene has no center particle ring.
  - Product scene has no fixed endpoint rows or horizontal guide rows.
  - Region scene still reads as a hemisphere/globe.
  - Section boundary blends with the page background.
  - Mobile and reduced-motion render static frames.

## Acceptance Criteria

- [x] Extra static Canvas anchor particles are removed.
- [x] Repeated bottom/horizontal guide rows are removed.
- [x] Platform-data section boundary is visually softer.
- [x] Student network is more open and does not cluster into a center ring.
- [x] Region globe remains recognizable.
- [x] No new dependency is added.
- [x] `CHANGELOG.md` is updated.

## Completion Notes

- Verified by frontend tests, production build, and Playwright visual smoke checks.
