# Task: Home Platform Data Network Stage

## Metadata

- ID: home-platform-data-network-stage
- Status: archived
- Owner: unassigned
- Track: feature
- Depends on: current homepage baseline
- Priority: medium
- Planned date: 2026-05-23
- Completed date: 2026-05-23

## Objective

Rework the homepage `平台数据` module from static tilted metric cards into four clickable data labels with a Canvas-based dynamic stage that expresses a warm campus transaction network.

## Background

The homepage already had an editorial hero and featured product rail, but the `平台数据` block was still a three-card static layout. The requested direction was a stronger showcase section inspired by Stripe-style metric tabs plus an animated lower stage, adapted to Youyu's warm campus marketplace style.

## Scope

- Add a homepage-only Canvas component for the platform data stage.
- Replace the old `home-stats` card grid with four metric tabs.
- Support click switching and 10-second automatic rotation.
- Keep mobile and `prefers-reduced-motion: reduce` rendering static instead of continuously animated.
- Keep the implementation frontend-only with no API, router, store, or dependency changes.

## Out of Scope

- Backend platform statistics API.
- Pinia store changes.
- Route changes.
- Global animation framework extraction.
- Third-party visualization libraries.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/04-standards/development-process.md`
- `docs/05-roadmap/current/stage-roadmap.md`
- `docs/05-roadmap/current/feature-roadmap.md`
- `docs/08-tasks/active/frontend-bundle-second-pass.md`
- `frontend/README.md`
- `frontend/src/views/app/HomeView.vue`
- `frontend/src/components/home/HomeFeaturedRail.vue`
- `frontend/src/styles/variables.css`
- `frontend/src/styles/index.css`

## Allowed Changes

- `frontend/src/components/home/HomeStatsNetwork.vue`
- `frontend/src/views/app/HomeView.vue`
- `CHANGELOG.md`
- `docs/08-tasks/archived/home-platform-data-network-stage.md`

## Delivered

1. Added `HomeStatsNetwork.vue` with native Canvas 2D renderers for:
   - `认证学生`: center-out node network.
   - `校园店铺`: left/right hourglass flows through a center node.
   - `上架商品`: layered shelf and wave-line product flows.
   - `覆盖地区`: hemisphere arc network.
2. Replaced the old three-card `平台数据` layout with four clickable metric labels.
3. Added 10-second automatic metric rotation, with click selection resetting the next rotation.
4. Added reduced-motion detection and static rendering behavior for reduced-motion and mobile states.
5. Kept the section in the existing warm Youyu palette without backend/API changes.

## Risks

- Canvas visuals are decorative and rely on manual visual inspection beyond unit tests.
- The section uses static marketing metrics until a real statistics API exists.

## Test Plan

- Backend: `mvnw.cmd test`
- Frontend: `npm test`
- Frontend build: `npm run build`
- Manual:
  - Desktop homepage metric click switching and auto rotation.
  - Mobile static Canvas composition without page overflow.
  - Reduced-motion static Canvas behavior.

## Acceptance Criteria

- [x] The homepage has four platform data metrics.
- [x] Clicking a metric switches the Canvas theme.
- [x] Metrics auto-rotate every 10 seconds unless reduced motion is requested.
- [x] Mobile rendering does not continuously animate the Canvas.
- [x] No third-party dependency is added.
- [x] No API, store, or router change is made.
- [x] `CHANGELOG.md` is updated.
- [x] Task record is archived.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] task status and archive move
- [x] `docs/06-http/` not applicable
- [x] `docs/09-api-spec/` not applicable

## Completion Notes

- Implemented as a homepage-local visual component and static metric data source.
- Verification commands are recorded in `CHANGELOG.md`.
