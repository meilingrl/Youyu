# Wave 0 Low-Conflict Closeout Execution Specification

## Summary

Wave 0 is a first governed multi-agent round for low-conflict feature closeout work in `Youyu`. It intentionally covers only:

- favorites closure
- real review-summary score distributions
- contract/doc alignment strictly required by those two slices

This wave does not claim category management, support/mediation boundary redesign, payment/refund upgrade, analytics dashboards, or full personalization work.

## Task Documents

| Task | Wave | Owner | Core delivery |
| --- | ---: | --- | --- |
| `wave0-low-conflict-closeout-parent` | all | main agent | status, boundaries, acceptance |
| `wave0-scope-lock-and-contract-boundary` | 0 | main agent | locked interfaces and deferred list |
| `wave0-favorites-closure` | 0 | worker A | real favorites backend + frontend contract closure |
| `wave0-review-summary-real-distribution` | 0 | worker B | real product/shop summary distributions |
| `wave0-integration-and-doc-closeout` | final | main agent | integration, verification, docs, archival |

## Locked Interfaces

- Favorites contract is frozen to:
  - `GET /api/favorites`
  - `POST /api/favorites`
  - `DELETE /api/favorites/{productId}`
- Favorites worker may not add category routes or recommendation logic.
- Review summary paths remain:
  - `GET /api/products/{id}/review-summary`
  - `GET /api/shops/{shopId}/review-summary`
- Review summary response shape remains:
  - `avgScore`
  - `reviewCount`
  - `distribution`
- `distribution` must always contain five score buckets for 1-5.
- Contract alignment in this wave may touch only favorites and review-summary assets.

## Test Plan

- Backend favorites tests pass.
- Backend review summary aggregation tests pass.
- Touched frontend tests pass.
- Touched frontend build passes.
- Favorites HTTP smoke and review HTTP smoke match the accepted implementation.
- Manual key paths:
  - favorite add/remove/list
  - product review summary
  - shop review summary

## Explicitly Deferred

- `GET /api/categories` and admin category management
- support / ticket / mediation boundary work
- payment and refund feature upgrades
- analytics collection and visualization
- full personalization/theme/notification expansion

## Main-Agent Launch Prompt

```text
Execute Wave 0 low-conflict closeout in E:\Dev\Projects\Youyu. Use Goal mode when available. Read AGENTS.md, CLAUDE.md, docs/README.md, development-process.md, the current roadmaps, and the Wave 0 task files before editing.

Confirm the base branch is clean, then create a topic branch and isolated worktree. Work only in the new worktree. Do not merge or push unless explicitly requested.

Create or verify the parent task and scope-lock task first. Freeze these locked interfaces before dispatch:
- GET /api/favorites
- POST /api/favorites
- DELETE /api/favorites/{productId}
- GET /api/products/{id}/review-summary
- GET /api/shops/{shopId}/review-summary
- review summary response keys remain avgScore, reviewCount, distribution
- distribution must contain 1-5 buckets even when all counts are zero

Dispatch Wave 0 workers in parallel with disjoint ownership:
- worker A: wave0-favorites-closure
- worker B: wave0-review-summary-real-distribution

Tell each worker:
- do not revert others' edits
- do not commit
- do not expand into categories, support/mediation, payment/refund, analytics, or personalization
- report changed files, checks run, findings, and blockers

Keep final integration, changelog, API/HTTP doc closeout, task archival, and verification with the main agent.

Final report:
1. child task status
2. branch/worktree
3. commands and results
4. blockers
5. deferred work
```

## Worker Prompt A

```text
Implement child task wave0-favorites-closure in the assigned worktree.

Ownership:
- You may edit: favorites-related backend files, frontend/src/api/modules/favorite.js, frontend/src/stores/market.js, favorites-related frontend views/tests, favorites HTTP/API docs
- Do not edit: category APIs, support/mediation files, payment/refund files, analytics/personalization files, unrelated roadmap/task files

Locked interfaces:
- GET /api/favorites
- POST /api/favorites
- DELETE /api/favorites/{productId}

Out of scope:
- category management
- recommendation/personalization based on favorites
- shop follow system
- unrelated product-list/filter refactors

Other agents are working concurrently. Do not revert their edits. Do not commit. In your final response list changed files, checks run, findings, and blockers.
```

## Worker Prompt B

```text
Implement child task wave0-review-summary-real-distribution in the assigned worktree.

Ownership:
- You may edit: review-related backend mapper/service/test files and directly related review HTTP/API docs
- Do not edit: favorites files, category files, support/mediation files, payment/refund files, analytics/personalization files, unrelated roadmap/task files

Locked interfaces:
- GET /api/products/{id}/review-summary
- GET /api/shops/{shopId}/review-summary
- response keys remain avgScore, reviewCount, distribution
- distribution always includes five score buckets for 1-5

Out of scope:
- review submission flow changes
- review list payload changes
- analytics or sentiment features

Other agents are working concurrently. Do not revert their edits. Do not commit. In your final response list changed files, checks run, findings, and blockers.
```
