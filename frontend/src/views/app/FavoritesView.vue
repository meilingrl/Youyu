<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import { useMarketStore } from '@/stores/market'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'

const marketStore = useMarketStore()
const loading = ref(false)
const loadError = ref(false)
const rows = computed(() => marketStore.favoriteProducts)

async function loadProducts() {
  loading.value = true
  loadError.value = false
  try {
    await Promise.all([
      marketStore.loadProducts(),
      marketStore.loadFavorites()
    ])
  } catch {
    loadError.value = true
    ElMessage.error('收藏商品加载失败')
  } finally {
    loading.value = false
  }
}

async function handleToggleFavorite(productId) {
  try {
    await marketStore.toggleFavoriteRemote(productId)
    ElMessage.success('收藏状态已更新')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '收藏更新失败')
  }
}

onMounted(loadProducts)
</script>

<template>
  <div class="shell-container page-stack">
    <section class="shell-card shell-hero shell-hero--compact">
      <div>
        <h1>我的收藏</h1>
        <p>把心动商品存进收藏夹，随时回来查看或下单。</p>
      </div>
      <div class="shell-hero__meta">
        <el-tag>{{ rows.length }} 个收藏</el-tag>
      </div>
    </section>

    <ErrorBlock v-if="loadError" @retry="loadProducts" />

    <section v-else-if="rows.length" class="product-grid">
      <article v-for="item in rows" :key="item.id" class="product-card shell-card">
        <img
          :src="item.cover"
          :alt="item.title"
          class="product-card__cover"
          loading="lazy"
          decoding="async"
        />
        <div class="product-card__body">
          <div class="product-card__meta">
            <el-tag size="small">{{ item.categoryName }}</el-tag>
            <el-tag size="small" type="success">{{ item.type }}</el-tag>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.subtitle }}</p>
          <div class="price-row">
            <strong>￥{{ item.price }}</strong>
            <span>{{ item.shopName }}</span>
          </div>
          <div class="shell-inline-actions">
            <el-button plain @click="handleToggleFavorite(item.id)">取消收藏</el-button>
            <el-button type="primary" @click="$router.push(`/app/products/${item.id}`)">
              查看详情
            </el-button>
          </div>
        </div>
      </article>
    </section>

    <EmptyState
      v-else-if="!loading"
      emoji="⭐"
      title="还没有收藏"
      description="在商品详情页点亮收藏，把喜欢的宝贝集中在这里。"
    >
      <el-button type="primary" @click="$router.push('/app/products')">去商品列表</el-button>
    </EmptyState>
  </div>
</template>
