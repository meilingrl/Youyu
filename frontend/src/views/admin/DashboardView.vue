<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import { getAdminDashboard } from '@/api/modules/admin'
import { resolveErrorMessage } from '@/utils/error-utils'

const loading = ref(false)
const cards = ref([])
const shortcuts = ref([])
const todo = ref({})
const cardCopy = [
  { label: '用户与身份', secondaryLabel: '禁用账号' },
  { label: '学生认证队列', secondaryLabel: '风险标记' },
  { label: '资料审核队列', secondaryLabel: '已驳回资料' },
  { label: '举报处置队列', secondaryLabel: '处理中举报' }
]
const shortcutCopyByPath = {
  '/admin/users': {
    label: '用户与身份',
    description: '查看账号状态、身份信息和治理上下文'
  },
  '/admin/verifications': {
    label: '学生认证队列',
    description: '处理待审核认证、风险标记和驳回原因'
  },
  '/admin/review-tasks': {
    label: '资料审核队列',
    description: '审核资料类商品并决定是否允许上架'
  },
  '/admin/reports': {
    label: '举报处置',
    description: '记录处置结论并推进举报状态流转'
  }
}

const workbenchCards = computed(() =>
  cards.value.map((card, index) => ({
    ...card,
    ...(cardCopy[index] || {})
  }))
)

const workbenchShortcuts = computed(() =>
  shortcuts.value.map((shortcut) => ({
    ...shortcut,
    ...(shortcutCopyByPath[shortcut.path] || {})
  }))
)

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
        <span class="eyebrow">Governance Workbench</span>
        <h1>治理总览</h1>
        <p>集中查看平台治理队列、待办风险和核心工作入口，作为管理员进入后的默认工作台。</p>
      </div>
    </section>

    <div class="metric-grid metric-grid--wide">
      <div v-for="card in workbenchCards" :key="card.label" class="metric-card">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <small>{{ card.secondaryLabel }}：{{ card.secondaryValue }}</small>
      </div>
    </div>

    <div class="shell-card">
      <div class="section-heading">
        <h2>治理入口</h2>
      </div>

      <div class="metric-grid metric-grid--wide">
        <router-link
          v-for="shortcut in workbenchShortcuts"
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
        <h2>待处理队列</h2>
      </div>

      <div class="todo-row">
        <el-tag type="warning">学生认证 {{ todo.pendingVerificationCount || 0 }}</el-tag>
        <el-tag type="danger">资料审核 {{ todo.pendingReviewTaskCount || 0 }}</el-tag>
        <el-tag type="info">举报处置 {{ todo.pendingReportCount || 0 }}</el-tag>
        <el-tag>店铺准入 {{ todo.pendingShopCount || 0 }}</el-tag>
      </div>
    </div>
  </div>
</template>
