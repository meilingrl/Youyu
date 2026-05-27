# Task: Admin Support Console Contract Definition

## Metadata

- ID: admin-support-console-contract-definition
- Status: archived
- Owner: unassigned
- Track: cross-cutting
- Depends on: current `/admin/support` placeholder; current chat/message backend baseline; current report/order/refund governance baseline
- Priority: medium
- Planned date: 2026-05-27
- Completed date: 2026-05-27

## Objective

Turn the existing `/admin/support` placeholder into an executable contract definition for a future support console, without building the support-console implementation in this task.

## Background / Current State

The frontend already exposes `/admin/support` through the admin navigation. `frontend/src/views/admin/SupportView.vue` is intentionally a reserved-state page: it names support conversations, after-sales assistance, group governance, and abnormal messages, but it does not call real APIs or display live queues.

The codebase now has real chat and notification modules, report governance, admin order/refund operations, and a documented admin API surface. However, there is no formal support-console contract that defines which existing data source owns each support lane, which endpoints a future page may call, and which lanes are still out of scope.

## Problem Statement

The current support page is useful as a navigation placeholder, but it is too open-ended for a sub-agent to implement safely. A later implementation could accidentally create a second report system, duplicate mediation scope, or invent support-ticket data structures without a documented boundary.

## Scope

- Audit the current support-related backend and frontend baseline:
  - chat conversation/message APIs
  - notification APIs if relevant
  - admin order/refund operations
  - report submit/process governance
  - `/admin/support` frontend placeholder
- Define the support console v1 boundary as a requirements document.
- Create one execution-ready implementation task for the first support-console slice if the boundary is clear.
- Keep this task documentation-only.

## Out of Scope

- Writing backend or frontend support-console implementation code.
- Creating support-ticket, mediation-case, or group-chat tables.
- Changing chat, report, order, refund, or notification endpoint behavior.
- Adding buyer-facing mediation UI.
- Updating `schema.sql` or seed data.

## Implementation Plan

1. Read the current support, chat, report, and order/admin code and formal API specs.
2. Decide which v1 support-console lane is implementable from existing data without new schema.
   - Default candidate: support context dashboard that links to existing reports, admin orders, and chat conversations.
   - Do not select mediation as v1 unless `docs/02-requirements/platform-mediation-scope.md` exists.
3. Create `docs/02-requirements/admin-support-console-scope.md`.
4. If an implementation slice is safe, create `docs/08-tasks/active/admin-support-console-implementation.md`.
5. Update `docs/09-api-spec/admin.md` only if the requirements document identifies existing admin endpoints that need clearer support-console ownership notes.
6. Prepend `CHANGELOG.md`.
7. Fill Completion Notes and archive this task only after the documentation artifacts are complete.

## File Scope

### Must Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/04-standards/development-process.md`
- `docs/05-roadmap/current/feature-roadmap.md`
- `docs/05-roadmap/current/open-questions.md`
- `docs/02-requirements/communication-and-after-sales-boundary.md`
- `docs/09-api-spec/admin.md`
- `docs/09-api-spec/chat.md`
- `docs/09-api-spec/order.md`
- `docs/09-api-spec/report.md`
- `docs/06-http/admin.http`
- `docs/06-http/chat.http`
- `docs/06-http/order.http`
- `docs/06-http/report.http`
- `frontend/src/views/admin/SupportView.vue`
- `frontend/src/router/modules/admin.js`
- `frontend/src/constants/navigation.js`
- `backend/src/main/java/com/youyu/backend/controller/chat/ChatController.java`
- `backend/src/main/java/com/youyu/backend/controller/order/AdminOrderController.java`
- `backend/src/main/java/com/youyu/backend/controller/admin/AdminController.java`
- `backend/src/main/java/com/youyu/backend/controller/report/ReportController.java`

### Allowed Changes

- `docs/02-requirements/admin-support-console-scope.md` (new)
- `docs/08-tasks/active/admin-support-console-implementation.md` (new, only if scope is implementation-ready)
- `docs/09-api-spec/admin.md` only for ownership notes if needed
- `CHANGELOG.md`
- This task document and its archive move

