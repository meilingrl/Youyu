# Task: Preference Theme Capability Gap

## Metadata

- ID: preference-theme-capability-gap
- Status: active
- Owner: unassigned
- Track: feature
- Depends on: current settings/preferences frontend baseline
- Priority: medium
- Planned date: 2026-05-20
- Completed date:

## Objective

Audit the current gap between what the preference/settings UI promises and what the theme capability actually supports, then land the smallest honest improvement.

## Background

The issue backlog reports that dark mode appears unimplemented and theme-color choice is too limited or misleading. This is partly a product-capability question and partly a UI honesty question.

## Scope

- inspect current preference/theme-related UI and state flow
- identify which options are really supported
- either implement a missing supported capability cleanly or downgrade/remove misleading UI promises

## Out of Scope

- full design-system rewrite
- extensive theme personalization product expansion
- unrelated settings cleanup

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `frontend/README.md`
- `frontend/src/views/app/SettingsView.vue`
- `frontend/src/views/app/PreferenceSettingsView.vue`
- theme/style variables and preference store files

## Allowed Changes

- the scoped settings/preferences/theme frontend files
- related frontend tests
- `CHANGELOG.md`
- task lifecycle files under `docs/08-tasks/`

## Implementation Plan

1. Audit the actual current theme capability end to end.
2. Decide the smallest honest improvement:
   - implement missing support if it is already implied by existing architecture
   - otherwise trim or relabel misleading options
3. Verify the resulting preference behavior.

## Risks

- adding a large theming system under a small bug-fix task
- leaving unsupported options visible after partial changes

## Test Plan

- Backend: not required unless a preference contract bug is touched
- Frontend:
  - run relevant tests/build if touched
- API validation:
  - update docs only if preference contract changes
- Manual:
  - verify the preference UI no longer overpromises unsupported theme behavior

## Acceptance Criteria

- [ ] The actual supported theme capability is clear and consistent in the UI
- [ ] Misleading unsupported options are removed, downgraded, or implemented cleanly
- [ ] The scoped settings/preferences flow is verified after the change

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [ ] task status and archive move

## Completion Notes
