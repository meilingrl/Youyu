<script setup>
import { computed, onMounted, ref } from 'vue'
import QRCode from 'qrcode'
import { ElMessage } from '@/plugins/element-plus-services'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail } from '@/api/modules/order'
import { completeMockPayment, getPaymentGateway, initiatePayment, resumePayment } from '@/api/modules/payment'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import TradeMobileActionBar from '@/components/trade/TradeMobileActionBar.vue'
import TradeMetricStrip from '@/components/trade/TradeMetricStrip.vue'
import TradePageShell from '@/components/trade/TradePageShell.vue'
import TradeStatusTag from '@/components/trade/TradeStatusTag.vue'
import {
  availablePaymentMethods,
  getPaymentAttemptMeta,
  getPaymentMethodMeta,
  latestPayment
} from '@/components/trade/payment-experience'
import {
  formatCurrency,
  getFulfillmentTypeMeta,
  getOrderStatusMeta,
  getPaymentStatusMeta
} from '@/components/trade/trade-meta'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const initiating = ref(false)
const confirming = ref(false)
const order = ref(null)
const payment = ref(null)
const gatewayResult = ref(null)
const gateway = ref(null)
const selectedPaymentMethod = ref('')
const qrImageUrl = ref('')

const orderStatusMeta = computed(() => getOrderStatusMeta(order.value?.orderStatus))
const paymentStatusMeta = computed(() => getPaymentStatusMeta(order.value?.paymentStatus))
const fulfillmentMeta = computed(() => getFulfillmentTypeMeta(order.value?.fulfillmentType))
const methods = computed(() => availablePaymentMethods(gateway.value))
const attemptMeta = computed(() => getPaymentAttemptMeta(payment.value?.paymentStatus))
const isPaid = computed(() => order.value?.paymentStatus === 'paid')
const canPay = computed(() => order.value?.orderStatus === 'pending_payment')
const hasActiveAttempt = computed(() => payment.value?.paymentStatus === 'initiated')
const canRetry = computed(() => canPay.value && attemptMeta.value.retryable)
const canCreatePayment = computed(() => canPay.value && !hasActiveAttempt.value)
const qrTarget = computed(() => gatewayResult.value?.qrCode || '')
const needsPaymentEntry = computed(() => hasActiveAttempt.value && !canConfirmLocally.value && !qrTarget.value)
const canConfirmLocally = computed(
  () => hasActiveAttempt.value && getPaymentMethodMeta(payment.value?.paymentMethod).confirmLocally
)
const busy = computed(() => initiating.value || confirming.value)
const mobileActionLabel = computed(() => {
  if (!canPay.value) return '返回订单'
  if (canConfirmLocally.value) return '确认支付'
  if (needsPaymentEntry.value) return '重新获取付款入口'
  if (hasActiveAttempt.value) return '刷新支付结果'
  return canRetry.value ? '重新发起支付' : '创建支付单'
})
const mobileActionHelper = computed(() => {
  if (!canPay.value) return `当前状态：${orderStatusMeta.value.label}`
  return attemptMeta.value.description
})
const mobileActionDisabled = computed(() => canPay.value && (busy.value || !selectedPaymentMethod.value))

const metrics = computed(() => [
  {
    label: '订单状态',
    value: orderStatusMeta.value.label,
    helper: orderStatusMeta.value.description
  },
  {
    label: '支付状态',
    value: isPaid.value ? '支付成功' : attemptMeta.value.label,
    helper: isPaid.value ? '付款已确认，请继续关注订单履约进度。' : attemptMeta.value.description
  },
  {
    label: '应付金额',
    value: formatCurrency(order.value?.payableAmount || 0),
    helper: '付款完成后，订单将进入对应的履约阶段。'
  }
])

function syncLatestPayment(detail) {
  payment.value = latestPayment(detail?.payments || [])
  gatewayResult.value = null
  qrImageUrl.value = ''
  if (payment.value?.paymentMethod) {
    selectedPaymentMethod.value = payment.value.paymentMethod
  }
}

