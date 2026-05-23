<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from '@/plugins/element-plus-services'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import HomeFeaturedRail from '@/components/home/HomeFeaturedRail.vue'
import HomeStatsNetwork from '@/components/home/HomeStatsNetwork.vue'
import { useRecommendStore } from '@/stores/recommend'

const router = useRouter()
const recommendStore = useRecommendStore()

const loading = ref(false)
const loadError = ref(false)
const activeMetricId = ref('students')
const reducedMotion = ref(false)

const shouldShowRail = computed(() => loading.value || recommendStore.homeRecommendList.length > 0)

let statsRotationTimer = null
let reducedMotionQuery = null

const introHighlights = [
  {
    eyebrow: '买家',
    title: '逛、买、收货，一条线走完。',
    desc: '认证身份后解锁完整权限，每笔交易都有状态可查、有平台兜底。'
  },
  {
    eyebrow: '卖家',
    title: '从发布到经营，门槛极低。',
    desc: '宿舍闲置可以发，长期开店也可以——同一个入口，按需升级。'
  },
  {
    eyebrow: '信任',
    title: '实名认证，交易有据可查。',
    desc: '订单状态、退款流程和平台介入都在平台内完成，不依赖群聊截图。'
  }
]

const statsMetrics = [
  {
    id: 'students',
    value: '12K+',
    label: '认证学生',
    desc: '买家、卖家和校园店主都在同一张可信网络里'
  },
  {
    id: 'shops',
    value: '860+',
    label: '校园店铺',
    desc: '长期经营的小店、社团摊位和个人卖家持续入驻'
  },
  {
    id: 'products',
    value: '28K+',
    label: '上架商品',
    desc: '教材、数码、宿舍用品和校园服务持续更新'
  },
  {
    id: 'regions',
    value: '150+',
    label: '覆盖地区',
    desc: '学习、生活、副业和社群场景逐步连接起来'
  }
]

const activeMetricIndex = computed(() => {
  const index = statsMetrics.findIndex((metric) => metric.id === activeMetricId.value)
  return index >= 0 ? index : 0
})
const activeMetricColumn = computed(() => activeMetricIndex.value % 2)
const activeMetricRow = computed(() => Math.floor(activeMetricIndex.value / 2))

const entryCards = [
  {
    title: '去逛逛',
    desc: '精选推荐 + 全量商品，随时可以开始。',
    action: '进入探索',
    handler: () => router.push('/app/explore')
  },
  {
    title: '学生认证',
    desc: '完成认证，解锁完整交易权限和身份标识。',
    action: '立即认证',
    handler: () => router.push('/app/verification')
  },
  {
    title: '开店',
    desc: '发布第一件商品，就算开始了。',
    action: '去发布',
    handler: () => router.push('/app/shop/manage/publish')
  }
]

const footerGroups = [
  {
    title: '逛',
    links: [
      { label: '探索好物', handler: () => router.push('/app/explore') },
      { label: '精选推荐', handler: () => document.getElementById('home-featured-rail')?.scrollIntoView({ behavior: 'smooth', block: 'start' }) },
      { label: '收藏夹', handler: () => router.push('/app/favorites') },
      { label: '我的订单', handler: () => router.push('/app/orders') }
    ]
  },
  {
    title: '买',
    links: [
      { label: '教材资料', handler: () => router.push({ path: '/app/explore', query: { keyword: '教材' } }) },
      { label: '数码设备', handler: () => router.push({ path: '/app/explore', query: { keyword: '数码' } }) },
      { label: '宿舍用品', handler: () => router.push({ path: '/app/explore', query: { keyword: '宿舍' } }) },
      { label: '校园服务', handler: () => router.push({ path: '/app/explore', query: { keyword: '服务' } }) }
    ]
  },
  {
    title: '卖',
    links: [
      { label: '发布商品', handler: () => router.push('/app/shop/manage/publish') },
      { label: '管理店铺', handler: () => router.push('/app/shop/manage') },
      { label: '查看订单', handler: () => router.push('/app/orders') },
      { label: '学生认证', handler: () => router.push('/app/verification') }
    ]
  },
  {
    title: '帮助',
    links: [
      { label: '如何认证', handler: () => router.push('/app/verification') },
      { label: '交易保障', handler: () => router.push('/app/orders') },
      { label: '开始探索', handler: () => router.push('/app/explore') },
      { label: '登录', handler: () => router.push('/login') }
    ]
  }
]

