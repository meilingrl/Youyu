# API Spec: user

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/campusmarket/backend/controller/user/UserController.java`
  - request DTO: `backend/src/main/java/com/campusmarket/backend/controller/user/dto/SubmitStudentVerificationRequest.java`
  - request DTO: `backend/src/main/java/com/campusmarket/backend/controller/user/dto/CreateUserAddressRequest.java`
  - shared response / error handling: `backend/src/main/java/com/campusmarket/backend/common/api/ApiResponse.java`
  - shared response / error handling: `backend/src/main/java/com/campusmarket/backend/controller/advice/GlobalExceptionHandler.java`
  - request sample: `docs/06-http/auth.http`
  - related task: `docs/08-tasks/drafts/api-spec-standardization-follow-up.md`
- Last updated: 2026-05-16

## Scope

This document covers authenticated user-side endpoints under `/api/users`:

- own profile
- preference read and update
- personal insight snapshot
- student verification read and submit
- address list, creation, and default-address switching

It does not cover authentication endpoints under `/api/auth`, product operations, shop operations, or admin governance endpoints.

## Authentication And Roles

- All endpoints in this module require login.
- `UserController` uses class-level `@LoginRequired`.
- Current project samples use Bearer tokens such as `mock-1001-USER`.

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
| `400` | `BAD_REQUEST` | Invalid request parameters or validation failure |
| `401` | `UNAUTHORIZED` | Missing token, invalid token, or not logged in |
| `403` | `FORBIDDEN` | Authenticated but blocked by access rules |
| `404` | `NOT_FOUND` | Requested user-side resource does not exist |
| `500` | `INTERNAL_SERVER_ERROR` | Unhandled or server-side failure |

### `BUSINESS_ERROR`

- Current backend behavior returns `200 OK` with `success=false`, `code="BUSINESS_ERROR"`, and a business-facing message.
- This module mainly uses `BAD_REQUEST`, `UNAUTHORIZED`, and `NOT_FOUND`, but callers should still be prepared for business-layer failures expressed through the unified envelope.

## Endpoints

### `GET /api/users/profile`

#### Purpose

Return the current authenticated user's profile summary.

#### Request

No query parameters, path parameters, or body.

#### Response

- `data` is the current user profile object returned by `UserService.profile()`.

| Field | Type | Notes |
|---|---|---|
| `data.id` | number | Current user ID |
| `data.*` | object | Caller should expect profile, identity, and account-state fields from the current user context |

#### Error Cases

- `401`: missing token or invalid token

### `GET /api/users/me/preference`

#### Purpose

Return persisted preference settings for the current user, or default values when the user has not customized them.

#### Request

No query parameters, path parameters, or body.

#### Response

- `data` is the preference object returned by `UserService.preference()`.

| Field | Type | Notes |
|---|---|---|
| `themeMode` | string | Example values include visual theme mode settings |
| `themeColor` | string | Theme accent selection |
| `homeDisplayMode` | string | Home-page display preference |
| `defaultFulfillmentType` | string | Preferred order fulfillment mode |
| `defaultPaymentMethod` | string | Preferred payment method |
| `defaultSortType` | string | Preferred list sorting |
| `notificationPreference` | object | Nested reminder / notification switches |

#### Error Cases

- `401`: missing token or invalid token

### `PUT /api/users/me/preference`

#### Purpose

Update persisted preference settings for the current user.

#### Request

##### Body

| Field | Required | Type | Notes |
|---|---|---|---|
| `themeMode` | no | string | Optional partial update field |
| `themeColor` | no | string | Optional partial update field |
| `homeDisplayMode` | no | string | Optional partial update field |
| `defaultFulfillmentType` | no | string | Optional partial update field |
| `defaultPaymentMethod` | no | string | Optional partial update field |
| `defaultSortType` | no | string | Optional partial update field |
| `notificationPreference` | no | object | Optional nested preference object |

#### Response

- `data` is the updated preference object returned by `UserService.updatePreference(...)`.

| Field | Type | Notes |
|---|---|---|
| `data` | object | Updated persisted preference state |

#### Error Cases

- `400`: request body shape or field values invalid
- `401`: missing token or invalid token

### `GET /api/users/me/insight-snapshot`

#### Purpose

Return a compact user insight snapshot for the current user.

#### Request

No query parameters, path parameters, or body.

#### Response

- `data` is the aggregated snapshot returned by `UserService.insightSnapshot()`.

| Field | Type | Notes |
|---|---|---|
| `data` | object | Caller should expect summary metrics and recent insight fields rather than full raw history |

#### Error Cases

- `401`: missing token or invalid token

### `GET /api/users/verification`

#### Purpose

Return the current user's student verification status and latest verification record when available.

#### Request

No query parameters, path parameters, or body.

#### Response

- `data` is the verification-status object returned by `UserService.verificationStatus()`.

| Field | Type | Notes |
|---|---|---|
| `status` | string | Verification lifecycle state |
| `data.*` | object | May include latest verification metadata and review outcome fields |

#### Error Cases

- `401`: missing token or invalid token

### `POST /api/users/verification`

#### Purpose

Submit a new student verification request for the current user.

#### Request

This endpoint uses `SubmitStudentVerificationRequest` with `@Valid`.

| Field | Required | Type | Notes |
|---|---|---|---|
| `studentNo` | yes | string | `@NotBlank`, max 64 |
| `realName` | yes | string | `@NotBlank`, max 64 |
| `college` | no | string | Max 128 |
| `major` | no | string | Max 128 |
| `grade` | no | string | Max 64 |
| `campusEmail` | no | string | Max 128 |
| `verificationMethod` | no | string | Max 32, current default `manual_review` |

#### Response

- `data` is the created or accepted verification record returned by `UserService.submitVerification(...)`.

| Field | Type | Notes |
|---|---|---|
| `data` | object | Verification submission result |

#### Error Cases

- `400`: validation failure or request violates current verification rules
- `401`: missing token or invalid token
- `BUSINESS_ERROR`: verification cannot proceed due to current account state

### `GET /api/users/addresses`

#### Purpose

List saved addresses belonging to the current user.

#### Request

No query parameters, path parameters, or body.

#### Response

- `data` is an array returned by `UserService.addresses()`.

| Field | Type | Notes |
|---|---|---|
| `data` | array | Address list |
| `data[].id` | number | Address ID |
| `data[].isDefault` | boolean | Default-address marker in response payload |

#### Error Cases

- `401`: missing token or invalid token

### `POST /api/users/addresses`

#### Purpose

Create a new saved address for the current user.

#### Request

This endpoint uses `CreateUserAddressRequest` with `@Valid`.

| Field | Required | Type | Notes |
|---|---|---|---|
| `receiverName` | yes | string | `@NotBlank`, max 64 |
| `receiverPhone` | yes | string | `@NotBlank`, max 32 |
| `addressType` | no | string | Max 32, current default `campus` |
| `province` | no | string | Optional |
| `city` | no | string | Optional |
| `district` | no | string | Optional |
| `detailAddress` | yes | string | `@NotBlank`, max 255 |
| `campusArea` | no | string | Max 128 |
| `defaultAddress` | no | boolean | Whether to mark the new address as default |

#### Response

- `data` is the newly created address object returned by `UserService.createAddress(...)`.

| Field | Type | Notes |
|---|---|---|
| `data` | object | Created address result |

#### Error Cases

- `400`: validation failure
- `401`: missing token or invalid token

### `PUT /api/users/addresses/{addressId}/default`

#### Purpose

Mark an existing address as the current user's default address.

#### Request

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `addressId` | yes | number | Address ID owned by the current user |

#### Response

- `data` is the updated address state returned by `UserService.setDefaultAddress(...)`.

| Field | Type | Notes |
|---|---|---|
| `data` | object | Updated default-address result |

#### Error Cases

- `401`: missing token or invalid token
- `404`: address does not exist

## Shared Types / Enumerations

- `verificationMethod`: current default is `manual_review`
- `addressType`: current request DTO default is `campus`
- Preference payloads are map-style update objects rather than strict request DTOs

## HTTP Asset Mapping

- Primary validation file: `docs/06-http/auth.http`
- Additional related files: none

## Known Drift Or Follow-Up Notes

- No known controller/spec drift is being left unresolved in this module as part of this iteration.
- `auth.http` currently hosts both auth and authenticated user examples; if user-side coverage grows further, the repository may later split a dedicated `user.http`.
