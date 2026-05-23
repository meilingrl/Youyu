<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from '@/plugins/element-plus-services'
import { useMarketStore } from '@/stores/market'
import { useSearchStore } from '@/stores/search'
import { useRecommendStore } from '@/stores/recommend'
import { useAppStore } from '@/stores/app'
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ExploreSearchShell from '@/components/explore/ExploreSearchShell.vue'
import ExploreProductCard from '@/components/explore/ExploreProductCard.vue'
import FeaturedShopsSection from '@/components/explore/FeaturedShopsSection.vue'
import { buildFeaturedShops } from '@/components/explore/featured-shop-helpers'

const BOOKMARK_STORAGE_KEY = 'youyu-explore-bookmark-v2'
const BOOKMARK_TOGGLE_THRESHOLD = 0.03

const route = useRoute()
const router = useRouter()
const marketStore = useMarketStore()
const searchStore = useSearchStore()
const recommendStore = useRecommendStore()
const appStore = useAppStore()

const pageRootRef = ref(null)
const sentinelRef = ref(null)
const loading = ref(true)
const loadingMore = ref(false)
const loadError = ref(false)
const loadMoreError = ref('')
const keyword = ref('')
const selectedCategoryId = ref('')
const selectedProductType = ref('')
const currentPage = ref(0)
const pageSize = ref(12)
const cards = ref([])
const total = ref(0)
const scrollProgress = ref(0)
const isSearchShellCondensed = computed(() => appStore.isHeaderCondensed)
const bookmarks = ref(readStoredBookmarks())
let syncingRoute = false
let sentinelObserver = null
let pendingBookmarkRestore = null

const categoryFilters = computed(() => [
  { id: '', name: '全部分类' },
  ...marketStore.categories.map((category) => ({
    id: String(category.id),
    name: category.name
  }))
])

const productTypeFilters = [
  { id: '', name: '全部类型' },
  { id: 'digital', name: '数字商品' },
  { id: 'physical', name: '实物商品' },
  { id: 'service', name: '服务' }
]

const hasActiveFilters = computed(() =>
  !!keyword.value.trim() || !!selectedCategoryId.value || !!selectedProductType.value
)
const hasMore = computed(() => cards.value.length < total.value)
const activeQuery = computed(() => buildFilterQuery())
const activeQueryKey = computed(() => buildQueryKey(activeQuery.value))
const currentQueryBookmarks = computed(() =>
  bookmarks.value.filter((item) => item.queryKey === activeQueryKey.value)
)
const bookmarkCountLabel = computed(() => {
  if (!bookmarks.value.length) return ''
  return `${bookmarks.value.length} 个书签`
})
const bookmarkHint = computed(() => {
  if (!bookmarks.value.length) return '双击轨道保存书签'
  return '点击圆点返回书签位置'
})
const visibleCountLabel = computed(() => {
  if (!cards.value.length) return `${total.value} 件商品`
  if (!hasMore.value) return `已展示全部 ${total.value} 件商品`
  return `已展示 ${cards.value.length} / ${total.value} 件商品`
})
const featuredShops = computed(() =>
  buildFeaturedShops([...cards.value, ...recommendStore.homeRecommendList], {
    maxShops: 3,
    fallbackDescription: '围绕学习与校园生活整理出一组值得慢慢逛的商品。'
  })
)

function clampProgress(value) {
  return Math.min(0.995, Math.max(0.005, Number(value) || 0))
}

function readStoredBookmarks() {
  if (typeof window === 'undefined') return []
  try {
    const raw = window.localStorage.getItem(BOOKMARK_STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed)
      ? parsed
          .filter((item) => item && typeof item === 'object')
          .map((item) => ({
            ...item,
            progress: clampProgress(item.progress)
          }))
          .sort((left, right) => left.progress - right.progress)
      : []
  } catch {
    return []
  }
}

