# Task: Cart And Payment API Spec Alignment

## Metadata

- ID: cart-payment-api-spec-alignment
- Status: completed
- Owner: worker-4
- Track: cross-cutting
- Depends on: current cart, order, payment, and review API baseline
- Priority: high
- Planned date: 2026-05-27
- Completed date: 2026-05-27

## Objective

Close the formal API documentation gap for cart and payment behavior used by the trade flow, and align HTTP smoke assets with current code behavior.

## Background

The trade UI depends on cart, order, payment, and review endpoints. `docs/09-api-spec/order.md` explicitly excludes cart and payment endpoints, while `docs/06-http/order.http` includes cart and payment examples. This makes the trade contract harder for agents to verify and increases drift risk.

This is a documentation and validation-asset task by default. It should not change runtime code unless a clear spec/code mismatch blocks accurate documentation.

## Scope

- Add a formal cart API spec document.
- Add a formal payment API spec document.
- Update `docs/README.md` API-spec module list if new spec files are added.
- Review `docs/06-http/order.http` and decide whether cart/payment examples should remain there or be split into dedicated files.
- Fix obvious smoke-example drift, including unsupported query examples if confirmed by controller/service behavior.
- Document mock-payment semantics clearly.

## Out of Scope

- Changing backend endpoint contracts.
- Adding real payment-provider integration.
- Changing frontend API modules.
- Changing order, review, report, or admin behavior.
- Implementing pagination/filtering unless already supported by code.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/04-standards/development-process.md`
- `docs/09-api-spec/order.md`
- `docs/09-api-spec/review.md`
- `docs/06-http/order.http`
- `frontend/src/api/modules/order.js`
- `frontend/src/api/modules/payment.js`
- `backend/src/main/java/com/youyu/backend/controller/order/CartController.java`
- `backend/src/main/java/com/youyu/backend/controller/payment/PaymentController.java`
- relevant cart/payment service methods if response or error behavior is unclear

## Allowed Changes

- New `docs/09-api-spec/cart.md`
- New `docs/09-api-spec/payment.md`
- `docs/README.md`
- `docs/06-http/order.http`
- New `docs/06-http/cart.http` and/or `docs/06-http/payment.http` if splitting improves clarity
- `docs/08-tasks/active/cart-payment-api-spec-alignment.md`
- `CHANGELOG.md` when the task is completed

## Implementation Plan

1. Inspect cart and payment controllers/services to identify current endpoints, auth requirements, request bodies, response shape, and common error cases.
2. Create `cart.md` for cart list, add item, update item, and remove item.
3. Create `payment.md` for gateway info, payment initiation, and mock success.
4. Update docs index references so the new specs are discoverable.
5. Align HTTP smoke examples with the formal specs; remove or annotate unsupported examples rather than implying unimplemented behavior.

## Risks

- Accidentally documenting aspirational behavior as current behavior.
- Mixing this docs task with runtime fixes.
- Moving HTTP examples without preserving smoke-test usability.

## Test Plan

- Backend:
  - Not required unless runtime code changes.
- Frontend:
  - Not required unless frontend code changes.
- API validation:
  - Manually inspect HTTP request examples for endpoint/path/header/body consistency.
- Manual:
  - Confirm every documented cart/payment endpoint exists in the current controller code.
  - Confirm every cart/payment HTTP example maps to a documented endpoint.

## Acceptance Criteria

- [x] Cart endpoints have a formal API spec.
- [x] Payment endpoints have a formal API spec.
- [x] `docs/README.md` lists the new API spec files.
- [x] HTTP smoke examples no longer imply unsupported cart/payment/order behavior.
- [x] No runtime code is changed unless a documented blocker required it and was reported.

## Documentation Updates Required

- [x] task document created
- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [x] `docs/README.md`
- [x] task status and archive move

## Required Agent Feedback

When finished, report:

- branch and commit or uncommitted status
- files changed
- list of documented cart endpoints
- list of documented payment endpoints
- any HTTP examples moved, removed, or corrected
- whether runtime code was untouched
- acceptance-criteria checklist

## Completion Notes

- Added `docs/09-api-spec/cart.md` documenting `GET /api/cart`, `POST /api/cart/items`, `PATCH /api/cart/items/{cartItemId}`, and `DELETE /api/cart/items/{cartItemId}` from current controller/service behavior.
- Added `docs/09-api-spec/payment.md` documenting `GET /api/payments/gateway`, `POST /api/payments/orders/{orderId}/initiate`, and `POST /api/payments/{paymentNo}/mock-success`, including current mock-payment semantics.
- Split cart and payment smoke examples out of `docs/06-http/order.http` into `docs/06-http/cart.http` and `docs/06-http/payment.http`.
- Corrected `docs/06-http/order.http` to remove unsupported status-filter examples and to include the required `fulfillmentType` in order creation examples.
- Runtime frontend/backend code was untouched.
