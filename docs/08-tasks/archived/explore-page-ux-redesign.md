# Task: Explore Page UX Redesign

## Metadata

- ID: explore-page-ux-redesign
- Status: archived
- Owner: unassigned
- Track: feature
- Depends on: current ProductListView + ExploreSearchShell baseline, `ui-ux-constitution.md`, `frontend-information-architecture.md`
- Priority: high
- Planned date: 2026-05-22
- Completed date: 2026-05-22

## Objective

Redesign the explore page (`ProductListView.vue`) and its search panel (`ExploreSearchShell.vue`) so the page feels like a real market browsing experience — not a feature-description page. The primary fix is removing verbal clutter and wrong-weight sections, restructuring the layout to put product browsing first, and making the search/filter control feel like a unified Airbnb-style discovery tool rather than a nested card-in-card form.

No new component files. Both existing files are modified in place. Prop/emit interfaces of `ExploreSearchShell` are preserved.

## Background

### Current problems (per code audit)

**ExploreSearchShell.vue:**
- Has an h1 hero heading and a long description paragraph that describe what the page does — not useful as UI
- Has a "N 个筛选条件生效" status pill positioned in the top-right corner of the hero, which is awkward and redundant
- Wraps the actual controls in a second inner surface panel (`.explore-search-shell__surface`), creating card-inside-card nesting
- Filter chips and hot keywords are inside this inner panel with a two-column flex layout that breaks at medium widths
- On mobile, the hero text takes up significant screen real estate before the user can see filters

**ProductListView.vue:**
- Has an `explore-overview` shell-card section with h2 ("商品浏览是主场，热搜与店铺是陪逛线索"), a description, and two CTAs ("查看购物车" and "返回首页") — entirely misplaced on an explore/browse page
- Has a "本周逛什么" PageSection that uses `curatedProducts` (first 4 from recommend list) rendered as `ExploreProductCard` — same card as the main grid below it, no editorial differentiation
- Has a sidebar layout (`explore-layout`: main + 320px aside) containing:
  - A "热搜关键词" side card (functional but the sidebar layout hurts mobile)
  - A "逛店铺比只看单品更轻松" text card with bullet-point editorial prose — this is placeholder content dressed as UI
- `FeaturedShopsSection` appears at the bottom, below the full product grid, where few users scroll

### What the explore page should be

Per `docs/03-architecture/frontend-information-architecture.md` §3.2:

> 探索页是 Etsy 式整体市场体验的核心页面，搜索筛选框应学习 Airbnb 的分段式、聚焦式、可扩展体验。

Per `docs/03-architecture/ui-ux-constitution.md` §7 (搜索与筛选):

> 搜索入口不是普通表单，而是用户开始浏览旅程的入口。筛选要帮助探索，而不是变成后台检索表单。选中状态必须明显。移动端不要一次展示过多筛选。

Target experience:
1. Search control is the visual anchor — clean, integrated, shows category + type filters directly as chip rows below the input
2. Hot keywords and history are subordinate discovery hints, separated from filters but still in the same panel
3. Product grid is full-width — the main event of the page
4. FeaturedShops appears above the grid (not at the bottom), and only when no filter is active — it helps browsing, not searching
5. Active filter state is shown as a compact results bar above the grid, not in the search panel header

## New Page Structure

```
ProductListView
│
├── 1. ExploreSearchShell (redesigned — same component, different template + css)
│   ├── Search input row (SearchSuggestInput, full width)
│   ├── Filter rows
│   │   ├── "分类" label + category chip row (horizontal scroll on mobile)
│   │   └── "类型" label + type chip row
│   ├── Discovery row (border-top separator, only rendered when searchHistory or hotKeywords exist)
│   │   ├── "最近" label + history chips (compact)
│   │   └── "热搜" label + hot keyword chips (≤6, compact)
│   └── Active bar (border-top separator, only when activeCount > 0)
│       ├── "N 个条件生效" count (left, primary color)
│       └── "清空全部" text button (right)
│
├── [Error / loading states]
│
├── 2. FeaturedShopsSection (existing component, unchanged)
│   - ONLY when !hasActiveFilters && featuredShops.length > 0
│   - Moved from bottom to above grid
│
├── 3. Results bar (new, compact shell-card)
│   ├── Left: "N 件商品" count + active filter tag chips
│   └── Right: "清空筛选" text button (only when hasActiveFilters)
│
└── 4. Product grid section (full-width, no prose header)
    ├── ExploreProductCard grid (auto-fill, minmax 240px)
    ├── EmptyState (if no results)
    └── el-pagination
```