function writeStoredBookmarks(value) {
  bookmarks.value = value
  if (typeof window === 'undefined') return
  try {
    if (value.length) {
      window.localStorage.setItem(BOOKMARK_STORAGE_KEY, JSON.stringify(value))
    } else {
      window.localStorage.removeItem(BOOKMARK_STORAGE_KEY)
    }
  } catch {
    // Ignore storage failures and keep the in-memory bookmark.
  }
}

function routeKeyword() {
  return typeof route.query.keyword === 'string' ? route.query.keyword : ''
}

function routeCategory() {
  return typeof route.query.category === 'string' ? route.query.category : ''
}

function routeProductType() {
  return typeof route.query.productType === 'string' ? route.query.productType : ''
}

function buildFilterQuery() {
  return {
    keyword: keyword.value.trim() || '',
    category: selectedCategoryId.value || '',
    productType: selectedProductType.value || ''
  }
}

function buildQuery() {
  return {
    keyword: keyword.value.trim() || undefined,
    category: selectedCategoryId.value || undefined,
    productType: selectedProductType.value || undefined
  }
}

function buildQueryKey(query) {
  return JSON.stringify({
    keyword: query.keyword || '',
    category: query.category || '',
    productType: query.productType || ''
  })
}

function dedupeProducts(list) {
  const seen = new Set()
  return list.filter((item) => {
    const id = String(item.id)
    if (seen.has(id)) return false
    seen.add(id)
    return true
  })
}

async function fetchProductsPage(page, { append = false } = {}) {
  if (append) {
    loadingMore.value = true
    loadMoreError.value = ''
  } else {
    loading.value = true
    loadError.value = false
  }

  try {
    const items = await marketStore.loadProducts({
      keyword: keyword.value || undefined,
      categoryId: selectedCategoryId.value ? Number(selectedCategoryId.value) : undefined,
      productType: selectedProductType.value || undefined,
      page,
      pageSize: pageSize.value
    })

    const normalizedItems = Array.isArray(items) ? [...items] : []
    total.value = Number(marketStore.searchTotal || normalizedItems.length || 0)
    currentPage.value = page
    cards.value = append
      ? dedupeProducts([...cards.value, ...normalizedItems])
      : normalizedItems

    searchStore.clearSuggestions()
    if (keyword.value) {
      searchStore.rememberKeyword(keyword.value)
    }

    await nextTick()
    refreshSentinelObserver()
    return normalizedItems
  } catch (error) {
    if (append) {
      loadMoreError.value = error?.response?.data?.message || error?.message || '继续加载商品失败'
    } else {
      loadError.value = true
      ElMessage.error(error?.response?.data?.message || error?.message || '探索页商品加载失败')
    }
    throw error
  } finally {
    if (append) {
      loadingMore.value = false
    } else {
      loading.value = false
    }
  }
}

async function loadProductsByRoute() {
  syncingRoute = true
  keyword.value = routeKeyword()
  selectedCategoryId.value = routeCategory()
  selectedProductType.value = routeProductType()
  cards.value = []
  total.value = 0
  currentPage.value = 0
  loadMoreError.value = ''

  try {
    await fetchProductsPage(1)
    const restored = await restorePendingBookmarkIfNeeded()
    if (!restored && typeof window !== 'undefined') {
      window.scrollTo({ top: 0, behavior: 'auto' })
    }
  } finally {
    syncingRoute = false
  }
}

async function loadSupportingData() {
  await Promise.allSettled([
    searchStore.loadHotKeywords(),
    recommendStore.loadHomeRecommend(8)
  ])
}

async function bootstrapExplorePage() {
  await Promise.allSettled([loadProductsByRoute(), loadSupportingData()])
}

async function loadNextPage() {
  if (loading.value || loadingMore.value || loadError.value || !hasMore.value) {
    return false
  }

  try {
    await fetchProductsPage(currentPage.value + 1, { append: true })
    return true
  } catch {
    return false
  }
}

function applyFilters() {
  searchStore.clearSuggestions()
  const nextQuery = buildQuery()

  if (
    routeKeyword() === (nextQuery.keyword || '') &&
    routeCategory() === (nextQuery.category || '') &&
    routeProductType() === (nextQuery.productType || '')
  ) {
    loadProductsByRoute()
    return
  }

  router.replace({ query: nextQuery })
}

