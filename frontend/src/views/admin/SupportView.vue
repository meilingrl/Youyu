<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import {
  createAdminSupportTicketMessage,
  getAdminSupportTicketDetail,
  getAdminSupportTickets,
  updateAdminSupportTicketStatus
} from '@/api/modules/admin'
import { resolveErrorMessage } from '@/utils/error-utils'

const categories = [
  { label: '全部类型', value: '' },
  { label: '账号', value: 'account' },
  { label: '订单', value: 'order' },
  { label: '商品', value: 'product' },
  { label: '店铺', value: 'shop' },
  { label: '支付', value: 'payment' },
  { label: '举报', value: 'report' },
  { label: '其他', value: 'other' }
]

const statuses = [
  { label: '全部状态', value: '' },
  { label: '待受理', value: 'open' },
  { label: '处理中', value: 'in_progress' },
  { label: '待用户补充', value: 'waiting_user' },
  { label: '已解决', value: 'resolved' },
  { label: '已关闭', value: 'closed' }
]

const nextStatusMap = {
  open: ['in_progress', 'closed'],
  in_progress: ['waiting_user', 'resolved', 'closed'],
  waiting_user: ['in_progress', 'resolved', 'closed'],
  resolved: ['closed'],
  closed: []
}

const quickLinks = [
  { label: '订单', path: '/admin/orders' },
  { label: '举报', path: '/admin/reports' },
  { label: '调解', path: '/admin/mediation' },
  { label: '用户', path: '/admin/users' },
  { label: '商品', path: '/admin/products' },
  { label: '店铺', path: '/admin/shops' }
]

const loadingList = ref(false)
const loadingDetail = ref(false)
const updatingStatus = ref(false)
const submittingMessage = ref(false)
const error = ref('')
const detailError = ref('')
const tickets = ref([])
const total = ref(0)
const selectedTicketId = ref(null)
const selectedTicket = ref(null)
const messages = ref([])
const filters = reactive({
  status: '',
  category: '',
  keyword: '',
  assignedToMe: false,
  page: 1,
  pageSize: 12
})
const messageForm = reactive({
  messageType: 'public_reply',
  content: ''
})

const selectedClosed = computed(() => selectedTicket.value?.status === 'closed')
const nextStatuses = computed(() => nextStatusMap[selectedTicket.value?.status] || [])
const publicMessages = computed(() => messages.value.filter((item) => messageType(item) !== 'internal_note'))
const internalNotes = computed(() => messages.value.filter((item) => messageType(item) === 'internal_note'))

function buildListParams() {
  return {
    status: filters.status || undefined,
    category: filters.category || undefined,
    keyword: filters.keyword.trim() || undefined,
    assignedToMe: filters.assignedToMe ? true : undefined,
    page: filters.page,
    pageSize: filters.pageSize
  }
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
  return statuses.find((item) => item.value === value)?.label || value || '未知状态'
}

function statusTagType(value) {
  if (value === 'open') return 'warning'
  if (value === 'in_progress') return 'primary'
  if (value === 'waiting_user') return 'danger'
  if (value === 'resolved') return 'success'
  if (value === 'closed') return 'info'
  return 'info'
}

function messageType(message) {
  return message.messageType || message.message_type || 'public_reply'
}

function roleLabel(message) {
  const role = message.senderRole || message.sender_role
  if (role === 'admin') return '客服'
  if (role === 'system') return '系统'
  return '用户'
}

