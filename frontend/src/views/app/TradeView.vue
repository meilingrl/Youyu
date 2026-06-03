<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCart, getOrderList } from '@/api/modules/order'
import { getPendingReviewItems } from '@/api/modules/review'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import TradeMetricStrip from '@/components/trade/TradeMetricStrip.vue'
import TradePageShell from '@/components/trade/TradePageShell.vue'
import { countOrdersByStatus, formatCurrency } from '@/components/trade/trade-meta'

const router = useRouter()

const cart = ref(createEmptyCart())
const orders = ref([])
const pendingReviews = ref([])

const sources = reactive({
  cart: {
    loading: false,
    error: ''
  },
  orders: {
    loading: false,
    error: ''
  },
  reviews: {
    loading: false,
    error: ''
  }
})

const sourceLabels = {
  cart: '购物车',
  orders: '订单',
  reviews: '待评价'
}

const loading = computed(() => sources.cart.loading || sources.orders.loading || sources.reviews.loading)
const sourceErrors = computed(() =>
  Object.entries(sources)
    .filter(([, source]) => source.error)
    .map(([key, source]) => ({
      key,
      label: sourceLabels[key],
      message: source.error
    }))
)
const allSourcesFailed = computed(() => sourceErrors.value.length === Object.keys(sources).length)

const cartItems = computed(() => cart.value.items)
const cartItemCount = computed(() => cartItems.value.length)
const selectedCartCount = computed(() => {
  const summaryCount = Number(cart.value.summary?.selectedCount)
  if (Number.isFinite(summaryCount)) {
    return summaryCount
  }
  return cartItems.value.filter((item) => item.selected).length
})
const selectedCartAmount = computed(() => Number(cart.value.summary?.selectedAmount || 0))

const orderCounts = computed(() => countOrdersByStatus(orders.value))
const pendingPaymentOrders = computed(() => orders.value.filter((order) => order.orderStatus === 'pending_payment'))
const pendingReceiptOrders = computed(() => orders.value.filter((order) => order.orderStatus === 'pending_receipt'))
const refundInProgressOrders = computed(() =>
  orders.value.filter((order) => ['refunding', 'refund_in_progress'].includes(order.orderStatus))
)
const pendingReviewCount = computed(() => pendingReviews.value.length)

const hasLoadedData = computed(
  () => cartItemCount.value > 0 || orders.value.length > 0 || pendingReviewCount.value > 0
)
const hasActionableData = computed(() => nextActions.value.length > 0)

const metrics = computed(() => [
  {
    label: '购物车商品',
    value: String(cartItemCount.value),
    helper: sources.cart.error ? '购物车暂未更新' : '来自当前购物车条目数'
  },
  {
    label: '已选购物车',
    value: String(selectedCartCount.value),
    helper: selectedCartCount.value
      ? `已选商品合计 ${formatCurrency(selectedCartAmount.value)}`
      : '来自购物车 selectedCount'
  },
  {
    label: '待支付',
    value: String(orderCounts.value.pending_payment),
    helper: '来自订单状态 pending_payment'
  },
  {
    label: '待收货',
    value: String(orderCounts.value.pending_receipt),
    helper: '来自订单状态 pending_receipt'
  },
  {
    label: '退款处理中',
    value: String(orderCounts.value.refunding),
    helper: '来自订单状态 refunding'
  },
  {
    label: '已完成',
    value: String(orderCounts.value.completed),
    helper: '来自订单状态 completed'
  },
  {
    label: '待评价',
    value: String(pendingReviewCount.value),
    helper: '来自待评价列表'
  }
])

