<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ChatDotRound,
  DataBoard,
  DocumentChecked,
  Goods,
  Management,
  Search,
  Service,
  Shop,
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
const adminName = computed(() =>
  authStore.currentUser?.nickname || authStore.currentUser?.loginId || '管理员'
)

const iconMap = {
  '/admin/dashboard': DataBoard,
  '/admin/users': User,
  '/admin/verifications': DocumentChecked,
  '/admin/products': Goods,
  '/admin/review-tasks': Tickets,
  '/admin/shops': Shop,
  '/admin/orders': ShoppingCart,
  '/admin/reports': Warning,
  '/admin/mediation': Management,
  '/admin/hot-search': Search,
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
        <component :is="iconFor(item.path)" class="admin-sidebar__nav-icon" />
        <span>{{ item.label }}</span>
      </el-menu-item>
    </el-menu>

    <div class="admin-sidebar__footer">
      <span class="admin-sidebar__account">{{ adminName }}</span>
      <button type="button" class="admin-sidebar__logout" aria-label="退出登录" title="退出登录" @click="handleLogout">
        <SwitchButton class="admin-sidebar__logout-icon" />
      </button>
    </div>
  </aside>
</template>
