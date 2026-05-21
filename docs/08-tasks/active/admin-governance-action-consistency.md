# Task: Admin Governance Action Consistency

## Metadata

- ID: admin-governance-action-consistency
- Status: active
- Owner: unassigned
- Track: cross-cutting
- Depends on: current admin governance baseline
- Priority: medium
- Planned date: 2026-05-20
- Completed date:

## Objective

Make admin governance actions across verification, report, and shop-management surfaces consistent and explainable without inventing unsupported business states.

## Background

The issue backlog reports that different objects expose different action sets in ways that feel inconsistent and opaque. This may be a UI-only clarity problem, a backend state-machine problem, or both.

## Scope

- audit action availability and labels on the scoped admin governance pages
- determine whether inconsistencies are intentional state-machine differences or accidental mismatch
- make the minimum frontend/backend changes needed so the surfaces are consistent and understandable

## Out of Scope

- redesign of the whole admin area
- new governance workflows
- mediation system implementation

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `frontend/README.md`
- `backend/README.md`
- `frontend/src/views/admin/ShopManageView.vue`
- `frontend/src/views/admin/ReviewTaskManageView.vue`
- report and verification related admin views/routes if different files own them
- `backend/src/main/java/com/campusmarket/backend/controller/admin/AdminController.java`
- related admin/report/shop service files

## Allowed Changes

- scoped admin frontend files
- minimum backend governance files needed if action semantics are wrong
- related docs/http/api-spec files only if contract semantics change
- `CHANGELOG.md`
- task lifecycle files under `docs/08-tasks/`

## Implementation Plan

1. Inventory current action sets and the state conditions behind them.
2. Distinguish real business-state differences from accidental UI inconsistency.
3. Fix the minimum surface or contract mismatch and document the result.

## Risks

- flattening genuinely different workflows into fake uniformity
- over-expanding into a full admin redesign

## Test Plan

- Backend:
  - run focused tests if backend files are touched
- Frontend:
  - run relevant tests/build if touched
- API validation:
  - update docs only if contract semantics change
- Manual:
  - verify the scoped admin pages show consistent and explainable action choices

## Acceptance Criteria

- [ ] The reason different objects expose different actions is explicit in code/UI or corrected
- [ ] Accidental inconsistency is reduced without inventing unsupported states
- [ ] The scoped admin governance pages are verified after the change

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [ ] task status and archive move

## Completion Notes
