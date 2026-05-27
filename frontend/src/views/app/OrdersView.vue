<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-services'
import { useRoute, useRouter } from 'vue-router'
import {
  accessDigitalAsset,
  applyRefund,
  buyerConfirmOffline,
  cancelOrder,
  confirmReceipt,
  getOrderDetail,
  getOrderList
} from '@/api/modules/order'
import { submitReport } from '@/api/modules/report'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import TradeMetricStrip from '@/components/trade/TradeMetricStrip.vue'
import TradeOrderCard from '@/components/trade/TradeOrderCard.vue'
import TradePageShell from '@/components/trade/TradePageShell.vue'
import TradeStatusTag from '@/components/trade/TradeStatusTag.vue'
import {
  countOrdersByStatus,
  formatCurrency,
  getFulfillmentTypeMeta,
  getOrderStatusMeta,
  getPaymentStatusMeta
} from '@/components/trade/trade-meta'
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

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detail = ref(null)
const activeDetailOrderId = ref(null)

const actionLoading = ref(false)
const messageActionLoading = ref(false)
const refundForm = reactive({
  refundReason: ''
})

const reportDialogVisible = ref(false)
const reportForm = reactive({
  reason: '',
  content: ''
})

const windowWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1024)

const drawerSize = computed(() => (windowWidth.value < 768 ? '100%' : '760px'))
const orderCounts = computed(() => countOrdersByStatus(orders.value))
const filteredOrders = computed(() => {
  if (activeFilter.value === 'all') {
    return orders.value
  }
  return orders.value.filter((order) => order.orderStatus === activeFilter.value)
})

const filterOptions = computed(() => [
  { key: 'all', label: '全部订单', count: orderCounts.value.all },
  { key: 'pending_payment', label: '待支付', count: orderCounts.value.pending_payment },
  { key: 'pending_fulfillment', label: '已支付', count: orderCounts.value.pending_fulfillment },
  { key: 'pending_receipt', label: '待收货', count: orderCounts.value.pending_receipt },
  { key: 'completed', label: '已完成', count: orderCounts.value.completed },
  { key: 'refund_in_progress', label: '退款中', count: orderCounts.value.refund_in_progress },
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
        orderCounts.value.refund_in_progress
    ),
    helper: '需要你操作或关注进度的订单'
  },
  {
    label: '已完成',
    value: String(orderCounts.value.completed),
    helper: '交易已完成，可以去评价'
  }
])

const detailOrderMeta = computed(() => getOrderStatusMeta(detail.value?.orderStatus))
const detailPaymentMeta = computed(() => getPaymentStatusMeta(detail.value?.paymentStatus))
const detailFulfillmentMeta = computed(() => getFulfillmentTypeMeta(detail.value?.fulfillmentType))

function onResize() {
  windowWidth.value = window.innerWidth
}

async function loadOrders() {
  loading.value = true
  loadError.value = ''
  try {
    const response = await getOrderList()
    orders.value = response.data || []
    if (route.query.orderId) {
      await openDetail(route.query.orderId)
    }
  } catch (error) {
    loadError.value = error?.response?.data?.message || error?.message || '订单列表加载失败'
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

async function openDetail(orderId) {
  activeDetailOrderId.value = orderId
  detailLoading.value = true
  detailError.value = ''
  try {
    const response = await getOrderDetail(orderId)
    detail.value = response.data
    detailVisible.value = true
  } catch (error) {
    detailError.value = error?.response?.data?.message || error?.message || '订单详情加载失败'
    ElMessage.error(detailError.value)
  } finally {
    detailLoading.value = false
  }
}

async function handleCancel(orderId) {
  if (actionLoading.value) return
  try {
    await ElMessageBox.confirm('确认取消这个待支付订单吗？', '取消订单')
    actionLoading.value = true
    await cancelOrder(orderId)
    ElMessage.success('订单已取消')
    detailVisible.value = false
    await loadOrders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '取消订单失败')
    }
  } finally {
    actionLoading.value = false
  }
}

async function handleConfirm(orderId, offline = false) {
  if (actionLoading.value) return
  actionLoading.value = true
  try {
    if (offline) {
      await buyerConfirmOffline(orderId)
    } else {
      await confirmReceipt(orderId)
    }
    ElMessage.success('已确认收货')
    await openDetail(orderId)
    await loadOrders()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '确认收货失败')
  } finally {
    actionLoading.value = false
  }
}