const nextActions = computed(() => {
  const actions = []
  const firstPendingPayment = pendingPaymentOrders.value[0]

  if (firstPendingPayment) {
    actions.push({
      key: 'pay',
      tone: 'warning',
      eyebrow: 'Pay now',
      title: `${pendingPaymentOrders.value.length} 个订单待支付`,
      description: firstPendingPayment.orderNo
        ? `优先处理 ${firstPendingPayment.orderNo}，避免订单继续停留在待支付。`
        : '优先完成待支付订单。',
      metric: formatCurrency(firstPendingPayment.payableAmount),
      buttonText: '去支付',
      route: `/app/payments/${firstPendingPayment.id}`
    })
  }

  if (pendingReceiptOrders.value.length) {
    actions.push({
      key: 'receipt',
      tone: 'primary',
      eyebrow: 'Confirm receipt',
      title: `${pendingReceiptOrders.value.length} 个订单待确认收货`,
      description: '订单页可以查看履约信息并完成确认收货。',
      metric: String(pendingReceiptOrders.value.length),
      buttonText: '查看订单',
      route: '/app/orders'
    })
  }

  if (refundInProgressOrders.value.length) {
    actions.push({
      key: 'refund',
      tone: 'danger',
      eyebrow: 'Refund',
      title: `${refundInProgressOrders.value.length} 个退款处理中`,
      description: '退款进度仍在订单与售后里跟踪。',
      metric: String(refundInProgressOrders.value.length),
      buttonText: '跟进售后',
      route: '/app/orders'
    })
  }

  if (pendingReviewCount.value) {
    actions.push({
      key: 'review',
      tone: 'success',
      eyebrow: 'Review',
      title: `${pendingReviewCount.value} 个商品待评价`,
      description: '完成评价后，交易闭环会更完整。',
      metric: String(pendingReviewCount.value),
      buttonText: '去评价',
      route: '/app/reviews/pending'
    })
  }

  if (selectedCartCount.value) {
    actions.push({
      key: 'selected-cart',
      tone: 'primary',
      eyebrow: 'Cart',
      title: `${selectedCartCount.value} 件商品已选中`,
      description: `当前选中商品合计 ${formatCurrency(selectedCartAmount.value)}。`,
      metric: formatCurrency(selectedCartAmount.value),
      buttonText: '回到购物车',
      route: '/app/cart'
    })
  } else if (cartItemCount.value) {
    actions.push({
      key: 'cart',
      tone: 'muted',
      eyebrow: 'Cart',
      title: `${cartItemCount.value} 件商品在购物车`,
      description: '购物车里还有未选中的商品。',
      metric: String(cartItemCount.value),
      buttonText: '整理购物车',
      route: '/app/cart'
    })
  }

  return actions.slice(0, 4)
})

const quickEntries = computed(() => [
  {
    key: 'cart',
    eyebrow: 'Cart',
    title: '购物车',
    description: cartItemCount.value
      ? `${cartItemCount.value} 件商品，${selectedCartCount.value} 件已选。`
      : '暂时没有购物车商品。',
    buttonText: '进入购物车',
    route: '/app/cart'
  },
  {
    key: 'orders',
    eyebrow: 'Orders',
    title: '订单与售后',
    description: `${orders.value.length} 个订单，其中 ${orderCounts.value.pending_payment + orderCounts.value.pending_receipt + orderCounts.value.refunding} 个需要关注。`,
    buttonText: '查看订单',
    route: '/app/orders'
  },
  {
    key: 'reviews-pending',
    eyebrow: 'Review',
    title: '待评价',
    description: pendingReviewCount.value
      ? `${pendingReviewCount.value} 个商品等待评价。`
      : '暂无待评价商品。',
    buttonText: '查看待评价',
    route: '/app/reviews/pending'
  },
  {
    key: 'reviews-mine',
    eyebrow: 'History',
    title: '我的评价',
    description: '回看已提交的商品与店铺评价。',
    buttonText: '查看我的评价',
    route: '/app/reviews/mine'
  }
])

function createEmptyCart() {
  return {
    items: [],
    summary: {}
  }
}

function normalizeCart(data) {
  return {
    ...createEmptyCart(),
    ...(data || {}),
    items: Array.isArray(data?.items) ? data.items : [],
    summary: data?.summary || {}
  }
}

function normalizePendingReviews(data) {
  if (Array.isArray(data)) {
    return data
  }
  if (Array.isArray(data?.items)) {
    return data.items
  }
  return []
}

