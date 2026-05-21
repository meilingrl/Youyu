# Task: Admin Governance UI Consistency

## Metadata

- ID: admin-governance-ui-consistency
- Status: completed
- Owner: Claude
- Track: cross-cutting
- Depends on: current admin management baseline
- Priority: medium
- Completed date: 2026-05-16

## Objective

Improve consistency, clarity, and maintainability across admin governance and management pages by aligning list-page structure, filter behavior, feedback states, and common interaction patterns.

## Scope

- Admin list-page filter and query behavior consistency
- Shared loading, empty, and error-state treatment
- Pagination and refresh behavior alignment
- Dialog submission and status-feedback consistency
- Extraction of reusable admin page helpers or shell components where justified

## Changes Made

### New file
- `frontend/src/utils/error-utils.js` — shared `resolveErrorMessage` utility

### Enhanced
- `frontend/src/components/shell/ListPageShell.vue` — added `loading`, `error`, `emptyTitle`, `emptyDescription` props; renders `ErrorBlock` / `EmptyState` when provided

### Refactored (all 9 admin pages)
- `VerificationManageView.vue` — added error/empty states, switched to shared error util
- `ReviewTaskManageView.vue` — same
- `ReportManageView.vue` — same
- `ProductManageView.vue` — same
- `UserManageView.vue` — same
- `ShopManageView.vue` — same
- `HotSearchGovernView.vue` — same
- `DashboardView.vue` — switched to shared error util
- `OrderManageView.vue` — structural refactor to ListPageShell, added summary/filters/table slots, switched to shared error util

## Acceptance Criteria

- [x] Admin pages follow consistent filter and reset behavior
- [x] Loading, empty, and error states are aligned across touched pages
- [x] Repeated dialog workflows provide consistent feedback
- [x] Reuse improves maintainability without erasing page-specific semantics
- [x] No unrelated business behavior is silently changed

## Verification

- `npm run build` — zero errors
- `npm test` — 6 test files passed, 25 tests passed
- No backend changes needed
- No API contract changes — `docs/06-http/` unchanged

## Completion Notes

All 9 admin pages now use a consistent structure: `ListPageShell` with summary/filters/table slots, shared `resolveErrorMessage` from `@/utils/error-utils`, and proper empty/error state handling via `EmptyState` and `ErrorBlock` components rendered through the shell. The refactoring was conservative — no business logic was changed, no new abstractions were created beyond what was justified by existing repetition.
