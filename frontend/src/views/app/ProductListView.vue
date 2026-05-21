<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from '@/plugins/element-plus-services'
import { useMarketStore } from '@/stores/market'
import { useSearchStore } from '@/stores/search'
import { useRecommendStore } from '@/stores/recommend'
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PageSection from '@/components/common/PageSection.vue'
import ExploreSearchShell from '@/components/explore/ExploreSearchShell.vue'
import ExploreProductCard from '@/components/explore/ExploreProductCard.vue'
import FeaturedShopsSection from '@/components/explore/FeaturedShopsSection.vue'
import { buildFeaturedShops } from '@/components/explore/featured-shop-helpers'

const route = useRoute()
const router = useRouter()
const marketStore = useMarketStore()
const searchStore = useSearchStore()
const recommendStore = useRecommendStore()

const loading = ref(true)
const loadError = ref(false)
const keyword = ref('')
const selectedCategoryId = ref('')
const selectedProductType = ref('')
const currentPage = ref(1)
const pageSize = ref(12)
let syncingRoute = false

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

const cards = computed(() => marketStore.products)
const total = computed(() => marketStore.searchTotal)
const pageTitle = computed(() => (route.name === 'app-explore' ? '探索' : '商品浏览'))
const pageDescription = computed(() =>
  route.name === 'app-explore'
    ? '把热搜、筛选、精选店铺和商品网格放在同一条浏览主线上。'
    : '旧入口仍然可用，但主体验已经对齐到新的探索结构。'
)

const curatedProducts = computed(() => recommendStore.homeRecommendList.slice(0, 4))
const featuredShops = computed(() =>
  buildFeaturedShops([...cards.value, ...recommendStore.homeRecommendList], {
    maxShops: 3,
    fallbackDescription: '围绕学习与校园生活整理出一组值得慢慢逛的商品。'
  })
)

function routeKeyword() {
  return typeof route.query.keyword === 'string' ? route.query.keyword : ''
}

function routeCategory() {
  return typeof route.query.category === 'string' ? route.query.category : ''
}

function routeProductType() {
  return typeof route.query.productType === 'string' ? route.query.productType : ''
}

function routePage() {
  const page = parseInt(route.query.page, 10)
  return Number.isFinite(page) && page > 0 ? page : 1
}

function buildQuery() {
  const query = {
    keyword: keyword.value.trim() || undefined,
    category: selectedCategoryId.value || undefined,
    productType: selectedProductType.value || undefined
  }

  if (currentPage.value > 1) {
    query.page = String(currentPage.value)
  }

  return query
}

async function loadProductsByRoute() {
  loading.value = true
  loadError.value = false

  try {
    syncingRoute = true
    keyword.value = routeKeyword()
    selectedCategoryId.value = routeCategory()
    selectedProductType.value = routeProductType()
    currentPage.value = routePage()

    await marketStore.loadProducts({
      keyword: keyword.value || undefined,
      categoryId: selectedCategoryId.value ? Number(selectedCategoryId.value) : undefined,
      productType: selectedProductType.value || undefined,
      page: currentPage.value,
      pageSize: pageSize.value
    })

    searchStore.clearSuggestions()
    if (keyword.value) {
      searchStore.rememberKeyword(keyword.value)
    }
  } catch (error) {
    loadError.value = true
    ElMessage.error(error?.response?.data?.message || error?.message || '探索页商品加载失败')
  } finally {
    syncingRoute = false
    loading.value = false
  }
}

async function loadSupportingData() {
  await Promise.allSettled([
    searchStore.loadHotKeywords(),
    recommendStore.loadHomeRecommend(8)
  ])
}

async function bootstrapExplorePage() {
  await Promise.all([loadProductsByRoute(), loadSupportingData()])
}

