<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import {
  deleteUserAccount,
  exportUserPersonalData,
  getUserConsentHistory
} from '@/api/modules/user'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const loadingHistory = ref(false)
const exporting = ref(false)
const deleting = ref(false)
const confirmation = ref('')
const consentHistory = ref([])
const exportPayload = ref(null)

function resolveErrorMessage(error) {
  return error?.response?.data?.message || error?.message || '请求失败'
}

async function loadConsentHistory() {
  loadingHistory.value = true
  try {
    const response = await getUserConsentHistory()
    consentHistory.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    loadingHistory.value = false
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const response = await exportUserPersonalData()
    exportPayload.value = response.data
    ElMessage.success('个人数据导出已生成')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    exporting.value = false
  }
}

async function handleDeleteAccount() {
  if (confirmation.value !== 'DELETE_ACCOUNT') {
    ElMessage.warning('请输入 DELETE_ACCOUNT 确认关闭账号')
    return
  }

  deleting.value = true
  try {
    await deleteUserAccount({ confirmation: confirmation.value })
    authStore.logout()
    ElMessage.success('账号已关闭并匿名化')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    deleting.value = false
  }
}

onMounted(loadConsentHistory)
</script>

<template>
  <div class="shell-container page-stack">
    <section class="shell-card settings-hero">
      <div>
        <span class="eyebrow">隐私</span>
        <h1>隐私与数据权利</h1>
        <p>
          查看同意历史、生成个人数据导出，或申请关闭账号。
        </p>
      </div>
      <div class="settings-hero__actions">
        <el-button plain @click="$router.push('/legal/privacy-policy')">隐私政策</el-button>
        <el-button plain @click="$router.push('/legal/cookie-policy')">Cookie 政策</el-button>
      </div>
    </section>

    <section class="shell-card privacy-panel">
      <div class="privacy-panel__header">
        <div>
          <strong>同意历史</strong>
          <p>注册、隐私政策、Cookie 和账号关闭同意记录按时间倒序展示。</p>
        </div>
        <el-button plain :loading="loadingHistory" @click="loadConsentHistory">刷新</el-button>
      </div>
      <div v-if="consentHistory.length" class="privacy-history">
        <article v-for="record in consentHistory" :key="record.id" class="privacy-history__item">
          <strong>{{ record.consentType }}</strong>
          <span>{{ record.consented ? '已同意' : '已拒绝' }} · {{ record.createdAt }}</span>
          <small>{{ record.source || 'client' }}</small>
        </article>
      </div>
      <p v-else class="privacy-empty">暂无同意记录。</p>
    </section>

    <section class="shell-card privacy-panel">
      <div class="privacy-panel__header">
        <div>
          <strong>个人数据导出</strong>
          <p>
            导出内容包含当前已实现的个人资料、地址、偏好、同意、订单和评价数据，不包含其他用户数据或内部密码哈希。
          </p>
        </div>
        <el-button type="primary" :loading="exporting" @click="handleExport">生成导出</el-button>
      </div>
      <pre v-if="exportPayload" class="privacy-export">{{ JSON.stringify(exportPayload, null, 2) }}</pre>
    </section>

    <section class="shell-card privacy-panel privacy-panel--danger">
      <strong>账号关闭</strong>
      <p>
        当前账号删除实现为软关闭。个人资料联系字段会匿名化，历史订单、评价和同意记录会为交易追溯保留。
      </p>
      <el-input v-model="confirmation" placeholder="Type DELETE_ACCOUNT" />
      <el-button type="danger" plain :loading="deleting" @click="handleDeleteAccount">
        关闭账号
      </el-button>
    </section>
  </div>
</template>

<style scoped>
.settings-hero,
.privacy-panel__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
}

.settings-hero h1 {
  margin: 10px 0 12px;
  font-size: clamp(28px, 3vw, 42px);
  line-height: 1.2;
}

.settings-hero__actions,
.privacy-panel__header {
  flex-wrap: wrap;
}

.privacy-panel,
.privacy-history {
  display: grid;
  gap: 14px;
}

.privacy-panel p,
.privacy-history__item span,
.privacy-history__item small,
.privacy-empty {
  color: var(--cm-text-secondary);
}

.privacy-history__item {
  display: grid;
  gap: 4px;
  padding: 14px;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.64);
}

.privacy-export {
  max-height: 420px;
  overflow: auto;
  padding: 14px;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  white-space: pre-wrap;
}

.privacy-panel--danger {
  border-color: rgba(217, 74, 56, 0.24);
}

@media (max-width: 760px) {
  .settings-hero,
  .privacy-panel__header {
    flex-direction: column;
  }
}
</style>
