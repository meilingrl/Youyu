# Task: Execution Wave Plan For Current Unfinished Work

## Metadata

- ID: execution-wave-plan-2026-05-20
- Status: active
- Owner: meilingrl
- Track: cross-cutting
- Depends on: current roadmap, `docs/08-tasks/drafts/*.md`, and `docs/08-tasks/drafts/issue.md`
- Priority: high
- Planned date: 2026-05-20
- Completed date:

## Objective

Turn the current unfinished roadmap items and issue backlog into a set of small, handoff-ready execution tasks that can be distributed to multiple agents over several rounds with minimal write-scope conflict.

## Background

The repository now has a cleaner task lifecycle, but the remaining work is still spread across:

- roadmap lanes that are not yet split into executable tasks
- umbrella draft tasks that are too large for safe parallel execution
- a raw issue note file with several concrete bugs and UX gaps

The goal of this planning task is not to implement those items directly. The goal is to make the next few rounds executable.

## Scope

- identify the current unfinished lanes that are ready to execute now
- split them into small active tasks with explicit file boundaries
- group the tasks into safe execution waves
- provide copy-ready prompts for downstream agents

## Out of Scope

- implementing the tasks listed in this plan
- broad reprioritization of project direction
- reopening already archived completed tasks

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/04-standards/development-process.md`
- `docs/05-roadmap/current/stage-roadmap.md`
- `docs/05-roadmap/current/feature-roadmap.md`
- `docs/05-roadmap/current/open-questions.md`
- `../drafts/api-spec-standardization-follow-up.md`
- `../drafts/architecture-performance-hardening.md`
- `../drafts/issue.md`

## Allowed Changes

- task files under `docs/08-tasks/active/`
- `CHANGELOG.md`

## Implementation Plan

1. Keep execution in small waves instead of opening every lane as one large mixed task.
2. Use round-based dispatch with explicit scope ownership and dependency notes.
3. Keep docs-only planning tasks separate from code-touching tasks whenever possible.

## Wave Breakdown

### Wave 1: low-conflict foundations

- `roadmap-hot-search-state-reconciliation.md`
- `api-spec-review-module-standardization.md`
- `registration-flow-contract-diagnosis.md`
- `seller-publish-loading-diagnosis.md`

Rationale:
- one roadmap/docs lane
- one API-doc lane
- two isolated bug-diagnosis lanes

### Wave 2: next-step planning and safe frontend cleanup

- `chat-mvp-scope-definition.md`
- `api-spec-report-module-standardization.md`
- `frontend-bundle-second-pass-planning.md`
- `user-facing-enum-label-normalization.md`

Rationale:
- chat scope planning is still docs-only
- report API spec is separate from review API spec
- bundle follow-up planning is cross-cutting but still planning-only
- enum normalization is a narrow UI cleanup slice if kept out of admin-governance pages

### Wave 3: behavior consistency and visible product gaps

- `admin-governance-action-consistency.md`
- `preference-theme-capability-gap.md`
- `review-entry-and-seed-flow-bridge.md`

Rationale:
- these are user-visible but independent enough once earlier planning work is underway
- each has a bounded write scope

### Wave 4: blocked by upstream clarification

- `platform-mediation-boundary-definition.md`

Rationale:
- depends on the chat MVP scope definition
- should stay docs-first until the boundary is explicit

## Not Opened As Separate Execution Tasks In This Wave

- raw issue note item `4 UI优化，问题很多`
  - not opened as one task because it is too vague to execute safely
  - decomposed instead into concrete slices such as enum labels, seller publish loading, preference theme gap, and review-flow bridge
- umbrella draft `architecture-performance-hardening.md`
  - not reopened here as one large active task
  - previously finished child slices are already archived
  - the remaining hardening work should be opened later as narrower follow-up tasks after the current backlog wave is reduced

## File-Conflict Rules

- `user-facing-enum-label-normalization.md` must not edit admin governance list/detail pages owned by `admin-governance-action-consistency.md`
- `api-spec-review-module-standardization.md` owns review-spec files; `api-spec-report-module-standardization.md` owns report-spec files
- `chat-mvp-scope-definition.md` owns chat scope/spec docs; `platform-mediation-boundary-definition.md` must not start until the chat scope task lands
- `seller-publish-loading-diagnosis.md` owns seller publish loading diagnosis and minimal fix scope; it should not absorb unrelated seller UI redesign

## Risks

- roadmap and code reality drifting again if the follow-up agents only change code and skip task closeout
- bug-fix tasks silently widening into UI redesign
- blocked planning lanes being treated as coding work too early

## Test Plan

- Backend: not applicable for this planning task
- Frontend: not applicable for this planning task
- API validation: not applicable for this planning task
- Manual:
  - confirm every currently unfinished lane now has either an active task or an explicit deferred note
  - confirm each active task has a bounded write scope
  - confirm each wave can be dispatched without obvious file overlap

## Acceptance Criteria

- [ ] Current unfinished work is split into active task documents
- [ ] The tasks are grouped into at least two execution waves
- [ ] Blocked tasks are marked clearly instead of being mixed into executable waves
- [ ] A companion prompt document exists for downstream agent handoff

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [ ] task status and archive move

## Completion Notes
