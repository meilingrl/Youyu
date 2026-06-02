# Task: Wave 1 Scope Lock And Owner Synchronization

## Metadata

- ID: wave1-scope-lock-and-owner-sync
- Status: completed
- Owner: main-agent
- Track: cross-cutting
- Depends on: `wave1-support-and-aftersales-parent`
- Priority: high
- Planned date: 2026-06-01
- Completed date: 2026-06-01

## Objective

Freeze the Wave 1 ownership model so delegated work does not merge online support chat, support tickets, refunds, mediation, and notifications into conflicting truths.

## Background

Live repository truth currently contains both:

- a durable support-ticket module under `/api/support/tickets/**` and `/api/admin/support/tickets/**`
- an accepted online customer-service chat lane under `/api/chat/**` and `/api/admin/support/chat/**`

At the same time, some requirements and older support-console boundary docs still describe `/admin/support` as if chat were excluded. Wave 1 must reconcile that operationally before implementation tasks branch out.

## Scope

- record the final accepted Wave 1 owner matrix
- document the exact admin/user synchronization rule for platform-handled flows
- define allowed mediation escalation entry points for Wave 1
- define what notification closeout means in this wave

## Out of Scope

- implementing the child slices
- changing payment-provider architecture
- re-scoping the chat MVP or mediation v1 ownership

## Files to Read

- `docs/02-requirements/customer-service-ticket-scope.md`
- `docs/02-requirements/admin-support-console-scope.md`
- `docs/02-requirements/platform-mediation-scope.md`
- `docs/07-decisions/2026-05-30-online-customer-service-on-chat.md`
- `docs/09-api-spec/support.md`
- `docs/09-api-spec/chat.md`
- `docs/09-api-spec/order.md`

## Locked Decisions

1. `/admin/support` is a dual-workspace route, not a single-owner module.
2. User support tickets and user support chat remain separate entry paths.
3. Refund deepening may add visibility, evidence, and handoff data, but it must not become payment-gateway work.
4. Mediation handoff in Wave 1 must reuse the existing report-backed mediation API. No new direct `order -> mediation case` creation contract is allowed in this wave.
5. Notification closeout in Wave 1 is limited to existing in-app notification surfaces and event coverage for support/refund/mediation changes.

## Acceptance Criteria

- [x] Child-task prompts can reference a stable owner matrix without ambiguity.
- [x] Wave 1 explicitly states how user-side actions are matched by admin-side handling.
- [x] Mediation handoff constraints are frozen before worker dispatch.

## Completion Notes

- Locked the dual-workspace `/admin/support` model before implementation.
- Locked report-backed mediation escalation as the only accepted Wave 1 handoff path.
- Recorded the user/admin synchronization rule that any user-visible platform action must have a real admin handling lane.
