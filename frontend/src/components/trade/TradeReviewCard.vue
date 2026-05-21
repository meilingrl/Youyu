<script setup>
import { formatCurrency } from '@/components/trade/trade-meta'

defineProps({
  title: {
    type: String,
    required: true
  },
  imageUrl: {
    type: String,
    default: ''
  },
  price: {
    type: [Number, String],
    default: 0
  },
  helper: {
    type: String,
    default: ''
  },
  statusText: {
    type: String,
    default: ''
  }
})
</script>

<template>
  <article class="shell-card trade-review-card">
    <div class="trade-review-card__main">
      <img v-if="imageUrl" :src="imageUrl" :alt="title" class="trade-review-card__image" />
      <div class="trade-review-card__copy">
        <h3>{{ title }}</h3>
        <p class="trade-review-card__price">{{ formatCurrency(price) }}</p>
        <p v-if="helper" class="trade-review-card__helper">{{ helper }}</p>
        <p v-if="statusText" class="trade-review-card__status">{{ statusText }}</p>
      </div>
    </div>
    <div v-if="$slots.default" class="trade-review-card__actions">
      <slot />
    </div>
  </article>
</template>

<style scoped>
.trade-review-card {
  display: grid;
  gap: 16px;
}

.trade-review-card__main {
  display: flex;
  gap: 16px;
  align-items: center;
}

.trade-review-card__image {
  width: 84px;
  height: 84px;
  border-radius: 18px;
  object-fit: cover;
  background: var(--cm-bg-soft);
}

.trade-review-card__copy {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.trade-review-card__price {
  color: var(--cm-price);
  font-weight: 700;
}

.trade-review-card__helper,
.trade-review-card__status {
  color: var(--cm-text-secondary);
  font-size: 13px;
  line-height: 1.55;
}

.trade-review-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .trade-review-card__main {
    align-items: flex-start;
  }

  .trade-review-card__actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
