<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-services'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import PageSection from '@/components/common/PageSection.vue'
import { useMarketStore } from '@/stores/market'
import { couponDiscountLabel, marketingStatusLabel, useMarketingStore } from '@/stores/marketing'
import { resolveErrorMessage } from '@/utils/error-utils'

const marketStore = useMarketStore()
const marketingStore = useMarketingStore()

const activeTab = ref('coupons')
const loading = ref(false)
const loadError = ref('')
const creatingCoupon = ref(false)
const creatingActivity = ref(false)

const couponForm = reactive({
  title: '',
  type: 'fixed',
  discountAmount: 5,
  thresholdAmount: 0,
  totalQuantity: 50,
  startTime: '',
  endTime: '',
  description: ''
})

const activityForm = reactive({
  title: '',
  startTime: '',
  endTime: '',
  description: ''
})

const ownedShop = computed(() => marketStore.ownedShop.shop)

function resetCouponForm() {
  couponForm.title = ''
  couponForm.type = 'fixed'
  couponForm.discountAmount = 5
  couponForm.thresholdAmount = 0
  couponForm.totalQuantity = 50
  couponForm.startTime = ''
  couponForm.endTime = ''
  couponForm.description = ''
}

function resetActivityForm() {
  activityForm.title = ''
  activityForm.startTime = ''
  activityForm.endTime = ''
  activityForm.description = ''
}

function statusType(status) {
  return {
    active: 'success',
    approved: 'success',
    disabled: 'info',
    pending_review: 'warning',
    rejected: 'danger'
  }[status] || 'info'
}

function validityText(row) {
  const start = row.startAt || row.startTime || row.validFrom || row.effectiveStartTime
  const end = row.endAt || row.endTime || row.validTo || row.effectiveEndTime
  if (!start && !end) {
    return '有效期未设置'
  }
  return `${start || '现在'} 至 ${end || '长期有效'}`
}

