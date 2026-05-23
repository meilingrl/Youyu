# API Spec: review

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/youyu/backend/controller/review/ReviewController.java`
  - controller: `backend/src/main/java/com/youyu/backend/controller/product/ProductController.java` (public review list and summary)
  - controller: `backend/src/main/java/com/youyu/backend/controller/shop/ShopController.java` (public shop review list and summary)
  - service: `backend/src/main/java/com/youyu/backend/service/review/impl/ReviewServiceImpl.java`
  - request sample: `docs/06-http/review.http`
- Last updated: 2026-05-20

## Scope

This document covers:

- product review submission and shop review submission (authenticated buyer)
- pending reviewable order items and my-reviews history (authenticated buyer)
- public product review lists and review summaries (hosted under `/api/products/`)
- public shop review lists and review summaries (hosted under `/api/shops/`)

It does not cover review-task management (admin-side product review tasks belong to the admin spec).

## Authentication And Roles

| Endpoint group | Auth |
|---|---|
| `POST /api/reviews/products`, `POST /api/reviews/shops` | USER role required |
| `GET /api/reviews/pending`, `GET /api/reviews/mine` | USER role required |
| `GET /api/products/{id}/reviews`, `GET /api/products/{id}/review-summary` | Public (no auth) |
| `GET /api/shops/{shopId}/reviews`, `GET /api/shops/{shopId}/review-summary` | Public (no auth) |

- Token format: `Authorization: Bearer <token>`
- Missing or invalid token on protected endpoints returns HTTP 401 (`UNAUTHORIZED`)
- Valid token with wrong role returns HTTP 403 (`FORBIDDEN`)

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

Failures use the same envelope with `success=false` and the appropriate `code` and HTTP status.

## Error Semantics

### HTTP Status And `ResultCode`

| HTTP status | `ResultCode` | Meaning |
|---|---|---|
| `400` | `BAD_REQUEST` | Invalid request parameters or validation failure |
| `401` | `UNAUTHORIZED` | Authentication required or invalid token |
| `403` | `FORBIDDEN` | Authenticated but wrong role |
| `404` | `NOT_FOUND` | Referenced resource does not exist |
| `500` | `INTERNAL_SERVER_ERROR` | Unhandled or server-side failure |
| `200` | `BUSINESS_ERROR` | Domain-rule rejection (duplicate review, ineligible order, etc.) |

`BUSINESS_ERROR` responses return HTTP 200 with `success=false` and `code="BUSINESS_ERROR"`. Callers must check `success` or `code`, not just HTTP status.

## Endpoints

### `POST /api/reviews/products`

#### Purpose

Submit a product review for a completed order item.

#### Request

- Header:
  - `Authorization: Bearer <token>`

##### Body

| Field | Required | Type | Notes |
|---|---|---|---|
| `orderItemId` | yes | integer | Must belong to a completed order owned by the caller |
| `score` | yes | integer | 1–5 |
| `content` | no | string | Max 1000 characters, defaults to empty string |

#### Response

- `data` shape:

| Field | Type | Notes |
|---|---|---|
| `id` | integer | Review ID |
| `orderItemId` | integer | |
| `buyerUserId` | integer | |
| `productId` | integer | |
| `score` | integer | 1–5 |
| `content` | string | |
| `createdAt` | string | Format `yyyy-MM-dd HH:mm` |
| `updatedAt` | string | Format `yyyy-MM-dd HH:mm` |

#### Error Cases

- `400`: missing `orderItemId` or `score`, score outside 1–5, content exceeds 1000 characters
- `401`: not logged in
- `403`: current role is not USER
- `404`: order item does not exist
- `BUSINESS_ERROR`: order not completed, order does not belong to caller, duplicate review

### `POST /api/reviews/shops`

#### Purpose

Submit a shop review. Caller must have at least one completed order from the target shop.

#### Request

- Header:
  - `Authorization: Bearer <token>`

##### Body

| Field | Required | Type | Notes |
|---|---|---|---|
| `shopId` | yes | integer | Target shop |
| `score` | yes | integer | 1–5 |
| `content` | no | string | Max 1000 characters, defaults to empty string |

#### Response

- `data` shape:

| Field | Type | Notes |
|---|---|---|
| `id` | integer | Review ID |
| `shopId` | integer | |
| `buyerUserId` | integer | |
| `score` | integer | 1–5 |
| `content` | string | |
| `createdAt` | string | Format `yyyy-MM-dd HH:mm` |
| `updatedAt` | string | Format `yyyy-MM-dd HH:mm` |

#### Error Cases

- `400`: missing `shopId` or `score`, score outside 1–5, content exceeds 1000 characters
- `401`: not logged in
- `403`: current role is not USER
- `404`: shop does not exist
- `BUSINESS_ERROR`: shop review status is not `approved`, no completed order from this shop, duplicate review

### `GET /api/reviews/pending`

#### Purpose

Return order items that are eligible for review but have not yet been reviewed by the current user.

#### Request

- Header:
  - `Authorization: Bearer <token>`

#### Response

- `data` shape:

| Field | Type | Notes |
|---|---|---|
| `items` | array | Pending order items |

Each item in `items`:

| Field | Type | Notes |
|---|---|---|
| `id` | integer | Order item ID |
| `orderId` | integer | |
| `productId` | integer | |
| `titleSnapshot` | string | Product title at order time |
| `imageSnapshot` | string | Product image at order time |
| `priceSnapshot` | number | Price at order time |
| `quantity` | integer | |
| `shopId` | integer | Shop that fulfilled this item |
| `completedAt` | string | Order completion timestamp, format `yyyy-MM-dd HH:mm` |

#### Error Cases

- `401`: not logged in

### `GET /api/reviews/mine`

#### Purpose

Return all reviews written by the current user, split into product reviews and shop reviews.

#### Request

- Header:
  - `Authorization: Bearer <token>`

#### Response

- `data` shape:

| Field | Type | Notes |
|---|---|---|
| `productReviews` | array | Product reviews by the current user |
| `shopReviews` | array | Shop reviews by the current user |

Product review items add `productTitle` and `productImage` (from order-item snapshot, nullable via LEFT JOIN).

Shop review items add `shopName` and `shopAvatar` (from shop record, nullable via LEFT JOIN).

#### Error Cases

- `401`: not logged in

### `GET /api/products/{id}/reviews`

#### Purpose

Public paginated list of reviews for a product, with reviewer nickname and avatar.

#### Request

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `id` | yes | integer | Product ID |

##### Query

| Field | Required | Type | Notes |
|---|---|---|---|
| `page` | no | integer | Default `1` |
| `pageSize` | no | integer | Default `10`, max `50` |

#### Response

- `data` shape:

| Field | Type | Notes |
|---|---|---|
| `items` | array | Review items, newest first |
| `total` | integer | Total review count for this product |
| `page` | integer | Current page |
| `pageSize` | integer | Current page size |

Each review item includes the base product review fields plus:

| Field | Type | Notes |
|---|---|---|
| `reviewerNickname` | string | Reviewer display name |
| `reviewerAvatar` | string | Reviewer avatar URL |

#### Error Cases

- `404`: product does not exist

### `GET /api/products/{id}/review-summary`

#### Purpose

Aggregated rating summary for a product.

#### Request

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `id` | yes | integer | Product ID |

#### Response

- `data` shape:

| Field | Type | Notes |
|---|---|---|
| `avgScore` | number | Rounded to 2 decimal places, 0.0 when no reviews |
| `reviewCount` | integer | Total number of reviews |
| `distribution` | array | Score distribution, 5 entries (score 1–5, each with `score` and `count`) |

The `distribution` array is currently a stub — all score levels return `count=0`. This will be replaced with real GROUP BY aggregation in a future iteration.

#### Error Cases

- `404`: product does not exist

### `GET /api/shops/{shopId}/reviews`

#### Purpose

Public paginated list of reviews for a shop, with reviewer nickname and avatar.

#### Request

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `shopId` | yes | integer | Shop ID |

##### Query

| Field | Required | Type | Notes |
|---|---|---|---|
| `page` | no | integer | Default `1` |
| `pageSize` | no | integer | Default `10`, max `50` |

#### Response

- `data` shape: same pagination envelope as product reviews (`items`, `total`, `page`, `pageSize`).

Each shop review item:

| Field | Type | Notes |
|---|---|---|
| `id` | integer | Review ID |
| `shopId` | integer | |
| `buyerUserId` | integer | |
| `score` | integer | 1–5 |
| `content` | string | |
| `createdAt` | string | Format `yyyy-MM-dd HH:mm` |
| `updatedAt` | string | Format `yyyy-MM-dd HH:mm` |
| `reviewerNickname` | string | Reviewer display name |
| `reviewerAvatar` | string | Reviewer avatar URL |

#### Error Cases

- `404`: shop does not exist

### `GET /api/shops/{shopId}/review-summary`

#### Purpose

Aggregated rating summary for a shop.

#### Request

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `shopId` | yes | integer | Shop ID |

#### Response

- `data` shape: same structure as product review summary (`avgScore`, `reviewCount`, `distribution`).

The `distribution` array is also a stub — same limitation as the product review summary.

#### Error Cases

- `404`: shop does not exist

## Shared Types / Enumerations

- **Score**: integer 1–5, validated server-side with `BusinessException(BAD_REQUEST)` for out-of-range values.
- **Content**: string, max 1000 characters. Empty string is the default.
- **Pagination**: `page` defaults to 1 (clamped to ≥1), `pageSize` defaults to 10 (clamped to 1–50).
- **Scoring side effects**: Submitting a review triggers a recalculation of the product or shop `rating_score` and `review_count` denormalized fields. These are written transactionally alongside the review insert.
- **Duplicate detection**: The `reviews` table has a unique constraint on `(order_item_id, buyer_user_id)`; the `shop_reviews` table has a unique constraint on `(shop_id, buyer_user_id)`. Duplicate submissions are caught via `DuplicateKeyException` and surfaced as `BUSINESS_ERROR`.

## HTTP Asset Mapping

- Primary validation file: `docs/06-http/review.http`
- Additional related files:
  - `docs/06-http/product.http` (covers public product review list and summary endpoints)
  - `docs/06-http/order.http` (covers order completion flow needed to produce reviewable items)

## Known Drift Or Follow-Up Notes

- `review.http` matches current controller behavior — no drift.
- The `distribution` array in both review summaries is a known stub (all score levels return `count=0`). Real GROUP BY aggregation is tracked as a TODO in `ReviewServiceImpl`.
- Four public review endpoints live on `ProductController` and `ShopController`, not `ReviewController`. Callers should not assume all review-related paths start with `/api/reviews/`.
