<script setup>
import { computed, onMounted, ref } from 'vue'
import { useReviewStore } from '@/stores/review'
import ReviewList from '@/components/common/ReviewList.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import TradeMetricStrip from '@/components/trade/TradeMetricStrip.vue'
import TradePageShell from '@/components/trade/TradePageShell.vue'

const store = useReviewStore()
const activeTab = ref('product')
const productPage = ref(1)
const shopPage = ref(1)

const metrics = computed(() => [
  {
    label: '商品评价',
    value: String(store.myProductReviews.length),
    helper: '来自已完成交易的商品反馈。'
  },
  {
    label: '店铺评价',
    value: String(store.myShopReviews.length),
    helper: '作为交易体验补充，帮助后续买家判断店铺。'
  },
  {
    label: '评价总数',
    value: String(store.myProductReviews.length + store.myShopReviews.length),
    helper: '交易中心会保留完整评价历史。'
  }
])

onMounted(() => {
  store.loadMyReviews()
})
</script>

<template>
  <TradePageShell
    eyebrow="交易中心"
    title="我的评价"
    description="这里回看你已经提交过的商品和店铺评价，让交易完成后的反馈仍然留在交易中心范围内，而不是分散到别处。"
    current-key="reviews-mine"
  >
    <template #actions>
      <el-button plain @click="$router.push('/app/orders')">回到订单页</el-button>
      <el-button type="primary" @click="$router.push('/app/reviews/pending')">查看待评价</el-button>
    </template>

    <template #metrics>
      <TradeMetricStrip :items="metrics" />
    </template>

    <ErrorBlock
      v-if="store.myReviewsError"
      :message="store.myReviewsError"
      @retry="store.loadMyReviews()"
    />

    <div v-else-if="store.loadingMyReviews" class="shell-card" v-loading="store.loadingMyReviews">
      <el-skeleton :rows="4" animated />
    </div>

    <section v-else class="shell-card my-reviews-panel">
      <div class="my-reviews-panel__head">
        <div>
          <h2>交易反馈历史</h2>
          <p>商品评价和店铺评价都继续保留在交易中心里，方便回看和补充上下文。</p>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="my-reviews-tabs">
        <el-tab-pane label="商品评价" name="product">
          <ReviewList
            v-if="store.myProductReviews.length > 0"
            :reviews="store.myProductReviews"
            :total="store.myProductReviews.length"
            :page="productPage"
            @update:page="(page) => (productPage = page)"
          />
          <EmptyState
            v-else
            title="暂无商品评价"
            description="完成订单后提交的商品反馈会显示在这里。"
          />
        </el-tab-pane>
        <el-tab-pane label="店铺评价" name="shop">
          <ReviewList
            v-if="store.myShopReviews.length > 0"
            :reviews="store.myShopReviews"
            :total="store.myShopReviews.length"
            :page="shopPage"
            @update:page="(page) => (shopPage = page)"
          />
          <EmptyState
            v-else
            title="暂无店铺评价"
            description="购买体验相关的店铺反馈会显示在这里。"
          />
        </el-tab-pane>
      </el-tabs>
    </section>
  </TradePageShell>
</template>

<style scoped>
.my-reviews-panel,
.my-reviews-panel__head {
  display: grid;
  gap: 16px;
}

.my-reviews-panel__head p {
  color: var(--cm-text-secondary);
}
</style>
