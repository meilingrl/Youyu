<script setup>
import { computed, onMounted, ref } from 'vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import {
  getAdminDashboard,
  getAdminProducts,
  getAdminReports,
  getAdminReviewTasks,
  getAdminSearchLogs,
  getAdminShops,
  getAdminUsers
} from '@/api/modules/admin'
import { getAdminOrderList } from '@/api/modules/order'
import { resolveErrorMessage } from '@/utils/error-utils'

const PAGE_SIZE = 5

const loading = ref(false)
const error = ref('')
const dashboard = ref({})
const reports = ref(emptyPage())
const orders = ref([])
const users = ref(emptyPage())
const shops = ref(emptyPage())
const products = ref(emptyPage())
const reviewTasks = ref(emptyPage())
const searchLogs = ref(emptyPage())

function emptyPage() {
  return {
    items: [],
    total: 0,
    page: 1,
    pageSize: PAGE_SIZE
  }
}

function normalizePage(payload) {
  if (Array.isArray(payload)) {
    return {
      ...emptyPage(),
      items: payload,
      total: payload.length
    }
  }

  const items = Array.isArray(payload?.items) ? payload.items : []
  return {
    ...emptyPage(),
    ...payload,
    items,
    total: Number.isFinite(Number(payload?.total)) ? Number(payload.total) : items.length
  }
}

function sampleCount(list) {
  return Array.isArray(list) ? list.length : 0
}

function formatCount(value, fallback = '0') {
  const number = Number(value)
  return Number.isFinite(number) ? String(number) : fallback
}

function describeReport(item) {
  const label = item.targetLabel || `#${item.targetId || item.id}`
  return `${label} / ${item.reasonType || '未标注原因'}`
}

function describeOrder(item) {
  return item.orderNo || item.productTitle || `订单 #${item.id}`
}

function describeGovernance(item, fallback) {
  return item.nickname || item.username || item.name || item.shopName || item.title || item.productTitle || fallback
}

function describeSearchLog(item) {
  return item.keyword || item.normalizedKeyword || `搜索记录 #${item.id}`
}

const dashboardCards = computed(() => (Array.isArray(dashboard.value.cards) ? dashboard.value.cards : []))
const dashboardTodo = computed(() => dashboard.value.todo || {})

