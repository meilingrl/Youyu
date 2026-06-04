# API Spec: recommend

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/youyu/backend/controller/product/RecommendController.java`
  - service: `backend/src/main/java/com/youyu/backend/service/product/impl/RecommendServiceImpl.java`
  - mapper: `backend/src/main/java/com/youyu/backend/mapper/recommend/impl/JdbcRecommendMapper.java`
  - shared response / error handling: `backend/src/main/java/com/youyu/backend/common/api/ApiResponse.java`
  - shared response / error handling: `backend/src/main/java/com/youyu/backend/controller/advice/GlobalExceptionHandler.java`
  - request sample: `docs/06-http/recommend.http`
  - related task: `docs/08-tasks/archived/api-spec-standardization-follow-up.md`
- Last updated: 2026-06-04
- Runtime note: as of 2026-06-04, these endpoints may use optional Redis
  caching when `YOUYU_REDIS_CACHE_ENABLED=true`; this does not change request or
  response contracts.

## Scope

This document covers public recommendation endpoints under `/api/recommend`:

- home recommendations using a hybrid cold-start / personalized strategy
- also-bought recommendations for a product detail page

It does not cover product list filtering, product detail, search suggestions, or shop detail. Those are documented in `product.md`, `search.md`, and `shop.md`.

## Authentication And Roles

- `GET /api/recommend/home` is public and also accepts an optional Bearer token.
- When a valid user token is present, the backend reads the current user from `AuthContextHolder` and may personalize the home list from purchase-category history.
- When no token is present, or when the current user has no purchase-category history, the endpoint falls back to popularity.
- `GET /api/recommend/also-bought/{productId}` is public.

## Response Envelope

All endpoints in this module use the unified response envelope:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Request succeeded",
  "data": [],
  "traceId": "..."
}
```

## Error Semantics

### HTTP Status And `ResultCode`

| HTTP status | `ResultCode` | Meaning |
|---|---|---|
| `400` | `BAD_REQUEST` | Invalid request parameters or binding failure |
| `401` | `UNAUTHORIZED` | Invalid token if an Authorization header is supplied |
| `403` | `FORBIDDEN` | Authenticated but not allowed; not expected for current public endpoints |
| `404` | `NOT_FOUND` | Resource does not exist; current also-bought behavior returns an empty array for unknown products |
| `500` | `INTERNAL_SERVER_ERROR` | Unhandled or server-side failure |

### `BUSINESS_ERROR`

- Current recommendation endpoints do not intentionally raise module-specific `BUSINESS_ERROR`.
- Callers should still handle the shared envelope shape because lower layers and global exception handling are shared across modules.

## Endpoints

### `GET /api/recommend/home`

#### Purpose

Return products for the home page recommendation rail.

The strategy is:

- anonymous user: popularity-based products
- logged-in user with purchase-category history: popular products from purchased categories, filled with popularity fallback if needed
- logged-in user without purchase-category history: popularity fallback

#### Request

##### Query

| Field | Required | Type | Notes |
|---|---|---|---|
| `limit` | no | integer | Default `8`; service clamps effective range to `1..50` |

##### Headers

| Field | Required | Type | Notes |
|---|---|---|---|
| `Authorization` | no | string | Optional `Bearer <token>`; enables personalization when valid |

#### Response

- `data` shape: array of recommendation product items

| Field | Type | Notes |
|---|---|---|
| `id` | number | Product ID |
| `title` | string | Product title |
| `subtitle` | string/null | Product subtitle |
| `categoryId` | number/null | Category ID |
| `categoryName` | string/null | Category display name |
| `shopId` | number/null | Shop ID |
| `shopName` | string | Shop name, falling back to `Personal Seller` when absent |
| `sellerName` | string/null | Seller nickname |
| `salePrice` / `price` | number | Current sale price, exposed under both compatibility keys |
| `coverUrl` / `cover` / `mainImageUrl` | string/null | Main image URL, exposed under compatibility keys |
| `productType` / `type` | string | Product type, currently `physical` or `digital` |
| `status` | string | Product sale status |
| `reviewStatus` | string | Product review status |
| `viewCount` | number | Product view count |
| `favoriteCount` | number | Product favorite count |
| `createdAt` / `publishedAt` | string | Creation timestamp, exposed under compatibility keys |
| `updatedAt` | string | Last update timestamp |
| `source` | string | `popularity` or `category_preference` |
| `reason` | string/null | Human-readable recommendation reason |
| `score` | number | Present on popularity rows |

#### Error Cases

- `400`: non-numeric `limit` binding failure
- `401`: invalid Bearer token if supplied
- `500`: unexpected query or server failure

#### HTTP Asset Mapping

- `docs/06-http/recommend.http` lines 10-18: anonymous cold-start examples
- `docs/06-http/recommend.http` lines 26-36: authenticated personalization / fallback examples
- `docs/06-http/recommend.http` lines 60-68: limit edge cases

### `GET /api/recommend/also-bought/{productId}`

#### Purpose

Return products that were co-purchased with the target product in completed, paid orders.

#### Request

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `productId` | yes | number | Target product ID |

##### Query

| Field | Required | Type | Notes |
|---|---|---|---|
| `limit` | no | integer | Default `6`; service clamps effective range to `1..20` |

#### Response

- `data` shape: array of recommendation product items

Fields match `GET /api/recommend/home`, with these endpoint-specific values:

| Field | Type | Notes |
|---|---|---|
| `source` | string | Always `also-bought` |
| `reason` | string | `购买本商品的用户也买了` |
| `coPurchaseCount` | number | Count of distinct completed paid orders that include the co-purchased product with the target product |

Unknown products or products with no co-purchases return `data: []`.

#### Error Cases

- `400`: non-numeric `productId` or `limit` binding failure
- `401`: invalid Bearer token if supplied
- `500`: unexpected query or server failure

#### HTTP Asset Mapping

- `docs/06-http/recommend.http` lines 44-52

## Shared Types / Enumerations

| Field | Meaning |
|---|---|
| `source=popularity` | Ranked by view count and completed paid sales signal |
| `source=category_preference` | Derived from categories the current user has purchased from |
| `source=also-bought` | Derived from co-purchase history with the target product |
| `limit` | Service-level clamp prevents very large recommendation payloads |

## HTTP Asset Mapping

- Primary validation file: `docs/06-http/recommend.http`
- Additional related files: none

## Known Drift Or Follow-Up Notes

- No known method/path/auth drift between `RecommendController` and `docs/06-http/recommend.http`.
- Recommendation payloads are currently map-based. If dedicated DTOs are introduced later, this spec should be updated to use those DTOs as the contract source.
- Optional Redis cache keys are internal implementation detail; clients must not
  rely on cache state or observe different payload shapes when caching is on.
