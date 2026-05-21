<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import { getAdminDashboard } from '@/api/modules/admin'
import { resolveErrorMessage } from '@/utils/error-utils'

const loading = ref(false)
const cards = ref([])
const shortcuts = ref([])
const todo = ref({})

async function loadDashboard() {
  loading.value = true

  try {
    const response = await getAdminDashboard()
    cards.value = response.data.cards || []
    shortcuts.value = response.data.shortcuts || []
    todo.value = response.data.todo || {}
  } catch (err) {
    ElMessage.error(resolveErrorMessage(err))
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <div class="page-stack" v-loading="loading">
    <section class="shell-hero shell-hero--compact">
      <div>
        <span class="eyebrow">Admin Dashboard</span>
        <h1>后台首页</h1>
        <p>首页先只承载核心统计卡片、待办提示和后台主链路入口，不扩展复杂看板。</p>
      </div>
    </section>

    <div class="metric-grid metric-grid--wide">
      <div v-for="card in cards" :key="card.label" class="metric-card">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <small>{{ card.secondaryLabel }}：{{ card.secondaryValue }}</small>
      </div>
    </div>

    <div class="shell-card">
      <div class="section-heading">
        <h2>快捷入口</h2>
      </div>

      <div class="metric-grid metric-grid--wide">
        <router-link
          v-for="shortcut in shortcuts"
          :key="shortcut.path"
          :to="shortcut.path"
          class="metric-card metric-card--link"
        >
          <strong>{{ shortcut.label }}</strong>
          <span>{{ shortcut.description }}</span>
        </router-link>
      </div>
    </div>

    <div class="shell-card">
      <div class="section-heading">
        <h2>当前待办</h2>
      </div>

      <div class="todo-row">
        <el-tag type="warning">待审认证 {{ todo.pendingVerificationCount || 0 }}</el-tag>
        <el-tag type="danger">待审资料 {{ todo.pendingReviewTaskCount || 0 }}</el-tag>
        <el-tag type="info">待处理举报 {{ todo.pendingReportCount || 0 }}</el-tag>
        <el-tag>待审店铺 {{ todo.pendingShopCount || 0 }}</el-tag>
      </div>
    </div>
  </div>
</template>
