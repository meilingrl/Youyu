<script setup>
import { computed, onMounted, ref } from 'vue'
import { useReviewStore } from '@/stores/review'
import ReviewForm from '@/components/common/ReviewForm.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import TradeMetricStrip from '@/components/trade/TradeMetricStrip.vue'
import TradePageShell from '@/components/trade/TradePageShell.vue'
import TradeReviewCard from '@/components/trade/TradeReviewCard.vue'

const store = useReviewStore()
const reviewingItem = ref(null)

const metrics = computed(() => [
  {
    label: '待评价订单',
    value: String(store.pendingItems.length),
    helper: '评价是交易闭环的一部分，不从订单完成节点消失。'
  },
  {
    label: '当前主路径',
    value: reviewingItem.value ? '填写评价' : '选择订单',
    helper: '移动端一次只强调一个下一步。'
  }
])

onMounted(() => {
  store.loadPendingReviews()
})

function startReview(item) {
  reviewingItem.value = item
}

function onSubmitted() {
  reviewingItem.value = null
  store.loadPendingReviews()
}
</script>

<template>
  <TradePageShell
    eyebrow="Trade Center"
    title="待评价"
    description="你有以下已收货商品还没有评价，写评价帮助其他同学做选择。"
    current-key="reviews-pending"
  >
    <template #actions>
      <el-button plain @click="$router.push('/app/orders')">回到订单页</el-button>
      <el-button type="primary" @click="$router.push('/app/reviews/mine')">查看我的评价</el-button>
    </template>

    <template #metrics>
      <TradeMetricStrip :items="metrics" />
    </template>

    <ErrorBlock
      v-if="store.pendingError"
      :message="store.pendingError"
      @retry="store.loadPendingReviews()"
    />

    <div v-else-if="store.loadingPending" class="shell-card" v-loading="store.loadingPending">
      <el-skeleton :rows="4" animated />
    </div>

    <EmptyState
      v-else-if="store.pendingItems.length === 0"
      emoji="⭐"
      title="暂时没有待评价订单"
      description="完成订单后，需要评价的商品会集中出现在这里。你也可以回到订单页继续跟进退款、举报或收货状态。"
    >
      <el-button type="primary" @click="$router.push('/app/orders')">回到订单页</el-button>
    </EmptyState>

    <template v-else>
      <section class="pending-review-list">
        <TradeReviewCard
          v-for="item in store.pendingItems"
          :key="item.id"
          :title="item.titleSnapshot"
          :image-url="item.imageSnapshot"
          :price="item.priceSnapshot"
          :helper="`完成时间：${item.completedAt}`"
          status-text="下一步：提交商品评价"
        >
          <template v-if="reviewingItem?.id === item.id">
            <div class="pending-review-form">
              <ReviewForm
                :order-item="item"
                type="product"
                @submitted="onSubmitted"
                @cancel="reviewingItem = null"
              />
            </div>
          </template>
          <el-button
            v-else
            type="primary"
            size="small"
            @click="startReview(item)"
          >
            写评价
          </el-button>
        </TradeReviewCard>
      </section>
    </template>
  </TradePageShell>
</template>

<style scoped>
.pending-review-list {
  display: grid;
  gap: 14px;
}

.pending-review-form {
  width: 100%;
  border-top: 1px solid var(--cm-border);
  padding-top: 12px;
}
</style>
