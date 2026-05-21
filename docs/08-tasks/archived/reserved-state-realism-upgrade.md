# Task: Reserved-State Realism Upgrade

## Metadata

- ID: reserved-state-realism-upgrade
- Status: completed
- Owner: meilingrl
- Track: cross-cutting
- Depends on: current profile, shop, insight, and placeholder baseline
- Priority: medium
- Planned date: 2026-05-16
- Completed date: 2026-05-16

## Objective

Upgrade reserved, placeholder, and pending-integration states so the product feels materially more realistic, informative, and testable, while still staying honest about what is and is not backed by real data.

## Background

Several current pages intentionally show `待接入`, `预留态`, or similar placeholder states instead of fabricated metrics. That honesty is correct, but some screens still feel too empty or mechanical. The next step is not to fake business truth, but to provide richer contextual content, stronger explanatory structure, and realistic supporting content where possible.

The target is a more convincing product experience without violating the requirement that pages must not pretend local fake data is real backend truth.

## Scope

- Reserved / placeholder / pending-integration states in user-facing views
- Empty-state and informational copy quality
- Contextual content fill that increases realism without falsifying live metrics
- Reusable reserved-state / empty-state presentation patterns
- Audit of pages that still feel blank, abrupt, or under-explained

## Out of Scope

- Inventing fake real-time metrics and presenting them as true backend results
- Replacing current real data sources with mock data
- Search suggestion work
- Bundle/performance optimization as the primary goal
- Full product copy rewrite across unrelated pages

## Files to Read

- `../../04-standards/development-process.md`
- `../../05-roadmap/current/stage-roadmap.md`
- `../../02-requirements/non-functional-requirements.md`
- `../../03-architecture/tech-stack-and-architecture-principles.md`
- `../../02-requirements/user-preferences-and-profile-insights.md`
- `../../02-requirements/analytics-requirements.md`
- relevant frontend views showing `待接入` / `预留态` / placeholder states
- shared frontend styles and common components

## Allowed Changes

- frontend user-facing views with reserved or empty states
- shared UI components for placeholders, empty states, and explanatory blocks
- static content assets or mock-like supporting content only when clearly presented as guidance, examples, or structure rather than live truth
- related task and changelog documents

## Implementation Plan

1. Audit all current reserved and placeholder states and classify them by severity: acceptable, too blank, misleading, or visually weak.
2. Build a consistent reserved-state presentation pattern that separates:
   - real unavailable metrics
   - explanatory copy
   - realistic guidance or content scaffolding
3. Replace blunt `待接入`-style experiences with richer but honest alternatives where possible, such as:
   - metric definitions
   - how the data will be calculated
   - what user action will unlock this area
   - realistic example structure, non-live preview cards, or scenario content
4. Add safe content fill for pages that currently look empty, making sure no generated content is mistaken for live transactional truth.
5. Review copy and visuals for mobile and desktop readability.

## Risks

- Crossing the line from realistic scaffolding into deceptive fake data
- Inconsistent wording between pages if the pattern is not centralized
- Adding too much decorative copy and hurting scanability
- Conflicting with future real-data integration if placeholders are tightly hardcoded

## Test Plan

- Backend:
  - no backend changes expected unless a page contract must be clarified
- Frontend:
  - run unit tests for touched view helpers or components
  - verify placeholders render correctly across current reserved-state pages
- API validation:
  - only update API docs if any display rule depends on changed contract semantics
- Manual:
  - inspect profile, shop, and similar reserved-state pages on desktop and mobile
  - confirm users can distinguish between live metrics, unavailable metrics, and illustrative content

## Acceptance Criteria

- [x] Reserved-state pages feel informative rather than blank or unfinished
- [x] Live data and illustrative or explanatory content are clearly distinguishable
- [x] Reusable placeholder / reserved-state patterns reduce page-by-page inconsistency
- [x] No page relies on fake local numbers to impersonate real backend truth
- [x] Product realism improves through safe content fill, structure, and explanation

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [ ] task status and archive move

## Completion Notes

- Created `ReservedMetricCard.vue` — reusable component replacing blunt `待接入` text in metric cards with label, scope definition, data source, and muted badge
- Created `ReservedPanel.vue` — reusable component replacing plain `placeholder-block` divs with structured icon + title + description layout
- Updated `ShopView.vue` — 4 metric cards and 1 hot-products panel now use the new components when `shopInsightReserved` is true; live data branches unchanged
- Updated `ProfileView.vue` — 4 metric cards and 2 content panels (recent browses, favorite preferences) now use the new components when `userInsightReserved` is true; live data branches unchanged
- Added `viewCountSummary` and `favoriteCountSummary` definitions to `shopInsightMetricDefinitions` for completeness
- All 25 Vitest tests pass; production build succeeds with zero errors
- No backend changes, no API contract changes, no fake data introduced
