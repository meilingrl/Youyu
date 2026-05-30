# Goal Prompt: Customer Service Ticket MVP

## Metadata

- ID: customer-service-goal-prompt
- Status: archived
- Owner: head agent
- Track: cross-cutting
- Depends on: `docs/08-tasks/active/customer-service-ticket-mvp.md`
- Priority: high
- Planned date: 2026-05-30
- Completed date: 2026-05-30

## Completion

Completed and archived after the customer-service ticket MVP passed backend, frontend, build, and diff checks on 2026-05-30.

## Prompt

You are the head Agent for the Youyu customer-service ticket MVP.

Worktree:

- `E:\Dev\Projects\Youyu-admin-module-goal`

Branch:

- `codex/admin-module-goal`

Primary task:

- `docs/08-tasks/active/customer-service-ticket-mvp.md`

Requirement boundary:

- `docs/02-requirements/customer-service-ticket-scope.md`

## Required Read Order

1. `AGENTS.md`
2. `CLAUDE.md`
3. `docs/README.md`
4. `docs/04-standards/development-process.md`
5. `docs/05-roadmap/current/admin-module-goal-roadmap.md`
6. `docs/02-requirements/customer-service-ticket-scope.md`
7. `docs/02-requirements/admin-support-console-scope.md`
8. `docs/02-requirements/chat-mvp-scope.md`
9. `docs/02-requirements/platform-mediation-scope.md`
10. `docs/08-tasks/active/customer-service-ticket-mvp.md`
11. Relevant frontend/backend module docs

## Hard Constraints

- Do not move or rewrite `CLAUDE.md`.
- Do not use archived task docs as current execution specs.
- Do not reintroduce `AdminDataStore`.
- Do not implement support tickets as buyer/seller chat.
- Do not call `/api/chat/**` from the admin support-ticket UI.
- Do not let support-ticket status mutate order, refund, report, product, shop, user, or mediation state.
- Keep schema changes additive and limited to the SQL authorized in the task.
- Preserve unrelated uncommitted admin-workbench UX changes.

## Dispatch Plan

Use separate agents with disjoint write scopes:

1. Backend worker:
   - owns schema, seed, support mapper/service/controller, backend tests, and backend support API docs.
2. Frontend worker:
   - owns frontend support API module, `/app/support`, `/admin/support`, and route updates.
3. Documentation/review worker:
   - owns HTTP smoke files, API spec consistency review, changelog draft, and acceptance checklist.

Each worker must return:

- changed file list;
- implementation summary;
- verification commands and results;
- acceptance checklist status;
- unresolved risks.

## Acceptance

The head Agent must inspect diffs, run or review:

- `backend`: `.\mvnw.cmd test`
- `frontend`: `npm test`
- `frontend`: `npm run build`

After passing validation:

- fill completion notes;
- move the task and prompt to `docs/08-tasks/archived/`;
- prepend `CHANGELOG.md`;
- commit and push.