function formatTime(value) {
  if (!value) return '未记录'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

function ticketNumber(ticket) {
  return ticket?.ticketNo || ticket?.ticket_no || ticket?.id || ''
}

function requesterName(ticket) {
  return ticket?.requesterName || ticket?.requester_name || ticket?.username || `用户 #${ticket?.requesterUserId || ticket?.requester_user_id || '-'}`
}

function relatedType(ticket) {
  return ticket?.relatedType || ticket?.related_type || ''
}

function relatedId(ticket) {
  return ticket?.relatedId || ticket?.related_id || ''
}

function selectedContextLinks(ticket) {
  if (!ticket) return []
  const links = []
  const type = relatedType(ticket)
  const id = relatedId(ticket)
  const requesterId = ticket.requesterUserId || ticket.requester_user_id

  if (requesterId) {
    links.push({ label: '用户资料', path: `/admin/users?keyword=${encodeURIComponent(requesterId)}` })
  }
  if (type === 'order' && id) links.push({ label: '订单履约', path: `/admin/orders?keyword=${encodeURIComponent(id)}` })
  if (type === 'report' && id) links.push({ label: '举报处置', path: `/admin/reports?keyword=${encodeURIComponent(id)}` })
  if (type === 'product' && id) links.push({ label: '商品治理', path: `/admin/products?keyword=${encodeURIComponent(id)}` })
  if (type === 'shop' && id) links.push({ label: '店铺准入', path: `/admin/shops?keyword=${encodeURIComponent(id)}` })
  links.push({ label: '调解案件', path: '/admin/mediation' })
  return links
}

async function loadTickets() {
  loadingList.value = true
  error.value = ''

  try {
    const response = await getAdminSupportTickets(buildListParams())
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
    const response = await getAdminSupportTicketDetail(ticketId)
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

function onFilterChange() {
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

async function changeStatus(status) {
  if (!selectedTicketId.value || updatingStatus.value) return

  updatingStatus.value = true
  try {
    await updateAdminSupportTicketStatus(selectedTicketId.value, {
      status,
      assignToMe: true
    })
    ElMessage.success('工单状态已更新')
    await Promise.all([loadTickets(), loadDetail(selectedTicketId.value)])
  } catch (err) {
    ElMessage.error(resolveErrorMessage(err))
  } finally {
    updatingStatus.value = false
  }
}

async function submitMessage() {
  if (!selectedTicketId.value || !messageForm.content.trim()) {
    ElMessage.warning('请填写回复或备注内容')
    return
  }

  submittingMessage.value = true
  try {
    await createAdminSupportTicketMessage(selectedTicketId.value, {
      messageType: messageForm.messageType,
      content: messageForm.content.trim()
    })
    ElMessage.success(messageForm.messageType === 'internal_note' ? '内部备注已保存' : '公开回复已发送')
    messageForm.content = ''
    await Promise.all([loadTickets(), loadDetail(selectedTicketId.value)])
  } catch (err) {
    ElMessage.error(resolveErrorMessage(err))
  } finally {
    submittingMessage.value = false
  }
}

watch(selectedTicketId, (ticketId) => {
  loadDetail(ticketId)
})

onMounted(loadTickets)
</script>

<template>
  <div class="page-stack admin-support">
    <section class="shell-hero shell-hero--compact admin-support__hero">
      <div>
        <span class="eyebrow">客服工单</span>
        <h1>平台客服工单队列</h1>
        <p>处理用户提交的异步平台客服工单。这里不接入实时聊天，也不直接改变订单、举报、调解或用户状态。</p>
      </div>
      <div class="admin-support__metric">
        <strong>{{ total }}</strong>
        <span>筛选结果</span>
      </div>
    </section>

    <section class="admin-support__quick">
      <router-link v-for="link in quickLinks" :key="link.path" :to="link.path" class="admin-support__quick-link">
        {{ link.label }}
      </router-link>
    </section>

    <section class="admin-support__filters">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索工单号、标题、用户或内容"
        clearable
        @keyup.enter="onFilterChange"
        @clear="onFilterChange"
      />
      <el-select v-model="filters.status" placeholder="状态" clearable @change="onFilterChange">
        <el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="filters.category" placeholder="类型" clearable @change="onFilterChange">
        <el-option v-for="item in categories" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <label class="admin-support__switch">
        <el-switch v-model="filters.assignedToMe" @change="onFilterChange" />
        <span>只看分配给我</span>
      </label>
      <el-button type="primary" :loading="loadingList" @click="onFilterChange">查询</el-button>
    </section>

    <section class="admin-support__workspace">
      <aside class="admin-support__queue">
        <ErrorBlock v-if="error" :message="error" @retry="loadTickets" />
        <SkeletonCard v-else-if="loadingList" :count="4" />
        <EmptyState
          v-else-if="!tickets.length"
          title="暂无客服工单"
          description="当前筛选条件下没有需要处理的工单。"
        />
        <div v-else class="admin-support__ticket-list">
          <button
            v-for="ticket in tickets"
            :key="ticket.id"
            type="button"
            class="admin-support__ticket"
            :class="{ 'is-active': selectedTicketId === ticket.id }"
            @click="selectTicket(ticket)"
          >
            <span class="admin-support__ticket-top">
              <strong>{{ ticket.subject }}</strong>
              <el-tag :type="statusTagType(ticket.status)" effect="plain">{{ statusLabel(ticket.status) }}</el-tag>
            </span>
            <span>{{ categoryLabel(ticket.category) }} · {{ ticketNumber(ticket) }}</span>
            <span>{{ requesterName(ticket) }} · 更新于 {{ formatTime(ticket.updatedAt || ticket.updated_at) }}</span>
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
      </aside>

      <main class="admin-support__detail">
        <ErrorBlock v-if="detailError" :message="detailError" @retry="loadDetail(selectedTicketId)" />
        <SkeletonCard v-else-if="loadingDetail" :count="2" />
        <EmptyState
          v-else-if="!selectedTicket"
          title="请选择一个工单"
          description="选择左侧工单后，可以查看详情、推进状态、公开回复或记录内部备注。"
        />

        <template v-else>
          <header class="admin-support__detail-head">
            <div>
              <span class="eyebrow">工单 {{ ticketNumber(selectedTicket) }}</span>
              <h2>{{ selectedTicket.subject }}</h2>
              <p>
                {{ requesterName(selectedTicket) }} · {{ categoryLabel(selectedTicket.category) }} ·
                创建于 {{ formatTime(selectedTicket.createdAt || selectedTicket.created_at) }}
              </p>
            </div>
            <el-tag :type="statusTagType(selectedTicket.status)" effect="plain">{{ statusLabel(selectedTicket.status) }}</el-tag>
          </header>

          <section class="admin-support__context">
            <div>
              <strong>关联上下文</strong>
              <span v-if="relatedType(selectedTicket) || relatedId(selectedTicket)">
                {{ relatedType(selectedTicket) || '未指定类型' }} #{{ relatedId(selectedTicket) || '未指定编号' }}
              </span>
              <span v-else>用户未关联业务对象</span>
            </div>
            <div class="admin-support__context-links">
              <router-link
                v-for="link in selectedContextLinks(selectedTicket)"
                :key="link.label + link.path"
                :to="link.path"
              >
                {{ link.label }}
              </router-link>
            </div>
          </section>

          <section class="admin-support__content">
            <h3>用户问题</h3>
            <p>{{ selectedTicket.content }}</p>
          </section>

          <section class="admin-support__status">
            <div>
              <h3>状态推进</h3>
              <p>按工单状态模型推进；状态变化只影响客服工单。</p>
            </div>
            <div class="admin-support__status-actions">
              <el-button
                v-for="status in nextStatuses"
                :key="status"
                :loading="updatingStatus"
                @click="changeStatus(status)"
              >
                标记为{{ statusLabel(status) }}
              </el-button>
              <el-tag v-if="!nextStatuses.length" type="info" effect="plain">当前状态无后续动作</el-tag>
            </div>
          </section>

          <section class="admin-support__message-panel">
            <div class="admin-support__thread">
              <h3>公开回复</h3>
              <EmptyState
                v-if="!publicMessages.length"
                title="暂无公开回复"
                description="公开回复会展示给用户。"
              />
              <template v-else>
                <article v-for="message in publicMessages" :key="message.id" class="admin-support__message">
                  <header>
                    <strong>{{ roleLabel(message) }}</strong>
                    <span>{{ formatTime(message.createdAt || message.created_at) }}</span>
                  </header>
                  <p>{{ message.content }}</p>
                </article>
              </template>
            </div>

            <div class="admin-support__thread admin-support__thread--note">
              <h3>内部备注</h3>
              <EmptyState
                v-if="!internalNotes.length"
                title="暂无内部备注"
                description="内部备注仅用于客服交接和审计。"
              />
              <template v-else>
                <article v-for="message in internalNotes" :key="message.id" class="admin-support__message">
                  <header>
                    <strong>{{ roleLabel(message) }}</strong>
                    <span>{{ formatTime(message.createdAt || message.created_at) }}</span>
                  </header>
                  <p>{{ message.content }}</p>
                </article>
              </template>
            </div>
          </section>

          <section class="admin-support__composer">
            <h3>回复或备注</h3>
            <el-alert
              v-if="selectedClosed"
              type="info"
              show-icon
              :closable="false"
              title="该工单已关闭，不能继续回复或备注。"
            />
            <template v-else>
              <el-radio-group v-model="messageForm.messageType">
                <el-radio-button label="public_reply">公开回复</el-radio-button>
                <el-radio-button label="internal_note">内部备注</el-radio-button>
              </el-radio-group>
              <el-input
                v-model="messageForm.content"
                type="textarea"
                :rows="4"
                maxlength="1000"
                show-word-limit
                placeholder="公开回复会展示给用户；内部备注只用于客服处理记录"
              />
              <el-button type="primary" :loading="submittingMessage" @click="submitMessage">
                {{ messageForm.messageType === 'internal_note' ? '保存内部备注' : '发送公开回复' }}
              </el-button>
            </template>
          </section>
        </template>
      </main>
    </section>
  </div>
</template>

<style scoped>
.admin-support__hero,
.admin-support__detail-head,
.admin-support__status,
.admin-support__context {
  align-items: center;
}

.admin-support__metric {
  display: grid;
  min-width: 128px;
  gap: 4px;
  padding: 16px;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
  text-align: center;
}

.admin-support__metric strong {
  font-size: 32px;
  line-height: 1;
}

.admin-support__metric span,
.admin-support__ticket span,
.admin-support__detail-head p,
.admin-support__context span,
.admin-support__status p,
.admin-support__message header span {
  color: var(--cm-text-secondary);
}

.admin-support__quick,
.admin-support__filters,
.admin-support__status-actions,
.admin-support__context-links {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.admin-support__quick-link,
.admin-support__context-links a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
  color: var(--cm-text);
  font-weight: 700;
}

.admin-support__filters {
  align-items: center;
  padding: 14px;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
}

.admin-support__filters :deep(.el-input),
.admin-support__filters :deep(.el-select) {
  width: 220px;
}

.admin-support__switch {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--cm-text-secondary);
  font-weight: 700;
}

.admin-support__workspace {
  display: grid;
  grid-template-columns: minmax(300px, 380px) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.admin-support__queue,
.admin-support__detail,
.admin-support__content,
.admin-support__status,
.admin-support__thread,
.admin-support__composer {
  display: grid;
  gap: 14px;
}

.admin-support__queue,
.admin-support__detail {
  min-width: 0;
  padding: 18px;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--cm-shadow-soft);
}

.admin-support__ticket-list {
  display: grid;
  gap: 10px;
}

.admin-support__ticket {
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

.admin-support__ticket.is-active {
  border-color: rgba(var(--cm-primary-rgb), 0.36);
  background: rgba(var(--cm-primary-rgb), 0.08);
}

.admin-support__ticket-top,
.admin-support__detail-head,
.admin-support__context,
.admin-support__status,
.admin-support__message header {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.admin-support__ticket-top strong,
.admin-support__detail-head h2,
.admin-support__content p,
.admin-support__message p,
.admin-support__context div {
  min-width: 0;
  overflow-wrap: anywhere;
}

.admin-support__detail h2,
.admin-support__detail h3,
.admin-support__detail p {
  margin: 0;
}

.admin-support__context,
.admin-support__content,
.admin-support__status,
.admin-support__thread,
.admin-support__composer {
  padding: 14px;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.68);
}

.admin-support__content {
  background: rgba(var(--cm-primary-rgb), 0.06);
}

.admin-support__message-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 14px;
}

.admin-support__message {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.74);
}

.admin-support__thread--note .admin-support__message {
  background: rgba(var(--cm-primary-rgb), 0.06);
}

@media (max-width: 1080px) {
  .admin-support__workspace,
  .admin-support__message-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .admin-support__filters :deep(.el-input),
  .admin-support__filters :deep(.el-select),
  .admin-support__filters :deep(.el-button),
  .admin-support__metric {
    width: 100%;
  }

  .admin-support__ticket-top,
  .admin-support__detail-head,
  .admin-support__context,
  .admin-support__status,
  .admin-support__message header {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
