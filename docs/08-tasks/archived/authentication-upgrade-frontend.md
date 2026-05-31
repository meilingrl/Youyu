# Task: Authentication Upgrade Frontend

## Metadata

- ID: authentication-upgrade-frontend
- Status: archived
- Owner: worker-frontend
- Track: feature
- Depends on: `authentication-upgrade-contract-and-schema`
- Priority: high
- Planned date: 2026-05-31
- Completed date: 2026-05-31

## Objective

Deliver the public registration, password-login, CAPTCHA, and
forgotten-password experience for the locked auth API.

## Scope

- add email and email-code controls to registration
- add send-code cooldown and request feedback
- restore `account` to `username` registration mapping in the auth API adapter
- add login CAPTCHA display and refresh behavior when required by the backend
- add `/forgot-password` route and password-reset form
- preserve current login redirect behavior
- add frontend tests for payload shaping and auth flow state

## Out of Scope

- backend changes
- student-verification UI redesign
- SMS UI
- email passwordless login
- unrelated login-page visual redesign

## Files to Read

- `docs/02-requirements/authentication-upgrade-scope.md`
- `frontend/README.md`
- `frontend/src/views/auth/LoginView.vue`
- `frontend/src/api/modules/auth.js`
- `frontend/src/stores/auth.js`
- `frontend/src/router/index.js`
- existing frontend auth tests

## Allowed Changes

- scoped frontend auth views and components
- `frontend/src/api/modules/auth.js`
- `frontend/src/stores/auth.js`
- `frontend/src/router/index.js`
- focused frontend tests

## Acceptance Criteria

- [x] Registration sends `username`, required email, and email code correctly.
- [x] Registration success returns the user to login without creating a session.
- [x] CAPTCHA appears only when required and can be refreshed.
- [x] Forgotten-password success returns the user to login.
- [x] Existing role-aware login redirects still work.
- [x] Frontend tests and build pass.

## Completion Notes

Reviewed by the main agent. Frontend tests pass with 49 tests, production build
succeeds, and browser checks cover login, registration, and forgotten-password
routes.
