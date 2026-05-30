<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import QuickReplyPanel from '@/components/chat/QuickReplyPanel.vue'
import { useAuthStore } from '@/stores/auth'
import {
  claimAdminSupportChatConversation,
  closeAdminSupportChatConversation,
  getAdminSupportChatConversation,
  getAdminSupportChatConversations,
  getAdminSupportChatMessages,
  markAdminSupportChatRead,
  replyAdminSupportChatConversation
} from '@/api/modules/admin'
import { resolveErrorMessage } from '@/utils/error-utils'

const POLL_INTERVAL = 8000

const filters = [
  { value: 'pending', label: '待接入' },
  { value: 'active', label: '进行中' },
  { value: 'mine', label: '我处理的' },
  { value: 'closed', label: '已结束' }
]

const statusMeta = {
  ai: { label: '智能客服', type: 'info' },
  pending: { label: '待接入', type: 'warning' },
  human: { label: '进行中', type: 'primary' },
  closed: { label: '已结束', type: 'info' }
}

const authStore = useAuthStore()

const activeFilter = ref('pending')
const conversations = ref([])
const counts = ref({ pending: 0, active: 0, mine: 0, closed: 0 })
const selectedId = ref(null)
const detail = ref(null)
const messages = ref([])
const replyContent = ref('')
const quickReplyOpen = ref(false)

const loadingList = ref(false)
const loadingThread = ref(false)
const submitting = ref(false)
const claiming = ref(false)
const closing = ref(false)
const error = ref('')
const threadError = ref('')
const threadRef = ref(null)
let pollingTimer = null

const currentAdminId = computed(() => authStore.currentUser?.id ?? null)
const selectedClosed = computed(() => detail.value?.supportStatus === 'closed')
const assignedToOther = computed(() => {
  const assigned = detail.value?.assignedAdminId
  return assigned != null && String(assigned) !== String(currentAdminId.value)
})
const canReply = computed(() => detail.value && !selectedClosed.value && !assignedToOther.value)

const requester = computed(() => detail.value?.requester || {})
const contextLinks = computed(() => {
  const id = requester.value?.id
  if (!id) return []
  return [
    { label: '用户资料', path: `/admin/users?keyword=${encodeURIComponent(id)}` },
    { label: '订单履约', path: `/admin/orders?keyword=${encodeURIComponent(id)}` },
    { label: '举报处置', path: `/admin/reports?keyword=${encodeURIComponent(id)}` },
    { label: '调解案件', path: '/admin/mediation' }
  ]
})

function requesterName(item) {
  const user = item?.requester || item || {}
  return user.nickname || user.username || `用户 #${user.id ?? '-'}`
}

function statusLabel(status) {
  return statusMeta[status]?.label || '在线客服'
}

function statusTagType(status) {
  return statusMeta[status]?.type || 'info'
}

function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const now = new Date()
  if (now - date < 24 * 60 * 60 * 1000) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
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

function unwrap(response) {
  return response?.data ?? response
}

function unreadFor(item) {
  const count = Number(item.unreadCount ?? item.unread_count ?? 0)
  if (!Number.isFinite(count) || count <= 0) return ''
  return count > 99 ? '99+' : String(count)
}

function isPlatformMessage(message) {
  const requesterId = requester.value?.id
  return requesterId != null && String(message.senderUserId) !== String(requesterId)
}

async function loadConversations(options = {}) {
  if (!options.silent) loadingList.value = true
  error.value = ''
  try {
    const payload = unwrap(await getAdminSupportChatConversations({ filter: activeFilter.value, page: 0, size: 50 }))
    conversations.value = payload?.content ?? []
    counts.value = { ...counts.value, ...(payload?.counts ?? {}) }
    if (!selectedId.value && conversations.value.length) {
      selectConversation(conversations.value[0].id)
    }
  } catch (err) {
    error.value = resolveErrorMessage(err)
  } finally {
    if (!options.silent) loadingList.value = false
  }
}