async function loadHomePage() {
  loading.value = true
  loadError.value = false

  try {
    await recommendStore.loadHomeRecommend(8)
  } catch (error) {
    loadError.value = true
    ElMessage.error(error?.response?.data?.message || error?.message || '首页数据加载失败')
  } finally {
    loading.value = false
  }
}

function openProduct(product) {
  router.push(`/app/products/${product.id}`)
}

function clearStatsRotation() {
  if (statsRotationTimer) {
    window.clearTimeout(statsRotationTimer)
    statsRotationTimer = null
  }
}

function advanceMetric() {
  const currentIndex = statsMetrics.findIndex((metric) => metric.id === activeMetricId.value)
  const nextIndex = currentIndex >= 0 ? (currentIndex + 1) % statsMetrics.length : 0
  activeMetricId.value = statsMetrics[nextIndex].id
}

function scheduleStatsRotation() {
  clearStatsRotation()

  if (reducedMotion.value) {
    return
  }

  statsRotationTimer = window.setTimeout(() => {
    advanceMetric()
    scheduleStatsRotation()
  }, 10000)
}

function selectStatsMetric(metricId) {
  activeMetricId.value = metricId
  scheduleStatsRotation()
}

function updateReducedMotionState(event) {
  reducedMotion.value = event.matches
}

function bindReducedMotionQuery() {
  if (!window.matchMedia) {
    scheduleStatsRotation()
    return
  }

  reducedMotionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
  updateReducedMotionState(reducedMotionQuery)

  if (reducedMotionQuery.addEventListener) {
    reducedMotionQuery.addEventListener('change', updateReducedMotionState)
  } else {
    reducedMotionQuery.addListener(updateReducedMotionState)
  }

  scheduleStatsRotation()
}

function unbindReducedMotionQuery() {
  if (!reducedMotionQuery) {
    return
  }

  if (reducedMotionQuery.removeEventListener) {
    reducedMotionQuery.removeEventListener('change', updateReducedMotionState)
  } else {
    reducedMotionQuery.removeListener(updateReducedMotionState)
  }

  reducedMotionQuery = null
}

watch(reducedMotion, () => {
  scheduleStatsRotation()
})

onMounted(() => {
  loadHomePage()
  bindReducedMotionQuery()
})

onBeforeUnmount(() => {
  clearStatsRotation()
  unbindReducedMotionQuery()
})
</script>

<template>
  <div class="home-view">
    <section class="home-hero">
      <div class="home-hero__inner shell-container">
        <span class="eyebrow">Youyu · 校园交易平台</span>
        <h1 class="home-hero__title">校园里的<br>买卖，更可信。</h1>
        <p class="home-hero__desc">
          发现好物、完成交易、认证身份、开店经营——都在这里。
        </p>

        <div class="home-hero__highlights">
          <article
            v-for="item in introHighlights"
            :key="item.title"
            class="home-hero__highlight"
          >
            <span class="home-hero__highlight-eyebrow">{{ item.eyebrow }}</span>
            <strong>{{ item.title }}</strong>
            <span>{{ item.desc }}</span>
          </article>
        </div>

        <div class="home-hero__actions">
          <el-button type="primary" size="large" @click="router.push('/app/explore')">探索好物</el-button>
          <el-button plain size="large" @click="router.push('/app/verification')">学生认证</el-button>
        </div>
      </div>
    </section>

    <section class="home-stats">
      <div class="home-stats__inner shell-container">
        <div class="home-stats__heading">
          <span class="eyebrow">平台数据</span>
          <h2>校园交易网络正在变密。</h2>
        </div>

        <div
          class="home-stats__metrics"
          :class="{ 'is-reduced-motion': reducedMotion }"
          role="tablist"
          aria-label="平台数据指标"
          :style="{
            '--active-stat-index': activeMetricIndex,
            '--active-stat-column': activeMetricColumn,
            '--active-stat-row': activeMetricRow,
            '--stat-count': statsMetrics.length
          }"
        >
          <button
            v-for="item in statsMetrics"
            :id="`home-stat-tab-${item.id}`"
            :key="item.id"
            type="button"
            class="home-stats__metric"
            :class="{ 'is-active': item.id === activeMetricId }"
            role="tab"
            :aria-selected="item.id === activeMetricId"
            :aria-controls="'home-stats-network'"
            @click="selectStatsMetric(item.id)"
          >
            <strong>{{ item.value }}</strong>
            <span class="home-stats__metric-label">{{ item.label }}</span>
            <span class="home-stats__metric-desc">{{ item.desc }}</span>
            <span class="home-stats__metric-flow" />
          </button>
        </div>
      </div>

      <div id="home-stats-network" class="home-stats__stage-band" role="tabpanel" :aria-labelledby="`home-stat-tab-${activeMetricId}`">
        <div class="home-stats__stage">
          <HomeStatsNetwork
            :metrics="statsMetrics"
            :active-metric-id="activeMetricId"
            :reduced-motion="reducedMotion"
          />
        </div>
      </div>
    </section>

    <div v-if="loadError" class="home-error shell-container">
      <ErrorBlock @retry="loadHomePage" />
    </div>

    <HomeFeaturedRail
      id="home-featured-rail"
      v-else-if="shouldShowRail"
      :products="recommendStore.homeRecommendList"
      :loading="loading"
      @open-product="openProduct"
    />

    <section class="home-entries shell-container">
      <div class="home-entries__heading">
        <span class="eyebrow">快速入口</span>
        <h2>从哪里开始都行。</h2>
      </div>

      <div class="home-entries__grid">
        <article v-for="item in entryCards" :key="item.title" class="home-entry-card">
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
          <button type="button" class="home-entry-card__action" @click="item.handler()">
            {{ item.action }} →
          </button>
        </article>
      </div>
    </section>

    <section class="home-footer shell-container">
      <div class="home-footer__inner">
        <div class="home-footer__grid">
          <article
            v-for="group in footerGroups"
            :key="group.title"
            class="home-footer__column"
          >
            <h3>{{ group.title }}</h3>
            <button
              v-for="link in group.links"
              :key="link.label"
              type="button"
              class="home-footer__link"
              @click="link.handler()"
            >
              {{ link.label }}
            </button>
          </article>
        </div>

        <div class="home-footer__bottom">
          <span>Youyu</span>
          <span>© 2026</span>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-view {
  display: flex;
  flex-direction: column;
  gap: 44px;
  min-height: 100vh;
  padding-bottom: 0;
}

