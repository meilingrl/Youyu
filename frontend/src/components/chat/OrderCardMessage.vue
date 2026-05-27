<script setup>
import { computed } from 'vue'

const props = defineProps({
  order: {
    type: Object,
    default: null
  },
  body: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['open'])

const statusMeta = {
  pending: { label: '待处理', tone: 'warning' },
  paid: { label: '已支付', tone: 'info' },
  shipped: { label: '已发货', tone: 'primary' },
  delivered: { label: '已送达', tone: 'success' },
  completed: { label: '已完成', tone: 'muted' },
  cancelled: { label: '已取消', tone: 'danger' },
  pending_payment: { label: '待支付', tone: 'warning' },
  pending_fulfillment: { label: '待履约', tone: 'info' },
  pending_receipt: { label: '待收货', tone: 'primary' },
  refund_in_progress: { label: '退款中', tone: 'danger' },
  refunded: { label: '已退款', tone: 'muted' }
}

const card = computed(() => {
  const order = props.order || {}
  const items = Array.isArray(order.items) ? order.items : []
  const firstItem = items[0] || {}
  const status = order.status || order.orderStatus || ''

  return {
    id: order.id ?? order.orderId ?? null,
    number: order.orderNumber || order.orderNo || order.order_number || order.order_no || `#${order.id ?? ''}`,
    status,
    totalAmount: Number(order.totalAmount ?? order.total_amount ?? order.payableAmount ?? order.payable_amount ?? order.amount ?? 0),
    productTitle:
      order.productTitle ||
      order.product_title ||
      order.productTitleSnapshot ||
      order.product_title_snapshot ||
      firstItem.productTitleSnapshot ||
      firstItem.product_title_snapshot ||
      firstItem.productTitle ||
      firstItem.product_title ||
      '订单商品',
    productImage:
      order.productImage ||
      order.product_image ||
      order.productImageUrl ||
      order.product_image_url ||
      firstItem.productImage ||
      firstItem.product_image ||
      firstItem.productImageUrl ||
      firstItem.product_image_url ||
      firstItem.coverUrl ||
      firstItem.cover_url ||
      ''
  }
})

const meta = computed(() => statusMeta[card.value.status] || {
  label: card.value.status || '状态未知',
  tone: 'muted'
})

function openOrder() {
  if (card.value.id) {
    emit('open', card.value.id)
  }
}
</script>

<template>
  <article class="order-card-message" @click="openOrder">
    <p v-if="body" class="order-card-message__body">{{ body }}</p>

    <div class="order-card-message__head">
      <strong>订单 {{ card.number }}</strong>
      <span class="order-card-message__status" :class="`is-${meta.tone}`">{{ meta.label }}</span>
    </div>

    <div class="order-card-message__content">
      <div class="order-card-message__image">
        <img v-if="card.productImage" :src="card.productImage" :alt="card.productTitle" loading="lazy" decoding="async" />
        <span v-else>订单</span>
      </div>
      <div class="order-card-message__main">
        <h3>{{ card.productTitle }}</h3>
        <strong>¥{{ card.totalAmount.toFixed(2) }}</strong>
      </div>
    </div>

    <button type="button" class="order-card-message__action" :disabled="!card.id" @click.stop="openOrder">
      查看订单详情
    </button>
  </article>
</template>

<style scoped>
.order-card-message {
  width: min(320px, 100%);
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid rgba(31, 41, 55, 0.1);
  border-radius: 8px;
  background: #fff;
  color: #1f2937;
  cursor: pointer;
}

.order-card-message__body {
  margin: 0;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.5;
}

.order-card-message__head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.order-card-message__head strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-card-message__status {
  flex: none;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.order-card-message__status.is-warning {
  background: #ffedd5;
  color: #c2410c;
}

.order-card-message__status.is-info {
  background: #dbeafe;
  color: #1d4ed8;
}

.order-card-message__status.is-primary {
  background: #ede9fe;
  color: #6d28d9;
}

.order-card-message__status.is-success {
  background: #dcfce7;
  color: #166534;
}

.order-card-message__status.is-danger {
  background: #fee2e2;
  color: #b91c1c;
}

.order-card-message__status.is-muted {
  background: #e5e7eb;
  color: #6b7280;
}

.order-card-message__content {
  display: grid;
  grid-template-columns: 60px minmax(0, 1fr);
  gap: 12px;
}

.order-card-message__image {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  background: #f3f4f6;
  color: #9ca3af;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
}

.order-card-message__image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.order-card-message__main {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.order-card-message__main h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
  line-height: 1.35;
  font-weight: 700;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.order-card-message__main strong {
  color: #dc2626;
  font-size: 17px;
  line-height: 1.2;
}

.order-card-message__action {
  width: 100%;
  min-height: 34px;
  border: none;
  border-radius: 8px;
  background: #ea580c;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.order-card-message__action:disabled {
  background: #d1d5db;
  cursor: not-allowed;
}
</style>
