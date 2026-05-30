<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import { getAdminDashboard } from '@/api/modules/admin'
import { resolveErrorMessage } from '@/utils/error-utils'
import { adminLabel } from '@/utils/admin-display-labels'

const loading = ref(false)
const summary = ref({})
const queueMetrics = ref([])
const governanceSignals = ref([])
const statusBreakdowns = ref({ orders: [], mediation: [] })
const unavailableMetrics = ref([])

const heroStats = computed(() => [
  { label: '用户', value: summary.value.userCount || 0, hint: '账号总量' },
  { label: '商品', value: summary.value.productCount || 0, hint: '商品池' },
  { label: '店铺', value: summary.value.shopCount || 0, hint: '入驻主体' },
  { label: '订单', value: summary.value.orderCount || 0, hint: '交易记录' }
])

const visibleQueueMetrics = computed(() =>
  queueMetrics.value.filter((metric) => metric.available && Number(metric.value || 0) > 0)
)
const visibleGovernanceSignals = computed(() =>
  governanceSignals.value.filter((metric) => metric.available && Number(metric.value || 0) > 0)
)
const visibleOrderBreakdowns = computed(() =>
  (statusBreakdowns.value.orders || []).filter((item) => Number(item.value || 0) > 0)
)
const visibleMediationBreakdowns = computed(() =>
  (statusBreakdowns.value.mediation || []).filter((item) => Number(item.value || 0) > 0)
)

function metricTarget(metric) {
  if (!metric?.target?.path) {
    return {}
  }
  return {
    to: {
      path: metric.target.path,
      query: metric.target.query || {}
    }
  }
}

function metricComponent(metric) {
  return metric?.available && metric?.target?.path ? 'router-link' : 'div'
}

function tagType(severity) {
  return {
    danger: 'danger',
    warning: 'warning',
    info: 'info',
    muted: 'info'
  }[severity] || 'info'
}

async function loadDashboard() {
  loading.value = true

  try {
    const response = await getAdminDashboard()
    const data = response.data || {}
    summary.value = data.summary || {}
    queueMetrics.value = data.queueMetrics || []
    governanceSignals.value = data.governanceSignals || []
    statusBreakdowns.value = data.statusBreakdowns || { orders: [], mediation: [] }
    unavailableMetrics.value = data.unavailableMetrics || []
  } catch (err) {
    ElMessage.error(resolveErrorMessage(err))
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <div class="page-stack admin-dashboard" v-loading="loading">
    <section class="shell-hero shell-hero--compact dashboard-hero">
      <div>
        <span class="eyebrow">治理工作台</span>
        <h1>治理总览</h1>
        <p>集中查看待办队列、治理信号和调解进度，帮助管理员优先处理有明确归属的工作。</p>
      </div>

      <div class="dashboard-hero__stats" aria-label="后台核心数据">
        <div v-for="item in heroStats" :key="item.label" class="dashboard-hero__stat">
          <span class="dashboard-hero__stat-label">{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
        </div>
      </div>
    </section>

    <section class="dashboard-section">
      <div class="dashboard-section__header">
        <div>
          <h2>待处理工作队列</h2>
          <p>数据按业务队列汇总，并指向对应的处理页面。</p>
        </div>
      </div>

      <div v-if="visibleQueueMetrics.length" class="observability-grid">
        <component
          :is="metricComponent(metric)"
          v-for="metric in visibleQueueMetrics"
          :key="metric.id"
          v-bind="metricTarget(metric)"
          class="queue-card"
          :class="[`queue-card--${metric.severity || 'info'}`, { 'queue-card--link': metric.available }]"
        >
          <div class="queue-card__top">
            <span>{{ metric.label }}</span>
            <el-tag :type="tagType(metric.severity)" effect="plain">待处理</el-tag>
          </div>
          <strong class="queue-card__value">{{ metric.value }}</strong>
          <p>{{ metric.description }}</p>
        </component>
      </div>
      <div v-else class="admin-empty-line">当前没有待处理队列。</div>
    </section>

    <section class="dashboard-section">
      <div class="dashboard-section__header">
        <div>
          <h2>治理信号</h2>
          <p>用于发现需要复核或持续关注的后台状态，辅助各业务列表处理。</p>
        </div>
      </div>

      <div v-if="visibleGovernanceSignals.length" class="signal-grid">
        <component
          :is="metricComponent(metric)"
          v-for="metric in visibleGovernanceSignals"
          :key="metric.id"
          v-bind="metricTarget(metric)"
          class="signal-row"
          :class="{ 'signal-row--link': metric.available }"
        >
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
          <small>{{ metric.description }}</small>
        </component>
      </div>
      <div v-else class="admin-empty-line">当前没有需要额外关注的治理信号。</div>
    </section>

    <section class="dashboard-section dashboard-section--split">
      <div class="breakdown-panel">
        <div class="dashboard-section__header">
          <div>
            <h2>订单状态</h2>
            <p>订单履约页负责处理发货、线下确认和退款完成动作。</p>
          </div>
        </div>

        <div class="breakdown-list">
          <router-link
            v-for="item in visibleOrderBreakdowns"
            :key="item.status"
            :to="item.target?.path || '/admin/orders'"
            class="breakdown-row"
          >
            <span>{{ adminLabel(item.status) }}</span>
            <strong>{{ item.value }}</strong>
          </router-link>
          <div v-if="!visibleOrderBreakdowns.length" class="admin-empty-line">暂无待跟进订单状态。</div>
        </div>
      </div>

      <div class="breakdown-panel">
        <div class="dashboard-section__header">
          <div>
            <h2>调解进度</h2>
            <p>正式平台争议处理来自已升级的订单举报。</p>
          </div>
        </div>

        <div class="breakdown-list">
          <router-link
            v-for="item in visibleMediationBreakdowns"
            :key="item.status"
            :to="item.target?.path || '/admin/mediation'"
            class="breakdown-row"
          >
            <span>{{ adminLabel(item.status) }}</span>
            <strong>{{ item.value }}</strong>
          </router-link>
          <div v-if="!visibleMediationBreakdowns.length" class="admin-empty-line">暂无进行中的调解案件。</div>
        </div>
      </div>
    </section>

    <section v-if="unavailableMetrics.length" class="dashboard-section">
      <div class="dashboard-section__header">
        <div>
          <h2>待完善指标</h2>
          <p>指标将在数据口径稳定后展示，避免造成误判。</p>
        </div>
      </div>

      <div class="unavailable-list">
        <div v-for="metric in unavailableMetrics" :key="metric.id" class="unavailable-row">
          <div>
            <strong>{{ metric.label }}</strong>
            <span>{{ metric.description }}</span>
          </div>
          <el-tag type="info" effect="plain">待完善</el-tag>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.admin-dashboard {
  gap: 24px;
}

.dashboard-hero {
  align-items: center;
}

.dashboard-hero__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(72px, 1fr));
  gap: 10px;
  min-width: 360px;
}

.dashboard-hero__stat,
.breakdown-panel,
.unavailable-row {
  border: 1px solid var(--cm-border);
  background: rgba(255, 255, 255, 0.7);
}

.dashboard-hero__stat {
  display: grid;
  gap: 6px;
  min-height: 96px;
  align-content: center;
  justify-items: start;
  padding: 16px;
  border-radius: 16px;
  box-shadow: var(--cm-shadow-soft);
}

.dashboard-hero__stat strong {
  font-size: 30px;
  line-height: 1;
}

.dashboard-hero__stat-label {
  font-size: 13px;
  font-weight: 700;
}

.dashboard-hero__stat small,
.dashboard-section__header p,
.queue-card p,
.signal-row small,
.unavailable-row span {
  color: var(--cm-text-secondary);
}

.admin-empty-line {
  min-height: 56px;
  display: grid;
  place-items: center;
  border: 1px dashed var(--cm-border-strong);
  border-radius: 16px;
  color: var(--cm-text-tertiary);
  background: rgba(255, 255, 255, 0.52);
}

.dashboard-section {
  display: grid;
  gap: 16px;
}

.dashboard-section__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.dashboard-section__header h2 {
  font-size: 18px;
  line-height: 1.35;
}

.dashboard-section__header p {
  margin-top: 4px;
  font-size: 13px;
}

.observability-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 14px;
}