function resolveErrorMessage(error, fallback) {
  return error?.response?.data?.message || error?.message || fallback
}

function assertSuccess(response, fallback) {
  if (response?.success === false) {
    throw new Error(response.message || fallback)
  }
}

async function loadCartSource() {
  sources.cart.loading = true
  sources.cart.error = ''
  try {
    const response = await getCart()
    assertSuccess(response, '购物车加载失败')
    cart.value = normalizeCart(response?.data)
  } catch (error) {
    sources.cart.error = resolveErrorMessage(error, '购物车加载失败')
    cart.value = createEmptyCart()
  } finally {
    sources.cart.loading = false
  }
}

async function loadOrdersSource() {
  sources.orders.loading = true
  sources.orders.error = ''
  try {
    const response = await getOrderList()
    assertSuccess(response, '订单加载失败')
    orders.value = Array.isArray(response?.data) ? response.data : []
  } catch (error) {
    sources.orders.error = resolveErrorMessage(error, '订单加载失败')
    orders.value = []
  } finally {
    sources.orders.loading = false
  }
}

async function loadReviewsSource() {
  sources.reviews.loading = true
  sources.reviews.error = ''
  try {
    const response = await getPendingReviewItems()
    assertSuccess(response, '待评价加载失败')
    pendingReviews.value = normalizePendingReviews(response?.data)
  } catch (error) {
    sources.reviews.error = resolveErrorMessage(error, '待评价加载失败')
    pendingReviews.value = []
  } finally {
    sources.reviews.loading = false
  }
}

function loadDashboard() {
  return Promise.all([loadCartSource(), loadOrdersSource(), loadReviewsSource()])
}

function retrySource(key) {
  if (key === 'cart') {
    return loadCartSource()
  }
  if (key === 'orders') {
    return loadOrdersSource()
  }
  return loadReviewsSource()
}

function go(route) {
  router.push(route)
}

onMounted(() => {
  loadDashboard()
})
</script>

<template>
  <TradePageShell
    eyebrow="Trade Center"
    title="交易中心"
    description="购物车、订单、售后和评价的实时状态汇总。"
    current-key="trade"
  >
    <template #actions>
      <el-button plain :loading="loading" @click="loadDashboard">刷新状态</el-button>
      <el-button type="primary" @click="go('/app/cart')">查看购物车</el-button>
    </template>

    <template #metrics>
      <TradeMetricStrip :items="metrics" />
    </template>

    <ErrorBlock
      v-if="allSourcesFailed && !loading"
      title="交易状态加载失败"
      message="购物车、订单和待评价数据都没有加载成功。"
      @retry="loadDashboard"
    />

    <section v-else class="trade-dashboard">
      <section v-if="sourceErrors.length" class="trade-dashboard-errors" aria-label="部分数据加载失败">
        <article
          v-for="error in sourceErrors"
          :key="error.key"
          class="trade-dashboard-errors__item"
        >
          <div>
            <strong>{{ error.label }}未更新</strong>
            <p>{{ error.message }}</p>
          </div>
          <el-button size="small" plain @click="retrySource(error.key)">重试</el-button>
        </article>
      </section>

      <section v-if="loading && !hasLoadedData" class="shell-card trade-dashboard-loading">
        <el-skeleton :rows="5" animated />
      </section>

      <section
        v-if="hasActionableData"
        class="shell-card trade-action-panel"
        aria-label="下一步交易动作"
      >
        <div class="trade-action-panel__head">
          <div>
            <span class="trade-section-eyebrow">Next actions</span>
            <h2>现在最值得处理的交易动作</h2>
          </div>
          <span class="trade-action-panel__count">{{ nextActions.length }} 项</span>
        </div>

        <div class="trade-action-list">
          <article
            v-for="action in nextActions"
            :key="action.key"
            class="trade-action-card"
            :class="`trade-action-card--${action.tone}`"
          >
            <div class="trade-action-card__copy">
              <span>{{ action.eyebrow }}</span>
              <h3>{{ action.title }}</h3>
              <p>{{ action.description }}</p>
            </div>
            <div class="trade-action-card__aside">
              <strong>{{ action.metric }}</strong>
              <el-button type="primary" plain @click="go(action.route)">
                {{ action.buttonText }}
              </el-button>
            </div>
          </article>
        </div>
      </section>

      <EmptyState
        v-else-if="!loading && !sourceErrors.length"
        emoji="✓"
        title="当前没有待处理交易动作"
        description="购物车、订单售后和待评价都暂时没有需要优先处理的事项。"
      >
        <el-button type="primary" @click="go('/app/products')">去逛商品</el-button>
      </EmptyState>

      <section class="trade-overview-grid" aria-label="交易快捷入口">
        <article
          v-for="entry in quickEntries"
          :key="entry.key"
          class="shell-card trade-overview-card"
        >
          <span class="trade-overview-card__eyebrow">{{ entry.eyebrow }}</span>
          <h2>{{ entry.title }}</h2>
          <p>{{ entry.description }}</p>
          <el-button plain @click="go(entry.route)">{{ entry.buttonText }}</el-button>
        </article>
      </section>
    </section>
  </TradePageShell>
