<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-services'
import { useRoute, useRouter } from 'vue-router'
import {
  accessDigitalAsset,
  applyRefund,
  buyerConfirmOffline,
  cancelOrder,
  confirmReceipt,
  getOrderDetail
} from '@/api/modules/order'
import { submitReport } from '@/api/modules/report'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
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

const detailLoading = ref(false)
const detailError = ref('')
const detail = ref(null)
const actionLoading = ref(false)
const refundSection = ref(null)

const refundForm = reactive({
  refundReason: ''
})

const reportDialogVisible = ref(false)
const reportForm = reactive({
  reason: '',
  content: ''
})

const orderId = computed(() => String(route.params.orderId || ''))
const lineItems = computed(() => detail.value?.items || detail.value?.orderItems || [])
const detailOrderMeta = computed(() => getOrderStatusMeta(detail.value?.orderStatus))
const detailPaymentMeta = computed(() => getPaymentStatusMeta(detail.value?.paymentStatus))
const detailFulfillmentMeta = computed(() => getFulfillmentTypeMeta(detail.value?.fulfillmentType))
const availableActions = computed(() => detail.value?.availableActions || [])
const hasPrimaryActions = computed(() =>
  ['pay', 'cancel', 'confirm_receipt', 'offline_buyer_confirm', 'apply_refund'].some((action) =>
    availableActions.value.includes(action)
  )
)

function hasAction(action) {
  return availableActions.value.includes(action)
}

async function loadDetail() {
  if (!orderId.value) {
    detailError.value = '订单编号缺失'
    return
  }

  detailLoading.value = true
  detailError.value = ''
  try {
    const response = await getOrderDetail(orderId.value)
    detail.value = response.data
  } catch (error) {
    detail.value = null
    detailError.value = error?.response?.data?.message || error?.message || '订单详情加载失败'
    ElMessage.error(detailError.value)
  } finally {
    detailLoading.value = false
  }
}

function goBack() {
  router.push('/app/orders')
}

function goPay() {
  if (!detail.value) return
  router.push(`/app/payments/${detail.value.id}`)
}

async function handleCancel() {
  if (actionLoading.value || !detail.value) return
  try {
    await ElMessageBox.confirm('确认取消这个待支付订单吗？', '取消订单')
    actionLoading.value = true
    await cancelOrder(detail.value.id)
    ElMessage.success('订单已取消')
    await loadDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '取消订单失败')
    }
  } finally {
    actionLoading.value = false
  }
}

async function handleConfirm(offline = false) {
  if (actionLoading.value || !detail.value) return
  actionLoading.value = true
  try {
    if (offline) {
      await buyerConfirmOffline(detail.value.id)
    } else {
      await confirmReceipt(detail.value.id)
    }
    ElMessage.success('已确认收货')
    await loadDetail()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '确认收货失败')
  } finally {
    actionLoading.value = false
  }
}

async function handleRefund() {
  if (actionLoading.value || !detail.value) return
  if (!refundForm.refundReason.trim()) {
    ElMessage.warning('请填写退款原因')
    await focusRefundReason()
    return
  }

  actionLoading.value = true
  try {
    await applyRefund(detail.value.id, { refundReason: refundForm.refundReason.trim() })
    ElMessage.success('退款申请已提交')
    refundForm.refundReason = ''
    await loadDetail()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '退款申请失败')
  } finally {
    actionLoading.value = false
  }
}

async function focusRefundReason() {
  await nextTick()
  refundSection.value?.scrollIntoView?.({ behavior: 'smooth', block: 'center' })
}

async function handleAccessAsset(assetId) {
  if (actionLoading.value || !detail.value) return
  actionLoading.value = true
  try {
    const response = await accessDigitalAsset(detail.value.id, assetId)
    const assetUrl = response.data?.asset?.assetUrl || response.data?.asset?.asset_url
    ElMessage.success(assetUrl ? `已记录资源访问：${assetUrl}` : '已记录资源访问')
    detail.value.digitalAccessLogs = response.data?.accessLogs || []
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '资源访问失败')
  } finally {
    actionLoading.value = false
  }
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

function openOrderMessage(intent = 'after_sales') {
  if (intent === 'support') {
    router.push({
      path: '/app/support',
      query: {
        category: 'order',
        relatedType: 'order',
        relatedId: String(detail.value?.id || '')
      }
    })
    return
  }

  router.push({
    path: '/app/messages',
    query: {
      category: 'trade',
      entry: 'order',
      entryId: String(detail.value?.id || ''),
      targetType: 'shop',
      targetId: String(detail.value?.shopId || detail.value?.sellerId || ''),
      intent
    }
  })
}

watch(orderId, loadDetail, { immediate: true })
</script>

