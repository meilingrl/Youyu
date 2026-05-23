<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from '@/plugins/element-plus-services'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import HomeFeaturedRail from '@/components/home/HomeFeaturedRail.vue'
import { useRecommendStore } from '@/stores/recommend'

const router = useRouter()
const recommendStore = useRecommendStore()

const loading = ref(false)
const loadError = ref(false)

const shouldShowRail = computed(() => loading.value || recommendStore.homeRecommendList.length > 0)

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

const statCards = [
  {
    value: '12K+',
    label: '认证学生',
    desc: '买家、卖家和校内小店主，都在同一张可信网络里。',
    tilt: 'left'
  },
  {
    value: '28K+',
    label: '在售商品',
    desc: '教材、数码、宿舍用品、轻服务，持续上新。',
    tilt: 'right'
  },
  {
    value: '150+',
    label: '覆盖场景',
    desc: '学习、生活、副业——校园里的交易需求基本都在这里。',
    tilt: 'left'
  }
]

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

onMounted(loadHomePage)
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

    <section class="home-stats shell-container">
      <div class="home-stats__heading">
        <span class="eyebrow">平台数据</span>
        <h2>已经有很多同学在这里了。</h2>
      </div>

      <div class="home-stats__grid">
        <article
          v-for="item in statCards"
          :key="item.label"
          class="home-stats__card"
          :class="item.tilt === 'left' ? 'home-stats__card--left' : 'home-stats__card--right'"
        >
          <strong>{{ item.value }}</strong>
          <h3>{{ item.label }}</h3>
          <p>{{ item.desc }}</p>
        </article>
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

.home-stats,
.home-entries,
.home-footer {
  display: grid;
  gap: 24px;
}

.home-stats,
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
  font-size: clamp(28px, 4vw, 46px);
  line-height: 1.05;
  letter-spacing: -0.04em;
}

.home-stats__grid,
.home-entries__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}

.home-stats__card,
.home-entry-card {
  display: grid;
  gap: 12px;
  padding: 30px;
}

.home-stats__card {
  border-radius: 28px;
  background: rgba(255, 252, 248, 0.78);
  border: 1px solid rgba(88, 62, 43, 0.08);
  box-shadow: 0 28px 72px rgba(88, 62, 43, 0.09);
  backdrop-filter: blur(10px);
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

.home-stats__card--left {
  transform: rotate(-3deg) translateY(8px);
}

.home-stats__card--right {
  transform: rotate(3deg) translateY(-8px);
}

.home-stats__card strong {
  font-size: clamp(42px, 5vw, 68px);
  line-height: 0.92;
  letter-spacing: -0.06em;
  color: var(--cm-text);
}

.home-stats__card h3,
.home-entry-card h3 {
  font-size: 20px;
  line-height: 1.3;
  color: var(--cm-text);
}

.home-stats__card p,
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
  .home-stats__grid,
  .home-entries__grid {
    grid-template-columns: 1fr;
  }

  .home-hero__highlight,
  .home-stats__card--left,
  .home-stats__card--right {
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

  .home-stats__card,
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
</style>
