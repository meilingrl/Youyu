# Wave 1 Support And After-Sales Execution Specification

## Summary

> Status: completed on 2026-06-01 and archived for traceability.

Wave 1 is the next governed multi-agent round for `Youyu`. It intentionally covers:

- dual-workspace admin support synchronization
- deeper refund / after-sales visibility with report-backed mediation handoff
- bounded in-app notification closeout for support, refund, and mediation events

This wave does not claim payment gateway replacement, SLA automation, global notification design, analytics, or personalization work.

## Locked User Decisions

- `/admin/support` keeps one route but must separate online-CS chat and support-ticket handling.
- Refund work goes deeper on history/progress/evidence and admin/user continuity, but stops short of payment-upgrade work.
- Notifications are included only through the existing in-app user notification system.
- Mediation handoff may be one-click only through the existing report-backed mediation lane.
- User-visible capabilities that require platform action must have a corresponding admin handling path.

## Task Documents

| Task | Wave | Owner | Core delivery |
| --- | ---: | --- | --- |
| `wave1-support-and-aftersales-parent` | all | main agent | status, boundaries, acceptance |
| `wave1-scope-lock-and-owner-sync` | 1 | main agent | owner matrix, handoff rules, deferred list |
| `wave1-support-dual-workspace-sync` | 1A | worker A | `/admin/support` dual workspace with explicit chat/ticket ownership |
| `wave1-refund-assistance-and-mediation-handoff` | 1A | worker B | refund visibility, user/admin trail, report-backed mediation handoff |
| `wave1-support-refund-mediation-notification-closeout` | 1B | worker B or main agent | bounded notification coverage after 1A stabilizes |
| `wave1-integration-and-doc-closeout` | final | main agent | integration, verification, docs, archival |

## Locked Interfaces

- `/admin/support` remains the single admin route.
- `/admin/support` must clearly present:
  - online CS chat powered by `/api/admin/support/chat/**`
  - support tickets powered by `/api/admin/support/tickets/**`
- `/app/support` remains the durable support-ticket route.
- user support chat remains in the chat/message-center lane.
- mediation entry remains report-backed:
  - `POST /api/admin/reports/{reportId}/escalate-to-mediation`
  - `/api/admin/mediation-cases/**`
- no Wave 1 slice may create a new direct `order -> mediation case` contract.
- notification closeout may use only the existing in-app notification surface and real owner-page links.

## Wave Order

### Wave 1A

Run in parallel:

- `wave1-support-dual-workspace-sync`
- `wave1-refund-assistance-and-mediation-handoff`

Reason:

- they touch different primary truths and can proceed in parallel once owner boundaries are frozen.
- the notification slice depends on accepted state changes and destination pages from both lanes.

### Wave 1B

Run after 1A review:

- `wave1-support-refund-mediation-notification-closeout`

Reason:

- notification events should follow accepted support/refund/mediation state transitions rather than invent them.

## Test Plan

- backend targeted tests for support/order/mediation/notification slices
- touched frontend tests
- frontend build
- `git diff --check`
- manual acceptance of:
  - user support ticket -> admin handling path
  - admin support chat and ticket lane separation
  - refund-visible user/admin continuity
  - report-backed mediation handoff
  - notification entries linking to real pages

## Explicitly Deferred

- payment gateway abstraction, callback redesign, provider sandbox work
- direct mediation creation without the report-backed path
- support SLA automation, auto-assignment, or workload balancing
- admin notification inboxes or notification template systems
- notification preference expansion
- analytics or personalization work

## Main-Agent Launch Prompt

```text
Execute Wave 1 support and after-sales synchronization in E:\Dev\Projects\Youyu. Read AGENTS.md, CLAUDE.md, docs/README.md, development-process.md, the current roadmaps, and the Wave 1 task files before editing.

Confirm the base branch/worktree state first. Freeze these interfaces before dispatch:
- /admin/support remains one route
- /admin/support must separate online CS chat (/api/admin/support/chat/**) from support tickets (/api/admin/support/tickets/**)
- /app/support remains the durable support-ticket route
- user support chat remains in the chat/message-center lane
- mediation handoff must reuse POST /api/admin/reports/{reportId}/escalate-to-mediation and existing /api/admin/mediation-cases/**
- no direct order->mediation contract may be added
- notification closeout may use only the existing in-app notification lane
- any user-visible capability requiring platform action must have a corresponding admin handling path

Dispatch Wave 1A in parallel:
- worker A: wave1-support-dual-workspace-sync
- worker B: wave1-refund-assistance-and-mediation-handoff

After review of 1A, dispatch Wave 1B:
- worker B or main agent: wave1-support-refund-mediation-notification-closeout

Tell every worker:
- do not revert others' edits
- do not commit
- respect the locked interfaces above
- report changed files, checks run, findings, and blockers

Keep final integration, changelog, HTTP/API doc closeout, task archival, and verification with the main agent.
```

## Worker Prompt A

```text
Implement child task wave1-support-dual-workspace-sync in the assigned worktree.

Ownership:
- You may edit: admin support frontend files, directly related frontend tests, and directly related support/chat docs if needed for accepted UI truth
- Do not edit: order/refund backend logic, mediation decision logic, payment gateway logic, notification systems beyond the support page's direct needs, unrelated roadmap/task files

Locked interfaces:
- /admin/support stays one route
- /admin/support must expose online CS chat and support-ticket handling as distinct workspaces
- chat lane uses /api/admin/support/chat/**
- ticket lane uses /api/admin/support/tickets/**
- /app/support remains the durable support-ticket route

Out of scope:
- replacing chat or ticket backend contracts
- SLA automation
- admin participation in buyer/seller chat outside the accepted online-CS lane

Other agents are working concurrently. Do not revert their edits. Do not commit. In your final response list changed files, checks run, findings, and blockers.
```

## Worker Prompt B

```text
Implement child task wave1-refund-assistance-and-mediation-handoff in the assigned worktree.

Ownership:
- You may edit: order/refund/mediation related backend files, directly related user/admin frontend files, related tests, and directly related docs
- Do not edit: admin support page structure unless a narrow handoff hook requires it, payment gateway/provider redesign, unrelated roadmap/task files

Locked interfaces:
- refund work must deepen progress/history/evidence visibility without becoming payment-upgrade work
- mediation handoff must reuse the existing report-backed path
- no direct order->mediation contract may be added
- any user-visible after-sales capability requiring platform action must have a matching admin handling path

Out of scope:
- payment callbacks, provider sandbox integration, idempotency redesign for payment gateways
- final mediation decision redesign
- global notification system redesign

Other agents are working concurrently. Do not revert their edits. Do not commit. In your final response list changed files, checks run, findings, and blockers.
```
