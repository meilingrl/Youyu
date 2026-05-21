# Task: Chat MVP Scope Definition

## Metadata

- ID: chat-mvp-scope-definition
- Status: active
- Owner: unassigned
- Track: feature
- Depends on: current roadmap and the archived messages/support UI shell task
- Priority: high
- Planned date: 2026-05-20
- Completed date:

## Objective

Turn the current chat MVP uncertainty into a precise, execution-ready first-version task spec that another agent can later implement without re-arguing the basic scope.

## Background

The roadmap names chat MVP as the next main feature line, but `open-questions.md` still leaves the minimum boundary unresolved. The existing `/app/messages` and `/admin/support` UI shell provides a safe frontend entry, but the real backend/model boundary is still undecided.

## Scope

- define the minimum first-version chat capability
- define the first-version excluded capabilities
- define required backend entities/endpoints at the planning level
- define the frontend surfaces that belong to MVP versus later phases
- create or update an executable task doc for later implementation

## Out of Scope

- implementing chat backend or frontend behavior
- websocket choice or production infra deep-dive
- platform mediation implementation

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/04-standards/development-process.md`
- `docs/05-roadmap/current/stage-roadmap.md`
- `docs/05-roadmap/current/feature-roadmap.md`
- `docs/05-roadmap/current/open-questions.md`
- `docs/02-requirements/communication-and-after-sales-boundary.md`
- `docs/03-architecture/frontend-information-architecture.md`
- `docs/08-tasks/archived/ui-redesign-messages-support.md`

## Allowed Changes

- `docs/02-requirements/*.md`
- `docs/03-architecture/*.md`
- `docs/05-roadmap/current/*.md` if needed for wording alignment
- `docs/08-tasks/active/*.md`
- `CHANGELOG.md`

## Implementation Plan

1. Resolve the minimum MVP boundary from the existing open questions and current message/support shell.
2. Write a concrete task spec for the first executable chat implementation slice.
3. Record explicit non-goals so later agents do not expand it into mediation or full support tooling.

## Risks

- planning drift into a full chat architecture rewrite
- mixing mediation/report logic into the chat MVP
- leaving the resulting scope still too vague to implement

## Test Plan

- Backend: not required
- Frontend: not required
- API validation: not required
- Manual:
  - confirm the resulting task doc can be handed to an agent directly
  - confirm non-goals are explicit enough to stop scope creep

## Acceptance Criteria

- [ ] The chat MVP minimum boundary is explicit
- [ ] First-version out-of-scope items are explicit
- [ ] A later implementation agent could start from the resulting task doc without needing a new planning pass
- [ ] `CHANGELOG.md` records the planning/spec work

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [x] roadmap or standards docs if applicable
- [ ] task status and archive move

## Completion Notes
