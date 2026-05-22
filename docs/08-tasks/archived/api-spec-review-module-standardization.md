# Task: API Spec Review Module Standardization

## Metadata

- ID: api-spec-review-module-standardization
- Status: archived
- Owner: unassigned
- Track: cross-cutting
- Depends on: current API spec workflow and existing review controllers
- Priority: medium
- Planned date: 2026-05-20
- Completed date: 2026-05-20

## Objective

Add the formal API specification for the review module and align `docs/06-http/review.http` with actual controller/runtime behavior.

## Background

The repository already has formal specs for auth, product, order, user, admin, and search, but review still lacks a formal module spec even though review behavior is already implemented and user-facing.

## Scope

- add `docs/09-api-spec/review.md`
- align `docs/06-http/review.http` with controller truth
- document auth, request fields, response data shape, and errors

## Out of Scope

- review feature redesign
- rating algorithm changes
- broad cleanup of unrelated `.http` files

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/04-standards/development-process.md`
- `docs/09-api-spec/README.md`
- `docs/09-api-spec/API_SPEC_TEMPLATE.md`
- `docs/06-http/review.http`
- `backend/src/main/java/com/youyu/backend/controller/review/ReviewController.java`
- `backend/src/main/java/com/youyu/backend/service/review/impl/ReviewServiceImpl.java`
- related request/response types if present

## Allowed Changes

- `docs/09-api-spec/*.md`
- `docs/06-http/review.http`
- minimal code/docs fixes only if direct contract drift demands it
- `CHANGELOG.md`
- task lifecycle files under `docs/08-tasks/`

## Implementation Plan

1. Audit current review endpoints against controller and service behavior.
2. Write the formal review module spec.
3. Correct direct drift in `review.http` and close the task.

## Risks

- review payloads may be partly map-shaped and require careful wording
- old examples may not match the current endpoint surface exactly

## Test Plan

- Backend: not required unless a tiny contract bug fix is necessary
- Frontend: not required
- API validation:
  - verify method/path/auth against controller truth
- Manual:
  - confirm the spec cites controller and HTTP assets explicitly

## Acceptance Criteria

- [x] `docs/09-api-spec/review.md` exists and follows the current template
- [x] `docs/06-http/review.http` matches current controller behavior in scope
- [x] Shared response-envelope and error semantics remain consistent with runtime truth
- [x] `CHANGELOG.md` records the spec expansion

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [x] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes

### Delivered

- Created `docs/09-api-spec/review.md` — formal spec covering 8 endpoints:
  - `POST /api/reviews/products`, `POST /api/reviews/shops` (submit, USER only)
  - `GET /api/reviews/pending`, `GET /api/reviews/mine` (history, USER only)
  - `GET /api/products/{id}/reviews`, `GET /api/products/{id}/review-summary` (public, on ProductController)
  - `GET /api/shops/{shopId}/reviews`, `GET /api/shops/{shopId}/review-summary` (public, on ShopController)
- Documented auth rules, request fields, response data shapes, error semantics, shared types (score 1–5, content max 1000, pagination defaults), and known limitations (distribution stub).
- Spec cites source-of-truth controllers, service, mapper, HTTP assets, and GlobalExceptionHandler.

### Drift audit

- Compared all 11 entries in `docs/06-http/review.http` against controller truth — methods, paths, auth requirements, and request body shapes match exactly. No drift found; no corrections needed.

### Ancillary

- Added `review.md` entry to `docs/09-api-spec/README.md` current-modules list.
- Prepended CHANGELOG entry.
