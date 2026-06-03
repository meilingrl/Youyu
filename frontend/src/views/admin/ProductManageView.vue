<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-services'
import ListPageShell from '@/components/shell/ListPageShell.vue'
import { batchUpdateAdminProductStatus, exportAdminDataset, getAdminProducts, updateAdminProductStatus } from '@/api/modules/admin'
import { useAuthStore } from '@/stores/auth'
import { hasAnyAdminPermission } from '@/utils/admin-permissions'
import { downloadBlobResponse } from '@/utils/download-utils'
import { resolveErrorMessage } from '@/utils/error-utils'
import { adminLabel, adminTagType } from '@/utils/admin-display-labels'

const authStore = useAuthStore()
const loading = ref(false)
const exportLoading = ref(false)
const error = ref('')
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const tableRef = ref(null)
const selectedRows = ref([])
const filters = reactive({
  keyword: '',
  status: '',
  reviewStatus: '',
  productType: ''
})
const canExport = computed(() => hasAnyAdminPermission(authStore.currentRole, ['ADMIN_DATA_EXPORT']))
async function loadProducts() {
  loading.value = true
  error.value = ''

  try {
    const response = await getAdminProducts({ ...filters, page: page.value, pageSize: pageSize.value })
    rows.value = response.data.items || []
    total.value = response.data.total || 0
  } catch (err) {
    error.value = resolveErrorMessage(err)
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

function onPageChange(p) {
  page.value = p
  loadProducts()
}

function onPageSizeChange(ps) {
  pageSize.value = ps
  page.value = 1
  loadProducts()
}

function onSearch() {
  page.value = 1
  loadProducts()
}

function onSelectionChange(selection) {
  selectedRows.value = selection
}

function canPutOnSale(row) {
  return row.status !== 'on_sale' && row.status !== 'closed' && ['approved', 'not_required'].includes(row.reviewStatus)
}

function hasProductAction(row) {
  return canPutOnSale(row) || row.status === 'on_sale'
}

async function changeStatus(row, status) {
  try {
    await ElMessageBox.confirm(`确认将商品状态调整为${adminLabel(status)}吗？`, '商品状态变更', {
      type: 'warning'
    })
    await updateAdminProductStatus(row.id, { status })
    ElMessage.success('商品状态已更新')
    await loadProducts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(resolveErrorMessage(error))
    }
  }
}

async function batchChangeStatus(status) {
  if (!selectedRows.value.length) return
  try {
    await ElMessageBox.confirm(`确认批量将 ${selectedRows.value.length} 个商品调整为${adminLabel(status)}吗？`, '批量商品状态变更', {
      type: 'warning'
    })
    await batchUpdateAdminProductStatus({
      ids: selectedRows.value.map((row) => row.id),
      status
    })
    ElMessage.success('批量商品状态已更新')
    selectedRows.value = []
    await loadProducts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(resolveErrorMessage(error))
    }
  }
}

async function downloadProductsExport() {
  if (!canExport.value) {
    return
  }

  exportLoading.value = true
  try {
    const response = await exportAdminDataset('products')
    downloadBlobResponse(response, 'admin-products-summary.csv')
    ElMessage.success('商品导出已开始')
  } catch (err) {
    ElMessage.error(resolveErrorMessage(err))
  } finally {
    exportLoading.value = false
  }
}

onMounted(loadProducts)
</script>

<template>
  <ListPageShell
    title="商品管理"
    description="查看商品上架与审核状态，处理需要平台介入的上下架操作。"
    :rows="rows"
    :loading="loading"
    :error="error"
    :selected-count="selectedRows.length"
    empty-title="暂无商品记录"
    empty-description="当前没有符合条件的商品。"
    @retry="loadProducts"
  >
    <template #toolbar>
      <el-button v-if="canExport" plain :loading="exportLoading" @click="downloadProductsExport">导出商品摘要</el-button>
    </template>

    <template #filters>
      <div class="filter-row">
        <el-input v-model="filters.keyword" placeholder="搜索商品标题 / 卖家 / 分类" clearable @keyup.enter="onSearch" />
        <el-select v-model="filters.productType" placeholder="商品类型" clearable>
          <el-option label="电子资料" value="digital" />
          <el-option label="实物商品" value="physical" />
        </el-select>
        <el-select v-model="filters.status" placeholder="商品状态" clearable>
          <el-option label="在售" value="on_sale" />
          <el-option label="下架" value="off_sale" />
          <el-option label="关闭" value="closed" />
        </el-select>
        <el-select v-model="filters.reviewStatus" placeholder="审核状态" clearable>
          <el-option label="待审核" value="pending_review" />
          <el-option label="已通过" value="approved" />
          <el-option label="已驳回" value="rejected" />
          <el-option label="无需审核" value="not_required" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </div>
    </template>

    <template #batch>
      <span>已选择 {{ selectedRows.length }} 个商品</span>
      <div class="shell-inline-actions">
        <el-button size="small" :disabled="!selectedRows.length" @click="batchChangeStatus('on_sale')">批量上架</el-button>
        <el-button size="small" :disabled="!selectedRows.length" @click="batchChangeStatus('off_sale')">批量下架</el-button>
        <el-button size="small" type="danger" plain :disabled="!selectedRows.length" @click="batchChangeStatus('closed')">
          批量关闭
        </el-button>
      </div>
    </template>

    <template #table>
      <el-table ref="tableRef" v-loading="loading" class="admin-select-table" row-key="id" :data="rows" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="title" label="商品标题" min-width="220" />
        <el-table-column prop="sellerName" label="卖家" min-width="100" />
        <el-table-column prop="categoryName" label="分类" min-width="120" />
        <el-table-column label="类型" min-width="100">
          <template #default="{ row }">{{ adminLabel(row.productType) }}</template>
        </el-table-column>
        <el-table-column label="商品状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="adminTagType(row.status)" effect="plain">{{ adminLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="adminTagType(row.reviewStatus)" effect="plain">{{ adminLabel(row.reviewStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewRejectReason" label="驳回原因" min-width="220" />
        <el-table-column label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canPutOnSale(row)"
              link
              type="success"
              @click="changeStatus(row, 'on_sale')"
            >
              上架
            </el-button>
            <el-button
              v-if="row.status === 'on_sale'"
              link
              type="warning"
              @click="changeStatus(row, 'off_sale')"
            >
              下架
            </el-button>
            <el-tag v-if="!hasProductAction(row)" type="info" effect="plain">无需操作</el-tag>
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
