# Payment Upgrade Main-Agent Launch Prompt

```text
Execute F2 payment gateway and refund consistency upgrade in:
E:\Dev\Projects\Youyu-payment-upgrade

Use Goal mode when available. Read AGENTS.md, CLAUDE.md, docs/README.md,
docs/04-standards/development-process.md, the current roadmaps, and all active
payment-upgrade task documents before editing. Work only in the existing
codex/payment-upgrade worktree. Do not merge or push unless explicitly asked.

Repository truth:
- Base branch was created from master at 5632356 after origin/master sync.
- Mock payment currently exists and must remain usable for local/test flows.
- Alipay sandbox is opt-in and credentials must come only from environment
  variables.
- The latest master baseline has two unrelated SupportChatTest failures.
- Frontend dependencies must be installed with npm ci before frontend checks.

Freeze these interfaces:
- preserve POST /api/payments/orders/{orderId}/initiate
- preserve POST /api/payments/{paymentNo}/mock-success for local/test mode
- preserve order-level paymentStatus values: unpaid, paid, refunding, refunded
- preserve the current order main statuses and fulfillment transitions
- refund requests remain owned by the order domain; gateway execution and
  record consistency belong to the payment domain
- all schema changes are additive; never commit secrets

Dispatch Wave 1 worker-backend-gateway for
payment-upgrade-gateway-foundation.md.

After the gateway contract stabilizes, dispatch Wave 2 workers in parallel:
- worker-backend-refund for payment-upgrade-refund-consistency.md
- worker-frontend-payment for payment-upgrade-frontend-experience.md

Keep payment-upgrade-verification-and-docs.md with the main agent. Review every
worker diff before acceptance. Workers must not commit, revert others' edits,
archive tasks, or broaden scope.

Forbidden expansion:
- commercial Alipay launch
- WeChat Pay
- split payments, escrow, settlement, reconciliation jobs
- customer-service, mediation, marketing, or unrelated order redesign

Final report:
1. child task status
2. commit SHA
3. commands and results
4. baseline warnings
5. blockers
6. deferred work
```
