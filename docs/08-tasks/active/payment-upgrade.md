# Task: Payment Gateway And Refund Consistency Upgrade

## Metadata

- ID: F2-payment-upgrade
- Status: active
- Owner: main-agent
- Track: cross-cutting
- Depends on: current order, payment, refund, notification, and launch-foundation baselines
- Priority: P1
- Planned date: 2026-05-31
- Completed date:

## Objective

Upgrade the internal payment flow into a replaceable payment-gateway design with
an optional Alipay sandbox adapter, explicit failure/cancellation/timeout
behavior, and refund consistency without replacing the existing order state
machine.

## Background

The current payment module already creates durable payment records and advances
orders after an authenticated local mock-success endpoint. It does not yet
model provider callbacks, signature verification, payment method selection,
failed attempts, cancellations, timeout closure, or gateway-backed refunds.

This wave targets a course/demo-ready sandbox integration. It does not claim
commercial payment readiness.

## Scope

- Keep mock payment available for local development and automated tests.
- Add an optional Alipay sandbox gateway adapter.
- Add gateway routing by payment method.
- Preserve existing order statuses and fulfillment transitions.
- Model payment failure, cancellation, timeout, retry, and callback replay.
- Route refund completion through the payment gateway contract.
- Update the buyer payment page so internal mock/test language is not exposed.
- Update API specs, HTTP smoke assets, configuration docs, and changelog.

## Out of Scope

- Alipay production credentials or commercial launch.
- WeChat Pay.
- Multi-merchant settlement, escrow, split payments, or reconciliation jobs.
- General launch hardening such as rate limiting, monitoring, or secret-manager
  infrastructure beyond payment-specific configuration documentation.
- Customer-service, mediation, marketing, and unrelated order UX changes.

## Child Tasks

- [x] `../archived/payment-upgrade-gateway-foundation.md`
- [x] `../archived/payment-upgrade-refund-consistency.md`
- [x] `../archived/payment-upgrade-frontend-experience.md`
- [ ] `payment-upgrade-verification-and-docs.md`

## Locked Interfaces

- Preserve `POST /api/payments/orders/{orderId}/initiate`.
- Preserve `POST /api/payments/{paymentNo}/mock-success` for local/test use.
- Preserve order-level `paymentStatus`: `unpaid`, `paid`, `refunding`,
  `refunded`.
- Preserve order main statuses and fulfillment transitions defined in
  `docs/02-requirements/order-lifecycle-and-fulfillment.md`.
- Payment-provider credentials must come from environment variables and must
  never be committed.
- Alipay sandbox is opt-in. Missing sandbox credentials must not break local
  mock development or automated tests.
- Refund requests remain owned by the order domain. Gateway execution and
  payment/refund record consistency belong to the payment domain.

## Risks

- Callback replay or partial failure can desynchronize payment records and
  orders.
- Refund completion currently mutates order state without a gateway operation.
- `TransactionDataStore` currently owns payment/refund SQL despite the empty
  `PaymentRecordMapper`; migration must remain incremental and focused.
- The latest `master` baseline has two unrelated `SupportChatTest` failures.

## Test Plan

- Backend: `.\mvnw.cmd test`
- Frontend: `npm ci`, `npm test`, `npm run build`
- API validation: run payment and order HTTP smoke paths for mock mode and
  sandbox configuration checks.
- Manual: checkout -> initiate -> pay -> callback result -> order detail ->
  refund request -> refund completion.
- Hygiene: `git diff --check`

## Acceptance Criteria

- [x] Mock payment remains available for local/test use.
- [x] Alipay sandbox can be enabled only through environment configuration.
- [x] Failure, cancellation, timeout, retry, and replay cases are covered.
- [x] Refund completion uses the payment gateway contract.
- [x] Existing order and fulfillment transitions remain compatible.
- [x] Buyer-facing pages do not expose mock/test implementation wording.
- [x] Documentation and smoke assets match runtime behavior.
- [x] Synchronous Alipay API responses are signature-verified, either directly
  or through the official SDK client.
- [ ] Every child task is reviewed before archival.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [x] `docs/09-api-spec/payment.md`
- [x] `docs/09-api-spec/order.md` if refund behavior changes
- [x] payment sandbox configuration guide
- [ ] task status and archive move

## Completion Notes

- Implementation slices are complete and reviewed. The total task remains
  active until real Alipay sandbox QR payment, asynchronous callback, replay,
  and refund verification are exercised with local credentials and a public
  HTTPS callback URL.
- Automated verification recorded so far: frontend 44 passing, frontend build
  passing, and `git diff --check` passing. Full backend verification passed
  with 184 tests; one earlier run
  reproduced the unrelated transient `SupportChatTest` same-timestamp ordering
  failure before the repeat passed.
- Interrupted-delivery audit added terminal-success callback protection and
  hides the Alipay method until all required sandbox variables are configured.
  Synchronous Alipay API responses are now verified against the original
  response-object text before provider data is accepted.
- 2026-06-03 follow-up: local mock completion is stable again on pre-existing
  MySQL databases after introducing startup-time payment schema compatibility
  repair for missing additive columns and tables.
- 2026-06-03 acceptance reminder: manual sandbox QR verification must be done
  with the sandbox wallet / sandbox buyer flow rather than the production
  Alipay app.
