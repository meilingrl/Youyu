# Task: Document Reality Calibration 2026-05-23

## Metadata

- ID: document-reality-calibration-2026-05-23
- Status: archived
- Owner: Codex
- Track: cross-cutting
- Depends on: archived `hot-search-enhancement-p3`, archived `roadmap-hot-search-state-reconciliation`, current backend runtime config
- Priority: high
- Planned date: 2026-05-23
- Completed date: 2026-05-23

## Objective

Re-align current planning and architecture documents with runtime truth after detecting stale roadmap and database-baseline text.

## Delivered

- Restored the current roadmap state so hot-search P3 is no longer listed as pending work.
- Removed resolved hot-search P3 and formal API-process questions from `open-questions.md`.
- Updated the feature roadmap to show formal API specs as in-progress with `recommend` and `shop` remaining.
- Corrected architecture and database docs so MySQL is the local/dev runtime database and H2 is test-only.
- Activated the `api-spec-standardization-follow-up` task for `recommend` and `shop` module specs.

## Verification

- Confirmed hot-search P3 code still exists in controller, service, mapper, frontend store/component, HTTP collection, and formal search spec.
- Ran backend focused tests for search suggestions and governance behavior:
  - `./mvnw.cmd -Dtest=YouyuBackendApplicationTests#searchSuggestionUsesLogPrefixAndDoesNotCreateExtraLogs+searchSuggestionHonorsGovernanceAndPinnedOrdering test`
- Confirmed backend runtime configuration uses `jdbc:mysql://localhost:3306/youyu`.
- Confirmed H2 is scoped to tests through `backend/src/test/resources/application.yml` and the Maven `h2` test dependency.

## Notes

No application source code was changed by this calibration task.
