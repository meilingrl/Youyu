<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from '@/plugins/element-plus-services'
import ListPageShell from '@/components/shell/ListPageShell.vue'
import { getAdminMediationCases } from '@/api/modules/admin'
import { resolveErrorMessage } from '@/utils/error-utils'

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
    title="Mediation Cases"
    description="Formal platform dispute cases escalated from eligible order-backed reports."
    :rows="rows"
    :loading="loading"
    :error="error"
    empty-title="No mediation cases"
    empty-description="Eligible order reports can be escalated from the report management page."
    @retry="loadCases"
  >
    <template #filters>
      <div class="filter-row mediation-filters">
        <el-input v-model="filters.keyword" placeholder="Case no / report / order keyword" clearable @keyup.enter="onSearch" />
        <el-select v-model="filters.status" placeholder="Status" clearable>
          <el-option v-for="status in statusOptions" :key="status" :label="status" :value="status" />
        </el-select>
        <el-select v-model="filters.decisionCategory" placeholder="Decision" clearable>
          <el-option v-for="item in decisionOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-input v-model="filters.reportId" placeholder="Report ID" clearable @keyup.enter="onSearch" />
        <el-input v-model="filters.orderId" placeholder="Order ID" clearable @keyup.enter="onSearch" />
        <el-button type="primary" :loading="loading" @click="onSearch">Search</el-button>
      </div>
    </template>

    <template #table>
      <el-table v-loading="loading" :data="rows">
        <el-table-column prop="caseNo" label="Case No" min-width="180" />
        <el-table-column prop="status" label="Status" min-width="140" />
        <el-table-column prop="decisionCategory" label="Decision" min-width="220">
          <template #default="{ row }">
            <span>{{ row.decisionCategory || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Source Report" min-width="220">
          <template #default="{ row }">
            <strong>#{{ row.sourceReportId }}</strong>
            <div class="muted">{{ row.sourceReport?.targetLabel || row.sourceReport?.reasonType || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="Order" min-width="240">
          <template #default="{ row }">
            <strong>{{ row.orderSummary?.orderNo || `#${row.relatedOrderId}` }}</strong>
            <div class="muted">
              {{ row.orderSummary?.productTitle || '-' }} / {{ row.orderSummary?.orderStatus || '-' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Participants" min-width="260">
          <template #default="{ row }">
            <div>Buyer: {{ participantLabel(row, 'buyer') }}</div>
            <div>Seller: {{ participantLabel(row, 'seller') }}</div>
            <div>Reporter: {{ participantLabel(row, 'reporter') }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="Updated" min-width="180" />
        <el-table-column label="Actions" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">Detail</el-button>
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
