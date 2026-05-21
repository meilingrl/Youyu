<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from '@/plugins/element-plus-services'
import PageSection from '@/components/common/PageSection.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import ReservedMetricCard from '@/components/common/ReservedMetricCard.vue'
import ReservedPanel from '@/components/common/ReservedPanel.vue'
import RatingSummary from '@/components/common/RatingSummary.vue'
import ReviewList from '@/components/common/ReviewList.vue'
import { shopInsightMetricDefinitions } from '@/constants/insightMetrics'
import { useAuthStore } from '@/stores/auth'
import { useMarketStore } from '@/stores/market'
import { useReviewStore } from '@/stores/review'

const props = defineProps({
  id: {
    type: String,
    default: ''
  }
})

const router = useRouter()
const authStore = useAuthStore()
const marketStore = useMarketStore()
const reviewStore = useReviewStore()

const loading = ref(false)
const loadError = ref(false)
const reviewPage = ref(1)

const shop = computed(() => marketStore.getShopById(props.id))
const products = computed(() => marketStore.getProductsByShopId(props.id))
const shopInsight = computed(() => marketStore.getShopInsightById(props.id))
const shopInsightReserved = computed(() => shopInsight.value?.metricSource !== 'real_query')
const shopInsightFailed = computed(() => shopInsight.value?.metricSource === 'unavailable')

const shopMetricDefMap = computed(() => {
  const map = {}
  shopInsightMetricDefinitions.forEach((item) => {
    map[item.key] = item
  })
  return map
})

const shopViewModel = computed(() => {
  if (!shop.value) {
    return null
  }

  const item = shop.value
  const capability = item.capability || {}

  return {
    ...item,
    statusLabel: item.status === 'active' ? '营业中' : '暂未营业',
    reviewStatusLabel:
      item.reviewStatus === 'approved'
        ? '审核通过'
        : item.reviewStatus === 'pending_review'
          ? '审核中'
          : item.reviewStatus === 'rejected'
            ? '审核未通过'
            : '状态待确认',
    ownerName: item.ownerName || '店主昵称待补充',
    coverUrl: item.coverUrl || item.cover || '',
    capability,
    capabilityTags: [
      capability.capabilityLevel ? `能力等级 ${capability.capabilityLevel}` : '',
      capability.maxProductCount ? `最多 ${capability.maxProductCount} 个在售商品` : '',
      capability.canSetNotice ? '支持店铺公告' : '',
      capability.canUseCoupon ? '预留轻优惠能力' : ''
    ].filter(Boolean)
  }
})

const liveIdentityCards = computed(() => {
  if (!shopViewModel.value) {
    return []
  }

  return [
    {
      label: '店主昵称',
      value: shopViewModel.value.ownerName,
      note: '当前接口已返回店主昵称，可作为店铺主体的最小公开身份。'
    },
    {
      label: '店铺信用',
      value: shopViewModel.value.creditLevel || '待补充',
      note: '当前以店铺信用等级摘要展示，不额外拆个人主页。'
    },
    {
      label: '审核状态',
      value: shopViewModel.value.reviewStatusLabel,
      note: '店铺公开状态与审核状态共同构成主体可信度。'
    },
    {
      label: '能力档位',
      value: shopViewModel.value.capabilityLevel || '基础档',
      note: '来自店铺能力档案，说明店铺当前可使用的经营能力。'
    }
  ]
})

function shopMetricDef(key) {
  return shopMetricDefMap.value[key] || {
    scope: '店铺经营统计',
    dataSource: ''
  }
}

function formatMoney(value) {
  if (value === null || value === undefined || value === '') {
    return '--'
  }
  return Number(value || 0).toFixed(2)
}

function formatMetric(value, suffix = '') {
  if (value === null || value === undefined || value === '') {
    return '--'
  }
  return `${value}${suffix}`
}

