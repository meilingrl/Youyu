# Task: Wave 0 Review Summary Real Distribution

## Metadata

- ID: wave0-review-summary-real-distribution
- Status: completed
- Owner: worker-b
- Track: feature
- Depends on: `wave0-scope-lock-and-contract-boundary`
- Priority: high
- Planned date: 2026-06-01
- Completed date: 2026-06-01

## Objective

Replace the current review-summary distribution stub with real score aggregation for both product and shop public review summaries.

## Background

`docs/09-api-spec/review.md` and `ReviewServiceImpl` explicitly mark the current `distribution` array as a stub that returns all-zero counts. This is a clean bounded backend closeout task with minimal route impact.

## Scope

- implement real score-count aggregation for product review summary
- implement real score-count aggregation for shop review summary
- preserve current endpoint paths and response field names
- update tests and docs that currently describe the stub behavior

## Out of Scope

- changing review submission flow
- changing review list pagination or payload shape
- adding sentiment tags, keyword extraction, or richer analytics

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `backend/src/main/java/com/youyu/backend/service/review/impl/ReviewServiceImpl.java`
- review-related mapper interfaces and JDBC implementations
- `docs/09-api-spec/review.md`
- `docs/06-http/review.http`

## Allowed Changes

- review-related backend service/mapper/test files
- `docs/09-api-spec/review.md`
- `docs/06-http/review.http`
- related frontend tests only if summary expectations need update

## Implementation Plan

1. Add real grouped score-count queries for product and shop review summaries.
2. Preserve the existing summary response shape while filling real counts.
3. Verify both reviewed and zero-review cases.

## Risks

- returning only partial score buckets instead of the full 1-5 distribution
- changing summary field names and drifting from the existing frontend

## Test Plan

- Backend:
  - add or update review summary aggregation tests
- Frontend:
  - run relevant tests if summary assumptions change
- API validation:
  - remove stub wording from review spec and examples
- Manual:
  - verify product and shop summary endpoints return real counts

## Acceptance Criteria

- [x] Product review summary returns real `distribution` counts for scores 1-5
- [x] Shop review summary returns real `distribution` counts for scores 1-5
- [x] No-review entities still return a complete 1-5 distribution with zero counts
- [x] Existing response keys remain `avgScore`, `reviewCount`, and `distribution`

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes
- Replaced the stubbed zero-count distribution path with real `GROUP BY score` aggregation for both product and shop review summaries.
- Preserved the public summary response shape and always return all five score buckets.
- Added integration assertions for both reviewed entities and zero-review entities.