## Sections Removed

| Removed | Reason |
|---|---|
| `explore-search-shell__hero` (eyebrow + h1 + p) | Verbal clutter, not useful UI |
| `explore-search-shell__status` pill top-right | Replaced by active-bar at bottom of shell |
| Inner `.explore-search-shell__surface` wrapper | Card-in-card anti-pattern |
| `explore-overview` shell-card section | Entirely misplaced CTAs and description |
| "本周逛什么" PageSection + `curatedProducts` | Duplicates product grid card type, no editorial value |
| `explore-layout` sidebar (320px aside) | Hot keywords moved to shell, removes mobile breakage |
| "热搜关键词" side card | Now in shell's discovery row |
| "逛店铺比只看单品更轻松" text card | Pure placeholder prose, no UI value |
| `FeaturedShopsSection` at bottom | Moved to top (above grid, no-filter state only) |

## Files to Modify

### 1. `frontend/src/components/explore/ExploreSearchShell.vue`

**Props and emits: unchanged.** Template and `<style scoped>` are fully replaced.

#### New template

```html
<template>
  <section class="explore-search-shell shell-card" aria-label="搜索与筛选">

    <!-- Row 1: Search input -->
    <div class="explore-search-shell__input-row">
      <SearchSuggestInput
        :model-value="modelValue"
        placeholder="搜索笔记、耳机、宿舍好物或服务"
        button-label="搜索"
        :suggestions="suggestions"
        :loading="loadingSuggestions"
        :error="suggestionError"
        @update:model-value="emit('update:modelValue', $event)"
        @change="emit('change', $event)"
        @submit="emit('submit', $event)"
        @select-suggestion="emit('select-suggestion', $event)"
      />
    </div>

    <!-- Row 2: Filters -->
    <div class="explore-search-shell__filters">
      <div class="explore-search-shell__filter-group">
        <span class="explore-search-shell__label">分类</span>
        <div class="explore-search-shell__chips">
          <button
            v-for="category in categories"
            :key="category.id || 'all-category'"
            type="button"
            class="explore-search-shell__chip"
            :class="{ 'is-active': selectedCategoryId === category.id }"
            @click="emit('select-category', category.id)"
          >
            {{ category.name }}
          </button>
        </div>
      </div>

      <div class="explore-search-shell__filter-group">
        <span class="explore-search-shell__label">类型</span>
        <div class="explore-search-shell__chips">
          <button
            v-for="productType in productTypes"
            :key="productType.id || 'all-type'"
            type="button"
            class="explore-search-shell__chip"
            :class="{ 'is-active': selectedProductType === productType.id }"
            @click="emit('select-product-type', productType.id)"
          >
            {{ productType.name }}
          </button>
        </div>
      </div>
    </div>

    <!-- Row 3: Discovery (history + hot) — shown only when data exists -->
    <div
      v-if="searchHistory.length || hotKeywords.length"
      class="explore-search-shell__discovery"
    >
      <div v-if="searchHistory.length" class="explore-search-shell__discovery-group">
        <span class="explore-search-shell__label">最近</span>
        <div class="explore-search-shell__chips explore-search-shell__chips--compact">
          <button
            v-for="item in searchHistory"
            :key="item"
            type="button"
            class="explore-search-shell__chip explore-search-shell__chip--ghost"
            @click="emit('apply-history', item)"
          >
            {{ item }}
          </button>
        </div>
      </div>

      <div v-if="hotKeywords.length" class="explore-search-shell__discovery-group">
        <span class="explore-search-shell__label">热搜</span>
        <div class="explore-search-shell__chips explore-search-shell__chips--compact">
          <button
            v-for="kw in hotKeywords.slice(0, 6)"
            :key="kw.normalizedKeyword || kw.keyword"
            type="button"
            class="explore-search-shell__chip explore-search-shell__chip--ghost"
            @click="emit('apply-hot', kw.keyword)"
          >
            {{ kw.keyword }}
          </button>
        </div>
      </div>
    </div>

    <!-- Row 4: Active bar — only when filters are active -->
    <div v-if="activeCount > 0" class="explore-search-shell__active-bar">
      <span class="explore-search-shell__active-count">{{ activeCount }} 个条件生效</span>
      <button type="button" class="explore-search-shell__clear-btn" @click="emit('clear')">
        清空全部
      </button>
    </div>

  </section>
</template>
```

