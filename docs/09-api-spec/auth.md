# API Spec: auth

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/campusmarket/backend/controller/auth/AuthController.java`
  - controller: `backend/src/main/java/com/campusmarket/backend/controller/admin/AdminAuthController.java`
  - request sample: `docs/06-http/auth.http`
- Last updated: 2026-05-16

## Scope

This document covers:

- user registration
- user login
- admin login
- current-user context query
- logout placeholder endpoint

It does not cover user profile, address, student verification, or preference endpoints.

## Authentication

- `POST /api/auth/register`: public
- `POST /api/auth/login`: public
- `POST /api/admin/auth/login`: public
- `GET /api/auth/me`: login required
- `POST /api/auth/logout`: login required

Current project samples use Bearer tokens such as `mock-1001-USER`.

## Response Envelope

All endpoints in this module use the unified response envelope:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Success",
  "data": {},
  "traceId": "..."
}
```

## Endpoints

### `POST /api/auth/register`

#### Purpose

Create a normal user account.

#### Request

- Body:
  - `username`: required, string, max 64
  - `password`: required, string, max 64
  - `nickname`: required, string, max 64
  - `phone`: optional, string, max 32
  - `email`: optional, string, max 128

#### Response

- `data` contains the registration result returned by `AuthService.register(...)`
- Callers should assume the response includes newly created user identity information and login-related context

#### Error Cases

- `400`: required field missing or field length invalid
- `409` or business error: username already exists

### `POST /api/auth/login`

#### Purpose

Log in as a normal user through the shared authentication service.

#### Request

- Body:
  - `loginId`: required, string
  - `password`: required, string

#### Response

- `data` contains the login result returned by `AuthService.unifiedLogin(...)`
- Callers should expect user identity, role, and token/session-related fields

#### Error Cases

- `400`: required field missing
- `401`: login ID or password invalid

### `POST /api/admin/auth/login`

#### Purpose

Log in through the admin login entrypoint.

#### Request

- Body:
  - `loginId`: required
  - `password`: required

#### Response

- `data` uses the same shared login result shape as `/api/auth/login`

#### Error Cases

- `401`: invalid admin credentials
- business error: account exists but does not satisfy admin-side access rules

### `GET /api/auth/me`

#### Purpose

Return the current authenticated user context.

#### Request

- Header:
  - `Authorization: Bearer <token>`

#### Response

- `data` contains the current user context returned by `AuthService.currentUser()`
- Callers should expect at least current identity and role information

#### Error Cases

- `401`: missing token or invalid token

### `POST /api/auth/logout`

#### Purpose

Reserve a logout entrypoint for future session invalidation logic.

#### Request

- Header:
  - `Authorization: Bearer <token>`

#### Response

- `message`: `Logout skeleton is reserved for future session invalidation logic`
- `data.module`: `auth`

#### Error Cases

- `401`: missing token or invalid token

## Shared Types / Enumerations

- Bearer token: current project examples use mock tokens during local development and testing
- Trace ID: every response may carry `traceId` for request tracing

## Notes

- Normal user login and admin login currently share the same underlying authentication service
- If login response fields are expanded later, update this document together with the controller-facing contract
