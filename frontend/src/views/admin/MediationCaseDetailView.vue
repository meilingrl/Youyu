<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-services'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import {
  getAdminMediationCaseDetail,
  recordAdminMediationDecision,
  updateAdminMediationStatus
} from '@/api/modules/admin'
import { resolveErrorMessage } from '@/utils/error-utils'
import { adminLabel, adminTagType } from '@/utils/admin-display-labels'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const actionLoading = ref(false)
const error = ref('')
const detail = ref(null)

const statusForm = reactive({
  status: '',
  cancelReason: ''
})

const decisionForm = reactive({
  decisionCategory: '',
  decisionSummary: '',
  enforcementSummary: ''
})

const decisionOptions = [
  'refund_full_to_buyer',
  'refund_rejected_release_to_seller',
  'order_completion_required',
  'platform_governance_action',
  'no_action_invalid_or_duplicate'
]

const caseRecord = computed(() => detail.value?.case || {})
const terminal = computed(() => ['resolved', 'cancelled'].includes(caseRecord.value.status))
const chatItems = computed(() => detail.value?.chatContext?.items || [])

const nextStatusOptions = computed(() => {
  switch (caseRecord.value.status) {
    case 'opened':
      return ['evidence_review', 'cancelled']
    case 'evidence_review':
      return ['decision_pending', 'cancelled']
    case 'decision_pending':
      return ['cancelled']
    default:
      return []
  }
})

async function loadDetail() {
  loading.value = true
  error.value = ''

  try {
    const response = await getAdminMediationCaseDetail(route.params.id)
    detail.value = response.data
    statusForm.status = ''
    statusForm.cancelReason = ''
    if (!decisionForm.decisionCategory && !terminal.value) {
      decisionForm.decisionCategory = decisionOptions[0]
    }
  } catch (err) {
    error.value = resolveErrorMessage(err)
  } finally {
    loading.value = false
  }
}

async function submitStatus() {
  if (!statusForm.status || actionLoading.value) return
  actionLoading.value = true
  try {
    await updateAdminMediationStatus(caseRecord.value.id, {
      status: statusForm.status,
      cancelReason: statusForm.cancelReason
    })
    ElMessage.success('调解状态已更新')
    await loadDetail()
  } catch (err) {
    ElMessage.error(resolveErrorMessage(err))
  } finally {
    actionLoading.value = false
  }
}

