# Task: Personalization Settings Center Integration

## Metadata

- ID: personalization-settings-center-integration
- Status: done
- Owner: main-agent
- Track: frontend
- Depends on: profile/avatar, email placeholder, preference-defaults child tasks
- Priority: P2
- Planned date: 2026-05-31
- Completed date: 2026-05-31

## Objective

Integrate personalization entries into the settings center so users can find profile, avatar, email, address, and effective preference controls without misleading placeholders.

## Scope

- Update settings center cards and routes to reflect implemented personalization capabilities.
- Add or connect settings subviews as needed:
  - profile/basic info
  - security/email placeholder
  - addresses/default address management
  - preferences/default flow settings
- Keep empty/loading/error states explicit.
- Ensure mobile layout does not overlap or hide action controls.
- Final integration pass across child-task outputs, docs, changelog, and task archival.

## Out of Scope

- Broad UI redesign of the whole app.
- Theme personalization.
- Full privacy settings implementation.
- Account deletion, password reset, or email verification delivery.

## Files to Read

- `frontend/src/router/modules/app.js`
- `frontend/src/views/app/SettingsView.vue`
- `frontend/src/views/app/ProfileView.vue`
- `frontend/src/views/app/PreferenceSettingsView.vue`
- `frontend/src/stores/market.js`
- `frontend/src/stores/auth.js`
- `frontend/src/components/common/EmptyState.vue`
- child task diffs before integration

## Allowed Changes

- `frontend/src/router/modules/app.js`
- `frontend/src/views/app/SettingsView.vue`
- `frontend/src/views/app/ProfileView.vue`
- `frontend/src/views/app/PreferenceSettingsView.vue`
- new scoped settings subviews under `frontend/src/views/app/`
- `frontend/src/stores/market.js`
- `frontend/src/stores/auth.js`
- related frontend tests
- `CHANGELOG.md`
- task lifecycle files under `docs/08-tasks/`

## Implementation Plan

1. Integrate child-task UI entries into settings center and route structure.
2. Remove or relabel stale placeholder cards that still say "coming soon" for implemented capabilities.
3. Run final frontend checks and review changed UI text for honesty.
4. Update changelog and child completion notes only after reviewing all child diffs.

## Risks

- Creating duplicate settings pages that claim the same source of truth.
- Leaving implemented features hidden behind old placeholder cards.
- Overlapping with child-task ownership before worker changes are reviewed.

## Test Plan

- Backend:
  - not required unless integration changes backend.
- Frontend:
  - `npm test`
  - `npm run build`
- API validation:
  - confirm docs/HTTP already updated by child tasks.
- Manual:
  - Navigate settings center on desktop and mobile widths.
  - Confirm profile/avatar/email/default preference entry points are discoverable and honest.

## Acceptance Criteria

- [x] Settings center reflects implemented personalization capabilities.
- [x] No duplicate or conflicting settings entry claims to own the same capability.
- [x] Placeholder text remains only for explicitly deferred capabilities.
- [x] Parent and reviewed child tasks are ready for archive.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] task status and archive move after review

## Completion Notes

- Added settings cards/routes for profile/avatar, email binding, address management, and preference defaults.
- Updated changelog, API spec, HTTP smoke examples, and task records.
