<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-services'
import ListPageShell from '@/components/shell/ListPageShell.vue'
import { batchUpdateAdminUserStatus, getAdminUserDetail, getAdminUsers, updateAdminUserStatus } from '@/api/modules/admin'
import { resolveErrorMessage } from '@/utils/error-utils'
import { adminLabel, adminTagType } from '@/utils/admin-display-labels'

const loading = ref(false)
const error = ref('')
const detailLoading = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const tableRef = ref(null)
const selectedRows = ref([])
const detailVisible = ref(false)
const detail = ref({ user: null, verifications: [], reports: [], products: [] })
const filters = reactive({
  keyword: '',
  status: '',
  verificationStatus: ''
})
async function loadUsers() {
  loading.value = true
  error.value = ''

  try {
    const response = await getAdminUsers({ ...filters, page: page.value, pageSize: pageSize.value })
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
  loadUsers()
}

function onPageSizeChange(ps) {
  pageSize.value = ps
  page.value = 1
  loadUsers()
}

function onSearch() {
  page.value = 1
  loadUsers()
}

function onSelectionChange(selection) {
  selectedRows.value = selection
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true

  try {
    const response = await getAdminUserDetail(row.id)
    detail.value = response.data
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    detailLoading.value = false
  }
}

async function changeStatus(row, status) {
  try {
    let restrictionReason = ''

    if (status === 'disabled') {
      const result = await ElMessageBox.prompt('请填写限制原因', '禁用用户', {
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      })
      restrictionReason = result.value
    } else {
      await ElMessageBox.confirm(`确认将用户状态调整为${adminLabel(status)}吗？`, '状态变更', {
        type: 'warning'
      })
    }

    await updateAdminUserStatus(row.id, { status, restrictionReason })
    ElMessage.success('用户状态已更新')
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(resolveErrorMessage(error))
    }
  }
}

async function batchChangeStatus(status) {
  if (!selectedRows.value.length) return
  try {
    let restrictionReason = ''
    if (status === 'disabled') {
      const result = await ElMessageBox.prompt('请填写批量禁用原因', '批量禁用用户', {
        confirmButtonText: '确认禁用',
        cancelButtonText: '取消'
      })
      restrictionReason = result.value
    } else {
      await ElMessageBox.confirm(`确认批量启用 ${selectedRows.value.length} 个用户吗？`, '批量启用用户', {
        type: 'warning'
      })
    }
    await batchUpdateAdminUserStatus({
      ids: selectedRows.value.map((row) => row.id),
      status,
      restrictionReason
    })
    ElMessage.success('批量用户状态已更新')
    selectedRows.value = []
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(resolveErrorMessage(error))
    }
  }
}

onMounted(loadUsers)
</script>

<template>
  <ListPageShell
    title="用户管理"
    description="管理用户资料、认证状态与启用/禁用，保障校园账号使用秩序。"
    :rows="rows"
    :loading="loading"
    :error="error"
    :selected-count="selectedRows.length"
    empty-title="暂无用户记录"
    empty-description="当前没有符合条件的用户。"
    @retry="loadUsers"
  >
    <template #filters>
      <div class="filter-row">
        <el-input v-model="filters.keyword" placeholder="搜索用户 / 学号 / 邮箱" clearable @keyup.enter="onSearch" />
        <el-select v-model="filters.status" placeholder="用户状态" clearable>
          <el-option label="正常" value="active" />
          <el-option label="禁用" value="disabled" />
        </el-select>
        <el-select v-model="filters.verificationStatus" placeholder="认证状态" clearable>
          <el-option label="待审核" value="pending_review" />
          <el-option label="已通过" value="approved" />
          <el-option label="已驳回" value="rejected" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </div>
    </template>

    <template #batch>
      <span>已选择 {{ selectedRows.length }} 个用户</span>
      <div class="shell-inline-actions">
        <el-button size="small" :disabled="!selectedRows.length" @click="batchChangeStatus('active')">批量启用</el-button>
        <el-button size="small" type="danger" plain :disabled="!selectedRows.length" @click="batchChangeStatus('disabled')">
          批量禁用
        </el-button>
      </div>
    </template>

    <template #table>
      <el-table ref="tableRef" v-loading="loading" class="admin-select-table" row-key="id" :data="rows" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="nickname" label="用户昵称" min-width="120" />
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="studentNo" label="学号" min-width="120" />
        <el-table-column label="用户状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="adminTagType(row.status)" effect="plain">{{ adminLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="认证状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="adminTagType(row.verificationStatus)" effect="plain">{{ adminLabel(row.verificationStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="privilegeLabel" label="权限档位" min-width="180" />
        <el-table-column prop="restrictionReason" label="限制原因" min-width="180" />
        <el-table-column label="操作" min-width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status !== 'active'"
              link
              type="success"
              @click="changeStatus(row, 'active')"
            >
              启用
            </el-button>
            <el-button
              v-if="row.status !== 'disabled'"
              link
              type="danger"
              @click="changeStatus(row, 'disabled')"
            >
              禁用
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

  <el-drawer v-model="detailVisible" size="42%" title="用户详情">
    <div v-loading="detailLoading" class="page-stack">
      <div v-if="detail.user" class="shell-card detail-grid">
        <span>昵称：{{ detail.user.nickname }}</span>
        <span>账号：{{ detail.user.username }}</span>
        <span>邮箱：{{ detail.user.email }}</span>
        <span>手机号：{{ detail.user.phone }}</span>
        <span>状态：{{ adminLabel(detail.user.status) }}</span>
        <span>认证状态：{{ adminLabel(detail.user.verificationStatus) }}</span>
        <span>信用等级：{{ detail.user.creditLevel }}</span>
        <span>权限：{{ detail.user.privilegeLabel }}</span>
      </div>

      <div class="shell-card">
        <div class="section-heading"><h3>认证记录</h3></div>
        <el-table :data="detail.verifications || []">
          <el-table-column prop="studentNo" label="学号" min-width="120" />
          <el-table-column label="状态" min-width="100">
            <template #default="{ row }">{{ adminLabel(row.verificationStatus) }}</template>
          </el-table-column>
          <el-table-column prop="rejectReason" label="驳回原因" min-width="180" />
        </el-table>
      </div>
    </div>
  </el-drawer>
</template>
