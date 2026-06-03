# Task: Payment Upgrade Integration, Verification, And Documentation

## Metadata

- ID: F2-payment-integration-docs
- Status: archived
- Owner: main-agent
- Track: cross-cutting
- Depends on: `payment-upgrade-gateway-foundation.md`,
  `payment-upgrade-refund-consistency.md`, `payment-upgrade-frontend-experience.md`
- Priority: P1
- Planned date: 2026-05-31
- Completed date: 2026-06-03

## Objective

Integrate the accepted payment slices, align documentation and smoke assets,
verify runtime behavior, and close the governed task lifecycle.

## Scope

- Review every worker diff before acceptance.
- Resolve narrow integration issues.
- Update API specs and HTTP smoke files.
- Add a payment sandbox configuration guide.
- Prepend `CHANGELOG.md`.
- Run required backend, frontend, build, smoke, and hygiene checks.
- Archive accepted child tasks and parent task only after verification.

## Out of Scope

- Fixing unrelated customer-service baseline failures unless separately
  approved.
- Commercial payment launch claims.
- Production secrets.

## Allowed Changes

- integration patches in files already owned by accepted child tasks
- `docs/04-standards/payment-sandbox-configuration.md`
- `docs/06-http/payment.http`
- refund-focused parts of `docs/06-http/order.http`
- `docs/09-api-spec/payment.md`
- refund-focused parts of `docs/09-api-spec/order.md`
- `CHANGELOG.md`
- payment task lifecycle files under `docs/08-tasks/`

## Acceptance Criteria

- [x] Worker diffs were reviewed and accepted.
- [x] Payment and refund API docs match runtime behavior.
- [x] Sandbox configuration guide contains no secrets.
- [x] Backend tests, frontend tests, frontend build, and `git diff --check` were
  run with outcomes recorded.
- [x] Unrelated baseline failures remain explicit if still present.
- [x] Parent and child task records are archived only after verification.

## Completion Notes

- Automated integration documentation and hygiene work completed on
  2026-05-31.
- Final automated verification: backend 184 passing, frontend 44 passing,
  frontend build passing, PowerShell launch-script syntax valid, and
  `git diff --check` passing. An earlier backend run reproduced the unrelated
  transient `SupportChatTest` same-timestamp ordering failure before the
  repeat full run passed.
- Remaining acceptance work closed on 2026-06-03 after user-confirmed sandbox
  QR checkout, callback, and refund verification. The task is now archived.
- Interrupted-delivery audit added synchronous Alipay API response signature
  verification against the original response-object text.
- 2026-06-03 follow-up: local mock completion failures on older MySQL databases
  were traced to missing additive payment columns. The fix now runs through
  startup-time compatibility repair (`PaymentSchemaUpgrader`) instead of
  putting `ALTER TABLE` statements into `schema.sql`.
- 2026-06-03 operator note retained for traceability: sandbox QR verification
  must use the Alipay sandbox wallet / sandbox buyer flow rather than the
  regular production Alipay app.
