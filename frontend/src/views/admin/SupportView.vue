<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from '@/plugins/element-plus-services'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import QuickReplyPanel from '@/components/chat/QuickReplyPanel.vue'
import { useAuthStore } from '@/stores/auth'
import {
  claimAdminSupportChatConversation,
  closeAdminSupportChatConversation,
  createAdminSupportTicketMessage,
  getAdminSupportChatConversation,
  getAdminSupportChatConversations,
  getAdminSupportChatMessages,
  getAdminSupportTicketDetail,
  getAdminSupportTickets,
  markAdminSupportChatRead,
  replyAdminSupportChatConversation,
  updateAdminSupportTicketStatus
} from '@/api/modules/admin'
import { resolveErrorMessage } from '@/utils/error-utils'
import {
  POLL_INTERVAL,
  chatFilterOptions,
  statusMeta,
  ticketCategoryOptions,
  ticketMessageTypeOptions,
  ticketStatusOptions,
  workspaceDisplayMeta,
  workspaces
} from './support-view-options'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const workspace = ref(normalizeWorkspace(route.query.lane))

const activeChatFilter = ref('pending')
const conversations = ref([])
const counts = ref({ pending: 0, active: 0, mine: 0, closed: 0 })
const selectedConversationId = ref(null)
const conversationDetail = ref(null)
const conversationMessages = ref([])
const replyContent = ref('')
const quickReplyOpen = ref(false)
const loadingChatList = ref(false)
const loadingChatDetail = ref(false)
const sendingChatReply = ref(false)
const claimingChat = ref(false)
const closingChat = ref(false)
const chatError = ref('')
const chatDetailError = ref('')
const threadRef = ref(null)
let pollingTimer = null

const ticketFilters = reactive({
  status: normalizeTicketStatus(route.query.status),
  category: normalizeTicketCategory(route.query.category),
  assignedToMe: route.query.assignedToMe === 'true',
  keyword: normalizeQueryString(route.query.keyword),
  page: 1,
  pageSize: 10
})
const tickets = ref([])
const ticketTotal = ref(0)
const selectedTicketId = ref(null)
const ticketDetail = ref(null)
const ticketMessages = ref([])
const ticketStatusForm = reactive({
  status: '',
  assignToMe: false
})
const ticketReplyForm = reactive({
  messageType: 'public_reply',
  content: ''
})

const loadingTicketList = ref(false)
const loadingTicketDetail = ref(false)
const updatingTicketStatus = ref(false)
const submittingTicketMessage = ref(false)
const ticketError = ref('')
const ticketDetailError = ref('')

const currentAdminId = computed(() => authStore.currentUser?.id ?? null)
const currentWorkspaceMeta = computed(
  () => workspaceDisplayMeta[workspace.value] || workspaceDisplayMeta.chat
)
const selectedChatClosed = computed(() => conversationDetail.value?.supportStatus === 'closed')
const assignedToOther = computed(() => {
  const assigned = conversationDetail.value?.assignedAdminId
  return assigned != null && String(assigned) !== String(currentAdminId.value)
})
const canReplyChat = computed(() => conversationDetail.value && !selectedChatClosed.value && !assignedToOther.value)
const requester = computed(() => conversationDetail.value?.requester || {})
const ticketClosed = computed(() => ticketDetail.value?.ticket?.status === 'closed')
const ticketSummary = computed(() => ticketDetail.value?.ticket || null)
const ticketStatusChoices = computed(() => {
  const current = ticketSummary.value?.status || ''
  const allowed = {
    open: ['in_progress', 'closed'],
    in_progress: ['waiting_user', 'resolved', 'closed'],
    waiting_user: ['in_progress', 'resolved', 'closed'],
    resolved: ['closed'],
    closed: []
  }[current] || []
  return [current, ...allowed]
    .filter(Boolean)
    .map((value) => ({
      value,
      label: displayStatusLabel(value)
    }))
})
const ticketContextLinks = computed(() => {
  const ticket = ticketSummary.value
  if (!ticket) return []
  const links = []
  if (ticket.requesterUserId) {
    links.push({
      label: '用户资料',
      path: `/admin/users?keyword=${encodeURIComponent(ticket.requesterUserId)}`
    })
  }
  if (ticket.relatedType === 'order' && ticket.relatedId) {
    links.push({
      label: '订单处理',
      path: `/admin/orders?keyword=${encodeURIComponent(ticket.relatedId)}`
    })
    links.push({
      label: '调解案件',
      path: `/admin/mediation?orderId=${encodeURIComponent(ticket.relatedId)}`
    })
  }
  if (ticket.relatedType === 'report' && ticket.relatedId) {
    links.push({
      label: '举报详情',
      path: `/admin/reports?keyword=${encodeURIComponent(ticket.relatedId)}`
    })
    links.push({
      label: '调解案件',
      path: `/admin/mediation?reportId=${encodeURIComponent(ticket.relatedId)}`
    })
  }
  if (ticket.relatedType === 'product' && ticket.relatedId) {
    links.push({
      label: '商品治理',
      path: `/admin/products?keyword=${encodeURIComponent(ticket.relatedId)}`
    })
  }
  if (ticket.relatedType === 'shop' && ticket.relatedId) {
    links.push({
      label: '店铺治理',
      path: `/admin/shops?keyword=${encodeURIComponent(ticket.relatedId)}`
    })
  }
  return links
})