.queue-card {
  display: grid;
  gap: 12px;
  min-height: 184px;
  padding: 20px;
  border: 1px solid var(--cm-border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: var(--cm-shadow-soft);
  transition:
    transform var(--cm-transition),
    border-color var(--cm-transition),
    box-shadow var(--cm-transition);
}

.queue-card--link:hover,
.signal-row--link:hover,
.breakdown-row:hover {
  transform: translateY(-2px);
  border-color: rgba(var(--cm-primary-rgb), 0.24);
  box-shadow: var(--cm-shadow-md);
}

.queue-card--danger {
  border-left: 4px solid #d94a38;
}

.queue-card--warning {
  border-left: 4px solid var(--cm-accent);
}

.queue-card--info {
  border-left: 4px solid var(--cm-muted-green);
}

.queue-card__top,
.signal-row,
.breakdown-row,
.unavailable-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.queue-card__top span,
.signal-row span,
.breakdown-row span,
.unavailable-row strong {
  font-weight: 700;
}

.queue-card__value {
  font-size: 34px;
  line-height: 1;
}

.signal-grid {
  display: grid;
  gap: 10px;
}

.signal-row,
.breakdown-row {
  min-height: 64px;
  padding: 14px 16px;
  border: 1px solid var(--cm-border);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.68);
  transition:
    transform var(--cm-transition),
    border-color var(--cm-transition),
    box-shadow var(--cm-transition);
}

.signal-row small {
  flex: 1;
}

.signal-row strong,
.breakdown-row strong {
  font-size: 22px;
}

.dashboard-section--split {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.breakdown-panel {
  display: grid;
  gap: 14px;
  padding: 20px;
  border-radius: 18px;
}

.breakdown-list,
.unavailable-list {
  display: grid;
  gap: 10px;
}

.unavailable-row {
  min-height: 72px;
  padding: 14px 16px;
  border-radius: 16px;
}

.unavailable-row div {
  display: grid;
  gap: 4px;
}

@media (max-width: 1100px) {
  .dashboard-hero__stats {
    min-width: 0;
    width: 100%;
  }

  .dashboard-section--split {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .dashboard-hero__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .signal-row,
  .unavailable-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
