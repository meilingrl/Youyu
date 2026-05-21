# API Spec: product

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/campusmarket/backend/controller/product/ProductController.java`
  - request sample: `docs/06-http/product.http`
- Last updated: 2026-05-16

## Scope

This document covers:

- public product listing and detail
- seller-owned product listing
- product publish, update, status change, and delete
- product review list and review summary under product detail

It does not cover favorites, shops, categories, or recommendation endpoints.

## Authentication

- Public:
  - `GET /api/products`
  - `GET /api/products/{productId}`
  - `GET /api/products/{id}/reviews`
  - `GET /api/products/{id}/review-summary`
- Logged-in user only:
  - `GET /api/products/mine`
  - `POST /api/products`
  - `PUT /api/products/{productId}`
  - `PUT /api/products/{productId}/status`
  - `DELETE /api/products/{productId}`

## Response Envelope

All endpoints in this module use the unified response envelope with `success`, `code`, `message`, `data`, and `traceId`.

## Endpoints

### `GET /api/products`

#### Purpose

List public products with basic filtering and pagination.

#### Request

- Query:
  - `keyword`: optional
  - `categoryId`: optional
  - `productType`: optional, typically `physical` or `digital`
  - `page`: optional, default `1`
  - `pageSize`: optional, default `12`

#### Response

- `data.items`: product list
- `data.total`: total matched records
- `data.page`: current page number
- `data.pageSize`: current page size

#### Error Cases

- `400`: invalid pagination or invalid query values

### `GET /api/products/{productId}`

#### Purpose

Return product detail for a public product.

#### Request

- Path:
  - `productId`: required

#### Response

- `data` is the detail object returned by `ProductService.getProductDetail(...)`
- Current detail payload includes product base fields and media information

#### Error Cases

- `404`: product does not exist
- business error: product is not visible to the current user

### `GET /api/products/mine`

#### Purpose

List products owned by the current seller user.

#### Request

- Header:
  - `Authorization: Bearer <token>`

#### Response

- `data`: array of seller-owned product objects

#### Error Cases

- `401`: not logged in
- `403`: role is not allowed

### `POST /api/products`

#### Purpose

Publish a new product.

#### Request

- Header:
  - `Authorization: Bearer <token>`
- Body common fields:
  - `title`: required
  - `categoryId`: required
  - `productType` or `type`: optional, default `physical`
  - `subtitle`: optional
  - `description`: optional
  - `coverUrl` or `cover`: optional
  - `salePrice` or `price`: required
  - `originalPrice`: optional
  - `stock` or `stockQuantity`: optional, minimum effective value `1`
  - `submitMode`: optional, `submit` or `draft`
  - `allowPreview`: optional, boolean
  - `previewRuleText` or `previewHint`: optional
  - delivery-related input must produce at least one delivery method:
    - logistics
    - offline
    - digital

#### Response

- `data` is the newly created product object
- Service-side behavior currently derives:
  - `status`
  - `reviewStatus`
  - delivery capability flags

#### Error Cases

- `400`: missing `title`, missing `categoryId`, missing delivery method, bad numeric value
- `401`: not logged in
- `403`: role is not allowed
- business error: seller has no approved shop or product is otherwise not publishable

### `PUT /api/products/{productId}`

#### Purpose

Update an existing seller-owned product.

#### Request

- Header:
  - `Authorization: Bearer <token>`
- Path:
  - `productId`: required
- Body:
  - same contract family as publish

#### Response

- `data`: updated product object

#### Error Cases

- `401`: not logged in
- `403`: current user does not own the product
- `404`: product does not exist

### `PUT /api/products/{productId}/status`

#### Purpose

Change product sale status.

#### Request

- Header:
  - `Authorization: Bearer <token>`
- Body:
  - `status`: required

#### Response

- `data`: updated product object after status change

#### Error Cases

- `400`: missing or invalid status
- `403`: current user does not own the product

### `DELETE /api/products/{productId}`

#### Purpose

Delete a seller-owned product.

#### Request

- Header:
  - `Authorization: Bearer <token>`

#### Response

- `data`: deletion result from service layer

#### Error Cases

- `403`: current user does not own the product
- `404`: product does not exist

### `GET /api/products/{id}/reviews`

#### Purpose

List public reviews for a product.

#### Request

- Path:
  - `id`: product ID
- Query:
  - `page`: optional, default `1`
  - `pageSize`: optional, default `10`

#### Response

- `data`: paginated review result returned by `ReviewService.getProductReviews(...)`

#### Error Cases

- `404`: product does not exist

### `GET /api/products/{id}/review-summary`

#### Purpose

Return aggregated review summary for a product.

#### Request

- Path:
  - `id`: product ID

#### Response

- `data`: summary object returned by `ReviewService.getProductReviewSummary(...)`

#### Error Cases

- `404`: product does not exist

## Shared Types / Enumerations

- `productType`: current code treats `physical` as default and supports `digital`
- delivery capability flags are normalized into:
  - `supportsLogistics`
  - `supportsOfflineDelivery`
  - `supportsDigitalDelivery`
- service-side status derivation currently distinguishes:
  - `draft`
  - `off_sale`
  - `on_sale`
  - `pending_review`
  - `not_required`

## Notes

- Product publish/update currently accept flexible map-style payloads rather than strict DTOs
- When this module later introduces dedicated request DTOs, update this spec to replace alias-style field descriptions
