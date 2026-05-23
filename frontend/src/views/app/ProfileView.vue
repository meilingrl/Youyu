<script setup>
import { computed, onMounted } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import PageSection from '@/components/common/PageSection.vue'
import { useAuthStore } from '@/stores/auth'
import { useMarketStore } from '@/stores/market'

const authStore = useAuthStore()
const marketStore = useMarketStore()

const profile = computed(() => marketStore.profile)
const myProducts = computed(() => marketStore.getMyProducts())
const userInsight = computed(() => marketStore.userInsightSnapshot)

function formatMoney(value) {
  if (value === null || value === undefined || value === '') return '--'
  return `\u00a5${Number(value || 0).toFixed(2)}`
}

function formatCount(value, unit = '') {
  if (value === null || value === undefined || value === '') return '--'
  return `${value}${unit}`
}

async function loadProfileData() {
  const results = await Promise.allSettled([
    marketStore.loadProfile(authStore.currentUser),
    marketStore.loadMyProducts(),
    marketStore.loadUserPreference(),
    marketStore.loadUserInsightSnapshot()
  ])

  if (results.some((item) => item.status === 'rejected')) {
    ElMessage.warning('个人中心部分数据暂未加载成功')
  }
}

onMounted(loadProfileData)
</script>

<template>
  <div class="shell-container page-stack">
    <section class="shell-card profile-hero">
      <img :src="profile.avatar" :alt="profile.nickname" class="profile-avatar" />
      <div class="profile-hero__content">
        <span class="eyebrow">{{ profile.school || 'Youyu' }}</span>
        <h1>{{ profile.nickname }}</h1>
        <p>{{ profile.bio || '还没有填写个人简介' }}</p>
        <div class="detail-inline-metrics">
          <span v-if="profile.major">{{ profile.major }}</span>
          <span v-if="profile.grade">{{ profile.grade }}</span>
          <span v-if="profile.campus">{{ profile.campus }}</span>
        </div>
      </div>
      <div class="profile-hero__actions">
        <el-button plain @click="$router.push('/app/settings')">设置</el-button>
        <el-button type="primary" @click="$router.push('/app/verification')">学生认证</el-button>
      </div>
    </section>

    <el-alert
      v-if="marketStore.profileError"
      :closable="false"
      type="warning"
      show-icon
      :title="marketStore.profileError"
      description="请刷新页面或稍后再试"
    />

    <PageSection title="账号与认证" description="查看认证状态、信用等级与交易权限">
      <div class="metric-grid metric-grid--wide">
        <div class="metric-card">
          <strong>{{ profile.verification.label }}</strong>
          <span>认证状态</span>
        </div>
        <div class="metric-card">
          <strong>{{ profile.creditLevel }}</strong>
          <span>{{ profile.creditScoreText || '暂无信用分' }}</span>
        </div>
        <div class="metric-card">
          <strong>{{ profile.privilege.canBuy ? '可购买' : '不可购买' }}</strong>
          <span>购买权限</span>
        </div>
        <div class="metric-card">
          <strong>{{ profile.privilege.canPublish ? '可发布' : '不可发布' }}</strong>
          <span>发布权限</span>
        </div>
      </div>
      <template #actions>
        <el-button plain @click="$router.push('/app/verification')">认证详情</el-button>
      </template>
    </PageSection>

    <PageSection title="交易概览" description="消费与在售数据来自订单统计">
      <div class="metric-grid">
        <div class="metric-card">
          <strong>{{ formatMoney(userInsight.totalSpendAmount) }}</strong>
          <span>累计消费</span>
        </div>
        <div class="metric-card">
          <strong>{{ formatCount(userInsight.totalPurchasedItemCount, ' 件') }}</strong>
          <span>已购商品</span>
        </div>
        <div class="metric-card">
          <strong>{{ formatCount(myProducts.length, ' 件') }}</strong>
          <span>在售商品</span>
        </div>
      </div>
      <template #actions>
        <el-button plain @click="$router.push('/app/trade')">查看交易中心</el-button>
      </template>
    </PageSection>

    <PageSection title="常用入口" description="收藏、店铺、偏好与订单的快捷入口">
      <div class="category-grid">
        <button class="category-chip" @click="$router.push('/app/favorites')">
          <span class="category-chip__emoji">❤️</span>
          <span class="category-chip__text">
            <strong>我的收藏</strong>
            <span>查看已收藏的商品</span>
          </span>
        </button>
        <button class="category-chip" @click="$router.push('/app/seller/products')">
          <span class="category-chip__emoji">🏪</span>
          <span class="category-chip__text">
            <strong>我的店铺</strong>
            <span>管理在售商品与发布</span>
          </span>
        </button>
        <button class="category-chip" @click="$router.push('/app/settings/preferences')">
          <span class="category-chip__emoji">⚙️</span>
          <span class="category-chip__text">
            <strong>偏好设置</strong>
            <span>调整推荐与展示偏好</span>
          </span>
        </button>
        <button class="category-chip" @click="$router.push('/app/orders')">
          <span class="category-chip__emoji">📦</span>
          <span class="category-chip__text">
            <strong>我的订单</strong>
            <span>查看全部订单记录</span>
          </span>
        </button>
      </div>
    </PageSection>

    <PageSection v-if="profile.addresses.length" title="收货地址" description="默认收货信息">
      <div class="metric-grid">
        <div class="metric-card">
          <strong>{{ profile.addresses[0]?.type || '默认地址' }}</strong>
          <span>{{ profile.addresses[0]?.detail }}</span>
          <span>{{ profile.addresses[0]?.contactName }} · {{ profile.addresses[0]?.phone }}</span>
        </div>
      </div>
    </PageSection>
  </div>
</template>

<style scoped>
.profile-hero {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 24px;
  align-items: center;
}

.profile-hero__content {
  display: grid;
  gap: 10px;
}

.profile-hero__actions {
  display: flex;
  gap: 12px;
  align-self: start;
}

@media (max-width: 860px) {
  .profile-hero {
    grid-template-columns: 1fr;
    text-align: center;
    justify-items: center;
  }

  .profile-hero__actions {
    justify-content: center;
  }
}
</style>