async function syncGatewayResult(result) {
  gatewayResult.value = result
  qrImageUrl.value = ''
  if (!result?.qrCode) return
  try {
    qrImageUrl.value = await QRCode.toDataURL(result.qrCode, { width: 240, margin: 1 })
  } catch {
    // Keep the direct payment target available if local QR rendering fails.
  }
}

async function loadOrder({ quiet = false } = {}) {
  loading.value = !quiet
  loadError.value = ''
  try {
    const [orderResponse, gatewayResponse] = await Promise.all([
      getOrderDetail(route.params.orderId),
      getPaymentGateway()
    ])
    order.value = orderResponse.data
    gateway.value = gatewayResponse.data
    syncLatestPayment(order.value)
    if (!selectedPaymentMethod.value) {
      const defaultMethod = gateway.value?.defaultPaymentMethod
      selectedPaymentMethod.value =
        methods.value.find((method) => method.paymentMethod === defaultMethod)?.paymentMethod ||
        methods.value[0]?.paymentMethod ||
        ''
    }
  } catch (error) {
    loadError.value = error?.response?.data?.message || error?.message || '支付页面加载失败'
    if (!quiet) ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

async function handleInitiate() {
  if (busy.value || !canCreatePayment.value || !selectedPaymentMethod.value) return

  initiating.value = true
  try {
    const response = await initiatePayment(route.params.orderId, selectedPaymentMethod.value)
    payment.value = response.data.payment
    await syncGatewayResult(response.data.gateway)
    ElMessage.success('支付单已创建，请继续完成付款')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '发起支付失败')
  } finally {
    initiating.value = false
  }
}

async function handleResume() {
  if (busy.value || !needsPaymentEntry.value) return

  initiating.value = true
  try {
    const response = await resumePayment(payment.value.paymentNo)
    payment.value = response.data.payment
    await syncGatewayResult(response.data.gateway)
    ElMessage.success('付款入口已恢复，请继续完成付款')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '恢复付款入口失败')
  } finally {
    initiating.value = false
  }
}

async function handleConfirm() {
  if (busy.value || !canConfirmLocally.value) return

  confirming.value = true
  try {
    await completeMockPayment(payment.value.paymentNo)
    ElMessage.success('支付成功')
    router.replace('/app/orders')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '支付失败，请稍后重试')
  } finally {
    confirming.value = false
  }
}

async function handleRefresh() {
  await loadOrder({ quiet: true })
  if (isPaid.value) {
    ElMessage.success('支付成功')
  } else {
    ElMessage.info(attemptMeta.value.description)
  }
}

function handleMobilePrimary() {
  if (!canPay.value) {
    router.push('/app/orders')
    return
  }
  if (canConfirmLocally.value) {
    handleConfirm()
    return
  }
  if (hasActiveAttempt.value) {
    if (needsPaymentEntry.value) handleResume()
    else handleRefresh()
    return
  }
  handleInitiate()
}

onMounted(loadOrder)
</script>