async function submitDecision() {
  if (terminal.value || actionLoading.value) return
  try {
    await ElMessageBox.confirm('确认记录最终裁决吗？裁决提交后不可重复写入。', '最终裁决', {
      confirmButtonText: '记录裁决',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (err) {
    return
  }

  actionLoading.value = true
  try {
    await recordAdminMediationDecision(caseRecord.value.id, decisionForm)
    ElMessage.success('最终裁决已记录')
    await loadDetail()
  } catch (err) {
    ElMessage.error(resolveErrorMessage(err))
  } finally {
    actionLoading.value = false
  }
}

function userLabel(user) {
  return user?.nickname || user?.username || `用户 ${user?.id || '-'}`
}

function userWithNumber(user) {
  const label = userLabel(user)
  return user?.id ? `${label}（用户编号 ${user.id}）` : label
}

function entityNumber(value, fallback = '-') {
  return value ? `编号 ${value}` : fallback
}

function reportReasonLabel(reason) {
  const labels = {
    inaccurate_content: '商品与描述不符',
    seller_not_fulfilling: '卖家未履约',
    quality_issue: '商品质量问题',
    fake_transaction: '虚假交易',
    other_violation: '其他违规'
  }
  return labels[reason] || reason || '-'
}

function messageTypeLabel(type) {
  const labels = {
    text: '文本消息',
    image: '图片消息',
    product_card: '商品卡片',
    order_card: '订单卡片',
    system: '系统消息'
  }
  return labels[type] || type || '消息'
}

function money(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number.toFixed(2) : '-'
}

onMounted(loadDetail)
</script>

<template>
  <div class="page-stack mediation-detail">
    <section class="shell-hero shell-hero--compact mediation-detail__hero">
      <div>
        <span class="eyebrow">平台调解</span>
        <h1>{{ caseRecord.caseNo || '调解案件' }}</h1>
        <p>查看争议上下文、推进处理状态，并记录一次性的正式平台裁决。</p>
      </div>
      <div class="shell-inline-actions">
        <el-button plain @click="router.push('/admin/mediation')">返回列表</el-button>
        <el-tag v-if="caseRecord.status" :type="adminTagType(caseRecord.status)" effect="plain">{{ adminLabel(caseRecord.status) }}</el-tag>
      </div>
    </section>

    <ErrorBlock v-if="error" :message="error" @retry="loadDetail" />
    <SkeletonCard v-else-if="loading" :count="4" />

    <template v-else-if="detail">
      <section class="mediation-detail__grid">
        <article class="shell-card detail-panel">
          <h2>案件信息</h2>
          <dl>
            <dt>案件号</dt>
            <dd>{{ caseRecord.caseNo }}</dd>
            <dt>状态</dt>
            <dd>{{ adminLabel(caseRecord.status) }}</dd>
            <dt>来源举报</dt>
            <dd>{{ entityNumber(caseRecord.sourceReportId) }}</dd>
            <dt>关联订单</dt>
            <dd>{{ entityNumber(caseRecord.relatedOrderId) }}</dd>
            <dt>裁决</dt>
            <dd>{{ adminLabel(caseRecord.decisionCategory, '-') }}</dd>
            <dt>创建时间</dt>
            <dd>{{ caseRecord.createdAt || '-' }}</dd>
            <dt>更新时间</dt>
            <dd>{{ caseRecord.updatedAt || '-' }}</dd>
          </dl>
        </article>

        <article class="shell-card detail-panel">
          <h2>参与方</h2>
          <dl>
            <dt>买家</dt>
            <dd>{{ userWithNumber(detail.participants.buyer) }}</dd>
            <dt>卖家</dt>
            <dd>{{ userWithNumber(detail.participants.seller) }}</dd>
            <dt>举报人</dt>
            <dd>{{ userWithNumber(detail.participants.reporter) }}</dd>
          </dl>
        </article>
      </section>

      <section class="mediation-detail__grid">
        <article class="shell-card detail-panel">
          <h2>来源举报</h2>
          <dl>
            <dt>对象</dt>
            <dd>{{ adminLabel(detail.sourceReport.targetType) }} {{ entityNumber(detail.sourceReport.targetId) }}</dd>
            <dt>对象名称</dt>
            <dd>{{ detail.sourceReport.targetLabel || '-' }}</dd>
            <dt>原因</dt>
            <dd>{{ reportReasonLabel(detail.sourceReport.reasonType) }}</dd>
            <dt>状态</dt>
            <dd>{{ adminLabel(detail.sourceReport.status) }}</dd>
            <dt>处理结论</dt>
            <dd>{{ detail.sourceReport.resolution || '-' }}</dd>
          </dl>
          <p class="detail-text">{{ detail.sourceReport.content }}</p>
        </article>

        <article class="shell-card detail-panel">
          <h2>订单与退款上下文</h2>
          <dl>
            <dt>订单号</dt>
            <dd>{{ detail.order.orderNo }}</dd>
            <dt>商品</dt>
            <dd>{{ detail.order.productTitle || '-' }}</dd>
            <dt>状态</dt>
            <dd>{{ adminLabel(detail.order.orderStatus) }} / {{ adminLabel(detail.order.paymentStatus) }}</dd>
            <dt>履约</dt>
            <dd>{{ adminLabel(detail.order.fulfillmentType) }}</dd>
            <dt>金额</dt>
            <dd>{{ money(detail.order.payAmount) }}</dd>
            <dt>退款数</dt>
            <dd>{{ detail.refunds.length }}</dd>
          </dl>

          <div v-if="detail.refunds.length" class="compact-list">
            <article v-for="refund in detail.refunds" :key="refund.id">
              <strong>{{ refund.refundNo }}</strong>
              <span>{{ adminLabel(refund.refundStatus) }} / {{ money(refund.refundAmount) }}</span>
            </article>
          </div>
        </article>
      </section>

      <section class="shell-card detail-panel">
        <h2>订单聊天记录</h2>
        <p class="detail-text">
          展示订单编号 {{ detail.chatContext.orderId }} 相关消息，便于核查争议背景。
        </p>

        <div v-if="chatItems.length" class="chat-context-list">
          <article v-for="message in chatItems" :key="message.id" class="chat-context-item">
            <header>
              <strong>{{ userLabel(message.sender) }}</strong>
              <span>{{ message.createdAt }}</span>
            </header>
            <p>{{ message.isRecalled ? '[已撤回]' : message.body }}</p>
            <small>{{ messageTypeLabel(message.messageType) }} / 会话编号 {{ message.conversationId }}</small>
          </article>
        </div>
        <div v-else class="empty-inline">暂无该订单关联聊天消息。</div>
      </section>

      <section class="mediation-detail__grid">
        <article class="shell-card detail-panel">
          <h2>状态操作</h2>
          <p class="detail-text">按案件流程推进状态，或取消仍在处理中的案件。</p>
          <div v-if="nextStatusOptions.length" class="action-stack">
            <el-select v-model="statusForm.status" placeholder="下一状态">
              <el-option v-for="status in nextStatusOptions" :key="status" :label="adminLabel(status)" :value="status" />
            </el-select>
            <el-input
              v-if="statusForm.status === 'cancelled'"
              v-model="statusForm.cancelReason"
              placeholder="取消原因"
            />
            <el-button type="primary" :loading="actionLoading" :disabled="!statusForm.status" @click="submitStatus">
              更新状态
            </el-button>
          </div>
          <el-tag v-else type="info" effect="plain">当前无可用状态操作</el-tag>
        </article>

        <article class="shell-card detail-panel">
          <h2>最终裁决</h2>
          <p class="detail-text">最终裁决只能记录一次，提交后案件进入已解决状态。</p>
          <div class="action-stack">
            <el-select v-model="decisionForm.decisionCategory" :disabled="terminal" placeholder="裁决类型">
              <el-option v-for="item in decisionOptions" :key="item" :label="adminLabel(item)" :value="item" />
            </el-select>
            <el-input
              v-model="decisionForm.decisionSummary"
              :disabled="terminal"
              type="textarea"
              :rows="4"
              placeholder="裁决说明"
            />
            <el-input
              v-model="decisionForm.enforcementSummary"
              :disabled="terminal"
              type="textarea"
              :rows="3"
              placeholder="执行说明"
            />
            <el-button
              type="warning"
              :loading="actionLoading"
              :disabled="terminal || !decisionForm.decisionCategory || !decisionForm.decisionSummary"
              @click="submitDecision"
            >
              记录最终裁决
            </el-button>
          </div>
        </article>
      </section>
    </template>
  </div>
</template>

<style scoped>
.mediation-detail__hero,
.mediation-detail__grid,
.detail-panel,
.action-stack,
.compact-list,
.chat-context-list {
  display: grid;
  gap: 16px;
}

.mediation-detail__grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.detail-panel {
  align-content: start;
  box-shadow: none;
}

.detail-panel h2 {
  margin: 0;
  font-size: 18px;
}

.detail-panel dl {
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: 10px 16px;
  margin: 0;
}

.detail-panel dt {
  color: var(--cm-text-tertiary);
  font-weight: 700;
}

.detail-panel dd {
  margin: 0;
  min-width: 0;
  overflow-wrap: anywhere;
}

.detail-text,
.compact-list span,
.chat-context-item small,
.chat-context-item header span,
.empty-inline {
  color: var(--cm-text-secondary);
  line-height: 1.65;
}

.compact-list article,
.chat-context-item {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
}

.chat-context-item header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.chat-context-item p {
  margin: 0;
}

@media (max-width: 900px) {
  .mediation-detail__grid {
    grid-template-columns: 1fr;
  }

  .detail-panel dl {
    grid-template-columns: 1fr;
  }
}
</style>
