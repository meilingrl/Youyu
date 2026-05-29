<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-services'
import ListPageShell from '@/components/shell/ListPageShell.vue'
import {
  disableAdminMarketingActivity,
  disableAdminMarketingCoupon,
  getAdminMarketingActivities,
  getAdminMarketingCoupons,
  reviewAdminMarketingActivity,
  reviewAdminMarketingCoupon
} from '@/api/modules/marketing'
import { couponDiscountLabel, marketingStatusLabel } from '@/stores/marketing'
import { resolveErrorMessage } from '@/utils/error-utils'

const activeTab = ref('coupons')
const loading = ref(false)
const error = ref('')
const coupons = ref([])
const activities = ref([])
const couponTotal = ref(0)
const activityTotal = ref(0)
const page = ref(1)
const pageSize = ref(10)

const filters = reactive({
  keyword: '',
  status: 'pending_review'
})

function normalizeList(response) {
  const data = response?.data
  return {
    items: Array.isArray(data) ? data : data?.items || data?.records || data?.list || [],
    total: Array.isArray(data) ? data.length : data?.total || data?.totalCount || 0
  }
}

function activeRows() {
  return activeTab.value === 'coupons' ? coupons.value : activities.value
}

async function loadRows() {
  loading.value = true
  error.value = ''
  try {
    const params = {
      keyword: filters.keyword,
      reviewStatus: filters.status,
      page: page.value,
      pageSize: pageSize.value
    }
    const response =
      activeTab.value === 'coupons'
        ? await getAdminMarketingCoupons(params)
        : await getAdminMarketingActivities(params)
    const data = normalizeList(response)
    if (activeTab.value === 'coupons') {
      coupons.value = data.items
      couponTotal.value = data.total
    } else {
      activities.value = data.items
      activityTotal.value = data.total
    }
  } catch (err) {
    error.value = resolveErrorMessage(err)
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  loadRows()
}

function onPageChange(nextPage) {
  page.value = nextPage
  loadRows()
}

function onPageSizeChange(nextPageSize) {
  pageSize.value = nextPageSize
  page.value = 1
  loadRows()
}

function onTabChange() {
  page.value = 1
  loadRows()
}

async function reviewRow(row, action) {
  try {
    let rejectReason = ''
    if (action === 'reject') {
      const result = await ElMessageBox.prompt('请填写驳回原因', '营销内容驳回', {
        confirmButtonText: '提交驳回',
        cancelButtonText: '取消',
        inputPattern: /.+/,
        inputErrorMessage: '驳回原因不能为空'
      })
      rejectReason = result.value
    } else {
      await ElMessageBox.confirm('确认审核通过该营销内容吗？', '营销内容审核', {
        type: 'warning'
      })
    }

    const id = row.id || row.couponId || row.activityId
    const payload = {
      action,
      rejectReason,
      reviewNote: action === 'approve' ? '营销内容审核通过' : ''
    }
    if (activeTab.value === 'coupons') {
      await reviewAdminMarketingCoupon(id, payload)
    } else {
      await reviewAdminMarketingActivity(id, payload)
    }
    ElMessage.success(action === 'approve' ? '已审核通过' : '已驳回')
    await loadRows()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(resolveErrorMessage(err))
    }
  }
}

async function disableRow(row) {
  try {
    const result = await ElMessageBox.prompt('请填写停用原因', '停用风险营销内容', {
      confirmButtonText: '确认停用',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '停用原因不能为空',
      type: 'warning'
    })
    const id = row.id || row.couponId || row.activityId
    const payload = { reason: result.value }
    if (activeTab.value === 'coupons') {
      await disableAdminMarketingCoupon(id, payload)
    } else {
      await disableAdminMarketingActivity(id, payload)
    }
    ElMessage.success('营销内容已停用')
    await loadRows()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(resolveErrorMessage(err))
    }
  }
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

function rowTitle(row) {
  return row.title || row.name || row.couponName || row.activityName || '营销内容'
}

function rowStatus(row) {
  if (row.status === 'disabled') {
    return 'disabled'
  }
  return row.reviewStatus || row.status || 'pending_review'
}

