<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, getOrderList } from '@/api/modules/order'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import TradeMetricStrip from '@/components/trade/TradeMetricStrip.vue'
import TradeOrderCard from '@/components/trade/TradeOrderCard.vue'
import TradePageShell from '@/components/trade/TradePageShell.vue'
import { countOrdersByStatus } from '@/components/trade/trade-meta'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()
const loading = ref(false)
const loadError = ref('')
const orders = ref([])
const activeFilter = ref('all')
const messageActionLoading = ref(false)

const orderCounts = computed(() => countOrdersByStatus(orders.value))
const filteredOrders = computed(() => {
  if (activeFilter.value === 'all') {
    return orders.value
  }
  if (activeFilter.value === 'refunding') {
    return orders.value.filter((order) => ['refunding', 'refund_in_progress'].includes(order.orderStatus))
  }
  return orders.value.filter((order) => order.orderStatus === activeFilter.value)
})

const filterOptions = computed(() => [
  { key: 'all', label: '全部订单', count: orderCounts.value.all },
  { key: 'pending_payment', label: '待支付', count: orderCounts.value.pending_payment },
  { key: 'pending_fulfillment', label: '已支付', count: orderCounts.value.pending_fulfillment },
  { key: 'pending_receipt', label: '待收货', count: orderCounts.value.pending_receipt },
  { key: 'completed', label: '已完成', count: orderCounts.value.completed },
  { key: 'refunding', label: '退款中', count: orderCounts.value.refunding },
  { key: 'refunded', label: '退款完成', count: orderCounts.value.refunded }
])

const metrics = computed(() => [
  {
    label: '全部订单',
    value: String(orderCounts.value.all),
    helper: '包含所有状态的订单记录'
  },
  {
    label: '待处理',
    value: String(
      orderCounts.value.pending_payment +
        orderCounts.value.pending_fulfillment +
        orderCounts.value.pending_receipt +
        orderCounts.value.refunding
    ),
    helper: '需要你操作或关注进度的订单'
  },
  {
    label: '已完成',
    value: String(orderCounts.value.completed),
    helper: '交易已完成，可以去评价'
  }
])

function getLegacyOrderId() {
  const legacyOrderId = route.query.orderId
  if (Array.isArray(legacyOrderId)) {
    return legacyOrderId[0]
  }
  return legacyOrderId
}

function redirectLegacyDetailLink() {
  const legacyOrderId = getLegacyOrderId()
  if (!legacyOrderId) {
    return false
  }

  router.replace({
    name: 'app-order-detail',
    params: { orderId: String(legacyOrderId) }
  })
  return true
}

