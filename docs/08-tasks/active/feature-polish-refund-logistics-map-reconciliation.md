# Task: Refund, Logistics, and Map Reconciliation

## Metadata

- ID: feature-polish-refund-logistics-map-reconciliation
- Status: active
- Owner: worker-wave-2
- Track: cross-cutting
- Depends on: feature-polish-closeout-parent
- Priority: P1
- Planned date: 2026-06-03
- Completed date:

## Objective

Make refund usable and clarify logistics/map integration so the trade/order experience does not present unavailable provider-backed functionality as complete.

## Background

The user reports:

- Refund currently does not work.
- Logistics and map are not connected.

The backend already exposes refund endpoints, including buyer refund application and admin refund completion. This task must start from live behavior and logs before changing code.

## Scope

- Reproduce the refund failure from the buyer UI/API.
- Trace refund state transitions through buyer order detail/list, backend service, payment/refund records, and admin completion.
- Fix the smallest root cause that makes refund usable in the current mock/payment baseline.
- Make logistics information visible and coherent when fulfillment data exists.
- Define a map-provider boundary and graceful fallback for missing provider keys.
- If actual map provider integration is deferred, make the UI explicit and useful without pretending live map capability exists.

## Out of Scope

- Real payment-gateway refund settlement beyond the current project payment baseline.
- Production logistics-provider integration without credentials, provider choice, and security review.
- Real-time shipment tracking unless a provider contract is approved.
- Changing payment architecture broadly.

## Files to Read

- `frontend/src/views/app/OrdersView.vue`
- `frontend/src/views/app/OrderDetailView.vue`
- `frontend/src/views/app/TradeView.vue`
- `frontend/src/components/trade/TradeOrderCard.vue`
- `frontend/src/api/modules/order.js`
- `frontend/src/api/modules/payment.js`
- `backend/src/main/java/com/youyu/backend/controller/order/OrderController.java`
- `backend/src/main/java/com/youyu/backend/controller/order/AdminOrderController.java`
- `backend/src/main/java/com/youyu/backend/service/order/OrderService.java`
- `backend/src/main/java/com/youyu/backend/service/order/impl/OrderServiceImpl.java`
- `backend/src/main/java/com/youyu/backend/entity/payment/RefundRecord.java`
- `backend/src/main/java/com/youyu/backend/common/enums/OrderStatus.java`
- `backend/src/main/resources/schema.sql`
- `docs/09-api-spec/order.md`
- `docs/09-api-spec/payment.md`
- `docs/06-http/order.http`

## Allowed Changes

- Order/trade frontend files listed above.
- Order/payment backend files listed above only after root-cause diagnosis.
- Add a narrow provider-adapter/fallback configuration only if needed for map/logistics.
- `docs/09-api-spec/order.md`
- `docs/09-api-spec/payment.md`
- `docs/06-http/order.http`
- focused backend/frontend tests.

## Implementation Plan

1. Reproduce refund failure with a seeded paid/completed order and record exact API response/log/error.
2. Identify whether the failure is route ID validation, frontend payload, backend state transition, refund record persistence, payment callback state, or admin completion.
3. Fix the smallest responsible layer and add regression coverage.
4. Inspect fulfillment fields currently returned to buyers and admins.
5. Add logistics display/fallback and map placeholder/provider boundary as appropriate.
6. Document any provider-backed capability that remains deferred.

## Risks

- Refund behavior is financial-state logic; UI-only fixes are not sufficient.
- Logistics/map can become misleading if demo data is shown as live tracking.
- Provider SDKs can add dependency and secret-management risk; prefer adapter boundary and fallback first.

## Test Plan

- Backend: `cd backend; .\mvnw.cmd test`
- Frontend: `cd frontend; npm test`
- Frontend build: `cd frontend; npm run build`
- API validation: refund apply, admin refund completion, order detail with fulfillment.
- Manual: buyer applies refund, order status updates, admin can complete/inspect refund, buyer sees final status.
- Manual: order detail displays logistics info and map fallback/provider state clearly.

## Acceptance Criteria

- [ ] Refund failure has a documented root cause before code changes.
- [ ] Buyer refund application works for an eligible order and gives clear feedback for ineligible orders.
- [ ] Admin refund completion or current equivalent path works and is permission-protected.
- [ ] Logistics details render from actual fulfillment data.
- [ ] Map UI has a clear provider-configured path or an honest fallback.
- [ ] Contracts and HTTP samples are updated for changed behavior.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] `docs/06-http/order.http`
- [ ] `docs/09-api-spec/order.md`
- [ ] `docs/09-api-spec/payment.md` if payment/refund contract changes
- [ ] task status and archive move

## Completion Notes

