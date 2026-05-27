<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-services'
import ListPageShell from '@/components/shell/ListPageShell.vue'
import { getAdminVerifications, reviewAdminVerification } from '@/api/modules/admin'
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

async function loadVerifications() {
  loading.value = true
  error.value = ''

  try {
    const response = await getAdminVerifications({ ...filters, page: page.value, pageSize: pageSize.value })
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
  loadVerifications()
}

function onPageSizeChange(ps) {
  pageSize.value = ps
  page.value = 1
  loadVerifications()
}

function onSearch() {
  page.value = 1
  loadVerifications()
}

async function review(row, action) {
  try {
    let rejectReason = ''
    let reviewNote = ''

    if (action === 'reject') {
      const result = await ElMessageBox.prompt('请填写驳回原因', '认证驳回', {
        confirmButtonText: '提交驳回',
        cancelButtonText: '取消'
      })
      rejectReason = result.value
    } else {
      await ElMessageBox.confirm(`确认通过 ${row.realName} 的学生认证吗？`, '认证通过', {
        type: 'warning'
      })
      reviewNote = '认证信息核验通过'
    }

    await reviewAdminVerification(row.id, { action, rejectReason, reviewNote })
    ElMessage.success(action === 'approve' ? '认证已通过' : '认证已驳回')
    await loadVerifications()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(resolveErrorMessage(error))
    }
  }
}

onMounted(loadVerifications)
</script>

<template>
  <ListPageShell
    title="学生认证审核"
    description="处理学生认证申请：审核通过或驳回，并留存驳回原因。"
    :rows="rows"
    :loading="loading"
    :error="error"
    empty-title="暂无认证申请"
    empty-description="当前没有待处理的认证记录。"
    @retry="loadVerifications"
  >
    <template #filters>
      <div class="filter-row">
        <el-input v-model="filters.keyword" placeholder="搜索学号 / 姓名 / 校园邮箱" clearable @keyup.enter="onSearch" />
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
        <el-table-column prop="studentNo" label="学号" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="collegeName" label="学院" min-width="140" />
        <el-table-column prop="majorName" label="专业" min-width="140" />
        <el-table-column prop="verificationStatus" label="认证状态" min-width="120" />
        <el-table-column prop="submittedAt" label="提交时间" min-width="160" />
        <el-table-column prop="rejectReason" label="驳回原因" min-width="200" />
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.verificationStatus === 'pending_review'"
              link
              type="success"
              @click="review(row, 'approve')"
            >
              通过
            </el-button>
            <el-button
              v-if="row.verificationStatus === 'pending_review'"
              link
              type="danger"
              @click="review(row, 'reject')"
            >
              驳回
            </el-button>
            <el-tag v-if="row.verificationStatus !== 'pending_review'" type="info" effect="plain">无需操作</el-tag>
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