function formatDateTime(date) {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function normalizeDateTime(value, fallback) {
  if (value) {
    return value.trim().replace('T', ' ')
  }
  const date = new Date()
  if (fallback === 'end') {
    date.setFullYear(date.getFullYear() + 1)
  }
  return formatDateTime(date)
}

function reviewDisplayStatus(row) {
  return row.reviewStatus || row.status || 'pending_review'
}

async function loadMarketing() {
  loading.value = true
  loadError.value = ''
  try {
    const results = await Promise.allSettled([
      marketStore.loadMyShop(),
      marketingStore.loadOwnerCoupons(),
      marketingStore.loadOwnerActivities()
    ])
    const rejected = results.find((item) => item.status === 'rejected')
    if (rejected) {
      throw rejected.reason
    }
  } catch (error) {
    loadError.value = resolveErrorMessage(error)
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

async function submitCoupon() {
  if (!couponForm.title.trim()) {
    ElMessage.warning('请填写优惠券名称')
    return
  }
  if (couponForm.type === 'threshold' && Number(couponForm.thresholdAmount) <= 0) {
    ElMessage.warning('满减券需要填写门槛金额')
    return
  }

  creatingCoupon.value = true
  try {
    await marketingStore.createCoupon({
      title: couponForm.title.trim(),
      couponType: couponForm.type === 'threshold' ? 'THRESHOLD' : 'FIXED',
      discountAmount: Number(couponForm.discountAmount || 0),
      minimumSpendAmount: couponForm.type === 'threshold' ? Number(couponForm.thresholdAmount || 0) : 0,
      totalQuantity: Number(couponForm.totalQuantity || 0),
      startAt: normalizeDateTime(couponForm.startTime, 'start'),
      endAt: normalizeDateTime(couponForm.endTime, 'end'),
      description: couponForm.description.trim()
    })
    ElMessage.success('优惠券已提交，等待管理员审核')
    resetCouponForm()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    creatingCoupon.value = false
  }
}

async function submitActivity() {
  if (!activityForm.title.trim()) {
    ElMessage.warning('请填写活动标题')
    return
  }
  if (!activityForm.description.trim()) {
    ElMessage.warning('请填写活动说明')
    return
  }

  creatingActivity.value = true
  try {
    await marketingStore.createActivity({
      title: activityForm.title.trim(),
      startAt: normalizeDateTime(activityForm.startTime, 'start'),
      endAt: normalizeDateTime(activityForm.endTime, 'end'),
      description: activityForm.description.trim()
    })
    ElMessage.success('店铺活动已提交，等待管理员审核')
    resetActivityForm()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    creatingActivity.value = false
  }
}

async function disableCoupon(row) {
  try {
    await ElMessageBox.confirm(`确认停用「${row.title || row.name || row.id}」吗？`, '停用优惠券', {
      type: 'warning'
    })
    await marketingStore.changeOwnerCouponStatus(row.id || row.couponId, { status: 'disabled' })
    ElMessage.success('优惠券已停用')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(resolveErrorMessage(error))
    }
  }
}

async function disableActivity(row) {
  try {
    await ElMessageBox.confirm(`确认停用「${row.title || row.name || row.id}」吗？`, '停用店铺活动', {
      type: 'warning'
    })
    await marketingStore.changeOwnerActivityStatus(row.id || row.activityId, { status: 'disabled' })
    ElMessage.success('店铺活动已停用')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(resolveErrorMessage(error))
    }
  }
}

onMounted(loadMarketing)
</script>

<template>
  <div class="shell-container page-stack">
    <section class="shell-card shell-hero shell-hero--compact">
      <div>
        <span class="eyebrow">Seller Marketing</span>
        <h1>店铺营销管理</h1>
        <p>
          创建店铺优惠券和活动内容，提交后进入管理员审核。活动只作为公开展示内容，不直接改变订单价格。
        </p>
      </div>
      <div class="shell-hero__meta shell-hero__meta--column">
        <el-button v-if="ownedShop?.id" type="primary" @click="$router.push(`/app/shops/${ownedShop.id}`)">
          查看店铺主页
        </el-button>
        <el-button plain @click="$router.push('/app/seller/products')">店铺商品</el-button>
        <el-button plain @click="$router.push('/app/me')">返回我的</el-button>
      </div>
    </section>

    <el-alert
      v-if="!ownedShop?.id && !loading"
      type="warning"
      show-icon
      :closable="false"
      title="暂未识别到店铺主体"
      description="如当前账号暂不具备创建或查看营销内容的权限，页面会提示对应原因。"
    />

    <ErrorBlock v-if="loadError" :message="loadError" @retry="loadMarketing" />

    <el-tabs v-else v-model="activeTab" class="marketing-tabs" v-loading="loading">
      <el-tab-pane label="优惠券" name="coupons">
        <div class="marketing-layout">
          <PageSection title="创建优惠券" description="支持立减券和满减券。新券默认等待管理员审核。">
            <el-form label-position="top" class="marketing-form">
              <el-form-item label="券名称">
                <el-input v-model="couponForm.title" maxlength="40" placeholder="例如 开学资料补贴券" />
              </el-form-item>
              <div class="marketing-form__grid">
                <el-form-item label="券类型">
                  <el-select v-model="couponForm.type">
                    <el-option label="立减券" value="fixed" />
                    <el-option label="满减券" value="threshold" />
                  </el-select>
                </el-form-item>
                <el-form-item label="优惠金额">
                  <el-input-number v-model="couponForm.discountAmount" :min="0.01" :precision="2" />
                </el-form-item>
                <el-form-item v-if="couponForm.type === 'threshold'" label="使用门槛">
                  <el-input-number v-model="couponForm.thresholdAmount" :min="0.01" :precision="2" />
                </el-form-item>
                <el-form-item label="发行数量">
                  <el-input-number v-model="couponForm.totalQuantity" :min="1" :step="1" />
                </el-form-item>
              </div>
              <div class="marketing-form__grid">
                <el-form-item label="开始时间">
                  <el-input v-model="couponForm.startTime" placeholder="yyyy-MM-dd HH:mm，可留空" />
                </el-form-item>
                <el-form-item label="结束时间">
                  <el-input v-model="couponForm.endTime" placeholder="yyyy-MM-dd HH:mm，可留空" />
                </el-form-item>
              </div>
              <el-form-item label="规则说明">
                <el-input v-model="couponForm.description" type="textarea" :rows="3" placeholder="说明适用商品或注意事项" />
              </el-form-item>
              <el-button type="primary" :loading="creatingCoupon" @click="submitCoupon">提交审核</el-button>
            </el-form>
          </PageSection>

          <PageSection title="优惠券列表" description="审核通过后用户才能领取。">
            <EmptyState
              v-if="!marketingStore.loadingOwnerCoupons && !marketingStore.ownerCoupons.length"
              title="暂无优惠券"
              description="创建第一张店铺优惠券后会出现在这里。"
            />
            <div v-else class="marketing-card-list">
              <article v-for="coupon in marketingStore.ownerCoupons" :key="coupon.id || coupon.couponId" class="marketing-row shell-card">
                <div>
                  <div class="marketing-row__title">
                    <h3>{{ coupon.title || coupon.name || '店铺优惠券' }}</h3>
                    <el-tag :type="statusType(reviewDisplayStatus(coupon))" effect="plain">
                      {{ marketingStatusLabel(reviewDisplayStatus(coupon)) }}
                    </el-tag>
                  </div>
                  <p>{{ coupon.description || coupon.ruleDescription || '暂无规则说明' }}</p>
                  <span>{{ validityText(coupon) }}</span>
                </div>
                <div class="marketing-row__side">
                  <strong>{{ couponDiscountLabel(coupon) }}</strong>
                  <el-button
                    v-if="coupon.status !== 'disabled'"
                    plain
                    type="warning"
                    @click="disableCoupon(coupon)"
                  >
                    停用
                  </el-button>
                </div>
              </article>
            </div>
          </PageSection>
        </div>
      </el-tab-pane>

      <el-tab-pane label="店铺活动" name="activities">
        <div class="marketing-layout">
          <PageSection title="创建店铺活动" description="活动通过审核后展示在店铺公开页面，不产生自动改价。">
            <el-form label-position="top" class="marketing-form">
              <el-form-item label="活动标题">
                <el-input v-model="activityForm.title" maxlength="50" placeholder="例如 本周教材专区上新" />
              </el-form-item>
              <div class="marketing-form__grid">
                <el-form-item label="开始时间">
                  <el-input v-model="activityForm.startTime" placeholder="yyyy-MM-dd HH:mm，可留空" />
                </el-form-item>
                <el-form-item label="结束时间">
                  <el-input v-model="activityForm.endTime" placeholder="yyyy-MM-dd HH:mm，可留空" />
                </el-form-item>
              </div>
              <el-form-item label="活动说明">
                <el-input v-model="activityForm.description" type="textarea" :rows="4" placeholder="说明活动内容，不填写自动折扣或统计承诺" />
              </el-form-item>
              <el-button type="primary" :loading="creatingActivity" @click="submitActivity">提交审核</el-button>
            </el-form>
          </PageSection>

          <PageSection title="活动列表" description="仅审核通过且生效的活动会在店铺页公开展示。">
            <EmptyState
              v-if="!marketingStore.loadingOwnerActivities && !marketingStore.ownerActivities.length"
              title="暂无店铺活动"
              description="创建活动后会进入管理员审核队列。"
            />
            <div v-else class="marketing-card-list">
              <article v-for="activity in marketingStore.ownerActivities" :key="activity.id || activity.activityId" class="marketing-row shell-card">
                <div>
                  <div class="marketing-row__title">
                    <h3>{{ activity.title || activity.name || '店铺活动' }}</h3>
                    <el-tag :type="statusType(reviewDisplayStatus(activity))" effect="plain">
                      {{ marketingStatusLabel(reviewDisplayStatus(activity)) }}
                    </el-tag>
                  </div>
                  <p>{{ activity.description || activity.content || '暂无活动说明' }}</p>
                  <span>{{ validityText(activity) }}</span>
                </div>
                <div class="marketing-row__side">
                  <el-button
                    v-if="activity.status !== 'disabled'"
                    plain
                    type="warning"
                    @click="disableActivity(activity)"
                  >
                    停用
                  </el-button>
                </div>
              </article>
            </div>
          </PageSection>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.marketing-tabs {
  display: grid;
  gap: 16px;
}

.marketing-layout {
  display: grid;
  grid-template-columns: minmax(320px, 0.88fr) minmax(0, 1.12fr);
  gap: 18px;
}

.marketing-form,
.marketing-card-list,
.marketing-row > div:first-child {
  display: grid;
  gap: 14px;
}

.marketing-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.marketing-row {
  display: flex;
  justify-content: space-between;
  gap: 18px;
}

.marketing-row__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.marketing-row h3 {
  margin: 0;
  font-size: 18px;
}

.marketing-row p,
.marketing-row span {
  margin: 0;
  color: var(--cm-text-secondary);
  line-height: 1.6;
}

.marketing-row__side {
  display: grid;
  justify-items: end;
  align-content: start;
  gap: 12px;
  min-width: 132px;
}

.marketing-row__side strong {
  color: var(--cm-price);
  font-size: 20px;
  white-space: nowrap;
}

@media (max-width: 980px) {
  .marketing-layout,
  .marketing-form__grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .marketing-row {
    flex-direction: column;
  }

  .marketing-row__side {
    justify-items: start;
  }
}
</style>
