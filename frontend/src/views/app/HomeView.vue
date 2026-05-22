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
    eyebrow: 'FOR STUDENTS',
    title: '不只是二手市场。',
    desc: 'Youyu 把校园交易、认证、开店和售后放进一条更完整的链路里，让它更像一个可信的学生商业网络。'
  },
  {
    eyebrow: 'FOR SELLERS',
    title: '发布、认证、经营，在同一个起点完成。',
    desc: '从宿舍闲置到长期经营的小店入口，我们让探索、交易和身份可信同时成立，而不是拆成零碎工具。'
  },
  {
    eyebrow: 'FOR TRUST',
    title: '不是匿名群消息，而是可追踪的交易体验。',
    desc: '身份验证、订单状态和平台介入能力都被前置到体验里，让校园内交易真正更安心。'
  }
]

const statCards = [
  {
    value: '12K+',
    label: '认证学生用户',
    desc: '覆盖买家、卖家与校内个体经营者的可信身份网络。',
    tilt: 'left'
  },
  {
    value: '28K+',
    label: '在架校园好物',
    desc: '从教材、数码、宿舍到轻服务，持续形成内容供给。',
    tilt: 'right'
  },
  {
    value: '150+',
    label: '校园服务范围',
    desc: '围绕宿舍生活、学习交易和学生副业场景持续扩展。',
    tilt: 'left'
  }
]

const entryCards = [
  {
    title: '探索入口',
    desc: '先从精选与全部商品出发，快速进入平台最有吸引力的内容面。',
    action: '进入探索',
    handler: () => router.push('/app/explore')
  },
  {
    title: '认证入口',
    desc: '完成学生认证后，才能获得更完整的交易权限与身份可信度。',
    action: '开始认证',
    handler: () => router.push('/app/verification')
  },
  {
    title: '开店入口',
    desc: '把一次性闲置发布，升级成一个可持续经营的校园小店。',
    action: '去开店',
    handler: () => router.push('/app/shop/manage/publish')
  }
]