<template>
  <div v-loading="loading">
    <TradePageShell
      eyebrow="Trade Center"
      title="支付确认"
      description="选择支付方式并完成付款。支付结果确认后，订单会进入对应的履约阶段。"
      current-key="payment"
    >
      <template #actions>
        <el-button plain @click="router.push('/app/orders')">返回订单</el-button>
        <el-button
          v-if="canConfirmLocally"
          type="primary"
          :loading="confirming"
          :disabled="busy"
          @click="handleConfirm"
        >
          确认支付
        </el-button>
        <el-button
          v-else-if="hasActiveAttempt"
          type="primary"
          :loading="needsPaymentEntry ? initiating : loading"
          :disabled="busy"
          @click="needsPaymentEntry ? handleResume() : handleRefresh()"
        >
          {{ needsPaymentEntry ? '重新获取付款入口' : '刷新支付结果' }}
        </el-button>
        <el-button
          v-else-if="canPay"
          type="primary"
          :loading="initiating"
          :disabled="busy || !selectedPaymentMethod"
          @click="handleInitiate"
        >
          {{ canRetry ? '重新发起支付' : '创建支付单' }}
        </el-button>
      </template>

      <template #metrics>
        <TradeMetricStrip :items="metrics" />
      </template>

      <ErrorBlock v-if="loadError" :message="loadError" @retry="loadOrder" />

      <EmptyState
        v-else-if="!order && !loading"
        emoji="￥"
        title="没有可支付的订单"
        description="订单可能已经失效或关闭，请返回订单列表确认当前交易状态。"
      >
        <el-button type="primary" @click="router.push('/app/orders')">返回订单列表</el-button>
      </EmptyState>

      <template v-else-if="order">
        <section class="shell-card payment-stage-card">
          <div class="payment-stage-card__copy">
            <h2>{{ isPaid ? '付款已确认' : attemptMeta.label }}</h2>
            <p>{{ isPaid ? '订单已进入履约阶段，请返回订单继续查看进度。' : attemptMeta.description }}</p>
          </div>
          <div class="payment-stage-card__tags">
            <TradeStatusTag kind="order" :value="order.orderStatus" />
            <TradeStatusTag kind="payment" :value="order.paymentStatus" />
          </div>
        </section>

        <section class="payment-grid">
          <article class="shell-card payment-card">
            <h2>订单信息</h2>
            <div class="payment-detail-list">
              <div class="payment-detail-list__row">
                <span>订单号</span>
                <strong>{{ order.orderNo }}</strong>
              </div>
              <div class="payment-detail-list__row">
                <span>履约方式</span>
                <strong>{{ fulfillmentMeta.label }}</strong>
              </div>
              <div class="payment-detail-list__row">
                <span>订单状态</span>
                <strong>{{ orderStatusMeta.label }}</strong>
              </div>
              <div class="payment-detail-list__row">
                <span>支付状态</span>
                <strong>{{ paymentStatusMeta.label }}</strong>
              </div>
              <div class="payment-detail-list__row">
                <span>应付金额</span>
                <strong class="payment-card__amount">{{ formatCurrency(order.payableAmount) }}</strong>
              </div>
            </div>
          </article>

          <article class="shell-card payment-card">
            <h2>选择支付方式</h2>
            <p class="payment-card__lead">请选择一种可用的支付方式完成本次付款。</p>
            <el-radio-group
              v-model="selectedPaymentMethod"
              class="payment-method-list"
              :disabled="hasActiveAttempt || busy || !canPay"
            >
              <el-radio
                v-for="method in methods"
                :key="method.paymentMethod"
                :value="method.paymentMethod"
                border
                class="payment-method-list__item"
              >
                <strong>{{ getPaymentMethodMeta(method.paymentMethod).label }}</strong>
                <span>{{ getPaymentMethodMeta(method.paymentMethod).description }}</span>
              </el-radio>
            </el-radio-group>

            <p v-if="!methods.length" class="payment-card__notice payment-card__notice--warning">
              暂无可用支付方式，请稍后重试。
            </p>

            <div v-if="payment" class="payment-detail-list">
              <div class="payment-detail-list__row">
                <span>当前方式</span>
                <strong>{{ getPaymentMethodMeta(payment.paymentMethod).label }}</strong>
              </div>
              <div class="payment-detail-list__row">
                <span>支付单号</span>
                <strong>{{ payment.paymentNo }}</strong>
              </div>
              <div class="payment-detail-list__row">
                <span>付款进度</span>
                <strong>{{ attemptMeta.label }}</strong>
              </div>
            </div>

            <div class="shell-inline-actions payment-card__actions">
              <el-button
                v-if="canCreatePayment"
                type="primary"
                :loading="initiating"
                :disabled="busy || !selectedPaymentMethod"
                @click="handleInitiate"
              >
                {{ canRetry ? '重新发起支付' : '创建支付单' }}
              </el-button>
              <el-button
                v-if="canConfirmLocally"
                type="primary"
                :loading="confirming"
                :disabled="busy"
                @click="handleConfirm"
              >
                确认支付
              </el-button>
              <el-button
                v-if="hasActiveAttempt && !canConfirmLocally"
                plain
                :loading="needsPaymentEntry && initiating"
                :disabled="busy"
                @click="needsPaymentEntry ? handleResume() : handleRefresh()"
              >
                {{ needsPaymentEntry ? '重新获取付款入口' : '刷新支付结果' }}
              </el-button>
            </div>

            <p v-if="isPaid" class="payment-card__notice">
              当前订单已完成付款，请返回订单页继续关注履约进度。
            </p>
            <p v-else-if="!canPay" class="payment-card__notice payment-card__notice--warning">
              当前订单已经不处于待支付状态，不能重复发起支付。
            </p>
          </article>
        </section>

        <section v-if="qrTarget && hasActiveAttempt" class="shell-card payment-qr-card">
          <div>
            <span class="eyebrow">Continue To Pay</span>
            <h2>前往支付宝完成付款</h2>
            <p>请使用支付宝扫码付款，也可以在当前设备打开付款入口。完成后返回此页刷新支付结果。</p>
          </div>
          <div class="payment-qr-card__entry">
            <img v-if="qrImageUrl" :src="qrImageUrl" alt="支付宝付款二维码" />
            <a class="payment-qr-card__target" :href="qrTarget" target="_blank" rel="noreferrer">
              在当前设备打开
            </a>
          </div>
        </section>
      </template>

      <TradeMobileActionBar
        v-if="order && !loadError"
        eyebrow="应付金额"
        :value="formatCurrency(order.payableAmount)"
        :helper="mobileActionHelper"
        :action-label="mobileActionLabel"
        :loading="busy"
        :disabled="mobileActionDisabled"
        @primary="handleMobilePrimary"
      />
    </TradePageShell>
  </div>
