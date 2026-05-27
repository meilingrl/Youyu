<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail } from '@/api/modules/order'
import { completeMockPayment, getPaymentGateway, initiatePayment } from '@/api/modules/payment'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import TradeMobileActionBar from '@/components/trade/TradeMobileActionBar.vue'
import TradeMetricStrip from '@/components/trade/TradeMetricStrip.vue'
import TradePageShell from '@/components/trade/TradePageShell.vue'
import TradeStatusTag from '@/components/trade/TradeStatusTag.vue'
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
const gateway = ref(null)

const orderStatusMeta = computed(() => getOrderStatusMeta(order.value?.orderStatus))
const paymentStatusMeta = computed(() => getPaymentStatusMeta(order.value?.paymentStatus))
const fulfillmentMeta = computed(() => getFulfillmentTypeMeta(order.value?.fulfillmentType))
const isPaid = computed(() => order.value?.paymentStatus === 'paid')
const canPay = computed(() => order.value?.orderStatus === 'pending_payment')
const mobileActionLabel = computed(() => (canPay.value ? '模拟支付成功' : '返回订单'))
const mobileActionHelper = computed(() =>
  canPay.value ? `支付状态：${paymentStatusMeta.value.label}` : `当前状态：${orderStatusMeta.value.label}`
)
const mobileActionDisabled = computed(() => canPay.value && (initiating.value || confirming.value))

const metrics = computed(() => [
  {
    label: '订单状态',
    value: orderStatusMeta.value.label,
    helper: orderStatusMeta.value.description
  },
  {
    label: '支付状态',
    value: paymentStatusMeta.value.label,
    helper: canPay.value ? '当前仍可继续支付。' : '若已支付，请回到订单页跟进后续动作。'
  },
  {
    label: '应付金额',
    value: formatCurrency(order.value?.payableAmount || 0),
    helper: '支付成功后订单会流转到已支付 / 待履约。'
  }
])

async function loadOrder() {
  loading.value = true
  loadError.value = ''
  try {
    const [orderResponse, gatewayResponse] = await Promise.all([
      getOrderDetail(route.params.orderId),
      getPaymentGateway()
    ])
    order.value = orderResponse.data
    gateway.value = gatewayResponse.data
  } catch (error) {
    loadError.value = error?.response?.data?.message || error?.message || '支付页加载失败'
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

async function handleInitiate() {
  if (initiating.value || confirming.value || !canPay.value) return

  initiating.value = true
  try {
    const response = await initiatePayment(route.params.orderId)
    payment.value = response.data.payment
    ElMessage.success('已创建支付单，可继续完成支付')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '发起支付失败')
  } finally {
    initiating.value = false
  }
}

async function handleSuccess() {
  if (initiating.value || confirming.value || !canPay.value) return

  confirming.value = true
  try {
    if (!payment.value?.paymentNo) {
      const response = await initiatePayment(route.params.orderId)
      payment.value = response.data.payment
    }
    await completeMockPayment(payment.value.paymentNo)
    ElMessage.success('模拟支付成功')
    router.replace('/app/orders')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '支付失败')
  } finally {
    confirming.value = false
  }
}

function handleMobilePrimary() {
  if (canPay.value) {
    handleSuccess()
    return
  }

  router.push('/app/orders')
}

onMounted(loadOrder)
</script>

<template>
  <div v-loading="loading">
    <TradePageShell
      eyebrow="Trade Center"
      title="支付确认"
      description="这一步只处理支付，不隐藏订单状态、非法状态或后续去向。支付完成后，请回到订单中心继续跟进履约、售后和评价。"
      current-key="payment"
    >
      <template #actions>
        <el-button plain @click="router.push('/app/orders')">返回订单</el-button>
        <el-button
          type="primary"
          :loading="confirming"
          :disabled="!canPay || initiating || confirming"
          @click="handleSuccess"
        >
          模拟支付成功
        </el-button>
      </template>

      <template #metrics>
        <TradeMetricStrip :items="metrics" />
      </template>

      <ErrorBlock v-if="loadError" :message="loadError" @retry="loadOrder" />

      <EmptyState
        v-else-if="!order && !loading"
        emoji="💳"
        title="没有可支付的订单"
        description="订单可能已失效、已关闭，或你是从旧链接进入。请回到订单列表重新确认当前交易状态。"
      >
        <el-button type="primary" @click="router.push('/app/orders')">回到订单列表</el-button>
      </EmptyState>

      <template v-else-if="order">
        <section class="shell-card payment-stage-card">
          <div class="payment-stage-card__copy">
            <h2>支付不会跳过关键状态</h2>
            <p>成功支付后，订单会从“待支付”进入“已支付 / 待履约”。如果状态已经变化，这里会明确告诉你，而不是继续允许错误操作。</p>
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
                <TradeStatusTag kind="order" :value="order.orderStatus" />
              </div>
              <div class="payment-detail-list__row">
                <span>支付状态</span>
                <TradeStatusTag kind="payment" :value="order.paymentStatus" />
              </div>
              <div class="payment-detail-list__row">
                <span>应付金额</span>
                <strong class="payment-card__amount">{{ formatCurrency(order.payableAmount) }}</strong>
              </div>
            </div>
          </article>

          <article class="shell-card payment-card">
            <h2>支付网关</h2>
            <p class="payment-card__lead">{{ gateway?.message }}</p>
            <div class="payment-detail-list">
              <div class="payment-detail-list__row">
                <span>默认网关</span>
                <strong>{{ gateway?.defaultGateway || 'mock' }}</strong>
              </div>
              <div class="payment-detail-list__row">
                <span>支付单号</span>
                <strong>{{ payment?.paymentNo || '尚未创建' }}</strong>
              </div>
            </div>

            <div class="shell-inline-actions payment-card__actions">
              <el-button
                plain
                :loading="initiating"
                :disabled="!canPay || initiating || confirming"
                @click="handleInitiate"
              >
                创建支付单
              </el-button>
              <el-button
                type="primary"
                :loading="confirming"
                :disabled="!canPay || initiating || confirming"
                @click="handleSuccess"
              >
                模拟支付成功
              </el-button>
            </div>

            <p v-if="isPaid" class="payment-card__notice">
              当前订单已完成支付，请回到订单页继续关注发货、收货、退款或评价。
            </p>
            <p v-else-if="!canPay" class="payment-card__notice payment-card__notice--warning">
              当前订单已经不处于待支付状态，不能重复发起支付。
            </p>
          </article>
        </section>
      </template>

      <TradeMobileActionBar
        v-if="order && !loadError"
        eyebrow="应付金额"
        :value="formatCurrency(order.payableAmount)"
        :helper="mobileActionHelper"
        :action-label="mobileActionLabel"
        :loading="canPay && confirming"
        :disabled="mobileActionDisabled"
        @primary="handleMobilePrimary"
      />
    </TradePageShell>
  </div>
</template>

<style scoped>
.payment-stage-card,
.payment-stage-card__tags {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.payment-stage-card__copy {
  display: grid;
  gap: 8px;
}

.payment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.payment-card,
.payment-detail-list {
  display: grid;
  gap: 14px;
}

.payment-detail-list__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.payment-detail-list__row span,
.payment-card__lead {
  color: var(--cm-text-secondary);
  line-height: 1.6;
}

.payment-card__amount {
  color: var(--cm-price);
  font-size: 28px;
  line-height: 1.1;
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

@media (max-width: 768px) {
  .payment-stage-card,
  .payment-stage-card__tags,
  .payment-detail-list__row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