async function loadShop() {
  if (!props.id) {
    return
  }

  loading.value = true
  loadError.value = false
  reviewPage.value = 1

  try {
    await marketStore.loadShopDetail(props.id)
    await Promise.allSettled([
      marketStore.loadShopInsightSnapshot(props.id),
      reviewStore.loadShopReviewSummary(Number(props.id)),
      reviewStore.loadShopReviews(Number(props.id), reviewPage.value, 10)
    ])
  } catch (error) {
    loadError.value = true
    ElMessage.error(error?.response?.data?.message || error?.message || '店铺详情加载失败')
  } finally {
    loading.value = false
  }
}

function handleContactOwner() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('联系店主前请先登录')
    router.push('/login')
    return
  }
  ElMessage.info('消息功能正在建设中')
  router.push({
    path: '/app/messages',
    query: {
      category: 'shop',
      entry: 'shop',
      entryId: String(props.id || ''),
      targetType: 'shop',
      targetId: String(props.id || ''),
      intent: 'consult'
    }
  })
}

function handleOpenShopMessagesHub() {
  router.push({
    path: '/app/messages',
    query: {
      category: 'shop',
      entry: 'shop',
      entryId: String(props.id || ''),
      targetType: 'shop',
      targetId: String(props.id || ''),
      intent: 'browse'
    }
  })
}

function handleGoProduct(productId) {
  router.push(`/app/products/${productId}`)
}

function handleReviewPageChange(page) {
  reviewPage.value = page
  reviewStore.loadShopReviews(Number(props.id), page, 10)
}

watch(() => props.id, loadShop)
onMounted(loadShop)
</script>