const liveLanes = computed(() => [
  {
    key: 'reports',
    title: '举报治理协同',
    eyebrow: 'Report triage',
    owner: 'Report/AdminController',
    statusLabel: '可复用',
    tagType: 'success',
    metricLabel: '待处理举报',
    metricValue: formatCount(reports.value.total),
    metricHint: '来自 GET /api/admin/reports?status=pending 的分页总数。',
    description: '展示待处理举报上下文。处理、驳回和结论记录仍在举报处理页完成。',
    routes: [{ label: '进入举报处理', path: '/admin/reports' }],
    previewTitle: '待处理样本',
    previewItems: reports.value.items.map((item) => ({
      id: item.id,
      title: describeReport(item),
      meta: `${item.targetType || 'target'} · ${item.status || 'pending'} · ${item.submittedAt || '未记录时间'}`
    })),
    emptyTitle: '暂无待处理举报',
    emptyDescription: '当前分页没有 pending 举报记录。'
  },
  {
    key: 'orders',
    title: '订单与退款协助',
    eyebrow: 'Order/refund context',
    owner: 'AdminOrderController',
    statusLabel: '样本上下文',
    tagType: 'success',
    metricLabel: '订单样本',
    metricValue: formatCount(sampleCount(orders.value)),
    metricHint: 'GET /api/admin/orders 当前返回列表，不暴露支持专用总量。',
    description: '展示订单/退款上下文入口。发货、线下确认和退款完成仍归订单管理页。',
    routes: [{ label: '进入订单管理', path: '/admin/orders' }],
    previewTitle: '最近订单样本',
    previewItems: orders.value.slice(0, PAGE_SIZE).map((item) => ({
      id: item.id || item.orderNo,
      title: describeOrder(item),
      meta: `${item.orderStatus || '未知订单状态'} · ${item.paymentStatus || '未知支付状态'} · ${item.fulfillmentType || '未知履约'}`
    })),
    emptyTitle: '暂无订单样本',
    emptyDescription: '当前订单列表没有返回可展示记录。'
  },
  {
    key: 'governance',
    title: '用户/店铺/商品治理',
    eyebrow: 'Governance context',
    owner: 'AdminController',
    statusLabel: '可复用',
    tagType: 'success',
    metricLabel: '治理对象',
    metricValue: formatCount(users.value.total + shops.value.total + products.value.total),
    metricHint: '合计用户、店铺、商品分页总量；资料审核单独展示。',
    description: '汇总治理对象样本，并把状态变更留在各自 owner 页面。',
    routes: [
      { label: '用户', path: '/admin/users' },
      { label: '店铺', path: '/admin/shops' },
      { label: '商品', path: '/admin/products' },
      { label: '资料审核', path: '/admin/review-tasks' }
    ],
    previewTitle: '治理样本',
    previewItems: [
      ...users.value.items.slice(0, 2).map((item) => ({
        id: `user-${item.id || item.userId}`,
        title: describeGovernance(item, '用户记录'),
        meta: `用户 · ${item.status || '未知状态'} · ${item.verificationStatus || '认证未知'}`
      })),
      ...shops.value.items.slice(0, 2).map((item) => ({
        id: `shop-${item.id || item.shopId}`,
        title: describeGovernance(item, '店铺记录'),
        meta: `店铺 · ${item.status || '未知状态'} · ${item.reviewStatus || '审核未知'}`
      })),
      ...products.value.items.slice(0, 2).map((item) => ({
        id: `product-${item.id || item.productId}`,
        title: describeGovernance(item, '商品记录'),
        meta: `商品 · ${item.status || '未知状态'} · ${item.reviewStatus || '审核未知'}`
      })),
      ...reviewTasks.value.items.slice(0, 2).map((item) => ({
        id: `review-${item.id || item.reviewTaskId}`,
        title: describeGovernance(item, '资料审核记录'),
        meta: `资料审核 · ${item.reviewStatus || '未知状态'}`
      }))
    ].slice(0, PAGE_SIZE),
    emptyTitle: '暂无治理样本',
    emptyDescription: '用户、店铺、商品和资料审核接口当前没有返回样本。'
  },
  {
    key: 'search',
    title: '搜索治理信号',
    eyebrow: 'Search/risk signal',
    owner: 'Search governance',
    statusLabel: '部分可用',
    tagType: 'warning',
    metricLabel: '搜索日志',
    metricValue: formatCount(searchLogs.value.total),
    metricHint: '仅来自 GET /api/admin/search/logs；不是异常消息检测。',
    description: '展示搜索日志上下文和热搜治理入口。异常聊天消息检测当前不存在。',
    routes: [{ label: '进入热搜治理', path: '/admin/hot-search' }],
    previewTitle: '搜索日志样本',
    previewItems: searchLogs.value.items.map((item) => ({
      id: item.id,
      title: describeSearchLog(item),
      meta: `结果 ${formatCount(item.resultCount)} · ${item.createdAt || '未记录时间'}`
    })),
    emptyTitle: '暂无搜索日志',
    emptyDescription: '当前分页没有返回搜索日志样本。'
  }
])

const blockedLanes = [
  {
    key: 'admin-chat',
    title: '管理员聊天可见性',
    eyebrow: 'Admin chat',
    owner: 'ChatController 当前为 USER 流程',
    statusLabel: '缺失',
    tagType: 'info',
    description: '不读取 /api/chat/**，不展示跨用户会话，不发送管理员消息。',
    gaps: ['无 admin conversation lookup', '无管理员参与者模型', '无三方客服会话']
  },
  {
    key: 'notification-group-risk',
    title: '通知、群治理与异常消息',
    eyebrow: 'Reserved lanes',
    owner: '未定义或非 support owner',
    statusLabel: '缺失',
    tagType: 'info',
    description: '通知仍是用户投递基础设施；群治理和异常消息检测没有当前 owner 或 API。',
    gaps: ['不调用 /api/notifications/**', '无 group governance endpoint', '无 abnormal message detection endpoint']
  }
]

const summaryMetrics = computed(() => [
  {
    label: '待处理举报',
    value: formatCount(reports.value.total),
    helper: '来自举报 owner 分页总数'
  },
  {
    label: '订单样本',
    value: formatCount(sampleCount(orders.value)),
    helper: '订单接口当前无分页总数'
  },
  {
    label: '资料审核',
    value: formatCount(reviewTasks.value.total),
    helper: '来自 review-task 分页总数'
  },
  {
    label: '搜索日志',
    value: formatCount(searchLogs.value.total),
    helper: '搜索日志不是消息风险检测'
  }
])

