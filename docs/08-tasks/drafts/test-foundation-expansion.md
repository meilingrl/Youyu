# Task: Test Foundation Expansion

## Metadata

- ID: test-foundation-expansion
- Status: draft
- Owner:
- Track: cross-cutting
- Depends on: current backend/frontend baseline
- Priority: high
- Planned date:
- Completed date:

## Objective

Expand automated test coverage around the core user, transaction, governance, and admin flows so later feature work can iterate with lower regression risk.

## Background

Current automated coverage is thin compared with the amount of implemented functionality. The repository already contains working backend and frontend modules, but the test matrix does not yet cover enough of the core execution paths, especially order, payment, report, admin management, and end-to-end page flows.

This task is intended as low-conflict foundation work that can proceed in parallel with feature development, as long as it does not rewrite current product behavior.

## Scope

- Backend integration and controller-level tests for major business domains
- Frontend store and page-level behavior checks where coverage is missing
- End-to-end smoke coverage for the main user and admin journeys
- Shared test helpers, fixtures, and setup cleanup to reduce duplication
- Test-oriented documentation updates when flows or assumptions need to be written down

## Out of Scope

- Rewriting business logic just to make tests easier
- Changing product requirements or endpoint contracts without separate approval
- Deep search / hot-search work already covered by another task line
- API specification workflow ownership

## Files to Read

- `../../04-standards/development-process.md`
- `../../04-standards/testing-workflow.md` if present
- `../../05-roadmap/current/stage-roadmap.md`
- `../../02-requirements/non-functional-requirements.md`
- `../../../backend/README.md`
- `../../../frontend/README.md`
- existing backend tests
- existing frontend store tests
- `../../06-http/*.http`

## Allowed Changes

- backend test sources under `backend/src/test/`
- frontend unit test sources under `frontend/src/**/__tests__/`
- frontend E2E test assets under `frontend/tests/`
- minimal test-support changes in app code when necessary for determinism
- relevant files in `../../06-http/`
- related task and changelog documents

## Implementation Plan

1. Audit existing automated coverage and map missing critical flows.
2. Add backend tests for auth, order, payment, report, and admin flows with realistic success and failure cases.
3. Add frontend tests for missing stores and high-risk views where interaction logic is non-trivial.
4. Add or expand E2E smoke flows for user purchase and admin governance paths.
5. Introduce shared helpers or fixtures to keep tests consistent and maintainable.
6. Align HTTP examples or task notes when tests expose undocumented behavior.

## Risks

- Brittle tests caused by unstable seed data assumptions
- Over-mocking frontend flows so tests stop reflecting real behavior
- Hidden coupling between current implementation and undocumented edge cases
- Slow CI or local test runs if scope is expanded without prioritization

## Test Plan

- Backend:
  - run `mvnw.cmd test`
  - verify new tests cover both success and failure paths
- Frontend:
  - run `npm run test`
  - ensure added tests reflect real store/component behavior rather than implementation trivia
- API validation:
  - confirm existing `docs/06-http/*.http` examples still match tested endpoints
- Manual:
  - spot-check at least one purchase flow and one admin governance flow after automated additions

## Acceptance Criteria

- [ ] Core user purchase path has automated regression coverage
- [ ] Admin governance path has automated regression coverage
- [ ] Backend tests cover key failure paths, not only happy paths
- [ ] Frontend or E2E coverage exists for major visible flows with current user impact
- [ ] New tests use shared helpers where duplication would otherwise grow quickly

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [ ] task status and archive move

## Completion Notes
