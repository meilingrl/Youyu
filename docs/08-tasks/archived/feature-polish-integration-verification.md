# Task: Feature Polish Integration Verification

## Metadata

- ID: feature-polish-integration-verification
- Status: completed
- Owner: main-agent
- Track: cross-cutting
- Depends on: all feature-polish child tasks
- Priority: P0
- Planned date: 2026-06-03
- Completed date: 2026-06-03

## Objective

Review, integrate, verify, document, and archive the feature-polish closeout work after child tasks report completion.

## Background

Workers must not be treated as proof of completion. The main Agent must review diffs and verify behavior before task archival or publishing.

## Scope

- Inspect every worker diff.
- Confirm child-task scope and locked-interface compliance.
- Resolve overlap conservatively.
- Run required backend/frontend checks.
- Verify manual browser flows for the requested user-facing/admin-facing paths.
- Update API specs, HTTP samples, changelog, task statuses, and archives.
- Produce a final readiness report separating completed, deferred, and blocked items.

## Out of Scope

- Accepting worker summaries without diff review.
- Archiving tasks with unverified claims.
- Pushing or opening PR unless the human asks.

## Files to Read

- all `docs/08-tasks/active/feature-polish-*.md`
- `CHANGELOG.md`
- changed files from every worker
- `docs/06-http/*.http` changed by workers
- `docs/09-api-spec/*.md` changed by workers

## Allowed Changes

- Narrow integration fixes in files already touched by accepted child tasks.
- `CHANGELOG.md`
- `docs/08-tasks/active/feature-polish-*.md`
- move completed task docs to `docs/08-tasks/archived/`
- matching API spec / HTTP docs where workers missed required contract updates

## Implementation Plan

1. Confirm current branch/worktree and preserve unrelated user/agent changes.
2. Review each child diff for scope, contracts, permissions, and UI state coverage.
3. Run automated checks.
4. Manually verify messages, explore, cart/reviews/refunds/logistics, admin users, admin dashboard, and export.
5. Patch narrow integration defects only when they are inside accepted scope.
6. Update changelog and task completion notes.
7. Archive reviewed tasks.
8. Report residual blockers and deferred provider-backed work.

## Risks

- Dirty worktree can hide unrelated changes.
- Multiple workers may touch shared frontend route/API modules.
- Some requested features may remain provider-blocked; final report must say so.

## Test Plan

- Backend: `cd backend; .\mvnw.cmd test`
- Frontend: `cd frontend; npm test`
- Frontend build: `cd frontend; npm run build`
- Frontend E2E smoke where available: `cd frontend; npm run test:e2e`
- Diff hygiene: `git diff --check`
- Manual browser verification for each requested path.

## Acceptance Criteria

- [ ] Every child task has reviewed diffs and completion notes.
- [ ] Automated checks pass or failures are documented with root cause.
- [ ] API docs and `.http` samples match changed contracts.
- [ ] `CHANGELOG.md` has a prepend entry for the completed polish wave.
- [ ] Completed task docs are archived; blocked/deferred tasks remain active or clearly marked.
- [ ] Final report lists branch/worktree, commands/results, blockers, warnings, and deferred work.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] relevant files in `docs/09-api-spec/`
- [ ] task status and archive move

## Completion Notes

- Reviewed the integrated feature-polish diff in the isolated worktree, ran the required backend/frontend checks, and repaired the final admin dashboard/export mismatch found during live verification.
- Completed browser/manual verification for messages, explore, cart, order logistics/refund detail, admin dashboard, and admin users before documentation closeout.
