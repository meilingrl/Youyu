# API Spec: payment

## Document Info

- Status: active
- Source of truth:
  - authenticated controller: `backend/src/main/java/com/youyu/backend/controller/payment/PaymentController.java`
  - provider callback controller: `backend/src/main/java/com/youyu/backend/controller/payment/PaymentGatewayCallbackController.java`
  - service: `backend/src/main/java/com/youyu/backend/service/payment/impl/PaymentServiceImpl.java`
  - request sample: `docs/06-http/payment.http`
- Last updated: 2026-05-31

## Scope

This document covers payment gateway discovery, payment initiation, active
payment resumption, local mock completion, and the Alipay sandbox asynchronous
callback. Refund request and admin completion remain order-domain endpoints and
are documented in `order.md`.

Mock payment remains the default for local development and automated tests.
Alipay sandbox is an opt-in integration path configured through environment
variables.

## Authentication

Endpoints under `/api/payments` require `Authorization: Bearer <token>`.
The buyer must own the target order.

`POST /api/payments/callbacks/alipay-sandbox` is intentionally public because
Alipay delivers asynchronous notifications from outside the application. It
performs RSA2 signature verification, payment-channel matching, amount
validation, and callback replay protection before changing state.

## Endpoints

### `GET /api/payments/gateway`

Returns the default method and currently enabled methods.

`data` fields:

| Field | Type | Notes |
| --- | --- | --- |
| `defaultGateway` | string | Current value is `MOCK`. |
| `defaultPaymentMethod` | string | Current value is `mock`. |
| `availableMethods` | array | Includes `mock`; includes `alipay_sandbox` only when sandbox configuration is complete and enabled. |

### `POST /api/payments/orders/{orderId}/initiate`

Creates one payment attempt for an order in `pending_payment`.

Query parameters:

| Field | Required | Notes |
| --- | --- | --- |
| `paymentMethod` | no | Defaults to `mock`; sandbox value is `alipay_sandbox`. |

`data` fields:

| Field | Type | Notes |
| --- | --- | --- |
| `payment` | object | Created durable payment record. |
| `gateway` | object | Provider-specific initiation result. |

For Alipay sandbox, `gateway.qrCode` is the value returned by
`alipay.trade.precreate`. The buyer page renders it as a scannable QR image.

Active `initiated` or `success` attempts block creation of another payment
attempt. Failed, cancelled, or timed-out attempts can be retried. Old
`initiated` attempts are marked `timed_out` after the configured timeout.

### `POST /api/payments/{paymentNo}/resume`

Regenerates the provider payment entry for an existing `initiated` attempt
without creating a second `payment_records` row. This supports browser refresh
and returning to the payment page after an Alipay QR target was lost.

The endpoint rejects non-buyers, non-active attempts, and orders that are no
longer in `pending_payment`.

### `POST /api/payments/{paymentNo}/mock-success`

Completes a local `mock` attempt. This endpoint is retained for local
development and deterministic automated tests. It rejects non-mock attempts.

On success, the payment record becomes `success`, the order payment status
becomes `paid`, and the existing fulfillment transition rules run unchanged.

### `POST /api/payments/callbacks/alipay-sandbox`

Consumes the form-encoded Alipay sandbox asynchronous notification and returns
plain text `success` after processing.

Before mutation the backend verifies:

- RSA2 signature using `ALIPAY_SANDBOX_PUBLIC_KEY`
- payment record existence and expected channel
- callback amount against the server-side payment record
- callback replay fingerprint

Successful callbacks complete the payment. Non-success callbacks update the
attempt status so the buyer can retry when appropriate. Replayed callbacks are
idempotent.

## Shared Types

Payment record statuses used by this flow:

- `initiated`
- `success`
- `failed`
- `cancelled`
- `timed_out`
- `refunded`

Payment methods:

- `mock`
- `alipay_sandbox`

## Configuration

See `docs/04-standards/payment-sandbox-configuration.md`. Sandbox credentials
must remain outside the repository. Missing sandbox variables must not break
mock mode or automated tests.
