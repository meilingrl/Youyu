# Task: Admin Governance Action Consistency

## Metadata

- ID: admin-governance-action-consistency
- Status: active
- Owner: unassigned
- Track: cross-cutting
- Depends on: current admin governance baseline; archived `admin-governance-ui-consistency`; archived `admin-query-pagination-hardening`
- Priority: high
- Planned date: 2026-05-27
- Completed date:

## Objective

Make admin governance actions across verification, report, shop, product-review, product-status, and user-status surfaces consistent, state-aware, and test-covered without inventing unsupported business states.

## Background / Current State

The admin module already has a broad working baseline:

- Frontend routes under `/admin/*`: dashboard, users, verifications, products, review-tasks, shops, orders, reports, hot-search, and support.
- Frontend admin pages use `ListPageShell`, paginated admin list responses, shared error handling, and Element Plus table-driven management pages.
- Backend admin governance endpoints exist under `/api/admin` and admin order operations exist under `/api/admin/orders`.
- `docs/09-api-spec/admin.md` and `docs/06-http/admin.http` cover most current `/api/admin` governance endpoints.
- `backend/src/test/java/com/youyu/backend/admin/AdminGovernanceTest.java` covers auth guards, dashboard, user list/status, verification review, product status, shop status, and search-governance CRUD.

Current action behavior is not yet equally constrained across surfaces:

- Verification review and review-task review support explicit `approve` / `reject` action semantics and require reject reasons for rejection.
- Shop review/status updates mix `status` and `reviewStatus`, but the backend does not consistently record the reviewer when called through `AdminServiceImpl.updateShopStatus`.
- Report processing accepts arbitrary `status` strings as long as the field is non-blank.
- Product and user status updates accept non-blank strings with limited or no allow-list enforcement.
- Frontend pages show raw status values and action buttons based on local page conditions, so terminal or invalid state behavior is hard to audit consistently.

## Problem Statement

Admin operators can see similar governance objects with different action vocabulary and state restrictions, while some backend endpoints still accept unsupported states. This makes sub-agent work risky because a UI-only alignment could leave invalid backend transitions open, while a backend-only fix could break existing frontend assumptions without documenting the intended action model.

## Scope

- Inventory and normalize the intended action policy for these admin surfaces:
  - student verification review
  - product review tasks
  - shop approval/rejection/disable flows
  - report processing
  - product status changes
  - user status changes
- Add the minimum backend validation needed to reject unsupported statuses/actions.
- Add or adjust frontend action labels, visibility, and disabled/empty-state explanations so every visible action maps to a documented backend state transition.
- Extend backend tests for allowed and rejected state transitions.
- Update admin API spec and HTTP smoke requests if request semantics or error expectations become more explicit.

## Out of Scope

- Redesigning the whole admin area.
- Adding new governance workflows, appeal flows, mediation cases, or support tickets.
- Adding new database tables or schema columns.
- Reworking admin pagination, list shells, or bundle-splitting behavior.
- Changing public user-facing routes outside what is strictly needed to preserve an admin action contract.

## Implementation Plan

1. Build a state/action matrix from current code before editing.
   - Record current frontend button conditions from the scoped admin views.
   - Record current backend accepted values and validation gaps in `AdminServiceImpl`.
2. Tighten backend state validation first.
   - Introduce local allow-list validation in the existing admin service layer or a small package-private helper if duplication becomes meaningful.
   - Preserve current supported values; reject only clearly unsupported values.
   - Keep error responses aligned with existing `BusinessException(ResultCode.BAD_REQUEST, ...)` behavior.
3. Align frontend actions with the backend-supported matrix.
   - Keep the existing page structure and `ListPageShell`.
   - Prefer small local helpers or constants in scoped admin files; only add a shared helper if at least three pages need the same mapping.
   - Add explanatory text only where an empty action column would otherwise be ambiguous.
4. Update tests and contracts.
   - Add backend tests for invalid report, product, user, and shop state transitions.
   - Add or adjust frontend tests only if a shared frontend helper is introduced.
   - Update `docs/09-api-spec/admin.md` and `docs/06-http/admin.http` if accepted values or failure examples are clarified.
5. Complete task lifecycle updates.
   - Prepend `CHANGELOG.md`.
   - Fill Completion Notes.
   - Move this task to `docs/08-tasks/archived/` only after validation passes.

## File Scope

