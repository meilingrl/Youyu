<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import { useRoute, useRouter } from 'vue-router'
import { createOrder, previewOrder } from '@/api/modules/order'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import TradeMetricStrip from '@/components/trade/TradeMetricStrip.vue'
import TradePageShell from '@/components/trade/TradePageShell.vue'
import TradeStatusTag from '@/components/trade/TradeStatusTag.vue'
import { formatCurrency, getFulfillmentTypeMeta } from '@/components/trade/trade-meta'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const loadError = ref('')
const preview = ref(null)
const form = reactive({
  fulfillmentType: '',
  addressId: null,
  offlineMeetTime: '',
  offlineMeetLocation: '',
  buyerNote: ''
})

const selectedIds = computed(() =>
  String(route.query.ids || '')
    .split(',')
    .map((item) => Number(item))
    .filter(Boolean)
)

const metrics = computed(() => [
  {
    label: '结算商品',
    value: String(preview.value?.items?.length || selectedIds.value.length || 0),
    helper: '这一步只确认订单信息，不会直接扣款。'
  },
  {
    label: '履约方式',
    value: getFulfillmentTypeMeta(form.fulfillmentType || preview.value?.selectedFulfillmentType).label,
    helper: preview.value?.requiresOfflineAppointment ? '需要补齐线下时间和地点。' : '根据商品类型自动限制可选方式。'
  },
  {
    label: '应付金额',
    value: formatCurrency(preview.value?.payableAmount || 0),
    helper: '提交订单后进入支付确认页。'
  }
])

watch(
  () => form.fulfillmentType,
  async (value, previous) => {
    if (!value || value === previous || !selectedIds.value.length) {
      return
    }
    await loadPreview(value)
  }
)

