# Task: Wave 1 Support And After-Sales Synchronization

## Metadata

- ID: wave1-support-and-aftersales-parent
- Status: completed
- Owner: main-agent
- Track: cross-cutting
- Depends on: Wave 0 completion, current roadmap truth, locked Wave 1 decisions on 2026-06-01
- Priority: high
- Planned date: 2026-06-01
- Completed date: 2026-06-01

## Objective

Run a governed multi-agent Wave 1 that deepens support, after-sales, and user/admin operational continuity without expanding into payment gateway replacement, analytics, or full personalization work.

## Background

The user confirmed these Wave 1 decisions:

- `/admin/support` must keep one route but expose two distinct owner workspaces:
  - online customer-service chat
  - durable support-ticket queue
- Wave 1 refund work should deepen visible progress, negotiation record, evidence, and admin/user traceability, but must not absorb the payment-upgrade phase.
- Existing message-center notifications should absorb support/refund/mediation events, but notification preferences, templates, and orchestration remain deferred.
- Support/refund flows may escalate into the existing mediation lane, but mediation ownership and final decisions remain with the formal mediation module.

An additional acceptance rule from the user is now explicit:

- user-facing capabilities must have a corresponding admin-side handling path where the business flow requires platform action.

## Scope

- freeze Wave 1 ownership boundaries and synced user/admin acceptance rules before delegation
- align `/admin/support` to the dual-workspace model without collapsing chat and ticket ownership
- deepen refund assistance and mediation handoff visibility across user and admin surfaces
- close the most important support/refund/mediation notification gaps in the existing message center
- keep final integration, verification, changelog, and task archival with the main agent

## Out of Scope

- payment gateway replacement, callback redesign, or refund-provider reconciliation
- notification preference expansion, notification templates, or admin notification composition
- SLA automation, workload balancing, or auto-assignment
- admin participation in buyer/seller chat outside the accepted online-CS support flow
- analytics dashboards, metrics collection, or recommendation/personalization work

## Child Tasks

- [x] `wave1-scope-lock-and-owner-sync`
- [x] `wave1-support-dual-workspace-sync`
- [x] `wave1-refund-assistance-and-mediation-handoff`
- [x] `wave1-support-refund-mediation-notification-closeout`
- [x] `wave1-integration-and-doc-closeout`

## Locked Interfaces

- `/admin/support` remains the single admin route, but it must clearly separate:
  - online customer-service chat backed by `/api/admin/support/chat/**`
  - support-ticket queue/detail backed by `/api/admin/support/tickets/**`
- `/app/support` remains the durable support-ticket user entry.
- user support chat remains in the message-center/chat lane, not the ticket lane.
- no Wave 1 slice may make support tickets own refund state, report state, or mediation decisions.
- mediation remains owned by:
  - `POST /api/admin/reports/{reportId}/escalate-to-mediation`
  - `/api/admin/mediation-cases/**`
- if a user-facing action requires admin follow-up, the admin-side workspace must expose a real handling path or an explicit blocked state.

## Acceptance Criteria

- [x] Every child task is reviewed by the main agent before archival.
- [x] User-facing support/refund paths have a corresponding admin-side operational owner where platform action is required.
- [x] `/admin/support` no longer hides the ticket/chat ownership split behind a single misleading surface.
- [x] Refund and mediation-related user/admin flows expose progress and handoff states consistently enough for manual acceptance.
- [x] Deferred payment-upgrade and notification-expansion work remains explicit after the round.

## Completion Notes

- Wave 1 completed in one governed round on 2026-06-01.
- `/admin/support` now exposes separated online-CS and support-ticket workspaces on one route.
- Buyer/admin order detail views now share explicit after-sales visibility around refunds, linked reports, and report-backed mediation.
- Existing in-app notifications now cover Wave 1 support-ticket and mediation updates without expanding into notification preferences or template systems.
