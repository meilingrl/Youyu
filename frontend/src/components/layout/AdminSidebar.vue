<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { adminNavigation } from '@/constants/navigation'
import { hasAnyAdminPermission, permissionsForAdminPath } from '@/utils/admin-permissions'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()
const visibleNavigation = computed(() =>
  adminNavigation.filter((item) =>
    hasAnyAdminPermission(authStore.currentRole, permissionsForAdminPath(item.path))
  )
)
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
        {{ item.label }}
      </el-menu-item>
    </el-menu>
  </aside>
</template>