function normalizeWorkspace(value) {
  const candidate = Array.isArray(value) ? value[0] : value
  return ['chat', 'tickets'].includes(candidate) ? candidate : 'chat'
}

function normalizeTicketStatus(value) {
  const candidate = Array.isArray(value) ? value[0] : value
  return ticketStatusOptions.some((item) => item.value === candidate) ? candidate : ''
}

function normalizeTicketCategory(value) {
  const candidate = Array.isArray(value) ? value[0] : value
  return ticketCategoryOptions.some((item) => item.value === candidate) ? candidate : ''
}

function normalizeQueryString(value) {
  const candidate = Array.isArray(value) ? value[0] : value
  return candidate ? String(candidate) : ''
}

function unwrap(response) {
  return response?.data ?? response
}

function statusLabel(status) {
  return statusMeta[status]?.label || status || '未知状态'
}

function statusTagType(status) {
  return statusMeta[status]?.type || 'info'
}

function displayStatusLabel(status) {
  return {
    ai: '智能客服',
    pending: '待接入',
    human: '处理中',
    closed: '已结束',
    open: '待受理',
    in_progress: '处理中',
    waiting_user: '待用户补充',
    resolved: '已解决'
  }[status] || status || '未知状态'
}

function displayTicketCategory(category) {
  return ticketCategoryOptions.find((item) => item.value === category)?.label || category || '未分类'
}

function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const now = new Date()
  if (now - date < 24 * 60 * 60 * 1000) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function unreadFor(item) {
  const count = Number(item.unreadCount ?? item.unread_count ?? 0)
  if (!Number.isFinite(count) || count <= 0) return ''
  return count > 99 ? '99+' : String(count)
}

function requesterName(item) {
  const user = item?.requester || item || {}
  return user.nickname || user.username || `用户 ${user.id ?? '-'}`
}

function isPlatformMessage(message) {
  const requesterId = requester.value?.id
  return requesterId != null && String(message.senderUserId) !== String(requesterId)
}

function normalizeMessage(raw = {}) {
  return {
    id: raw.id,
    senderUserId: raw.senderUserId ?? raw.sender_user_id,
    body: raw.body ?? '',
    messageType: raw.messageType ?? raw.message_type ?? 'text',
    isRecalled: Boolean(raw.isRecalled ?? raw.is_recalled),
    createdAt: raw.createdAt ?? raw.created_at
  }
}

function normalizeTicketPage(payload) {
  const data = payload || {}
  return {
    items: Array.isArray(data.items) ? data.items : [],
    total: Number(data.total ?? 0),
    page: Number(data.page ?? 1),
    pageSize: Number(data.pageSize ?? 10)
  }
}

function normalizeTicketDetail(payload) {
  return {
    ticket: payload?.ticket || payload || null,
    messages: Array.isArray(payload?.messages) ? payload.messages : []
  }
}

function updateLaneQuery(nextLane) {
  router.replace({
    query: {
      ...route.query,
      lane: nextLane
    }
  })
}

function updateTicketQuery() {
  router.replace({
    query: {
      ...route.query,
      lane: workspace.value,
      status: ticketFilters.status || undefined,
      category: ticketFilters.category || undefined,
      assignedToMe: ticketFilters.assignedToMe ? 'true' : undefined,
      keyword: ticketFilters.keyword || undefined
    }
  })
}

