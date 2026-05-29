<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from '@/plugins/element-plus-services'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import {
  createSupportTicket,
  getSupportTicketDetail,
  getSupportTickets,
  replySupportTicket
} from '@/api/modules/support'
import { resolveErrorMessage } from '@/utils/error-utils'

const route = useRoute()
const router = useRouter()

const categories = [
  { label: '账号与认证', value: 'account' },
  { label: '订单售后', value: 'order' },
  { label: '商品信息', value: 'product' },
  { label: '店铺服务', value: 'shop' },
  { label: '支付问题', value: 'payment' },
  { label: '举报进度', value: 'report' },
  { label: '其他问题', value: 'other' }
]

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待受理', value: 'open' },
  { label: '处理中', value: 'in_progress' },
  { label: '待我补充', value: 'waiting_user' },
  { label: '已解决', value: 'resolved' },
  { label: '已关闭', value: 'closed' }
]

const loadingList = ref(false)
const loadingDetail = ref(false)
const creating = ref(false)
const replying = ref(false)
const error = ref('')
const detailError = ref('')
const tickets = ref([])
const total = ref(0)
const selectedTicketId = ref(null)
const selectedTicket = ref(null)
const messages = ref([])
const filters = reactive({
  status: '',
  page: 1,
  pageSize: 10
})
const form = reactive({
  category: normalizeCategory(route.query.category) || 'order',
  subject: '',
  content: '',
  relatedType: normalizeRelatedType(route.query.relatedType),
  relatedId: normalizeRelatedId(route.query.relatedId)
})
const replyForm = reactive({
  content: ''
})

const selectedTicketClosed = computed(() => selectedTicket.value?.status === 'closed')
const selectedTicketNumber = computed(() => selectedTicket.value?.ticketNo || selectedTicket.value?.ticket_no || selectedTicket.value?.id || '')

function normalizeCategory(value) {
  const candidate = Array.isArray(value) ? value[0] : value
  return categories.some((item) => item.value === candidate) ? candidate : ''
}

function normalizeRelatedType(value) {
  const candidate = Array.isArray(value) ? value[0] : value
  return ['order', 'product', 'shop', 'user', 'report'].includes(candidate) ? candidate : ''
}

function normalizeRelatedId(value) {
  const candidate = Array.isArray(value) ? value[0] : value
  return candidate ? String(candidate) : ''
}

function normalizePage(payload) {
  const items = Array.isArray(payload) ? payload : Array.isArray(payload?.items) ? payload.items : []
  return {
    items,
    total: Number.isFinite(Number(payload?.total)) ? Number(payload.total) : items.length
  }
}

function normalizeDetail(payload) {
  return {
    ticket: payload?.ticket || payload?.supportTicket || payload || null,
    messages: Array.isArray(payload?.messages)
      ? payload.messages
      : Array.isArray(payload?.ticket?.messages)
        ? payload.ticket.messages
        : []
  }
}

function categoryLabel(value) {
  return categories.find((item) => item.value === value)?.label || value || '未分类'
}

function statusLabel(value) {
  return statusOptions.find((item) => item.value === value)?.label || value || '未知状态'
}

function statusTagType(value) {
  if (value === 'open') return 'warning'
  if (value === 'in_progress') return 'primary'
  if (value === 'waiting_user') return 'danger'
  if (value === 'resolved') return 'success'
  if (value === 'closed') return 'info'
  return 'info'
}

