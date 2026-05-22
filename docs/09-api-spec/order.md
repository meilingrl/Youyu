# API Spec: order

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/youyu/backend/controller/order/OrderController.java`
  - controller: `backend/src/main/java/com/youyu/backend/controller/order/AdminOrderController.java`
  - request sample: `docs/06-http/order.http`
- Last updated: 2026-05-16

## Scope

This document covers:

- buyer order list, preview, create, detail, cancel, receipt confirmation, refund, and digital asset access
- admin order list, detail, shipping, offline confirmation, and refund completion

It does not cover cart endpoints or payment endpoints.

## Authentication

- Buyer endpoints under `/api/orders/**`: login required
- Admin endpoints under `/api/admin/orders/**`: admin role required

## Response Envelope

All endpoints in this module use the unified response envelope with `success`, `code`, `message`, `data`, and `traceId`.

## Buyer Endpoints

### `GET /api/orders`

#### Purpose

List orders belonging to the current buyer.

#### Request

- Header:
  - `Authorization: Bearer <token>`

#### Response

- `data`: array of order list items

#### Error Cases

- `401`: not logged in

### `POST /api/orders/preview`

#### Purpose

Preview an order before creation.

#### Request

- Header:
  - `Authorization: Bearer <token>`
- Body:
  - `cartItemIds`: required, array of selected cart item IDs
  - `fulfillmentType`: optional, can be used to force preview under a specific fulfillment mode

#### Response

- `data`: preview object returned by `OrderService.previewOrder(...)`
- Current service behavior includes selected fulfillment result, order items, and amount-related preview data

#### Error Cases

- `400`: cart item list missing or invalid
- business error: selected product cannot be purchased

### `POST /api/orders`

#### Purpose

Create an order from selected cart items.

#### Request

- Header:
  - `Authorization: Bearer <token>`
- Body common fields:
  - `cartItemIds`: required, array
  - `fulfillmentType`: required
- Additional requirements by fulfillment type:
  - `logistics`:
    - `addressId`: required
  - `offline`:
    - `offlineMeetTime`: required, format `yyyy-MM-dd HH:mm`
    - `offlineMeetLocation`: required

#### Response

- `data`: created order object

#### Error Cases

- `400`: missing required fields, invalid address, invalid offline meeting time format
- business error: cart item invalid, product unavailable, fulfillment type not allowed

### `GET /api/orders/{orderId}`

#### Purpose

Return order detail for the current buyer.

#### Request

- Path:
  - `orderId`: required

#### Response

- `data` currently includes:
  - order base information
  - `fulfillment`
  - `orderItems`
  - `payments`
  - `refunds`
  - `availableActions`
  - digital asset visibility data when applicable

#### Error Cases

- `403`: current user is not the buyer
- `404`: order does not exist

### `POST /api/orders/{orderId}/cancel`

#### Purpose

Cancel an order as the buyer.

#### Response

- `data`: updated order result

#### Error Cases

- `403`: current user is not the buyer
- business error: current order status does not allow cancellation

### `POST /api/orders/{orderId}/confirm-receipt`

#### Purpose

Confirm receipt for logistics or digital completion flow.

#### Response

- `data`: updated order result

#### Error Cases

- `403`: current user is not the buyer
- business error: current order state does not allow confirmation

### `POST /api/orders/{orderId}/offline/buyer-confirm`

#### Purpose

Buyer confirms offline delivery completion.

#### Response

- `data`: updated order result

#### Error Cases

- `403`: current user is not the buyer
- business error: order is not an offline order or current state does not allow confirmation

### `POST /api/orders/{orderId}/refunds`

#### Purpose

Apply for a refund.

#### Request

- Body:
  - `refundReason`: required

#### Response

- `data`: created refund or updated order result from service layer

#### Error Cases

- `403`: current user is not the buyer
- business error:
  - digital goods do not support refund
  - cancelled/refunded orders cannot re-apply
  - unpaid order cannot request refund
  - payment record missing

### `GET /api/orders/{orderId}/assets/{assetId}/access`

#### Purpose

Grant authorized access to a digital asset after order completion rules are satisfied.

#### Response

- `data` contains the authorized digital asset access result

#### Error Cases

- `403`: current user is not the buyer
- business error:
  - order not eligible for full access yet
  - asset does not belong to the order

## Admin Endpoints

### `GET /api/admin/orders`

#### Purpose

List all orders from the admin view.

#### Error Cases

- `401`: not logged in
- `403`: not an admin

### `GET /api/admin/orders/{orderId}`

#### Purpose

Return order detail from the admin view.

#### Response

- `data`: same detail family as buyer view, but without buyer ownership restriction

### `POST /api/admin/orders/{orderId}/ship`

#### Purpose

Mark a logistics order as shipped.

#### Request

- Body:
  - `trackingNo`: required
  - `logisticsCompany`: optional but recommended

#### Error Cases

- `400`: not a logistics order, missing tracking number
- business error: current order state does not allow shipping

### `POST /api/admin/orders/{orderId}/offline/seller-confirm`

#### Purpose

Seller/admin confirms offline handoff from the seller side.

#### Error Cases

- business error: order is not offline or state transition is invalid

### `POST /api/admin/orders/{orderId}/refunds/{refundId}/complete`

#### Purpose

Mark a refund as completed from the admin side.

#### Error Cases

- `404`: refund does not exist
- business error: refund or order state does not allow completion

## Shared Types / Enumerations

- `fulfillmentType`:
  - `logistics`
  - `offline`
  - `digital`
- Order detail exposes action-oriented state through `availableActions`
- Refund and receipt behavior depends on current `orderStatus` and `paymentStatus`

## Notes

- Order create and operational endpoints currently accept map-style payloads rather than dedicated DTOs
- If later the project adds stricter request objects or OpenAPI generation, this document should be tightened to exact field-by-field schemas
