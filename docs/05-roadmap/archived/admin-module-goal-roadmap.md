# Admin Module Goal Roadmap

> Archived on 2026-06-03 after the admin module sequence completed and stopped
> serving as a current execution roadmap.

## Purpose

This roadmap guides the next long-running head Agent for the Youyu admin module.

It is a module-specific execution roadmap. It does not replace `stage-roadmap.md`, `feature-roadmap.md`, or the task lifecycle under `docs/08-tasks/`.

## Current Baseline

- The active worktree is expected to be `E:\Dev\Projects\Youyu-admin-module-goal`.
- The active branch is expected to be `codex/admin-module-goal`.
- The existing admin surface already covers dashboard, users, verifications, products, review tasks, shops, orders, reports, hot search governance, and support context.
- `/admin/support` entered this task as a support context dashboard, not a ticketing, mediation, or admin-chat system.
- Customer-service ticket MVP work is active as of 2026-05-30. It adds a new support-ticket module for user-created tickets and admin ticket handling while preserving the existing `/admin/support` context dashboard boundary.
- Platform mediation v1 is implemented from `docs/02-requirements/platform-mediation-scope.md` with report escalation, case workflow, decision records, and read-only related chat context.
- Admin role handling now has backend-enforced staff roles for `SUPER_ADMIN`, `SUPPORT_AGENT`, `REVIEWER`, `OPERATOR`, and `ORDER_ADMIN`, while legacy `ADMIN` remains a full-access compatibility role.
- Full-flow seed coverage now includes role-specific staff accounts, core admin queues, mediation scenarios, audit examples, and `docs/06-http/admin-full-flow.http` for local verification.

## Locked Product Decisions

- Admin users should enter the admin workbench after login. The admin experience is a dedicated left-nav plus detail workspace, not the buyer/seller application shell.
- The current admin role is the seed for a future `super_admin` role.
- Multi-role UI and permission implementation are not part of the first admin-entry slice, but the role model must be specified before implementation begins.
- Support and mediation are related but distinct:
  - support is customer-service ticket handling, context gathering, triage, notes, and operator workflow;
  - mediation is a formal platform decision over a dispute.
- Support tickets may link to orders, products, shops, users, reports, or mediation context, but they do not mutate those owner records.
- Mediation v1 uses report escalation and creates `mediation_cases`.
- `reports` remain accusation/governance records. `mediation_cases` own formal dispute handling and decision records.
- Mediation v1 may provide read-only access to related buyer/seller chat context. Admins must not join or send messages into user chat in v1.
- Seed data should be added incrementally with each feature slice, then consolidated in a final full-flow seed task.

## Role Model Direction

The first permission model should use this five-role minimum set:

| Role | Purpose | Default access boundary |
|---|---|---|
| `super_admin` | Highest authority and current admin successor | All admin routes, permission management, audit logs, and final mediation decisions |
| `support_agent` | Support triage and mediation assistance | Support console, report/order context, mediation case handling except restricted final-authority actions |
| `reviewer` | Review and moderation specialist | Student verification, product review, review tasks, and related detail context |
| `operator` | Operations and content governance | Hot search governance, content/risk signal context, and operational dashboards |
| `order_admin` | Order and refund operations | Admin orders, refunds, dispute order context, and mediation participation where allowed |

Do not implement role control as frontend menu hiding only. Backend authorization and tests are mandatory before a role-permission task is accepted.

## Recommended Sequence

All items in this sequence are completed and archived as of 2026-05-28.

1. `admin-entry-workbench-navigation`
   - Simplify the admin login destination, dashboard frame, and navigation.
   - Keep a single admin runtime role for now.
2. `chat-mvp-scope-recovery`
   - Restore the missing chat MVP requirement artifact.
   - Unblock mediation boundary work from the missing chat-scope-document blocker.
3. `platform-mediation-boundary-and-contract`
   - Completed boundary definition for support, report, order, refund, chat visibility, and mediation case ownership.
   - Produced implementation task constraints.
4. `platform-mediation-implementation`
   - Implement the mediation v1 workflow from `docs/02-requirements/platform-mediation-scope.md`.
5. `admin-dashboard-observability`
   - Upgrade the admin dashboard into a task and flow monitor using real backend data.
6. `admin-audit-log-foundation`
   - Add durable operator action records for accountability.
7. `admin-role-permission-model`
   - Implement the five-role model with backend enforcement and frontend routing/menu alignment.
8. `seed-full-admin-flow`
   - Consolidate seed data so the full admin journey can be operated and verified locally.

## Head Agent Operating Rules

- Read `AGENTS.md`, `CLAUDE.md`, `docs/README.md`, `docs/04-standards/development-process.md`, this roadmap, and the target task before dispatching work.
- Do not use archived tasks as current execution specs.
- Keep exactly one active source of truth for each rule set or feature boundary.
- Do not move or rewrite `CLAUDE.md`.
- Do not reintroduce `AdminDataStore` as persistent business logic.
- Prefer one task per dispatch unless the file scopes are independent and non-overlapping.
- The head Agent should not personally implement large business-code changes. Its core work is document constraints, dispatch, review, verification, task updates, archiving, and commits.
- If a sub-agent result fails acceptance, update the task with the gap and dispatch another round instead of silently broadening the implementation.

## Completion Expectations

Each implementation task is complete only when:

- code changes are reviewed against the active task;
- relevant backend/frontend/API validations pass;
- docs in `docs/06-http/` and `docs/09-api-spec/` are updated when contracts change;
- `CHANGELOG.md` is prepended for substantive work;
- completion notes are filled;
- the task is moved to `docs/08-tasks/archived/`.

## Tracking

Update this roadmap when the admin module sequence changes, a task becomes blocked/unblocked, or a major product decision changes.
