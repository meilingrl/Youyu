# Task: HomeView Featured Rail Redesign

## Metadata

- ID: home-featured-rail-redesign
- Status: archived
- Owner: unassigned
- Track: feature
- Depends on: current HomeView baseline, `ui-ux-constitution.md`, `frontend-information-architecture.md`
- Priority: high
- Planned date: 2026-05-22
- Completed date: 2026-05-22

## Objective

Redesign `frontend/src/views/app/HomeView.vue` to feel like editorial commerce — not a card-stack utility page. The visual core of the new home is a `HomeFeaturedRail` component: a horizontally scrolling, variable-width, scroll-snapping editorial strip of featured products. The rest of the page (hero, trust) becomes lighter and more breathing.

## Background

The current homepage looks like a standard shell-card-stacked page:
- Hero has a two-column layout with a right panel of three equal metric cards
- "精选好物" is a standard 3-column ExploreProductCard grid, identical to explore page
- Three CTA buttons compete with equal weight
- Guide cards and trust cards are interchangeable shell-cards
- Hot search occupies a full PageSection
- No horizontal scroll, no lead/accent card rhythm

Target: a page where the user immediately feels the platform has personality and the "精选好物" rail is the visual center of gravity.

## Reference Standard

From `docs/03-architecture/ui-ux-constitution.md`:

- §9.2 Apple-style featured display: "strong horizontal scroll rhythm, clear primary/secondary cards, stable inter-card spacing, clean CTA, beautiful deceleration near stop points"
- §8.1 Product card: browsing and scanning, don't cram every field, stable image container/ratio
- §6.1 Action hierarchy: one primary CTA per viewport, exploratory entries prefer card-click or text links
- §5 Glass: use on navigation, modals, search shell — NOT on every product card
- §3.3 Narrative structure: understand → key entry → explorable content → trust → natural next step

## New Page Structure

```
HomeView
├── 1. Hero (no shell-card wrapper, full-width area)
│   ├── eyebrow badge
│   ├── h1 headline
│   ├── one-line tagline (p)
│   ├── SearchSuggestInput (prominent, hero-scale)
│   ├── hot-chips row (5 keywords, small pill chips, below search)
│   └── 2 CTAs: "探索好物" (primary) + "学生认证" (plain, subordinate)
│
├── 2. HomeFeaturedRail (full container-width, horizontal scroll)
│   ├── section header: "精选好物" (left) + "查看全部 →" text link (right)
│   └── scrollable track: [lead card 360px] [accent card 260px] [accent] [accent]...
│       - scroll-snap-type: x mandatory
│       - gradient fade mask at right (and left when scrolled)
│       - each card: full-bleed product image + bottom gradient overlay + title + price
│
├── 3. Trust strip (compact horizontal, NOT shell-cards)
│   └── 3 inline pillars: icon + title + one-sentence desc
│
└── (Removed: guide cards, metric panel, FeaturedShopsSection, standalone hot search section)
```

## Files to Modify or Create

### Create
- `frontend/src/components/home/HomeFeaturedRail.vue`

### Modify
- `frontend/src/views/app/HomeView.vue`

### Do NOT touch
- `frontend/src/components/explore/ExploreProductCard.vue`
- `frontend/src/components/explore/FeaturedShopsSection.vue`
- `frontend/src/components/search/SearchSuggestInput.vue`
- `frontend/src/stores/market.js`, `recommend.js`, `search.js`
- `frontend/src/styles/variables.css`, `index.css` (only add scoped styles in components)
- All backend files, API modules, route files

## Component Spec: HomeFeaturedRail.vue

### Props

```javascript
defineProps({
  products: { type: Array, default: () => [] },   // normalized product objects from recommendStore
  loading:  { type: Boolean, default: false }
})

defineEmits(['open-product'])
```

### Template structure