<template>
  <div class="shell-container page-stack">
    <template v-if="loading">
      <section class="shop-hero shell-card shop-hero--loading">
        <el-skeleton animated>
          <template #template>
            <el-skeleton-item variant="image" class="shop-hero__loading-cover" />
            <div class="shop-hero__loading-body">
              <el-skeleton :rows="6" animated />
              <el-skeleton :rows="4" animated />
            </div>
          </template>
        </el-skeleton>
      </section>
    </template>

    <ErrorBlock v-else-if="loadError" @retry="loadShop" />

    <EmptyState
      v-else-if="!loading && !shopViewModel"
      title="店铺暂时不可查看"
      description="该店铺可能尚未通过审核，或公开数据暂时不可用。"
    >
      <el-button type="primary" @click="$router.push('/app/products')">继续浏览商品</el-button>
    </EmptyState>

    <template v-else-if="shopViewModel">
      <section class="shop-hero shell-card">
        <div class="shop-hero__cover" :style="shopViewModel.coverUrl ? { backgroundImage: `url(${shopViewModel.coverUrl})` } : undefined" />

        <div class="shop-hero__body">
          <div class="shop-hero__main">
            <div class="shop-hero__eyebrows">
              <span class="eyebrow">店铺主页</span>
              <el-tag round effect="plain">{{ shopViewModel.reviewStatusLabel }}</el-tag>
              <el-tag round effect="plain" type="success">{{ shopViewModel.statusLabel }}</el-tag>
            </div>

            <div class="shop-hero__headline">
              <h1>{{ shopViewModel.name }}</h1>
              <p>{{ shopViewModel.slogan || shopViewModel.description || '这是一家面向校园公开经营的店铺主页。' }}</p>
            </div>

            <div class="shop-hero__actions">
              <el-button type="primary" size="large" @click="handleContactOwner">联系店主</el-button>
              <el-button plain size="large" @click="handleOpenShopMessagesHub">消息入口</el-button>
              <el-button plain size="large" disabled>关注店铺</el-button>
            </div>

            <div class="shop-hero__owner shell-card">
              <div>
                <span class="shop-hero__owner-label">店铺对外主体</span>
                <strong>{{ shopViewModel.ownerName }}</strong>
                <p>普通个人信息不再与店铺主页竞争展示，当前以店主昵称作为对外主体的最小公开信息。</p>
              </div>
            </div>
          </div>

          <aside class="shop-hero__sidebar">
            <article class="shop-hero__stat">
              <span>店铺评分</span>
              <strong>{{ formatMetric(shopViewModel.rating) }}</strong>
            </article>
            <article class="shop-hero__stat">
              <span>在售商品</span>
              <strong>{{ products.length }}</strong>
            </article>
            <article class="shop-hero__stat">
              <span>关注人数</span>
              <strong>{{ formatMetric(shopViewModel.followers) }}</strong>
            </article>
            <article class="shop-hero__stat">
              <span>回复效率</span>
              <strong>{{ shopViewModel.responseRate || '待接口补充' }}</strong>
            </article>
          </aside>
        </div>
      </section>

      <PageSection title="店铺介绍" description="了解这家店铺的定位和主营方向。">
        <div class="shop-grid shop-grid--intro">
          <article class="shop-panel shell-card">
            <h3>店铺说明</h3>
            <p>{{ shopViewModel.description || '店主暂未补充更详细的店铺介绍。' }}</p>
          </article>

          <article class="shop-panel shell-card">
            <h3>店铺公告</h3>
            <p>{{ shopViewModel.announcement || '当前没有公开公告。' }}</p>
          </article>
        </div>

        <div v-if="shopViewModel.capabilityTags.length" class="shop-capability-tags">
          <el-tag
            v-for="tag in shopViewModel.capabilityTags"
            :key="tag"
            effect="plain"
            round
          >
            {{ tag }}
          </el-tag>
        </div>
      </PageSection>

      <PageSection title="店主信息" description="这位店主的校园身份和认证信息。">
        <div class="shop-grid shop-grid--identity">
          <article v-for="item in liveIdentityCards" :key="item.label" class="shop-panel shell-card">
            <span class="shop-panel__eyebrow">{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <p>{{ item.note }}</p>
          </article>

          <article class="shop-panel shell-card is-reserved">
            <span class="shop-panel__eyebrow">校园身份精选</span>
            <strong>待接口补充</strong>
            <p>如院系、认证标签、经营年限等精选身份信息，需要后续店铺主体接口补充。</p>
          </article>
        </div>
      </PageSection>

      <PageSection title="信用与交易数据" description="店铺的交易记录和信誉评分。">
        <template #actions>
          <el-tag :type="shopInsightReserved ? 'warning' : 'success'" effect="plain">
            {{ shopInsightReserved ? '部分数据加载中' : '数据已更新' }}
          </el-tag>
        </template>

        <el-alert
          v-if="shopInsightFailed"
          type="error"
          show-icon
          :closable="false"
          :title="marketStore.shopInsightError || '店铺经营统计加载失败'"
          description="部分数据暂时无法加载，已为你展示可用信息。"
        />
        <el-alert
          v-else-if="shopInsightReserved"
          type="warning"
          show-icon
          :closable="false"
          title="更多经营数据即将上线"
          description="更多店铺功能即将上线。"
        />

        <div class="shop-metric-grid">
          <ReservedMetricCard
            v-if="shopInsightReserved"
            label="本月销售额"
            :scope="shopMetricDef('monthlySalesAmount').scope"
            :data-source="shopMetricDef('monthlySalesAmount').dataSource"
            unit="元"
          />
          <article v-else class="shop-metric-card shell-card">
            <span>本月销售额</span>
            <strong>¥{{ formatMoney(shopInsight?.monthlySalesAmount) }}</strong>
          </article>

          <ReservedMetricCard
            v-if="shopInsightReserved"
            label="本月订单数"
            :scope="shopMetricDef('monthlyOrderCount').scope"
            :data-source="shopMetricDef('monthlyOrderCount').dataSource"
            unit="单"
          />
          <article v-else class="shop-metric-card shell-card">
            <span>本月订单数</span>
            <strong>{{ formatMetric(shopInsight?.monthlyOrderCount, ' 单') }}</strong>
          </article>

          <ReservedMetricCard
            v-if="shopInsightReserved"
            label="浏览总量"
            :scope="shopMetricDef('viewCountSummary').scope"
            :data-source="shopMetricDef('viewCountSummary').dataSource"
          />
          <article v-else class="shop-metric-card shell-card">
            <span>浏览总量</span>
            <strong>{{ formatMetric(shopInsight?.viewCountSummary) }}</strong>
          </article>

          <ReservedMetricCard
            v-if="shopInsightReserved"
            label="收藏总量"
            :scope="shopMetricDef('favoriteCountSummary').scope"
            :data-source="shopMetricDef('favoriteCountSummary').dataSource"
          />
          <article v-else class="shop-metric-card shell-card">
            <span>收藏总量</span>
            <strong>{{ formatMetric(shopInsight?.favoriteCountSummary) }}</strong>
          </article>
        </div>

        <div class="shop-grid shop-grid--analytics">
          <article class="shop-panel shell-card">
            <h3>回购与热销</h3>
            <div v-if="!shopInsightReserved && shopInsight?.hotProducts?.length" class="shop-hot-list">
              <div v-for="item in shopInsight.hotProducts" :key="item.productId" class="shop-hot-list__item">
                <div>
                  <strong>{{ item.title }}</strong>
                  <p>已售 {{ item.soldCount }} · 收藏 {{ item.favoriteCount }} · 浏览 {{ item.viewCount }}</p>
                </div>
                <el-button text type="primary" @click="handleGoProduct(item.productId)">查看</el-button>
              </div>
              <div class="shop-hot-list__foot">
                <span>复购买家</span>
                <strong>{{ formatMetric(shopInsight?.repeatBuyerCount, ' 人') }}</strong>
              </div>
            </div>
            <ReservedPanel
              v-else
              title="热销榜与复购信号"
              description="更详细的经营数据即将展示。"
              icon="⏳"
            />
          </article>

          <article class="shop-panel shell-card">
            <h3>社群与经营入口</h3>
            <div class="shop-reserved-actions">
              <el-button plain disabled>关注店铺</el-button>
              <el-button plain disabled>粉丝群</el-button>
              <el-button plain disabled>优惠群</el-button>
            </div>
            <p class="shop-panel__hint">
              这些入口已经在信息架构中留位，但当前不会假装后端和真实社群链路已完成。
            </p>
          </article>
        </div>
      </PageSection>

      <PageSection title="店铺评价" description="看看其他买家对这家店的评价。">
        <div class="shop-grid shop-grid--reviews">
          <div class="shop-panel shell-card">
            <h3>评分概览</h3>
            <RatingSummary
              :summary="reviewStore.shopReviewSummary"
              :loading="reviewStore.loadingShopSummary"
            />
          </div>

          <div class="shop-panel shell-card">
            <h3>评价详情</h3>
            <ReviewList
              :reviews="reviewStore.shopReviews"
              :loading="reviewStore.loadingShopReviews"
              :total="reviewStore.shopReviewTotal"
              :page="reviewPage"
              @update:page="handleReviewPageChange"
            />
          </div>
        </div>
      </PageSection>

      <PageSection title="店铺商品" description="这家店铺正在出售的所有商品。">
        <div v-if="products.length" class="product-grid">
          <article v-for="item in products" :key="item.id" class="product-card shell-card">
            <img :src="item.cover" :alt="item.title" class="product-card__cover" />
            <div class="product-card__body">
              <div class="product-card__meta">
                <el-tag size="small">{{ item.categoryName }}</el-tag>
                <el-tag size="small" effect="plain">{{ item.productType || item.type }}</el-tag>
              </div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.subtitle || item.description }}</p>
              <div class="price-row">
                <strong>¥{{ Number(item.salePrice || item.price || 0).toFixed(2) }}</strong>
                <span>{{ item.favoriteCount }} 收藏</span>
              </div>
              <el-button type="primary" plain @click="handleGoProduct(item.id)">查看商品</el-button>
            </div>
          </article>
        </div>
        <EmptyState
          v-else
          title="这家店铺暂时没有公开在售商品"
          description="该店铺暂时还没有在售商品。"
        />
      </PageSection>
    </template>
  </div>
