# Task: API spec standardization follow-up

## Metadata

- ID: docs-api-spec-standardization-follow-up
- Status: draft
- Owner: AI agent
- Track: cross-cutting
- Depends on: current module specs in `docs/09-api-spec/`
- Priority: medium
- Planned date: 2026-05-16
- Completed date:

## Objective

Continue turning the current API collaboration assets into a sustainable workflow by expanding formal API specs module by module and keeping `.http` validation assets aligned with real controller behavior.

## Background

The repository already separates `docs/06-http/` and `docs/09-api-spec/`, but formal spec coverage still lags behind the controller surface area. The first module specs (`auth`, `product`, `order`, `user`, `admin`) establish the format, error semantics, and maintenance expectations, but additional modules remain undocumented at the formal-contract layer.

## Scope

- Add formal specs for uncovered or partially covered API modules
- Keep `.http` assets aligned with controller methods and paths
- Record shared envelope, error, and role rules consistently across specs
- Expand module-to-HTTP mapping without introducing OpenAPI generation in this task

## Out of Scope

- Backend business refactors
- Controller signature redesign purely for documentation convenience
- Introducing Swagger / OpenAPI generation tooling
- Rewriting every existing `.http` file unless drift is directly relevant to the module being standardized

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/04-standards/development-process.md`
- `docs/05-roadmap/current/stage-roadmap.md`
- `docs/05-roadmap/current/feature-roadmap.md`
- `docs/09-api-spec/README.md`
- `docs/09-api-spec/API_SPEC_TEMPLATE.md`
- relevant controllers and `.http` files for the target module

## Allowed Changes

- `docs/09-api-spec/*.md`
- relevant files in `docs/06-http/`
- `CHANGELOG.md`
- task lifecycle files under `docs/08-tasks/`

## Implementation Plan

1. Pick the next uncovered module and identify its controller(s), DTOs, and matching `.http` validation assets.
2. Write or expand a formal spec that documents authentication, request fields, response `data` shape, error semantics, and `.http` mapping.
3. Fix any direct controller-to-HTTP drift discovered for that module and record substantive progress in `CHANGELOG.md`.

## Risks

- Old `.http` examples may have drifted in method/path or payload details and need focused correction.
- Some modules still use map-style payloads, so exact request-body contracts may remain looser than DTO-backed modules.
- One logical domain may span multiple controllers, requiring careful module-boundary decisions.

## Test Plan

- Backend: not required unless a documentation task exposes a code defect that must be fixed
- Frontend: not required unless a related API consumer change is intentionally included
- API validation: static controller/spec/`.http` alignment check for the targeted module
- Manual: spot-check paths, methods, role requirements, and error semantics against source controllers and `GlobalExceptionHandler`

## Acceptance Criteria

- [ ] At least one additional uncovered API module has a formal spec under `docs/09-api-spec/`
- [ ] The targeted module spec cites its source controller(s) and related `.http` file(s)
- [ ] Direct controller-to-`.http` drift found in scope is corrected
- [ ] Shared response-envelope and error semantics remain consistent with current backend behavior
- [ ] `CHANGELOG.md` records substantive documentation progress

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [ ] task status and archive move

## Completion Notes

Recommended next module candidates:

- `review`
- `report`
- `recommend`
- `search`
- `payment`
- `shop`
- `cart`

Completion rule for each added module:

- controller behavior aligned
- formal spec added or expanded
- `.http` mapping documented
- direct drift corrected
- change recorded in `CHANGELOG.md`
