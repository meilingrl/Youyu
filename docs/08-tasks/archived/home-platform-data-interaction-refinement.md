# Task: Home Platform Data Interaction Refinement

## Metadata

- ID: home-platform-data-interaction-refinement
- Status: archived
- Owner: unassigned
- Track: feature
- Depends on: archived `home-platform-data-network-stage`
- Priority: medium
- Planned date: 2026-05-23
- Completed date: 2026-05-23

## Objective

Refine the homepage `平台数据` network stage so metric switching feels continuous, the Canvas reacts to pointer movement, and theme transitions gather particles into the center before expanding into the next scene.

## Background

After the first Canvas stage implementation, the metric labels still felt too segmented, the line animations were not interactive, and theme switches were too abrupt compared with the provided Stripe reference recording.

## Scope

- Replace the separated metric-card feel with a continuous metric strip and animated active highlight.
- Add a 10-second active progress line that restarts with tab changes and respects reduced motion.
- Add desktop pointer interaction for Canvas lines and particles.
- Replace direct theme swaps with a center-gather transition before expanding the target scene.
- Preserve mobile and reduced-motion static rendering behavior.

## Out of Scope

- Backend statistics API.
- New dependencies.
- Route, store, or API changes.
- Pause or mode-switch UI controls.

## Files to Read

- `frontend/src/views/app/HomeView.vue`
- `frontend/src/components/home/HomeStatsNetwork.vue`
- Reference video: `C:\Users\Meilingluo\Videos\屏幕录制\Recording 2026-05-23 143055.mp4`

## Allowed Changes

- `frontend/src/views/app/HomeView.vue`
- `frontend/src/components/home/HomeStatsNetwork.vue`
- `CHANGELOG.md`
- `docs/08-tasks/archived/home-platform-data-interaction-refinement.md`

## Delivered

1. Converted the metric area into a connected tab strip with a sliding active background and active progress line.
2. Added Canvas pointer disturbance on desktop so nearby lines and particles bend around the cursor, while keeping the native cursor and avoiding visible pointer rings.
3. Rebuilt the Canvas renderer around one shared curve/particle field instead of separate hard-swapped scene renderers.
4. Added a theme transition pipeline:
   - current scene particles collapse into a loose center cluster,
   - the target scene expands from the center,
   - the full target renderer fades in at the end.
5. Strengthened the scene silhouettes with guide structures for the student network, shop hourglass, product shelf lines, and region hemisphere grid.
6. Kept mobile and reduced-motion behavior static after each selected scene renders.

## Risks

- Canvas interaction quality still depends on visual judgment beyond automated tests.
- The effect remains custom to the homepage instead of a shared animation system.

## Test Plan

- Frontend: `npm test`
- Frontend build: `npm run build`
- Manual/browser:
  - Desktop click begins transition and resolves into the next scene.
  - Metric strip active highlight slides instead of jumping.
  - Pointer movement changes the Canvas frame.
  - Mobile click switches to a static scene with no page overflow.
  - Reduced-motion click switches to a static scene without continuous animation.

## Acceptance Criteria

- [x] Metric labels no longer read as four isolated cards.
- [x] Active metric movement has a visible sliding transition.
- [x] Canvas responds to desktop pointer movement.
- [x] Theme switching collapses particles toward the center before expanding.
- [x] Mobile and reduced-motion rendering stay static.
- [x] No new dependency is added.
- [x] `CHANGELOG.md` is updated.
- [x] Task record is archived.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] task status and archive move
- [x] `docs/06-http/` not applicable
- [x] `docs/09-api-spec/` not applicable

## Completion Notes

- Used ffmpeg frame extraction to inspect the provided local reference recording.
- Verified the interaction with Playwright browser smoke checks after `npm test` and `npm run build`.
