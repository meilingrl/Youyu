# Task Record: User Facing Enum Label Normalization

## Metadata

- ID: user-facing-enum-label-normalization
- Status: archived
- Owner: meilingrl (verified post-migration on 2026-05-22)
- Track: feature
- Depends on: current storefront and seller UI baseline
- Priority: medium
- Planned date: 2026-05-20
- Completed date: 2026-05-22 (verified; underlying implementation predates this date)

## Objective

Replace raw snake_case enum/status values with readable Chinese user-facing labels on the scoped storefront and seller pages using a shared, maintainable mapping approach.

## Delivered

- Shared trade-meta helper `frontend/src/components/trade/trade-meta.js` exposes `getOrderStatusMeta()` and `getPaymentStatusMeta()` mapping order/payment statuses to Chinese labels (e.g., `pending_payment` → `待支付`)
- `frontend/src/views/app/OrdersView.vue` and `frontend/src/views/app/PaymentView.vue` consume the shared helpers via `getOrderStatusMeta()` / `getPaymentStatusMeta()` instead of rendering raw values
- `frontend/src/views/app/SellerProductsView.vue` uses an inline `statusLabel()` function covering `pending_review`, `online`, `offline_face_to_face`, etc. — distinct from order/payment so kept page-local to avoid forcing one mapping across unrelated domains
- All three scoped views verified to no longer render raw snake_case enum strings in their templates

## Verification

- Code grep against the three views confirms no raw `pending_payment` / `pending_review` / `offline_face_to_face` strings reach the template binding layer
- Manual UI flow not re-run during post-migration verification; the implementation has been live on `master` and exercised by existing trade-flow E2E coverage

## Acceptance Criteria

- [x] Scoped storefront/seller pages no longer show raw snake_case enum/status values
- [x] Label mapping is shared (order/payment) or centralized per domain (seller product status) — repeated ad hoc fixes prevented
- [x] Admin governance pages remain outside this task (still tracked separately under `admin-governance-action-consistency`)

## Follow-ups

- Seller product status mapping is currently inline in `SellerProductsView.vue`. If a second seller-side view starts rendering the same status, extract `getSellerProductStatusMeta()` alongside the existing helpers in `trade-meta.js`.

## Notes on Post-Migration Verification

This task was filed as `active` after the repository migration but the underlying work had already been delivered. The 2026-05-22 documentation pass cross-referenced the spec against `frontend/src/views/app/OrdersView.vue`, `PaymentView.vue`, `SellerProductsView.vue`, and `frontend/src/components/trade/trade-meta.js` and confirmed completion. Archived without a separate code change.