async function handleRefund(orderId) {
  if (actionLoading.value) return
  if (!refundForm.refundReason.trim()) {
    ElMessage.warning('请填写退款原因')
    return
  }
  actionLoading.value = true
  try {
    await applyRefund(orderId, { refundReason: refundForm.refundReason.trim() })
    ElMessage.success('退款申请已提交')
    refundForm.refundReason = ''
    await openDetail(orderId)
    await loadOrders()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '退款申请失败')
  } finally {
    actionLoading.value = false
  }
}

async function handleAccessAsset(orderId, assetId) {
  if (actionLoading.value) return
  actionLoading.value = true
  try {
    const response = await accessDigitalAsset(orderId, assetId)
    ElMessage.success(`已记录访问，资源路径：${response.data.asset.assetUrl}`)
    detail.value.digitalAccessLogs = response.data.accessLogs
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '资源访问失败')
  } finally {
    actionLoading.value = false
  }
}

function goPay(orderId) {
  router.push(`/app/payments/${orderId}`)
}

function openReportDialog() {
  reportForm.reason = ''
  reportForm.content = ''
  reportDialogVisible.value = true
}

async function handleSubmitReport() {
  if (actionLoading.value || !detail.value) return
  if (!reportForm.reason.trim()) {
    ElMessage.warning('请选择举报原因')
    return
  }
  if (!reportForm.content.trim()) {
    ElMessage.warning('请填写举报描述')
    return
  }
  actionLoading.value = true
  try {
    await submitReport({
      targetType: 'order',
      targetId: detail.value.id,
      targetLabel: detail.value.orderNo || '订单',
      reason: reportForm.reason.trim(),
      content: reportForm.content.trim()
    })
    ElMessage.success('举报已提交，平台会尽快处理')
    reportDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '举报提交失败')
  } finally {
    actionLoading.value = false
  }
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

