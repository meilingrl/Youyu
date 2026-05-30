<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from '@/plugins/element-plus-services'
import ListPageShell from '@/components/shell/ListPageShell.vue'
import { getAdminMediationCases } from '@/api/modules/admin'
import { resolveErrorMessage } from '@/utils/error-utils'
import { adminLabel, adminTagType } from '@/utils/admin-display-labels'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const filters = reactive({
  keyword: '',
  status: '',
  decisionCategory: '',
  reportId: '',
  orderId: ''
})

const statusOptions = [
  'opened',
  'evidence_review',
  'decision_pending',
  'resolved',
  'cancelled'
]

const decisionOptions = [
  'refund_full_to_buyer',
  'refund_rejected_release_to_seller',
  'order_completion_required',
  'platform_governance_action',
  'no_action_invalid_or_duplicate'
]

function numericOrEmpty(value) {
  const text = String(value || '').trim()
  if (!text) {
    return ''
  }
  const number = Number(text)
  return Number.isFinite(number) ? number : ''
}

function queryParams() {
  return {
    keyword: filters.keyword,
    status: filters.status,
    decisionCategory: filters.decisionCategory,
    reportId: numericOrEmpty(filters.reportId),
    orderId: numericOrEmpty(filters.orderId),
    page: page.value,
    pageSize: pageSize.value
  }
}

async function loadCases() {
  loading.value = true
  error.value = ''

  try {
    const response = await getAdminMediationCases(queryParams())
    rows.value = response.data.items || []
    total.value = response.data.total || 0
  } catch (err) {
    error.value = resolveErrorMessage(err)
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  loadCases()
}

function onPageChange(nextPage) {
  page.value = nextPage
  loadCases()
}

function onPageSizeChange(nextPageSize) {
  pageSize.value = nextPageSize
  page.value = 1
  loadCases()
}

function openDetail(row) {
  router.push(`/admin/mediation/${row.id}`)
}

function participantLabel(row, key) {
  const participant = row.participants?.[key] || {}
  return participant.nickname || `#${participant.id || '-'}`
}

onMounted(loadCases)
</script>

<template>
  <ListPageShell
    title="调解案件"
    description="处理由订单举报升级来的正式平台争议，记录证据核查、处理进度和最终裁决。"
    :rows="rows"
    :loading="loading"
    :error="error"
    empty-title="暂无调解案件"
    empty-description="符合条件的订单举报可从举报页面升级为调解案件。"
    @retry="loadCases"
  >
    <template #filters>
      <div class="filter-row mediation-filters">
        <el-input v-model="filters.keyword" placeholder="搜索案件号 / 举报 / 订单" clearable @keyup.enter="onSearch" />
        <el-select v-model="filters.status" placeholder="案件状态" clearable>
          <el-option v-for="status in statusOptions" :key="status" :label="adminLabel(status)" :value="status" />
        </el-select>
        <el-select v-model="filters.decisionCategory" placeholder="裁决类型" clearable>
          <el-option v-for="item in decisionOptions" :key="item" :label="adminLabel(item)" :value="item" />
        </el-select>
        <el-input v-model="filters.reportId" placeholder="举报 ID" clearable @keyup.enter="onSearch" />
        <el-input v-model="filters.orderId" placeholder="订单 ID" clearable @keyup.enter="onSearch" />
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </div>
    </template>

    <template #table>
      <el-table v-loading="loading" :data="rows">
        <el-table-column prop="caseNo" label="案件号" min-width="180" />
        <el-table-column label="案件状态" min-width="140">
          <template #default="{ row }">
            <el-tag :type="adminTagType(row.status)" effect="plain">{{ adminLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="裁决类型" min-width="220">
          <template #default="{ row }">
            <span>{{ adminLabel(row.decisionCategory, '-') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="来源举报" min-width="220">
          <template #default="{ row }">
            <strong>#{{ row.sourceReportId }}</strong>
            <div class="muted">{{ row.sourceReport?.targetLabel || row.sourceReport?.reasonType || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="关联订单" min-width="240">
          <template #default="{ row }">
            <strong>{{ row.orderSummary?.orderNo || `#${row.relatedOrderId}` }}</strong>
            <div class="muted">
              {{ row.orderSummary?.productTitle || '-' }} / {{ adminLabel(row.orderSummary?.orderStatus, '-') }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="参与方" min-width="260">
          <template #default="{ row }">
            <div>买家：{{ participantLabel(row, 'buyer') }}</div>
            <div>卖家：{{ participantLabel(row, 'seller') }}</div>
            <div>举报人：{{ participantLabel(row, 'reporter') }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
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
.mediation-filters {
  grid-template-columns: minmax(220px, 1.5fr) repeat(4, minmax(130px, 1fr)) auto;
}

.muted {
  color: var(--cm-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 1100px) {
  .mediation-filters {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
