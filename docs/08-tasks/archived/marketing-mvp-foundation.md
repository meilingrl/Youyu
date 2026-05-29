# Task: Marketing MVP Foundation

## Metadata

- ID: marketing-mvp-foundation
- Status: completed
- Owner: head Agent
- Track: feature
- Depends on: `docs/02-requirements/marketing-mvp-scope.md`, current shop/order/admin/frontend baseline
- Priority: high
- Planned date: 2026-05-29
- Completed date: 2026-05-29

## Objective

Coordinate and deliver the first marketing MVP: shop-owner coupons, shop-owner activities, admin review, buyer coupon claim/use, activity display, and the required documentation and verification.

This task is written for a head Agent that may dispatch implementation work to sub-agents. It intentionally defines requirements, boundaries, acceptance criteria, and reporting expectations rather than prescribing detailed implementation.

## Background

Marketing features were previously roadmap placeholders. The clarified direction is:

- Coupons can be implemented first because they are the more complex foundation.
- Coupons are created by shop owners and require admin review before issuance.
- Shop activities follow the same ownership and review model.
- First coupon types are fixed discount and threshold discount.
- Users actively claim coupons.
- Coupon management should have a clear user entry, selected according to current UI structure.
- Concurrency protection is required from the first version.
- Activity statistics are not planned for this stage.

## Scope

- Define or update the marketing API contract and HTTP smoke examples.
- Add backend capability for shop-owner coupon management.
- Add backend capability for shop-owner activity management.
- Add backend admin review capability for coupons and activities.
- Add buyer coupon discovery, claim, and owned-coupon capabilities.
- Integrate one valid coupon into order preview and order creation.
- Add frontend surfaces for:
  - buyer coupon management;
  - checkout coupon selection;
  - shop-owner marketing management;
  - admin review;
  - public activity display.
- Add tests and verification appropriate to changed backend and frontend surfaces.
- Update roadmap, task, API, HTTP, and changelog documents required by the implementation.

## Out of Scope

