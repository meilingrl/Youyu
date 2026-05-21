# Task: Review Entry And Seed Flow Bridge

## Metadata

- ID: review-entry-and-seed-flow-bridge
- Status: active
- Owner: unassigned
- Track: cross-cutting
- Depends on: current review, order, and product-detail baseline
- Priority: medium
- Planned date: 2026-05-20
- Completed date:

## Objective

Make the review flow both visible and locally testable by adding a product-detail review entry affordance and ensuring there is at least one realistic completed-order path for exercising review behavior in local/demo flow.

## Background

The issue backlog points out two real gaps:

- product detail lacks a visible "write review" entry or guidance
- current seed/demo flow may not expose a usable completed-order path, making review behavior hard to validate end to end

## Scope

- add a review-entry affordance or guidance on product detail
- make the review flow reachable or verifiable in local/demo conditions
- use the smallest safe approach to bridge the missing completed-order validation path

## Out of Scope

- redesigning the entire review system
- fabricating private user history broadly beyond the minimum needed validation path
- broad seed-data expansion unrelated to the review flow

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `frontend/README.md`
- `backend/README.md`
- `frontend/src/views/app/ProductDetailView.vue`
- `frontend/src/views/app/PendingReviewsView.vue`
- `frontend/src/stores/review.js`
- `backend/src/main/java/com/campusmarket/backend/controller/review/ReviewController.java`
- relevant order/review seed or local validation assets

## Allowed Changes

- scoped frontend review/product detail files
- minimum backend or seed/demo assets if truly required
- relevant docs/http/api-spec files only if the contract changes
- `CHANGELOG.md`
- task lifecycle files under `docs/08-tasks/`

## Implementation Plan

1. Make the review entry/guidance visible from product detail without misrepresenting eligibility.
2. Identify the smallest safe way to make a completed-order review path testable in local/demo flow.
3. Verify the resulting review path and record any remaining backend/data limits.

## Risks

- over-solving with broad fake user/order data
- implying users can review without purchase eligibility
- turning a small bridge task into a review redesign

## Test Plan

- Backend:
  - run focused tests if backend/seed files are touched
- Frontend:
  - run relevant tests/build if touched
- API validation:
  - update docs only if contract changes
- Manual:
  - verify the product detail review entry is visible and honest
  - verify there is a realistic path to exercise review behavior locally

## Acceptance Criteria

- [ ] Product detail exposes a visible review entry or eligibility explanation
- [ ] The local/demo review flow is testable with at least one realistic completed-order path
- [ ] The task documents any remaining constraints instead of hiding them

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [ ] task status and archive move

## Completion Notes