```html
<section class="featured-rail" aria-label="精选好物">
  <div class="featured-rail__header shell-container">
    <div class="featured-rail__title-row">
      <h2>精选好物</h2>
      <router-link to="/app/explore" class="featured-rail__more">查看全部 →</router-link>
    </div>
  </div>

  <!-- Loading skeleton -->
  <div v-if="loading" class="featured-rail__track featured-rail__track--skeleton">
    <div v-for="i in 5" :key="i" class="featured-rail__skeleton"
         :class="i === 1 ? 'featured-rail__skeleton--lead' : ''" />
  </div>

  <!-- Real rail -->
  <div v-else-if="products.length" class="featured-rail__track" role="list">
    <article
      v-for="(product, i) in products"
      :key="product.id"
      class="featured-rail__card"
      :class="i === 0 ? 'featured-rail__card--lead' : 'featured-rail__card--accent'"
      role="listitem"
      @click="$emit('open-product', product)"
    >
      <div class="featured-rail__media">
        <img
          :src="product.coverUrl || product.cover"
          :alt="product.title"
          loading="lazy"
          decoding="async"
        />
        <div class="featured-rail__overlay">
          <div class="featured-rail__chips">
            <span class="featured-rail__chip">{{ product.categoryName || '校园精选' }}</span>
          </div>
          <div class="featured-rail__info">
            <h3>{{ product.title }}</h3>
            <div class="featured-rail__meta">
              <strong class="featured-rail__price">￥{{ formatPrice(product.salePrice ?? product.price) }}</strong>
              <span class="featured-rail__shop">{{ product.shopName || '校园卖家' }}</span>
            </div>
          </div>
        </div>
      </div>
    </article>
  </div>
</section>
```

### CSS spec for HomeFeaturedRail

```css
/* Rail wrapper: full container width, handles mask */
.featured-rail {
  display: grid;
  gap: 18px;
  overflow: hidden;           /* clips the track overflow */
  -webkit-mask-image: linear-gradient(to right, black 85%, transparent 100%);
  mask-image: linear-gradient(to right, black 85%, transparent 100%);
}

/* Header: constrained to shell-container width */
.featured-rail__header {
  /* inherits .shell-container centering */
}

.featured-rail__title-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
}

.featured-rail__title-row h2 {
  margin: 0;
  font-size: clamp(20px, 1.8vw, 24px);
  font-weight: 600;
}

.featured-rail__more {
  color: var(--cm-primary);
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  transition: opacity var(--cm-transition-micro);
}

.featured-rail__more:hover { opacity: 0.7; }

/* The scrollable track */
.featured-rail__track {
  display: flex;
  gap: 14px;
  overflow-x: scroll;
  scroll-snap-type: x mandatory;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
  /* Left padding matches shell-container start */
  padding-left: max(20px, calc((100vw - min(1240px, 100vw - 40px)) / 2 + 20px));
  padding-right: 80px;       /* generous right trailing space */
  padding-bottom: 16px;      /* reveal box-shadow at bottom */
}

.featured-rail__track::-webkit-scrollbar { display: none; }

/* Cards */
.featured-rail__card {
  flex-shrink: 0;
  scroll-snap-align: start;
  scroll-snap-stop: always;  /* always stop at each card, no skip */
  border-radius: var(--cm-radius-lg);      /* 24px */
  overflow: hidden;
  cursor: pointer;
  transition:
    transform var(--cm-duration-standard) var(--cm-ease-standard),
    box-shadow var(--cm-duration-standard) var(--cm-ease-standard);
  box-shadow: 0 12px 32px rgba(88, 62, 43, 0.10);
}

.featured-rail__card:hover {
  transform: translateY(-6px);
  box-shadow: 0 24px 52px rgba(88, 62, 43, 0.18);
}

/* Lead card: wider */
.featured-rail__card--lead {
  width: min(360px, 82vw);
  height: 480px;
}

/* Accent cards */
.featured-rail__card--accent {
  width: 260px;
  height: 480px;
}

/* Full-bleed media container */
.featured-rail__media {
  position: relative;
  width: 100%;
  height: 100%;
  background:
    linear-gradient(180deg, rgba(255, 249, 243, 0.2), rgba(233, 217, 198, 0.52)),
    #f0e6da;
}

.featured-rail__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--cm-duration-feature) var(--cm-ease-standard);
  /* duration-feature = 420ms */
}

.featured-rail__card:hover .featured-rail__media img {
  transform: scale(1.04);
}

/* Bottom gradient overlay with product info */
.featured-rail__overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 18px;
  background: linear-gradient(
    to top,
    rgba(36, 25, 20, 0.80) 0%,
    rgba(36, 25, 20, 0.20) 40%,
    transparent 60%
  );
  pointer-events: none;
}

.featured-rail__chips {
  display: flex;
  gap: 8px;
  align-self: flex-start;
}

.featured-rail__chip {
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(255, 252, 247, 0.9);
  color: var(--cm-text);
  font-size: 12px;
  font-weight: 700;
  backdrop-filter: blur(10px);
}

.featured-rail__info {
  display: grid;
  gap: 8px;
  transform: translateY(0);
  transition: transform var(--cm-duration-standard) var(--cm-ease-enter);
}

.featured-rail__card:hover .featured-rail__info {
  transform: translateY(-4px);
}

.featured-rail__info h3 {
  margin: 0;
  color: #fff;
  font-size: 16px;
  line-height: 1.35;
  font-weight: 600;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.featured-rail__meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.featured-rail__price {
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.featured-rail__shop {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Skeleton */
.featured-rail__skeleton {
  flex-shrink: 0;
  width: 260px;
  height: 480px;
  border-radius: var(--cm-radius-lg);
  background: linear-gradient(
    90deg,
    rgba(240, 232, 224, 0.8) 25%,
    rgba(248, 243, 236, 0.95) 50%,
    rgba(240, 232, 224, 0.8) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.6s ease-in-out infinite;
}

.featured-rail__skeleton--lead {
  width: min(360px, 82vw);
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* Mobile */
@media (max-width: 768px) {
  .featured-rail__card--lead  { width: min(300px, 82vw); height: 420px; }
  .featured-rail__card--accent { width: min(220px, 65vw); height: 420px; }
  .featured-rail__track {
    padding-left: 20px;
    padding-right: 60px;
  }
}
```

