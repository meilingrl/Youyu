<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import PageSection from '@/components/common/PageSection.vue'
import { couponDiscountLabel, marketingStatusLabel, useMarketingStore } from '@/stores/marketing'
import { resolveErrorMessage } from '@/utils/error-utils'

const marketingStore = useMarketingStore()
const statusFilter = ref('')
const loadError = ref('')

const rows = computed(() =>
  marketingStore.myCoupons.filter((item) => !statusFilter.value || (item.userCouponStatus || item.status) === statusFilter.value)
)

function couponTitle(coupon) {
  return coupon.title || coupon.name || coupon.couponName || '店铺优惠券'
}

function couponShopName(coupon) {
  return coupon.shopName || coupon.shop?.name || '适用店铺'
}

function validityText(coupon) {
  const start = coupon.startTime || coupon.validFrom || coupon.effectiveStartTime
  const end = coupon.endTime || coupon.validTo || coupon.effectiveEndTime
  if (!start && !end) {
    return '有效期以店铺规则为准'
  }
  return `${start || '现在'} 至 ${end || '长期有效'}`
}

function statusType(status) {
  return {
    active: 'success',
    available: 'success',
    claimed: 'success',
    disabled: 'info',
    expired: 'info',
    used: 'warning'
  }[status] || 'info'
}

async function loadCoupons() {
  loadError.value = ''
  try {
    await marketingStore.loadMyCoupons()
  } catch (error) {
    loadError.value = resolveErrorMessage(error)
    ElMessage.error(loadError.value)
  }
}

onMounted(loadCoupons)
</script>

<template>
  <div class="shell-container page-stack">
    <section class="shell-card shell-hero shell-hero--compact">
      <div>
        <span class="eyebrow">Coupons</span>
        <h1>我的优惠券</h1>
        <p>集中查看已领取的店铺优惠券。结算时只能选择一张符合条件的优惠券，最终优惠以订单预览和下单校验结果为准。</p>
      </div>
      <div class="shell-hero__meta shell-hero__meta--column">
        <el-button type="primary" @click="$router.push('/app/explore')">去逛店铺</el-button>
        <el-button plain @click="$router.push('/app/me')">返回我的</el-button>
      </div>
    </section>

    <PageSection title="券包列表" description="只展示你已领取的优惠券，不承诺自动选择最优券。">
      <template #actions>
        <el-select v-model="statusFilter" clearable placeholder="状态筛选" class="coupon-status-filter">
          <el-option label="可用" value="active" />
          <el-option label="已领取" value="claimed" />
          <el-option label="已使用" value="used" />
          <el-option label="已过期" value="expired" />
          <el-option label="已停用" value="disabled" />
        </el-select>
      </template>

      <ErrorBlock v-if="loadError" :message="loadError" @retry="loadCoupons" />

      <EmptyState
        v-else-if="!marketingStore.loadingMyCoupons && !rows.length"
        title="暂无优惠券"
        description="进入店铺主页后，可领取已通过审核且正在生效的优惠券。"
      >
        <el-button type="primary" @click="$router.push('/app/explore')">浏览店铺商品</el-button>
      </EmptyState>

      <div v-else class="coupon-grid" v-loading="marketingStore.loadingMyCoupons">
        <article v-for="coupon in rows" :key="coupon.userCouponId || coupon.id" class="coupon-card shell-card">
          <div class="coupon-card__main">
            <div>
              <span class="eyebrow">{{ couponShopName(coupon) }}</span>
              <h3>{{ couponTitle(coupon) }}</h3>
            </div>
            <strong>{{ couponDiscountLabel(coupon) }}</strong>
          </div>
          <p>{{ coupon.description || coupon.ruleDescription || '结算时由服务端校验适用范围和有效状态。' }}</p>
          <div class="coupon-card__foot">
            <span>{{ validityText(coupon) }}</span>
            <el-tag :type="statusType(coupon.userCouponStatus || coupon.status)" effect="plain">
              {{ marketingStatusLabel(coupon.userCouponStatus || coupon.status) }}
            </el-tag>
          </div>
        </article>
      </div>
    </PageSection>
  </div>
</template>

<style scoped>
.coupon-status-filter {
  width: 180px;
}

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.coupon-card {
  display: grid;
  gap: 14px;
}

.coupon-card__main,
.coupon-card__foot {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.coupon-card h3 {
  margin: 4px 0 0;
  font-size: 18px;
}

.coupon-card strong {
  color: var(--cm-price);
  font-size: 22px;
  white-space: nowrap;
}

.coupon-card p,
.coupon-card__foot span {
  margin: 0;
  color: var(--cm-text-secondary);
  line-height: 1.6;
}

@media (max-width: 640px) {
  .coupon-card__main,
  .coupon-card__foot {
    flex-direction: column;
  }
}
</style>