async function handleContactOrder(order = detail.value) {
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

function openOrderMessage(intent = 'after_sales') {
  if (intent !== 'support') {
    handleContactOrder(detail.value)
    return
  }

  router.push({
    path: '/app/messages',
    query: {
      category: intent === 'support' ? 'support' : 'trade',
      entry: 'order',
      entryId: String(detail.value?.id || ''),
      targetType: intent === 'support' ? 'support' : 'shop',
      targetId: String(detail.value?.shopId || detail.value?.sellerId || ''),
      intent
    }
  })
}

watch(
  () => route.query.orderId,
  (orderId) => {
    if (orderId && String(orderId) !== String(activeDetailOrderId.value || '')) {
      openDetail(orderId)
    }
  }
)

onMounted(() => {
  loadOrders()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
})
</script>

<template>
  <div v-loading="loading">
    <TradePageShell
      eyebrow="Trade Center"
      title="订单与售后"
      description="查看所有订单的支付、物流和售后状态。"
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
              <p>待支付、已支付、待收货、退款中、退款完成和已完成都会明确展示，售后与举报不会因为视觉简化被藏起来。</p>
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
            emoji="🧭"
            title="这个阶段暂时没有订单"
            description="可以切回全部订单，或者继续前往待评价、退款与举报处理路径。"
          >
            <el-button type="primary" @click="activeFilter = 'all'">查看全部订单</el-button>
          </EmptyState>
        </template>
      </template>
    </TradePageShell>

    <el-drawer v-model="detailVisible" :size="drawerSize" title="订单详情">
      <div v-if="detailLoading" class="drawer-loading shell-card">
        <el-skeleton :rows="8" animated />
      </div>

      <ErrorBlock
        v-else-if="detailError"
        :message="detailError"
        @retry="activeDetailOrderId && openDetail(activeDetailOrderId)"
      />

      <div v-else-if="detail" class="drawer-stack">
        <section class="drawer-panel drawer-panel--hero">
          <div class="drawer-panel__hero-copy">
            <p class="drawer-panel__eyebrow">{{ detail.orderNo }}</p>
            <h2>{{ detailOrderMeta.label }}</h2>
            <p>{{ detailOrderMeta.description }}</p>
          </div>
          <div class="drawer-panel__hero-meta">
            <TradeStatusTag kind="order" :value="detail.orderStatus" />
            <TradeStatusTag kind="payment" :value="detail.paymentStatus" />
            <TradeStatusTag kind="fulfillment" :value="detail.fulfillmentType" />
            <strong>{{ formatCurrency(detail.payableAmount) }}</strong>
          </div>
        </section>

        <section class="drawer-panel">
          <h3>订单概览</h3>
          <div class="detail-grid">
            <div class="detail-kv">
              <span>订单状态</span>
              <strong>{{ detailOrderMeta.label }}</strong>
            </div>
            <div class="detail-kv">
              <span>支付状态</span>
              <strong>{{ detailPaymentMeta.label }}</strong>
            </div>
            <div class="detail-kv">
              <span>履约方式</span>
              <strong>{{ detailFulfillmentMeta.label }}</strong>
            </div>
            <div class="detail-kv">
              <span>买家备注</span>
              <strong>{{ detail.buyerNote || '无' }}</strong>
            </div>
          </div>
        </section>

        <section class="drawer-panel">
          <h3>商品快照</h3>
          <article v-for="item in detail.items" :key="item.id" class="line-item">
            <div class="line-item__copy">
              <strong>{{ item.productTitleSnapshot }}</strong>
              <span>x{{ item.quantity }}</span>
            </div>
            <span>{{ formatCurrency(item.subtotalAmount) }}</span>
          </article>
        </section>

        <section class="drawer-panel">
          <h3>履约信息</h3>
          <div class="detail-grid">
            <div class="detail-kv">
              <span>履约状态</span>
              <strong>{{ detail.fulfillment?.fulfillmentStatus || '未开始' }}</strong>
            </div>
            <div class="detail-kv" v-if="detail.fulfillment?.addressSnapshot">
              <span>收货地址</span>
              <strong>
                {{ detail.fulfillment.addressSnapshot.campusName }} /
                {{ detail.fulfillment.addressSnapshot.detailAddress }}
              </strong>
            </div>
            <div class="detail-kv" v-if="detail.fulfillment?.logisticsCompany">
              <span>物流信息</span>
              <strong>{{ detail.fulfillment.logisticsCompany }} / {{ detail.fulfillment.trackingNo }}</strong>
            </div>
            <div class="detail-kv" v-if="detail.fulfillment?.offlineMeetTime">
              <span>线下约定</span>
              <strong>{{ detail.fulfillment.offlineMeetTime }} / {{ detail.fulfillment.offlineMeetLocation }}</strong>
            </div>
            <div class="detail-kv" v-if="detail.fulfillment?.downloadAccessStatus !== 'not_applicable'">
              <span>下载权限</span>
              <strong>{{ detail.fulfillment?.downloadAccessStatus }}</strong>
            </div>
          </div>
        </section>

        <section v-if="detail.digitalAssets?.length" class="drawer-panel">
          <h3>数字交付资源</h3>
          <article v-for="asset in detail.digitalAssets" :key="asset.id" class="line-item">
            <div class="line-item__copy">
              <strong>{{ asset.assetName }}</strong>
              <span>{{ asset.isFullAsset ? '完整资源' : '预览资源' }}</span>
            </div>
            <template v-if="asset.isFullAsset">
              <el-button
                size="small"
                type="primary"
                :loading="actionLoading"
                @click="handleAccessAsset(detail.id, asset.id)"
              >
                访问资源
              </el-button>
            </template>
            <span v-else class="text-muted">{{ asset.assetUrl }}</span>
          </article>

          <div v-if="detail.digitalAccessLogs?.length" class="access-log">
            <h4>访问记录</h4>
            <article v-for="log in detail.digitalAccessLogs" :key="log.id" class="log-item">
              <span>{{ log.assetName }}</span>
              <span>{{ log.accessType }}</span>
              <span>{{ log.accessedAt }}</span>
            </article>
          </div>
        </section>

        <section v-if="detail.payments?.length" class="drawer-panel">
          <h3>支付记录</h3>
          <article v-for="paymentItem in detail.payments" :key="paymentItem.id" class="line-item">
            <div class="line-item__copy">
              <strong>{{ paymentItem.paymentNo }}</strong>
              <span>{{ getPaymentStatusMeta(paymentItem.paymentStatus).label }}</span>
            </div>
            <span>{{ formatCurrency(paymentItem.payableAmount || paymentItem.amount) }}</span>
          </article>
        </section>

        <section class="drawer-panel">
          <h3>退款与售后</h3>
          <p class="refund-rule">{{ detail.refundRuleText }}</p>
          <div v-if="detail.refunds?.length">
            <article v-for="refund in detail.refunds" :key="refund.id" class="line-item">
              <div class="line-item__copy">
                <strong>{{ refund.refundNo }}</strong>
                <span>{{ refund.refundStatus }}</span>
              </div>
              <span>{{ formatCurrency(refund.refundAmount) }}</span>
            </article>
          </div>
          <p v-else class="text-muted">暂无退款记录</p>

          <div v-if="detail.availableActions?.includes('apply_refund')" class="refund-box">
            <el-input v-model="refundForm.refundReason" placeholder="填写退款原因" />
            <el-button
              type="warning"
              :loading="actionLoading"
              :disabled="actionLoading"
              @click="handleRefund(detail.id)"
            >
              申请退款
            </el-button>
          </div>
        </section>

        <section class="drawer-panel">
          <h3>举报与平台介入</h3>
          <p>如果遇到履约异常、商品与描述不符或其他交易纠纷，可以在这里提交举报，不需要跳到交易域外处理。</p>
          <div class="shell-inline-actions">
            <el-button type="danger" plain :disabled="actionLoading" @click="openReportDialog">
              提交举报
            </el-button>
            <el-button
              plain
              :loading="messageActionLoading"
              :disabled="actionLoading || messageActionLoading"
              @click="openOrderMessage()"
            >
              发送订单卡片
            </el-button>
            <el-button plain :disabled="actionLoading" @click="openOrderMessage('support')">
              联系平台客服
            </el-button>
          </div>
        </section>

        <div class="shell-inline-actions drawer-actions">
          <el-button v-if="detail.availableActions?.includes('pay')" type="primary" @click="goPay(detail.id)">
            去支付
          </el-button>
          <el-button
            v-if="detail.availableActions?.includes('cancel')"
            plain
            :loading="actionLoading"
            :disabled="actionLoading"
            @click="handleCancel(detail.id)"
          >
            取消订单
          </el-button>
          <el-button
            v-if="detail.availableActions?.includes('confirm_receipt')"
            type="success"
            :loading="actionLoading"
            :disabled="actionLoading"
            @click="handleConfirm(detail.id)"
          >
            确认收货
          </el-button>
          <el-button
            v-if="detail.availableActions?.includes('offline_buyer_confirm')"
            type="success"
            :loading="actionLoading"
            :disabled="actionLoading"
            @click="handleConfirm(detail.id, true)"
          >
            线下确认收货
          </el-button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="reportDialogVisible" title="提交举报" width="480px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="举报原因">
          <el-select v-model="reportForm.reason" placeholder="请选择举报原因">
            <el-option label="商品与描述不符" value="inaccurate_content" />
            <el-option label="卖家未履约" value="seller_not_fulfilling" />
            <el-option label="商品质量问题" value="quality_issue" />
            <el-option label="虚假交易" value="fake_transaction" />
            <el-option label="其他违规" value="other_violation" />
          </el-select>
        </el-form-item>
        <el-form-item label="举报描述">
          <el-input
            v-model="reportForm.content"
            type="textarea"
            :rows="4"
            placeholder="请详细描述举报内容（不超过 1000 字）"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleSubmitReport">
          提交举报
        </el-button>
      </template>
    </el-dialog>
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
.orders-filter-card__head,
.drawer-stack {
  display: grid;
  gap: 16px;
}

.orders-filter-row,
.drawer-panel__hero-meta {
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

.order-list,
.drawer-loading {
  display: grid;
  gap: 16px;
}

.drawer-panel {
  border: 1px solid rgba(50, 91, 63, 0.12);
  border-radius: 18px;
  padding: 18px;
  background: rgba(255, 255, 255, 0.68);
}

.drawer-panel--hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.drawer-panel__hero-copy {
  display: grid;
  gap: 8px;
}

.drawer-panel__eyebrow {
  color: var(--cm-text-tertiary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.drawer-panel__hero-meta strong {
  color: var(--cm-price);
  font-size: 28px;
  line-height: 1.1;
}

.drawer-panel h3,
.access-log h4 {
  margin: 0;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.detail-kv,
.line-item,
.refund-box {
  display: flex;
}

.detail-kv {
  flex-direction: column;
  gap: 6px;
}

.detail-kv span,
.refund-rule,
.text-muted,
.log-item {
  color: var(--cm-text-secondary);
  font-size: 13px;
  line-height: 1.55;
}

.line-item,
.refund-box {
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.line-item {
  padding: 10px 0;
  border-bottom: 1px solid rgba(88, 62, 43, 0.08);
}

.line-item:last-child {
  border-bottom: 0;
}

.line-item__copy {
  display: grid;
  gap: 4px;
}

.refund-box {
  margin-top: 12px;
}

.access-log {
  margin-top: 12px;
  border-top: 1px solid rgba(50, 91, 63, 0.08);
  padding-top: 12px;
}

.log-item {
  display: flex;
  gap: 12px;
  padding: 4px 0;
}

.drawer-actions {
  padding-bottom: 4px;
}

@media (max-width: 768px) {
  .orders-stage-card,
  .orders-stage-card__links,
  .drawer-panel--hero,
  .line-item,
  .refund-box,
  .log-item {
    flex-direction: column;
    align-items: stretch;
  }

  .orders-filter-chip,
  .drawer-actions :deep(.el-button),
  .orders-stage-card__links :deep(.el-button),
  .refund-box :deep(.el-button) {
    width: 100%;
  }
}
</style>
