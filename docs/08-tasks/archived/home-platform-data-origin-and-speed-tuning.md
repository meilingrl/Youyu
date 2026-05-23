# Task: Home Platform Data Origin And Speed Tuning

## Metadata

- ID: home-platform-data-origin-and-speed-tuning
- Status: archived
- Owner: unassigned
- Track: feature
- Depends on: archived `home-platform-data-visual-polish`
- Priority: medium
- Planned date: 2026-05-24
- Completed date: 2026-05-24

## Objective

Refine the homepage platform-data Canvas scenes after visual feedback so the student and region scenes read as single-origin network bursts, while the shop scene has slower flowing particles and persistent endpoint dots.

## Scope

- `frontend/src/components/home/HomeStatsNetwork.vue`
- `CHANGELOG.md`
- This archived task record

## Delivered

1. Updated the authenticated-student scene so all straight network segments start from one shared origin point.
2. Updated the covered-region scene so network arcs start from one shared left-side hub.
3. Reduced the shop-scene moving particle speed by 50%.
4. Restored persistent static endpoint dots at both ends of shop-scene lines.
5. Kept the change limited to native Canvas drawing logic with no new dependencies.

## Out of Scope

- Backend statistics API.
- Home metric copy, rotation timing, route, store, or API changes.
- New UI controls or third-party animation libraries.

## Test Plan

- Frontend unit tests: `npm test`
- Frontend production build: `npm run build`
- Browser smoke check: local preview plus Playwright tab/canvas check
- Browser checks:
  - Student scene emits straight lines from a single point.
  - Shop scene retains endpoint dots and has slower moving particles.
  - Region scene starts arcs from a single hub without breaking the globe silhouette.
  - Mobile and reduced-motion still render static frames.

## Acceptance Criteria

- [x] Student scene uses a shared origin.
- [x] Shop-scene particle speed is halved.
- [x] Shop-scene endpoint dots stay visible after transition completes.
- [x] Region scene uses a shared origin hub.
- [x] No new dependency is added.
- [x] `CHANGELOG.md` is updated.

## Completion Notes

- Verified by frontend unit tests, production build, and a Playwright smoke check against local preview.
