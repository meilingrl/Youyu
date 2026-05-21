<script setup>
import { computed } from 'vue'
import TradeMetricStrip from '@/components/trade/TradeMetricStrip.vue'
import TradePageShell from '@/components/trade/TradePageShell.vue'

const metrics = computed(() => [
  {
    label: '购物车',
    value: '🛒',
    helper: '查看已选商品，准备下单'
  },
  {
    label: '我的订单',
    value: '📦',
    helper: '查看订单状态与物流进度'
  },
  {
    label: '评价',
    value: '⭐',
    helper: '为已收货的商品写评价'
  }
])
</script>

<template>
  <TradePageShell
    eyebrow="Trade Center"
    title="交易中心"
    description="购物车、订单、售后和评价，都在这里统一管理。"
    current-key="trade"
  >
    <template #actions>
      <el-button type="primary" @click="$router.push('/app/cart')">查看购物车</el-button>
    </template>

    <template #metrics>
      <TradeMetricStrip :items="metrics" />
    </template>

    <section class="trade-overview-grid">
      <article class="shell-card trade-overview-card">
        <span class="trade-overview-card__eyebrow">购物车</span>
        <h2>选好商品，准备下单</h2>
        <p>查看已加入购物车的商品，确认数量和价格后进入结算。</p>
        <el-button plain @click="$router.push('/app/cart')">去购物车</el-button>
      </article>

      <article class="shell-card trade-overview-card">
        <span class="trade-overview-card__eyebrow">订单管理</span>
        <h2>随时查看订单状态</h2>
        <p>待支付、已支付、待收货、退款……所有订单和售后进度一目了然。</p>
        <el-button plain @click="$router.push('/app/orders')">查看订单</el-button>
      </article>

      <article class="shell-card trade-overview-card">
        <span class="trade-overview-card__eyebrow">交易评价</span>
        <h2>收货后别忘了评价</h2>
        <p>好评帮助卖家成长，也能帮其他同学做出选择。</p>
        <div class="trade-overview-card__actions">
          <el-button plain @click="$router.push('/app/reviews/pending')">待评价</el-button>
          <el-button plain @click="$router.push('/app/reviews/mine')">我的评价</el-button>
        </div>
      </article>
    </section>
  </TradePageShell>
</template>

<style scoped>
.trade-overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.trade-overview-card,
.trade-overview-card__actions {
  display: grid;
  gap: 12px;
}

.trade-overview-card__eyebrow {
  color: var(--cm-text-tertiary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.trade-overview-card p {
  color: var(--cm-text-secondary);
  line-height: 1.65;
}
</style>
