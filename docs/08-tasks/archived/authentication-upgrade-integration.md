# Task: Authentication Upgrade Integration

## Metadata

- ID: authentication-upgrade-integration
- Status: archived
- Owner: main-agent
- Track: cross-cutting
- Depends on: `authentication-upgrade-backend`, `authentication-upgrade-frontend`
- Priority: high
- Planned date: 2026-05-31
- Completed date: 2026-06-01

## Objective

Review worker diffs, resolve narrow integration issues, verify real SMTP
delivery, close documentation, and archive the accepted authentication upgrade.

## Scope

- inspect each worker diff for scope and locked-interface compliance
- run backend, frontend, build, diff, API smoke, and manual checks
- exercise a real SMTP recipient for registration and password reset
- update operational documentation with SMTP setup and troubleshooting
- align customer-service FAQ wording with the delivered recovery behavior
- prepend `CHANGELOG.md`
- fill completion notes and archive reviewed tasks

## Out of Scope

- new capability expansion during integration
- SMS verification
- email passwordless login
- provider credentials in repository files

## Files to Read

- all authentication-upgrade task files
- worker diffs
- `docs/04-standards/operations-and-deployment.md`
- `backend/README.md`
- `backend/src/main/java/com/youyu/backend/service/chat/SupportFaqKnowledgeBase.java`

## Allowed Changes

- narrow auth integration fixes
- auth API docs and HTTP smoke files
- SMTP operator documentation
- support FAQ wording
- `CHANGELOG.md`
- task lifecycle files under `docs/08-tasks/`

## Acceptance Criteria

- [x] Worker diffs are read and accepted by the main agent.
- [x] `backend`: `.\mvnw.cmd test` passes.
- [x] `frontend`: `npm test` passes.
- [x] `frontend`: `npm run build` passes.
- [x] `git diff --check` passes.
- [x] A real registration email is delivered and consumed successfully.
- [x] A real password-reset email is delivered and consumed successfully.
- [x] CAPTCHA is required after three consecutive failed login attempts.
- [x] SMTP setup and deferred security work remain explicit in docs.
- [x] Parent and child tasks are archived only after verification.

## Completion Notes

Automated integration and manual SMTP acceptance are complete: backend, frontend, build, diff, security
scan, API-doc, HTTP-example, SMTP-operator-doc, FAQ, and browser-route checks
pass. Manual acceptance covered registration delivery and consumption,
password-reset delivery and consumption, login with the new password, and
CAPTCHA escalation after repeated failures. No real sender address, recipient
address, credential, or verification code is committed.
