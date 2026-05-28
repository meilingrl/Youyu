<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function handleLogout() {
  authStore.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <header class="admin-topbar shell-card">
    <div>
      <div class="admin-topbar__title">治理工作台</div>
      <div class="admin-topbar__subtitle">集中处理身份、内容、交易与风险队列</div>
    </div>

    <div class="admin-topbar__actions">
      <el-button plain @click="$router.push('/app/home')">返回前台</el-button>
      <el-button type="danger" @click="handleLogout">退出登录</el-button>
    </div>
  </header>
</template>