## Hero Redesign Spec

### Remove
- Two-column grid layout (right panel with metric cards)
- Three CTA buttons → reduce to two
- `guideCards` section
- The `heroMetrics` computed and `marketStore.loadProducts()` call

### New Hero Template

```html
<section class="home-hero">
  <div class="home-hero__inner shell-container">
    <span class="eyebrow">Youyu</span>
    <h1 class="home-hero__title">校园里的好物，<br>都在这里。</h1>
    <p class="home-hero__desc">
      学生专属的二手与新品交易平台——认证身份、轻松发布、安心买卖。
    </p>

    <div class="home-hero__search">
      <SearchSuggestInput
        v-model="homeKeyword"
        placeholder="搜索热搜关键词或直接去探索"
        button-label="去探索"
        :suggestions="searchStore.suggestions"
        :loading="searchStore.loadingSuggestions"
        :error="searchStore.suggestionError"
        @change="handleHomeKeywordChange"
        @submit="goToSearch"
        @select-suggestion="handleHomeSuggestionSelect"
      />
    </div>

    <!-- Hot keyword chips: inline, subordinate, NOT a full section -->
    <div v-if="searchStore.hotKeywords.length" class="home-hero__hot">
      <span class="home-hero__hot-label">热搜：</span>
      <button
        v-for="kw in searchStore.hotKeywords.slice(0, 5)"
        :key="kw.keyword"
        type="button"
        class="home-hero__hot-chip"
        @click="goToSearch(kw.keyword)"
      >{{ kw.keyword }}</button>
    </div>

    <div class="home-hero__actions">
      <el-button type="primary" size="large" @click="$router.push('/app/explore')">探索好物</el-button>
      <el-button plain size="large" @click="$router.push('/app/verification')">学生认证</el-button>
    </div>
  </div>
</section>
```

### Hero CSS

