# Task: API Spec Report Module Standardization

## Metadata

- ID: api-spec-report-module-standardization
- Status: active
- Owner: unassigned
- Track: cross-cutting
- Depends on: current API spec workflow and existing report/admin report endpoints
- Priority: medium
- Planned date: 2026-05-20
- Completed date:

## Objective

Add the formal API specification for the report module and align `docs/06-http/report.http` with actual controller/runtime behavior.

## Background

User-side report submission and admin-side report processing already exist, but formal contract coverage for this domain is still missing.

## Scope

- add `docs/09-api-spec/report.md`
- align `docs/06-http/report.http` with controller truth
- document both user-side and admin-side report flows in one coherent module spec

## Out of Scope

- report workflow redesign
- platform mediation implementation
- unrelated admin-governance UI fixes

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/04-standards/development-process.md`
- `docs/09-api-spec/README.md`
- `docs/09-api-spec/API_SPEC_TEMPLATE.md`
- `docs/06-http/report.http`
- `backend/src/main/java/com/campusmarket/backend/controller/report/ReportController.java`
- `backend/src/main/java/com/campusmarket/backend/controller/admin/AdminController.java`
- `backend/src/main/java/com/campusmarket/backend/service/report/impl/ReportServiceImpl.java`

## Allowed Changes

- `docs/09-api-spec/*.md`
- `docs/06-http/report.http`
- minimal code/docs fixes only if direct contract drift demands it
- `CHANGELOG.md`
- task lifecycle files under `docs/08-tasks/`

## Implementation Plan

1. Audit current report endpoints against controller and service behavior.
2. Write the formal report module spec.
3. Correct direct drift in `report.http` and close the task.

## Risks

- the report domain spans both user and admin flows
- controller semantics may be split across modules and need careful cross-reference

## Test Plan

- Backend: not required unless a tiny contract bug fix is necessary
- Frontend: not required
- API validation:
  - verify method/path/auth against controller truth
- Manual:
  - confirm both submit and admin-process flows are documented

## Acceptance Criteria

- [ ] `docs/09-api-spec/report.md` exists and follows the current template
- [ ] `docs/06-http/report.http` matches current controller behavior in scope
- [ ] Shared response-envelope and error semantics remain consistent with runtime truth
- [ ] `CHANGELOG.md` records the spec expansion

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [ ] task status and archive move

## Completion Notes