- Activity statistics.
- Complex promotion rule engine.
- Activity-driven automatic order discounting.
- Discount-rate coupons.
- Free-shipping coupons.
- Coupon stacking.
- Cross-shop coupons.
- Automatic best-coupon selection.
- Payment gateway changes.
- Broad redesign of trade, profile, shop, or admin navigation unrelated to marketing.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/02-requirements/marketing-mvp-scope.md`
- `docs/04-standards/development-process.md`
- `docs/05-roadmap/current/feature-roadmap.md`
- `docs/05-roadmap/current/launch-preparation-roadmap.md`
- `backend/README.md`
- `frontend/README.md`
- Current order, shop, admin, route, navigation, and checkout files relevant to the assigned subtask.

Sub-agents should read only the implementation files relevant to their assigned slice after reading the required project and task documents.

## Allowed Changes

- Marketing requirement, task, API spec, HTTP smoke, roadmap, and changelog documents.
- Backend files required for coupon, activity, admin review, order integration, tests, schema, and seed data.
- Frontend files required for buyer coupon management, checkout selection, shop-owner marketing management, admin review, activity display, routing, navigation, and tests.

Do not modify unrelated feature areas except where necessary to connect marketing to existing order, shop, admin, or navigation flows.

## Dispatch Guidance

The head Agent may split work into independent sub-agent tasks. Suggested slices:

- Backend marketing domain and persistence.
- Order preview/create coupon integration.
- Admin review surface and permissions.
- Buyer/shop-owner frontend surfaces.
- API, HTTP, seed data, and verification follow-up.

These slices are suggestions, not mandatory implementation structure. The head Agent should avoid assigning two sub-agents the same write scope at the same time.

Each sub-agent dispatch must include:

- this task path;
- the relevant scope slice;
- files or modules the sub-agent owns;
- explicit instruction not to modify unrelated files;
- required verification for that slice;
- required final report format.

## Sub-Agent Report Requirements

Each sub-agent must report back to the head Agent with:

- changed file list;
- implementation summary;
- business scope covered;
- acceptance criteria covered and not covered;
- verification commands run and results;
- unresolved risks, assumptions, or decisions needed from the head Agent;
- any migrations, seed data, or API contract changes;
- any frontend route/navigation choices and why they were chosen.

## Implementation Requirements

The implementation must satisfy these requirements without being constrained to a specific internal design:

- Shop owners can create and manage their own coupons and activities.
- Admin review gates public issuance/display.
- Buyers can actively claim approved coupons.
- Buyers can view owned coupons.
- Checkout/order preview can apply one valid owned coupon.
- Order creation revalidates coupon eligibility on the server.
- Coupon application records the actual discount used by the order.
- Coupon stock and user-coupon use state are protected against obvious race conditions.
- Activities can be displayed publicly only when approved and effective.
- Activity display does not imply unimplemented automatic discounting or statistics.
- Permission checks exist on backend endpoints, not only in frontend navigation.

## Acceptance Criteria

- [ ] A shop owner can submit a fixed discount coupon for their own shop.
- [ ] A shop owner can submit a threshold discount coupon for their own shop.
- [ ] A shop owner can submit a shop activity for their own shop.
- [ ] Admin review is required before coupons can be claimed.
- [ ] Admin review is required before activities are publicly displayed.
- [ ] Buyer can claim an approved, active coupon.
- [ ] Buyer can view owned coupons from a clear frontend entry.
- [ ] Checkout/order preview can show and apply one valid coupon.
- [ ] Order creation revalidates the coupon and persists the applied discount.
- [ ] Duplicate claim, over-claim, and duplicate-use scenarios are rejected or safely handled.
- [ ] Public shop or product-facing UI can display approved effective activities.
- [ ] Backend tests cover the core marketing lifecycle and permission boundaries.
- [ ] Frontend tests or build verification cover touched marketing surfaces.
- [ ] API spec and HTTP smoke examples reflect the implemented contract.
- [ ] Activity statistics and complex promotion rule engine are not introduced.

## Test Plan

- Backend:
  - run the backend test suite or focused tests agreed by the head Agent;
  - include lifecycle, permission, and duplicate/overuse cases.
- Frontend:
  - run relevant frontend tests;
  - run build verification when routes or lazy-loaded views are touched.
- API validation:
  - provide smoke requests for owner, admin, buyer, and order use flows.
- Manual:
  - demonstrate owner submit -> admin approve -> buyer claim/use;
  - demonstrate owner submit activity -> admin approve -> public display.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] relevant files in `docs/09-api-spec/`
- [ ] roadmap docs if feature status changes
- [ ] task completion notes and archive move

## Completion Notes

Completed on 2026-05-29.

Implemented backend marketing foundation:

- Added additive DDL for `marketing_coupons`, `user_coupons`, `order_coupon_applications`, and `shop_activities`.
- Added owner coupon/activity APIs, buyer coupon discovery/claim/my-coupons APIs, public shop-activity API, and admin marketing review/disable APIs.
- Added `ADMIN_MARKETING_REVIEW` and enforced it on backend admin marketing endpoints.
- Integrated one claimed coupon into order preview and order creation.
- Order creation revalidates coupon eligibility server-side, records the applied discount on `orders.discount_amount`, writes an immutable coupon application snapshot, and marks the user coupon used in the same transactional flow.
- First-pass consistency protection covers duplicate claim, over-claim, duplicate use, cross-shop misuse, and rollback if order creation fails.

Implemented frontend surfaces:

- Buyer coupon entry at `/app/coupons`, linked from the existing profile/me area.
- Checkout coupon selector using `userCouponId`.
- Seller marketing management at `/app/seller/marketing`, linked from the existing profile/me seller area.
- Admin marketing review at `/admin/marketing`, wired through the existing admin route/sidebar/permission model.
- Shop page display for approved effective activities and available coupons.

Documentation completed:

- Added `docs/09-api-spec/marketing.md`.
- Updated `docs/09-api-spec/order.md`, `docs/09-api-spec/admin.md`, `docs/09-api-spec/README.md`, and `docs/README.md`.
- Added `docs/06-http/marketing.http` and updated `docs/06-http/order.http`.
- Updated roadmap notes and `CHANGELOG.md`.

Verification:

- `backend/.\\mvnw.cmd test` passed: 150 tests, 0 failures, 0 errors.
- `frontend/npm test` passed: 7 files, 39 tests.
- `frontend/npm run build` passed.

Acceptance criteria:

- Fixed and threshold coupon submission: covered.
- Shop activity submission: covered.
- Admin review gate for claim/display: covered.
- Buyer claim and owned-coupon list: covered.
- Checkout/order preview and create with one valid coupon: covered.
- Server-side coupon revalidation and persisted discount: covered.
- Duplicate claim, over-claim, duplicate use: covered by backend tests.
- Public activity display: covered.
- Backend permission boundary: covered.
- Frontend route/build coverage: covered by tests/build.
- Activity statistics and complex rule engine: not introduced.

Residual risks:

- Consistency protection is the first single-node JDBC/MySQL/H2-compatible version using transactional writes, uniqueness constraints, conditional updates, and synchronized order persistence. A future distributed deployment should revisit this with database row locking or a dedicated reservation strategy.
- No seed marketing rows were added; smoke examples require creating and approving rows before using downstream IDs.
