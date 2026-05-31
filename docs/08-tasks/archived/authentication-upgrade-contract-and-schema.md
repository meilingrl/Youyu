# Task: Authentication Upgrade Contract and Schema

## Metadata

- ID: authentication-upgrade-contract-and-schema
- Status: archived
- Owner: worker-contract-schema
- Track: cross-cutting
- Depends on: `authentication-upgrade`
- Priority: high
- Planned date: 2026-05-31
- Completed date: 2026-05-31

## Objective

Freeze the auth API contract and add the additive persistence foundation for
email codes, graphical CAPTCHA challenges, and login-failure counters.

## Scope

- align formal auth API docs and HTTP smoke examples with the locked contract
- add additive auth challenge tables to `schema.sql`
- add mapper interfaces and JDBC implementations for challenge persistence
- add focused mapper tests if required by the implementation

## Out of Scope

- controller and service orchestration
- SMTP implementation
- frontend changes
- edits to existing `users` columns
- seed credentials or production secrets

## Files to Read

- `docs/02-requirements/authentication-upgrade-scope.md`
- `docs/09-api-spec/auth.md`
- `docs/06-http/auth.http`
- `backend/src/main/resources/schema.sql`
- existing JDBC mapper patterns

## Allowed Changes

- `backend/src/main/resources/schema.sql`
- new auth challenge mapper files under `backend/src/main/java/com/youyu/backend/mapper/auth/`
- focused backend mapper tests
- `docs/09-api-spec/auth.md`
- `docs/06-http/auth.http`

## Acceptance Criteria

- [x] Schema additions are additive and idempotent.
- [x] Codes are stored only as hashes.
- [x] API docs cover email-code send, registration verification, CAPTCHA login,
      and password reset.
- [x] HTTP examples contain no real secrets or real recipient addresses.

## Completion Notes

Reviewed by the main agent. Mapper tests pass, schema additions are additive,
HTTP examples use reserved addresses, and `git diff --check` is clean.