async function loadConversations(options = {}) {
  if (!options.silent) loadingChatList.value = true
  chatError.value = ''
  try {
    const payload = unwrap(
      await getAdminSupportChatConversations({ filter: activeChatFilter.value, page: 0, size: 50 })
    )
    conversations.value = payload?.content ?? []
    counts.value = { ...counts.value, ...(payload?.counts ?? {}) }
    if (!selectedConversationId.value && conversations.value.length) {
      selectConversation(conversations.value[0].id)
    }
  } catch (error) {
    chatError.value = resolveErrorMessage(error)
  } finally {
    if (!options.silent) loadingChatList.value = false
  }
}

async function loadConversationDetail(conversationId, options = {}) {
  if (!conversationId) {
    conversationDetail.value = null
    conversationMessages.value = []
    return
  }
  if (!options.silent) loadingChatDetail.value = true
  chatDetailError.value = ''
  try {
    const [detailPayload, messagesPayload] = await Promise.all([
      getAdminSupportChatConversation(conversationId),
      getAdminSupportChatMessages(conversationId, { page: 0, size: 50 })
    ])
    conversationDetail.value = unwrap(detailPayload)
    conversationMessages.value = (unwrap(messagesPayload)?.content ?? []).map(normalizeMessage).reverse()
    await markAdminSupportChatRead(conversationId).catch(() => {})
    await nextTick()
    scrollToBottom()
  } catch (error) {
    if (!options.silent) chatDetailError.value = resolveErrorMessage(error)
  } finally {
    if (!options.silent) loadingChatDetail.value = false
  }
}

async function loadTickets() {
  loadingTicketList.value = true
  ticketError.value = ''
  try {
    const payload = unwrap(
      await getAdminSupportTickets({
        status: ticketFilters.status,
        category: ticketFilters.category,
        assignedToMe: ticketFilters.assignedToMe,
        keyword: ticketFilters.keyword,
        page: ticketFilters.page,
        pageSize: ticketFilters.pageSize
      })
    )
    const page = normalizeTicketPage(payload)
    tickets.value = page.items
    ticketTotal.value = page.total
    ticketFilters.page = page.page
    ticketFilters.pageSize = page.pageSize
    if (!selectedTicketId.value && tickets.value.length) {
      selectTicket(tickets.value[0].id)
    }
  } catch (error) {
    ticketError.value = resolveErrorMessage(error)
  } finally {
    loadingTicketList.value = false
  }
}

async function loadTicketDetail(ticketId) {
  if (!ticketId) {
    ticketDetail.value = null
    ticketMessages.value = []
    return
  }
  loadingTicketDetail.value = true
  ticketDetailError.value = ''
  try {
    const payload = unwrap(await getAdminSupportTicketDetail(ticketId))
    const detail = normalizeTicketDetail(payload)
    ticketDetail.value = detail
    ticketMessages.value = detail.messages
    ticketStatusForm.status = detail.ticket?.status || ''
    ticketStatusForm.assignToMe = false
  } catch (error) {
    ticketDetailError.value = resolveErrorMessage(error)
  } finally {
    loadingTicketDetail.value = false
  }
}

function selectConversation(id) {
  selectedConversationId.value = id
  quickReplyOpen.value = false
}

function selectTicket(id) {
  selectedTicketId.value = id
}

function switchWorkspace(nextLane) {
  if (workspace.value === nextLane) return
  workspace.value = nextLane
  updateLaneQuery(nextLane)
}

function changeChatFilter(value) {
  activeChatFilter.value = value
  selectedConversationId.value = null
  conversationDetail.value = null
  conversationMessages.value = []
  loadConversations()
}

async function claim() {
  if (!selectedConversationId.value || claimingChat.value) return
  claimingChat.value = true
  try {
    conversationDetail.value = unwrap(await claimAdminSupportChatConversation(selectedConversationId.value))
    ElMessage.success('已接入该会话')
    await Promise.all([
      loadConversations({ silent: true }),
      loadConversationDetail(selectedConversationId.value, { silent: true })
    ])
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    claimingChat.value = false
  }
}

async function closeConversation() {
  if (!selectedConversationId.value || closingChat.value) return
  closingChat.value = true
  try {
    conversationDetail.value = unwrap(await closeAdminSupportChatConversation(selectedConversationId.value))
    ElMessage.success('会话已结束')
    await loadConversations({ silent: true })
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    closingChat.value = false
  }
}