.home-hero {
  padding: 72px 0 20px;
  background:
    radial-gradient(circle at 18% 26%, rgba(182, 95, 59, 0.14), transparent 34%),
    radial-gradient(circle at 82% 16%, rgba(196, 122, 44, 0.11), transparent 30%),
    linear-gradient(180deg, #fffaf3 0%, #f8efe4 58%, #f1e3d3 100%);
}

.home-hero__inner {
  display: grid;
  gap: 28px;
  max-width: 1120px;
}

.home-hero__title {
  font-size: clamp(44px, 6.4vw, 80px);
  line-height: 1.0;
  letter-spacing: -0.04em;
  font-weight: 800;
  color: var(--cm-text);
}

.home-hero__desc {
  max-width: 480px;
  color: var(--cm-text-secondary);
  font-size: clamp(16px, 1.4vw, 19px);
  line-height: 1.75;
}

.home-hero__highlights {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.home-hero__highlight {
  display: grid;
  gap: 10px;
  padding: 22px 22px 24px;
  border-radius: 24px;
  background: rgba(255, 252, 248, 0.74);
  border: 1px solid rgba(88, 62, 43, 0.08);
  box-shadow: 0 24px 60px rgba(88, 62, 43, 0.08);
  backdrop-filter: blur(10px);
}

.home-hero__highlight:nth-child(1) {
  transform: rotate(-3deg) translateY(8px);
}

.home-hero__highlight:nth-child(2) {
  transform: rotate(2deg) translateY(-6px);
}

.home-hero__highlight:nth-child(3) {
  transform: rotate(-2deg) translateY(12px);
}

.home-hero__highlight-eyebrow {
  color: var(--cm-primary);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.home-hero__highlight strong {
  font-size: 18px;
  line-height: 1.35;
  color: var(--cm-text);
}

.home-hero__highlight span:last-child {
  color: var(--cm-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.home-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.home-hero__actions .el-button.is-plain {
  box-shadow: none;
}

.home-entries,
.home-footer {
  display: grid;
  gap: 24px;
}

.home-stats {
  position: relative;
  display: grid;
  gap: 0;
  margin-top: 8px;
  padding-top: 12px;
  overflow: hidden;
  background:
    radial-gradient(circle at 50% 64%, rgba(var(--cm-primary-rgb), 0.08), transparent 56%),
    linear-gradient(180deg, rgba(255, 250, 243, 0) 0%, rgba(255, 250, 243, 0.78) 12%, #fffaf3 32%, #f8efe4 74%, rgba(255, 250, 243, 0.72) 100%);
}

.home-stats__inner {
  position: relative;
  z-index: 2;
  display: grid;
  gap: 24px;
}

.home-entries {
  margin-top: 8px;
}

.home-stats__heading,
.home-entries__heading {
  display: grid;
  gap: 10px;
  max-width: 760px;
}

.home-stats__heading h2,
.home-entries__heading h2 {
  font-size: 42px;
  line-height: 1.05;
  letter-spacing: 0;
}

.home-entries__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}

.home-entry-card {
  display: grid;
  gap: 12px;
  padding: 30px;
}

.home-stats__metrics {
  position: relative;
  isolation: isolate;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  overflow: hidden;
  border-top: 1px solid rgba(88, 62, 43, 0.08);
  border-bottom: 1px solid rgba(88, 62, 43, 0.05);
  background:
    linear-gradient(90deg, rgba(88, 62, 43, 0.035) 1px, transparent 1px) 25% 0 / 25% 100% no-repeat,
    linear-gradient(180deg, rgba(255, 250, 243, 0.38), rgba(255, 250, 243, 0.14));
}

.home-stats__metrics::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  z-index: 0;
  width: calc(100% / var(--stat-count, 4));
  background:
    linear-gradient(180deg, rgba(255, 250, 243, 0.9), rgba(255, 250, 243, 0.42)),
    linear-gradient(135deg, rgba(var(--cm-primary-rgb), 0.12), rgba(var(--cm-accent-rgb), 0.08));
  box-shadow:
    inset 0 -2px 0 var(--cm-text),
    0 18px 44px rgba(88, 62, 43, 0.07);
  transform: translateX(calc(var(--active-stat-index, 0) * 100%));
  transition:
    transform 660ms var(--cm-ease-enter),
    background-color var(--cm-transition);
}

.home-stats__metrics::after {
  content: '';
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(36, 25, 20, 0.12), transparent);
}

.home-stats__metric {
  position: relative;
  z-index: 1;
  display: grid;
  align-content: start;
  gap: 8px;
  min-height: 168px;
  padding: 24px 22px;
  border: 0;
  background: transparent;
  color: var(--cm-text-tertiary);
  text-align: left;
  cursor: pointer;
  transition:
    color var(--cm-transition),
    background-color var(--cm-transition);
}

.home-stats__metric:hover,
.home-stats__metric:focus-visible,
.home-stats__metric.is-active {
  color: var(--cm-text);
  background: transparent;
}

.home-stats__metric:focus-visible {
  outline: 2px solid rgba(var(--cm-primary-rgb), 0.32);
  outline-offset: -2px;
}

.home-stats__metric strong {
  font-size: 48px;
  line-height: 1;
  font-weight: 800;
  letter-spacing: 0;
}

.home-stats__metric-label {
  font-size: 16px;
  line-height: 1.35;
  font-weight: 700;
}

.home-stats__metric-desc {
  max-width: 220px;
  color: inherit;
  font-size: 13px;
  line-height: 1.65;
  opacity: 0.82;
}

.home-stats__metric-flow {
  position: absolute;
  right: 22px;
  bottom: 13px;
  left: 22px;
  height: 2px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(36, 25, 20, 0.1);
  opacity: 0;
  transform: scaleX(0.4);
  transform-origin: left center;
  transition: opacity var(--cm-transition), transform var(--cm-transition);
}

.home-stats__metric-flow::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, var(--cm-text), rgba(var(--cm-primary-rgb), 0.42));
  transform: scaleX(0);
  transform-origin: left center;
}

.home-stats__metric.is-active .home-stats__metric-flow {
  opacity: 1;
  transform: scaleX(1);
}

.home-stats__metric.is-active .home-stats__metric-flow::after {
  animation: homeStatsMetricFlow 10s linear infinite;
}

.home-stats__metrics.is-reduced-motion .home-stats__metric-flow::after {
  animation: none;
  transform: scaleX(1);
  opacity: 0.42;
}

.home-stats__stage-band {
  position: relative;
  overflow: hidden;
  margin-top: -1px;
  background:
    radial-gradient(circle at 50% 96%, rgba(var(--cm-primary-rgb), 0.11), transparent 58%),
    linear-gradient(180deg, rgba(255, 250, 243, 0.62) 0%, #fffaf3 16%, #f8efe4 66%, rgba(255, 250, 243, 0.68) 100%);
}

.home-stats__stage-band::before,
.home-stats__stage-band::after {
  content: '';
  position: absolute;
  right: 0;
  left: 0;
  z-index: 1;
  pointer-events: none;
}

.home-stats__stage-band::before {
  top: 0;
  height: 112px;
  background: linear-gradient(180deg, rgba(255, 250, 243, 0.9) 0%, rgba(255, 250, 243, 0.34) 48%, rgba(255, 250, 243, 0) 100%);
}

.home-stats__stage-band::after {
  bottom: 0;
  height: 88px;
  background: linear-gradient(0deg, rgba(255, 250, 243, 0.76) 0%, rgba(255, 250, 243, 0) 100%);
}

.home-stats__stage {
  position: relative;
  z-index: 0;
  width: 100%;
  padding: 0;
}

.home-entry-card {
  border-radius: 22px;
  background: var(--cm-bg-elevated);
  border: 1px solid var(--cm-border);
  box-shadow: var(--cm-shadow-soft);
  transition: transform var(--cm-transition-micro), border-color var(--cm-transition-micro);
}

.home-entry-card:hover {
  transform: translateY(-3px);
  border-color: rgba(88, 62, 43, 0.18);
}

.home-entry-card h3 {
  font-size: 20px;
  line-height: 1.3;
  color: var(--cm-text);
}

.home-entry-card p {
  color: var(--cm-text-secondary);
  font-size: 14px;
  line-height: 1.75;
}

.home-entry-card__action {
  width: fit-content;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--cm-primary);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.home-error {
  padding-top: 6px;
}

.home-footer {
  position: relative;
  margin-top: auto;
  padding: 0;
  background: none;
}

.home-footer::before {
  content: '';
  position: absolute;
  inset: 0;
  left: 50%;
  width: 100vw;
  transform: translateX(-50%);
  background: #ede0d4;
  border-top: 1px solid rgba(88, 62, 43, 0.1);
  z-index: 0;
}

.home-footer__inner {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 0;
  padding: 40px 0 0;
}

.home-footer__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
  padding-bottom: 32px;
}

.home-footer__column {
  display: grid;
  align-content: start;
  gap: 8px;
  padding: 0 24px 0 0;
}

.home-footer__column:last-child {
  padding-right: 0;
}

.home-footer__column h3 {
  margin-bottom: 6px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--cm-text-tertiary);
}