async function loadThread(conversationId, options = {}) {
  if (!conversationId) {
    detail.value = null
    messages.value = []
    return
  }
  if (!options.silent) loadingThread.value = true
  threadError.value = ''
  try {
    const [detailPayload, messagesPayload] = await Promise.all([
      getAdminSupportChatConversation(conversationId),
      getAdminSupportChatMessages(conversationId, { page: 0, size: 50 })
    ])
    detail.value = unwrap(detailPayload)
    messages.value = (unwrap(messagesPayload)?.content ?? []).map(normalizeMessage).reverse()
    await markAdminSupportChatRead(conversationId).catch(() => {})
    scrollToBottom()
  } catch (err) {
    if (!options.silent) threadError.value = resolveErrorMessage(err)
  } finally {
    if (!options.silent) loadingThread.value = false
  }
}

function selectConversation(id) {
  selectedId.value = id
  quickReplyOpen.value = false
}

function changeFilter(value) {
  activeFilter.value = value
  selectedId.value = null
  detail.value = null
  messages.value = []
  loadConversations()
}

async function claim() {
  if (!selectedId.value || claiming.value) return
  claiming.value = true
  try {
    detail.value = unwrap(await claimAdminSupportChatConversation(selectedId.value))
    ElMessage.success('已接入该会话')
    await Promise.all([loadConversations({ silent: true }), loadThread(selectedId.value, { silent: true })])
  } catch (err) {
    ElMessage.error(resolveErrorMessage(err))
  } finally {
    claiming.value = false
  }
}

async function closeConversation() {
  if (!selectedId.value || closing.value) return
  closing.value = true
  try {
    detail.value = unwrap(await closeAdminSupportChatConversation(selectedId.value))
    ElMessage.success('会话已结束')
    await loadConversations({ silent: true })
  } catch (err) {
    ElMessage.error(resolveErrorMessage(err))
  } finally {
    closing.value = false
  }
}

async function submitReply() {
  const body = replyContent.value.trim()
  if (!body || !selectedId.value || submitting.value) return
  submitting.value = true
  try {
    await replyAdminSupportChatConversation(selectedId.value, { body })
    replyContent.value = ''
    quickReplyOpen.value = false
    await Promise.all([loadThread(selectedId.value, { silent: true }), loadConversations({ silent: true })])
    scrollToBottom()
  } catch (err) {
    ElMessage.error(resolveErrorMessage(err))
  } finally {
    submitting.value = false
  }
}

function applyQuickReply(content) {
  replyContent.value = content
  quickReplyOpen.value = false
}

function scrollToBottom() {
  requestAnimationFrame(() => {
    if (threadRef.value) threadRef.value.scrollTop = threadRef.value.scrollHeight
  })
}

function startPolling() {
  stopPolling()
  pollingTimer = setInterval(() => {
    loadConversations({ silent: true })
    if (selectedId.value) loadThread(selectedId.value, { silent: true })
  }, POLL_INTERVAL)
}

function stopPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

watch(selectedId, (id) => {
  loadThread(id)
})

onMounted(async () => {
  await loadConversations()
  startPolling()
})

onBeforeUnmount(stopPolling)
</script>

