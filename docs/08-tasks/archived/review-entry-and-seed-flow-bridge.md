# Task: Review Entry And Seed Flow Bridge

## Metadata

- ID: review-entry-and-seed-flow-bridge
- Status: completed
- Owner: worker-3
- Track: cross-cutting
- Depends on: current review, order, and product-detail baseline
- Priority: high
- Planned date: 2026-05-20
- Completed date: 2026-05-27

## Objective

Make the review flow visible, honest, and locally testable by adding a product-detail review entry affordance and ensuring there is at least one realistic completed-order path for exercising review behavior in local/demo flow.

## Background

The issue backlog points out two real gaps:

- product detail lacks a visible "write review" entry or guidance
- current seed/demo flow may not expose a usable completed-order path, making review behavior hard to validate end to end

## Scope

- Add a review-entry affordance or eligibility explanation on product detail.
- Link eligible or potentially eligible users toward `/app/reviews/pending`.
- Make local/demo review behavior testable with at least one realistic completed-order path.
- Keep the existing review rules intact: only completed purchased order items can receive product reviews.
- Keep pending-review and my-review pages compatible with current review store behavior.

## Out of Scope

- redesigning the entire review system
- fabricating private user history broadly beyond the minimum needed validation path
- broad seed-data expansion unrelated to the review flow
- changing product or shop review eligibility semantics
- redesigning `PendingReviewsView.vue` or `MyReviewsView.vue` beyond what is needed for reachability
- touching order detail or trade dashboard UI

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `frontend/README.md`
- `backend/README.md`
- `frontend/src/views/app/ProductDetailView.vue`
- `frontend/src/views/app/PendingReviewsView.vue`
- `frontend/src/stores/review.js`
- `backend/src/main/java/com/youyu/backend/controller/review/ReviewController.java`
- relevant order/review seed or local validation assets
- `docs/09-api-spec/review.md`
- `docs/06-http/review.http`

## Allowed Changes

- `frontend/src/views/app/ProductDetailView.vue`
- `frontend/src/views/app/PendingReviewsView.vue` only if a small entry/empty-state adjustment is required
- `frontend/src/stores/review.js` only if the current store cannot support the visible flow
- minimum backend or seed/demo assets if truly required
- relevant docs/http/api-spec files only if the contract changes
- `CHANGELOG.md`
- task lifecycle files under `docs/08-tasks/`

## Implementation Plan

1. Add a product-detail review entry block that explains review eligibility and points users to pending reviews without promising eligibility incorrectly.
2. Inspect existing seed/demo transaction data and identify the smallest realistic way to provide a completed-order review path.
3. If seed/demo data must change, make the smallest targeted addition and document the exact demo account/order path.
4. Verify `/app/reviews/pending` can show a reviewable item in local/demo conditions.
5. Record any remaining constraints instead of hiding them in copy.

## Risks

- over-solving with broad fake user/order data
- implying users can review without purchase eligibility
- turning a small bridge task into a review redesign
- colliding with the trade dashboard task by editing `/app/trade`

## Test Plan

- Backend:
  - run focused tests if backend/seed files are touched
- Frontend:
  - run `npm test`
  - run `npm run build`
- API validation:
  - update docs only if contract changes
- Manual:
  - verify the product detail review entry is visible and honest
  - verify there is a realistic path to exercise review behavior locally
  - verify duplicate or ineligible review behavior is still rejected by existing backend rules

## Acceptance Criteria

- [x] Product detail exposes a visible review entry or eligibility explanation
- [x] The local/demo review flow is testable with at least one realistic completed-order path
- [x] The task documents any remaining constraints instead of hiding them
- [x] Review eligibility rules remain unchanged
- [x] No trade dashboard, order detail, cart, checkout, or payment files are modified by this task

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [x] roadmap or standards docs if applicable (not applicable)
- [x] task status and archive move

## Required Agent Feedback

When finished, report:

- branch and commit or uncommitted status
- files changed
- exact product-detail entry behavior
- exact local/demo account and order path used to test review submission
- test commands and exit codes
- acceptance-criteria checklist
- any remaining seed or eligibility limitation

## Completion Notes

- Added a product-detail review entry block in `frontend/src/views/app/ProductDetailView.vue`. It explains that eligibility is determined by completed purchased order items and sends logged-in users to `/app/reviews/pending`; logged-out users are sent to `/login?redirect=/app/reviews/pending`.
- Kept review eligibility semantics unchanged. The frontend does not claim that the current product is reviewable; `/api/reviews/pending` remains the source of truth.
- Documented the local seed validation path in `docs/06-http/review.http`: use `mock-1010-USER`, order `8002`, generated payment number from `POST /api/payments/orders/8002/initiate`, `POST /api/payments/{paymentNo}/mock-success`, `POST /api/orders/8002/confirm-receipt`, then review order item `8102`.
- Backend verification passed: `backend\\.\\mvnw.cmd test` exited 0 with 100 tests passing.
- Head-agent frontend verification after installing dependencies: `npm test` passed with 7 test files and 30 tests; `npm run build` passed.
- Concurrent uncommitted changes from other workers exist in trade/cart/order/payment/docs scopes; this task did not edit those owned files.
