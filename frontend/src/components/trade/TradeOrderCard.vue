<script setup>
import { computed } from 'vue'
import TradeStatusTag from '@/components/trade/TradeStatusTag.vue'
import {
  formatCurrency,
  getFulfillmentTypeMeta,
  getOrderStatusMeta,
  getPaymentStatusMeta
} from '@/components/trade/trade-meta'

const props = defineProps({
  order: {
    type: Object,
    required: true
  }
})

const orderMeta = computed(() => getOrderStatusMeta(props.order.orderStatus))
const paymentMeta = computed(() => getPaymentStatusMeta(props.order.paymentStatus))
const fulfillmentMeta = computed(() => getFulfillmentTypeMeta(props.order.fulfillmentType))
</script>

<template>
  <article class="shell-card trade-order-card">
    <div class="trade-order-card__header">
      <div class="trade-order-card__identity">
        <p class="trade-order-card__order-no">{{ order.orderNo }}</p>
        <h3>{{ order.productTitle }}</h3>
        <p class="trade-order-card__description">{{ orderMeta.description }}</p>
      </div>
      <div class="trade-order-card__amount">
        <strong>{{ formatCurrency(order.payableAmount) }}</strong>
      </div>
    </div>

    <div class="trade-order-card__meta">
      <TradeStatusTag kind="order" :value="order.orderStatus" />
      <TradeStatusTag kind="payment" :value="order.paymentStatus" />
      <TradeStatusTag kind="fulfillment" :value="order.fulfillmentType" />
      <span class="trade-order-card__meta-copy">{{ paymentMeta.label }} · {{ fulfillmentMeta.label }}</span>
    </div>

    <div v-if="$slots.default" class="trade-order-card__actions">
      <slot />
    </div>
  </article>
</template>

<style scoped>
.trade-order-card {
  display: grid;
  gap: 16px;
}

.trade-order-card__header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.trade-order-card__identity {
  display: grid;
  gap: 6px;
}

.trade-order-card__order-no {
  color: var(--cm-text-tertiary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.trade-order-card__description,
.trade-order-card__meta-copy {
  color: var(--cm-text-secondary);
  font-size: 13px;
  line-height: 1.55;
}

.trade-order-card__amount strong {
  display: block;
  color: var(--cm-price);
  font-size: 28px;
  line-height: 1.1;
  letter-spacing: -0.03em;
}

.trade-order-card__meta,
.trade-order-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

@media (max-width: 768px) {
  .trade-order-card__header {
    flex-direction: column;
  }

  .trade-order-card__amount strong {
    font-size: 24px;
  }

  .trade-order-card__actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