const footerGroups = [
  {
    title: '平台入口',
    links: [
      { label: '探索好物', handler: () => router.push('/app/explore') },
      { label: '精选推荐', handler: () => document.getElementById('home-featured-rail')?.scrollIntoView({ behavior: 'smooth', block: 'start' }) },
      { label: '学生认证', handler: () => router.push('/app/verification') },
      { label: '开店经营', handler: () => router.push('/app/shop/manage/publish') }
    ]
  },
  {
    title: '交易场景',
    links: [
      { label: '教材资料', handler: () => router.push({ path: '/app/explore', query: { keyword: '教材' } }) },
      { label: '数码设备', handler: () => router.push({ path: '/app/explore', query: { keyword: '数码' } }) },
      { label: '宿舍生活', handler: () => router.push({ path: '/app/explore', query: { keyword: '宿舍' } }) },
      { label: '校园服务', handler: () => router.push({ path: '/app/explore', query: { keyword: '服务' } }) }
    ]
  },
  {
    title: '卖家能力',
    links: [
      { label: '发布商品', handler: () => router.push('/app/shop/manage/publish') },
      { label: '管理店铺', handler: () => router.push('/app/shop/manage') },
      { label: '查看订单', handler: () => router.push('/app/orders') },
      { label: '身份可信', handler: () => router.push('/app/verification') }
    ]
  },
  {
    title: '帮助与说明',
    links: [
      { label: '为什么要认证', handler: () => router.push('/app/verification') },
      { label: '如何开始探索', handler: () => router.push('/app/explore') },
      { label: '平台交易保障', handler: () => router.push('/app/orders') },
      { label: '返回登录', handler: () => router.push('/login') }
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
        <span class="eyebrow">Youyu</span>
        <h1 class="home-hero__title">把校园里的交易、<br>信任与经营，放进同一个平台。</h1>
        <p class="home-hero__desc">
          Youyu 不是一个只会堆商品的首页。它更像学生自己的商业入口，让你在一个地方完成发现、交易、认证和开店。
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
        <span class="eyebrow">Platform Signals</span>
        <h2>平台规模先建立信任，再让内容完成转化。</h2>
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
        <span class="eyebrow">Key Entrances</span>
        <h2>从探索、认证到开店，首页把关键入口直接摊开。</h2>
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
        <div class="home-footer__top">
          <span class="eyebrow">Youyu</span>
          <button type="button" class="home-footer__login" @click="router.push('/login')">
            登录 →
          </button>
        </div>

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
          <span>Vue 3 · Vue Router · Pinia · Element Plus</span>
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
  max-width: 10.8ch;
  font-size: clamp(42px, 6vw, 78px);
  line-height: 0.98;
  letter-spacing: -0.045em;
  font-weight: 800;
  color: var(--cm-text);
}

.home-hero__desc {
  max-width: 760px;
  color: var(--cm-text-secondary);
  font-size: clamp(17px, 1.5vw, 21px);
  line-height: 1.8;
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
  padding: 56px 0 0;
  background: none;
}

.home-footer::before {
  content: '';
  position: absolute;
  inset: 0;
  left: 50%;
  width: 100vw;
  transform: translateX(-50%);
  background: linear-gradient(180deg, #4a372f 0%, #3f2f28 58%, #392a24 100%);
  border-top: 1px solid rgba(255, 250, 243, 0.08);
  z-index: 0;
}

.home-footer__inner {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 0;
  padding: 0;
  background: linear-gradient(180deg, #4a372f 0%, #3f2f28 58%, #392a24 100%);
}

.home-footer__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 0 28px;
  border-bottom: 1px solid rgba(255, 250, 243, 0.12);
}

.home-footer__login {
  padding: 0;
  border: 0;
  background: transparent;
  color: rgba(255, 250, 243, 0.9);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: color var(--cm-transition-micro);
}

.home-footer__login:hover {
  color: #ffffff;
}

.home-footer__grid {
  display: grid;
  grid-template-columns: 1.35fr 1.25fr 1.25fr 0.95fr;
  gap: 0;
  padding-top: 20px;
}

.home-footer__column {
  display: grid;
  align-content: start;
  gap: 12px;
  min-height: 236px;
  padding: 8px 24px 10px;
  border-right: 1px solid rgba(255, 250, 243, 0.08);
}

.home-footer__column:last-child {
  border-right: 0;
}

.home-footer__column h3 {
  margin-bottom: 4px;
  font-size: 17px;
  font-weight: 700;
  color: #fff6ea;
}

.home-footer__link {
  width: fit-content;
  padding: 0;
  border: 0;
  background: transparent;
  color: rgba(255, 244, 230, 0.72);
  font-size: 15px;
  font-weight: 500;
  line-height: 1.55;
  text-align: left;
  cursor: pointer;
  transition: color var(--cm-transition-micro);
}

.home-footer__link:hover {
  color: #ffffff;
}

.home-footer__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 26px 0 0;
  border-top: 1px solid rgba(255, 250, 243, 0.08);
  color: rgba(255, 244, 230, 0.58);
  font-size: 14px;
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
    gap: 24px 0;
  }

  .home-footer__column:nth-child(2n) {
    border-right: 0;
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
  .home-entry-card,
  .home-footer__inner {
    padding: 24px;
  }

  .home-footer__top {
    padding: 0 0 18px;
  }

  .home-footer__grid {
    grid-template-columns: 1fr;
    border-top: 0;
    padding-top: 18px;
  }

  .home-footer__column {
    min-height: auto;
    padding: 0 0 20px;
    border-right: 0;
    border-bottom: 1px solid rgba(255, 250, 243, 0.08);
  }

  .home-footer__column:last-child {
    border-bottom: 0;
    padding-bottom: 0;
  }

  .home-footer__bottom {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
