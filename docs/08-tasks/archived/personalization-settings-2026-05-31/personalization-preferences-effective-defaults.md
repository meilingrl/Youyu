# Task: Personalization Preferences Effective Defaults

## Metadata

- ID: personalization-preferences-effective-defaults
- Status: done
- Owner: worker-or-main-agent
- Track: cross-cutting
- Depends on: existing preference API and address API
- Priority: P1/P2
- Planned date: 2026-05-31
- Completed date: 2026-05-31

## Objective

Make supported preference defaults affect real user flows before implementing theme personalization.

## Scope

- Default product sort should initialize product listing/search sort where that UI supports sorting.
- Default address should be manageable from settings and should prefill checkout when available.
- Default payment method should affect checkout/payment display where the current payment flow supports choice.
- Default fulfillment type should preselect a compatible checkout fulfillment option where available.
- Remove, disable, or relabel preference options that cannot honestly affect a flow in this wave.
- Keep preference API contract under `PUT /api/users/me/preference`.

## Out of Scope

- Theme mode and theme color implementation.
- Recommendation algorithm changes.
- Notification delivery engine.
- Payment gateway upgrade or real payment-provider integration.
- Address schema redesign beyond list/create/default if edit/delete are not included by this child.

## Files to Read

- `docs/08-tasks/active/preference-theme-capability-gap.md`
- `frontend/src/views/app/PreferenceSettingsView.vue`
- `frontend/src/views/app/ProductListView.vue`
- `frontend/src/views/app/CheckoutView.vue`
- `frontend/src/views/app/PaymentView.vue`
- `frontend/src/stores/market.js`
- `frontend/src/api/modules/user.js`
- `frontend/src/constants/insightMetrics.js`
- `backend/src/main/java/com/youyu/backend/service/user/impl/UserServiceImpl.java`
- `docs/09-api-spec/user.md`

## Allowed Changes

- `frontend/src/views/app/PreferenceSettingsView.vue`
- `frontend/src/views/app/ProductListView.vue`
- `frontend/src/views/app/CheckoutView.vue`
- `frontend/src/views/app/PaymentView.vue`
- `frontend/src/stores/market.js`
- `frontend/src/api/modules/user.js`
- `frontend/src/constants/insightMetrics.js`
- related frontend tests
- backend user preference validation only if required by existing behavior
- `docs/09-api-spec/user.md`
- `docs/06-http/*`

## Implementation Plan

1. Audit which preference fields currently have downstream UI hooks.
2. Wire default sort, address, payment, and fulfillment defaults into existing flow state.
3. Trim or relabel options that remain unsupported.
4. Add focused tests or manual verification notes for each supported default.

## Risks

- Reintroducing internal "mock payment" wording into user-visible pages.
- Changing checkout/order semantics instead of only preselecting supported defaults.
- Colliding with payment-upgrade worktree if payment flow files are modified concurrently.
- Accidentally completing the active theme task under this child.

## Test Plan

- Backend:
  - `mvnw.cmd test` only if backend validation is touched.
- Frontend:
  - `npm test`
  - `npm run build`
- API validation:
  - Update user preference spec if supported values or semantics are clarified.
- Manual:
  - Set default sort and open product list.
  - Set default address and open checkout.
  - Set default payment/fulfillment and verify checkout/payment preselection or honest fallback.

## Acceptance Criteria

- [x] Every preference option shown in this wave either affects a real flow or is visibly downgraded/removed.
- [x] Default sort affects product listing/search initialization.
- [x] Default address affects checkout prefill when address data exists.
- [x] Default payment and fulfillment settings affect supported checkout/payment choices without changing order status semantics.
- [x] Theme-related fields are not implemented as part of this task.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] `docs/09-api-spec/user.md`
- [ ] relevant files in `docs/06-http/`
- [ ] task status and archive move after review

## Completion Notes

- Product listing applies the saved default sort client-side after loading.
- Checkout loads preferences and uses default fulfillment/address where compatible with the current preview.
- Payment page displays the configured default payment preference without exposing internal mock wording.
- Theme controls were removed from the active preference UI and are not submitted by the preference form.
