# Authentication Upgrade Scope

## Status

- Decision status: locked for Wave 1 planning
- Execution status: Wave 1 implemented and manually accepted
- Track: cross-cutting
- Date: 2026-05-31

## 1. Objective

Upgrade the public user authentication flow so that registration verifies a
real reachable email address, login adds a CAPTCHA challenge after repeated
failures, and users can recover a forgotten password through a verified email.

This scope keeps normal account registration separate from student identity
verification. A user may register with a non-campus email address. Student
verification continues to use the existing `/api/users/verification` flow.

## 2. Locked Product Decisions

- Registration email is required.
- Registration requires a verified email code before account creation.
- Registration succeeds without creating a login session. The frontend returns
  the user to the login view.
- Login remains password-based.
- After three consecutive failed login attempts, further password login attempts
  require a graphical CAPTCHA challenge.
- Forgotten-password recovery uses an email verification code and sets a new
  password.
- Email delivery must send real email through configured SMTP. A development
  log-only sender is not an accepted delivery mode.
- SMS verification and email passwordless login are explicitly deferred to
  Wave 2.

## 3. Terms

- Email passwordless login: sign in with an email verification code instead of
  a password. This is not included in Wave 1.
- SMTP: the standard protocol used by the backend to submit outgoing email to a
  configured mail provider.
- Graphical CAPTCHA: a server-generated image challenge used to slow automated
  password guessing.

## 4. Locked API Interfaces

### `POST /api/auth/email-codes`

Send a real email verification code.

Request:

```json
{
  "email": "user@example.com",
  "purpose": "register"
}
```

Allowed `purpose` values:

- `register`
- `reset_password`

Response:

```json
{
  "cooldownSeconds": 60,
  "expiresInSeconds": 600
}
```

Security rules:

- Do not return the verification code in an API response or log it.
- For password recovery, return the same response whether the email exists or
  not, to avoid account enumeration.
- Limit sends by email address and request source.

### `POST /api/auth/register`

Create a user after email verification.

Request:

```json
{
  "username": "new-user",
  "password": "pass123456",
  "nickname": "New User",
  "email": "user@example.com",
  "emailCode": "482913"
}
```

Response:

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

Registration must not return a JWT.

### `GET /api/auth/captcha`

Create a graphical CAPTCHA challenge.

Response:

```json
{
  "challengeId": "opaque-id",
  "imageDataUrl": "data:image/png;base64,...",
  "expiresInSeconds": 300
}
```

### `POST /api/auth/login`

Preserve the current password-login contract and add optional CAPTCHA fields.

Request:

```json
{
  "loginId": "new-user",
  "password": "pass123456",
  "captchaChallengeId": "opaque-id",
  "captchaCode": "AB12"
}
```

Behavior:

- The CAPTCHA fields may be omitted before the failure threshold.
- After three consecutive failed attempts for the login identifier and request
  source, CAPTCHA is required.
- A successful login clears the relevant failure counter.
- An invalid CAPTCHA does not reveal whether the account exists.

### `POST /api/auth/password-reset`

Reset a user password after email verification.

Request:

```json
{
  "email": "user@example.com",
  "emailCode": "482913",
  "newPassword": "new-pass123456"
}
```

Behavior:

- Consume the verification code exactly once.
- Hash the new password using the existing password service.
- Do not create a login session.

## 5. Persistence Boundary

Add additive tables for:

- email verification challenges
- graphical CAPTCHA challenges
- login failure counters

Requirements:

- Store verification and CAPTCHA codes as hashes, never plaintext.
- Store purpose, expiry, consumed state, attempt count, and cooldown data.
- Keep schema changes additive. Do not alter or drop existing auth tables.
- Use JDBC mapper interfaces and implementations following the existing backend
  architecture.

## 6. SMTP Configuration Boundary

Use Spring mail SMTP support with secrets supplied through environment
variables. Do not commit credentials.

Required runtime variables:

```text
APP_MAIL_HOST
APP_MAIL_PORT
APP_MAIL_USERNAME
APP_MAIL_PASSWORD
APP_MAIL_FROM
APP_MAIL_SSL_ENABLED
```

Rules:

- `test` profile uses a deterministic fake sender and never accesses the
  network.
- Any environment that enables public email-code endpoints must use SMTP.
- Missing or invalid SMTP configuration must produce a clear operational
  failure. Do not claim that a code was sent when delivery failed.
- Provider-specific credentials and sender-domain setup remain deployment
  configuration, not repository source code.

## 7. Security Baseline

- Email code expiry: 10 minutes.
- Resend cooldown: 60 seconds.
- Maximum verification attempts per challenge: 5.
- Login CAPTCHA threshold: 3 consecutive failed password attempts.
- CAPTCHA expiry: 5 minutes.
- Apply request-source and identifier-based rate limiting.
- Avoid account-enumeration differences in recovery responses.
- Do not log passwords, codes, SMTP passwords, or reset payloads.

Exact rate-limit windows may be tuned during implementation, but the locked
security behavior above must remain intact.

## 8. Frontend Boundary

- Keep `/login` and `/register` as public routes.
- Add a public `/forgot-password` route.
- Registration collects email, sends a real verification code, and requires
  the code before submission.
- Login requests and displays CAPTCHA only after the backend reports it is
  required.
- Password recovery sends an email code, accepts a new password, and returns to
  login after success.
- Preserve the current unified login destination logic for normal users and
  admin roles.

## 9. Known Existing Regression

`frontend/src/views/auth/LoginView.vue` stores the registration username in
`registerForm.account`, while `POST /api/auth/register` expects `username`.
`frontend/src/api/modules/auth.js` currently passes the registration payload
through without mapping the field. The Wave 1 frontend slice must restore the
contract adapter and cover it with a frontend test.

## 10. Explicitly Deferred

- SMS verification
- Email passwordless login
- OAuth or social login
- Admin password recovery
- Student-verification redesign
- Distributed rate limiting for multi-node deployment
- Session invalidation or JWT revocation after password reset

## 11. Production Blockers

- A real SMTP provider, verified sender address, and valid credentials must be
  configured before runtime acceptance.
- The current single-node persistence design is adequate for this wave but must
  be revisited before multi-node deployment.
- Password reset does not revoke already-issued JWTs in this wave. This remains
  a visible security follow-up.