async function loadSupportContext() {
  loading.value = true
  error.value = ''

  try {
    const [
      dashboardResponse,
      reportResponse,
      orderResponse,
      userResponse,
      shopResponse,
      productResponse,
      reviewTaskResponse,
      searchLogResponse
    ] = await Promise.all([
      getAdminDashboard(),
      getAdminReports({ status: 'pending', page: 1, pageSize: PAGE_SIZE }),
      getAdminOrderList(),
      getAdminUsers({ page: 1, pageSize: PAGE_SIZE }),
      getAdminShops({ page: 1, pageSize: PAGE_SIZE }),
      getAdminProducts({ page: 1, pageSize: PAGE_SIZE }),
      getAdminReviewTasks({ page: 1, pageSize: PAGE_SIZE }),
      getAdminSearchLogs({ page: 1, pageSize: PAGE_SIZE })
    ])

    dashboard.value = dashboardResponse.data || {}
    reports.value = normalizePage(reportResponse.data)
    orders.value = Array.isArray(orderResponse.data) ? orderResponse.data : []
    users.value = normalizePage(userResponse.data)
    shops.value = normalizePage(shopResponse.data)
    products.value = normalizePage(productResponse.data)
    reviewTasks.value = normalizePage(reviewTaskResponse.data)
    searchLogs.value = normalizePage(searchLogResponse.data)
  } catch (err) {
    error.value = resolveErrorMessage(err)
  } finally {
    loading.value = false
  }
}

onMounted(loadSupportContext)
</script>

<template>
  <div class="page-stack">
    <section class="shell-hero shell-hero--compact admin-support-hero">
      <div>
        <span class="eyebrow">Support Console</span>
        <h1>客服支持上下文</h1>
        <p>
          这个页面只汇总现有后台治理上下文，帮助运营人员判断应该进入哪个 owner 页面处理。
          它不创建客服工单、不处理调解案件、不读取用户聊天或通知接口。
        </p>
      </div>
      <div class="shell-inline-actions">
        <el-tag effect="plain" type="success">Frontend-only</el-tag>
        <el-tag effect="plain" type="info">No support API</el-tag>
      </div>
    </section>

    <ErrorBlock v-if="error" :message="error" @retry="loadSupportContext" />

    <template v-else>
      <SkeletonCard v-if="loading" :count="4" />

      <template v-else>
        <section class="metric-grid metric-grid--wide">
          <article v-for="metric in summaryMetrics" :key="metric.label" class="metric-card admin-support-metric">
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
            <small>{{ metric.helper }}</small>
          </article>
        </section>

        <section v-if="dashboardCards.length || Object.keys(dashboardTodo).length" class="shell-card admin-support-overview">
          <div class="section-heading">
            <h2>后台总览参考</h2>
          </div>
          <div class="admin-support-overview__grid">
            <article v-for="card in dashboardCards" :key="card.label" class="admin-support-overview__item">
              <span>{{ card.label }}</span>
              <strong>{{ card.value }}</strong>
              <small v-if="card.secondaryLabel">{{ card.secondaryLabel }}: {{ card.secondaryValue }}</small>
            </article>
            <article class="admin-support-overview__item">
              <span>Dashboard todo</span>
              <strong>{{ formatCount(dashboardTodo.pendingReportCount) }}</strong>
              <small>dashboard 内的 pendingReportCount，仅作为参考。</small>
            </article>
          </div>
        </section>

        <section class="admin-support-grid">
          <article v-for="lane in liveLanes" :key="lane.key" class="shell-card admin-support-lane">
            <header class="admin-support-lane__header">
              <div>
                <span class="admin-support-lane__eyebrow">{{ lane.eyebrow }}</span>
                <h2>{{ lane.title }}</h2>
              </div>
              <el-tag effect="plain" :type="lane.tagType">{{ lane.statusLabel }}</el-tag>
            </header>

            <div class="admin-support-lane__owner">
              <span>Owner</span>
              <strong>{{ lane.owner }}</strong>
            </div>

            <p>{{ lane.description }}</p>

            <div class="admin-support-lane__metric">
              <span>{{ lane.metricLabel }}</span>
              <strong>{{ lane.metricValue }}</strong>
              <small>{{ lane.metricHint }}</small>
            </div>

            <div class="admin-support-lane__actions">
              <router-link
                v-for="route in lane.routes"
                :key="route.path"
                :to="route.path"
                class="admin-support-link"
              >
                {{ route.label }}
              </router-link>
            </div>

            <div class="admin-support-preview">
              <h3>{{ lane.previewTitle }}</h3>
              <div v-if="lane.previewItems.length" class="admin-support-preview__list">
                <article v-for="item in lane.previewItems" :key="item.id || item.title" class="admin-support-preview__item">
                  <strong>{{ item.title }}</strong>
                  <span>{{ item.meta }}</span>
                </article>
              </div>
              <EmptyState
                v-else
                :title="lane.emptyTitle"
                :description="lane.emptyDescription"
              />
            </div>
          </article>
        </section>

        <section class="admin-support-blocked">
          <article v-for="lane in blockedLanes" :key="lane.key" class="shell-card admin-support-lane admin-support-lane--blocked">
            <header class="admin-support-lane__header">
              <div>
                <span class="admin-support-lane__eyebrow">{{ lane.eyebrow }}</span>
                <h2>{{ lane.title }}</h2>
              </div>
              <el-tag effect="plain" :type="lane.tagType">{{ lane.statusLabel }}</el-tag>
            </header>

            <div class="admin-support-lane__owner">
              <span>Owner</span>
              <strong>{{ lane.owner }}</strong>
            </div>

            <p>{{ lane.description }}</p>

            <ul class="admin-support-gap-list">
              <li v-for="gap in lane.gaps" :key="gap">{{ gap }}</li>
            </ul>

            <div class="admin-support-disabled-actions">
              <el-button plain disabled>无可用队列</el-button>
              <el-button plain disabled>等待独立范围与 API</el-button>
            </div>
          </article>
        </section>
      </template>
    </template>
  </div>
