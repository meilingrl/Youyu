# Task: Frontend Bundle Second Pass Planning

## Metadata

- ID: frontend-bundle-second-pass-planning
- Status: active
- Owner: unassigned
- Track: cross-cutting
- Depends on: archived `frontend-bundle-optimization` first pass
- Priority: medium
- Planned date: 2026-05-20
- Completed date:

## Objective

Plan the next small, low-risk bundle-governance slice after the first pass so the repository has an execution-ready second-pass task instead of only a vague roadmap note.

## Background

The first pass already reduced global Element Plus cost and lazy-loaded route shells, but the roadmap still lists frontend bundle governance as in progress without a concrete next slice.

## Scope

- baseline the current bundle situation after the first pass
- identify the next bounded optimization target
- open a later executable task/spec for that slice

## Out of Scope

- broad bundle refactor in this task
- API contract changes
- generic frontend redesign

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/05-roadmap/current/stage-roadmap.md`
- `docs/05-roadmap/current/feature-roadmap.md`
- `frontend/README.md`
- `docs/08-tasks/archived/frontend-bundle-optimization.md`
- current frontend build setup and router/layout entry files

## Allowed Changes

- `docs/05-roadmap/current/*.md` if wording alignment is needed
- `docs/08-tasks/active/*.md`
- `CHANGELOG.md`

## Implementation Plan

1. Reconstruct the post-first-pass bundle baseline from current frontend structure and prior task notes.
2. Pick the highest-value low-risk next slice.
3. Produce an execution-ready task doc for that second pass.

## Risks

- planning a too-large optimization batch instead of a reversible slice
- accidentally drifting into code changes instead of planning

## Test Plan

- Backend: not required
- Frontend: optional baseline build inspection if needed for planning
- API validation: not required
- Manual:
  - confirm the resulting next slice has a bounded write scope
  - confirm it does not require API contract changes

## Acceptance Criteria

- [ ] The current bundle-governance lane has a concrete second-pass plan
- [ ] The plan identifies a bounded next slice rather than a broad refactor
- [ ] `CHANGELOG.md` records the planning/spec work

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [x] roadmap or standards docs if applicable
- [ ] task status and archive move

## Completion Notes