#### New CSS (full replacement of `<style scoped>`)

```css
.explore-search-shell {
  display: grid;
  gap: 16px;
  padding: 20px 24px;
  background:
    radial-gradient(circle at top right, rgba(255, 219, 179, 0.38), transparent 32%),
    linear-gradient(180deg, rgba(255, 251, 246, 0.97), rgba(248, 241, 233, 0.95));
}

/* Row 1: search input occupies full width */
.explore-search-shell__input-row {
  width: 100%;
}

/* Row 2: filter rows */
.explore-search-shell__filters {
  display: grid;
  gap: 12px;
}

.explore-search-shell__filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.explore-search-shell__label {
  color: var(--cm-text-secondary);
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
  flex-shrink: 0;
  min-width: 28px;
}

.explore-search-shell__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.explore-search-shell__chips--compact {
  gap: 6px;
}

/* Row 3: discovery */
.explore-search-shell__discovery {
  display: flex;
  flex-wrap: wrap;
  gap: 14px 24px;
  padding-top: 12px;
  border-top: 1px solid var(--cm-border);
}

.explore-search-shell__discovery-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

/* Row 4: active bar */
.explore-search-shell__active-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--cm-border);
}

.explore-search-shell__active-count {
  font-size: 13px;
  font-weight: 600;
  color: var(--cm-primary);
}

.explore-search-shell__clear-btn {
  appearance: none;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid var(--cm-border);
  background: transparent;
  color: var(--cm-text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition:
    color var(--cm-transition-micro),
    border-color var(--cm-transition-micro);
}

.explore-search-shell__clear-btn:hover {
  color: var(--cm-text);
  border-color: rgba(var(--cm-primary-rgb), 0.28);
}

/* Chips */
.explore-search-shell__chip {
  appearance: none;
  min-height: 36px;
  padding: 7px 14px;
  border-radius: 999px;
  border: 1px solid rgba(144, 113, 89, 0.15);
  background: rgba(255, 255, 255, 0.85);
  color: var(--cm-text-secondary);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition:
    transform var(--cm-transition-micro),
    border-color var(--cm-transition-micro),
    color var(--cm-transition-micro),
    background var(--cm-transition-micro);
}

.explore-search-shell__chip:hover {
  transform: translateY(-1px);
  color: var(--cm-text);
  border-color: rgba(var(--cm-primary-rgb), 0.25);
  background: rgba(255, 247, 237, 0.95);
}

.explore-search-shell__chip.is-active {
  color: var(--cm-primary-deep);
  border-color: rgba(var(--cm-primary-rgb), 0.36);
  background: rgba(var(--cm-primary-rgb), 0.09);
  font-weight: 700;
}

.explore-search-shell__chip--ghost {
  min-height: 32px;
  padding: 5px 12px;
  font-size: 13px;
  font-weight: 600;
  background: rgba(255, 254, 250, 0.7);
  border-color: rgba(144, 113, 89, 0.10);
}

/* Mobile: filter chip rows become horizontal-scrollable */
@media (max-width: 768px) {
  .explore-search-shell {
    padding: 16px;
    gap: 14px;
  }

  .explore-search-shell__filter-group {
    overflow-x: auto;
    scrollbar-width: none;
    -webkit-overflow-scrolling: touch;
    flex-wrap: nowrap;
  }

  .explore-search-shell__filter-group::-webkit-scrollbar {
    display: none;
  }

  .explore-search-shell__chips {
    flex-wrap: nowrap;
  }

  .explore-search-shell__discovery {
    gap: 10px 16px;
  }
}
```

