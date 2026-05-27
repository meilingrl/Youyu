# API Spec: cart

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/youyu/backend/controller/order/CartController.java`
  - service: `backend/src/main/java/com/youyu/backend/service/order/impl/OrderServiceImpl.java`
  - request sample: `docs/06-http/cart.http`
- Last updated: 2026-05-27

## Scope

This document covers the authenticated shopping-cart API:

- cart list and summary
- add item
- update item quantity or selection state
- remove item

It does not cover order preview, order creation, payment, fulfillment, refund, or digital asset access.

## Authentication And Roles

All endpoints under `/api/cart` require `Authorization: Bearer <token>`.

`CartController` uses `@LoginRequired` with the default role set (`USER`, `ADMIN`). The cart contract is buyer-oriented; callers should use a normal user token for trade flows.

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
| `400` | `BAD_REQUEST` | Missing or invalid request fields |
| `401` | `UNAUTHORIZED` | Missing or invalid token |
| `403` | `FORBIDDEN` | Authenticated role is not allowed |
| `404` | `NOT_FOUND` | Product or cart item does not exist |
| `500` | `INTERNAL_SERVER_ERROR` | Unhandled server-side failure |
| `200` | `BUSINESS_ERROR` | Domain-rule rejection, such as product not purchasable |

`BUSINESS_ERROR` responses return HTTP 200 with `success=false`. Callers must check `success` or `code`, not only HTTP status.

## Endpoints

### `GET /api/cart`

#### Purpose

Return the current user's cart items and selected-item summary.

#### Request

- Header:
  - `Authorization: Bearer <token>`

#### Response

- `data` shape:

| Field | Type | Notes |
|---|---|---|
| `items` | array | Cart items ordered by creation time and ID |
| `summary` | object | Aggregate cart summary |

Each `items[]` entry:

| Field | Type | Notes |
|---|---|---|
| `id` | integer | Cart item ID |
| `productId` | integer | Product ID |
| `title` | string | Current product title |
| `coverUrl` | string | Current product cover URL |
| `productType` | string | Current product type |
| `quantity` | integer | Cart quantity |
| `unitPrice` | number | Current sale price |
| `subtotal` | number | `unitPrice * quantity` |
| `selected` | boolean | Whether this item participates in checkout selection |
| `allowedFulfillmentTypes` | array | Current supported fulfillment modes for the product |

`summary` fields:

| Field | Type | Notes |
|---|---|---|
| `itemCount` | integer | Number of cart rows |
| `selectedAmount` | number | Sum of `subtotal` for `selected=true` rows only |
| `selectedCount` | integer | Count of selected rows |

#### Error Cases

- `401`: not logged in
- `404`: a referenced product no longer exists while rendering the cart

### `POST /api/cart/items`

#### Purpose

Add a product to the cart. If the same product is already in the current user's cart, the existing row is updated instead of creating a duplicate.

#### Request

- Header:
  - `Authorization: Bearer <token>`

##### Body

| Field | Required | Type | Notes |
|---|---|---|---|
| `productId` | yes | integer | Product to add |
| `quantity` | yes | integer | Must be greater than 0 |

#### Response

- `data`: same shape as `GET /api/cart`
- Upsert behavior sets the cart row's `selected` state to `true`.

#### Error Cases

- `400`: missing `productId`, missing `quantity`, non-numeric value, or `quantity <= 0`
- `401`: not logged in
- `404`: product does not exist
- `BUSINESS_ERROR`: product is not currently purchasable

### `PATCH /api/cart/items/{cartItemId}`

#### Purpose

Update an existing cart item's quantity and/or selected state.

#### Request

- Header:
  - `Authorization: Bearer <token>`

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `cartItemId` | yes | integer | Cart item owned by the current user |

##### Body

| Field | Required | Type | Notes |
|---|---|---|---|
| `quantity` | no | integer | When supplied, must be greater than 0 |
| `selected` | no | boolean | Parsed through Java `Boolean.valueOf(String)` |

At least one of `quantity` or `selected` should be supplied by callers. Current service behavior accepts an empty body as a no-op if the cart item exists.

#### Response

- `data`: same shape as `GET /api/cart`

#### Error Cases

- `400`: non-numeric `quantity` or `quantity <= 0`
- `401`: not logged in
- `404`: cart item does not exist or does not belong to the current user

### `DELETE /api/cart/items/{cartItemId}`

#### Purpose

Remove a cart item owned by the current user.

#### Request

- Header:
  - `Authorization: Bearer <token>`

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `cartItemId` | yes | integer | Cart item owned by the current user |

#### Response

- `data` shape:

| Field | Type | Notes |
|---|---|---|
| `removed` | boolean | `true` when deletion succeeds |

#### Error Cases

- `401`: not logged in
- `404`: cart item does not exist or does not belong to the current user

## Shared Types / Enumerations

- `productType`: current product type value from product data.
- `allowedFulfillmentTypes`: array containing zero or more of `logistics`, `offline`, `digital`.
- `selected`: selected rows are the rows callers normally pass to `POST /api/orders/preview` or `POST /api/orders`.

## HTTP Asset Mapping

- Primary validation file: `docs/06-http/cart.http`
- Related files:
  - `docs/06-http/order.http` covers checkout from selected cart items

## Known Drift Or Follow-Up Notes

- No known cart endpoint drift after the 2026-05-27 smoke-file split.
- Cart rendering uses current product data. A stale cart row referencing a deleted product can fail with `NOT_FOUND`.
