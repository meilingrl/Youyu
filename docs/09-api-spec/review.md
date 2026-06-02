# API Spec: review

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/youyu/backend/controller/review/ReviewController.java`
  - controller: `backend/src/main/java/com/youyu/backend/controller/product/ProductController.java`
  - controller: `backend/src/main/java/com/youyu/backend/controller/shop/ShopController.java`
  - service: `backend/src/main/java/com/youyu/backend/service/review/impl/ReviewServiceImpl.java`
  - request sample: `docs/06-http/review.http`
- Last updated: 2026-06-01

## Scope

This document covers:

- product review submission and shop review submission
- pending reviewable order items and my review history
- public product review lists and review summaries
- public shop review lists and review summaries

It does not cover admin review-task management.

## Authentication And Roles

| Endpoint group | Auth |
|---|---|
| `POST /api/reviews/products`, `POST /api/reviews/shops` | USER role required |
| `GET /api/reviews/pending`, `GET /api/reviews/mine` | USER role required |
| `GET /api/products/{id}/reviews`, `GET /api/products/{id}/review-summary` | Public |
| `GET /api/shops/{shopId}/reviews`, `GET /api/shops/{shopId}/review-summary` | Public |

- Token format: `Authorization: Bearer <token>`
- Missing or invalid token on protected endpoints returns HTTP `401` with `UNAUTHORIZED`
- Valid token with wrong role returns HTTP `403` with `FORBIDDEN`

## Response Envelope

All endpoints use the unified response envelope:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Request succeeded",
  "data": {},
  "traceId": "..."
}
```

Failures use the same envelope with `success=false`.

## Error Semantics

| HTTP status | `ResultCode` | Meaning |
|---|---|---|
| `400` | `BAD_REQUEST` | Invalid request parameters or validation failure |
| `401` | `UNAUTHORIZED` | Authentication required or invalid token |
| `403` | `FORBIDDEN` | Authenticated but wrong role |
| `404` | `NOT_FOUND` | Referenced resource does not exist |
| `500` | `INTERNAL_SERVER_ERROR` | Unhandled or server-side failure |
| `200` | `BUSINESS_ERROR` | Domain-rule rejection |

`BUSINESS_ERROR` responses return HTTP `200` with `success=false`.

## Endpoints

### `POST /api/reviews/products`

Submit a product review for a completed order item.

Request body:

| Field | Required | Type | Notes |
|---|---|---|---|
| `orderItemId` | yes | integer | Must belong to a completed order owned by the caller |
| `score` | yes | integer | 1-5 |
| `content` | no | string | Max 1000 characters, defaults to empty string |

Response `data`:

| Field | Type |
|---|---|
| `id` | integer |
| `orderItemId` | integer |
| `buyerUserId` | integer |
| `productId` | integer |
| `score` | integer |
| `content` | string |
| `createdAt` | string |
| `updatedAt` | string |

Error cases:

- `400`: missing fields, invalid score, content too long
- `401`: not logged in
- `403`: wrong role
- `404`: order item does not exist
- `BUSINESS_ERROR`: order not completed, wrong buyer, duplicate review

### `POST /api/reviews/shops`

Submit a shop review. Caller must have at least one completed order from the target shop.

Request body:

| Field | Required | Type | Notes |
|---|---|---|---|
| `shopId` | yes | integer | Target shop |
| `score` | yes | integer | 1-5 |
| `content` | no | string | Max 1000 characters, defaults to empty string |

Response `data`:

| Field | Type |
|---|---|
| `id` | integer |
| `shopId` | integer |
| `buyerUserId` | integer |
| `score` | integer |
| `content` | string |
| `createdAt` | string |
| `updatedAt` | string |

Error cases:

- `400`: missing fields, invalid score, content too long
- `401`: not logged in
- `403`: wrong role
- `404`: shop does not exist
- `BUSINESS_ERROR`: shop not reviewable, no completed order, duplicate review

### `GET /api/reviews/pending`

Return order items eligible for review but not yet reviewed by the current user.

Response `data`:

| Field | Type | Notes |
|---|---|---|
| `items` | array | Pending order items |

Each item includes:

| Field | Type |
|---|---|
| `id` | integer |
| `orderId` | integer |
| `productId` | integer |
| `titleSnapshot` | string |
| `imageSnapshot` | string |
| `priceSnapshot` | number |
| `quantity` | integer |
| `shopId` | integer |
| `completedAt` | string |

### `GET /api/reviews/mine`

Return all reviews written by the current user.

Response `data`:

| Field | Type |
|---|---|
| `productReviews` | array |
| `shopReviews` | array |

### `GET /api/products/{id}/reviews`

Public paginated product review list.

Query params:

| Field | Required | Type | Notes |
|---|---|---|---|
| `page` | no | integer | Default `1` |
| `pageSize` | no | integer | Default `10`, max `50` |

Response `data`:

| Field | Type |
|---|---|
| `items` | array |
| `total` | integer |
| `page` | integer |
| `pageSize` | integer |

Each review item includes reviewer display fields:

| Field | Type |
|---|---|
| `reviewerNickname` | string |
| `reviewerAvatar` | string |

### `GET /api/products/{id}/review-summary`

Aggregated rating summary for a product.

Response `data`:

| Field | Type | Notes |
|---|---|---|
| `avgScore` | number | Rounded to 2 decimal places, `0.0` when no reviews |
| `reviewCount` | integer | Total number of reviews |
| `distribution` | array | Always 5 entries for scores 1-5 |

`distribution` always returns all five score buckets. Real review scores carry aggregated counts and missing score levels return `count=0`.

Each `distribution` item:

| Field | Type |
|---|---|
| `score` | integer |
| `count` | integer |

### `GET /api/shops/{shopId}/reviews`

Public paginated shop review list.

Query params:

| Field | Required | Type | Notes |
|---|---|---|---|
| `page` | no | integer | Default `1` |
| `pageSize` | no | integer | Default `10`, max `50` |

Response `data` uses the same pagination envelope as product reviews.

Each review item includes:

| Field | Type |
|---|---|
| `id` | integer |
| `shopId` | integer |
| `buyerUserId` | integer |
| `score` | integer |
| `content` | string |
| `createdAt` | string |
| `updatedAt` | string |
| `reviewerNickname` | string |
| `reviewerAvatar` | string |

### `GET /api/shops/{shopId}/review-summary`

Aggregated rating summary for a shop.

Response `data` has the same structure as product review summary: `avgScore`, `reviewCount`, and `distribution`.

`distribution` follows the same rules as product review summary: all five score buckets are always present and use real aggregated counts.

## Shared Types / Enumerations

- **Score**: integer 1-5
- **Content**: max 1000 characters
- **Pagination**: `page` clamps to at least `1`; `pageSize` clamps to `1-50`
- **Scoring side effects**: review submission recalculates the denormalized `rating_score` and `review_count` fields on the owning product or shop
- **Duplicate detection**:
  - product reviews: unique `(order_item_id, buyer_user_id)`
  - shop reviews: unique `(shop_id, buyer_user_id)`

## HTTP Asset Mapping

- Primary validation file: `docs/06-http/review.http`
- Additional related files:
  - `docs/06-http/product.http`
  - `docs/06-http/order.http`

## Known Drift Or Follow-Up Notes

- `review.http` matches current controller behavior.
- Four public review endpoints live on `ProductController` and `ShopController`, not `ReviewController`.
