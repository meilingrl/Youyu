# Task: Cart Selection and Review Image Upload

## Metadata

- ID: feature-polish-cart-selection-review-images
- Status: completed
- Owner: worker-wave-1
- Track: feature
- Depends on: feature-polish-closeout-parent
- Priority: P0
- Planned date: 2026-06-03
- Completed date: 2026-06-03

## Objective

Add buyer-facing cart select-all / invert-selection controls and support image upload or image attachment for product/shop reviews.

## Background

This task combines two relatively visible trade-page fixes, but it must keep the contracts separate:

- Cart selection is order/cart state.
- Review images are review/media state and may require upload or URL handling.

If review image upload needs a new storage/upload endpoint, document the exact boundary before implementation.

## Scope

- Add cart select-all and invert-selection behavior with correct selected count and selected amount.
- Preserve existing cart quantity, remove, checkout, and error states.
- Add review image upload/attachment in the existing review form flow.
- Reuse existing upload conventions where available, such as avatar/chat image validation patterns.
- Store and display review image metadata consistently in my reviews and product/shop review lists if backend support exists or is added.

## Out of Scope

- Real object-storage migration.
- Moderation AI or image content review.
- Reworking the payment/checkout flow.
- Adding logistics/map behavior; that is owned by `feature-polish-refund-logistics-map-reconciliation`.

## Files to Read

- `frontend/src/views/app/CartView.vue`
- `frontend/src/views/app/TradeView.vue`
- `frontend/src/views/app/PendingReviewsView.vue`
- `frontend/src/components/common/ReviewForm.vue`
- `frontend/src/components/common/ReviewList.vue`
- `frontend/src/stores/review.js`
- `frontend/src/api/modules/order.js`
- `frontend/src/api/modules/review.js`
- `frontend/src/components/chat/ImageUploader.vue`
- `backend/src/main/java/com/youyu/backend/controller/order/CartController.java`
- `backend/src/main/java/com/youyu/backend/controller/review/ReviewController.java`
- `backend/src/main/java/com/youyu/backend/service/review/ReviewService.java`
- `backend/src/main/java/com/youyu/backend/service/review/impl/ReviewServiceImpl.java`
- `backend/src/main/java/com/youyu/backend/mapper/review/ReviewMapper.java`
- `backend/src/main/resources/schema.sql`
- `docs/09-api-spec/cart.md`
- `docs/09-api-spec/review.md`
- `docs/06-http/cart.http`
- `docs/06-http/review.http`

## Allowed Changes

- Cart/review frontend files listed above.
- Review backend controller/service/mapper/schema files only if current review payload cannot persist image metadata.
- Additive schema changes only when approved inside this task and compatible with H2/MySQL tests.
- `docs/09-api-spec/cart.md`
- `docs/09-api-spec/review.md`
- `docs/06-http/cart.http`
- `docs/06-http/review.http`
- focused tests for cart selection and review image payloads.

## Implementation Plan

1. Verify current cart item selected state and update endpoint behavior.
2. Implement select-all and invert-selection using existing cart update actions or a narrow batch helper if needed.
3. Inspect current review persistence for image/media fields.
4. If no backend image support exists, choose one documented minimal path: attach validated image URLs, reuse an existing upload endpoint, or add a narrow review-media upload endpoint.
5. Update review form, review submission, and review display paths.

## Risks

- Batch cart selection can generate many requests; prefer a backend batch endpoint only if needed and documented.
- Review image upload can become fake if the UI accepts files but does not persist/display them.
- Schema changes must stay additive and test-compatible.

## Test Plan

- Backend: `cd backend; .\mvnw.cmd test`
- Frontend: `cd frontend; npm test`
- Frontend build: `cd frontend; npm run build`
- API validation: cart and review `.http` examples when contracts change.
- Manual: select all, invert selection, remove/update quantity after selection, submit review with image, reload and confirm the image remains visible.

## Acceptance Criteria

- [ ] Cart provides select-all and invert-selection controls.
- [ ] Selected count and amount update correctly after select-all, invert, item remove, and quantity changes.
- [ ] Review form accepts only allowed image types/sizes and exposes clear error feedback.
- [ ] Submitted review images persist and display in relevant review surfaces, or the task records a blocker with no fake completion claim.
- [ ] Any review/cart contract changes are documented.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] `docs/06-http/cart.http` if cart API changes
- [ ] `docs/06-http/review.http` if review API changes
- [ ] `docs/09-api-spec/cart.md` if cart API changes
- [ ] `docs/09-api-spec/review.md` if review API changes
- [ ] task status and archive move

## Completion Notes

- Added cart select-all and invert-selection controls with correct selected count and amount updates after item selection changes.
- Added review image attachment support that persists and renders through the existing review flow instead of remaining UI-only.