function validityText(row) {
  const start = row.startAt || row.startTime || row.validFrom || row.effectiveStartTime
  const end = row.endAt || row.endTime || row.validTo || row.effectiveEndTime
  if (!start && !end) {
    return '有效期未设置'
  }
  return `${start || '现在'} 至 ${end || '长期有效'}`
}

onMounted(loadRows)
</script>

<template>
  <ListPageShell
    title="营销审核"
    description="集中处理店铺优惠券和店铺活动审核。权限失败以接口返回为准，前端导航不替代后端鉴权。"
    :rows="activeRows()"
    :loading="loading"
    :error="error"
    empty-title="暂无营销审核内容"
    empty-description="当前筛选条件下没有需要处理的优惠券或店铺活动。"
    @retry="loadRows"
  >
    <template #filters>
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="优惠券" name="coupons" />
        <el-tab-pane label="店铺活动" name="activities" />
      </el-tabs>
      <div class="filter-row">
        <el-input v-model="filters.keyword" placeholder="搜索标题 / 店铺 / 创建人" clearable @keyup.enter="onSearch" />
        <el-select v-model="filters.status" placeholder="审核状态" clearable>
          <el-option label="待审核" value="pending_review" />
          <el-option label="已通过" value="approved" />
          <el-option label="已驳回" value="rejected" />
          <el-option label="已停用" value="disabled" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </div>
    </template>

    <template #table>
      <div class="marketing-review-list" v-loading="loading">
        <article v-for="row in activeRows()" :key="row.id || row.couponId || row.activityId" class="marketing-review-row shell-card">
          <div class="marketing-review-row__body">
            <div class="marketing-review-row__title">
              <h3>{{ rowTitle(row) }}</h3>
              <el-tag :type="statusType(rowStatus(row))" effect="plain">
                {{ marketingStatusLabel(rowStatus(row)) }}
              </el-tag>
            </div>
            <p>{{ row.description || row.ruleDescription || row.content || '暂无说明' }}</p>
            <div class="marketing-review-row__meta">
              <span>店铺：{{ row.shopName || row.shop?.name || row.shopId || '未返回' }}</span>
              <span>提交人：{{ row.ownerName || row.creatorName || row.ownerId || '未返回' }}</span>
              <span>{{ validityText(row) }}</span>
            </div>
            <p v-if="row.rejectReason || row.reviewRejectReason" class="marketing-review-row__reason">
              驳回原因：{{ row.rejectReason || row.reviewRejectReason }}
            </p>
          </div>
          <div class="marketing-review-row__actions">
            <strong v-if="activeTab === 'coupons'">{{ couponDiscountLabel(row) }}</strong>
            <el-button
              v-if="rowStatus(row) === 'pending_review'"
              type="success"
              plain
              @click="reviewRow(row, 'approve')"
            >
              通过
            </el-button>
            <el-button
              v-if="rowStatus(row) === 'pending_review'"
              type="danger"
              plain
              @click="reviewRow(row, 'reject')"
            >
              驳回
            </el-button>
            <el-button
              v-if="rowStatus(row) !== 'disabled'"
              type="warning"
              plain
              @click="disableRow(row)"
            >
              停用
            </el-button>
          </div>
        </article>
      </div>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="activeTab === 'coupons' ? couponTotal : activityTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="onPageChange"
          @size-change="onPageSizeChange"
        />
      </div>
    </template>
  </ListPageShell>
</template>

<style scoped>
.marketing-review-list {
  display: grid;
  gap: 14px;
}

.marketing-review-row {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.marketing-review-row__body {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.marketing-review-row__title,
.marketing-review-row__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.marketing-review-row h3 {
  margin: 0;
  font-size: 18px;
}

.marketing-review-row p,
.marketing-review-row__meta {
  margin: 0;
  color: var(--cm-text-secondary);
  line-height: 1.6;
}

.marketing-review-row__reason {
  color: var(--cm-danger, #d94a38);
}

.marketing-review-row__actions {
  display: grid;
  justify-items: end;
  align-content: start;
  gap: 10px;
  min-width: 120px;
}

.marketing-review-row__actions strong {
  color: var(--cm-price);
  font-size: 18px;
  white-space: nowrap;
}

@media (max-width: 760px) {
  .marketing-review-row {
    flex-direction: column;
  }

  .marketing-review-row__actions {
    justify-items: start;
  }
}
</style>
