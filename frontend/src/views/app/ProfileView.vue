<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import PageSection from '@/components/common/PageSection.vue'
import InsightBarList from '@/components/common/InsightBarList.vue'
import SpendChart from '@/components/common/SpendChart.vue'
import { useAuthStore } from '@/stores/auth'
import { useMarketStore } from '@/stores/market'

const authStore = useAuthStore()
const marketStore = useMarketStore()

const profile = computed(() => marketStore.profile)
const myProducts = computed(() => marketStore.getMyProducts())
const userInsight = computed(() => marketStore.userInsightSnapshot)
const spendAgg = computed(() => marketStore.spendAggregation)
const avatarInitial = computed(() => (profile.value.nickname || '友').slice(0, 1))
const avatarInputRef = ref(null)
const maxAvatarSize = 10 * 1024 * 1024
const spendTab = ref('monthly')
const spendTabs = [
  { key: 'monthly', label: '月度' },
  { key: 'yearly', label: '年度' },
  { key: 'category', label: '分类' }
]
const spendChartData = computed(() => {
  const agg = marketStore.spendAggregation
  if (!agg) return []
  if (spendTab.value === 'monthly') return agg.monthly || []
  if (spendTab.value === 'yearly') return agg.yearly || {}
  return agg.category || []
})
const editForm = reactive({
  nickname: ''
})

const monthlyChartData = computed(() => spendAgg.value?.monthly || [])
const yearlyChartData = computed(() => spendAgg.value?.yearly || {})
const categoryChartData = computed(() => {
  const fromOrders = spendAgg.value?.category || []
  if (fromOrders.length) {
    return fromOrders.map((item) => ({
      label: item.label || item.name || '其他',
      amount: Number(item.amount ?? item.value ?? 0)
    }))
  }

  const items = Array.isArray(userInsight.value.favoritePreferenceSummary)
    ? userInsight.value.favoritePreferenceSummary
    : []
  return items
    .filter((item) => Number(item.spendAmount || 0) > 0)
    .map((item) => ({
      label: item.categoryName || '未分类',
      amount: Number(item.spendAmount || 0)
    }))
})
const spendDataSource = computed(() => {
  if (spendAgg.value.source === 'orders') return '基于近期订单统计'
  return '月度明细待后端口径完善'
})

const categoryPreferenceBars = computed(() => {
  const items = Array.isArray(userInsight.value.favoritePreferenceSummary)
    ? userInsight.value.favoritePreferenceSummary
    : []

  return items.map((item) => ({
    id: `category-${item.categoryId || item.categoryName}`,
    label: item.categoryName || '未分类',
    value: Number(item.spendAmount || 0),
    displayValue: formatMoney(item.spendAmount),
    helper: `已购 ${Number(item.count || 0)} 件`,
    tone: 'primary'
  }))
})

const recentPurchaseBars = computed(() => {
  const items = Array.isArray(userInsight.value.recentBrowses) ? userInsight.value.recentBrowses : []

  return items.map((item, index) => ({
    id: `purchase-${item.productId || index}`,
    label: item.title || '订单记录',
    value: Number(item.subtotalAmount || 0),
    displayValue: formatMoney(item.subtotalAmount),
    helper: `${item.categoryName || '未分类'} · ${item.quantity || 0} 件 · ${item.purchasedAt || item.viewedAt || '时间暂缺'}`,
    tone: index === 0 ? 'warning' : 'info'
  }))
})

watch(
  () => profile.value.nickname,
  (nickname) => {
    editForm.nickname = nickname || ''
  },
  { immediate: true }
)

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
    marketStore.loadUserInsightSnapshot(),
    marketStore.loadSpendAggregation()
  ])

  if (results.some((item) => item.status === 'rejected')) {
    ElMessage.warning('个人中心部分数据暂未加载成功')
  }
}

async function saveProfile() {
  try {
    const updated = await marketStore.updateProfile({ nickname: editForm.nickname })
    authStore.updateCurrentUser({
      nickname: updated.nickname,
      avatar: updated.avatar,
      email: updated.email
    })
    ElMessage.success('个人资料已保存')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '个人资料保存失败')
  }
}

function pickAvatar() {
  avatarInputRef.value?.click()
}

async function handleAvatarFile(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.error('头像仅支持 JPG、PNG 或 WebP 图片')
    return
  }
  if (file.size > maxAvatarSize) {
    ElMessage.error('头像文件不能超过 10MB')
    return
  }

  try {
    const updated = await marketStore.uploadAvatar(file)
    authStore.updateCurrentUser({
      nickname: updated.nickname,
      avatar: updated.avatar,
      email: updated.email
    })
    ElMessage.success('头像已更新')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '头像上传失败')
  }
}

onMounted(loadProfileData)
</script>

