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
    ElMessage.success('Status updated')
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
    await ElMessageBox.confirm('Record final write-once platform decision?', 'Final decision', {
      confirmButtonText: 'Record',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
  } catch (err) {
    return
  }

  actionLoading.value = true
  try {
    await recordAdminMediationDecision(caseRecord.value.id, decisionForm)
    ElMessage.success('Final decision recorded')
    await loadDetail()
  } catch (err) {
    ElMessage.error(resolveErrorMessage(err))
  } finally {
    actionLoading.value = false
  }
}

function userLabel(user) {
  return user?.nickname || user?.username || `#${user?.id || '-'}`
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
        <span class="eyebrow">Platform Mediation</span>
        <h1>{{ caseRecord.caseNo || 'Mediation Case' }}</h1>
        <p>Read-only dispute context and formal write-once platform decision workspace.</p>
      </div>
      <div class="shell-inline-actions">
        <el-button plain @click="router.push('/admin/mediation')">Back to list</el-button>
        <el-tag v-if="caseRecord.status" effect="plain">{{ caseRecord.status }}</el-tag>
      </div>
    </section>

    <ErrorBlock v-if="error" :message="error" @retry="loadDetail" />
    <SkeletonCard v-else-if="loading" :count="4" />

    <template v-else-if="detail">
      <section class="mediation-detail__grid">
        <article class="shell-card detail-panel">
          <h2>Case</h2>
          <dl>
            <dt>Case no</dt>
            <dd>{{ caseRecord.caseNo }}</dd>
            <dt>Status</dt>
            <dd>{{ caseRecord.status }}</dd>
            <dt>Source report</dt>
            <dd>#{{ caseRecord.sourceReportId }}</dd>
            <dt>Related order</dt>
            <dd>#{{ caseRecord.relatedOrderId }}</dd>
            <dt>Decision</dt>
            <dd>{{ caseRecord.decisionCategory || '-' }}</dd>
            <dt>Created</dt>
            <dd>{{ caseRecord.createdAt || '-' }}</dd>
            <dt>Updated</dt>
            <dd>{{ caseRecord.updatedAt || '-' }}</dd>
          </dl>
        </article>

        <article class="shell-card detail-panel">
          <h2>Participants</h2>
          <dl>
            <dt>Buyer</dt>
            <dd>{{ userLabel(detail.participants.buyer) }} (#{{ detail.participants.buyer.id }})</dd>
            <dt>Seller</dt>
            <dd>{{ userLabel(detail.participants.seller) }} (#{{ detail.participants.seller.id }})</dd>
            <dt>Reporter</dt>
            <dd>{{ userLabel(detail.participants.reporter) }} (#{{ detail.participants.reporter.id }})</dd>
          </dl>
        </article>
      </section>

      <section class="mediation-detail__grid">
        <article class="shell-card detail-panel">
          <h2>Source Report</h2>
          <dl>
            <dt>Target</dt>
            <dd>{{ detail.sourceReport.targetType }} #{{ detail.sourceReport.targetId }}</dd>
            <dt>Label</dt>
            <dd>{{ detail.sourceReport.targetLabel || '-' }}</dd>
            <dt>Reason</dt>
            <dd>{{ detail.sourceReport.reasonType || '-' }}</dd>
            <dt>Status</dt>
            <dd>{{ detail.sourceReport.status }}</dd>
            <dt>Resolution</dt>
            <dd>{{ detail.sourceReport.resolution || '-' }}</dd>
          </dl>
          <p class="detail-text">{{ detail.sourceReport.content }}</p>
        </article>

        <article class="shell-card detail-panel">
          <h2>Order And Refund Context</h2>
          <dl>
            <dt>Order no</dt>
            <dd>{{ detail.order.orderNo }}</dd>
            <dt>Product</dt>
            <dd>{{ detail.order.productTitle || '-' }}</dd>
            <dt>Status</dt>
            <dd>{{ detail.order.orderStatus }} / {{ detail.order.paymentStatus }}</dd>
            <dt>Fulfillment</dt>
            <dd>{{ detail.order.fulfillmentType }}</dd>
            <dt>Amount</dt>
            <dd>{{ money(detail.order.payAmount) }}</dd>
            <dt>Refunds</dt>
            <dd>{{ detail.refunds.length }}</dd>
          </dl>

          <div v-if="detail.refunds.length" class="compact-list">
            <article v-for="refund in detail.refunds" :key="refund.id">
              <strong>{{ refund.refundNo }}</strong>
              <span>{{ refund.refundStatus }} / {{ money(refund.refundAmount) }}</span>
            </article>
          </div>
        </article>
      </section>

      <section class="shell-card detail-panel">
        <h2>Read-Only Chat Context</h2>
        <p class="detail-text">
          Scope: chat_messages.order_id = {{ detail.chatContext.orderId }}. This view does not call /api/chat/** and does not mutate chat read state.
        </p>

        <div v-if="chatItems.length" class="chat-context-list">
          <article v-for="message in chatItems" :key="message.id" class="chat-context-item">
            <header>
              <strong>{{ userLabel(message.sender) }}</strong>
              <span>{{ message.createdAt }}</span>
            </header>
            <p>{{ message.isRecalled ? '[recalled]' : message.body }}</p>
            <small>{{ message.messageType }} / conversation #{{ message.conversationId }}</small>
          </article>
        </div>
        <div v-else class="empty-inline">No scoped order chat messages.</div>
      </section>

      <section class="mediation-detail__grid">
        <article class="shell-card detail-panel">
          <h2>Status Action</h2>
          <p class="detail-text">Only non-final transitions and cancellation are available in mediation v1.</p>
          <div v-if="nextStatusOptions.length" class="action-stack">
            <el-select v-model="statusForm.status" placeholder="Next status">
              <el-option v-for="status in nextStatusOptions" :key="status" :label="status" :value="status" />
            </el-select>
            <el-input
              v-if="statusForm.status === 'cancelled'"
              v-model="statusForm.cancelReason"
              placeholder="Cancel reason"
            />
            <el-button type="primary" :loading="actionLoading" :disabled="!statusForm.status" @click="submitStatus">
              Update status
            </el-button>
          </div>
          <el-tag v-else type="info" effect="plain">No status action available</el-tag>
        </article>

        <article class="shell-card detail-panel">
          <h2>Final Decision</h2>
          <p class="detail-text">Final decisions are write-once and transition the case to resolved.</p>
          <div class="action-stack">
            <el-select v-model="decisionForm.decisionCategory" :disabled="terminal" placeholder="Decision category">
              <el-option v-for="item in decisionOptions" :key="item" :label="item" :value="item" />
            </el-select>
            <el-input
              v-model="decisionForm.decisionSummary"
              :disabled="terminal"
              type="textarea"
              :rows="4"
              placeholder="Decision summary"
            />
            <el-input
              v-model="decisionForm.enforcementSummary"
              :disabled="terminal"
              type="textarea"
              :rows="3"
              placeholder="Enforcement summary"
            />
            <el-button
              type="warning"
              :loading="actionLoading"
              :disabled="terminal || !decisionForm.decisionCategory || !decisionForm.decisionSummary"
              @click="submitDecision"
            >
              Record final decision
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
