# API Spec: auth

## Document Info

- Status: active
- Source of truth:
  - locked requirement: `docs/02-requirements/authentication-upgrade-scope.md`
  - user controller: `backend/src/main/java/com/youyu/backend/controller/auth/AuthController.java`
  - admin controller: `backend/src/main/java/com/youyu/backend/controller/admin/AdminAuthController.java`
  - request sample: `docs/06-http/auth.http`
- Last updated: 2026-05-31

## Scope

This document covers:

- registration email-code delivery
- verified user registration
- graphical CAPTCHA creation
- password login with CAPTCHA escalation
- password reset by verified email
- admin login
- current-user context query
- logout placeholder endpoint

Registration email verification remains separate from student identity
verification. Student verification continues to use `/api/users/verification`.

## Authentication

- `POST /api/auth/email-codes`: public
- `POST /api/auth/register`: public
- `GET /api/auth/captcha`: public
- `POST /api/auth/login`: public
- `POST /api/auth/password-reset`: public
- `POST /api/admin/auth/login`: public
- `GET /api/auth/me`: login required
- `POST /api/auth/logout`: login required

Current project samples use Bearer tokens such as `mock-1001-USER`,
`mock-9001-ADMIN`, and specialist admin tokens such as
`mock-9103-REVIEWER`.

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

### `POST /api/auth/email-codes`

#### Purpose

Send a real email verification code through configured SMTP.

#### Request

```json
{
  "email": "user@example.com",
  "purpose": "register"
}
```

- `email`: required, string, max 128
- `purpose`: required, one of `register`, `reset_password`

#### Response

```json
{
  "cooldownSeconds": 60,
  "expiresInSeconds": 600
}
```

#### Security Behavior

- The verification code is never returned in an API response or written to logs.
- Password-recovery requests return the same public response whether the email
  exists or not.
- Sending is limited by email address and request source.
- SMTP delivery failure is returned as an operational failure. The endpoint
  must not claim that a code was sent after delivery fails.

### `POST /api/auth/register`

#### Purpose

Create a normal user account after email verification.

#### Request

```json
{
  "username": "new-user",
  "password": "pass123456",
  "nickname": "New User",
  "email": "user@example.com",
  "emailCode": "482913"
}
```

- `username`: required, string, max 64
- `password`: required, string, max 64
- `nickname`: required, string, max 64
- `email`: required, string, max 128
- `emailCode`: required, string
- `phone`: optional, string, max 32

#### Response

```json
{
  "user": {
    "id": "123",
    "loginId": "new-user",
    "nickname": "New User",
    "verificationStatus": "unverified"
  }
}
```

Registration does not return a JWT or create a login session.

#### Error Cases

- `400`: required field missing, field length invalid, or verification code invalid
- `409` or business error: username or email already exists

### `GET /api/auth/captcha`

#### Purpose

Create a graphical CAPTCHA challenge for password-login escalation.

#### Response

```json
{
  "challengeId": "opaque-id",
  "imageDataUrl": "data:image/png;base64,...",
  "expiresInSeconds": 300
}
```

The CAPTCHA answer is never returned separately and is stored only as a hash.

### `POST /api/auth/login`

#### Purpose

Log in as a normal user through the shared authentication service.

#### Request

```json
{
  "loginId": "new-user",
  "password": "pass123456",
  "captchaChallengeId": "opaque-id",
  "captchaCode": "AB12"
}
```

- `loginId`: required, string
- `password`: required, string
- `captchaChallengeId`: optional before CAPTCHA escalation, string
- `captchaCode`: optional before CAPTCHA escalation, string

#### Behavior

- CAPTCHA is required after three consecutive failed password attempts for the
  login identifier and request source.
- A successful login clears the relevant failure counter.
- An invalid CAPTCHA does not reveal whether the account exists.

#### Response

- `data` contains user identity, role, privilege, and token fields returned by
  the shared login service.

#### Error Cases

- `400`: required field missing
- `401`: login ID, password, or CAPTCHA invalid
- business error: CAPTCHA is required before another password-login attempt

### `POST /api/auth/password-reset`

#### Purpose

Reset a user password after email verification.

#### Request

```json
{
  "email": "user@example.com",
  "emailCode": "482913",
  "newPassword": "new-pass123456"
}
```

- `email`: required, string, max 128
- `emailCode`: required, string
- `newPassword`: required, string, max 64

#### Behavior

- The verification code is consumed exactly once.
- The new password is hashed using the existing password service.
- Password reset does not create a login session.
- Password reset does not revoke already-issued JWTs in Wave 1.

### `POST /api/admin/auth/login`

#### Purpose

Log in through the admin login entrypoint.

#### Request

- `loginId`: required
- `password`: required

#### Response

- `data` uses the same shared login result shape as `/api/auth/login`.
- Admin staff roles in `data.role` may be `admin`, `super_admin`,
  `support_agent`, `reviewer`, `operator`, or `order_admin`.
- The legacy `admin` role remains full-access compatible with `super_admin`.

#### Error Cases

- `401`: invalid admin credentials
- business error: account exists but does not satisfy admin-side access rules

### `GET /api/auth/me`

#### Purpose

Return the current authenticated user context.

#### Request

- Header: `Authorization: Bearer <token>`

#### Response

- `data` contains the current user context returned by `AuthService.currentUser()`.

#### Error Cases

- `401`: missing token or invalid token

### `POST /api/auth/logout`

#### Purpose

Reserve a logout entrypoint for future session invalidation logic.

#### Request

- Header: `Authorization: Bearer <token>`

#### Response

- `message`: `Logout skeleton is reserved for future session invalidation logic`
- `data.module`: `auth`

#### Error Cases

- `401`: missing token or invalid token

## Shared Types / Enumerations

- Email-code purpose: `register`, `reset_password`
- Bearer token: current project examples use mock tokens during local development
  and testing
- Mock token role segment accepts uppercase role names including underscores,
  for example `mock-9102-SUPPORT_AGENT`
- Admin staff roles: `ADMIN`, `SUPER_ADMIN`, `SUPPORT_AGENT`, `REVIEWER`,
  `OPERATOR`, `ORDER_ADMIN`
- Trace ID: every response may carry `traceId` for request tracing

## Explicitly Deferred

- SMS verification
- email passwordless login
- OAuth or social login
- student-verification redesign
- JWT revocation after password reset
- distributed rate limiting for multi-node deployment
