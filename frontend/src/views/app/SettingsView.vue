<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from '@/plugins/element-plus-services'
import { useAuthStore } from '@/stores/auth'
import { useMarketStore } from '@/stores/market'

const authStore = useAuthStore()
const marketStore = useMarketStore()
const router = useRouter()

const profile = computed(() => marketStore.profile)
const ownedShop = computed(() => marketStore.ownedShop.shop)

const settingsSections = computed(() => [
  {
    title: '个人资料与头像',
    path: '/app/me',
    badge: '可修改',
    badgeType: 'success',
    description: '修改对外展示的昵称，上传头像图片。',
    detail: `当前昵称：${profile.value.nickname || '未设置'}`
  },
  {
    title: '邮箱绑定',
    path: '/app/settings/security',
    badge: profile.value.email ? '待验证' : '未绑定',
    badgeType: profile.value.email ? 'warning' : 'info',
    description: '填写常用邮箱，用于后续账号验证和消息通知。',
    detail: `当前邮箱：${profile.value.email || '未绑定'}`
  },
  {
    title: '地址管理',
    path: '/app/settings/addresses',
    badge: profile.value.addresses.length ? `${profile.value.addresses.length} 个地址` : '未添加',
    badgeType: profile.value.addresses.length ? 'success' : 'info',
    description: '管理收货地址，并设置结算时优先使用的默认地址。',
    detail: profile.value.addresses.find((item) => item.isDefault)?.detail || '暂无默认地址'
  },
  {
    title: '偏好设置',
    path: '/app/settings/preferences',
    badge: '可修改',
    badgeType: 'success',
    description: '设置商品默认排序、默认交付方式、默认支付方式和提醒开关。',
    detail: `默认排序：${marketStore.userPreference.defaultSortType}`
  },
  {
    title: '账号安全',
    badge: '正常',
    badgeType: 'success',
    description: '登录账号信息和身份认证状态。',
    detail: `登录账号：${profile.value.loginId || authStore.currentUser?.loginId || '未设置'} · 认证：${profile.value.verification.label}`
  },
  {
    title: '通知设置',
    path: '/app/settings/preferences',
    badge: '可修改',
    badgeType: 'success',
    description: '控制订单提醒、评价提醒等通知的开关。',
    detail: `订单提醒：${marketStore.userPreference.notificationPreference.orderReminder ? '开启' : '关闭'} · 评价提醒：${marketStore.userPreference.notificationPreference.reviewReminder ? '开启' : '关闭'}`
  },
  {
    title: 'Privacy Rights',
    path: '/app/settings/privacy',
    badge: 'Available',
    badgeType: 'success',
    description: 'Review consent history, generate a personal data export, and request soft account closure.',
    detail: 'Account closure anonymizes profile fields and retains transaction history.'
  }
])

async function loadSettingsData() {
  const results = await Promise.allSettled([
    marketStore.loadProfile(authStore.currentUser),
    marketStore.loadUserAddresses(),
    marketStore.loadUserPreference(),
    marketStore.loadMyShop()
  ])

  if (results.some((item) => item.status === 'rejected')) {
    ElMessage.warning('设置中心部分信息暂未加载成功')
  }
}

function logout() {
  authStore.logout()
  router.push('/login')
}

onMounted(loadSettingsData)
</script>

<template>
  <div class="shell-container page-stack" v-loading="marketStore.loadingProfile || marketStore.loadingPreference">
    <section class="shell-card settings-hero">
      <div>
        <span class="eyebrow">设置中心</span>
        <h1>设置中心</h1>
        <p>
          管理你的偏好、地址、账号安全和通知设置。
        </p>
      </div>
      <div class="settings-hero__actions">
        <el-button plain @click="$router.push('/app/me')">返回个人主页</el-button>
        <el-button type="primary" @click="$router.push('/app/settings/preferences')">进入偏好设置</el-button>
      </div>
    </section>

    <el-alert
      v-if="marketStore.profileError"
      type="warning"
      show-icon
      :closable="false"
      :title="marketStore.profileError"
      description="部分个人信息暂时无法加载，请稍后刷新重试。"
    />

    <el-alert
      v-if="marketStore.preferenceError"
      type="info"
      show-icon
      :closable="false"
      :title="marketStore.preferenceError"
      description="偏好设置暂时无法加载，你仍可浏览其他设置项。"
    />

    <section class="settings-grid" aria-label="设置分区">
      <article
        v-for="item in settingsSections"
        :key="item.title"
        class="shell-card settings-card"
      >
        <div class="settings-card__header">
          <strong>{{ item.title }}</strong>
          <el-tag :type="item.badgeType" effect="plain">{{ item.badge }}</el-tag>
        </div>
        <p>{{ item.description }}</p>
        <span>{{ item.detail }}</span>
        <el-button
          v-if="item.path"
          class="settings-card__action"
          plain
          @click="$router.push(item.path)"
        >
          查看入口
        </el-button>
      </article>
    </section>

    <section class="settings-detail-grid">
      <article class="shell-card settings-detail-card">
        <strong>当前地址摘要</strong>
        <p>
          {{
            profile.addresses[0]
              ? `${profile.addresses[0].type || '收货地址'} · ${profile.addresses[0].detail}`
              : '当前没有默认地址摘要'
          }}
        </p>
        <span>
          {{
            profile.addresses[0]
              ? `${profile.addresses[0].contactName || '未命名'} · ${profile.addresses[0].phone || '暂无电话'}`
              : '添加地址后，结算时会自动带入默认地址。'
          }}
        </span>
      </article>

      <article class="shell-card settings-detail-card">
        <strong>店铺信息</strong>
        <p>
          {{
            ownedShop?.id
              ? `你的店铺「${ownedShop.name}」正在运营中，可以从个人主页进入店铺管理。`
              : '你还没有开店，完成学生认证后即可创建自己的店铺。'
          }}
        </p>
        <span>{{ ownedShop?.id ? '店铺对外展示内容在店铺主页管理' : '开店后可以发布商品、管理订单' }}</span>
      </article>
    </section>

    <section class="shell-card logout-panel">
      <div>
        <strong>退出登录</strong>
        <p>退出当前账号后需要重新登录才能继续使用。</p>
      </div>
      <el-button type="danger" plain @click="logout">退出登录</el-button>
    </section>
  </div>
</template>

<style scoped>
.settings-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
}

.settings-hero h1 {
  margin: 10px 0 12px;
  font-size: clamp(30px, 3vw, 46px);
  line-height: 1.15;
}

.settings-hero__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.settings-grid,
.settings-detail-grid {
  display: grid;
  gap: 16px;
}

.settings-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.settings-detail-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.settings-card,
.settings-detail-card,
.logout-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.settings-card__header,
.logout-panel {
  justify-content: space-between;
}

.settings-card__header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.settings-card strong,
.settings-detail-card strong,
.logout-panel strong {
  font-size: 18px;
}

.settings-card p,
.settings-card span,
.settings-detail-card p,
.settings-detail-card span,
.logout-panel p {
  margin: 0;
  color: var(--cm-text-secondary);
  line-height: 1.6;
}

.settings-card__action {
  align-self: flex-start;
}

.logout-panel {
  flex-direction: row;
  align-items: center;
}

@media (max-width: 860px) {
  .settings-hero,
  .logout-panel {
    flex-direction: column;
    align-items: stretch;
  }

  .settings-grid,
  .settings-detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