<template>
  <TradePageShell
    eyebrow="Order Detail"
    title="订单详情"
    description="查看订单进度、履约信息、数字资源、退款与平台介入入口。"
    current-key="orders"
  >
    <template #actions>
      <el-button plain @click="goBack">返回订单列表</el-button>
      <el-button :disabled="detailLoading" @click="loadDetail">刷新</el-button>
    </template>

    <div v-if="detailLoading" class="detail-loading shell-card">
      <el-skeleton :rows="10" animated />
    </div>

    <ErrorBlock
      v-else-if="detailError"
      :message="detailError"
      @retry="loadDetail"
    />

    <div v-else-if="detail" class="order-detail-stack">
      <section class="detail-panel detail-panel--hero">
        <div class="detail-panel__hero-copy">
          <p class="detail-panel__eyebrow">{{ detail.orderNo }}</p>
          <h2>{{ detailOrderMeta.label }}</h2>
          <p>{{ detailOrderMeta.description }}</p>
        </div>
        <div class="detail-panel__hero-meta">
          <TradeStatusTag kind="order" :value="detail.orderStatus" />
          <TradeStatusTag kind="payment" :value="detail.paymentStatus" />
          <TradeStatusTag kind="fulfillment" :value="detail.fulfillmentType" />
          <strong>{{ formatCurrency(detail.payableAmount) }}</strong>
        </div>
      </section>

      <section class="detail-panel">
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

      <section class="detail-panel">
        <h3>商品快照</h3>
        <article v-for="item in lineItems" :key="item.id" class="line-item">
          <div class="line-item__copy">
            <strong>{{ item.productTitleSnapshot }}</strong>
            <span>x{{ item.quantity }}</span>
          </div>
          <span>{{ formatCurrency(item.subtotalAmount) }}</span>
        </article>
      </section>

      <section class="detail-panel">
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

      <section v-if="detail.digitalAssets?.length" class="detail-panel">
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
              @click="handleAccessAsset(asset.id)"
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

      <section v-if="detail.payments?.length" class="detail-panel">
        <h3>支付记录</h3>
        <article v-for="paymentItem in detail.payments" :key="paymentItem.id" class="line-item">
          <div class="line-item__copy">
            <strong>{{ paymentItem.paymentNo }}</strong>
            <span>{{ getPaymentStatusMeta(paymentItem.paymentStatus).label }}</span>
          </div>
          <span>{{ formatCurrency(paymentItem.payableAmount || paymentItem.amount) }}</span>
        </article>
      </section>

      <section ref="refundSection" class="detail-panel">
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

        <div v-if="hasAction('apply_refund')" class="refund-box">
          <el-input v-model="refundForm.refundReason" placeholder="填写退款原因" />
          <el-button
            type="warning"
            :loading="actionLoading"
            :disabled="actionLoading"
            @click="handleRefund"
          >
            申请退款
          </el-button>
        </div>
      </section>

      <section class="detail-panel">
        <h3>举报与平台介入</h3>
        <p class="section-copy">如果遇到履约异常、商品与描述不符或其他交易纠纷，可以在这里提交举报，不需要跳到交易域外处理。</p>
        <div class="shell-inline-actions">
          <el-button type="danger" plain :disabled="actionLoading" @click="openReportDialog">
            提交举报
          </el-button>
          <el-button plain :disabled="actionLoading" @click="openOrderMessage()">
            进入售后消息入口
          </el-button>
          <el-button plain :disabled="actionLoading" @click="openOrderMessage('support')">
            联系平台客服
          </el-button>
        </div>
      </section>

      <div v-if="hasPrimaryActions" class="detail-action-bar">
        <el-button v-if="hasAction('pay')" type="primary" @click="goPay">
          去支付
        </el-button>
        <el-button
          v-if="hasAction('cancel')"
          plain
          :loading="actionLoading"
          :disabled="actionLoading"
          @click="handleCancel"
        >
          取消订单
        </el-button>
        <el-button
          v-if="hasAction('confirm_receipt')"
          type="success"
          :loading="actionLoading"
          :disabled="actionLoading"
          @click="handleConfirm()"
        >
          确认收货
        </el-button>
        <el-button
          v-if="hasAction('offline_buyer_confirm')"
          type="success"
          :loading="actionLoading"
          :disabled="actionLoading"
          @click="handleConfirm(true)"
        >
          线下确认收货
        </el-button>
        <el-button
          v-if="hasAction('apply_refund')"
          type="warning"
          :loading="actionLoading"
          :disabled="actionLoading"
          @click="handleRefund"
        >
          申请退款
        </el-button>
      </div>
    </div>

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
  </TradePageShell>
</template>

<style scoped>
.detail-loading,
.order-detail-stack {
  display: grid;
  gap: 16px;
}

.detail-panel {
  border: 1px solid rgba(50, 91, 63, 0.12);
  border-radius: 8px;
  padding: 18px;
  background: rgba(255, 255, 255, 0.72);
}

.detail-panel--hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.detail-panel__hero-copy {
  display: grid;
  gap: 8px;
}

.detail-panel__eyebrow {
  color: var(--cm-text-tertiary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

.detail-panel__hero-meta,
.shell-inline-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.detail-panel__hero-meta strong {
  color: var(--cm-price);
  font-size: 28px;
  line-height: 1.1;
}

.detail-panel h3,
.access-log h4 {
  margin: 0 0 14px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.detail-kv,
.line-item__copy {
  display: grid;
  gap: 6px;
}

.detail-kv span,
.refund-rule,
.section-copy,
.text-muted,
.log-item {
  color: var(--cm-text-secondary);
  font-size: 13px;
  line-height: 1.55;
}

.line-item,
.refund-box {
  display: flex;
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

.detail-action-bar {
  position: sticky;
  bottom: 12px;
  z-index: 2;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
  border: 1px solid rgba(50, 91, 63, 0.14);
  border-radius: 8px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 42px rgba(88, 62, 43, 0.12);
}

@media (max-width: 768px) {
  .detail-panel--hero,
  .line-item,
  .refund-box,
  .log-item {
    flex-direction: column;
    align-items: stretch;
  }

  .detail-panel__hero-meta {
    align-items: flex-start;
  }

  .detail-action-bar,
  .shell-inline-actions {
    align-items: stretch;
  }

  .detail-action-bar :deep(.el-button),
  .shell-inline-actions :deep(.el-button),
  .refund-box :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }
}
</style>
