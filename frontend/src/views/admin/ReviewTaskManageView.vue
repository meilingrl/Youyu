<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-services'
import ListPageShell from '@/components/shell/ListPageShell.vue'
import { getAdminReviewTasks, reviewAdminTask } from '@/api/modules/admin'
import { resolveErrorMessage } from '@/utils/error-utils'

const loading = ref(false)
const error = ref('')
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const filters = reactive({
  keyword: '',
  status: ''
})

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

onMounted(loadTasks)
</script>

<template>
  <ListPageShell
    title="资料商品审核"
    description="这是后台治理主流程，支持资料类商品审核通过、驳回和驳回原因记录。"
    :rows="rows"
    :loading="loading"
    :error="error"
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

    <template #table>
      <el-table v-loading="loading" :data="rows">
        <el-table-column prop="productTitle" label="商品标题" min-width="220" />
        <el-table-column prop="sellerName" label="卖家" min-width="100" />
        <el-table-column prop="reviewType" label="审核类型" min-width="120" />
        <el-table-column prop="reviewStatus" label="审核状态" min-width="120" />
        <el-table-column prop="submittedAt" label="提交时间" min-width="160" />
        <el-table-column prop="rejectReason" label="驳回原因" min-width="220" />
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
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
