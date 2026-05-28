# Task: Admin Audit Log Foundation

## Metadata

- ID: admin-audit-log-foundation
- Status: completed
- Owner: Codex
- Track: cross-cutting
- Depends on: current admin governance actions; mediation implementation if mediation actions are included
- Priority: medium
- Planned date: 2026-05-28
- Completed date: 2026-05-28

## Objective

Create a durable foundation for admin operation logs so sensitive backend actions are attributable to a specific operator.

## Background

The admin module already mutates users, products, shops, reports, review tasks, orders, and search governance rules. As roles and mediation grow, accountability becomes a core platform requirement. The first audit slice should capture critical actions without trying to build a full compliance analytics product.

## Scope

- Define and implement a minimum admin audit log model.
- Record operator, action type, target type, target id, reason or summary, and timestamp.
- Cover the highest-risk existing admin actions selected by the task implementer based on code audit.
- Provide a basic admin-only read path if it is required for verification.
- Update seed/test data only as needed.

## Out of Scope

- Full compliance reporting.
- Immutable external log storage.
- Request tracing infrastructure.
- Role-permission implementation unless needed only to protect audit reads.
- Exhaustive coverage of every admin endpoint in the first slice.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/05-roadmap/current/admin-module-goal-roadmap.md`
- backend admin controllers/services for current mutations
- backend auth context files
- `backend/src/main/resources/schema.sql`
- `docs/09-api-spec/admin.md`
- `docs/06-http/admin.http`

## Allowed Changes

- Backend schema, entity/mapper/service/controller/test files needed for audit logs.
- Scoped frontend admin audit view only if required by the accepted implementation path.
- API spec and HTTP docs for audit endpoints.
- Seed data if needed.
- `CHANGELOG.md`.
- This task document lifecycle updates.

## Implementation Plan

1. Identify high-risk admin mutations to cover first.
2. Add a minimum durable audit record path.
3. Write audit records from selected admin actions.
4. Add verification for successful audit writes and admin-only read access if a read endpoint exists.
5. Update docs, tests, and completion notes.

## File Scope

This task may touch cross-cutting backend admin mutation code. Keep frontend work minimal and only add UI if needed for acceptance.

## API / Data Contract Impact

If an audit read endpoint is added, document it in `docs/09-api-spec/admin.md` and `docs/06-http/admin.http`.

## Risks

- Missing operator identity in background or test flows.
- Logging sensitive full payloads unnecessarily.
- Changing existing admin behavior while adding logging.

## Verification Plan

- Backend: focused audit tests covering at least two existing admin mutations.
- Backend: run `.\mvnw.cmd test` from `backend/`.
- Frontend: run `npm test` and `npm run build` only if frontend changes.
- API validation: verify audit read smoke if an endpoint is added.

## Acceptance Criteria

- [x] Critical admin mutations selected for v1 write audit records.
- [x] Audit record includes operator, action, target, reason/summary, and timestamp.
- [x] Audit logging does not replace existing business validation.
- [x] Audit read access, if added, is admin-only.
- [x] Required backend tests pass.
- [x] API docs and HTTP files are updated for any new endpoint.
- [x] `CHANGELOG.md` is updated.
- [x] Completion notes are filled before archive.

## Sub-agent Instructions

- Do not log secrets or unnecessary full snapshots.
- Do not broaden into role-permission work unless the task is explicitly updated.
- Return covered actions, audit schema/contract summary, changed files, verification results, and uncovered actions recommended for later.

## Feedback To Head Agent

Return:

- covered admin actions;
- audit record contract summary;
- changed files;
- verification commands and results;
- uncovered high-risk actions recommended for the next slice.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/` if API changes
- [ ] `docs/09-api-spec/` if API changes
- [x] task status and archive move

## Head Agent Notes

- 2026-05-28: Unblocked after `platform-mediation-implementation` and `admin-dashboard-observability` were completed and committed. Current admin governance actions and mediation actions are available for selecting the v1 audit coverage.

## Completion Notes

- Added durable `admin_audit_logs` persistence with `operator_user_id`, `operator_role`, `action`, `target_type`, `target_id`, `summary`, and `created_at`.
- Added `GET /api/admin/audit-logs` as an admin-only paginated read path with `action` and `targetType` filters.
- v1 audited admin actions:
  - `USER_STATUS_UPDATE`
  - `PRODUCT_STATUS_UPDATE`
  - `STUDENT_VERIFICATION_REVIEW`
  - `PRODUCT_REVIEW_TASK_REVIEW`
  - `SHOP_STATUS_UPDATE`
  - `REPORT_PROCESS`
  - `SEARCH_GOVERNANCE_RULE_CREATE`
  - `SEARCH_GOVERNANCE_RULE_UPDATE`
  - `SEARCH_GOVERNANCE_RULE_DELETE`
- Kept business validation in existing service flows before audit writes; audit records are written only after successful mutations.
- Updated `docs/09-api-spec/admin.md`, `docs/06-http/admin.http`, and `CHANGELOG.md`.
- Verification:
  - `backend/ .\mvnw.cmd test` passed, 134 tests.
- Uncovered follow-up actions:
  - Admin order operations under `/api/admin/orders/**`.
  - Mediation escalation/status/decision operations under `/api/admin/mediation-cases/**`.
  - Future role-permission management once the role model task is implemented.