---

### 2. `frontend/src/views/app/ProductListView.vue`

#### Script changes

**Remove:**
```javascript
// remove: curatedProducts computed
// remove: pageTitle computed (or simplify — keep only for route awareness if needed)
// remove: pageDescription computed
```

**Add:**
```javascript
const hasActiveFilters = computed(() =>
  !!keyword.value.trim() || !!selectedCategoryId.value || !!selectedProductType.value
)
```

**Keep:** all existing data loading logic, route sync, store imports, `featuredShops`, `openProduct`, `openShopByProduct`, `openShop`, `clearFilters`, `handlePageChange`, filter handlers.

**Remove import:** `PageSection` (no longer needed — all sections are now scoped style classes)

#### Template — full replacement

```html
<template>
  <div class="shell-container page-stack">

    <!-- 1. Search + Filters -->
    <ExploreSearchShell
      v-model="keyword"
      :categories="categoryFilters"
      :product-types="productTypeFilters"
      :selected-category-id="selectedCategoryId"
      :selected-product-type="selectedProductType"
      :suggestions="searchStore.suggestions"
      :loading-suggestions="searchStore.loadingSuggestions"
      :suggestion-error="searchStore.suggestionError"
      :search-history="searchStore.searchHistory"
      :hot-keywords="searchStore.hotKeywords"
      :loading-hot-keywords="searchStore.loadingHotKeywords"
      @change="handleKeywordChange"
      @submit="handleKeywordSubmit"
      @select-suggestion="handleSuggestionSelect"
      @select-category="setCategory"
      @select-product-type="setProductType"
      @apply-history="applyHistoryKeyword"
      @apply-hot="handleKeywordSubmit"
      @clear="clearFilters"
    />

    <!-- Error -->
    <ErrorBlock v-if="loadError" @retry="bootstrapExplorePage" />

    <!-- Loading -->
    <SkeletonCard v-else-if="loading" :count="8" />

    <template v-else>

      <!-- 2. Featured Shops — only when browsing without filters -->
      <FeaturedShopsSection
        v-if="!hasActiveFilters && featuredShops.length"
        :shops="featuredShops"
        title="精选店铺"
        description="从同一位卖家的商品里判断风格与信任度，再挑单品往往更快。"
        @open-shop="openShop"
        @open-product="openProduct"
      />

      <!-- 3. Results bar -->
      <div class="explore-results-bar shell-card">
        <div class="explore-results-bar__info">
          <span class="explore-results-bar__count">{{ total }} 件商品</span>
          <div v-if="hasActiveFilters" class="explore-results-bar__tags">
            <span v-if="keyword" class="explore-results-bar__tag">{{ keyword }}</span>
            <span
              v-if="selectedCategoryId"
              class="explore-results-bar__tag"
            >{{ categoryFilters.find(c => c.id === selectedCategoryId)?.name }}</span>
            <span
              v-if="selectedProductType"
              class="explore-results-bar__tag"
            >{{ productTypeFilters.find(t => t.id === selectedProductType)?.name }}</span>
          </div>
        </div>
        <button
          v-if="hasActiveFilters"
          type="button"
          class="explore-results-bar__clear"
          @click="clearFilters"
        >
          清空筛选
        </button>
      </div>

      <!-- 4. Product grid -->
      <section class="explore-grid-section">
        <div v-if="cards.length" class="explore-product-grid">
          <ExploreProductCard
            v-for="product in cards"
            :key="product.id"
            :product="product"
            @open-product="openProduct"
            @open-shop="openShopByProduct"
          />
        </div>

        <EmptyState
          v-else
          title="没有找到匹配的商品"
          description="可以试试更短的关键词，或者清空分类与类型筛选后重新浏览。"
        >
          <el-button type="primary" @click="clearFilters">清空筛选</el-button>
        </EmptyState>

        <el-pagination
          v-if="cards.length && total > pageSize"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next, total"
          class="explore-pagination"
          @current-change="handlePageChange"
        />
      </section>

    </template>
  </div>
</template>
```