</template>

<style scoped>
.shop-hero {
  overflow: hidden;
  padding: 0;
}

.shop-hero--loading {
  display: grid;
  gap: 0;
}

.shop-hero__loading-cover :deep(.el-skeleton__image) {
  width: 100%;
  height: 220px;
  border-radius: 0;
}

.shop-hero__loading-body {
  display: grid;
  gap: 24px;
  padding: 28px;
}

.shop-hero__cover {
  min-height: 220px;
  background:
    radial-gradient(circle at top left, rgba(var(--cm-primary-rgb), 0.18), transparent 28%),
    radial-gradient(circle at 80% 20%, rgba(var(--cm-accent-rgb), 0.16), transparent 22%),
    linear-gradient(140deg, #f4e6d8 0%, #f8f1e8 48%, #efe2d3 100%);
  background-size: cover;
  background-position: center;
}

.shop-hero__body {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(260px, 0.8fr);
  gap: 24px;
  padding: 28px;
  margin-top: -76px;
}

.shop-hero__main,
.shop-hero__sidebar,
.shop-panel {
  display: grid;
  gap: 16px;
}

.shop-hero__main {
  padding: 24px;
  border-radius: var(--cm-radius-lg);
  background: rgba(255, 250, 243, 0.9);
  backdrop-filter: blur(var(--cm-blur-medium));
  box-shadow: var(--cm-shadow-soft);
}

.shop-hero__eyebrows {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.shop-hero__headline {
  display: grid;
  gap: 10px;
}

.shop-hero__headline h1 {
  margin: 0;
  font-size: clamp(34px, 5vw, 48px);
  line-height: 1.02;
}

.shop-hero__headline p,
.shop-hero__owner p,
.shop-panel p,
.shop-hot-list__item p,
.shop-panel__hint {
  margin: 0;
  color: var(--cm-text-secondary);
  line-height: 1.7;
}

.shop-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.shop-hero__owner {
  padding: 18px 20px;
  background: rgba(255, 255, 255, 0.76);
}

.shop-hero__owner-label,
.shop-panel__eyebrow {
  display: inline-block;
  margin-bottom: 8px;
  color: var(--cm-text-tertiary);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.shop-hero__owner strong,
.shop-panel strong,
.shop-hot-list__foot strong {
  font-size: 22px;
  line-height: 1.25;
}

.shop-hero__sidebar {
  align-content: start;
}

.shop-hero__stat,
.shop-metric-card {
  display: grid;
  gap: 8px;
  padding: 18px 20px;
  border-radius: var(--cm-radius-md);
  background: var(--cm-surface-strong);
  box-shadow: var(--cm-shadow-soft);
}

.shop-hero__stat span,
.shop-metric-card span {
  color: var(--cm-text-tertiary);
  font-size: 13px;
}

.shop-hero__stat strong,
.shop-metric-card strong {
  font-size: 28px;
  color: var(--cm-price);
}

.shop-grid {
  display: grid;
  gap: 18px;
}

.shop-grid--intro,
.shop-grid--analytics,
.shop-grid--reviews {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.shop-grid--identity {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.shop-capability-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.shop-panel h3 {
  margin: 0;
}

.shop-panel.is-reserved {
  border: 1px dashed rgba(var(--cm-primary-rgb), 0.28);
  background: rgba(var(--cm-primary-rgb), 0.06);
}

.shop-metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.shop-hot-list {
  display: grid;
  gap: 14px;
}

.shop-hot-list__item,
.shop-hot-list__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid var(--cm-border);
}

.shop-hot-list__item:last-child {
  border-bottom: none;
}

.shop-hot-list__foot {
  border-bottom: none;
  padding-top: 8px;
}

.shop-reserved-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 980px) {
  .shop-hero__body,
  .shop-grid--intro,
  .shop-grid--analytics,
  .shop-grid--reviews {
    grid-template-columns: 1fr;
  }

  .shop-hero__body {
    margin-top: -52px;
  }
}

@media (max-width: 640px) {
  .shop-hero__body {
    padding: 18px;
  }

  .shop-hero__main {
    padding: 20px;
  }

  .shop-hero__actions,
  .shop-reserved-actions,
  .shop-hot-list__item,
  .shop-hot-list__foot {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
