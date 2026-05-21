<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-services'
import ListPageShell from '@/components/shell/ListPageShell.vue'
import { getAdminReports, processAdminReport } from '@/api/modules/admin'
import { resolveErrorMessage } from '@/utils/error-utils'

const loading = ref(false)
const error = ref('')
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const filters = reactive({
  keyword: '',
  status: '',
  targetType: ''
})

async function loadReports() {
  loading.value = true
  error.value = ''

  try {
    const response = await getAdminReports({ ...filters, page: page.value, pageSize: pageSize.value })
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
  loadReports()
}

function onPageSizeChange(ps) {
  pageSize.value = ps
  page.value = 1
  loadReports()
}

function onSearch() {
  page.value = 1
  loadReports()
}

async function process(row, status) {
  try {
    const result = await ElMessageBox.prompt('请填写处理结论', '举报处理', {
      confirmButtonText: '提交',
      cancelButtonText: '取消'
    })
    await processAdminReport(row.id, {
      status,
      resolution: result.value
    })
    ElMessage.success('举报处理结果已记录')
    await loadReports()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(resolveErrorMessage(error))
    }
  }
}

onMounted(loadReports)
</script>

<template>
  <ListPageShell
    title="举报处理"
    description="查看举报列表，更新处理状态并记录处理结论。"
    :rows="rows"
    :loading="loading"
    :error="error"
    empty-title="暂无举报记录"
    empty-description="当前没有待处理的举报信息。"
    @retry="loadReports"
  >
    <template #filters>
      <div class="filter-row">
        <el-input v-model="filters.keyword" placeholder="搜索对象名称 / 举报人 / 内容" clearable @keyup.enter="onSearch" />
        <el-select v-model="filters.targetType" placeholder="对象类型" clearable>
          <el-option label="用户" value="user" />
          <el-option label="商品" value="product" />
          <el-option label="店铺" value="shop" />
        </el-select>
        <el-select v-model="filters.status" placeholder="处理状态" clearable>
          <el-option label="待处理" value="pending" />
          <el-option label="处理中" value="processing" />
          <el-option label="已解决" value="resolved" />
          <el-option label="已驳回" value="rejected" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </div>
    </template>

    <template #table>
      <el-table v-loading="loading" :data="rows">
        <el-table-column prop="targetType" label="举报对象类型" min-width="120" />
        <el-table-column prop="targetLabel" label="对象名称" min-width="180" />
        <el-table-column prop="reporterName" label="举报人" min-width="100" />
        <el-table-column prop="reasonType" label="原因类型" min-width="120" />
        <el-table-column prop="status" label="处理状态" min-width="120" />
        <el-table-column prop="resolution" label="处理结论" min-width="220" />
        <el-table-column label="操作" min-width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'pending'"
              link
              type="warning"
              @click="process(row, 'processing')"
            >
              标记处理中
            </el-button>
            <el-button
              v-if="row.status !== 'resolved'"
              link
              type="success"
              @click="process(row, 'resolved')"
            >
              处理完成
            </el-button>
            <el-button
              v-if="row.status !== 'rejected'"
              link
              type="danger"
              @click="process(row, 'rejected')"
            >
              驳回举报
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
