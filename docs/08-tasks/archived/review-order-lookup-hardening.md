# Task: Review Order Lookup Hardening

## Metadata

- ID: review-order-lookup-hardening
- Status: archived
- Owner: meilingrl
- Track: cross-cutting
- Depends on: architecture-performance-hardening
- Priority: high
- Planned date: 2026-05-17
- Completed date: 2026-05-17

## Objective

Eliminate the current full-order scan used during product review submission by introducing a direct order-item lookup path that verifies ownership and order status without scanning all orders and all order items.

The aim is to harden a user-facing interactive path with the smallest possible backend change set, while preserving current review rules and response semantics.

## Background

`ReviewServiceImpl.submitProductReview()` currently resolves the owning order for an `orderItemId` through a placeholder strategy:

1. load all orders
2. iterate through each order
3. query the order items for each order
4. locate the matching order item

The source code already acknowledges that this is not suitable for production-like behavior. At the current project stage, this is one of the clearest hidden latency risks because it sits on a real user action path rather than a rare maintenance operation.

This task exists to replace that placeholder with a direct, bounded query path while keeping the rest of the review domain unchanged.

## Scope

### In Scope

- product review submission order-item lookup
- supporting mapper/query additions for:
  - locating the order by `order_item_id`
  - verifying buyer ownership
  - resolving the related product if needed
- focused cleanup in review submission flow to remove order-wide scans
- backend tests for review submission edge cases
- related HTTP or API docs if they need behavioral clarification

### In Scope But Only If Clearly Needed

- small helper methods in transaction or review support layers
- introducing a dedicated mapper rather than reusing a broad support class if that yields a cleaner query path

## Out of Scope

- schema/index additions in `schema.sql`
- admin pagination work
- storefront product list work
- review summary redesign
- shop review eligibility redesign
- order/refund/payment workflow changes unrelated to review submission lookup

## Files to Read

- `docs/04-standards/development-process.md`
- `backend/README.md`
- `backend/src/main/java/com/campusmarket/backend/service/review/ReviewService.java`
- `backend/src/main/java/com/campusmarket/backend/service/review/impl/ReviewServiceImpl.java`
- `backend/src/main/java/com/campusmarket/backend/service/transaction/support/TransactionDataStore.java`
- review-related mapper interfaces and implementations
- order-related mapper or support files if needed for direct lookup
- current backend tests covering reviews or order-linked behavior
- relevant `.http` files if the task changes validation expectations

## Allowed Changes

- `backend/src/main/java/com/campusmarket/backend/service/review/**`
- review-related mapper interfaces and implementations
- order-item/order lookup support classes required for direct query resolution
- focused backend tests for review submission and lookup validation
- review-related HTTP/API docs if needed
- `CHANGELOG.md`

## Parallelization Boundary

This task is designed to be parallel-safe with:

- `admin-query-pagination-hardening`
- `runtime-index-hardening`
- `product-list-request-flow-hardening`

### Do Not Touch In This Task

- `backend/src/main/resources/schema.sql`
- `backend/src/main/java/com/campusmarket/backend/service/admin/**`
- `frontend/**`
- product search query code

If a proposed improvement requires index changes, record it and hand it to `runtime-index-hardening` instead of editing the schema here.

## Required Behavioral Constraints

- Review ownership checks must remain intact
- Only completed orders may be reviewed
- Duplicate-review protection must remain intact
- Response shape must remain backward compatible unless explicitly documented

## Implementation Plan

1. Trace the current review submission flow.
   - identify exactly which fields are needed from the order and order item
   - avoid carrying forward unnecessary full-order detail loading

2. Design the direct lookup query path.
   - one query or one bounded join-based chain should resolve:
     - order id
     - buyer id
     - order status
     - product id

3. Implement dedicated mapper/support methods.
   - prefer explicit query methods over hidden generic plumbing
   - keep naming tied to the review use case if the method is review-specific

4. Replace the scan logic in `ReviewServiceImpl`.
   - remove `listOrders()` traversal from the product review path
   - keep validation logic readable and unchanged in outcome

5. Add regression coverage.
   - successful submission
   - missing order item
   - foreign buyer
   - non-completed order
   - duplicate submission

## Risks

- introducing a direct lookup that accidentally bypasses existing validation checks
- moving too much transaction logic out of its current location and creating a larger refactor than needed
- mixing this task with index work and colliding with another active task
- changing shop review behavior by mistake while touching the review module

## Test Plan

- Backend:
  - add focused tests for direct order-item resolution and submission behavior
  - verify current business rules still hold
- Frontend:
  - no frontend changes expected
- API validation:
  - update review-related or product-related smoke examples only if needed
- Manual:
  - complete an order
  - submit a product review
  - verify duplicate and invalid cases still fail clearly

## Acceptance Criteria

- [x] Product review submission no longer scans all orders to resolve `orderItemId`
- [x] Ownership and completed-order validation remain correct
- [x] Duplicate-review behavior remains unchanged
- [x] No schema or frontend files were modified in this task

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes

This task is intentionally narrow. It exists to remove one clear high-cost path without mixing in:

- index design
- admin query modernization
- storefront changes

Keep the implementation small, testable, and easy to review.

Completed outcome:

- Replaced the full-order traversal with a direct `order_items` + `orders` join query
- Preserved buyer ownership, completed-order, and duplicate-review protections
- Added focused regression coverage for missing order items and non-completed order submission