<template>
  <div class="shell-container page-stack">
    <section class="shell-card profile-hero">
      <img v-if="profile.avatar" :src="profile.avatar" :alt="profile.nickname" class="profile-avatar" />
      <div v-else class="profile-avatar profile-avatar--fallback">{{ avatarInitial }}</div>
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
        <input
          ref="avatarInputRef"
          class="profile-avatar-input"
          type="file"
          accept="image/jpeg,image/png,image/webp"
          @change="handleAvatarFile"
        />
        <el-button plain :loading="marketStore.uploadingAvatar" @click="pickAvatar">更换头像</el-button>
        <el-button plain @click="$router.push('/app/settings')">设置</el-button>
        <el-button type="primary" @click="$router.push('/app/verification')">学生认证</el-button>
      </div>
    </section>

    <PageSection title="基本资料" description="昵称会展示给其他用户，登录账号不会被修改。">
      <el-form label-position="top" class="profile-edit-form">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="登录账号">
          <el-input :model-value="profile.loginId" disabled />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input :model-value="profile.email || '未绑定'" disabled />
        </el-form-item>
      </el-form>
      <template #actions>
        <el-button type="primary" :loading="marketStore.savingProfile" @click="saveProfile">保存资料</el-button>
      </template>
    </PageSection>

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

    <PageSection title="交易概览" description="支出与在售数据来自真实订单统计">
      <div class="metric-grid">
        <div class="metric-card">
          <strong>{{ formatMoney(userInsight.totalSpendAmount) }}</strong>
          <span>累计支出</span>
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

    <PageSection title="支出统计" description="按已完成且已支付订单统计支出结构。">
      <template #actions>
        <div class="spend-tabs">
          <button
            v-for="tab in spendTabs"
            :key="tab.key"
            :class="['spend-tab', { 'spend-tab--active': spendTab === tab.key }]"
            @click="spendTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>
      </template>

      <SpendChart
        v-if="spendTab === 'monthly'"
        mode="monthly"
        :monthly-data="monthlyChartData"
        :loading="marketStore.loadingSpendAggregation"
      />
      <SpendChart
        v-else-if="spendTab === 'yearly'"
        mode="yearly"
        :yearly-data="yearlyChartData"
        :loading="marketStore.loadingSpendAggregation"
      />
      <SpendChart
        v-else-if="spendTab === 'category'"
        mode="category"
        :category-data="categoryChartData"
        :loading="marketStore.loadingSpendAggregation"
      />
      <p class="spend-note">* 数据基于近期订单统计，仅供参考</p>

      <div class="profile-insight-grid">
        <article class="shell-card profile-insight-panel">
          <div class="profile-insight-panel__header">
            <h3>分类支出</h3>
            <span>按商品分类汇总</span>
          </div>
          <InsightBarList :items="categoryPreferenceBars" empty-text="暂无分类支出记录" />
        </article>
        <article class="shell-card profile-insight-panel">
          <div class="profile-insight-panel__header">
            <h3>近期支出</h3>
            <span>按最近完成时间排序</span>
          </div>
          <InsightBarList :items="recentPurchaseBars" empty-text="暂无近期支出记录" />
        </article>
      </div>
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
        <button class="category-chip" @click="$router.push('/app/coupons')">
          <span class="category-chip__emoji">券</span>
          <span class="category-chip__text">
            <strong>我的优惠券</strong>
            <span>查看已领取的店铺优惠券</span>
          </span>
        </button>
        <button class="category-chip" @click="$router.push('/app/seller/products')">
          <span class="category-chip__emoji">🏪</span>
          <span class="category-chip__text">
            <strong>我的店铺</strong>
            <span>管理在售商品与发布</span>
          </span>
        </button>
        <button class="category-chip" @click="$router.push('/app/seller/marketing')">
          <span class="category-chip__emoji">营</span>
          <span class="category-chip__text">
            <strong>店铺营销</strong>
            <span>管理优惠券与店铺活动</span>
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
  flex-wrap: wrap;
  justify-content: flex-end;
}

.profile-avatar-input {
  display: none;
}

.profile-avatar--fallback {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #f2f7ff, #fff4ec);
  color: var(--cm-primary);
  font-size: 32px;
  font-weight: 700;
}

.profile-edit-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.profile-insight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.profile-insight-panel {
  display: grid;
  gap: 16px;
  padding: 20px;
}

.profile-insight-panel__header {
  display: grid;
  gap: 4px;
}

.profile-insight-panel__header h3 {
  margin: 0;
  font-size: 18px;
}

.profile-insight-panel__header span {
  color: var(--cm-text-secondary);
  font-size: 13px;
}

.spend-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 16px;
  padding: 3px;
  background: var(--cm-surface-muted);
  border-radius: var(--cm-radius-pill);
  width: fit-content;
}

.spend-tab {
  padding: 6px 18px;
  border: none;
  border-radius: var(--cm-radius-pill);
  background: transparent;
  color: var(--cm-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--cm-transition-micro);
}

.spend-tab--active {
  background: var(--cm-surface-strong);
  color: var(--cm-primary);
  box-shadow: 0 1px 4px rgba(88, 62, 43, 0.1);
}

.spend-tab:hover:not(.spend-tab--active) {
  color: var(--cm-text);
}

.spend-note {
  margin-top: 12px;
  font-size: 12px;
  color: #a09286;
  text-align: right;
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

  .profile-edit-form,
  .profile-insight-grid {
    grid-template-columns: 1fr;
  }
}
</style>
