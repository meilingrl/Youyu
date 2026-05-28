# API Spec: payment

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/youyu/backend/controller/payment/PaymentController.java`
  - service: `backend/src/main/java/com/youyu/backend/service/payment/impl/PaymentServiceImpl.java`
  - gateway service: `backend/src/main/java/com/youyu/backend/service/payment/impl/MockPaymentGatewayServiceImpl.java`
  - request sample: `docs/06-http/payment.http`
- Last updated: 2026-05-27

## Scope

This document covers the authenticated payment API:

- payment gateway info
- payment initiation for an order
- mock payment success callback endpoint

It does not cover real third-party payment-provider integration, order creation, refund requests, or admin refund completion.

## Authentication And Roles

All endpoints under `/api/payments` require `Authorization: Bearer <token>`.

`PaymentController` uses `@LoginRequired` with the default role set (`USER`, `ADMIN`). The payment contract is buyer-oriented. Payment initiation and mock completion both verify that the authenticated user owns the target order.

## Response Envelope

All endpoints in this module use the unified response envelope:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Request succeeded",
  "data": {},
  "traceId": "..."
}
```

Failures use the same envelope with `success=false` and the appropriate `code`.

## Error Semantics

| HTTP status | `ResultCode` | Meaning |
|---|---|---|
| `400` | `BAD_REQUEST` | Invalid path variable or request shape |
| `401` | `UNAUTHORIZED` | Missing or invalid token |
| `403` | `FORBIDDEN` | Current user does not own the order |
| `404` | `NOT_FOUND` | Order or payment record does not exist |
| `500` | `INTERNAL_SERVER_ERROR` | Unhandled server-side failure |
| `200` | `BUSINESS_ERROR` | Domain-rule rejection, such as duplicate payment or invalid state transition |

`BUSINESS_ERROR` responses return HTTP 200 with `success=false`. Callers must check `success` or `code`, not only HTTP status.

## Endpoints

### `GET /api/payments/gateway`

#### Purpose

Return the currently configured payment gateway information for the trade flow.

#### Request

- Header:
  - `Authorization: Bearer <token>`

#### Response

- `data` shape:

| Field | Type | Notes |
|---|---|---|
| `defaultGateway` | string | Current value is `MOCK` |
| `message` | string | Human-readable note that mock payment is wired and future gateway fields are reserved |

#### Error Cases

- `401`: not logged in

### `POST /api/payments/orders/{orderId}/initiate`

#### Purpose

Create a payment record for an order that is waiting for payment.

#### Request

- Header:
  - `Authorization: Bearer <token>`

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `orderId` | yes | integer | Must belong to the current user |

##### Body

No request body is currently accepted or required.

#### Response

- `data` shape:

| Field | Type | Notes |
|---|---|---|
| `payment` | object | Created payment record |
| `gateway` | object | Mock gateway payload for the order |

`payment` fields:

| Field | Type | Notes |
|---|---|---|
| `id` | integer | Payment record ID |
| `orderId` | integer | Target order ID |
| `paymentNo` | string | Generated payment number, format starts with `PAY` |
| `paymentMethod` | string | Current value is `mock_gateway` |
| `paymentChannel` | string | Current value is `internal_mock` |
| `paymentStatus` | string | Current initial value is `initiated` |
| `paymentAmount` | number | Order payable amount |
| `initiatedAt` | string | Format `yyyy-MM-dd HH:mm` |
| `paidAt` | string/null | Null before mock success |
| `failedReason` | string | Empty string on creation |
| `callbackSummary` | string | Reserved callback summary text |

`gateway` fields:

| Field | Type | Notes |
|---|---|---|
| `gateway` | string | Current value is `MOCK` |
| `orderNo` | string | Order number |
| `status` | string | Current value is `PENDING` |
| `message` | string | Mock gateway placeholder message |

#### Error Cases

- `401`: not logged in
- `403`: current user is not the order buyer
- `404`: order does not exist
- `BUSINESS_ERROR`: order is not in `pending_payment`, or the order already has an active non-failed payment record

### `POST /api/payments/{paymentNo}/mock-success`

#### Purpose

Complete a mock payment and advance the target order according to fulfillment type.

#### Request

- Header:
  - `Authorization: Bearer <token>`

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `paymentNo` | yes | string | Payment number returned by payment initiation |

##### Body

No request body is currently accepted or required.

#### Response

- `data` shape:

| Field | Type | Notes |
|---|---|---|
| `orderId` | integer | Target order ID |
| `orderNo` | string | Target order number |
| `paymentNo` | string | Completed payment number |
| `orderStatus` | string | Updated order status |
| `paymentStatus` | string | Updated order-level payment status, currently `paid` |
| `fulfillmentType` | string | `logistics`, `offline`, or `digital` |
| `nextAction` | string | Caller-facing next action hint |

Mock success side effects:

- payment record `paymentStatus` becomes `success`
- payment record `paidAt` is set
- order `paymentStatus` becomes `paid`
- order `paidAt` is set
- digital orders move to `pending_receipt`
- logistics and offline orders move to `pending_fulfillment`

#### Error Cases

- `401`: not logged in
- `403`: current user is not the order buyer
- `404`: payment record or order does not exist
- `BUSINESS_ERROR`: payment is already successful or the order state cannot transition to the next mock-payment state

## Shared Types / Enumerations

- `paymentStatus` on payment records: current flow creates `initiated`, then mock success changes it to `success`. Failed-payment retry behavior is reserved in service logic but no failure endpoint currently exists.
- Order-level `paymentStatus`: `unpaid`, `paid`, `refunding`, `refunded`.
- `nextAction` values currently returned:
  - `buyer_confirm_receipt_to_unlock_full_download`
  - `seller_fill_tracking_info`
  - `buyer_confirm_offline_delivery`
  - `wait_for_offline_appointment_and_double_confirmation`
  - `none`

## Mock Payment Semantics

The current implementation is an internal mock gateway, not a real payment-provider integration.

- `/api/payments/gateway` reports `defaultGateway=MOCK`.
- `/api/payments/orders/{orderId}/initiate` creates a local `payment_records` row and returns a mock gateway payload.
- `/api/payments/{paymentNo}/mock-success` is an authenticated local endpoint that simulates the payment-provider success callback and mutates both payment and order state.
- The mock success endpoint still enforces buyer ownership.

## HTTP Asset Mapping

- Primary validation file: `docs/06-http/payment.http`
- Related files:
  - `docs/06-http/order.http` covers order creation and post-payment order operations

## Known Drift Or Follow-Up Notes

- No known payment endpoint drift after the 2026-05-27 smoke-file split.
- There is no real gateway callback signature, failure callback, payment-method selection, or refund-provider integration yet.
