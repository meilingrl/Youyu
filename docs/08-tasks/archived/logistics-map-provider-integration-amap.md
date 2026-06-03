# Task: Logistics Tracking And Amap Map Integration

## Metadata

- ID: logistics-map-provider-integration-amap
- Status: completed
- Owner: worker-logistics
- Track: cross-cutting
- Depends on: feature-polish-refund-logistics-map-reconciliation
- Priority: P0
- Planned date: 2026-06-03
- Completed date: 2026-06-03

## Objective

Integrate provider-backed logistics tracking with a real map API so logistics orders can show shipment events and a map view without pretending that map rendering alone is live package tracking.

## Background

The current order API can expose persisted fulfillment fields such as `addressSnapshot`, `trackingNo`, `logisticsCompany`, and `shippedAt`. The current UI is allowed to show an honest fallback when no map provider is configured, but the human now requires the logistics experience to connect to a real map API.

Map APIs and logistics tracking APIs are separate responsibilities:

- A map provider, preferably Amap/高德 for the current China campus scenario, renders maps, markers, geocoding, routes, and optional track lines.
- A logistics provider such as Kuaidi100 or Kdniao provides shipment events for a tracking number and carrier.

This task must add a provider boundary for both. It must not present a route line or moving package marker as real tracking unless the data comes from provider-backed logistics events.

## Scope

- Add backend configuration for Amap and one logistics tracking provider boundary.
- Add a logistics tracking service that can fetch or represent shipment events for logistics orders.
- Persist or cache logistics events only if an additive, test-safe schema/runtime strategy is included.
- Return logistics/map data from order detail without breaking existing order response fields.
- Add frontend order-detail map rendering using the configured Amap JS API path.
- Keep a clear fallback state when provider keys are missing, provider requests fail, or a logistics order has no tracking number.
- Update API docs and HTTP samples.

## Out of Scope

- Replacing the current order lifecycle.
- Real-time courier GPS location unless the chosen logistics provider contract actually returns that data.
- Fake animated package movement based on guessed coordinates.
- Production credential management beyond environment-variable configuration and frontend key domain restrictions.
- International logistics provider support.
- Broad payment/refund changes.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/04-standards/development-process.md`
- `docs/02-requirements/order-lifecycle-and-fulfillment.md`
- `docs/09-api-spec/order.md`
- `docs/09-api-spec/payment.md`
- `docs/06-http/order.http`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/schema.sql`
- `backend/src/main/java/com/youyu/backend/controller/order/OrderController.java`
- `backend/src/main/java/com/youyu/backend/controller/order/AdminOrderController.java`
- `backend/src/main/java/com/youyu/backend/service/order/OrderService.java`
- `backend/src/main/java/com/youyu/backend/service/order/impl/OrderServiceImpl.java`
- `backend/src/main/java/com/youyu/backend/entity/order/OrderFulfillment.java`
- `backend/src/main/java/com/youyu/backend/mapper/order/OrderMapper.java`
- `backend/src/main/java/com/youyu/backend/mapper/order/impl/JdbcOrderMapper.java`
- `frontend/src/views/app/OrdersView.vue`
- `frontend/src/views/app/OrderDetailView.vue`
- `frontend/src/components/trade/TradeOrderCard.vue`
- `frontend/src/api/modules/order.js`
- `frontend/src/utils/market-normalizers.js`

## Allowed Changes

- Order/trade frontend files listed above and a focused logistics/map component if needed.
- Backend order/fulfillment service, mapper, config, and narrow provider-adapter files needed for logistics tracking and map payload generation.
- Additive schema support only if required for logistics events; do not use destructive DDL.
- Frontend environment/config files only for public Amap JS key or map enable flags.
- `docs/09-api-spec/order.md`
- `docs/06-http/order.http`
- Focused backend/frontend tests.

Do not modify product search, Meilisearch integration, admin export, user auth, or unrelated frontend pages.

