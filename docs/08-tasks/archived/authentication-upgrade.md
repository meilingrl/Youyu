# Task: Authentication Upgrade Wave 1

## Metadata

- ID: authentication-upgrade
- Status: archived
- Owner: main-agent
- Track: cross-cutting
- Depends on: current auth baseline, SMTP provider credentials for runtime acceptance
- Priority: high
- Planned date: 2026-05-31
- Completed date: 2026-06-01

## Objective

Deliver real-email registration verification, CAPTCHA-gated password login
after repeated failures, and forgotten-password recovery without mixing normal
registration with student identity verification.

## Background

The existing auth module supports username, email, or student-number password
login and optional registration email storage. It does not send or verify email
codes, issue CAPTCHA challenges, or reset forgotten passwords. The customer
service FAQ already tells users that forgotten-password recovery is available
through campus email, so runtime behavior must be aligned with that promise.

## Scope

- implement the locked contract in `docs/02-requirements/authentication-upgrade-scope.md`
- preserve current unified user/admin login behavior
- send real email using SMTP configuration
- add focused backend, frontend, API, and manual verification coverage

## Out of Scope

- SMS verification
- email passwordless login
- OAuth or social login
- student-verification redesign
- distributed rate limiting
- JWT revocation

## Child Tasks

- [x] `authentication-upgrade-contract-and-schema`
- [x] `authentication-upgrade-backend`
- [x] `authentication-upgrade-frontend`
- [x] `authentication-upgrade-integration`

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/02-requirements/authentication-upgrade-scope.md`
- `docs/04-standards/development-process.md`
- `docs/09-api-spec/auth.md`
- `docs/06-http/auth.http`
- relevant auth frontend and backend files

## Allowed Changes

- files authorized by reviewed child tasks
- task lifecycle files under `docs/08-tasks/`
- auth API docs and HTTP smoke files
- `CHANGELOG.md` during final integration

## Implementation Plan

1. Review and activate the parent and child tasks.
2. Create an isolated topic worktree.
3. Dispatch contract/schema, backend, and frontend work in dependency order.
4. Review each worker diff before integration.
5. Run backend tests, frontend tests, frontend build, API smoke, and manual SMTP
   verification.
6. Update docs, archive accepted tasks, and prepend `CHANGELOG.md`.

## Risks

- leaking verification codes or SMTP secrets through logs
- returning account-enumeration signals from password recovery
- overlapping schema ownership across workers
- reintroducing the existing `account` versus `username` registration mismatch
- claiming email delivery works without exercising a real SMTP provider

## Test Plan

- Backend: `.\mvnw.cmd test`
- Frontend: `npm test`
- Frontend: `npm run build`
- API validation: exercise the updated `docs/06-http/auth.http`
- Manual: send a real registration email, complete registration, trigger
  CAPTCHA after failed logins, and reset a password through a real email

## Acceptance Criteria

- [x] Reviewed worker tasks are archived after main-agent verification.
- [x] Registration requires and consumes a verified email code.
- [x] Registration returns to login without issuing a session.
- [x] Login requires CAPTCHA after three consecutive failed attempts.
- [x] Forgotten-password recovery sends real email and updates the password.
- [x] Student identity verification remains a separate workflow.
- [x] Required tests, build, smoke, and manual SMTP verification pass.
- [x] Deferred work and production blockers remain explicit.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] `docs/06-http/auth.http`
- [x] `docs/09-api-spec/auth.md`
- [x] reviewed worker-task status and archive moves

## Completion Notes

Automated implementation and manual SMTP acceptance are complete. Contract/schema, backend, and frontend
worker diffs were reviewed and verified before archival. Backend tests,
frontend tests, frontend build, diff checks, browser route review, API docs,
HTTP examples, SMTP operator docs, and support FAQ wording are complete.

Manual acceptance verified real registration-email delivery, verified
registration without JWT issuance, password-reset delivery, login using the new
password, and CAPTCHA escalation after repeated failures. Deferred work remains
limited to the previously documented Wave 2 scope.