async function loadPreview(fulfillmentType = '') {
  if (!selectedIds.value.length) {
    ElMessage.warning('未选择购物车商品')
    router.replace('/app/cart')
    return
  }

  loading.value = true
  loadError.value = ''
  try {
    const response = await previewOrder({
      cartItemIds: selectedIds.value,
      fulfillmentType
    })
    preview.value = response.data
    if (!form.fulfillmentType) {
      form.fulfillmentType = preview.value.selectedFulfillmentType
    }
    if (preview.value.addressOptions?.length && !form.addressId) {
      const defaultAddress = preview.value.addressOptions.find((item) => item.isDefault)
      form.addressId = defaultAddress?.id || preview.value.addressOptions[0]?.id || null
    }
  } catch (error) {
    loadError.value = error?.response?.data?.message || error?.message || '结算预览加载失败'
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

function validateOfflineTime(time) {
  if (!time) return false
  return /^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}$/.test(time.trim())
}

async function submitOrder() {
  if (submitting.value || !preview.value) return

  if (preview.value.requiresOfflineAppointment) {
    if (!form.offlineMeetTime.trim()) {
      ElMessage.warning('请填写线下约定时间')
      return
    }
    if (!validateOfflineTime(form.offlineMeetTime)) {
      ElMessage.warning('线下约定时间格式应为 yyyy-MM-dd HH:mm')
      return
    }
    if (!form.offlineMeetLocation.trim()) {
      ElMessage.warning('请填写线下约定地点')
      return
    }
  }

  if (preview.value.requiresAddress && !form.addressId) {
    ElMessage.warning('请选择收货地址')
    return
  }

  submitting.value = true
  try {
    const response = await createOrder({
      cartItemIds: selectedIds.value,
      fulfillmentType: form.fulfillmentType,
      addressId: form.addressId,
      offlineMeetTime: form.offlineMeetTime,
      offlineMeetLocation: form.offlineMeetLocation,
      buyerNote: form.buyerNote
    })
    ElMessage.success('订单创建成功')
    router.replace(`/app/payments/${response.data.id}`)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '订单创建失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => loadPreview())
</script>

<template>
  <div v-loading="loading">
    <TradePageShell
      eyebrow="Trade Center"
      title="结算确认"
      description="这里负责补齐履约信息，确保支付前的地址、线下交付或数字交付规则都足够明确。"
      current-key="checkout"
    >
      <template #actions>
        <el-button plain @click="$router.push('/app/cart')">返回购物车</el-button>
        <el-button type="primary" :loading="submitting" :disabled="submitting || !preview" @click="submitOrder">
          提交订单
        </el-button>
      </template>

      <template #metrics>
        <TradeMetricStrip :items="metrics" />
      </template>

      <ErrorBlock v-if="loadError" :message="loadError" @retry="loadPreview()" />

      <EmptyState
        v-else-if="!preview && !loading"
        emoji="🧾"
        title="没有可结算的商品"
        description="请先回到购物车勾选商品，再进入结算。"
      >
        <el-button type="primary" @click="$router.push('/app/cart')">返回购物车</el-button>
      </EmptyState>

      <template v-else-if="preview">
        <section class="checkout-lead shell-card">
          <div class="checkout-lead__copy">
            <h2>这一步只确认订单信息</h2>
            <p>提交后会进入支付页。所有非法状态、地址缺失和线下约定不完整都会在这里先拦住。</p>
          </div>
          <TradeStatusTag kind="fulfillment" :value="form.fulfillmentType" />
        </section>

        <section class="checkout-grid">
          <article class="shell-card checkout-block">
            <h2>订单商品</h2>
            <div class="checkout-items">
              <div v-for="item in preview.items" :key="item.cartItemId" class="checkout-item">
                <div class="checkout-item__copy">
                  <strong>{{ item.title }}</strong>
                  <p>{{ item.productType === 'digital' ? '数字交付' : '实物商品' }}</p>
                </div>
                <span>x{{ item.quantity }}</span>
                <span>{{ formatCurrency(item.subtotal) }}</span>
              </div>
            </div>
          </article>

          <article class="shell-card checkout-block">
            <h2>履约信息</h2>
            <el-form label-position="top">
              <el-form-item label="履约方式">
                <el-radio-group v-model="form.fulfillmentType" class="checkout-radio-group">
                  <el-radio-button
                    v-for="item in preview.allowedFulfillmentTypes"
                    :key="item"
                    :label="item"
                  >
                    {{ getFulfillmentTypeMeta(item).label }}
                  </el-radio-button>
                </el-radio-group>
              </el-form-item>

              <el-form-item v-if="preview.requiresAddress" label="收货地址">
                <el-select v-model="form.addressId" placeholder="请选择地址">
                  <el-option
                    v-for="address in preview.addressOptions"
                    :key="address.id"
                    :label="`${address.campusName} / ${address.detailAddress}`"
                    :value="address.id"
                  />
                </el-select>
              </el-form-item>

              <template v-if="preview.requiresOfflineAppointment">
                <el-form-item label="线下约定时间">
                  <el-input v-model="form.offlineMeetTime" placeholder="例如 2026-05-17 18:30" />
                </el-form-item>
                <el-form-item label="线下约定地点">
                  <el-input v-model="form.offlineMeetLocation" placeholder="例如 图书馆门口" />
                </el-form-item>
              </template>

              <el-form-item label="买家备注">
                <el-input
                  v-model="form.buyerNote"
                  :rows="3"
                  type="textarea"
                  placeholder="可选：补充收货或交付说明"
                />
              </el-form-item>
            </el-form>

            <div v-if="preview.digitalRuleText" class="checkout-tip">
              <strong>数字交付提醒</strong>
              <p>{{ preview.digitalRuleText }}</p>
            </div>
          </article>
        </section>

        <section class="shell-card checkout-summary">
          <div class="checkout-summary__copy">
            <h2>下一步：支付确认</h2>
            <p>支付前仍可回看订单信息。提交订单只会生成一条待支付订单，不会跳过关键交易状态。</p>
          </div>
          <div class="checkout-summary__meta">
            <div>
              <span>商品金额</span>
              <strong>{{ formatCurrency(preview.productAmount) }}</strong>
            </div>
            <div>
              <span>实付金额</span>
              <strong>{{ formatCurrency(preview.payableAmount) }}</strong>
            </div>
            <el-button type="primary" :loading="submitting" :disabled="submitting" @click="submitOrder">
              提交订单
            </el-button>
          </div>
        </section>
      </template>
    </TradePageShell>
  </div>
</template>

<style scoped>
.checkout-lead,
.checkout-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.checkout-lead__copy,
.checkout-summary__copy {
  display: grid;
  gap: 8px;
}

.checkout-grid {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 20px;
}

.checkout-block,
.checkout-items {
  display: grid;
  gap: 14px;
}

.checkout-item,
.checkout-summary__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.checkout-item__copy {
  display: grid;
  gap: 4px;
}

.checkout-item__copy p,
.checkout-summary__meta span,
.checkout-tip p {
  color: var(--cm-text-secondary);
  font-size: 13px;
  line-height: 1.55;
}

.checkout-summary__meta strong {
  display: block;
  color: var(--cm-price);
  font-size: 28px;
  line-height: 1.1;
}

.checkout-tip {
  display: grid;
  gap: 6px;
  border: 1px dashed rgba(var(--cm-primary-rgb), 0.22);
  border-radius: 16px;
  padding: 14px;
  background: rgba(244, 248, 243, 0.85);
}

.checkout-radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 900px) {
  .checkout-grid,
  .checkout-lead,
  .checkout-summary,
  .checkout-summary__meta,
  .checkout-item {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
