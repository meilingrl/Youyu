# Task: Payment Refund Consistency

## Metadata

- ID: F2-payment-refund-consistency
- Status: completed
- Owner: worker-backend-refund
- Track: cross-cutting
- Depends on: `payment-upgrade-gateway-foundation.md`
- Priority: P1
- Planned date: 2026-05-31
- Completed date: 2026-05-31

## Objective

Route refund completion through the payment gateway contract and keep order,
payment, and refund records consistent under retries and partial failures.

## Scope

- Integrate refund execution with the payment gateway router.
- Keep refund application in the order domain.
- Update refund records only after gateway-confirmed completion.
- Add idempotency for duplicate admin completion and callback/provider retries.
- Select the successful payment record explicitly when creating a refund.
- Add focused backend tests.

## Out of Scope

- Refund negotiation UI.
- Customer-service, mediation, or report ownership changes.
- Partial refunds unless required by the existing full-refund contract.
- Production reconciliation jobs.

## Allowed Changes

- refund-focused methods in
  `backend/src/main/java/com/youyu/backend/service/order/impl/OrderServiceImpl.java`
- refund-focused methods in
  `backend/src/main/java/com/youyu/backend/service/transaction/support/TransactionDataStore.java`
- `backend/src/main/java/com/youyu/backend/service/payment/`
- `backend/src/main/java/com/youyu/backend/mapper/payment/`
- `backend/src/main/java/com/youyu/backend/entity/payment/`
- refund-focused additive sections of `backend/src/main/resources/schema.sql`
- additive payment/refund migration file under `database/`
- refund-focused backend tests

## Locked Interfaces

- Refund request remains `POST /api/orders/{orderId}/refunds`.
- Admin refund completion remains
  `POST /api/admin/orders/{orderId}/refunds/{refundId}/complete`.
- Order-level payment status remains `refunding` then `refunded`.
- A gateway failure must not falsely mark the refund or order as completed.

## Acceptance Criteria

- [x] Refund completion invokes the selected payment gateway.
- [x] Duplicate refund completion is idempotent.
- [x] Gateway failure leaves a diagnosable non-completed refund state.
- [x] The refunded payment record is selected explicitly and safely.
- [x] Changed-file list stays within ownership.

## Completion Notes

- Refund application selects the latest successful payment explicitly.
- Admin completion executes the selected gateway, remains idempotent when
  repeated, and preserves a diagnosable failed refund without falsely marking
  the order refunded when provider execution fails.
- Verified by focused refund tests and the accepted payment/refund suite on
  2026-05-31.