</template>

<style scoped>
.admin-support-hero,
.admin-support-grid,
.admin-support-blocked {
  display: grid;
  gap: 18px;
}

.admin-support-metric small,
.admin-support-lane p,
.admin-support-lane__metric small,
.admin-support-preview__item span,
.admin-support-overview__item small {
  color: var(--cm-text-secondary);
  line-height: 1.65;
}

.admin-support-overview__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.admin-support-overview__item,
.admin-support-lane__metric,
.admin-support-preview__item {
  display: grid;
  gap: 6px;
  padding: 14px;
  border: 1px solid var(--cm-border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.72);
}

.admin-support-overview__item span,
.admin-support-lane__metric span,
.admin-support-lane__owner span {
  color: var(--cm-text-tertiary);
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
}

.admin-support-overview__item strong,
.admin-support-lane__metric strong {
  font-size: 24px;
}

.admin-support-grid,
.admin-support-blocked {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.admin-support-lane {
  display: grid;
  align-content: start;
  gap: 16px;
  box-shadow: none;
}

.admin-support-lane--blocked {
  border-style: dashed;
  background: rgba(255, 255, 255, 0.72);
}

.admin-support-lane__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.admin-support-lane__eyebrow {
  color: var(--cm-text-tertiary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.admin-support-lane h2 {
  margin: 6px 0 0;
}

.admin-support-lane__owner {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.admin-support-lane__actions,
.admin-support-disabled-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.admin-support-link {
  display: inline-flex;
  min-height: 36px;
  align-items: center;
  justify-content: center;
  padding: 0 14px;
  border: 1px solid rgba(50, 91, 63, 0.2);
  border-radius: 999px;
  color: var(--cm-primary);
  font-weight: 700;
  text-decoration: none;
  transition: background 160ms ease, border-color 160ms ease;
}

.admin-support-link:hover,
.admin-support-link:focus-visible {
  border-color: rgba(50, 91, 63, 0.42);
  background: rgba(50, 91, 63, 0.08);
}

.admin-support-preview {
  display: grid;
  gap: 12px;
}

.admin-support-preview h3 {
  margin: 0;
  font-size: 16px;
}

.admin-support-preview__list,
.admin-support-gap-list {
  display: grid;
  gap: 10px;
}

.admin-support-preview__item strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-support-gap-list {
  margin: 0;
  padding-left: 18px;
  color: var(--cm-text-secondary);
  line-height: 1.65;
}

@media (max-width: 960px) {
  .admin-support-grid,
  .admin-support-blocked {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .admin-support-lane__header {
    flex-direction: column;
  }
}
</style>