```css
.home-hero {
  padding: 64px 0 56px;
  background:
    radial-gradient(circle at 15% 30%, rgba(182, 95, 59, 0.10), transparent 40%),
    radial-gradient(circle at 85% 10%, rgba(196, 122, 44, 0.08), transparent 32%),
    linear-gradient(180deg, #fffaf3 0%, #f7efe5 60%, #efe2d3 100%);
}

/* home-hero__inner uses shell-container for centering but is NOT a shell-card */

.home-hero__inner {
  display: grid;
  gap: 24px;
  max-width: 680px;   /* constrain text width for editorial feel */
}

.home-hero__title {
  font-size: clamp(36px, 4.5vw, 56px);
  line-height: 1.18;
  letter-spacing: -0.02em;
  font-weight: 700;
  color: var(--cm-text);
  /* text-wrap: balance; -- if supported */
}

.home-hero__desc {
  color: var(--cm-text-secondary);
  font-size: clamp(15px, 1.2vw, 17px);
  line-height: 1.75;
  max-width: 520px;
}

.home-hero__search {
  max-width: 600px;
  margin-top: 4px;
}

/* Hot chips row */
.home-hero__hot {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: -6px;   /* pull closer to search */
}

.home-hero__hot-label {
  color: var(--cm-text-secondary);
  font-size: 13px;
  white-space: nowrap;
}

.home-hero__hot-chip {
  appearance: none;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid var(--cm-border);
  background: rgba(255, 252, 248, 0.9);
  color: var(--cm-text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition:
    color var(--cm-transition-micro),
    border-color var(--cm-transition-micro),
    background var(--cm-transition-micro);
}

.home-hero__hot-chip:hover {
  color: var(--cm-primary);
  border-color: rgba(var(--cm-primary-rgb), 0.3);
  background: rgba(var(--cm-primary-rgb), 0.05);
}

.home-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
}

/* Reduce secondary CTA visual weight */
.home-hero__actions .el-button.is-plain {
  box-shadow: none;
}

@media (max-width: 768px) {
  .home-hero {
    padding: 48px 0 40px;
  }

  .home-hero__title {
    font-size: clamp(30px, 8vw, 40px);
    line-height: 1.22;
  }
}
```

## Trust Strip Redesign Spec

Replace the 3-column `shell-card` grid with a compact horizontal strip.

### Template

```html
<section class="home-trust shell-container">
  <ul class="home-trust__list">
    <li v-for="item in trustItems" :key="item.title" class="home-trust__pillar">
      <span class="home-trust__icon" aria-hidden="true">{{ item.icon }}</span>
      <div class="home-trust__copy">
        <strong>{{ item.title }}</strong>
        <span>{{ item.desc }}</span>
      </div>
    </li>
  </ul>
</section>
```

### Trust items data (update in script)

```javascript
const trustItems = [
  { icon: '🎓', title: '学生专属', desc: '校园身份认证后参与交易，买卖双方都有真实背景。' },
  { icon: '🔒', title: '交易有保障', desc: '订单全程可追踪，支持退款售后与平台介入。' },
  { icon: '✨', title: '随时开店', desc: '认证后即可发布商品，轻松经营自己的校园小店。' }
]
```

### Trust CSS

```css
.home-trust {
  padding-block: 8px;  /* minimal vertical space */
}

.home-trust__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.home-trust__pillar {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 20px;
  border-radius: var(--cm-radius-md);   /* 20px */
  background: rgba(255, 252, 248, 0.72);
  border: 1px solid var(--cm-border);
  backdrop-filter: blur(10px);
}

.home-trust__icon {
  font-size: 24px;
  line-height: 1;
  flex-shrink: 0;
  margin-top: 2px;
}

.home-trust__copy {
  display: grid;
  gap: 4px;
}

.home-trust__copy strong {
  font-size: 15px;
  font-weight: 700;
  color: var(--cm-text);
}

.home-trust__copy span {
  font-size: 13px;
  color: var(--cm-text-secondary);
  line-height: 1.6;
}

@media (max-width: 768px) {
  .home-trust__list {
    grid-template-columns: 1fr;
  }
}

@media (min-width: 769px) and (max-width: 1100px) {
  .home-trust__list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
```

## Updated Script Section for HomeView

### Data loading changes

