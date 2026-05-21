# ADR-002: Replace Mock Token Auth with HMAC-SHA256 JWT

**Date:** 2026-05-11
**Status:** Accepted

## Context

Token issuance in both `AuthServiceImpl` and `AdminServiceImpl` returns a hardcoded string `"mock-{userId}-{ROLE}"`. `MockAuthTokenServiceImpl` parses this string to reconstruct the user identity. There is no signature, no expiry, and no way to invalidate a token.

This is acceptable during early development when the only concern is routing, but it is a hard blocker for any deployment because:

- Any user can forge a token by guessing the format (`mock-9001-ADMIN`)
- There is no expiry mechanism
- JWT is the de facto standard for stateless API auth in Spring Boot projects

## Decision

Implement `JwtAuthTokenServiceImpl` using the `jjwt` library (0.12.6) with HMAC-SHA256 signing. The interface gains a `generate(Long userId, String role)` method. Token issuance in `AuthServiceImpl` and `AdminServiceImpl` calls `authTokenService.generate(...)` instead of string concatenation.

Mock token parsing (`mock-{id}-{ROLE}`) is **retained inside `JwtAuthTokenServiceImpl.resolve()`** as a developer convenience, not exposed as a public API. This allows all existing `.http` files and development workflows to continue using mock tokens without changes.

## Consequences

**Positive:** Tokens are cryptographically signed and carry an expiry. Forged tokens are rejected at the interceptor. The auth surface is now production-ready.

**Negative:** Tokens cannot be individually revoked without a token blacklist (not implemented). Developers must use either a mock token or log in to get a real JWT — they can no longer fabricate arbitrary user IDs.

**Neutral:** The `AuthInterceptor` is unchanged — it still calls `AuthTokenService.resolve()` and reads `AuthContextHolder`. The change is entirely within the token service layer.
