<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-services'
import { useRouter } from 'vue-router'
import { getCart, removeCartItem, updateCartItem } from '@/api/modules/order'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import TradeMobileActionBar from '@/components/trade/TradeMobileActionBar.vue'
import TradeMetricStrip from '@/components/trade/TradeMetricStrip.vue'
import TradePageShell from '@/components/trade/TradePageShell.vue'
import { formatCurrency } from '@/components/trade/trade-meta'

const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const cart = ref({ items: [], summary: {} })
const pendingKeys = ref([])

const selectedItems = computed(() => cart.value.items.filter((item) => item.selected))
const hasSelectedItems = computed(() => selectedItems.value.length > 0)
const selectedCount = computed(() => Number(cart.value.summary.selectedCount || selectedItems.value.length || 0))
const selectedAmount = computed(() => Number(cart.value.summary.selectedAmount || 0))
const mobileActionHelper = computed(() =>
  hasSelectedItems.value ? `已选 ${selectedCount.value} 件，可进入结算` : '先勾选商品再结算'
)

const metrics = computed(() => [
  {
    label: '购物车商品',
    value: String(cart.value.items.length || 0),
    helper: '先确认本次要买哪些商品。'
  },
  {
    label: '已勾选结算',
    value: String(cart.value.summary.selectedCount || 0),
    helper: hasSelectedItems.value ? '已经可以进入结算。' : '还没有选择要结算的商品。'
  },
  {
    label: '待支付金额',
    value: formatCurrency(selectedAmount.value),
    helper: '金额会在提交订单前再次确认。'
  }
])

function markPending(key, active) {
  if (active) {
    pendingKeys.value = [...new Set([...pendingKeys.value, key])]
    return
  }
  pendingKeys.value = pendingKeys.value.filter((item) => item !== key)
}

function isPending(key) {
  return pendingKeys.value.includes(key)
}

async function loadCart() {
  loading.value = true
  loadError.value = ''
  try {
    const response = await getCart()
    cart.value = response.data || { items: [], summary: {} }
  } catch (error) {
    loadError.value = error?.response?.data?.message || error?.message || '购物车加载失败'
    ElMessage.error('购物车加载失败')
  } finally {
    loading.value = false
  }
}

async function updateItem(item, payload) {
  const actionKey = `update:${item.id}`
  if (isPending(actionKey)) return

  markPending(actionKey, true)
  try {
    const response = await updateCartItem(item.id, payload)
    cart.value = response.data || { items: [], summary: {} }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '购物车更新失败')
  } finally {
    markPending(actionKey, false)
  }
}

async function handleRemove(item) {
  const actionKey = `remove:${item.id}`
  if (isPending(actionKey)) return

  try {
    await ElMessageBox.confirm(`确认移除“${item.title}”吗？`, '移除购物车')
    markPending(actionKey, true)
    await removeCartItem(item.id)
    ElMessage.success('已移除')
    await loadCart()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '移除失败')
    }
  } finally {
    markPending(actionKey, false)
  }
}

function goCheckout() {
  if (!hasSelectedItems.value) {
    ElMessage.warning('请先勾选要结算的商品')
    return
  }

  const ids = selectedItems.value.map((item) => item.id)
  router.push({
    name: 'app-checkout',
    query: {
      ids: ids.join(',')
    }
  })
}

onMounted(loadCart)
</script>