```javascript
// Remove: marketStore.loadProducts() — no product grid or heroMetrics on home
// Remove: buildFeaturedShops, featuredShops computed
// Remove: heroMetrics computed
// Remove: guideCards

// Keep:
// - recommendStore.loadHomeRecommend(8)  — for rail, increase to 8
// - searchStore.loadHotKeywords()
// - searchStore.loadSuggestions() for search input

async function loadHomePage() {
  loading.value = true
  loadError.value = false
  try {
    await Promise.all([
      recommendStore.loadHomeRecommend(8).catch(() => []),
      searchStore.loadHotKeywords().catch(() => [])
    ])
  } catch (error) {
    loadError.value = true
    ElMessage.error(error?.response?.data?.message || error?.message || '首页数据加载失败')
  } finally {
    loading.value = false
  }
}
```

### Computed changes

```javascript
// Remove: spotlightProducts, heroMetrics, featuredShops
// Add: rail uses recommendStore.homeRecommendList directly (up to 8 items)
```

### Removed imports

```javascript
// Remove: PageSection, ExploreProductCard, FeaturedShopsSection, buildFeaturedShops, useMarketStore
// Add: HomeFeaturedRail
```

## Complete HomeView Template Structure

```html
<template>
  <div>
    <!-- 1. Hero -->
    <section class="home-hero"> ... </section>

    <!-- 2. Error / Empty states (shown only if rail would be empty) -->
    <div v-if="loadError" class="shell-container" style="padding-top: 24px">
      <ErrorBlock @retry="loadHomePage" />
    </div>

    <!-- 3. Featured Rail (primary visual core) -->
    <HomeFeaturedRail
      v-if="!loadError"
      :products="recommendStore.homeRecommendList"
      :loading="loading"
      @open-product="openProduct"
    />

    <!-- 4. Trust strip -->
    <section class="home-trust shell-container"> ... </section>
  </div>
</template>
```

Note: the outer `<div>` replaces `<div class="page-stack">`. The page-stack gap is replaced by section-level padding/margin so each section can control its own spacing.

## Animation Summary

| Element | Trigger | Effect | Duration |
|---|---|---|---|
| Rail card | hover | translateY(-6px) + shadow increase | 240ms standard |
| Rail card image | hover | scale(1.04) | 420ms feature |
| Rail card info overlay | hover | translateY(-4px) | 240ms enter |
| Search input | focus | translateY(-2px) + shadow increase | 260ms (existing) |
| Hot chip | hover | color + border-color change | 160ms micro |
| Page load | mount | no entry animation on rail — snap is enough | — |

All transitions use existing `--cm-ease-standard` / `--cm-ease-enter` tokens. No JS-driven animation libraries needed.

## Data Flow (Unchanged Contracts)

```
onMounted → loadHomePage()
  → recommendStore.loadHomeRecommend(8) → homeRecommendList (normalized products)
  → searchStore.loadHotKeywords() → hotKeywords[]

search input change → searchStore.loadSuggestions(keyword) → suggestions[]
hot chip click → goToSearch(keyword) → router.push('/app/explore?keyword=...')
rail card click → openProduct(product) → router.push('/app/products/:id')
```

All through existing stores and API modules. No new API module needed.

## Out of Scope

- Modifying `ExploreProductCard.vue`, `FeaturedShopsSection.vue`, `SearchSuggestInput.vue`
- Modifying any store, API module, or normalizer
- Changing the explore page, product detail, or any other view
- Backend, schema, or route changes
- Adding new UI libraries
- Modifying `frontend/src/styles/variables.css` or `index.css`
- Modifying `AppHeader.vue`, `AppFooter.vue`, or layout files

## Hard Limits

- Do NOT use `style="..."` inline styles in the template except for truly one-off single-property cases (e.g. `padding-top: 24px` on error wrapper)
- Do NOT directly import axios or call fetch — all data through existing stores
- Do NOT modify `recommendStore.homeRecommendList` normalization — consume as-is
- Do NOT fake product data for the rail — if `homeRecommendList` is empty, show skeleton while loading, nothing after load
- Do NOT change scroll behavior on the explore page or shared layout
- Do NOT use JavaScript scroll event listeners for the fade mask — CSS mask-image is sufficient
- Do NOT import or use any new npm package