<template>
  <div class="page-stack admin-cs">
    <section class="shell-hero shell-hero--compact admin-cs__hero">
      <div>
        <span class="eyebrow">在线客服</span>
        <h1>在线客服控制台</h1>
        <p>实时接待用户的客服会话：认领待接入会话、以平台客服身份回复，并在解决后结束会话。</p>
      </div>
      <div class="admin-cs__metrics">
        <div class="admin-cs__metric">
          <strong>{{ counts.pending }}</strong>
          <span>待接入</span>
        </div>
        <div class="admin-cs__metric">
          <strong>{{ counts.mine }}</strong>
          <span>我处理的</span>
        </div>
      </div>
    </section>

    <section class="admin-cs__workspace">
      <aside class="admin-cs__queue">
        <div class="admin-cs__filters">
          <button
            v-for="item in filters"
            :key="item.value"
            type="button"
            class="admin-cs__filter"
            :class="{ 'is-active': activeFilter === item.value }"
            @click="changeFilter(item.value)"
          >
            {{ item.label }}
            <span v-if="item.value === 'pending' && counts.pending" class="admin-cs__filter-badge">{{ counts.pending }}</span>
          </button>
        </div>

        <ErrorBlock v-if="error" :message="error" @retry="loadConversations" />
        <SkeletonCard v-else-if="loadingList" :count="4" />
        <EmptyState
          v-else-if="!conversations.length"
          title="暂无客服会话"
          description="当前筛选条件下没有会话，可切换其他状态查看。"
        />
        <div v-else class="admin-cs__list">
          <button
            v-for="item in conversations"
            :key="item.id"
            type="button"
            class="admin-cs__session"
            :class="{ 'is-active': String(selectedId) === String(item.id) }"
            @click="selectConversation(item.id)"
          >
            <span class="admin-cs__session-top">
              <strong>{{ requesterName(item) }}</strong>
              <el-tag :type="statusTagType(item.supportStatus)" size="small" effect="plain">
                {{ statusLabel(item.supportStatus) }}
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

      <main class="admin-cs__thread">
        <ErrorBlock v-if="threadError" :message="threadError" @retry="loadThread(selectedId)" />
        <SkeletonCard v-else-if="loadingThread && !messages.length" :count="3" />
        <EmptyState
          v-else-if="!detail"
          title="请选择一个会话"
          description="从左侧队列选择客服会话后即可查看对话并回复。"
        />

        <template v-else>
          <header class="admin-cs__thread-head">
            <div>
              <h2>{{ requesterName(detail) }}</h2>
              <el-tag :type="statusTagType(detail.supportStatus)" size="small" effect="plain">
                {{ statusLabel(detail.supportStatus) }}
              </el-tag>
            </div>
          </header>

          <div ref="threadRef" class="admin-cs__messages">
            <EmptyState v-if="!messages.length" title="暂无消息" description="该会话还没有消息记录。" />
            <article
              v-for="message in messages"
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
              v-if="selectedClosed"
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
                @select="applyQuickReply"
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
                <el-button type="primary" :loading="submitting" @click="submitReply">发送回复</el-button>
              </div>
            </template>
          </div>
        </template>
      </main>

      <aside v-if="detail" class="admin-cs__context">
        <section class="admin-cs__panel">
          <h3>请求者</h3>
          <p class="admin-cs__requester">{{ requesterName(detail) }}</p>
          <p class="admin-cs__requester-sub">用户 ID：{{ requester.id || '-' }}</p>
        </section>

        <section class="admin-cs__panel">
          <h3>快捷跳转</h3>
          <div class="admin-cs__links">
            <router-link v-for="link in contextLinks" :key="link.path" :to="link.path">{{ link.label }}</router-link>
          </div>
        </section>

        <section class="admin-cs__panel">
          <h3>会话操作</h3>
          <div class="admin-cs__actions">
            <el-button
              v-if="detail.supportStatus !== 'human' || assignedToOther"
              type="primary"
              :loading="claiming"
              :disabled="selectedClosed"
              @click="claim"
            >
              认领 / 接入
            </el-button>
            <el-button
              :loading="closing"
              :disabled="selectedClosed || assignedToOther"
              @click="closeConversation"
            >
              结束会话
            </el-button>
          </div>
        </section>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.admin-cs__hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.admin-cs__metrics {
  display: flex;
  gap: 12px;
}

.admin-cs__metric {
  display: grid;
  min-width: 96px;
  gap: 4px;
  padding: 14px;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
  text-align: center;
}

.admin-cs__metric strong {
  font-size: 28px;
  line-height: 1;
}