async function submitChatReply() {
  const body = replyContent.value.trim()
  if (!body || !selectedConversationId.value || sendingChatReply.value) return
  sendingChatReply.value = true
  try {
    await replyAdminSupportChatConversation(selectedConversationId.value, { body })
    replyContent.value = ''
    quickReplyOpen.value = false
    await Promise.all([
      loadConversationDetail(selectedConversationId.value, { silent: true }),
      loadConversations({ silent: true })
    ])
    scrollToBottom()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    sendingChatReply.value = false
  }
}

async function applyTicketFilters() {
  ticketFilters.page = 1
  selectedTicketId.value = null
  updateTicketQuery()
  await loadTickets()
}

async function updateTicketStatus() {
  if (!selectedTicketId.value || !ticketStatusForm.status || updatingTicketStatus.value) return
  updatingTicketStatus.value = true
  try {
    await updateAdminSupportTicketStatus(selectedTicketId.value, {
      status: ticketStatusForm.status,
      assignToMe: ticketStatusForm.assignToMe
    })
    ElMessage.success('工单状态已更新')
    ticketStatusForm.assignToMe = false
    await Promise.all([loadTickets(), loadTicketDetail(selectedTicketId.value)])
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    updatingTicketStatus.value = false
  }
}

async function submitTicketMessage() {
  if (!selectedTicketId.value || !ticketReplyForm.content.trim() || submittingTicketMessage.value) return
  submittingTicketMessage.value = true
  try {
    await createAdminSupportTicketMessage(selectedTicketId.value, {
      messageType: ticketReplyForm.messageType,
      content: ticketReplyForm.content.trim()
    })
    ticketReplyForm.content = ''
    ElMessage.success(ticketReplyForm.messageType === 'internal_note' ? '内部备注已保存' : '公开回复已发送')
    await Promise.all([loadTickets(), loadTicketDetail(selectedTicketId.value)])
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    submittingTicketMessage.value = false
  }
}

function scrollToBottom() {
  requestAnimationFrame(() => {
    if (threadRef.value) {
      threadRef.value.scrollTop = threadRef.value.scrollHeight
    }
  })
}

function startPolling() {
  stopPolling()
  pollingTimer = window.setInterval(() => {
    if (workspace.value !== 'chat') return
    loadConversations({ silent: true })
    if (selectedConversationId.value) {
      loadConversationDetail(selectedConversationId.value, { silent: true })
    }
  }, POLL_INTERVAL)
}

function stopPolling() {
  if (pollingTimer) {
    window.clearInterval(pollingTimer)
    pollingTimer = null
  }
}

watch(selectedConversationId, (id) => {
  if (workspace.value === 'chat') {
    loadConversationDetail(id)
  }
})

watch(selectedTicketId, (id) => {
  if (workspace.value === 'tickets') {
    loadTicketDetail(id)
  }
})

watch(workspace, async (nextLane) => {
  if (nextLane === 'chat') {
    startPolling()
    if (!conversations.value.length) {
      await loadConversations()
    } else if (selectedConversationId.value) {
      await loadConversationDetail(selectedConversationId.value)
    }
  } else {
    stopPolling()
    if (!tickets.value.length) {
      await loadTickets()
    } else if (selectedTicketId.value) {
      await loadTicketDetail(selectedTicketId.value)
    }
  }
})

onMounted(async () => {
  if (workspace.value === 'tickets') {
    await loadTickets()
  } else {
    await loadConversations()
    startPolling()
  }
})

onBeforeUnmount(stopPolling)
</script>