## Test Plan

### Frontend unit tests
```bash
cd frontend && npm test
```
Must pass with zero failures. If HomeView has existing unit tests, they should still pass or be updated to match the new structure.

### Build check
```bash
cd frontend && npm run build
```
Must succeed with no errors.

### Manual verification checklist

**Desktop (>1100px)**
- [ ] Hero: single column, large title, search box, hot chip row (≤5 keywords), 2 buttons
- [ ] Featured Rail: visible, horizontally scrollable, first card wider than others, all cards 480px height
- [ ] Rail: scroll-snap stops at each card, does not skip
- [ ] Rail: gradient fade visible at right edge
- [ ] Rail card hover: card lifts, image scales smoothly, overlay text shifts up
- [ ] Trust strip: 3 columns, icon + title + desc, compact
- [ ] No shell-card wrapper around hero
- [ ] No guide cards section
- [ ] No standalone hot search section
- [ ] No FeaturedShopsSection

**Mobile (≤768px)**
- [ ] Hero: stacks naturally, title readable, search usable, CTAs wrap correctly
- [ ] Rail: lead card ~82vw, accent cards ~65vw, still scrollable and snapping
- [ ] Trust strip: collapses to 1 column
- [ ] Hot chips wrap without overflow

**Navigation**
- [ ] "探索好物" → `/app/explore`
- [ ] "学生认证" → `/app/verification`
- [ ] Rail card click → `/app/products/:id`
- [ ] "查看全部 →" → `/app/explore`
- [ ] Hot chip click → `/app/explore?keyword=...`

**Data states**
- [ ] While loading: rail shows shimmer skeleton (5 cards matching lead+accent sizing)
- [ ] If loadError: ErrorBlock shown, retry works
- [ ] If homeRecommendList empty after load: rail not shown, no broken layout

## Acceptance Criteria

- [ ] HomePage does NOT look like a standard shell-card-stacked page
- [ ] A horizontally scrollable featured rail exists and is the visual center of gravity
- [ ] Rail uses CSS scroll-snap, not a JS carousel library
- [ ] Rail cards have gradient overlay with product info — not cards with separate text body below image
- [ ] First (lead) card is visibly wider than subsequent (accent) cards
- [ ] Hero has ≤2 CTAs and no right-side metric panel
- [ ] Trust section is compact (not 3 large shell-cards)
- [ ] Hot search appears as inline chip row, not a full PageSection
- [ ] `npm test` passes (zero failures)
- [ ] `npm run build` succeeds
- [ ] No new npm dependencies added
- [ ] No store, API module, or normalizer modified
- [ ] No backend file modified

## Documentation Updates Required

- [x] `CHANGELOG.md` — prepend entry under `### feat`
- [ ] `docs/06-http/` — not applicable
- [ ] `docs/09-api-spec/` — not applicable (no contract change)
- [x] `docs/08-tasks/active/home-featured-rail-redesign.md` → move to `archived/` after completion

## Final Report Format (for implementing agent)

```markdown
## Return Report — home-featured-rail-redesign

### A. Files Changed
- (paste git diff --stat)

### B. Implementation Notes
- (any deviations from this spec, with reason)
- (any data limitations discovered, e.g. homeRecommendList consistently empty in dev)

### C. Test Results
- npm test: (exit code, N passed)
- npm run build: (exit code)

### D. Manual Verification
- (checklist result per item)

### E. Acceptance Criteria Check
- [ ] each criterion with evidence

### F. Out-of-scope Findings
- (none, or items for follow-up tasks)
```

## Completion Notes

- Added `frontend/src/components/home/HomeFeaturedRail.vue` and rebuilt `frontend/src/views/app/HomeView.vue` into the specified Hero -> Featured Rail -> Trust Strip structure.
- Kept the homepage on existing `recommendStore` and `searchStore` contracts only, without touching shared stores, API modules, or global style token files.
- Verified the frontend build passes and re-ran the frontend unit suite after one transient guard-test timeout; the subsequent full run passed.