.admin-cs__metric span {
  color: var(--cm-text-secondary);
  font-size: 13px;
}

.admin-cs__workspace {
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(0, 1fr) minmax(220px, 280px);
  gap: 16px;
  align-items: start;
}

.admin-cs__queue,
.admin-cs__thread,
.admin-cs__context {
  min-width: 0;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--cm-shadow-soft);
}

.admin-cs__queue {
  display: grid;
  gap: 12px;
  padding: 14px;
}

.admin-cs__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.admin-cs__filter {
  position: relative;
  height: 32px;
  padding: 0 12px;
  border: 1px solid var(--cm-border);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
  color: var(--cm-text-secondary);
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.admin-cs__filter.is-active {
  border-color: rgba(var(--cm-primary-rgb), 0.4);
  background: rgba(var(--cm-primary-rgb), 0.1);
  color: var(--cm-primary);
}

.admin-cs__filter-badge {
  margin-left: 4px;
  color: #dc2626;
}

.admin-cs__list {
  display: grid;
  gap: 8px;
  max-height: 620px;
  overflow-y: auto;
}

.admin-cs__session {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.74);
  color: var(--cm-text);
  text-align: left;
  cursor: pointer;
}

.admin-cs__session.is-active {
  border-color: rgba(var(--cm-primary-rgb), 0.4);
  background: rgba(var(--cm-primary-rgb), 0.08);
}

.admin-cs__session-top,
.admin-cs__session-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.admin-cs__session-preview {
  color: var(--cm-text-secondary);
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-cs__session-foot span {
  color: var(--cm-text-secondary);
  font-size: 12px;
}

.admin-cs__session-unread {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #dc2626;
  color: #fff !important;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}

.admin-cs__thread {
  display: flex;
  flex-direction: column;
  min-height: 640px;
}

.admin-cs__thread-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid var(--cm-border);
}

.admin-cs__thread-head h2 {
  margin: 0 0 6px;
  font-size: 18px;
}

.admin-cs__messages {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  overflow-y: auto;
  background: rgba(0, 0, 0, 0.015);
}

.admin-cs__bubble {
  max-width: 72%;
  padding: 10px 14px;
  border-radius: 14px;
}

.admin-cs__bubble p {
  margin: 0;
  line-height: 1.6;
}

.admin-cs__bubble time {
  display: block;
  margin-top: 4px;
  font-size: 11px;
  opacity: 0.6;
}

.admin-cs__bubble--other {
  align-self: flex-start;
  background: #fff;
  border: 1px solid var(--cm-border);
}

.admin-cs__bubble--self {
  align-self: flex-end;
  background: rgba(var(--cm-primary-rgb), 0.12);
}

.admin-cs__bubble-recalled {
  color: var(--cm-text-secondary);
  font-style: italic;
}

.admin-cs__composer {
  position: relative;
  display: grid;
  gap: 10px;
  padding: 14px 16px;
  border-top: 1px solid var(--cm-border);
}

.admin-cs__composer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.admin-cs__quick-reply {
  position: absolute;
  left: 16px;
  bottom: calc(100% - 6px);
  z-index: 10;
}

.admin-cs__context {
  display: grid;
  gap: 14px;
  padding: 16px;
}

.admin-cs__panel {
  display: grid;
  gap: 8px;
}

.admin-cs__panel h3 {
  margin: 0;
  font-size: 14px;
}

.admin-cs__requester {
  margin: 0;
  font-weight: 700;
}

.admin-cs__requester-sub {
  margin: 0;
  color: var(--cm-text-secondary);
  font-size: 13px;
}

.admin-cs__links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-cs__links a {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
  color: var(--cm-text);
  font-weight: 700;
}

.admin-cs__actions {
  display: grid;
  gap: 8px;
}

@media (max-width: 1180px) {
  .admin-cs__workspace {
    grid-template-columns: 1fr;
  }
}
</style>
