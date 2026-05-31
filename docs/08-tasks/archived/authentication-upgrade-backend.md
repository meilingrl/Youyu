# Task: Authentication Upgrade Backend

## Metadata

- ID: authentication-upgrade-backend
- Status: archived
- Owner: worker-backend
- Track: feature
- Depends on: `authentication-upgrade-contract-and-schema`
- Priority: high
- Planned date: 2026-05-31
- Completed date: 2026-05-31

## Objective

Implement real SMTP email-code delivery, verified registration, CAPTCHA-gated
login, and forgotten-password reset using the locked auth contract.

## Scope

- add SMTP configuration and a mail sender abstraction
- use a deterministic fake sender only under the `test` profile
- add email-code send, CAPTCHA, verified registration, and password-reset
  controller/service behavior
- apply expiry, cooldown, attempts, and rate-limit rules
- preserve unified normal-user and admin login behavior
- add focused backend tests

## Out of Scope

- frontend changes
- SMS verification
- email passwordless login
- OAuth
- JWT revocation
- student-verification changes
- distributed rate limiting

## Files to Read

- `docs/02-requirements/authentication-upgrade-scope.md`
- `backend/pom.xml`
- current auth controller, DTO, service, password service, configuration, and
  exception-handler files
- challenge mapper files produced by the contract/schema task
- existing backend test patterns

## Allowed Changes

- `backend/pom.xml`
- auth controller, DTO, service, mapper, and configuration files
- focused backend auth tests
- `backend/src/main/resources/application*.yml` only for non-secret defaults and
  environment-variable wiring

## Acceptance Criteria

- [x] No verification code, password, reset payload, or SMTP password is logged.
- [x] Registration requires email and a valid one-time code.
- [x] Registration does not return a JWT.
- [x] Recovery responses do not expose whether an account exists.
- [x] CAPTCHA is required after three consecutive failed password attempts.
- [x] SMTP failures are reported honestly.
- [x] Backend tests pass without network access under the `test` profile.

## Completion Notes

Reviewed by the main agent. Full backend suite passes with 178 tests. The test
profile remains network-free. Real SMTP manual acceptance is tracked by the
blocked integration task.