#### Scoped styles (full replacement)

```css
/* Results bar */
.explore-results-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 20px;
}

.explore-results-bar__info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  min-width: 0;
}

.explore-results-bar__count {
  font-size: 15px;
  font-weight: 600;
  color: var(--cm-text);
  white-space: nowrap;
}

.explore-results-bar__tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.explore-results-bar__tag {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(var(--cm-primary-rgb), 0.08);
  color: var(--cm-primary-deep);
  font-size: 13px;
  font-weight: 600;
}

.explore-results-bar__clear {
  appearance: none;
  flex-shrink: 0;
  padding: 7px 14px;
  border-radius: 999px;
  border: 1px solid var(--cm-border);
  background: transparent;
  color: var(--cm-text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition:
    color var(--cm-transition-micro),
    border-color var(--cm-transition-micro);
}

.explore-results-bar__clear:hover {
  color: var(--cm-text);
  border-color: rgba(var(--cm-primary-rgb), 0.28);
}

/* Product grid: full width, no sidebar */
.explore-grid-section {
  display: grid;
  gap: 20px;
}

.explore-product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 18px;
}

.explore-pagination {
  justify-content: center;
}

@media (max-width: 768px) {
  .explore-results-bar {
    padding: 12px 16px;
  }

  .explore-product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
  }
}

@media (max-width: 480px) {
  .explore-product-grid {
    grid-template-columns: 1fr;
  }
}
```

## Data Flow (Unchanged)

All existing store actions, route watchers, and filter handlers remain untouched. Only template restructuring.

```
onMounted → bootstrapExplorePage()
  → loadProductsByRoute()    → marketStore.loadProducts() → cards, total
  → loadSupportingData()
      → searchStore.loadHotKeywords() → hotKeywords
      → recommendStore.loadHomeRecommend(8) → homeRecommendList (used by featuredShops)

route query change (keyword/category/productType/page) → loadProductsByRoute()

filter actions → applyFilters() → router.replace() → route watcher → reload
```

## Out of Scope

- Modifying `ExploreProductCard.vue`
- Modifying `FeaturedShopsSection.vue`
- Modifying `SearchSuggestInput.vue`
- Modifying `HotSearchList.vue`
- Modifying any store, API module, normalizer
- Modifying backend, schema, or routes
- Adding new npm packages
- Modifying `variables.css` or `index.css`
- Redesigning the product detail page or any other view

## Hard Limits

- Do NOT change the props or emits of `ExploreSearchShell` — ProductListView uses them unchanged
- Do NOT remove the `syncingRoute` guard in `ProductListView` — it prevents double-load on programmatic navigation
- Do NOT use `axios` or `fetch` directly in the view — all data through existing stores
- Do NOT add `HotSearchList` component separately in the view — hot keywords stay inside ExploreSearchShell
- Do NOT fake product or shop data
- Do NOT add a new UI library

## Test Plan

```bash
cd frontend && npm test
cd frontend && npm run build
```

Both must exit 0.

### Manual verification checklist

**Desktop (>1100px)**
- [ ] ExploreSearchShell: no h1 hero text, no long description paragraph
- [ ] ExploreSearchShell: search input is top element, full width
- [ ] Category chips: visible directly below search, "全部分类" selected by default
- [ ] Type chips: visible below category chips
- [ ] Discovery row (history + hot): rendered below a separator line, only when data exists
- [ ] Active bar: NOT rendered in default state (no active filters)
- [ ] No `explore-overview` card with "商品浏览是主场..." h2 anywhere on page
- [ ] No "本周逛什么" section anywhere on page
- [ ] No sidebar (two-column layout) — product grid is full width
- [ ] No "逛店铺比只看单品更轻松" text card
- [ ] FeaturedShopsSection: visible ABOVE results bar when no filter active
- [ ] Results bar: shows "N 件商品", no active filter tags when no filter active
- [ ] Product grid: 4+ columns at desktop width, clean, no prose header above it