.home-footer__link {
  width: fit-content;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--cm-text-secondary);
  font-size: 14px;
  font-weight: 400;
  line-height: 1.6;
  text-align: left;
  cursor: pointer;
  transition: color var(--cm-transition-micro);
}

.home-footer__link:hover {
  color: var(--cm-text);
}

.home-footer__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 0 32px;
  border-top: 1px solid rgba(88, 62, 43, 0.08);
  color: var(--cm-text-tertiary);
  font-size: 12px;
}

@media (max-width: 960px) {
  .home-hero__highlights,
  .home-entries__grid {
    grid-template-columns: 1fr;
  }

  .home-stats__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    background: rgba(255, 250, 243, 0.26);
  }

  .home-stats__metrics::before {
    width: 50%;
    height: 50%;
    transform: translate(
      calc(var(--active-stat-column, 0) * 100%),
      calc(var(--active-stat-row, 0) * 100%)
    );
  }

  .home-stats__metric:nth-child(2n) {
    border-right: 0;
  }

  .home-stats__metric:nth-child(n + 3) {
    box-shadow: inset 0 1px 0 rgba(88, 62, 43, 0.08);
  }

  .home-hero__highlight {
    transform: none;
  }

  .home-footer__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 32px 0;
    padding-bottom: 32px;
  }

  .home-footer__column {
    padding-right: 16px;
  }

  .home-footer__column:nth-child(2n) {
    padding-right: 0;
  }
}

@media (max-width: 768px) {
  .home-view {
    gap: 34px;
  }

  .home-hero {
    padding-top: 52px;
  }

  .home-hero__title {
    font-size: clamp(34px, 9vw, 48px);
    line-height: 1.04;
  }

  .home-stats__heading h2,
  .home-entries__heading h2 {
    font-size: 32px;
    line-height: 1.12;
  }

  .home-stats__metric {
    min-height: 154px;
    padding: 20px 16px;
  }

  .home-stats__metric-flow {
    right: 16px;
    bottom: 10px;
    left: 16px;
  }

  .home-stats__metric strong {
    font-size: 38px;
  }

  .home-entry-card {
    padding: 24px;
  }

  .home-footer__inner {
    padding-top: 28px;
  }

  .home-footer__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 24px 0;
    padding-bottom: 20px;
  }

  .home-footer__column:nth-child(2n) {
    padding-right: 0;
  }

  .home-footer__bottom {
    flex-direction: row;
    padding-bottom: 24px;
  }
}

@keyframes homeStatsMetricFlow {
  0% {
    transform: scaleX(0);
  }

  100% {
    transform: scaleX(1);
  }
}
</style>
