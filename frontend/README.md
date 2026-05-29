# Frontend

Vue 3 + Vite frontend for Youyu. Uses Pinia for state, Axios for HTTP, Vue Router 4 for routing, Element Plus for UI components.

## Commands

```bash
npm ci                  # install dependencies
npm run dev             # dev server at http://localhost:5173, proxies /api to :8080
npm run build           # production build
npm run preview         # preview production build locally
npm test                # Vitest unit tests
npm run test:watch      # Vitest in watch mode
npm run test:e2e        # Playwright E2E tests (requires backend + frontend running)
npm run test:all        # Vitest + Playwright
```

## Directory conventions

```
src/
  views/
    app/        user-facing pages (home, products, cart, orders, support tickets, reviews, etc.)
    admin/      10 admin pages (dashboard, users, products, orders, etc.)
    auth/       LoginView, RegisterView
  components/
    common/     EmptyState, ErrorBlock, SkeletonCard, PageSection, ReviewForm, etc.
    explore/    ExploreProductCard, ExploreSearchShell, FeaturedShopsSection
    layout/     AppHeader, AppFooter, MobileNav, MobileBottomNav, AdminSidebar, AdminTopbar
    search/     HotSearchList, SearchSuggestInput
    shell/      ListPageShell, FormPageShell
    trade/      TradePageShell, TradeOrderCard, TradeReviewCard, TradeMetricStrip, TradeStatusTag
  layouts/      AppLayout.vue, AdminLayout.vue
  stores/       auth, market, search, recommend, review, app
  router/       index.js, guards.js, modules/app.js, modules/admin.js
  api/          client.js + domain modules
  utils/        auth.js, storage.js, market-normalizers.js, error-utils.js
  constants/    navigation.js, insightMetrics.js
  styles/       variables.css, index.css
  plugins/      element-plus.js, element-plus-services.js
  mocks/        market.js
```

## Key conventions

- State: Pinia stores with `ref()` state, `async function` actions, `computed` derived state, try/catch/finally for loading flags
- API calls: always go through `src/api/modules/` — thin wrappers around the Axios client
- Data normalization: `utils/market-normalizers.js` converts snake_case API responses to camelCase; normalize at the store boundary, not in components
- Empty/error/loading states: use `EmptyState.vue`, `ErrorBlock.vue`, `SkeletonCard.vue` — never leave a blank area
- New views: add to the appropriate route module (`router/modules/app.js` or `router/modules/admin.js`) with correct `meta` fields (`title`, `requiresAuth`, `role`, `navKey`, `public`, `hiddenInNav`)

## Architecture

See `CLAUDE.md` at the repository root for the full store list, route map, and coding conventions.