<template>
  <div v-loading="loading">
    <TradePageShell
      eyebrow="Trade Center"
      title="购物车"
      description="确认要购买的商品，然后进入结算。"
      current-key="cart"
    >
      <template #actions>
        <el-button plain @click="$router.push('/app/trade')">返回交易中心</el-button>
        <el-button type="primary" :disabled="!hasSelectedItems" @click="goCheckout">
          去结算
        </el-button>
      </template>

      <template #metrics>
        <TradeMetricStrip :items="metrics" />
      </template>

      <ErrorBlock v-if="loadError" :message="loadError" @retry="loadCart" />

      <template v-else-if="cart.items.length">
        <section class="shell-card cart-stage-card">
          <div>
            <h2>这一步只做一件事</h2>
            <p>勾选本次准备购买的商品，数量确认无误后再去结算，移动端也只强调这一条清晰路径。</p>
          </div>
          <div class="cart-stage-card__summary">
            <span>已选 {{ cart.summary.selectedCount || 0 }} 件</span>
            <strong>{{ formatCurrency(cart.summary.selectedAmount || 0) }}</strong>
          </div>
        </section>

        <section class="cart-grid">
          <article v-for="item in cart.items" :key="item.id" class="shell-card cart-item">
            <img
              :src="item.coverUrl"
              :alt="item.title"
              class="cart-item__cover"
              loading="lazy"
              decoding="async"
            />
            <div class="cart-item__content">
              <div class="cart-item__top">
                <div class="cart-item__copy">
                  <p class="cart-item__eyebrow">
                    {{ item.productType === 'digital' ? '数字交付' : '实物商品' }}
                  </p>
                  <h3>{{ item.title }}</h3>
                  <p class="cart-item__note">
                    {{ item.selected ? '已加入本次结算' : '暂未加入本次结算，可稍后再决定' }}
                  </p>
                </div>
                <el-checkbox
                  :model-value="item.selected"
                  :disabled="isPending(`update:${item.id}`)"
                  @change="(value) => updateItem(item, { selected: value })"
                >
                  本次购买
                </el-checkbox>
              </div>

              <div class="cart-item__controls">
                <div class="cart-item__quantity">
                  <span>数量</span>
                  <el-input-number
                    :model-value="item.quantity"
                    :min="1"
                    :disabled="isPending(`update:${item.id}`)"
                    @change="(value) => updateItem(item, { quantity: value })"
                  />
                </div>
                <div class="cart-item__actions">
                  <span class="cart-item__price">{{ formatCurrency(item.subtotal) }}</span>
                  <el-button
                    text
                    type="danger"
                    :loading="isPending(`remove:${item.id}`)"
                    :disabled="isPending(`remove:${item.id}`)"
                    @click="handleRemove(item)"
                  >
                    移除
                  </el-button>
                </div>
              </div>
            </div>
          </article>
        </section>

        <section class="shell-card cart-summary-card">
          <div class="cart-summary-card__copy">
            <h2>下一步：结算确认</h2>
            <p>提交订单前还会确认履约方式、地址或线下交付信息。未勾选的商品会继续保留在购物车里。</p>
          </div>
          <div class="cart-summary-card__actions">
            <div>
              <span>本次实付</span>
              <strong>{{ formatCurrency(cart.summary.selectedAmount || 0) }}</strong>
            </div>
            <el-button type="primary" :disabled="!hasSelectedItems" @click="goCheckout">
              去结算
            </el-button>
          </div>
        </section>
      </template>

      <EmptyState
        v-else
        emoji="🛒"
        title="购物车还是空的"
        description="先去探索页挑选心仪商品，加入购物车后再来结算。"
      >
        <el-button type="primary" @click="$router.push('/app/products')">去逛商品</el-button>
      </EmptyState>

      <TradeMobileActionBar
        v-if="cart.items.length && !loadError"
        eyebrow="本次结算"
        :value="formatCurrency(selectedAmount)"
        :helper="mobileActionHelper"
        action-label="去结算"
        :disabled="!hasSelectedItems"
        @primary="goCheckout"
      />
    </TradePageShell>
  </div>
</template>

<style scoped>
.cart-stage-card,
.cart-summary-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.cart-stage-card__summary,
.cart-summary-card__actions {
  display: flex;
  align-items: center;
  gap: 18px;
}

.cart-stage-card__summary span,
.cart-summary-card__actions span {
  color: var(--cm-text-secondary);
  font-size: 13px;
}

.cart-stage-card__summary strong,
.cart-summary-card__actions strong {
  color: var(--cm-price);
  font-size: 28px;
  line-height: 1.1;
  letter-spacing: -0.03em;
}

.cart-grid {
  display: grid;
  gap: 18px;
}

.cart-item {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: 18px;
}

.cart-item__cover {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  border-radius: 18px;
  background: #eef4ee;
}

.cart-item__content,
.cart-item__copy {
  display: grid;
  gap: 10px;
}

.cart-item__top,
.cart-item__controls,
.cart-item__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.cart-item__eyebrow {
  color: var(--cm-text-tertiary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.cart-item__note {
  color: var(--cm-text-secondary);
  font-size: 13px;
  line-height: 1.55;
}

.cart-item__quantity {
  display: grid;
  gap: 8px;
}

.cart-item__quantity span {
  color: var(--cm-text-secondary);
  font-size: 13px;
}

.cart-item__price {
  min-width: 92px;
  text-align: right;
  color: var(--cm-price);
  font-size: 24px;
  font-weight: 700;
}

.cart-summary-card__copy {
  display: grid;
  gap: 8px;
}

@media (max-width: 768px) {
  .cart-stage-card,
  .cart-summary-card,
  .cart-item,
  .cart-item__top,
  .cart-item__controls,
  .cart-item__actions,
  .cart-summary-card__actions {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }

  .cart-item__price {
    text-align: left;
  }
}
</style>