**With active filter (select a category or type)**
- [ ] Chip shows `is-active` styling (warmer background, primary border color)
- [ ] Active bar appears in ExploreSearchShell: "1 个条件生效" + "清空全部" button
- [ ] FeaturedShopsSection is hidden
- [ ] Results bar shows active filter tag(s) + "清空筛选" button
- [ ] Click "清空全部" in shell → all filters cleared, FeaturedShops reappears
- [ ] Click "清空筛选" in results bar → same behavior

**With search keyword**
- [ ] Typing → suggestions panel appears (existing SearchSuggestInput behavior, unchanged)
- [ ] Submit → products filtered, keyword tag appears in results bar
- [ ] Active bar shows "1 个条件生效"

**Mobile (≤768px)**
- [ ] Category chip row scrolls horizontally without wrapping (no line breaks)
- [ ] Type chip row same behavior
- [ ] Search input usable, button visible
- [ ] Product grid: 2 columns
- [ ] Results bar: count + tags visible, wraps gracefully
- [ ] FeaturedShopsSection: still visible, not broken

**Navigation integrity**
- [ ] Clicking a product card → `/app/products/:id`
- [ ] Clicking a shop → `/app/shops/:id`
- [ ] Clicking a FeaturedShop "查看" → `/app/shops/:id`
- [ ] URL reflects active filters (keyword, category, productType, page in query params)
- [ ] Browser back/forward preserves filter state

**Data states**
- [ ] Loading: SkeletonCard shown (8 cards)
- [ ] Error: ErrorBlock shown, retry works
- [ ] Empty results: EmptyState shown with "清空筛选" button
- [ ] No FeaturedShops data: FeaturedShopsSection not rendered (already handled by `featuredShops.length > 0`)

## Acceptance Criteria

- [ ] `ExploreSearchShell` has no h1 or description paragraph — search input is the first visible element
- [ ] Category and type filters are inline chip rows immediately below the search input
- [ ] Hot keywords appear as compact chips in a discovery row inside the search shell, not in a separate sidebar card
- [ ] The `explore-overview` section does not exist on the page
- [ ] The "本周逛什么" section does not exist on the page
- [ ] No sidebar layout — product grid is full-width
- [ ] FeaturedShopsSection appears above the product grid, not below it
- [ ] FeaturedShopsSection is hidden when any filter is active
- [ ] Results bar shows product count + active filter tags + clear button
- [ ] Active filter count is shown inside ExploreSearchShell, not in the top-right of a hero header
- [ ] `npm test` passes (zero failures)
- [ ] `npm run build` succeeds (exit 0)
- [ ] No new npm dependencies added
- [ ] Props and emits of ExploreSearchShell are unchanged
- [ ] No store or API module modified

## Documentation Updates Required

- [x] `CHANGELOG.md` — prepend entry under `### feat`
- [ ] `docs/06-http/` — not applicable
- [ ] `docs/09-api-spec/` — not applicable
- [x] `docs/08-tasks/active/explore-page-ux-redesign.md` → move to `archived/` after completion

## Final Report Format

```markdown
## Return Report — explore-page-ux-redesign

### A. Files Changed
(paste git diff --stat)

### B. Implementation Notes
(deviations from spec, with reasons)
(data limitations: e.g. featuredShops always empty in dev because no recommend data)

### C. Test Results
- npm test: exit code X, N passed
- npm run build: exit code X

### D. Manual Verification
(checklist result per item)

### E. Acceptance Criteria Check
(each criterion with evidence)

### F. Out-of-scope Findings
(none, or follow-up items)
```

## Completion Notes

- Rebuilt `ExploreSearchShell.vue` into a compact four-row search/filter shell with inline chips, discovery hints, and an active-filter bar while preserving its prop/emit contract.
- Reworked `ProductListView.vue` into a full-width browse flow with conditional featured shops above the grid, a compact results bar, and the existing loading/error/empty states intact.
- Verified the slice with `cd frontend && npm test` and `cd frontend && npm run build`.
