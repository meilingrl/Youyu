<script setup>
const props = defineProps({
  product: {
    type: Object,
    required: true
  },
  showShopLink: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['open-product', 'open-shop'])

const productTypeLabels = {
  digital: '数字商品',
  physical: '实物商品',
  service: '服务'
}

const fulfillmentLabels = {
  logistics: '物流配送',
  offline: '线下交易',
  digital: '数字交付'
}

function formatPrice(value) {
  return Number(value || 0).toFixed(2)
}

function openProduct() {
  emit('open-product', props.product)
}

function openShop() {
  emit('open-shop', props.product)
}
</script>

<template>
  <article class="explore-product-card shell-card" @click="openProduct">
    <div class="explore-product-card__media">
      <img
        :src="product.coverUrl || product.cover"
        :alt="product.title"
        class="explore-product-card__image"
        loading="lazy"
        decoding="async"
      />
      <div class="explore-product-card__chips">
        <span class="explore-product-card__chip">{{ product.categoryName || '校园精选' }}</span>
        <span v-if="product.type" class="explore-product-card__chip explore-product-card__chip--muted">
          {{ productTypeLabels[product.type] || product.type }}
        </span>
      </div>
    </div>

    <div class="explore-product-card__body">
      <div class="explore-product-card__text">
        <h3>{{ product.title }}</h3>
        <p>{{ product.subtitle || '适合校园日常的轻松浏览与快速下单。' }}</p>
      </div>

      <div class="explore-product-card__meta">
        <strong class="explore-product-card__price">￥{{ formatPrice(product.salePrice || product.price) }}</strong>
        <button
          v-if="showShopLink && product.shopId"
          type="button"
          class="explore-product-card__shop"
          @click.stop="openShop"
        >
          {{ product.shopName || '店铺主页' }}
        </button>
        <span v-else class="explore-product-card__shop">{{ product.shopName || '校园卖家' }}</span>
      </div>

      <div class="explore-product-card__footer">
        <div class="explore-product-card__tags">
          <span
            v-for="type in product.allowedFulfillmentTypes || []"
            :key="type"
            class="explore-product-card__tag"
          >
            {{ fulfillmentLabels[type] || type }}
          </span>
        </div>
        <span class="explore-product-card__signal">{{ product.favoriteCount || 0 }} 人收藏</span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.explore-product-card {
  display: grid;
  gap: 0;
  overflow: hidden;
  padding: 0;
  cursor: pointer;
  transition:
    transform var(--cm-transition),
    box-shadow var(--cm-transition),
    border-color var(--cm-transition);
}

.explore-product-card:hover {
  transform: translateY(-4px);
  border-color: rgba(201, 93, 49, 0.22);
  box-shadow: 0 22px 40px rgba(95, 58, 30, 0.12);
}

.explore-product-card__media {
  position: relative;
  aspect-ratio: 4 / 5;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 249, 243, 0.2), rgba(233, 217, 198, 0.52)),
    #f0e6da;
}

.explore-product-card__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 320ms ease;
}

.explore-product-card:hover .explore-product-card__image {
  transform: scale(1.04);
}

.explore-product-card__chips {
  position: absolute;
  inset: 14px 14px auto 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.explore-product-card__chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 252, 247, 0.92);
  color: var(--cm-text);
  font-size: 12px;
  font-weight: 700;
  backdrop-filter: blur(10px);
}

.explore-product-card__chip--muted {
  background: rgba(98, 69, 49, 0.12);
}

.explore-product-card__body {
  display: grid;
  gap: 14px;
  padding: 18px 18px 20px;
}

.explore-product-card__text {
  display: grid;
  gap: 8px;
}

.explore-product-card__text h3 {
  margin: 0;
  font-size: 18px;
  line-height: 1.35;
}

.explore-product-card__text p {
  margin: 0;
  color: var(--cm-text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.explore-product-card__meta,
.explore-product-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.explore-product-card__price {
  color: var(--cm-price);
  font-size: 24px;
  line-height: 1;
}

.explore-product-card__shop {
  appearance: none;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--cm-text-secondary);
  font-weight: 600;
  cursor: pointer;
}

.explore-product-card__shop:hover {
  color: var(--cm-text);
}

.explore-product-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.explore-product-card__tag {
  padding: 5px 9px;
  border-radius: 999px;
  background: rgba(252, 244, 233, 0.95);
  color: #8a5a39;
  font-size: 12px;
  font-weight: 600;
}

.explore-product-card__signal {
  color: var(--cm-text-secondary);
  font-size: 12px;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .explore-product-card__body {
    padding: 16px;
  }

  .explore-product-card__text h3 {
    font-size: 17px;
  }
}
</style>
