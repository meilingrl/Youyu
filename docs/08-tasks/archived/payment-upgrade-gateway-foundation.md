# Task: Payment Gateway Foundation And Alipay Sandbox Adapter

## Metadata

- ID: F2-payment-gateway-foundation
- Status: completed
- Owner: worker-backend-gateway
- Track: feature
- Depends on: `payment-upgrade.md`
- Priority: P1
- Planned date: 2026-05-31
- Completed date: 2026-05-31

## Objective

Create the backend payment-gateway foundation, keep mock mode compatible, and
add an opt-in Alipay sandbox adapter with callback verification.

## Scope

- Introduce a gateway router keyed by payment method.
- Expand the gateway contract for initiation, callback verification, status
  updates, and refund execution.
- Preserve the existing local/test mock-success route.
- Add opt-in Alipay sandbox configuration through environment variables.
- Add additive payment persistence fields and migration SQL where required.
- Add focused backend tests for success, failure, cancellation, timeout,
  retries, callback replay, ownership, amount verification, and missing config.

## Out of Scope

- Frontend views.
- Refund request UX.
- Commercial credentials, production launch, WeChat Pay, split payments, or
  reconciliation.
- Customer-service and mediation modules.

## Files to Read

- `backend/src/main/java/com/youyu/backend/controller/payment/`
- `backend/src/main/java/com/youyu/backend/service/payment/`
- `backend/src/main/java/com/youyu/backend/service/transaction/support/TransactionDataStore.java`
- `backend/src/main/resources/schema.sql`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/youyu/backend/payment/PaymentEdgeCaseTest.java`

## Allowed Changes

- `backend/pom.xml`
- `backend/src/main/java/com/youyu/backend/controller/payment/`
- `backend/src/main/java/com/youyu/backend/service/payment/`
- `backend/src/main/java/com/youyu/backend/mapper/payment/`
- `backend/src/main/java/com/youyu/backend/entity/payment/`
- payment-focused sections of
  `backend/src/main/java/com/youyu/backend/service/transaction/support/TransactionDataStore.java`
- payment-focused additive sections of `backend/src/main/resources/schema.sql`
- `backend/src/main/resources/application*.yml`
- additive payment migration file under `database/`
- payment-focused backend tests

## Locked Interfaces

- Preserve `POST /api/payments/orders/{orderId}/initiate`.
- Preserve `POST /api/payments/{paymentNo}/mock-success` in local/test mode.
- Alipay sandbox credentials come only from environment variables.
- Missing Alipay configuration must not break mock mode.
- Do not alter order main-status names or fulfillment transitions.
- All schema work must be additive. Do not use destructive DDL.

## Acceptance Criteria

- [x] Mock payment tests remain compatible.
- [x] Sandbox configuration is optional and validated when enabled.
- [x] Callback signature verification and replay protection are implemented.
- [x] Amount is validated server-side.
- [x] Failed, cancelled, and timed-out attempts can be retried safely.
- [x] Changed-file list stays within ownership.

## Completion Notes

- Added mock-compatible payment gateway routing and opt-in Alipay sandbox
  `alipay.trade.precreate`, RSA2 callback verification, amount checks, replay
  protection, timeout, retry, and additive schema changes.
- Main-agent integration added active-payment resume so a refreshed buyer page
  can recover the same payment entry without creating a duplicate record.
- Verified by the accepted payment/refund focused backend suite on 2026-05-31.
