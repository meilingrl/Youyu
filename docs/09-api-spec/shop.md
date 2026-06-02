# API Spec: shop

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/youyu/backend/controller/shop/ShopController.java`
  - service: `backend/src/main/java/com/youyu/backend/service/shop/impl/ShopServiceImpl.java`
  - mapper: `backend/src/main/java/com/youyu/backend/mapper/shop/impl/JdbcShopMapper.java`
  - shared response / error handling: `backend/src/main/java/com/youyu/backend/common/api/ApiResponse.java`
  - shared response / error handling: `backend/src/main/java/com/youyu/backend/controller/advice/GlobalExceptionHandler.java`
  - request sample: `docs/06-http/shop.http`
  - related task: `docs/08-tasks/archived/api-spec-standardization-follow-up.md`
- Last updated: 2026-05-23

## Scope

This document covers the endpoints currently exposed by `ShopController` under `/api/shops`:

- module skeleton
- current user's shop
- shop application submission
- public shop detail
- public shop insight snapshot
- public shop review list and summary

The two review endpoints are also covered from the review-domain perspective in `review.md`; they are repeated here because they are hosted by `ShopController`.

This document does not cover admin shop governance under `/api/admin/shops`, which belongs to `admin.md`.

## Authentication And Roles

- Public:
  - `GET /api/shops/skeleton`
  - `GET /api/shops/{shopId}`
  - `GET /api/shops/{shopId}/insight-snapshot`
  - `GET /api/shops/{shopId}/reviews`
  - `GET /api/shops/{shopId}/review-summary`
- Logged-in user only:
  - `GET /api/shops/mine`
  - `POST /api/shops/applications`

Authenticated endpoints use `@LoginRequired(roles = {UserRole.USER})`.

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

## Error Semantics

### HTTP Status And `ResultCode`

| HTTP status | `ResultCode` | Meaning |
|---|---|---|
| `400` | `BAD_REQUEST` | Invalid request parameters, duplicate shop name, or user already has a shop/application |
| `401` | `UNAUTHORIZED` | Missing token, invalid token, or not logged in |
| `403` | `FORBIDDEN` | User is not eligible to apply for a shop |
| `404` | `NOT_FOUND` | Shop does not exist or is not public-visible |
| `500` | `INTERNAL_SERVER_ERROR` | Unhandled or server-side failure |

### `BUSINESS_ERROR`

- Current shop-service validations mostly throw `BAD_REQUEST`, `FORBIDDEN`, or `NOT_FOUND`.
- Callers should still handle `success=false` with `code="BUSINESS_ERROR"` because review endpoints and shared lower layers may use that envelope.

## Endpoints

### `GET /api/shops/skeleton`

#### Purpose

Return module availability metadata for the shop domain.

#### Request

No query parameters, path parameters, or body.

#### Response

- `data` shape:

| Field | Type | Notes |
|---|---|---|
| `module` | string | Current value: `shop` |
| `status` | string | Current value: `persistent` |
| `next` | string | Short implementation note |

#### Error Cases

- `500`: unexpected server failure

#### HTTP Asset Mapping

- `docs/06-http/shop.http` lines 11-12

### `GET /api/shops/mine`

#### Purpose

Return the current user's shop or pending shop application.

#### Request

##### Headers

| Field | Required | Type | Notes |
|---|---|---|---|
| `Authorization` | yes | string | `Bearer <token>` for a user account |

#### Response

- `data` shape:

| Field | Type | Notes |
|---|---|---|
| `shop` | object | Current user's shop/application, or `{}` when absent |
| `capability` | object | Capability profile, or `{}` when absent |
| `products` | array | Products owned by the shop owner; not filtered to public-only for this endpoint |

Shop object fields include:

| Field | Type | Notes |
|---|---|---|
| `id` | number | Shop ID |
| `ownerUserId` / `ownerId` | number | Owner user ID under compatibility keys |
| `ownerName` | string/null | Owner nickname |
| `name` | string | Shop name |
| `description` | string/null | Shop description |
| `avatarUrl` | string/null | Shop avatar |
| `coverUrl` / `cover` | string/null | Cover image under compatibility keys |
| `announcement` / `notice` | string/null | Shop announcement under compatibility keys |
| `status` | string | Shop availability state |
| `reviewStatus` | string | Admin review state |
| `reviewedAt` | string/null | Review timestamp |
| `reviewedBy` | number/null | Admin reviewer ID |
| `rejectReason` | string/null | Rejection reason |
| `ratingScore` / `score` | number | Rating under compatibility keys |
| `followerCount` | number | Follower count |
| `createdAt` | string | Creation timestamp |
| `updatedAt` | string | Last update timestamp |

#### Error Cases

- `401`: missing token or invalid token
- `403`: role is not allowed

#### HTTP Asset Mapping

- `docs/06-http/shop.http` lines 45-47
- `docs/06-http/shop.http` lines 66-67 cover missing-auth behavior

### `POST /api/shops/applications`

#### Purpose

Submit a shop application for the current user.

The service creates a shop row with `status=inactive` and `reviewStatus=pending_review`, and creates a basic capability profile.

#### Request

##### Headers

| Field | Required | Type | Notes |
|---|---|---|---|
| `Authorization` | yes | string | `Bearer <token>` for a user account |
| `Content-Type` | yes | string | `application/json` |

##### Body

| Field | Required | Type | Notes |
|---|---|---|---|
| `name` | yes | string | Shop name |
| `description` | no | string | Defaults to empty string |
| `coverUrl` / `cover` | no | string | Cover image URL under accepted aliases |
| `announcement` / `notice` | no | string | Announcement text under accepted aliases |

#### Response

- `data` shape matches `GET /api/shops/mine` and contains the created shop/application, capability, and products.

#### Error Cases

- `400`: missing `name`, current user already has a shop/application, or shop name is already used
- `401`: missing token or invalid token
- `403`: current user cannot apply for a shop or is restricted
- `500`: application insert succeeded but follow-up read failed

#### HTTP Asset Mapping

- `docs/06-http/shop.http` lines 51-62

### `GET /api/shops/{shopId}`

#### Purpose

Return public shop detail for an active and approved shop.

#### Request

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `shopId` | yes | number | Target shop ID |

#### Response

- `data` shape:

| Field | Type | Notes |
|---|---|---|
| `shop` | object | Public shop object |
| `capability` | object | Capability profile |
| `products` | array | Public-visible shop products only: `status=on_sale` and `reviewStatus=approved` or `not_required` |

Product items use the product mapper shape documented in `product.md`, including compatibility keys such as `salePrice` / `price`, `coverUrl` / `cover`, and `productType` / `type`.

#### Error Cases

- `400`: non-numeric `shopId` binding failure
- `404`: shop does not exist, is deleted, inactive, or not approved
- `500`: unexpected server failure

#### HTTP Asset Mapping

- `docs/06-http/shop.http` lines 16-17
- `docs/06-http/shop.http` lines 21-22 cover non-existent shop behavior

### `GET /api/shops/{shopId}/insight-snapshot`

#### Purpose

Return a public insight snapshot for a shop.

#### Request

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `shopId` | yes | number | Target shop ID |

#### Response

- `data` shape:

| Field | Type | Notes |
|---|---|---|
| `shopId` | number | Target shop ID |
| `monthlySalesAmount` | number | Sum of completed paid order amount in the current month |
| `monthlyOrderCount` | number | Completed paid order count in the current month |
| `hotProducts` | array | Top products ranked by sold count, favorite count, view count, and ID |
| `viewCountSummary` | number | Sum of product view counts for the shop |
| `favoriteCountSummary` | number | Sum of product favorite counts for the shop |
| `repeatBuyerCount` | number | Buyers with more than one completed paid order in the shop |
| `lastCalculatedAt` | string | Local timestamp formatted as `yyyy-MM-dd HH:mm` |
| `metricSource` | string | Current value: `real_query` |

`hotProducts` items include:

| Field | Type | Notes |
|---|---|---|
| `productId` | number | Product ID |
| `title` | string | Product title |
| `soldCount` | number | Completed paid sold quantity in the current month |
| `salesAmount` | number | Sum of item subtotals for completed paid orders in the current month |
| `favoriteCount` | number | Product favorite count |
| `viewCount` | number | Product view count |

#### Error Cases

- `400`: non-numeric `shopId` binding failure
- `500`: unexpected server failure

#### HTTP Asset Mapping

- `docs/06-http/shop.http` lines 26-27

### `GET /api/shops/{shopId}/reviews`

#### Purpose

Return public paginated reviews for a shop.

Detailed review semantics are documented in `review.md`; this section records the route because it is implemented on `ShopController`.

#### Request

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `shopId` | yes | number | Target shop ID |

##### Query

| Field | Required | Type | Notes |
|---|---|---|---|
| `page` | no | integer | Default `1` |
| `pageSize` | no | integer | Default `10`; service clamps effective maximum to `50` |

#### Response

- `data` shape:

| Field | Type | Notes |
|---|---|---|
| `items` | array | Current page of shop reviews |
| `total` | number | Total shop reviews |
| `page` | number | Effective page |
| `pageSize` | number | Effective page size |

#### Error Cases

- `400`: non-numeric `shopId`, `page`, or `pageSize` binding failure
- `500`: unexpected server failure

#### HTTP Asset Mapping

- `docs/06-http/shop.http` lines 31-32
- Also covered in `docs/06-http/review.http`

### `GET /api/shops/{shopId}/review-summary`

#### Purpose

Return aggregated shop rating summary.

Detailed review semantics are documented in `review.md`; this section records the route because it is implemented on `ShopController`.

#### Request

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `shopId` | yes | number | Target shop ID |

#### Response

- `data` shape:

| Field | Type | Notes |
|---|---|---|
| `avgScore` | number | Average score rounded to two decimals |
| `reviewCount` | number | Total review count |
| `distribution` | array | Current implementation returns score buckets 1..5 with placeholder counts |

#### Error Cases

- `400`: non-numeric `shopId` binding failure
- `500`: unexpected server failure

#### HTTP Asset Mapping

- `docs/06-http/shop.http` lines 36-37
- Also covered in `docs/06-http/review.http`

## Shared Types / Enumerations

| Field | Known values / notes |
|---|---|
| `status` | Shop availability state. Current service creates applications as `inactive`; public detail requires `active`. |
| `reviewStatus` | Admin review state. Public detail requires `approved`; applications start as `pending_review`. |
| `capabilityLevel` | Current application path creates `basic`. |
| `canSetNotice` | From `shop_capability_profiles.can_config_announcement`. |
| `canSetLoyaltyDiscount` | From `shop_capability_profiles.can_config_loyalty_offer`. |
| `canUseCoupon` | From `shop_capability_profiles.can_issue_light_coupon`. |
| `canJoinActivity` | From `shop_capability_profiles.can_join_platform_activity`. |

## HTTP Asset Mapping

- Primary validation file: `docs/06-http/shop.http`
- Additional related files:
  - `docs/06-http/product.http` contains legacy public shop examples.
  - `docs/06-http/review.http` also covers the public shop-review endpoints.

## Known Drift Or Follow-Up Notes

- `docs/06-http/product.http` still contains a legacy `GET /api/shops/{shopId}/products` example. Current runtime does not expose that route; shop products are returned inside `GET /api/shops/{shopId}`. This task leaves `product.http` unchanged because the allowed write scope is limited to the new shop collection.
- Shop payloads are currently map-based rather than strict DTO-based. If dedicated DTOs are introduced later, this spec should be updated to use those DTOs as the contract source.
