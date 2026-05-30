<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-services'
import ListPageShell from '@/components/shell/ListPageShell.vue'
import {
  getAdminSearchGovernanceRules,
  createAdminSearchGovernanceRule,
  updateAdminSearchGovernanceRule,
  deleteAdminSearchGovernanceRule,
  getAdminSearchLogs
} from '@/api/modules/admin'
import { resolveErrorMessage } from '@/utils/error-utils'

const loading = ref(false)
const error = ref('')
const rules = ref([])
const filters = reactive({ ruleType: '' })
const createDialogVisible = ref(false)
const logDialogVisible = ref(false)
const logs = ref([])
const logPage = ref(1)
const logPageSize = ref(10)
const logTotal = ref(0)
const logLoading = ref(false)

const createForm = reactive({
  ruleType: 'SENSITIVE_WORD',
  keyword: '',
  displayLabel: ''
})

const ruleTypeOptions = [
  { label: '敏感词', value: 'SENSITIVE_WORD' },
  { label: '停用词', value: 'STOP_WORD' },
  { label: '隐藏关键词', value: 'HIDE_KEYWORD' },
  { label: '置顶关键词', value: 'PIN_KEYWORD' }
]

const ruleTypeLabels = {
  SENSITIVE_WORD: '敏感词',
  STOP_WORD: '停用词',
  HIDE_KEYWORD: '隐藏关键词',
  PIN_KEYWORD: '置顶关键词'
}

async function loadRules() {
  loading.value = true
  error.value = ''
  try {
    const response = await getAdminSearchGovernanceRules()
    const all = Array.isArray(response.data) ? response.data : (response.data?.items || [])
    rules.value = filters.ruleType
      ? all.filter(r => r.ruleType === filters.ruleType)
      : all
  } catch (err) {
    error.value = resolveErrorMessage(err)
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

async function createRule() {
  if (!createForm.keyword.trim()) {
    ElMessage.warning('请输入关键词')
    return
  }
  try {
    await createAdminSearchGovernanceRule({
      ruleType: createForm.ruleType,
      keyword: createForm.keyword.trim(),
      displayLabel: createForm.displayLabel.trim() || undefined
    })
    ElMessage.success('规则已创建')
    createDialogVisible.value = false
    createForm.keyword = ''
    createForm.displayLabel = ''
    await loadRules()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  }
}

async function toggleRule(row) {
  try {
    await updateAdminSearchGovernanceRule(row.id, { isActive: !row.isActive })
    ElMessage.success(row.isActive ? '规则已禁用' : '规则已启用')
    await loadRules()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  }
}

async function deleteRule(row) {
  try {
    await ElMessageBox.confirm(`确定删除规则 "${row.keyword}"？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteAdminSearchGovernanceRule(row.id)
    ElMessage.success('规则已删除')
    await loadRules()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(resolveErrorMessage(error))
    }
  }
}

async function loadLogs(page) {
  logLoading.value = true
  try {
    const response = await getAdminSearchLogs({ page: page || logPage.value, pageSize: logPageSize.value })
    logs.value = response.data?.items || []
    logTotal.value = response.data?.total || 0
    logPage.value = response.data?.page || page || 1
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    logLoading.value = false
  }
}

function openLogDialog() {
  logPage.value = 1
  logDialogVisible.value = true
  loadLogs(1)
}

onMounted(loadRules)
</script>

<template>
  <ListPageShell
    title="热搜治理"
    description="管理敏感词、停用词、隐藏和置顶规则。规则启用后自动影响公开热搜榜排序。"
    :rows="rules"
    :loading="loading"
    :error="error"
    empty-title="暂无治理规则"
    empty-description="当前没有热搜治理规则。"
    @retry="loadRules"
  >
    <template #summary>
      <div class="filter-row filter-row--summary">
        <el-tag>规则总数 {{ rules.length }}</el-tag>
        <el-tag type="warning">敏感词 {{ rules.filter(r => r.ruleType === 'SENSITIVE_WORD').length }}</el-tag>
        <el-tag type="info">停用词 {{ rules.filter(r => r.ruleType === 'STOP_WORD').length }}</el-tag>
        <el-tag type="danger">隐藏 {{ rules.filter(r => r.ruleType === 'HIDE_KEYWORD').length }}</el-tag>
        <el-tag type="success">置顶 {{ rules.filter(r => r.ruleType === 'PIN_KEYWORD').length }}</el-tag>
      </div>
    </template>

    <template #filters>
      <div class="filter-row">
        <el-select v-model="filters.ruleType" placeholder="规则类型" clearable>
          <el-option v-for="opt in ruleTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="loadRules">查询</el-button>
        <el-button @click="createDialogVisible = true">新增规则</el-button>
        <el-button @click="openLogDialog">浏览搜索日志</el-button>
      </div>
    </template>

    <template #table>
      <el-table v-loading="loading" :data="rules">
        <el-table-column label="规则类型" min-width="120">
          <template #default="{ row }">
            <el-tag :type="row.ruleType === 'PIN_KEYWORD' ? 'success' : row.ruleType === 'HIDE_KEYWORD' ? 'danger' : 'warning'">
              {{ ruleTypeLabels[row.ruleType] || row.ruleType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="keyword" label="关键词" min-width="180" />
        <el-table-column prop="displayLabel" label="显示标签" min-width="150" />
        <el-table-column label="状态" min-width="80">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'info'">
              {{ row.isActive ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="toggleRule(row)">
              {{ row.isActive ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="deleteRule(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </ListPageShell>

  <el-dialog v-model="createDialogVisible" title="新增治理规则" width="480px">
    <el-form label-position="top">
      <el-form-item label="规则类型">
        <el-select v-model="createForm.ruleType" style="width: 100%">
          <el-option v-for="opt in ruleTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="createForm.keyword" placeholder="输入关键词（自动标准化为小写）" />
      </el-form-item>
      <el-form-item label="显示标签（可选，用于置顶展示）">
        <el-input v-model="createForm.displayLabel" placeholder="留空则使用关键词原文" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="createRule">创建</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="logDialogVisible" title="搜索日志" width="700px">
    <el-table v-loading="logLoading" :data="logs" max-height="400">
      <el-table-column prop="keyword" label="搜索关键词" min-width="150" />
      <el-table-column prop="normalizedKeyword" label="规范化关键词" min-width="150" />
      <el-table-column prop="resultCount" label="结果数" min-width="80" />
      <el-table-column prop="createdAt" label="搜索时间" min-width="160" />
    </el-table>
    <el-pagination
      v-if="logTotal > logPageSize"
      :current-page="logPage"
      :page-size="logPageSize"
      :total="logTotal"
      layout="prev, pager, next"
      style="justify-content: center; margin-top: 16px"
      @current-change="loadLogs"
    />
  </el-dialog>
</template>
