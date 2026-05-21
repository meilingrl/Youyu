<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import ListPageShell from '@/components/shell/ListPageShell.vue'
import {
  completeRefund,
  getAdminOrderDetail,
  getAdminOrderList,
  sellerConfirmOffline,
  shipOrder
} from '@/api/modules/order'
import { resolveErrorMessage } from '@/utils/error-utils'

const loading = ref(false)
const actionLoading = ref(false)
const error = ref('')
const rows = ref([])
const detailVisible = ref(false)
const detail = ref(null)
const shipForm = reactive({
  logisticsCompany: '',
  trackingNo: ''
})

const windowWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1024)
function onResize() {
  windowWidth.value = window.innerWidth
}
const drawerSize = computed(() => (windowWidth.value < 768 ? '100%' : '720px'))

async function loadOrders() {
  loading.value = true
  error.value = ''

  try {
    const response = await getAdminOrderList()
    rows.value = response.data || []
  } catch (err) {
    error.value = resolveErrorMessage(err)
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

async function openDetail(orderId) {
  try {
    const response = await getAdminOrderDetail(orderId)
    detail.value = response.data
    detailVisible.value = true
  } catch (err) {
    ElMessage.error(resolveErrorMessage(err))
  }
}

async function handleShip() {
  if (actionLoading.value) return
  actionLoading.value = true
  try {
    await shipOrder(detail.value.id, shipForm)
    ElMessage.success('已记录发货信息')
    shipForm.logisticsCompany = ''
    shipForm.trackingNo = ''
    await openDetail(detail.value.id)
    await loadOrders()
  } catch (err) {
    ElMessage.error(resolveErrorMessage(err))
  } finally {
    actionLoading.value = false
  }
}

async function handleOfflineConfirm() {
  if (actionLoading.value) return
  actionLoading.value = true
  try {
    await sellerConfirmOffline(detail.value.id)
    ElMessage.success('已记录卖家线下确认')
    await openDetail(detail.value.id)
    await loadOrders()
  } catch (err) {
    ElMessage.error(resolveErrorMessage(err))
  } finally {
    actionLoading.value = false
  }
}

async function handleCompleteRefund(refundId) {
  if (actionLoading.value) return
  actionLoading.value = true
  try {
    await completeRefund(detail.value.id, refundId)
    ElMessage.success('退款已完成')
    await openDetail(detail.value.id)
    await loadOrders()
  } catch (err) {
    ElMessage.error(resolveErrorMessage(err))
  } finally {
    actionLoading.value = false
  }
}

onMounted(() => {
  loadOrders()
  window.addEventListener('resize', onResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
})
</script>

<template>
  <ListPageShell
    title="订单管理"
    description="后台只做订单查看和基础流转操作，不扩真实支付，也不伪造线下物流过程。"
    :rows="rows"
    :loading="loading"
    :error="error"
    empty-title="暂无订单记录"
    empty-description="当前没有订单数据。"
    @retry="loadOrders"
  >
    <template #summary>
      <div class="filter-row filter-row--summary">
        <el-tag>订单总数 {{ rows.length }}</el-tag>
      </div>
    </template>

    <template #filters>
      <div class="filter-row">
        <el-button type="primary" :loading="loading" @click="loadOrders">查询</el-button>
      </div>
    </template>

    <template #table>
      <el-table v-loading="loading" :data="rows">
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="productTitle" label="商品" min-width="200" />
        <el-table-column prop="orderStatus" label="订单状态" min-width="100" />
        <el-table-column prop="paymentStatus" label="支付状态" min-width="100" />
        <el-table-column prop="fulfillmentType" label="履约方式" min-width="100" />
        <el-table-column label="金额" min-width="100">
          <template #default="{ row }">
            <span class="admin-order-card__amount">￥{{ Number(row.payableAmount).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </ListPageShell>

  <el-drawer v-model="detailVisible" :size="drawerSize" title="后台订单详情">
    <div v-if="detail" class="drawer-stack">
      <section class="drawer-panel">
        <h3>订单总览</h3>
        <p>订单号：{{ detail.orderNo }}</p>
        <p>主状态：{{ detail.orderStatus }}</p>
        <p>支付状态：{{ detail.paymentStatus }}</p>
        <p>履约方式：{{ detail.fulfillmentType }}</p>
        <p v-if="detail.buyerNote">买家备注：{{ detail.buyerNote }}</p>
        <p>应付金额：<strong>￥{{ Number(detail.payableAmount).toFixed(2) }}</strong></p>
      </section>

      <section class="drawer-panel">
        <h3>履约信息</h3>
        <p>履约状态：{{ detail.fulfillment.fulfillmentStatus }}</p>
        <p v-if="detail.fulfillment.offlineMeetTime">
          约定时间：{{ detail.fulfillment.offlineMeetTime }}
        </p>
        <p v-if="detail.fulfillment.offlineMeetLocation">
          约定地点：{{ detail.fulfillment.offlineMeetLocation }}
        </p>
        <p v-if="detail.fulfillment.logisticsCompany">
          物流：{{ detail.fulfillment.logisticsCompany }} / {{ detail.fulfillment.trackingNo }}
        </p>
      </section>

      <section v-if="detail.payments?.length" class="drawer-panel">
        <h3>支付记录</h3>
        <article v-for="payment in detail.payments" :key="payment.id" class="line-item">
          <strong>{{ payment.paymentNo }}</strong>
          <span>{{ payment.paymentStatus }}</span>
          <span>￥{{ Number(payment.payableAmount || payment.amount).toFixed(2) }}</span>
        </article>
      </section>

      <section v-if="detail.availableActions.includes('ship')" class="drawer-panel">
        <h3>记录快递发货</h3>
        <div class="refund-box">
          <el-input v-model="shipForm.logisticsCompany" placeholder="物流公司" />
          <el-input v-model="shipForm.trackingNo" placeholder="物流单号" />
          <el-button type="primary" :loading="actionLoading" :disabled="actionLoading" @click="handleShip">提交发货</el-button>
        </div>
      </section>

      <section v-if="detail.availableActions.includes('offline_seller_confirm')" class="drawer-panel">
        <h3>线下交付确认</h3>
        <p>这里只记录约定与双方确认结果，不做伪物流过程。</p>
        <el-button type="primary" :loading="actionLoading" :disabled="actionLoading" @click="handleOfflineConfirm">记录卖家已交付</el-button>
      </section>

      <section class="drawer-panel">
        <h3>退款记录</h3>
        <article v-for="refund in detail.refunds" :key="refund.id" class="line-item">
          <strong>{{ refund.refundNo }}</strong>
          <span>{{ refund.refundStatus }}</span>
          <span>￥{{ Number(refund.refundAmount).toFixed(2) }}</span>
          <el-button
            v-if="detail.availableActions.includes('complete_refund') && refund.refundStatus !== 'completed'"
            type="warning"
            plain
            :loading="actionLoading"
            :disabled="actionLoading"
            @click="handleCompleteRefund(refund.id)"
          >
            完成退款
          </el-button>
        </article>
        <p v-if="!detail.refunds.length">暂无退款记录</p>
      </section>
    </div>
  </el-drawer>
</template>

<style scoped>
.drawer-stack {
  display: grid;
  gap: 16px;
}

.line-item,
.refund-box {
  display: flex;
}

.line-item {
  justify-content: space-between;
  gap: 16px;
}

.admin-order-card__amount {
  color: var(--cm-price);
  font-size: 24px;
  font-weight: 700;
}

.drawer-panel {
  border: 1px solid rgba(50, 91, 63, 0.12);
  border-radius: 14px;
  padding: 16px;
}

.refund-box {
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .line-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .refund-box {
    flex-direction: column;
  }
}
</style>