function setCategory(id) {
  selectedCategoryId.value = String(id || '')
  applyFilters()
}

function setProductType(id) {
  selectedProductType.value = String(id || '')
  applyFilters()
}

function clearFilters() {
  keyword.value = ''
  selectedCategoryId.value = ''
  selectedProductType.value = ''
  searchStore.clearSuggestions()
  router.replace({ query: {} })
}

function handleKeywordChange(value) {
  keyword.value = value
  searchStore.loadSuggestions(value).catch(() => [])
}

function handleKeywordSubmit(value) {
  keyword.value = String(value || '')
  applyFilters()
}

function handleSuggestionSelect(value) {
  keyword.value = String(value || '')
  applyFilters()
}

function applyHistoryKeyword(item) {
  keyword.value = item
  applyFilters()
}

function openProduct(product) {
  router.push(`/app/products/${product.id}`)
}

function openShopByProduct(product) {
  if (product.shopId) {
    router.push(`/app/shops/${product.shopId}`)
  }
}

function openShop(shop) {
  if (shop.id) {
    router.push(`/app/shops/${shop.id}`)
  }
}

function updateScrollProgress() {
  if (typeof window === 'undefined') return
  const scrollRange = document.documentElement.scrollHeight - window.innerHeight
  scrollProgress.value = scrollRange > 0 ? Math.min(1, Math.max(0, window.scrollY / scrollRange)) : 0

  if (window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 960) {
    loadNextPage()
  }
}

function findCurrentAnchor() {
  if (typeof document === 'undefined') return null

  const nodes = [...document.querySelectorAll('[data-explore-product-id]')]
  if (!nodes.length) return null

  const visibleNode =
    nodes.find((node) => node.getBoundingClientRect().top >= 72) ||
    nodes.find((node) => node.getBoundingClientRect().bottom > 72) ||
    nodes[0]

  if (!visibleNode) return null

  const rect = visibleNode.getBoundingClientRect()
  return {
    id: visibleNode.getAttribute('data-explore-product-id'),
    topGap: Math.max(0, Math.round(rect.top))
  }
}

function buildTrackRatio(event) {
  const track = event.currentTarget
  if (!(track instanceof HTMLElement)) return null
  const rect = track.getBoundingClientRect()
  return clampProgress((event.clientY - rect.top) / rect.height)
}

