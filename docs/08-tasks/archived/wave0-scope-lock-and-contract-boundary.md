# Task: Wave 0 Scope Lock And Contract Boundary

## Metadata

- ID: wave0-scope-lock-and-contract-boundary
- Status: completed
- Owner: main-agent
- Track: cross-cutting
- Depends on: `wave0-low-conflict-closeout-parent`
- Priority: high
- Planned date: 2026-06-01
- Completed date: 2026-06-01

## Objective

Freeze the exact Wave 0 delivery boundary so delegated workers can implement low-conflict slices without reopening roadmap-level scope questions.

## Background

The repository has several unfinished lanes, but only a subset is easy enough for a first parallel round. The main risk is accidental expansion from a small contract-closeout round into category management, support/mediation ownership, or payment/refund redesign.

## Scope

- record the approved Wave 0 slices
- record frozen routes and response shapes
- record explicit deferred capability list
- define which documentation alignment is allowed in this round

## Out of Scope

- implementation code changes
- revisiting roadmap ordering
- introducing new active lanes outside Wave 0

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/05-roadmap/current/stage-roadmap.md`
- `docs/05-roadmap/current/feature-roadmap.md`
- `docs/05-roadmap/current/launch-preparation-roadmap.md`
- `docs/06-http/product.http`
- `docs/06-http/review.http`
- `docs/09-api-spec/review.md`

## Allowed Changes

- this task file
- `wave0-low-conflict-execution-spec.md`
- other Wave 0 task files under `docs/08-tasks/active/`

## Implementation Plan

1. Reconcile current roadmap truth with live code.
2. Freeze the exact Wave 0 scope and locked interfaces.
3. Hand the frozen boundary to worker and integration tasks.

## Risks

- letting the favorites slice absorb category work
- letting contract cleanup expand into unrelated API normalization

## Test Plan

- Backend: not applicable
- Frontend: not applicable
- API validation: task text must match current approved Wave 0 routes
- Manual: verify all child tasks remain inside the frozen boundary

## Acceptance Criteria

- [x] Wave 0 in-scope work is explicit and narrow
- [x] Deferred work is explicit and visible to all workers
- [x] Worker ownership is disjoint enough for parallel execution

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes
- Froze Wave 0 to favorites closure, review-summary real distribution, and direct contract/doc alignment for those slices only.
- Locked the favorites REST routes and review-summary response shape before dispatching workers.
- Recorded the deferred larger lanes explicitly to prevent worker scope expansion.