</template>

<style scoped>
.trade-dashboard,
.trade-dashboard-loading,
.trade-action-panel,
.trade-action-list,
.trade-action-card__copy,
.trade-overview-card {
  display: grid;
  gap: 16px;
}

.trade-dashboard-errors {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.trade-dashboard-errors__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 16px;
  border: 1px solid rgba(245, 158, 11, 0.26);
  border-radius: 16px;
  background: rgba(255, 251, 235, 0.78);
}

.trade-dashboard-errors__item p {
  margin: 4px 0 0;
  color: var(--cm-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.trade-action-panel__head,
.trade-action-card,
.trade-action-card__aside {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.trade-action-panel__head h2 {
  margin: 4px 0 0;
}

.trade-section-eyebrow,
.trade-overview-card__eyebrow,
.trade-action-card__copy span {
  color: var(--cm-text-tertiary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.trade-action-panel__count {
  min-width: 52px;
  text-align: center;
  padding: 7px 10px;
  border-radius: 999px;
  background: rgba(var(--cm-primary-rgb), 0.1);
  color: var(--cm-primary);
  font-size: 13px;
  font-weight: 700;
}

.trade-action-card {
  padding: 18px;
  border: 1px solid rgba(50, 91, 63, 0.12);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
}

.trade-action-card--warning {
  border-color: rgba(245, 158, 11, 0.24);
  background: rgba(255, 251, 235, 0.82);
}

.trade-action-card--danger {
  border-color: rgba(239, 68, 68, 0.2);
  background: rgba(254, 242, 242, 0.8);
}

.trade-action-card--success {
  border-color: rgba(16, 185, 129, 0.18);
  background: rgba(236, 253, 245, 0.76);
}

.trade-action-card__copy h3,
.trade-action-card__copy p,
.trade-overview-card h2,
.trade-overview-card p {
  margin: 0;
}

.trade-action-card__copy p,
.trade-overview-card p {
  color: var(--cm-text-secondary);
  line-height: 1.65;
}

.trade-action-card__aside strong {
  color: var(--cm-price);
  font-size: clamp(22px, 2.4vw, 30px);
  line-height: 1.1;
  white-space: nowrap;
}

.trade-overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 16px;
}

.trade-overview-card {
  align-content: start;
}

.trade-overview-card :deep(.el-button) {
  justify-self: start;
}

@media (max-width: 768px) {
  .trade-dashboard-errors__item,
  .trade-action-panel__head,
  .trade-action-card,
  .trade-action-card__aside {
    flex-direction: column;
    align-items: stretch;
  }

  .trade-action-panel__count {
    align-self: flex-start;
  }

  .trade-action-card__aside strong {
    white-space: normal;
  }

  .trade-action-card__aside :deep(.el-button),
  .trade-overview-card :deep(.el-button),
  .trade-dashboard-errors__item :deep(.el-button) {
    width: 100%;
  }
}
</style>
