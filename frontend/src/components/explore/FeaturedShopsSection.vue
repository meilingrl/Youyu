<script setup>
defineProps({
  shops: {
    type: Array,
    default: () => []
  },
  title: {
    type: String,
    default: '精选店铺'
  },
  description: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['open-shop', 'open-product'])
</script>

<template>
  <section class="featured-shops shell-card">
    <header class="featured-shops__header">
      <div>
        <h2>{{ title }}</h2>
        <p v-if="description">{{ description }}</p>
      </div>
    </header>

    <div v-if="shops.length" class="featured-shops__grid">
      <article
        v-for="shop in shops"
        :key="shop.id || shop.name"
        class="featured-shops__card"
      >
        <button type="button" class="featured-shops__main" @click="emit('open-shop', shop)">
          <div class="featured-shops__avatar">{{ shop.initials }}</div>
          <div class="featured-shops__copy">
            <div class="featured-shops__title-row">
              <h3>{{ shop.name }}</h3>
              <span class="featured-shops__badge">{{ shop.categoryLabel }}</span>
            </div>
            <p>{{ shop.description }}</p>
          </div>
        </button>

        <div class="featured-shops__metrics">
          <span>{{ shop.productCount }} 件在售</span>
          <span>￥{{ shop.priceLabel }} 起</span>
          <span>{{ shop.favoriteCount }} 次收藏</span>
        </div>

        <div v-if="shop.previewProducts.length" class="featured-shops__previews">
          <button
            v-for="product in shop.previewProducts"
            :key="product.id"
            type="button"
            class="featured-shops__preview"
            @click="emit('open-product', product)"
          >
            <img :src="product.coverUrl || product.cover" :alt="product.title" />
            <span>{{ product.title }}</span>
          </button>
        </div>
      </article>
    </div>

    <p v-else class="featured-shops__empty">当前公开商品还不足以生成精选店铺，更多内容会随着商品丰富而补齐。</p>
  </section>
</template>

<style scoped>
.featured-shops {
  display: grid;
  gap: 20px;
}

.featured-shops__header h2 {
  margin: 0;
}

.featured-shops__header p {
  margin: 8px 0 0;
  color: var(--cm-text-secondary);
}

.featured-shops__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 18px;
}

.featured-shops__card {
  display: grid;
  gap: 16px;
  padding: 20px;
  border-radius: 26px;
  background:
    linear-gradient(180deg, rgba(255, 252, 246, 0.95), rgba(247, 239, 230, 0.94)),
    #fff;
  border: 1px solid rgba(154, 117, 88, 0.14);
}

.featured-shops__main {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 14px;
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.featured-shops__avatar {
  width: 56px;
  height: 56px;
  border-radius: 20px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, rgba(214, 127, 86, 0.22), rgba(126, 161, 133, 0.22));
  color: #854d2d;
  font-size: 18px;
  font-weight: 800;
}

.featured-shops__copy {
  display: grid;
  gap: 8px;
}

.featured-shops__title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.featured-shops__title-row h3 {
  margin: 0;
  font-size: 18px;
}

.featured-shops__badge {
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(255, 247, 237, 0.94);
  color: #b45309;
  font-size: 12px;
  font-weight: 700;
}

.featured-shops__copy p,
.featured-shops__empty {
  margin: 0;
  color: var(--cm-text-secondary);
  line-height: 1.6;
}

.featured-shops__metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  color: var(--cm-text-secondary);
  font-size: 13px;
}

.featured-shops__previews {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.featured-shops__preview {
  display: grid;
  gap: 8px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.featured-shops__preview img {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  border-radius: 16px;
  background: #efe6da;
}

.featured-shops__preview span {
  color: var(--cm-text);
  font-size: 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@media (max-width: 768px) {
  .featured-shops__card {
    padding: 18px;
  }
}
</style>
