# Task: Frontend Bundle Optimization

## Metadata

- ID: frontend-bundle-optimization
- Status: archived
- Owner: Codex
- Track: cross-cutting
- Depends on:
- Priority: medium
- Planned date: 2026-05-16
- Completed date: 2026-05-16

## Objective

Reduce the frontend's initial bundle cost through low-risk dependency and routing optimizations without changing the tech stack, API contracts, or page behavior.

## Background

Baseline build analysis showed the main performance issue was not route-page size, but a heavy shared dependency path:

- `Element Plus` was globally registered through `app.use(ElementPlus)`
- `element-plus/dist/index.css` was globally loaded
- `AppLayout` and `AdminLayout` were synchronously imported into the main router bundle

This caused the production entry to preload a very large UI-library chunk and its full stylesheet even before admin-only shells or unused UI components were needed.

## Scope

- Replace global Element Plus registration with a local on-demand plugin
- Narrow imported Element Plus styles to the component set actually used in the project
- Lazy-load app and admin layout shells from the router
- Rebuild and compare bundle output

## Out of Scope

- Backend APIs and database changes
- Page rewrites or UI redesign
- Framework or build-tool migration
- Broad manual chunking refactors beyond what was necessary for this iteration

## Files to Read

- `../../04-standards/development-process.md`
- `../../05-roadmap/current/stage-roadmap.md`
- `../../05-roadmap/current/feature-roadmap.md`
- `../../../frontend/README.md`
- `../../../frontend/vite.config.js`
- `../../../frontend/src/main.js`
- `../../../frontend/src/router/modules/app.js`
- `../../../frontend/src/router/modules/admin.js`

## Allowed Changes

- `frontend/src/main.js`
- `frontend/src/plugins/*`
- `frontend/src/router/modules/*`
- relevant frontend views importing Element Plus services
- `CHANGELOG.md`
- roadmap/task documentation needed to record the result

## Implementation Plan

1. Add a local Element Plus plugin that registers only the component set actually used in the project and exports shared service APIs.
2. Remove the global Element Plus CSS entry and replace it with component-level style imports.
3. Convert `AppLayout` and `AdminLayout` to route-level lazy imports.
4. Rebuild, rerun frontend tests, and record the new baseline.

## Risks

- Missing implicit Element Plus style dependency causing regressions in dialogs, drawers, menus, or messages
- Runtime registration gaps for nested Element Plus subcomponents
- Bundle-size reduction not being large enough to materially improve initial preload

## Test Plan

- Backend:
  - none
- Frontend:
  - `npm test`
- API validation:
  - none
- Manual:
  - `npm run build`
  - inspect `dist/index.html` preload entries
  - smoke-check login, home, product list, product detail, cart, admin dashboard, and admin governance screens

## Acceptance Criteria

- [x] Global `app.use(ElementPlus)` is removed
- [x] Full `element-plus/dist/index.css` import is removed
- [x] App and admin layout shells are lazy-loaded from the router
- [x] Frontend build passes after the optimization
- [x] Frontend unit tests continue to pass

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] roadmap update for current feature status
- [x] task archived with completion notes

## Completion Notes

- Implemented a new local plugin at `frontend/src/plugins/element-plus.js` for on-demand component registration.
- Added `frontend/src/plugins/element-plus-services.js` so frontend views can share `ElMessage` / `ElMessageBox` exports without coupling tests to CSS imports.
- Converted `AppLayout` and `AdminLayout` to lazy route components.
- Post-change build baseline:
  - `element-plus` JS chunk reduced from `773.84 kB` to `296.86 kB`
  - `element-plus` CSS reduced from `357.13 kB` to `163.74 kB`
  - preloaded entry assets dropped from about `1472 kB` minified / `416 kB` gzip to about `744 kB` minified / `222 kB` gzip
- Validation completed with `npm run build` and `npm test` passing after the final implementation.
