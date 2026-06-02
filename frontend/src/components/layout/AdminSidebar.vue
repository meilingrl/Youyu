<script setup>
import { computed, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ChatDotRound,
  Bell,
  DataBoard,
  DocumentChecked,
  Goods,
  Search,
  Service,
  ShoppingCart,
  SwitchButton,
  Tickets,
  User,
  Warning
} from '@element-plus/icons-vue'
import { adminNavigation } from '@/constants/navigation'
import { hasAnyAdminPermission, permissionsForAdminPath } from '@/utils/admin-permissions'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const visibleNavigation = computed(() =>
  adminNavigation.filter((item) =>
    hasAnyAdminPermission(authStore.currentRole, permissionsForAdminPath(item.path))
  )
)

const adminName = computed(
  () => authStore.currentUser?.nickname || authStore.currentUser?.loginId || '平台管理员'
)

const OutlineShopIcon = {
  name: 'OutlineShopIcon',
  render() {
    return h(
      'svg',
      {
        viewBox: '0 0 24 24',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '1.8',
        'stroke-linecap': 'round',
        'stroke-linejoin': 'round',
        'aria-hidden': 'true'
      },
      [
        h('path', { d: 'M4 10.5 6 5h12l2 5.5' }),
        h('path', {
          d: 'M3.5 10.5h17a1 1 0 0 1 1 1v1.25a3.25 3.25 0 0 1-5.75 2.06A3.25 3.25 0 0 1 12 14.75a3.25 3.25 0 0 1-3.75.06A3.25 3.25 0 0 1 2.5 12.75V11.5a1 1 0 0 1 1-1Z'
        }),
        h('path', { d: 'M5.5 14.75V19a1 1 0 0 0 1 1h11a1 1 0 0 0 1-1v-4.25' }),
        h('path', { d: 'M9 20v-3.5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1V20' })
      ]
    )
  }
}

const OutlineMediationIcon = {
  name: 'OutlineMediationIcon',
  render() {
    return h(
      'svg',
      {
        viewBox: '0 0 24 24',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '1.8',
        'stroke-linecap': 'round',
        'stroke-linejoin': 'round',
        'aria-hidden': 'true'
      },
      [
        h('path', { d: 'M12 3.5v13' }),
        h('path', { d: 'M4.5 6.5h15' }),
        h('path', { d: 'M7.5 6.5 4.5 11.25h6Z' }),
        h('path', { d: 'M16.5 6.5 13.5 11.25h6Z' }),
        h('path', { d: 'M4.75 11.25a2.75 2.25 0 0 0 5.5 0Z' }),
        h('path', { d: 'M13.75 11.25a2.75 2.25 0 0 0 5.5 0Z' }),
        h('path', { d: 'M8 20h8' })
      ]
    )
  }
}

const iconMap = {
  '/admin/dashboard': DataBoard,
  '/admin/users': User,
  '/admin/verifications': DocumentChecked,
  '/admin/products': Goods,
  '/admin/review-tasks': Tickets,
  '/admin/shops': OutlineShopIcon,
  '/admin/orders': ShoppingCart,
  '/admin/reports': Warning,
  '/admin/mediation': OutlineMediationIcon,
  '/admin/hot-search': Search,
  '/admin/notifications': Bell,
  '/admin/support': Service
}

function iconFor(path) {
  return iconMap[path] || ChatDotRound
}

function handleLogout() {
  authStore.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <aside class="admin-sidebar">
    <div class="admin-sidebar__brand">
      <div class="admin-sidebar__logo">CM</div>
      <div>
        <div class="admin-sidebar__title">Youyu</div>
        <div class="admin-sidebar__subtitle">平台治理工作台</div>
      </div>
    </div>

    <el-menu :default-active="route.meta?.navKey" class="admin-sidebar__menu" router>
      <el-menu-item v-for="item in visibleNavigation" :key="item.path" :index="item.path">
        <component
          :is="iconFor(item.path)"
          :class="[
            'admin-sidebar__nav-icon',
            item.path === '/admin/mediation' ? 'admin-sidebar__nav-icon--mediation' : ''
          ]"
        />
        <span>{{ item.label }}</span>
      </el-menu-item>
    </el-menu>

    <div class="admin-sidebar__footer">
      <span class="admin-sidebar__account">{{ adminName }}</span>
      <button
        type="button"
        class="admin-sidebar__logout"
        aria-label="退出登录"
        title="退出登录"
        @click="handleLogout"
      >
        <SwitchButton class="admin-sidebar__logout-icon" />
      </button>
    </div>
  </aside>
</template>
