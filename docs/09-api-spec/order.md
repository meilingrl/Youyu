# API Spec: order

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/youyu/backend/controller/order/OrderController.java`
  - controller: `backend/src/main/java/com/youyu/backend/controller/order/AdminOrderController.java`
  - request sample: `docs/06-http/order.http`
- Last updated: 2026-06-01

## Scope

This document covers:

- buyer order list, preview, create, detail, cancel, receipt confirmation, refund, and digital asset access
- admin order list, detail, shipping, offline confirmation, and refund completion

It does not cover cart endpoints or payment endpoints.

## Authentication

- Buyer endpoints under `/api/orders/**`: login required
- Admin endpoints under `/api/admin/orders/**`: admin staff role plus backend order permission required.
  - `ADMIN` / `SUPER_ADMIN`: all order admin actions.
  - `ORDER_ADMIN`: order read and order manage actions.
  - `SUPPORT_AGENT`: order read context only.

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
  - `userCouponId`: optional, a claimed user coupon ID to apply to the preview

#### Response

- `data`: preview object returned by `OrderService.previewOrder(...)`
- Current service behavior includes selected fulfillment result, order items, amount-related preview data, applicable coupons, and the selected coupon when `userCouponId` is valid.
- Coupon-related response fields:
  - `availableCoupons`: claimed coupons that match the order shop and amount
  - `appliedCoupon`: selected coupon snapshot, or `null`
  - `couponDiscountAmount`: discount contributed by the selected coupon
  - `discountAmount`: total discount amount for the order preview

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
  - `userCouponId`: optional, a claimed user coupon ID; the server revalidates it during order creation
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
- business error: cart item invalid, product unavailable, fulfillment type not allowed, coupon already used, coupon inactive, coupon not applicable to the order shop, or coupon threshold not met

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
  - `items`
  - `payments`
  - `refunds`
  - `appliedCoupon`
  - `availableActions`
  - digital asset visibility data when applicable
  - `refundSupported`
  - `refundRuleText`
  - `relatedReports`
  - `mediationSummary`
  - `afterSalesSummary`

#### After-sales visibility fields

`GET /api/orders/{orderId}` and `GET /api/admin/orders/{orderId}` now return a shared after-sales visibility family:

| Field | Type | Notes |
|---|---|---|
| `refundSupported` | boolean | `false` for digital orders that do not support refund. |
| `refundRuleText` | string | Honest current-phase guidance text; it must not imply a completed payment-gateway refund upgrade. |
| `relatedReports` | array | Order-linked reports visible in the current role context. Buyer view returns only reports submitted by the current buyer. Admin view returns all order-linked reports. |
| `mediationSummary` | object or null | Latest report-backed mediation case summary for the order when one exists. |
| `afterSalesSummary` | object | Aggregated visibility object for refund/report/mediation progress. |

`relatedReports[]` items currently expose report list fields such as `id`, `reporterUserId`, `reporterName`, `targetType`, `targetId`, `reasonType`, `content`, `status`, `submittedAt`, `processedAt`, `processedBy`, and `resolution`.

`mediationSummary` currently exposes:

| Field | Type | Notes |
|---|---|---|
| `id` | number | Mediation case ID. |
| `caseNo` | string | Human-readable mediation case number. |
| `status` | string | Current mediation case status. |
| `decisionCategory` | string or null | Present once a decision category exists. |
| `sourceReportId` | number | Report ID used for the report-backed handoff. |
| `updatedAt` | string | Latest mediation case update time. |
| `reportCount` | number | Count of currently visible related reports in the response context. |

`afterSalesSummary` currently exposes:

| Field | Type | Notes |
|---|---|---|
| `hasRefunds` | boolean | Whether the order currently has refund records. |
| `hasReports` | boolean | Whether the current response context includes linked reports. |
| `hasMediation` | boolean | Whether a mediation case exists for the order. |
| `currentStage` | string | Current aggregate after-sales stage, such as `normal`, `report_submitted`, `refund_in_progress`, `refund_completed`, or `mediation_in_progress`. |
| `userGuidance` | string | Current-phase guidance text for the viewer. |

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
- `403`: current role lacks order read permission

### `GET /api/admin/orders/{orderId}`

#### Purpose

Return order detail from the admin view.

#### Response

- `data`: same detail family as buyer view, but without buyer ownership restriction
- Admin detail is the required handling continuation for the buyer-visible after-sales fields above:
  - it includes all order-linked reports
  - it includes the same mediation summary family
  - it is expected to link operators onward into support-ticket, report, and mediation handling pages

### `POST /api/admin/orders/{orderId}/ship`

#### Purpose

Mark a logistics order as shipped.

#### Request

- Body:
  - `trackingNo`: required
  - `logisticsCompany`: optional but recommended

#### Error Cases

- `400`: not a logistics order, missing tracking number
- `403`: current role lacks order manage permission
- business error: current order state does not allow shipping

### `POST /api/admin/orders/{orderId}/offline/seller-confirm`

#### Purpose

Seller/admin confirms offline handoff from the seller side.

#### Error Cases

- `403`: current role lacks order manage permission
- business error: order is not offline or state transition is invalid

### `POST /api/admin/orders/{orderId}/refunds/{refundId}/complete`

#### Purpose

Mark a refund as completed from the admin side.

#### Error Cases

- `404`: refund does not exist
- `403`: current role lacks order manage permission
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
