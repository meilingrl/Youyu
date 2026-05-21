# Task: User Facing Enum Label Normalization

## Metadata

- ID: user-facing-enum-label-normalization
- Status: active
- Owner: unassigned
- Track: feature
- Depends on: current storefront and seller UI baseline
- Priority: medium
- Planned date: 2026-05-20
- Completed date:

## Objective

Replace raw snake_case enum/status values with readable Chinese user-facing labels on the scoped storefront and seller pages using a shared, maintainable mapping approach.

## Background

The issue backlog shows multiple places where raw values such as `offline_face_to_face`, `pending_payment`, and `pending_review` are rendered directly in the UI.

## Scope

- normalize user-facing labels on:
  - `OrdersView.vue`
  - `PaymentView.vue`
  - `SellerProductsView.vue`
- if needed, extract a shared label mapping utility or constants file

## Out of Scope

- admin governance pages handled by `admin-governance-action-consistency.md`
- broad copy redesign
- backend enum changes

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `frontend/README.md`
- `frontend/src/views/app/OrdersView.vue`
- `frontend/src/views/app/PaymentView.vue`
- `frontend/src/views/app/SellerProductsView.vue`
- existing frontend constants/normalizers/format helpers related to labels

## Allowed Changes

- the scoped frontend views
- shared frontend label/format helper files if needed
- related frontend tests
- `CHANGELOG.md`
- task lifecycle files under `docs/08-tasks/`

## Implementation Plan

1. Inventory the raw enum/status values still rendered in the scoped pages.
2. Introduce one shared mapping approach instead of repeated inline conversions.
3. Verify the pages display readable labels without changing backend semantics.

## Risks

- duplicating label maps in multiple files
- accidentally editing admin pages that belong to another task

## Test Plan

- Backend: not required
- Frontend:
  - run relevant unit tests/build if touched
- API validation: not required
- Manual:
  - verify the scoped pages no longer show raw snake_case values

## Acceptance Criteria

- [ ] Scoped storefront/seller pages no longer show raw snake_case enum/status values
- [ ] Label mapping is shared or centralized enough to avoid repeated ad hoc fixes
- [ ] Admin governance pages remain outside this task

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [ ] task status and archive move

## Completion Notes