function createBookmarkId() {
  return `bookmark-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

function saveScrollBookmark(event) {
  const progress = buildTrackRatio(event) ?? scrollProgress.value
  const scrollRange =
    typeof window === 'undefined' ? 0 : document.documentElement.scrollHeight - window.innerHeight
  const targetScrollY = Math.max(0, Math.round(progress * scrollRange))
  const nextBookmarks = [...bookmarks.value]
  const matchedBookmark = nextBookmarks.find(
    (item) =>
      item.queryKey === activeQueryKey.value &&
      Math.abs(clampProgress(item.progress) - progress) <= BOOKMARK_TOGGLE_THRESHOLD
  )

  if (matchedBookmark) {
    writeStoredBookmarks(
      nextBookmarks
        .filter((item) => item.id !== matchedBookmark.id)
        .sort((left, right) => left.progress - right.progress)
    )
    ElMessage.success('已移除书签')
    return
  }

  const payload = {
    id: createBookmarkId(),
    query: buildQuery(),
    queryKey: activeQueryKey.value,
    page: currentPage.value,
    progress,
    scrollY: targetScrollY,
    anchorProductId: null,
    anchorTopGap: 96,
    savedAt: new Date().toISOString()
  }
  nextBookmarks.push(payload)
  writeStoredBookmarks(nextBookmarks.sort((left, right) => left.progress - right.progress))
  ElMessage.success('已添加书签')
}

function jumpByRail(event) {
  if (typeof window === 'undefined') return
  const clickRatio = buildTrackRatio(event)
  if (clickRatio === null) return
  const scrollRange = document.documentElement.scrollHeight - window.innerHeight
  window.scrollTo({
    top: clickRatio * scrollRange,
    behavior: 'smooth'
  })
}

async function ensurePagesLoaded(targetPage) {
  while (currentPage.value < targetPage && hasMore.value) {
    const loaded = await loadNextPage()
    if (!loaded) break
  }
}

async function scrollToBookmarkPosition(targetBookmark) {
  if (typeof window === 'undefined' || !targetBookmark) return

  await ensurePagesLoaded(targetBookmark.page || 1)
  await nextTick()

  if (targetBookmark.anchorProductId) {
    const anchor = document.querySelector(`[data-explore-product-id="${targetBookmark.anchorProductId}"]`)
    if (anchor instanceof HTMLElement) {
      window.scrollTo({
        top: Math.max(0, anchor.offsetTop - (targetBookmark.anchorTopGap || 96)),
        behavior: 'smooth'
      })
      return
    }
  }

  const scrollRange = document.documentElement.scrollHeight - window.innerHeight
  const fallbackTop =
    typeof targetBookmark.scrollY === 'number'
      ? targetBookmark.scrollY
      : Math.round(clampProgress(targetBookmark.progress) * scrollRange)

  window.scrollTo({
    top: Math.max(0, fallbackTop),
    behavior: 'smooth'
  })
}

async function restoreBookmark(targetBookmark) {
  if (!targetBookmark) return

  if (targetBookmark.queryKey !== activeQueryKey.value) {
    pendingBookmarkRestore = targetBookmark
    await router.replace({ query: targetBookmark.query || {} })
    return
  }

  await scrollToBookmarkPosition(targetBookmark)
}

async function restorePendingBookmarkIfNeeded() {
  if (!pendingBookmarkRestore) return false
  if (pendingBookmarkRestore.queryKey !== activeQueryKey.value) return false

  const targetBookmark = pendingBookmarkRestore
  pendingBookmarkRestore = null
  await scrollToBookmarkPosition(targetBookmark)
  return true
}

function disconnectSentinelObserver() {
  sentinelObserver?.disconnect()
  sentinelObserver = null
}

function refreshSentinelObserver() {
  disconnectSentinelObserver()

  if (typeof window === 'undefined' || typeof IntersectionObserver === 'undefined') return
  if (!sentinelRef.value || loadError.value) return

  sentinelObserver = new IntersectionObserver(
    (entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        loadNextPage()
      }
    },
    {
      rootMargin: '960px 0px 960px 0px'
    }
  )

  sentinelObserver.observe(sentinelRef.value)
}

let scrollRafId = null

function onScroll() {
  if (scrollRafId) return
  scrollRafId = requestAnimationFrame(() => {
    updateScrollProgress()
    scrollRafId = null
  })
}

onMounted(() => {
  updateScrollProgress()
  window.addEventListener('scroll', onScroll, { passive: true })
  bootstrapExplorePage()
})

onBeforeUnmount(() => {
  disconnectSentinelObserver()
  window.removeEventListener('scroll', onScroll)
  if (scrollRafId) cancelAnimationFrame(scrollRafId)
})

watch(
  [() => route.query.keyword, () => route.query.category, () => route.query.productType],
  () => {
    if (!syncingRoute) {
      loadProductsByRoute()
    }
  }
)

watch(keyword, (val) => {
  appStore.setKeyword(val)
})

watch(sentinelRef, () => {
  nextTick(() => {
    refreshSentinelObserver()
  })
})
</script>

<template>
  <div ref="pageRootRef" class="shell-container page-stack explore-page">
    <div
      class="explore-search-shell-sticky"
      :class="{ 'is-condensed': isSearchShellCondensed }"
    >
      <ExploreSearchShell
        v-model="keyword"
        class="explore-search-shell-sticky__shell"
        :class="{ 'is-condensed': isSearchShellCondensed }"
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
    </div>

    <aside class="explore-bookmark-rail shell-card" aria-label="浏览位置书签">
      <button
        type="button"
        class="explore-bookmark-rail__track"
        title="单击快速跳转，双击保存当前位置"
        @click="jumpByRail"
        @dblclick.prevent="saveScrollBookmark"
      >
        <span class="explore-bookmark-rail__progress" :style="{ height: `${Math.max(scrollProgress * 100, 6)}%` }" />
        <span
          v-for="bookmark in bookmarks"
          :key="bookmark.id"
          class="explore-bookmark-rail__marker"
          :class="{ 'is-inactive': bookmark.queryKey !== activeQueryKey }"
          :style="{ top: `${clampProgress(bookmark.progress) * 100}%` }"
          :title="bookmark.queryKey === activeQueryKey ? '返回该书签位置' : '切回该书签对应筛选并恢复位置'"
          @click.stop="restoreBookmark(bookmark)"
        />
      </button>

      <button
        v-if="bookmarks.length"
        type="button"
        class="explore-bookmark-rail__restore"
        @click="restoreBookmark(currentQueryBookmarks.at(-1) || bookmarks.at(-1))"
      >
        {{ bookmarkCountLabel }}
      </button>

      <p class="explore-bookmark-rail__hint">{{ bookmarkHint }}</p>
    </aside>

    <ErrorBlock v-if="loadError" @retry="bootstrapExplorePage" />

    <SkeletonCard v-else-if="loading" :count="8" />

    <template v-else>
      <FeaturedShopsSection
        v-if="!hasActiveFilters && featuredShops.length"
        :shops="featuredShops"
        title="精选店铺"
        description="从同一位卖家的商品里判断风格与信任度，再挑单品往往更快。"
        @open-shop="openShop"
        @open-product="openProduct"
      />

      <div class="explore-results-bar shell-card">
        <div class="explore-results-bar__info">
          <span class="explore-results-bar__count">{{ visibleCountLabel }}</span>
          <div v-if="hasActiveFilters" class="explore-results-bar__tags">
            <span v-if="keyword" class="explore-results-bar__tag">{{ keyword }}</span>
            <span v-if="selectedCategoryId" class="explore-results-bar__tag">
              {{ categoryFilters.find((category) => category.id === selectedCategoryId)?.name }}
            </span>
            <span v-if="selectedProductType" class="explore-results-bar__tag">
              {{ productTypeFilters.find((type) => type.id === selectedProductType)?.name }}
            </span>
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

      <section class="explore-grid-section">
        <div v-if="cards.length" class="explore-product-grid">
          <ExploreProductCard
            v-for="product in cards"
            :key="product.id"
            :product="product"
            :data-explore-product-id="product.id"
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

        <div v-if="cards.length" class="explore-feed-status">
          <div v-if="loadingMore" class="explore-feed-status__card shell-card">
            正在继续加载更多商品…
          </div>

          <div v-else-if="loadMoreError" class="explore-feed-status__card shell-card is-error">
            <span>{{ loadMoreError }}</span>
            <button type="button" class="explore-feed-status__retry" @click="loadNextPage">
              重试加载
            </button>
          </div>

          <div v-else-if="!hasMore" class="explore-feed-status__card shell-card is-end">
            已经看到最后了
          </div>

          <div ref="sentinelRef" class="explore-feed-status__sentinel" aria-hidden="true" />
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.explore-page {
  position: relative;
}

.explore-search-shell-sticky {
  position: sticky;
  top: 96px;
  z-index: 24;
  display: flex;
  justify-content: center;
  padding-top: 4px;
  transition: padding var(--cm-transition-feature);
}

.explore-search-shell-sticky::before {
  content: '';
  position: absolute;
  inset: -8px -24px auto;
  height: calc(100% + 18px);
  border-radius: 42px;
  background: linear-gradient(180deg, rgba(247, 239, 229, 0.92), rgba(247, 239, 229, 0));
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  opacity: 0;
  pointer-events: none;
  transition: opacity var(--cm-transition-feature);
}

.explore-search-shell-sticky.is-condensed {
  padding-top: 0;
  top: 88px;
}

.explore-search-shell-sticky.is-condensed::before {
  opacity: 1;
}

.explore-search-shell-sticky__shell {
  width: min(100%, 1180px);
  transition:
    width var(--cm-transition-feature),
    margin var(--cm-transition-feature);
}

.explore-search-shell-sticky.is-condensed .explore-search-shell-sticky__shell {
  width: min(100%, 1180px);
}

.explore-bookmark-rail {
  position: fixed;
  top: 220px;
  right: max(16px, calc((100vw - var(--cm-container)) / 2 - 92px));
  z-index: 8;
  width: 72px;
  display: grid;
  gap: 12px;
  justify-items: center;
  padding: 16px 12px;
}

.explore-bookmark-rail__track {
  position: relative;
  width: 14px;
  height: 240px;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: rgba(88, 62, 43, 0.08);
  cursor: pointer;
}

.explore-bookmark-rail__progress {
  position: absolute;
  inset: 0 0 auto 0;
  border-radius: inherit;
  background: linear-gradient(180deg, rgba(var(--cm-primary-rgb), 0.45), rgba(var(--cm-primary-rgb), 0.95));
}

.explore-bookmark-rail__marker {
  position: absolute;
  left: 50%;
  width: 16px;
  height: 16px;
  border: 1.5px solid rgba(var(--cm-primary-rgb), 0.92);
  border-radius: 999px;
  background: rgba(255, 250, 243, 0.98);
  box-shadow: 0 4px 12px rgba(88, 62, 43, 0.16);
  transform: translate(-50%, -50%);
}

.explore-bookmark-rail__marker.is-inactive {
  border-color: rgba(88, 62, 43, 0.22);
  background: rgba(255, 255, 255, 0.92);
}

.explore-bookmark-rail__restore {
  appearance: none;
  width: 100%;
  padding: 8px 10px;
  border: 1px solid var(--cm-border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.88);
  color: var(--cm-text);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.explore-bookmark-rail__hint {
  margin: 0;
  color: var(--cm-text-secondary);
  font-size: 12px;
  line-height: 1.5;
  text-align: center;
}

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
  color: var(--cm-text);
  font-size: 15px;
  font-weight: 600;
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
  border: 1px solid var(--cm-border);
  border-radius: 999px;
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

.explore-results-bar__clear:hover,
.explore-bookmark-rail__restore:hover,
.explore-feed-status__retry:hover {
  color: var(--cm-text);
  border-color: rgba(var(--cm-primary-rgb), 0.28);
}

.explore-grid-section {
  display: grid;
  gap: 20px;
}

.explore-product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 18px;
}

.explore-feed-status {
  display: grid;
  gap: 12px;
}

.explore-feed-status__card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 14px 18px;
  color: var(--cm-text-secondary);
}

.explore-feed-status__card.is-error {
  justify-content: space-between;
}

.explore-feed-status__card.is-end {
  color: var(--cm-text-tertiary);
}

.explore-feed-status__retry {
  appearance: none;
  padding: 8px 14px;
  border: 1px solid var(--cm-border);
  border-radius: 999px;
  background: transparent;
  color: var(--cm-text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.explore-feed-status__sentinel {
  height: 1px;
}

@media (max-width: 1480px) {
  .explore-bookmark-rail {
    right: 16px;
  }
}

@media (max-width: 1024px) {
  .explore-bookmark-rail {
    display: none;
  }
}

@media (max-width: 768px) {
  .explore-search-shell-sticky {
    top: 70px;
  }

  .explore-search-shell-sticky::before {
    inset-inline: -8px;
    border-radius: 28px;
  }

  .explore-search-shell-sticky__shell,
  .explore-search-shell-sticky.is-condensed .explore-search-shell-sticky__shell {
    width: 100%;
  }

  .explore-results-bar {
    padding: 12px 16px;
  }

  .explore-product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
  }

  .explore-feed-status__card.is-error {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