### Not Allowed

- `backend/src/**`
- `frontend/src/**`
- `backend/src/main/resources/schema.sql`
- Seed SQL files
- `CLAUDE.md`
- Existing ADR rewrites
- Archived task rewrites

## API / Data Contract Impact

This task should not change runtime API contracts. It may document an intended future support-console API slice, but any implementation endpoint additions must be deferred to the generated implementation task.

If the scope document determines that existing endpoints are enough for v1, list them explicitly with owner module, method/path, auth, response dependency, and limitation.

## Risks

- Mistaking `/admin/support` for permission to implement a full customer-service system.
- Duplicating report or mediation responsibilities.
- Depending on a missing platform mediation scope document.
- Treating chat P2 implementation as a three-party support workflow when current chat entities may still be peer-to-peer.

## Verification Plan

- Backend: not required.
- Frontend: not required.
- API validation: not required.
- Manual documentation checks:
  - `admin-support-console-scope.md` names each lane and its owner module.
  - Any generated implementation task includes complete File Scope, API/Data Contract Impact, Verification Plan, Acceptance Criteria, and Sub-agent Instructions.
  - No application code changed.

## Acceptance Criteria

- [x] `docs/02-requirements/admin-support-console-scope.md` exists.
- [x] The scope document clearly separates support console v1 from report processing, platform mediation, and chat MVP/P2 features.
- [x] The scope document lists existing reusable endpoints and explicitly marks missing endpoints.
- [x] If `admin-support-console-implementation.md` is created, it is executable by a sub-agent without additional product-design decisions.
- [x] No backend, frontend, schema, or seed file is modified.
- [x] `CHANGELOG.md` has a new top entry for the documentation change.
- [x] This task is archived only after the docs are complete.

## Sub-agent Instructions

You are executing a documentation-only task in the `codex/admin-module-goal` worktree.

1. Read `AGENTS.md`, `CLAUDE.md`, `docs/README.md`, and this task document first.
2. Stay documentation-only. Do not edit `backend/src/**`, `frontend/src/**`, schema, or seed files.
3. Do not move or rewrite `CLAUDE.md`.
4. Do not reintroduce `AdminDataStore` or any persistent in-memory business store.
5. Use current code and API specs as runtime truth when documentation conflicts.
6. If platform mediation scope is missing, keep mediation out of the support-console v1 implementation task and document it as blocked.
7. Return:
   - modified file list
   - scope decisions made
   - verification checks performed
   - unresolved risks or questions for the head Agent

## Completion Notes

- Created `docs/02-requirements/admin-support-console-scope.md` with v1 support-console boundaries, lane ownership, reusable endpoints, missing endpoint inventory, non-goals, and explicit mediation blockage.
- Created `docs/08-tasks/active/admin-support-console-implementation.md` because the v1 support context dashboard is implementation-ready using existing frontend admin route structure and existing admin/report/order/search endpoints, without backend or schema changes.
- Updated `docs/09-api-spec/admin.md` with an ownership note clarifying that `/admin/support` is a frontend route and no `/api/admin/support/**` namespace currently exists.
- Prepended `CHANGELOG.md` with the documentation change.
- Verified platform mediation remains blocked because `docs/02-requirements/platform-mediation-scope.md` is absent; `docs/02-requirements/chat-mvp-scope.md` is also absent in this worktree.
- No backend, frontend, schema, seed, or `CLAUDE.md` files were modified.

Head-Agent acceptance:

- Reviewed `docs/02-requirements/admin-support-console-scope.md` and confirmed v1 is limited to an admin support context dashboard.
- Reviewed `docs/08-tasks/active/admin-support-console-implementation.md` and confirmed it is frontend-only, uses existing admin-owned APIs, and keeps missing mediation/chat/group lanes blocked.
- Reviewed `docs/09-api-spec/admin.md` ownership note and `CHANGELOG.md`.
- Ran `git diff --check`: passed.
