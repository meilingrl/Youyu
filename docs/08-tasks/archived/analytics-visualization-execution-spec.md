# Task: Analytics Visualization Execution Spec

## Metadata

- ID: analytics-visualization-execution-spec
- Status: completed
- Owner: main-agent
- Track: cross-cutting
- Depends on: `analytics-visualization-parent`
- Priority: high
- Planned date: 2026-06-02
- Completed date: 2026-06-02

## Objective

Lock the current analytics-visualization scope, wave order, owned files, and deferred items before implementation or delegation.

## Background

This repository already has three analytics entry points with real or partially real data:

- user insight snapshot powering the profile domain
- shop insight snapshot powering the storefront domain
- admin dashboard counts and workbench observability

What is still missing is not basic data existence, but better visualization, explanation, and contract discipline.

## Scope

- document the frozen wave boundaries for analytics visualization
- define worker-safe ownership for user/shop, admin, and contract reconciliation slices
- record explicitly deferred analytics collection or payment-related work

## Out of Scope

- code implementation
- task archival
- changelog closeout

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/04-standards/development-process.md`
- `frontend/src/views/app/ProfileView.vue`
- `frontend/src/views/app/ShopView.vue`
- `frontend/src/views/admin/DashboardView.vue`
- `backend/src/main/java/com/youyu/backend/service/user/impl/UserServiceImpl.java`
- `backend/src/main/java/com/youyu/backend/service/shop/impl/ShopServiceImpl.java`
- `backend/src/main/java/com/youyu/backend/service/admin/impl/AdminServiceImpl.java`

## Allowed Changes

- this task document
- related analytics wave task documents

## Implementation Plan

1. Record current repo truth for user/shop/admin analytics.
2. Freeze wave order and locked interfaces.
3. Hand off implementation ownership through child tasks.

## Risks

- mixing data-collection architecture work into a visualization-focused wave
- letting one slice silently redefine analytics semantics used by another slice

## Test Plan

- Backend: not applicable
- Frontend: not applicable
- API validation: not applicable
- Manual: review the execution spec against live code truth before dispatch

## Acceptance Criteria

- [x] Wave order and ownership are explicit.
- [x] Deferred work is listed clearly.
- [x] Locked interfaces match live repo truth.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes

- Proposed wave order:
  1. `analytics-visualization-user-shop-surfaces`
  2. `analytics-visualization-admin-dashboard`
  3. `analytics-visualization-contract-reconciliation`
- Recommended ownership:
  - user/shop slice may edit the two app views, shared chart/presentation components, and only narrow backend fields if the current endpoints are insufficient for explanation
  - admin slice may edit dashboard frontend plus narrow backend dashboard aggregation support
  - contract slice is main-agent-owned and runs after implementation stabilizes
- Explicitly deferred:
  - payment/refund analytics and any merge with the payment branch
  - new telemetry collection pipelines
  - recommendation/personalization expansion
  - theme/presentation work unrelated to analytics comprehension
