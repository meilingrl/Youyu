<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import { useRouter } from 'vue-router'
import PageSection from '@/components/common/PageSection.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import SearchSuggestInput from '@/components/search/SearchSuggestInput.vue'
import HotSearchList from '@/components/search/HotSearchList.vue'
import ExploreProductCard from '@/components/explore/ExploreProductCard.vue'
import FeaturedShopsSection from '@/components/explore/FeaturedShopsSection.vue'
import { buildFeaturedShops } from '@/components/explore/featured-shop-helpers'
import { useMarketStore } from '@/stores/market'
import { useSearchStore } from '@/stores/search'
import { useRecommendStore } from '@/stores/recommend'

const router = useRouter()
const marketStore = useMarketStore()
const searchStore = useSearchStore()
const recommendStore = useRecommendStore()

const loading = ref(false)
const loadError = ref(false)
const homeKeyword = ref('')

const spotlightProducts = computed(() => recommendStore.homeRecommendList.slice(0, 3))
const heroMetrics = computed(() => [
  {
    label: '在售商品',
    value: marketStore.products.length,
    note: '学习资料、宿舍好物、数码配件…'
  },
  {
    label: '商品分类',
    value: marketStore.categories.length,
    note: '快速定位你想找的类别'
  },
  {
    label: '热门搜索',
    value: searchStore.hotKeywords.length,
    note: '看看最近大家都在找什么'
  }
])

const trustItems = [
  {
    title: '学生专属',
    description: '通过校园身份认证后参与交易，买卖双方都有真实校园背景。'
  },
  {
    title: '交易有保障',
    description: '订单全程可追踪，支持退款售后与平台介入，安心买卖。'
  },
  {
    title: '买卖都方便',
    description: '想买就去逛探索，想卖就直接发布，认证后即可开店经营。'
  }
]

const guideCards = [
  {
    title: '我想买东西',
    description: '逛热搜、按分类筛选、看精选店铺，找到心仪好物直接下单。',
    action: '去逛逛',
    handler: () => router.push('/app/explore')
  },
  {
    title: '我想卖东西',
    description: '完成学生认证后即可发布商品，经营自己的校园小店。',
    action: '去发布',
    handler: () => router.push('/app/seller/publish')
  }
]

const featuredShops = computed(() =>
  buildFeaturedShops([...marketStore.products, ...recommendStore.homeRecommendList], {
    maxShops: 2
  })
)

async function loadHomePage() {
  loading.value = true
  loadError.value = false

  try {
    await Promise.all([
      marketStore.loadProducts({ page: 1, pageSize: 8 }),
      searchStore.loadHotKeywords().catch(() => []),
      recommendStore.loadHomeRecommend(6).catch(() => [])
    ])
  } catch (error) {
    loadError.value = true
    ElMessage.error(error?.response?.data?.message || error?.message || '首页数据加载失败')
  } finally {
    loading.value = false
  }
}

function goToSearch(keyword) {
  const finalKeyword = String(keyword || '').trim()
  if (!finalKeyword) {
    router.push('/app/explore')
    return
  }

  searchStore.clearSuggestions()
  searchStore.rememberKeyword(finalKeyword)
  router.push({ name: 'app-explore', query: { keyword: finalKeyword } })
}

function handleHomeKeywordChange(value) {
  homeKeyword.value = value
  searchStore.loadSuggestions(value).catch(() => [])
}