</template>

<style scoped>
.payment-stage-card,
.payment-stage-card__tags,
.payment-qr-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.payment-stage-card__copy,
.payment-card,
.payment-detail-list,
.payment-method-list,
.payment-qr-card div {
  display: grid;
  gap: 14px;
}

.payment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.payment-detail-list__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.payment-detail-list__row span,
.payment-card__lead,
.payment-method-list__item span,
.payment-qr-card p {
  color: var(--cm-text-secondary);
  line-height: 1.6;
}

.payment-card__amount {
  color: var(--cm-price);
  font-size: 28px;
  line-height: 1.1;
}

.payment-method-list__item {
  width: 100%;
  height: auto;
  min-height: 72px;
  margin: 0;
  padding: 14px;
}

.payment-method-list__item :deep(.el-radio__label) {
  display: grid;
  gap: 4px;
  white-space: normal;
}

.payment-card__actions {
  margin-top: 4px;
}

.payment-card__notice {
  color: #0f7a57;
  font-weight: 600;
  line-height: 1.6;
}

.payment-card__notice--warning {
  color: #9a5b07;
}

.payment-qr-card__target {
  display: inline-flex;
  min-height: 46px;
  align-items: center;
  justify-content: center;
  padding: 0 18px;
  border-radius: 14px;
  background: var(--cm-primary);
  color: #fff;
  font-weight: 700;
}

.payment-qr-card__entry {
  justify-items: center;
}

.payment-qr-card__entry img {
  width: 168px;
  height: 168px;
  border-radius: 12px;
}

@media (max-width: 768px) {
  .payment-stage-card,
  .payment-stage-card__tags,
  .payment-detail-list__row,
  .payment-qr-card {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