function formatTime(value) {
  if (!value) return '未记录'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

function messageRoleLabel(message) {
  const role = message.senderRole || message.sender_role
  return role === 'admin' ? '平台客服' : '我'
}

function messageTypeLabel(message) {
  const type = message.messageType || message.message_type
  return type === 'internal_note' ? '内部备注' : '公开回复'
}

function buildCreatePayload() {
  const payload = {
    category: form.category,
    subject: form.subject.trim(),
    content: form.content.trim()
  }

  if (form.relatedType) payload.relatedType = form.relatedType
  if (form.relatedId) payload.relatedId = Number(form.relatedId)
  return payload
}

async function loadTickets() {
  loadingList.value = true
  error.value = ''

  try {
    const response = await getSupportTickets({ ...filters })
    const page = normalizePage(response.data)
    tickets.value = page.items
    total.value = page.total
    if (!selectedTicketId.value && tickets.value.length) {
      selectedTicketId.value = tickets.value[0].id
    }
  } catch (err) {
    error.value = resolveErrorMessage(err)
  } finally {
    loadingList.value = false
  }
}

async function loadDetail(ticketId) {
  if (!ticketId) {
    selectedTicket.value = null
    messages.value = []
    return
  }

  loadingDetail.value = true
  detailError.value = ''

  try {
    const response = await getSupportTicketDetail(ticketId)
    const detail = normalizeDetail(response.data)
    selectedTicket.value = detail.ticket
    messages.value = detail.messages
  } catch (err) {
    detailError.value = resolveErrorMessage(err)
  } finally {
    loadingDetail.value = false
  }
}

function selectTicket(ticket) {
  selectedTicketId.value = ticket.id
}

async function submitTicket() {
  if (!form.subject.trim() || !form.content.trim()) {
    ElMessage.warning('请填写工单标题和问题说明')
    return
  }

  creating.value = true
  try {
    const response = await createSupportTicket(buildCreatePayload())
    ElMessage.success('平台客服工单已提交，请等待异步处理')
    form.subject = ''
    form.content = ''
    const createdId = response.data?.id || response.data?.ticket?.id
    await loadTickets()
    if (createdId) selectedTicketId.value = createdId
  } catch (err) {
    ElMessage.error(resolveErrorMessage(err))
  } finally {
    creating.value = false
  }
}

async function submitReply() {
  if (!selectedTicketId.value || !replyForm.content.trim()) {
    ElMessage.warning('请填写补充内容')
    return
  }

  replying.value = true
  try {
    await replySupportTicket(selectedTicketId.value, {
      content: replyForm.content.trim()
    })
    replyForm.content = ''
    ElMessage.success('公开回复已提交')
    await Promise.all([loadTickets(), loadDetail(selectedTicketId.value)])
  } catch (err) {
    ElMessage.error(resolveErrorMessage(err))
  } finally {
    replying.value = false
  }
}

function onStatusChange() {
  filters.page = 1
  selectedTicketId.value = null
  selectedTicket.value = null
  messages.value = []
  loadTickets()
}

function onPageChange(page) {
  filters.page = page
  selectedTicketId.value = null
  loadTickets()
}

function clearRelatedContext() {
  form.relatedType = ''
  form.relatedId = ''
  router.replace({ query: { ...route.query, relatedType: undefined, relatedId: undefined } })
}

watch(selectedTicketId, (ticketId) => {
  loadDetail(ticketId)
})

onMounted(loadTickets)
</script>

<template>
  <div class="page-stack support-page">
    <section class="shell-hero shell-hero--compact support-page__hero">
      <div>
        <span class="eyebrow">平台客服工单</span>
        <h1>异步客服工单</h1>
        <p>这里用于向平台客服提交问题、补充材料和查看公开回复；客服会按工单处理，不是实时聊天窗口。</p>
      </div>
      <div class="support-page__notice">
        <strong>{{ total }}</strong>
        <span>我的工单</span>
      </div>
    </section>

    <section class="support-page__grid">
      <aside class="support-page__side">
        <section class="support-card support-card--form">
          <header class="support-card__head">
            <div>
              <h2>创建工单</h2>
              <p>请写清楚问题和期望处理方向，平台客服会异步回复。</p>
            </div>
          </header>

          <el-form label-position="top" @submit.prevent>
            <el-form-item label="问题类型">
              <el-select v-model="form.category" class="support-page__full">
                <el-option v-for="item in categories" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="工单标题">
              <el-input v-model="form.subject" maxlength="120" show-word-limit placeholder="例如：订单支付后没有更新状态" />
            </el-form-item>
            <el-form-item label="问题说明">
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="5"
                maxlength="1000"
                show-word-limit
                placeholder="请补充订单号、商品、时间、已尝试的操作等信息"
              />
            </el-form-item>
            <div v-if="form.relatedType || form.relatedId" class="support-page__context">
              <span>已带入关联对象：{{ form.relatedType || '未指定' }} #{{ form.relatedId || '未指定' }}</span>
              <el-button link type="primary" @click="clearRelatedContext">移除</el-button>
            </div>
            <el-button class="support-page__submit" type="primary" :loading="creating" @click="submitTicket">
              提交客服工单
            </el-button>
          </el-form>
        </section>

        <section class="support-card">
          <header class="support-card__head">
            <div>
              <h2>我的工单</h2>
              <p>按状态筛选后选择一条工单查看进展。</p>
            </div>
          </header>

          <el-select v-model="filters.status" class="support-page__full" @change="onStatusChange">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>

          <ErrorBlock v-if="error" :message="error" @retry="loadTickets" />
          <SkeletonCard v-else-if="loadingList" :count="3" />
          <EmptyState
            v-else-if="!tickets.length"
            title="暂无客服工单"
            description="提交后可以在这里查看客服公开回复和处理状态。"
          />
          <div v-else class="support-ticket-list">
            <button
              v-for="ticket in tickets"
              :key="ticket.id"
              class="support-ticket"
              :class="{ 'is-active': selectedTicketId === ticket.id }"
              type="button"
              @click="selectTicket(ticket)"
            >
              <span class="support-ticket__top">
                <strong>{{ ticket.subject }}</strong>
                <el-tag :type="statusTagType(ticket.status)" effect="plain">{{ statusLabel(ticket.status) }}</el-tag>
              </span>
              <span class="support-ticket__meta">
                {{ categoryLabel(ticket.category) }} · {{ ticket.ticketNo || ticket.ticket_no || `#${ticket.id}` }}
              </span>
              <span class="support-ticket__time">更新于 {{ formatTime(ticket.updatedAt || ticket.updated_at) }}</span>
            </button>
          </div>

          <el-pagination
            v-if="total > filters.pageSize"
            v-model:current-page="filters.page"
            :page-size="filters.pageSize"
            :total="total"
            layout="prev, pager, next"
            small
            background
            @current-change="onPageChange"
          />
        </section>
      </aside>

      <main class="support-card support-detail">
        <ErrorBlock v-if="detailError" :message="detailError" @retry="loadDetail(selectedTicketId)" />
        <SkeletonCard v-else-if="loadingDetail" :count="2" />
        <EmptyState
          v-else-if="!selectedTicket"
          title="请选择工单"
          description="左侧选择一条工单后，可查看公开回复并继续补充材料。"
        />
        <template v-else>
          <header class="support-detail__head">
            <div>
              <span class="eyebrow">工单 {{ selectedTicketNumber }}</span>
              <h2>{{ selectedTicket.subject }}</h2>
              <p>
                {{ categoryLabel(selectedTicket.category) }} · 创建于
                {{ formatTime(selectedTicket.createdAt || selectedTicket.created_at) }}
              </p>
            </div>
            <el-tag :type="statusTagType(selectedTicket.status)" effect="plain">{{ statusLabel(selectedTicket.status) }}</el-tag>
          </header>

          <section class="support-detail__content">
            <h3>问题说明</h3>
            <p>{{ selectedTicket.content }}</p>
          </section>

          <section class="support-messages">
            <h3>公开回复</h3>
            <EmptyState
              v-if="!messages.length"
              title="暂无公开回复"
              description="客服处理后会在这里留下公开回复。"
            />
            <template v-else>
              <article
                v-for="message in messages"
                :key="message.id"
                class="support-message"
                :class="{ 'is-admin': (message.senderRole || message.sender_role) === 'admin' }"
              >
                <header>
                  <strong>{{ messageRoleLabel(message) }}</strong>
                  <span>{{ messageTypeLabel(message) }} · {{ formatTime(message.createdAt || message.created_at) }}</span>
                </header>
                <p>{{ message.content }}</p>
              </article>
            </template>
          </section>

          <section class="support-reply">
            <h3>补充公开回复</h3>
            <el-alert
              v-if="selectedTicketClosed"
              type="info"
              show-icon
              :closable="false"
              title="该工单已关闭，不能继续回复。"
            />
            <template v-else>
              <el-input
                v-model="replyForm.content"
                type="textarea"
                :rows="4"
                maxlength="800"
                show-word-limit
                placeholder="补充问题进展或客服需要的材料，内容会作为公开回复保存"
              />
              <el-button type="primary" :loading="replying" @click="submitReply">提交公开回复</el-button>
            </template>
          </section>
        </template>
      </main>
    </section>
  </div>
</template>

<style scoped>
.support-page__hero,
.support-detail__head {
  align-items: center;
}

.support-page__notice {
  display: grid;
  min-width: 128px;
  gap: 4px;
  padding: 16px;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
  text-align: center;
}

.support-page__notice strong {
  font-size: 32px;
  line-height: 1;
}

.support-page__notice span,
.support-card__head p,
.support-detail__head p,
.support-ticket__meta,
.support-ticket__time,
.support-message header span {
  color: var(--cm-text-secondary);
}

.support-page__grid {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.support-page__side,
.support-card,
.support-card__head,
.support-detail,
.support-detail__content,
.support-messages,
.support-reply {
  display: grid;
  gap: 14px;
}

.support-card {
  min-width: 0;
  padding: 18px;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--cm-shadow-soft);
}

.support-card h2,
.support-card h3,
.support-card p {
  margin: 0;
}

.support-page__full,
.support-page__submit {
  width: 100%;
}

.support-page__context {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
  margin-bottom: 14px;
  padding: 10px 12px;
  border: 1px solid rgba(var(--cm-primary-rgb), 0.2);
  border-radius: 8px;
  background: rgba(var(--cm-primary-rgb), 0.08);
}

.support-page__context span {
  min-width: 0;
  overflow-wrap: anywhere;
}

.support-ticket-list {
  display: grid;
  gap: 10px;
}

.support-ticket {
  display: grid;
  gap: 6px;
  width: 100%;
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--cm-text);
  text-align: left;
  cursor: pointer;
}

.support-ticket.is-active {
  border-color: rgba(var(--cm-primary-rgb), 0.36);
  background: rgba(var(--cm-primary-rgb), 0.08);
}

.support-ticket__top,
.support-detail__head,
.support-message header,
.support-reply {
  display: flex;
  gap: 10px;
}

.support-ticket__top,
.support-detail__head,
.support-message header {
  justify-content: space-between;
}

.support-ticket__top strong,
.support-detail__head h2,
.support-message p,
.support-detail__content p {
  min-width: 0;
  overflow-wrap: anywhere;
}

.support-ticket__meta,
.support-ticket__time {
  font-size: 13px;
}

.support-detail__content {
  padding: 14px;
  border-radius: 8px;
  background: rgba(var(--cm-primary-rgb), 0.06);
}

.support-message {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
}

.support-message.is-admin {
  border-color: rgba(var(--cm-primary-rgb), 0.24);
  background: rgba(var(--cm-primary-rgb), 0.07);
}

.support-reply {
  flex-direction: column;
}

@media (max-width: 900px) {
  .support-page__grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .support-page__notice {
    width: 100%;
  }

  .support-detail__head,
  .support-ticket__top,
  .support-message header,
  .support-page__context {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