async function loadOrders() {
  loading.value = true
  loadError.value = ''
  try {
    const response = await getOrderList()
    orders.value = response.data || []
  } catch (error) {
    loadError.value = error?.response?.data?.message || error?.message || '订单列表加载失败'
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

function openDetail(orderId) {
  router.push({
    name: 'app-order-detail',
    params: { orderId: String(orderId) }
  })
}

function goPay(orderId) {
  router.push(`/app/payments/${orderId}`)
}

function resolvePeerUserId(order) {
  if (!order) return null
  const currentUserId = String(authStore.currentUser?.id || '')
  const candidates = [
    order.peerUserId,
    order.counterpartyUserId,
    order.sellerId,
    order.sellerUserId,
    order.buyerId,
    order.buyerUserId
  ].filter((id) => id !== undefined && id !== null && id !== '')

  return candidates.find((id) => String(id) !== currentUserId) || null
}

async function ensureOrderForMessage(order) {
  if (!order?.id) return null
  if (resolvePeerUserId(order)) {
    return order
  }
  const response = await getOrderDetail(order.id)
  return response.data
}

async function handleContactOrder(order) {
  if (!order?.id || messageActionLoading.value) return

  messageActionLoading.value = true
  try {
    const sourceOrder = await ensureOrderForMessage(order)
    const peerUserId = resolvePeerUserId(sourceOrder)
    if (!peerUserId) {
      ElMessage.error('订单缺少对方用户字段，无法创建会话')
      return
    }

    const conversation = await chatStore.findOrCreateConversation(
      Number(peerUserId),
      null,
      sourceOrder.shopId || sourceOrder.shop_id || null
    )
    await chatStore.sendOrderCardMessage(conversation.id, Number(sourceOrder.id))
    ElMessage.success('已发送订单卡片')
    router.push({
      name: 'app-message-detail',
      params: { conversationId: String(conversation.id) }
    })
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '订单卡片发送失败')
  } finally {
    messageActionLoading.value = false
  }
}

onMounted(() => {
  if (redirectLegacyDetailLink()) {
    return
  }
  loadOrders()
})

watch(
  () => route.query.orderId,
  () => {
    redirectLegacyDetailLink()
  }
)
</script>

<template>
  <div v-loading="loading">
    <TradePageShell
      eyebrow="Trade Center"
      title="订单与售后"
      description="查看所有订单的支付、履约和售后状态。"
      current-key="orders"
    >
      <template #actions>
        <el-button plain @click="$router.push('/app/trade')">返回交易中心</el-button>
        <el-button type="primary" @click="$router.push('/app/reviews/pending')">前往待评价</el-button>
      </template>

      <template #metrics>
        <TradeMetricStrip :items="metrics" />
      </template>

      <ErrorBlock v-if="loadError" :message="loadError" @retry="loadOrders" />

      <template v-else-if="!loading">
        <EmptyState
          v-if="!orders.length"
          emoji="📦"
          title="暂时没有订单"
          description="去探索页逛逛，下单后订单会出现在这里。"
        >
          <el-button type="primary" @click="$router.push('/app/products')">去逛商品</el-button>
        </EmptyState>

        <template v-else>
          <section class="shell-card orders-stage-card">
            <div class="orders-stage-card__copy">
              <h2>当前只需要先看清状态，再做动作</h2>
              <p>待支付、已支付、待收货、退款中、退款完成和已完成都会明确展示，售后与举报不再挤在列表页里处理。</p>
            </div>
            <div class="orders-stage-card__links">
              <el-button plain @click="$router.push('/app/reviews/pending')">待评价</el-button>
              <el-button plain @click="$router.push('/app/reviews/mine')">我的评价</el-button>
            </div>
          </section>

          <section class="shell-card orders-filter-card">
            <div class="orders-filter-card__head">
              <div>
                <h2>按交易阶段查看</h2>
                <p>先看当前最需要处理的状态，移动端也只保留这一条清晰路径。</p>
              </div>
            </div>
            <div class="orders-filter-row">
              <button
                v-for="item in filterOptions"
                :key="item.key"
                class="orders-filter-chip"
                :class="{ 'is-active': activeFilter === item.key }"
                type="button"
                @click="activeFilter = item.key"
              >
                <span>{{ item.label }}</span>
                <strong>{{ item.count }}</strong>
              </button>
            </div>
          </section>

          <section class="order-list">
            <TradeOrderCard
              v-for="order in filteredOrders"
              :key="order.id"
              :order="order"
            >
              <el-button plain @click="openDetail(order.id)">查看详情</el-button>
              <el-button plain :loading="messageActionLoading" @click="handleContactOrder(order)">联系对方</el-button>
              <el-button
                v-if="order.orderStatus === 'pending_payment'"
                type="primary"
                @click="goPay(order.id)"
              >
                去支付
              </el-button>
            </TradeOrderCard>
          </section>

          <EmptyState
            v-if="orders.length > 0 && filteredOrders.length === 0"
            emoji="🧾"
            title="这个阶段暂时没有订单"
            description="可以切回全部订单，或者继续前往待评价、退款与举报处理路径。"
          >
            <el-button type="primary" @click="activeFilter = 'all'">查看全部订单</el-button>
          </EmptyState>
        </template>
      </template>
    </TradePageShell>
  </div>
</template>

<style scoped>
.orders-stage-card,
.orders-stage-card__links {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.orders-stage-card__copy,
.orders-filter-card,
.orders-filter-card__head {
  display: grid;
  gap: 16px;
}

.orders-filter-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.orders-filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  padding: 10px 14px;
  border-radius: 999px;
  border: 1px solid var(--cm-border);
  background: rgba(255, 255, 255, 0.72);
  color: var(--cm-text-secondary);
  cursor: pointer;
  transition:
    transform var(--cm-transition),
    border-color var(--cm-transition),
    background-color var(--cm-transition);
}

.orders-filter-chip strong {
  color: var(--cm-text);
}

.orders-filter-chip:hover {
  transform: translateY(-1px);
  border-color: rgba(var(--cm-primary-rgb), 0.2);
}

.orders-filter-chip.is-active {
  border-color: rgba(var(--cm-primary-rgb), 0.24);
  background: rgba(var(--cm-primary-rgb), 0.1);
  color: var(--cm-primary);
}

.order-list {
  display: grid;
  gap: 16px;
}

@media (max-width: 768px) {
  .orders-stage-card,
  .orders-stage-card__links {
    flex-direction: column;
    align-items: stretch;
  }

  .orders-filter-chip,
  .orders-stage-card__links :deep(.el-button) {
    width: 100%;
  }
}
</style>