## Implementation Plan

1. Inspect current order detail fulfillment payload and admin ship flow.
2. Define provider configuration:
   - backend Amap WebService key, if server-side geocoding is needed
   - frontend Amap JS key/security config with domain restriction guidance
   - logistics provider type, app key/secret, and disabled fallback
3. Add a backend provider boundary:
   - `LogisticsTrackingProvider` interface
   - disabled/mock-safe implementation for tests
   - one concrete provider adapter or a documented stub ready for Kuaidi100/Kdniao credentials
4. Add logistics tracking response fields to order detail:
   - tracking number and company
   - event list with time, status text, location text, and optional coordinates
   - map provider status
   - map markers/polyline only when coordinates are provider-derived or geocoded with clear confidence
5. Add frontend order-detail map UI:
   - render real Amap map when configured
   - render markers/events from API data
   - show clear fallback when map key, tracking number, or provider data is unavailable
6. Keep offline and digital fulfillment out of logistics-map behavior.
7. Update docs, `.http` samples, and focused tests.

## Risks

- A map API does not equal real shipment tracking; the UI must not claim live package location without provider data.
- Frontend map keys can leak if not domain-restricted; backend service keys must stay server-side.
- Provider API failures must not break order detail.
- Persisting provider events can create schema and retention obligations; keep changes additive and documented.
- H2 tests must not perform external network calls.

## Test Plan

- Backend: `cd backend; .\mvnw.cmd test`
- Frontend: `cd frontend; npm test`
- Frontend build: `cd frontend; npm run build`
- API validation:
  - logistics order detail with tracking fields
  - order detail with provider disabled
  - order detail with missing tracking number
  - admin ship endpoint still works
- Manual:
  - configure Amap frontend key and verify `/app/orders/{id}` renders a real map for a logistics order
  - verify fallback copy/state when keys are absent
  - verify offline/digital orders do not display fake logistics tracking

## Acceptance Criteria

- [x] Logistics order detail exposes provider-status, tracking event, and map-display fields without breaking existing order fields.
- [x] Amap-backed frontend map renders only when a map key/config is present.
- [x] Logistics tracking data comes from a provider boundary or an explicitly disabled/test implementation; fake live tracking is not shown.
- [x] Provider failures degrade to a clear order-detail fallback and do not fail the whole order-detail request.
- [x] Backend tests do not require network access or real provider credentials.
- [x] Offline and digital orders do not show logistics map UI.
- [x] Order API docs and order `.http` examples are updated.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] `docs/06-http/order.http`
- [x] `docs/09-api-spec/order.md`
- [x] `backend/README.md` if new environment variables are added
- [x] frontend README or env documentation if frontend map key configuration is added
- [x] task status and archive move

## Completion Notes

- Added backend logistics provider configuration with disabled default behavior and a Kdniao-ready provider adapter gated by credentials.
- Added logistics-only order detail payloads: `logisticsTracking` and `logisticsMap`.
- Kept offline and digital orders out of logistics tracking/map UI by returning `null` logistics payloads and rendering the frontend component only for logistics orders.
- Added an Amap JS frontend map panel that loads only when `VITE_AMAP_JS_KEY` is present and the backend map payload is ready with provider-derived coordinates.
- Provider failures and disabled credentials degrade to explicit fallback messages without failing order detail.
- Updated `docs/09-api-spec/order.md`, `docs/06-http/order.http`, `backend/README.md`, `frontend/README.md`, and `CHANGELOG.md`.
- Verification run:
  - `backend/.\\mvnw.cmd -Dtest=YouyuBackendApplicationTests#logisticsOrderShipAndReceiptFlow test` passed.
  - `frontend/npm run build` passed.
  - Main-agent verification: `cd backend; .\mvnw.cmd test` passed, 226 tests.
  - Main-agent verification: `cd frontend; npm test` passed, 63 tests.
  - Main-agent verification: `cd frontend; npm run build` passed.
