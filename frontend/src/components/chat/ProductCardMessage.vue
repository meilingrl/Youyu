<script setup>
import { computed } from 'vue'

const props = defineProps({
  product: {
    type: Object,
    default: null
  },
  body: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['open'])

const unavailableStatuses = new Set(['off_sale', 'closed', 'deleted', 'disabled', 'inactive', 'unavailable'])

const card = computed(() => {
  const product = props.product || {}
  const media = Array.isArray(product.media) ? product.media : []
  const image = product.imageUrl || product.image_url || product.coverUrl || product.cover_url || product.cover || media[0] || ''
  const status = product.status || ''

  return {
    id: product.id ?? product.productId ?? null,
    title: product.title || product.productTitle || '商品信息更新中',
    price: Number(product.price ?? product.salePrice ?? product.sale_price ?? 0),
    status,
    statusLabel: getStatusLabel(status),
    image
  }
})

const isAvailable = computed(() => {
  if (!card.value.id) return false
  return !unavailableStatuses.has(String(card.value.status).toLowerCase())
})

function getStatusLabel(status) {
  const labels = {
    active: '在售',
    on_sale: '在售',
    off_sale: '已下架',
    closed: '已下架',
    draft: '暂存',
    inactive: '不可用',
    unavailable: '不可用'
  }
  return labels[status] || status || '状态未知'
}

function openProduct() {
  if (isAvailable.value) {
    emit('open', card.value.id)
  }
}
</script>

<template>
  <article class="product-card-message" :class="{ 'is-disabled': !isAvailable }" @click="openProduct">
    <p v-if="body" class="product-card-message__body">{{ body }}</p>

    <div class="product-card-message__content">
      <div class="product-card-message__image">
        <img v-if="card.image" :src="card.image" :alt="card.title" loading="lazy" decoding="async" />
        <span v-else>商品</span>
      </div>

      <div class="product-card-message__main">
        <div class="product-card-message__top">
          <h3>{{ card.title }}</h3>
          <span class="product-card-message__status" :class="{ 'is-muted': !isAvailable }">
            {{ card.statusLabel }}
          </span>
        </div>
        <strong class="product-card-message__price">¥{{ card.price.toFixed(2) }}</strong>
      </div>
    </div>

    <button type="button" class="product-card-message__action" :disabled="!isAvailable" @click.stop="openProduct">
      查看详情
    </button>
  </article>
</template>

<style scoped>
.product-card-message {
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

.product-card-message.is-disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.product-card-message__body {
  margin: 0;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.5;
}

.product-card-message__content {
  display: grid;
  grid-template-columns: 80px minmax(0, 1fr);
  gap: 12px;
}

.product-card-message__image {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  background: #f3f4f6;
  color: #9ca3af;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
}

.product-card-message__image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-card-message__main,
.product-card-message__top {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.product-card-message__top h3 {
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

.product-card-message__status {
  width: fit-content;
  padding: 2px 8px;
  border-radius: 999px;
  background: #dcfce7;
  color: #166534;
  font-size: 12px;
  font-weight: 700;
}

.product-card-message__status.is-muted {
  background: #e5e7eb;
  color: #6b7280;
}

.product-card-message__price {
  color: #dc2626;
  font-size: 18px;
  line-height: 1.2;
}

.product-card-message__action {
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

.product-card-message__action:disabled {
  background: #d1d5db;
  cursor: not-allowed;
}
</style>