### Must Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `frontend/README.md`
- `backend/README.md`
- `docs/09-api-spec/admin.md`
- `docs/06-http/admin.http`
- `frontend/src/api/modules/admin.js`
- `frontend/src/views/admin/VerificationManageView.vue`
- `frontend/src/views/admin/ReviewTaskManageView.vue`
- `frontend/src/views/admin/ShopManageView.vue`
- `frontend/src/views/admin/ReportManageView.vue`
- `frontend/src/views/admin/ProductManageView.vue`
- `frontend/src/views/admin/UserManageView.vue`
- `backend/src/main/java/com/youyu/backend/controller/admin/AdminController.java`
- `backend/src/main/java/com/youyu/backend/controller/admin/dto/ReviewVerificationRequest.java`
- `backend/src/main/java/com/youyu/backend/controller/admin/dto/UpdateUserStatusRequest.java`
- `backend/src/main/java/com/youyu/backend/service/admin/AdminService.java`
- `backend/src/main/java/com/youyu/backend/service/admin/impl/AdminServiceImpl.java`
- `backend/src/test/java/com/youyu/backend/admin/AdminGovernanceTest.java`

### Allowed Changes

- Scoped admin frontend views listed above.
- `frontend/src/api/modules/admin.js` only if request payload shape or helper naming changes.
- `backend/src/main/java/com/youyu/backend/controller/admin/**`
- `backend/src/main/java/com/youyu/backend/service/admin/**`
- DTO files under `backend/src/main/java/com/youyu/backend/controller/admin/dto/` if typed request validation is introduced.
- `backend/src/test/java/com/youyu/backend/admin/AdminGovernanceTest.java`
- `docs/06-http/admin.http`
- `docs/09-api-spec/admin.md`
- `CHANGELOG.md`
- This task document and its archive move.

### Not Allowed

- `backend/src/main/resources/schema.sql`
- Seed SQL files
- Non-admin frontend views
- `CLAUDE.md`
- Archived task documents except for reading context
- Any new `AdminDataStore` or other persistent in-memory business store

## API / Data Contract Impact

Expected contract impact is narrow:

- No endpoint path changes.
- No response envelope changes.
- Existing accepted values should remain accepted.
- Unsupported state/action values should become explicit `BAD_REQUEST` business errors where they are currently accepted accidentally.
- If accepted values are clarified, update `docs/09-api-spec/admin.md` field tables and add negative smoke examples to `docs/06-http/admin.http`.

No database schema change is expected.

## Risks

- Accidentally blocking a state value currently used by seed data or frontend filters.
- Flattening genuinely different workflows into fake uniformity.
- Creating a broad shared abstraction before the real duplication is proven.
- Updating frontend labels without backend enforcement, leaving invalid transitions reachable through direct API calls.

## Verification Plan

- Backend:
  - From `backend/`, run `.\mvnw.cmd -Dtest=AdminGovernanceTest test`.
  - From `backend/`, run `.\mvnw.cmd test` before completion.
- Frontend:
  - From `frontend/`, run `npm test`.
  - From `frontend/`, run `npm run build`.
- API validation:
  - Review `docs/06-http/admin.http` examples against the final accepted action/status matrix.
- Manual:
  - Open or inspect each scoped admin page and confirm visible actions match the accepted state matrix.
  - Confirm terminal states show no misleading action or have a clear explanation where needed.

## Acceptance Criteria

- [ ] A state/action matrix is recorded in Completion Notes or an implementation summary before archiving.
- [ ] Backend rejects unsupported report statuses, product statuses, user statuses, shop status/reviewStatus combinations, verification actions, and review-task actions with clear `BAD_REQUEST` messages.
- [ ] Existing valid admin actions continue to work.
- [ ] Frontend action buttons on the scoped admin pages map only to backend-supported transitions.
- [ ] Terminal or non-actionable rows are understandable instead of silently inconsistent.
- [ ] Backend tests cover at least one invalid transition for report, product, user, and shop governance.
- [ ] `docs/09-api-spec/admin.md` and `docs/06-http/admin.http` are updated if accepted values or failure examples are clarified.
- [ ] `CHANGELOG.md` has a new top entry for the delivered change.
- [ ] Verification commands listed above are run and recorded.
- [ ] This task is archived only after all applicable criteria pass.

## Sub-agent Instructions

You are implementing this task in the `codex/admin-module-goal` worktree.

1. Read `AGENTS.md`, `CLAUDE.md`, `docs/README.md`, and this task document first.
2. Stay inside the File Scope. Do not modify unrelated files.
3. Do not move or rewrite `CLAUDE.md`.
4. Do not reintroduce `AdminDataStore` or any new persistent in-memory business store.
5. Start by writing down the current state/action matrix in your working notes. Use that matrix to keep frontend and backend changes aligned.
6. Prefer minimal service-layer validation and scoped frontend updates over a broad admin redesign.
7. Run the Verification Plan. If a required command cannot be run, report the exact blocker.
8. Return:
   - modified file list
   - implementation summary
   - validation commands and results
   - acceptance-criteria checklist
   - unresolved risks or decisions needed from the head Agent

## Completion Notes

(Filled in after implementation and head-Agent acceptance.)