function applyFilters() {
  searchStore.clearSuggestions()
  currentPage.value = 1
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

function handlePageChange(page) {
  currentPage.value = page
  router.replace({ query: buildQuery() })
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
  currentPage.value = 1
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

onMounted(bootstrapExplorePage)

watch([() => route.query.keyword, () => route.query.category, () => route.query.productType, () => route.query.page], () => {
  if (!syncingRoute) {
    loadProductsByRoute()
  }
})
</script>

<template>
  <div class="shell-container page-stack">
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

    <section class="explore-overview shell-card">
      <div>
        <span class="eyebrow">{{ pageTitle }}</span>
        <h2>商品浏览是主场，热搜与店铺是陪逛线索</h2>
        <p>{{ pageDescription }}</p>
      </div>
      <div class="explore-overview__actions">
        <el-button type="primary" @click="$router.push('/app/cart')">查看购物车</el-button>
        <el-button plain @click="$router.push('/app/home')">返回首页</el-button>
      </div>
    </section>

    <ErrorBlock v-if="loadError" @retry="bootstrapExplorePage" />

    <SkeletonCard v-else-if="loading" :count="6" />

    <template v-else>
      <PageSection title="本周逛什么" description="用少量横向精选给浏览一个轻松起点，不和主商品网格争主角。">
        <div v-if="curatedProducts.length" class="explore-curated-strip">
          <ExploreProductCard
            v-for="product in curatedProducts"
            :key="product.id"
            :product="product"
            @open-product="openProduct"
            @open-shop="openShopByProduct"
          />
        </div>
        <EmptyState
          v-else
          title="精选内容暂未就绪"
          description="推荐数据不足时，探索页仍然会优先保证商品搜索和筛选可用。"
        />
      </PageSection>

      <div class="explore-layout">
        <section class="explore-layout__main shell-card">
          <header class="explore-layout__header">
            <div>
              <h2>商品网格</h2>
              <p>当前共 {{ total }} 件公开商品，支持关键词、分类、类型与分页浏览。</p>
            </div>
          </header>

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

        <aside class="explore-layout__side">
          <section class="explore-side-card shell-card">
            <h3>热搜关键词</h3>
            <p>从公共搜索行为里快速切入当前最容易被浏览的方向。</p>
            <div class="explore-side-card__chips">
              <button
                v-for="item in searchStore.hotKeywords.slice(0, 8)"
                :key="item.normalizedKeyword || item.keyword"
                type="button"
                class="explore-side-card__chip"
                @click="handleKeywordSubmit(item.keyword)"
              >
                <span>#{{ item.keyword }}</span>
                <small>{{ item.searchCount }}</small>
              </button>
            </div>
          </section>

          <section class="explore-side-card shell-card">
            <h3>逛店铺比只看单品更轻松</h3>
            <p>从同一位卖家的几件商品里判断风格、可信度和适配度，会更像真实逛市场。</p>
            <ul class="explore-side-card__notes">
              <li>先看店铺，再挑单品，能更快建立信任感。</li>
              <li>图片不完美也没关系，卡片比例和裁切会保持稳定。</li>
              <li>公开商品越丰富，这里的精选店铺会越有参考价值。</li>
            </ul>
          </section>
        </aside>
      </div>

      <FeaturedShopsSection
        :shops="featuredShops"
        title="精选店铺"
        description="用现有商品数据兼容生成店铺卡片，帮助用户从单品浏览转到店铺探索。"
        @open-shop="openShop"
        @open-product="openProduct"
      />
    </template>
  </div>
</template>

<style scoped>
.explore-overview {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.explore-overview p,
.explore-layout__header p,
.explore-side-card p {
  margin: 10px 0 0;
  color: var(--cm-text-secondary);
  line-height: 1.7;
}

.explore-overview__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.explore-curated-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  overflow-x: auto;
}

.explore-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}

.explore-layout__main,
.explore-side-card {
  display: grid;
  gap: 18px;
}

.explore-layout__header h2,
.explore-side-card h3 {
  margin: 0;
}

.explore-product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 18px;
}

.explore-layout__side {
  display: grid;
  gap: 18px;
}

.explore-side-card__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.explore-side-card__chip {
  appearance: none;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid rgba(201, 93, 49, 0.16);
  border-radius: 16px;
  background: rgba(255, 247, 237, 0.95);
  color: var(--cm-text);
  cursor: pointer;
}

.explore-side-card__chip small {
  color: var(--cm-text-secondary);
}

.explore-side-card__notes {
  margin: 0;
  padding-left: 18px;
  color: var(--cm-text-secondary);
  display: grid;
  gap: 10px;
  line-height: 1.7;
}

.explore-pagination {
  justify-content: center;
  margin-top: 8px;
}

@media (max-width: 1100px) {
  .explore-curated-strip {
    grid-template-columns: repeat(2, minmax(260px, 1fr));
  }

  .explore-layout {
    grid-template-columns: 1fr;
  }

  .explore-layout__side {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .explore-overview {
    flex-direction: column;
    align-items: stretch;
  }

  .explore-curated-strip {
    grid-template-columns: minmax(0, 1fr);
  }

  .explore-layout__side {
    grid-template-columns: 1fr;
  }
}
</style>
