# API Spec: marketing

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/youyu/backend/controller/marketing/MarketingController.java`
  - admin controller: `backend/src/main/java/com/youyu/backend/controller/admin/AdminController.java`
  - service: `backend/src/main/java/com/youyu/backend/service/marketing/MarketingService.java`
  - request sample: `docs/06-http/marketing.http`
- Last updated: 2026-05-29

## Scope

This document covers the first marketing MVP:

- shop-owner coupon and activity management
- buyer coupon discovery, claim, and owned-coupon list
- public shop activity display
- admin review/disable for coupons and activities

Order preview/create coupon usage is documented in `order.md`.

## Authentication And Permissions

- Owner endpoints require login and an approved shop owned by the current user.
- Buyer coupon claim and owned-coupon endpoints require login.
- Public shop activities are readable without login.
- Admin review endpoints require an admin staff role with `ADMIN_MARKETING_REVIEW`.
  - `ADMIN` / `SUPER_ADMIN`: allowed
  - `REVIEWER`: allowed
  - `OPERATOR`, `SUPPORT_AGENT`, `ORDER_ADMIN`: not allowed

## Shared Fields

### Coupon

| Field | Type | Notes |
|---|---|---|
| `id` | number | Coupon ID |
| `shopId` | number | Owning shop |
| `ownerUserId` | number | Shop owner |
| `title` | string | Coupon title |
| `description` | string | Optional rule text |
| `couponType` | string | `FIXED` or `THRESHOLD` |
| `discountAmount` | decimal | Discount amount |
| `minimumSpendAmount` | decimal | Threshold amount; `0.00` for fixed coupons |
| `totalQuantity` | number | Issued quantity cap |
| `claimedQuantity` | number | Claimed count |
| `remainingQuantity` | number | Derived remaining count |
| `status` | string | `active` or `disabled` |
| `reviewStatus` | string | `pending_review`, `approved`, or `rejected` |
| `rejectReason` | string | Present for rejected content |
| `startAt` / `endAt` | timestamp | Effective window |

### User Coupon

Owned-coupon rows include coupon fields plus:

| Field | Type | Notes |
|---|---|---|
| `userCouponId` | number | User-owned coupon ID used by checkout |
| `couponId` | number | Source coupon ID |
| `userCouponStatus` | string | `claimed` or `used` |
| `claimedAt` / `usedAt` | timestamp | Claim/use timestamps |
| `orderId` | number | Order that consumed the coupon, when used |

### Activity

| Field | Type | Notes |
|---|---|---|
| `id` | number | Activity ID |
| `shopId` | number | Owning shop |
| `ownerUserId` | number | Shop owner |
| `title` | string | Activity title |
| `description` | string | Display copy |
| `status` | string | `active` or `disabled` |
| `reviewStatus` | string | `pending_review`, `approved`, or `rejected` |
| `rejectReason` | string | Present for rejected content |
| `startAt` / `endAt` | timestamp | Effective window |

Activities are display-only in this MVP and do not change order prices.

## Owner Endpoints

### `GET /api/marketing/owner/coupons`

List coupons for the current user's approved shop.

### `POST /api/marketing/owner/coupons`

Create a coupon in `pending_review`.

Required body fields: `title`, `couponType`, `discountAmount`, `totalQuantity`, `startAt`, `endAt`.
For `THRESHOLD`, `minimumSpendAmount` is required and must be greater than `discountAmount`.

### `PUT /api/marketing/owner/coupons/{couponId}`

Update an owned coupon. Editing resets review state to `pending_review`; claimed coupons cannot be edited.

### `PUT /api/marketing/owner/coupons/{couponId}/status`

Enable or disable an owned coupon. Body: `{ "status": "active" | "disabled" }`.

### `GET /api/marketing/owner/activities`

List activities for the current user's approved shop.

### `POST /api/marketing/owner/activities`

Create an activity in `pending_review`. Body fields: `title`, `description`, `startAt`, `endAt`.

### `PUT /api/marketing/owner/activities/{activityId}`

Update an owned activity and reset review state to `pending_review`.

### `PUT /api/marketing/owner/activities/{activityId}/status`

Enable or disable an owned activity. Body: `{ "status": "active" | "disabled" }`.

## Buyer And Public Endpoints

### `GET /api/marketing/coupons/available?shopId={shopId}`

List approved, active, effective, in-stock coupons for a shop. Login is required.

### `POST /api/marketing/coupons/{couponId}/claim`

Claim a coupon. Duplicate claim and over-claim return a business error.

### `GET /api/marketing/my-coupons`

List coupons claimed by the current user.

### `GET /api/marketing/shops/{shopId}/activities`

Publicly list approved, active, effective activities for a shop.

## Admin Review Endpoints

### `GET /api/admin/marketing/coupons`

Query coupon review queue. Optional query: `reviewStatus`.

### `PUT /api/admin/marketing/coupons/{couponId}/review`

Approve or reject a coupon.

Body fields:

- `action`: `approve` or `reject`
- `rejectReason`: required when rejecting
- `reviewNote`: optional

### `PUT /api/admin/marketing/coupons/{couponId}/disable`

Disable a coupon that carries risk.

### `GET /api/admin/marketing/activities`

Query activity review queue. Optional query: `reviewStatus`.

### `PUT /api/admin/marketing/activities/{activityId}/review`

Approve or reject an activity. Body shape matches coupon review.

### `PUT /api/admin/marketing/activities/{activityId}/disable`

Disable an activity that carries risk.

## Error Cases

- `401`: missing or invalid login
- `403`: not shop owner, shop not approved, or admin role lacks `ADMIN_MARKETING_REVIEW`
- `404`: coupon/activity/user coupon not found
- `400`: invalid payload or coupon not applicable to the selected order/shop
- `BUSINESS_ERROR`: duplicate claim, over-claim, duplicate use, inactive content, or failed business transition