function handleHomeSuggestionSelect(keyword) {
  homeKeyword.value = keyword
  goToSearch(keyword)
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

onMounted(loadHomePage)
</script>

<template>
  <div class="page-stack">
    <div class="shell-container page-stack">
      <section class="home-hero shell-card">
        <div class="home-hero__copy">
          <span class="eyebrow">CampusMarket</span>
          <h1>校园里的学习好物与生活好物，都在这里。</h1>
          <p>
            学生专属的交易平台——从教材笔记到宿舍神器，认证身份、放心交易、轻松开店。
          </p>
          <div class="home-hero__actions">
            <el-button type="primary" size="large" @click="$router.push('/app/explore')">进入探索页</el-button>
            <el-button plain size="large" @click="$router.push('/app/verification')">学生认证</el-button>
            <el-button plain size="large" @click="$router.push('/app/seller/publish')">发布商品</el-button>
          </div>
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
        </div>

        <div class="home-hero__visual">
          <div class="home-hero__panel home-hero__panel--highlight">
            <span>今日推荐</span>
            <strong>搜索 / 分类 / 热门 / 精选店铺</strong>
            <p>用关键词直达想要的商品，或按分类慢慢挑选——还有精选店铺等你发现。</p>
          </div>
          <div class="home-hero__metric-grid">
            <article v-for="item in heroMetrics" :key="item.label" class="home-hero__metric">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <p>{{ item.note }}</p>
            </article>
          </div>
        </div>
      </section>

      <ErrorBlock v-if="loadError" @retry="loadHomePage" />

      <EmptyState
        v-else-if="!loading && !marketStore.products.length"
        title="暂时还没有商品上架"
        description="去探索页看看有没有新上架的好物，或者成为第一个发布商品的卖家！"
      >
        <el-button type="primary" @click="$router.push('/app/explore')">去探索</el-button>
      </EmptyState>

      <template v-else>
        <PageSection title="快速开始" description="不管你是想买还是想卖，这里是最快的起点。">
          <div class="home-guides">
            <article v-for="item in guideCards" :key="item.title" class="home-guide-card shell-card">
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
              <el-button type="primary" plain @click="item.handler()">{{ item.action }}</el-button>
            </article>
          </div>
        </PageSection>

        <PageSection title="精选好物" description="每天都有同学在上新——先看看这些热门商品吧。">
          <div v-if="spotlightProducts.length" class="home-spotlight-grid">
            <ExploreProductCard
              v-for="product in spotlightProducts"
              :key="product.id"
              :product="product"
              @open-product="openProduct"
              @open-shop="openShopByProduct"
            />
          </div>
          <EmptyState
            v-else
            title="精选内容即将上线"
            description="去探索页浏览更多商品吧。"
          />
        </PageSection>

        <PageSection title="安心交易" description="校园身份认证 + 订单全程保障，让你放心买卖。">
          <div class="home-trust-grid">
            <article v-for="item in trustItems" :key="item.title" class="home-trust-card shell-card">
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
            </article>
          </div>
        </PageSection>

        <PageSection title="此刻热搜" description="看看同学们最近都在搜什么。">
          <HotSearchList
            :keywords="searchStore.hotKeywords.slice(0, 8)"
            :loading="searchStore.loadingHotKeywords"
            @select="goToSearch"
          />
        </PageSection>

        <FeaturedShopsSection
          :shops="featuredShops"
          title="精选店铺"
          description="校园里活跃的优质卖家，商品丰富、评价好。"
          @open-shop="openShop"
          @open-product="openProduct"
        />
      </template>
    </div>
  </div>
</template>

<style scoped>
.home-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 24px;
  padding: 28px;
  background:
    radial-gradient(circle at top left, rgba(255, 219, 179, 0.46), transparent 36%),
    linear-gradient(180deg, rgba(255, 251, 246, 0.98), rgba(248, 241, 233, 0.96));
}

.home-hero__copy,
.home-hero__visual,
.home-hero__metric-grid,
.home-guides,
.home-spotlight-grid,
.home-trust-grid {
  display: grid;
  gap: 18px;
}

.home-hero__copy h1 {
  margin: 0;
  max-width: 12ch;
}

.home-hero__copy p,
.home-guide-card p,
.home-trust-card p {
  margin: 0;
  color: var(--cm-text-secondary);
  line-height: 1.75;
}

.home-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.home-hero__search {
  max-width: 620px;
}

.home-hero__panel {
  padding: 22px;
  border-radius: 28px;
  background: rgba(255, 253, 249, 0.8);
  border: 1px solid rgba(156, 124, 94, 0.14);
}

.home-hero__panel--highlight {
  display: grid;
  gap: 12px;
}

.home-hero__panel--highlight span {
  color: #b45309;
  font-size: 13px;
  font-weight: 700;
}

.home-hero__panel--highlight strong {
  font-size: 28px;
  line-height: 1.3;
}

.home-hero__panel--highlight p,
.home-hero__metric p {
  margin: 0;
  color: var(--cm-text-secondary);
  line-height: 1.7;
}

.home-hero__metric-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.home-hero__metric,
.home-guide-card,
.home-trust-card {
  display: grid;
  gap: 10px;
  padding: 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(156, 124, 94, 0.1);
}

.home-hero__metric span {
  color: var(--cm-text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.home-hero__metric strong {
  font-size: 28px;
  line-height: 1;
}

.home-guides,
.home-trust-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.home-spotlight-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.home-guide-card h3,
.home-trust-card h3 {
  margin: 0;
}

@media (max-width: 1100px) {
  .home-hero {
    grid-template-columns: 1fr;
  }

  .home-hero__metric-grid,
  .home-spotlight-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .home-hero {
    padding: 20px;
  }

  .home-hero__copy h1 {
    max-width: none;
  }

  .home-hero__metric-grid,
  .home-guides,
  .home-spotlight-grid,
  .home-trust-grid {
    grid-template-columns: 1fr;
  }
}
</style>
