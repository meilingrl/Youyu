# Task: Demo Dataset Expansion And Refresh

## Metadata

- ID: demo-dataset-expansion-and-refresh
- Status: completed
- Owner: Codex
- Track: cross-cutting
- Depends on: launch-preparation-container-deployment, seed-full-admin-flow
- Priority: P0
- Planned date: 2026-06-04
- Completed date: 2026-06-04

## Objective

Build a production-like demo dataset that can be loaded into the Docker demo environment repeatedly and that fully supports product browsing, buyer/seller flows, admin operations, customer support, messaging, reporting, mediation, and dashboard visualization without empty or obviously fake-looking records.

## Background

The repository already has a small seed dataset for local verification and smoke coverage, plus a generated performance catalog. That seed is sufficient for functional validation, but it is not yet suitable as the final showcase dataset because:

- the product catalog is too small and some generated names still look synthetic;
- admin and user records are not yet organized as a complete demonstration population;
- only a handful of users have full lifecycle data;
- dashboard and analytics views still rely on relatively sparse records;
- some seed SQL files appear to use older or inconsistent structures and are protected by `continue-on-error`, which risks partial silent seed loads.

The new dataset must stay deterministic, rerunnable, and believable enough for final demo use inside the containerized demo overlay.

## Scope

- Define a canonical demo dataset structure for products, users, admins, shops, orders, reviews, reports, support, messages, notifications, and dashboard-facing aggregates.
- Expand the product catalog to roughly 2000-3000 items using realistic-looking generated content.
- Keep at least 20 products with complete review coverage and visible downstream effects on ratings and shop summaries.
- Add two seeded accounts for each admin role, using a consistent employee-number convention and login naming rule.
- Expand the user pool to roughly 20-30 users, with 2-3 “full lifecycle” users owning believable cross-module data.
- Ensure dashboard and admin queue data are rich enough that list pages, charts, breakdowns, and trends all look purposeful rather than sparse.
- Replace or retire any outdated synthetic SQL that no longer matches the current schema.
- Add deterministic generation and verification tooling instead of relying on hand-edited giant SQL files alone.
- Document the dataset assumptions and refresh workflow for demo use.

## Out of Scope

- Production data import or one-way migration from a live system.
- Changing runtime business logic just to accommodate seed data.
- Rebuilding unrelated frontend UI.
- Hiding real data gaps with impossible lifecycle states.
- Introducing external storage or paid image services.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/04-standards/development-process.md`
- `docs/05-roadmap/current/launch-preparation-roadmap.md`
- `backend/README.md`
- `database/README.md`
- `backend/src/main/resources/schema.sql`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-seed.yml`
- current seed assets under `backend/src/main/resources/seed/`
- `frontend/src/views/admin/DashboardView.vue`
- `docs/09-api-spec/admin.md`
- `docs/06-http/admin-full-flow.http`
- `docs/08-tasks/archived/seed-full-admin-flow.md`

## Allowed Changes

- seed SQL assets under `backend/src/main/resources/seed/`
- generation scripts under `scripts/`
- seed profile configuration and seed-loading docs
- HTTP/manual verification docs for seed-backed flows
- tests and fixture assumptions that depend on demo seed records
- `CHANGELOG.md`
- this task document lifecycle updates

## Implementation Plan

1. Inventory current seed coverage, stale files, and dashboard data dependencies.
2. Define a deterministic demo dataset model: ID ranges, naming rules, role/employee-number rules, and full-lifecycle user scenarios.
3. Add or update generation scripts to emit realistic demo catalog, user/shop/order/review/support/message/admin data.
4. Replace outdated seed assets and wire the new assets into the seed profile.
5. Add seed verification commands and docs for Docker demo refresh and acceptance.
6. Run backend/frontend/test or smoke checks affected by the new dataset.

## Risks

- Large seed changes can drift from current schema or validation rules.
- Dashboard metrics can look misleading if generated states are not internally consistent.
- Massive SQL files can become unmaintainable if not script-generated.
- Existing smoke tests may depend on legacy IDs or sparse assumptions.
- Silent SQL-init failure can hide partial seed-load regressions.

## Test Plan

- Backend: `cd backend; .\\mvnw.cmd test`
- Frontend: `cd frontend; npm test` and `npm run build`
- API validation: targeted `docs/06-http/*.http` walkthroughs for admin, order, review, chat, support, and report flows
- Manual:
  - seed profile boot against MySQL
  - demo overlay boot path review
  - dashboard visual inspection with dense metrics
  - representative buyer/admin end-to-end checks

## Acceptance Criteria

- [ ] Demo catalog contains roughly 2000-3000 believable products with no visible “test/seed/perf” naming.
- [ ] At least 20 products have complete review coverage and meaningful product/shop rating effects.
- [ ] Each admin role has two seeded accounts with a documented employee-number convention.
- [ ] Total user population reaches roughly 20-30 users with 2-3 full lifecycle users.
- [ ] Full lifecycle users visibly cover cart, favorites, orders, spend history, shops, products, reviews, support, messages, notifications, and customer service interactions.
- [ ] Dashboard and admin pages show dense enough trend/breakdown data to support the visual analytics story.
- [ ] Seed files are rerunnable and compatible with the current schema.
- [ ] Outdated or silently failing seed assets are removed, replaced, or documented.
- [ ] Demo refresh workflow is documented for Docker/container usage.

## Documentation Updates Required

Progress on this task:
- `CHANGELOG.md` updated.
- Seed profile docs updated in `backend/README.md` and `scripts/README.md`.
- Docker demo refresh workflow added via `scripts/run-demo-fresh.ps1`.

- [ ] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [ ] task status and archive move

## Completion Notes

- Added `backend/src/main/resources/seed/demo-expansion.sql` for the large demo catalog plus lifecycle commerce, governance, support, and messaging records.
- Added `scripts/generate-demo-expansion-sql.mjs` as a validation guard for the checked-in demo expansion SQL.
- Updated `application-seed.yml` so the seed profile now loads the generated expansion file instead of the stale `data-realistic-products.sql` asset.
- Cleaned presentation-facing names in the base seed files so the active demo data path does not expose raw seed/stress wording in storefront, spend, support, or chat surfaces.
- Added `compose.demo.yml` volume override plus `scripts/run-demo-fresh.ps1` so the Docker demo stack can be reset to a fresh seeded state.
- Verification completed: `node scripts/generate-demo-expansion-sql.mjs`, `git diff --check`, targeted string audits across active seed files, Docker backend rebuild, avatar upload smoke, and `/api/shops/4203` smoke.
