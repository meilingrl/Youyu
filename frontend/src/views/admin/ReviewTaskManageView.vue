<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-services'
import ListPageShell from '@/components/shell/ListPageShell.vue'
import {
  batchReviewAdminTasks,
  getAdminReviewTaskDetail,
  getAdminReviewTasks,
  reviewAdminTask
} from '@/api/modules/admin'
import { resolveErrorMessage } from '@/utils/error-utils'
import { adminLabel, adminTagType } from '@/utils/admin-display-labels'
import { useAdminRowSwipeSelection } from '@/utils/admin-row-swipe-selection'

const loading = ref(false)
const error = ref('')
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const tableRef = ref(null)
const selectedRows = ref([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref({ reviewTask: null, product: null, media: [], digitalAssets: [] })
const filters = reactive({
  keyword: '',
  status: ''
})

useAdminRowSwipeSelection(tableRef, rows, selectedRows)

async function loadTasks() {
  loading.value = true
  error.value = ''

  try {
    const response = await getAdminReviewTasks({ ...filters, page: page.value, pageSize: pageSize.value })
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
  loadTasks()
}

function onPageSizeChange(ps) {
  pageSize.value = ps
  page.value = 1
  loadTasks()
}

function onSearch() {
  page.value = 1
  loadTasks()
}

function onSelectionChange(selection) {
  selectedRows.value = selection
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const response = await getAdminReviewTaskDetail(row.id)
    detail.value = response.data
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    detailLoading.value = false
  }
}

async function review(row, action) {
  try {
    let rejectReason = ''
    let reviewNote = ''

    if (action === 'reject') {
      const result = await ElMessageBox.prompt('请填写资料驳回原因', '资料审核驳回', {
        confirmButtonText: '提交驳回',
        cancelButtonText: '取消'
      })
      rejectReason = result.value
    } else {
      await ElMessageBox.confirm(`确认审核通过《${row.productTitle}》并自动上架吗？`, '资料审核通过', {
        type: 'warning'
      })
      reviewNote = '资料审核通过并自动上架'
    }

    await reviewAdminTask(row.id, { action, rejectReason, reviewNote })
    ElMessage.success(action === 'approve' ? '资料审核已通过' : '资料审核已驳回')
    await loadTasks()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(resolveErrorMessage(error))
    }
  }
}

async function batchReview(action) {
  if (!selectedRows.value.length) return
  try {
    let rejectReason = ''
    let reviewNote = ''
    if (action === 'reject') {
      const result = await ElMessageBox.prompt('请填写批量驳回原因', '批量驳回资料审核', {
        confirmButtonText: '提交驳回',
        cancelButtonText: '取消'
      })
      rejectReason = result.value
    } else {
      await ElMessageBox.confirm(`确认批量通过 ${selectedRows.value.length} 条资料审核并自动上架吗？`, '批量通过资料审核', {
        type: 'warning'
      })
      reviewNote = '资料审核批量通过并自动上架'
    }
    await batchReviewAdminTasks({
      ids: selectedRows.value.map((row) => row.id),
      action,
      rejectReason,
      reviewNote
    })
    ElMessage.success(action === 'approve' ? '资料审核已批量通过' : '资料审核已批量驳回')
    selectedRows.value = []
    await loadTasks()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(resolveErrorMessage(error))
    }
  }
}

onMounted(loadTasks)
</script>

<template>
  <ListPageShell
    title="资料商品审核"
    description="这是后台治理主流程，支持资料类商品审核通过、驳回和驳回原因记录。"
    :rows="rows"
    :loading="loading"
    :error="error"
    :selected-count="selectedRows.length"
    empty-title="暂无审核任务"
    empty-description="当前没有待处理的资料审核任务。"
    @retry="loadTasks"
  >
    <template #filters>
      <div class="filter-row">
        <el-input v-model="filters.keyword" placeholder="搜索商品标题 / 卖家" clearable @keyup.enter="onSearch" />
        <el-select v-model="filters.status" placeholder="审核状态" clearable>
          <el-option label="待审核" value="pending_review" />
          <el-option label="已通过" value="approved" />
          <el-option label="已驳回" value="rejected" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </div>
    </template>

    <template #batch>
      <span>已选择 {{ selectedRows.length }} 条资料审核任务</span>
      <div class="shell-inline-actions">
        <el-button size="small" :disabled="!selectedRows.length" @click="batchReview('approve')">批量通过并上架</el-button>
        <el-button size="small" type="danger" plain :disabled="!selectedRows.length" @click="batchReview('reject')">
          批量驳回
        </el-button>
      </div>
    </template>

    <template #table>
      <el-table ref="tableRef" v-loading="loading" class="admin-select-table" row-key="id" :data="rows" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="productTitle" label="商品标题" min-width="220" />
        <el-table-column prop="sellerName" label="卖家" min-width="100" />
        <el-table-column label="审核类型" min-width="120">
          <template #default="{ row }">{{ adminLabel(row.reviewType) }}</template>
        </el-table-column>
        <el-table-column label="审核状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="adminTagType(row.reviewStatus)" effect="plain">{{ adminLabel(row.reviewStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submittedAt" label="提交时间" min-width="160" />
        <el-table-column prop="rejectReason" label="驳回原因" min-width="220" />
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看资料</el-button>
            <el-button
              v-if="row.reviewStatus === 'pending_review'"
              link
              type="success"
              @click="review(row, 'approve')"
            >
              通过并上架
            </el-button>
            <el-button
              v-if="row.reviewStatus === 'pending_review'"
              link
              type="danger"
              @click="review(row, 'reject')"
            >
              驳回
            </el-button>
            <el-tag v-if="row.reviewStatus !== 'pending_review'" type="info" effect="plain">无需操作</el-tag>
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

  <el-drawer v-model="detailVisible" size="520px" title="资料审核详情">
    <div v-loading="detailLoading" class="page-stack">
      <div v-if="detail.product" class="shell-card detail-grid">
        <span>商品标题：{{ detail.product.title }}</span>
        <span>商品类型：{{ adminLabel(detail.product.productType) }}</span>
        <span>商品状态：{{ adminLabel(detail.product.status) }}</span>
        <span>审核状态：{{ adminLabel(detail.product.reviewStatus) }}</span>
        <span>卖家：{{ detail.reviewTask?.sellerName || '-' }}</span>
        <span>提交时间：{{ detail.reviewTask?.submittedAt || '-' }}</span>
        <span>驳回原因：{{ detail.reviewTask?.rejectReason || '无' }}</span>
      </div>

      <div v-if="detail.product" class="shell-card">
        <div class="section-heading"><h3>商品资料</h3></div>
        <p>{{ detail.product.description || detail.product.subtitle || '暂无资料说明' }}</p>
      </div>

      <div v-if="detail.digitalAssets?.length" class="shell-card">
        <div class="section-heading"><h3>数字资料</h3></div>
        <el-table :data="detail.digitalAssets">
          <el-table-column prop="assetName" label="资料名称" min-width="180" />
          <el-table-column prop="assetType" label="资料类型" min-width="120" />
          <el-table-column prop="assetUrl" label="资料地址" min-width="220" />
        </el-table>
      </div>

      <div v-if="detail.media?.length" class="shell-card">
        <div class="section-heading"><h3>商品图片</h3></div>
        <div class="review-media-grid">
          <img v-for="item in detail.media" :key="item.id || item.url" :src="item.url || item.mediaUrl" :alt="detail.product?.title" />
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped>
.review-media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}

.review-media-grid img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 12px;
  border: 1px solid var(--cm-border);
}
</style>