<template>
  <div class="page-stack admin-support">
    <section class="shell-hero shell-hero--compact admin-support__hero">
      <div>
        <span class="eyebrow">客服运营工作台</span>
        <h1>客服接待与工单跟进</h1>
        <p>{{ currentWorkspaceMeta.description }}</p>
      </div>
      <div class="admin-support__hero-actions">
        <button
          v-for="item in workspaces"
          :key="item.key"
          type="button"
          class="admin-support__workspace-chip"
          :class="{ 'is-active': workspace === item.key }"
          @click="switchWorkspace(item.key)"
        >
          <strong>{{ workspaceDisplayMeta[item.key]?.title || item.title }}</strong>
          <span>{{ workspaceDisplayMeta[item.key]?.eyebrow || item.eyebrow }}</span>
        </button>
      </div>
    </section>

    <section v-if="workspace === 'chat'" class="admin-cs__workspace">
      <aside class="admin-cs__queue ui-surface-panel ui-stack-card">
        <div class="admin-cs__filters">
          <button
            v-for="item in chatFilterOptions"
            :key="item.value"
            type="button"
            class="admin-cs__filter"
            :class="{ 'is-active': activeChatFilter === item.value }"
            @click="changeChatFilter(item.value)"
          >
            {{ item.label }}
            <span v-if="item.value === 'pending' && counts.pending" class="admin-cs__filter-badge">{{ counts.pending }}</span>
          </button>
        </div>

        <ErrorBlock v-if="chatError" :message="chatError" @retry="loadConversations" />
        <SkeletonCard v-else-if="loadingChatList" :count="4" />
        <EmptyState
          v-else-if="!conversations.length"
              title="暂无在线客服会话"
              description="当前筛选条件下没有需要接待的在线咨询。"
        />
        <div v-else class="admin-cs__list">
          <button
            v-for="item in conversations"
            :key="item.id"
            type="button"
            class="admin-cs__session"
            :class="{ 'is-active': String(selectedConversationId) === String(item.id) }"
            @click="selectConversation(item.id)"
          >
            <span class="admin-cs__session-top">
              <strong>{{ requesterName(item) }}</strong>
              <el-tag :type="statusTagType(item.supportStatus)" size="small" effect="plain">
                {{ displayStatusLabel(item.supportStatus) }}
              </el-tag>
            </span>
            <span class="admin-cs__session-preview">{{ item.lastMessagePreview || '开始对话' }}</span>
            <span class="admin-cs__session-foot">
              <span>{{ formatTime(item.lastMessageAt) }}</span>
              <span v-if="unreadFor(item)" class="admin-cs__session-unread">{{ unreadFor(item) }}</span>
            </span>
          </button>
        </div>
      </aside>

      <main class="admin-cs__thread ui-surface-panel">
        <ErrorBlock v-if="chatDetailError" :message="chatDetailError" @retry="loadConversationDetail(selectedConversationId)" />
        <SkeletonCard v-else-if="loadingChatDetail && !conversationMessages.length" :count="3" />
        <EmptyState
          v-else-if="!conversationDetail"
          title="请选择一个在线客服会话"
          description="从左侧队列选择会话后即可查看对话并接入处理。"
        />
        <template v-else>
          <header class="admin-cs__thread-head">
            <div>
              <h2>{{ requesterName(conversationDetail) }}</h2>
              <el-tag :type="statusTagType(conversationDetail.supportStatus)" size="small" effect="plain">
                {{ displayStatusLabel(conversationDetail.supportStatus) }}
              </el-tag>
            </div>
          </header>

          <div ref="threadRef" class="admin-cs__messages">
            <EmptyState v-if="!conversationMessages.length" title="暂无消息" description="该会话还没有消息记录。" />
            <article
              v-for="message in conversationMessages"
              v-else
              :key="message.id"
              class="admin-cs__bubble"
              :class="isPlatformMessage(message) ? 'admin-cs__bubble--self' : 'admin-cs__bubble--other'"
            >
              <p v-if="message.isRecalled" class="admin-cs__bubble-recalled">该消息已撤回</p>
              <p v-else>{{ message.body }}</p>
              <time>{{ formatTime(message.createdAt) }}</time>
            </article>
          </div>

          <div class="admin-cs__composer">
            <el-alert
              v-if="selectedChatClosed"
              type="info"
              show-icon
              :closable="false"
              title="该会话已结束，不能继续回复。"
            />
            <el-alert
              v-else-if="assignedToOther"
              type="warning"
              show-icon
              :closable="false"
              title="该会话由其他客服处理，请勿重复接入。"
            />
            <template v-else>
              <QuickReplyPanel
                v-if="quickReplyOpen"
                class="admin-cs__quick-reply"
                scenario="support"
                @select="replyContent = $event; quickReplyOpen = false"
              />
              <el-input
                v-model="replyContent"
                type="textarea"
                :rows="3"
                maxlength="2000"
                show-word-limit
                placeholder="以平台客服身份回复用户"
              />
              <div class="admin-cs__composer-actions">
                <el-button text @click="quickReplyOpen = !quickReplyOpen">快捷话术</el-button>
                <el-button type="primary" :loading="sendingChatReply" :disabled="!canReplyChat" @click="submitChatReply">
                  发送回复
                </el-button>
              </div>
            </template>
          </div>
        </template>
      </main>

      <aside v-if="conversationDetail" class="admin-cs__context ui-surface-panel ui-stack-card">
        <section class="admin-cs__panel">
          <h3>请求者</h3>
          <p class="admin-cs__requester">{{ requesterName(conversationDetail) }}</p>
          <p class="admin-cs__requester-sub">用户编号：{{ requester.id || '-' }}</p>
        </section>

        <section class="admin-cs__panel">
          <h3>快捷跳转</h3>
          <div class="admin-cs__links">
            <router-link :to="`/admin/users?keyword=${encodeURIComponent(requester.id || '')}`">用户资料</router-link>
            <router-link :to="`/admin/orders?keyword=${encodeURIComponent(requester.id || '')}`">订单履约</router-link>
            <router-link :to="`/admin/reports?keyword=${encodeURIComponent(requester.id || '')}`">举报处置</router-link>
            <router-link to="/admin/mediation">调解案件</router-link>
          </div>
        </section>

        <section class="admin-cs__panel">
          <h3>会话操作</h3>
          <div class="admin-cs__actions">
            <el-button
              v-if="conversationDetail.supportStatus !== 'human' || assignedToOther"
              type="primary"
              :loading="claimingChat"
              :disabled="selectedChatClosed"
              @click="claim"
            >
              认领 / 接入
            </el-button>
            <el-button
              :loading="closingChat"
              :disabled="selectedChatClosed || assignedToOther"
              @click="closeConversation"
            >
              结束会话
            </el-button>
          </div>
        </section>
      </aside>
    </section>

    <section v-else class="ticket-workspace">
      <aside class="ticket-workspace__queue">
        <section class="ticket-card ui-surface-panel ui-stack-card">
          <header class="ticket-card__head">
            <div>
              <h2>支持工单队列</h2>
              <p>处理用户提交的问题单，持续跟进材料补充、进度同步和处理结果。</p>
            </div>
            <strong>{{ ticketTotal }}</strong>
          </header>

          <div class="ticket-filter-grid">
            <el-select v-model="ticketFilters.status" placeholder="状态" @change="applyTicketFilters">
              <el-option v-for="item in ticketStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="ticketFilters.category" placeholder="分类" @change="applyTicketFilters">
              <el-option v-for="item in ticketCategoryOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-checkbox v-model="ticketFilters.assignedToMe" @change="applyTicketFilters">仅看指派给我</el-checkbox>
            <div class="ticket-filter-grid__keyword">
              <el-input
                v-model="ticketFilters.keyword"
                placeholder="搜索工单号、标题或描述"
                @keyup.enter="applyTicketFilters"
              />
              <el-button type="primary" @click="applyTicketFilters">筛选</el-button>
            </div>
          </div>

          <ErrorBlock v-if="ticketError" :message="ticketError" @retry="loadTickets" />
          <SkeletonCard v-else-if="loadingTicketList" :count="4" />
          <EmptyState
            v-else-if="!tickets.length"
            title="暂无支持工单"
            description="当前筛选条件下没有匹配的工单。"
          />
          <div v-else class="ticket-list">
            <button
              v-for="item in tickets"
              :key="item.id"
              type="button"
              class="ticket-list__item"
              :class="{ 'is-active': String(selectedTicketId) === String(item.id) }"
              @click="selectTicket(item.id)"
            >
              <span class="ticket-list__top">
                <strong>{{ item.subject }}</strong>
                <el-tag :type="statusTagType(item.status)" size="small" effect="plain">
                  {{ displayStatusLabel(item.status) }}
                </el-tag>
              </span>
              <span class="ticket-list__meta">
                {{ item.ticketNo || `#${item.id}` }} / {{ displayTicketCategory(item.category) }} / {{ item.requesterName || `用户 ${item.requesterUserId}` }}
              </span>
              <span class="ticket-list__time">更新于 {{ formatTime(item.updatedAt) }}</span>
            </button>
          </div>

          <el-pagination
            v-if="ticketTotal > ticketFilters.pageSize"
            v-model:current-page="ticketFilters.page"
            :page-size="ticketFilters.pageSize"
            :total="ticketTotal"
            layout="prev, pager, next"
            small
            background
            @current-change="loadTickets"
          />
        </section>
      </aside>

      <main class="ticket-card ticket-detail ui-surface-panel ui-stack-card">
        <ErrorBlock v-if="ticketDetailError" :message="ticketDetailError" @retry="loadTicketDetail(selectedTicketId)" />
        <SkeletonCard v-else-if="loadingTicketDetail" :count="3" />
        <EmptyState
          v-else-if="!ticketSummary"
          title="请选择一个支持工单"
          description="从左侧工单队列选择一条工单后即可查看详情、变更状态并回复。"
        />
        <template v-else>
          <header class="ticket-detail__head">
            <div>
              <span class="eyebrow">工单 {{ ticketSummary.ticketNo || `#${ticketSummary.id}` }}</span>
              <h2>{{ ticketSummary.subject }}</h2>
              <p>分类：{{ displayTicketCategory(ticketSummary.category) }} / 请求人：{{ ticketSummary.requesterName || ticketSummary.requesterUserId }}</p>
            </div>
            <el-tag :type="statusTagType(ticketSummary.status)" effect="plain">{{ displayStatusLabel(ticketSummary.status) }}</el-tag>
          </header>

          <section class="ticket-detail__content">
            <h3>问题说明</h3>
            <p>{{ ticketSummary.content }}</p>
          </section>

          <section class="ticket-detail__grid">
            <article class="ticket-detail__panel">
              <h3>状态处理</h3>
              <el-select v-model="ticketStatusForm.status" :disabled="ticketClosed">
                <el-option
                  v-for="option in ticketStatusChoices"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
              <el-checkbox v-model="ticketStatusForm.assignToMe" :disabled="ticketClosed">指派给我</el-checkbox>
              <el-button type="primary" :loading="updatingTicketStatus" :disabled="ticketClosed" @click="updateTicketStatus">
                更新工单状态
              </el-button>
            </article>

            <article class="ticket-detail__panel">
              <h3>上下文跳转</h3>
              <div class="ticket-detail__links">
                <router-link v-for="link in ticketContextLinks" :key="link.path" :to="link.path">{{ link.label }}</router-link>
              </div>
              <p v-if="!ticketContextLinks.length" class="ticket-detail__muted">该工单当前没有可跳转的关联上下文。</p>
            </article>
          </section>

          <section class="ticket-messages">
            <h3>工单消息</h3>
            <EmptyState
              v-if="!ticketMessages.length"
              title="暂无工单消息"
              description="工单创建后，公开回复和内部备注会显示在这里。"
            />
            <article
              v-for="message in ticketMessages"
              v-else
              :key="message.id"
              class="ticket-message"
              :class="{ 'is-internal': message.messageType === 'internal_note' }"
            >
              <header>
                <strong>{{ message.senderName || message.senderRole || '平台' }}</strong>
                <span>{{ message.messageType === 'internal_note' ? '内部备注' : '公开回复' }} / {{ formatTime(message.createdAt) }}</span>
              </header>
              <p>{{ message.content }}</p>
            </article>
          </section>

          <section class="ticket-reply">
            <h3>新增回复 / 备注</h3>
            <el-alert
              v-if="ticketClosed"
              type="info"
              show-icon
              :closable="false"
              title="该工单已关闭，不能继续回复或添加备注。"
            />
            <template v-else>
              <el-radio-group v-model="ticketReplyForm.messageType">
              <el-radio-button v-for="item in ticketMessageTypeOptions" :key="item.value" :label="item.value">
                {{ item.label }}
              </el-radio-button>
              </el-radio-group>
              <el-input
                v-model="ticketReplyForm.content"
                type="textarea"
                :rows="4"
                maxlength="2000"
                show-word-limit
                placeholder="公开回复会同步给用户；内部备注仅供平台处理团队查看。"
              />
              <el-button type="primary" :loading="submittingTicketMessage" @click="submitTicketMessage">
                提交{{ ticketReplyForm.messageType === 'internal_note' ? '内部备注' : '公开回复' }}
              </el-button>
            </template>
          </section>
        </template>
      </main>
    </section>
  </div>
</template>

<style scoped src="./SupportView.css"></style>
