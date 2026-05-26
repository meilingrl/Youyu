<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElImageViewer } from 'element-plus/es/components/image-viewer/index.mjs'
import 'element-plus/es/components/image-viewer/style/css'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-services'
import EmptyState from '@/components/common/EmptyState.vue'
import ConversationMenu from '@/components/chat/ConversationMenu.vue'
import ImageUploader from '@/components/chat/ImageUploader.vue'
import MessageSearch from '@/components/chat/MessageSearch.vue'
import OrderCardMessage from '@/components/chat/OrderCardMessage.vue'
import ProductCardMessage from '@/components/chat/ProductCardMessage.vue'
import QuickReplyPanel from '@/components/chat/QuickReplyPanel.vue'
import NotificationsView from '@/views/app/NotificationsView.vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { useNotificationStore } from '@/stores/notification'

const props = defineProps({
  conversationId: {
    type: String,
    default: ''
  }
})

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()
const notificationStore = useNotificationStore()

const selectedCategory = ref('trade')
const searchOpen = ref(false)
const quickReplyOpen = ref(false)
const messageInput = ref('')
const threadRef = ref(null)
const inputRef = ref(null)
const previewImageUrl = ref('')
const failedImageIds = ref(new Set())
const highlightedMessageId = ref(null)
const windowWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1200)

const categories = [
  { id: 'trade', label: '交易' },
  { id: 'shop', label: '店铺' },
  { id: 'support', label: '客服' },
  { id: 'notifications', label: '通知' }
]

const isMobile = computed(() => windowWidth.value < 900)
const activeConversation = computed(() => chatStore.activeConversation)
const showMobileDetail = computed(() => isMobile.value && activeConversation.value && selectedCategory.value !== 'notifications')
const visibleConversations = computed(() => selectedCategory.value === 'notifications' ? [] : chatStore.conversations)

const conversationRows = computed(() => {
  return visibleConversations.value.map((conversation) => {
    const peerUser = conversation.peerUser || {}
    return {
      id: conversation.id,
      title: peerUser.nickname || peerUser.username || '用户',
      preview: conversation.lastMessagePreview || '开始对话',
      time: formatTime(conversation.lastMessageAt || conversation.createdAt),
      unread: conversation.isMuted ? '' : formatUnread(conversation.unreadCount),
      isPinned: conversation.isPinned,
      isMuted: conversation.isMuted
    }
  })
})

const displayMessages = computed(() => {
  const currentUserId = String(authStore.currentUser?.id || '')
  return chatStore.messages.map((message) => {
    const senderId = String(message.senderId ?? message.senderUserId ?? '')
    const isSelf = senderId === currentUserId
    return {
      ...message,
      role: isSelf ? 'self' : 'other',
      canRecall: isSelf && canRecall(message),
      recalledText: isSelf ? '你撤回了一条消息' : '对方撤回了一条消息',
      isHighlighted: String(highlightedMessageId.value || '') === String(message.id),
      imageFailed: failedImageIds.value.has(message.id)
    }
  })
})

const quickReplyScenario = computed(() => {
  if (selectedCategory.value === 'support') return 'support'
  if (activeConversation.value?.shopId) return 'seller'
  return 'buyer'
})

function formatUnread(count) {
  const value = Number(count || 0)
  if (value <= 0) return ''
  return value > 99 ? '99+' : String(value)
}

function categoryUnread(categoryId) {
  if (categoryId === 'trade') return formatUnread(chatStore.unreadCount)
  if (categoryId === 'notifications') return formatUnread(notificationStore.unreadCount)
  return ''
}

function formatTime(timestamp) {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const diff = now - date
  if (diff < 24 * 60 * 60 * 1000) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

function canRecall(message) {
  if (message.isRecalled || !message.createdAt) return false
  const createdAt = new Date(message.createdAt).getTime()
  return !Number.isNaN(createdAt) && Date.now() - createdAt <= 2 * 60 * 1000
}

function onResize() {
  windowWidth.value = window.innerWidth
}

function openCategory(categoryId) {
  selectedCategory.value = categoryId
  searchOpen.value = false
  quickReplyOpen.value = false
  if (categoryId === 'notifications') {
    chatStore.activeConversationId = null
  } else if (!isMobile.value && !chatStore.activeConversationId && visibleConversations.value.length) {
    openConversation(visibleConversations.value[0].id)
  }
}

async function openConversation(conversationId, options = {}) {
  chatStore.activeConversationId = conversationId
  quickReplyOpen.value = false
  highlightedMessageId.value = options.messageId || null
  try {
    await chatStore.fetchMessages(conversationId)
    chatStore.startPolling(conversationId)
    if (options.messageId) {
      scrollToMessage(options.messageId)
    } else {
      scrollToBottom()
    }
  } catch (error) {
    ElMessage.error('加载消息失败')
  }
}

function backToList() {
  chatStore.stopPolling()
  chatStore.activeConversationId = null
  highlightedMessageId.value = null
}

async function sendText() {
  const body = messageInput.value.trim()
  if (!body || !chatStore.activeConversationId || chatStore.sending) return
  messageInput.value = ''
  quickReplyOpen.value = false
  try {
    await chatStore.sendMessage(chatStore.activeConversationId, { body, messageType: 'text' })
    scrollToBottom()
  } catch (error) {
    messageInput.value = body
    ElMessage.error('发送消息失败')
  }
}

async function sendImage(image) {
  if (!chatStore.activeConversationId || chatStore.sending) return
  quickReplyOpen.value = false
  try {
    await chatStore.sendMessage(chatStore.activeConversationId, {
      body: image.fileName || '',
      messageType: 'image',
      mediaUrl: image.mediaUrl
    })
    scrollToBottom()
  } catch (error) {
    ElMessage.error('发送图片失败')
  }
}

function selectQuickReply(content) {
  messageInput.value = content
  quickReplyOpen.value = false
  nextTick(() => inputRef.value?.focus())
}

async function togglePin(conversationId) {
  const conversation = chatStore.conversations.find((item) => String(item.id) === String(conversationId))
  if (!conversation) return
  await chatStore.setConversationPinned(conversationId, !conversation.isPinned)
}

async function toggleMute(conversationId) {
  const conversation = chatStore.conversations.find((item) => String(item.id) === String(conversationId))
  if (!conversation) return
  await chatStore.setConversationMuted(conversationId, !conversation.isMuted)
}

async function deleteConversation(conversationId) {
  try {
    await ElMessageBox.confirm('删除后将不再显示此会话，但历史消息会保留。', '删除会话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await chatStore.deleteConversation(conversationId)
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error('删除会话失败')
  }
}

async function recallMessage(message) {
  try {
    await ElMessageBox.confirm('确认撤回这条消息？', '撤回消息', {
      confirmButtonText: '撤回',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await chatStore.recallMessage(message.id)
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error('撤回失败')
  }
}

async function openSearchResult(result) {
  searchOpen.value = false
  selectedCategory.value = 'trade'
  await openConversation(result.conversationId, { messageId: result.id })
}

function scrollToBottom() {
  nextTick(() => {
    if (threadRef.value) threadRef.value.scrollTop = threadRef.value.scrollHeight
  })
}

function scrollToMessage(messageId) {
  nextTick(() => {
    const element = threadRef.value?.querySelector(`[data-message-id="${messageId}"]`)
    if (element) element.scrollIntoView({ block: 'center', behavior: 'smooth' })
    else scrollToBottom()
  })
}

function openProduct(productId) {
  if (productId) router.push(`/app/products/${productId}`)
}

function openOrder(orderId) {
  if (orderId) router.push({ path: '/app/orders', query: { orderId: String(orderId) } })
}

function onImageError(messageId) {
  failedImageIds.value = new Set([...failedImageIds.value, messageId])
}

async function load() {
  try {
    if (route.query.category) selectedCategory.value = String(route.query.category)
    await chatStore.fetchConversations()
    notificationStore.loadUnreadCount().catch(() => {})
    const initialId = props.conversationId || route.query.conversationId || ''
    const initial = initialId
      ? chatStore.conversations.find((item) => String(item.id) === String(initialId))
      : null
    if (initial || (!isMobile.value && chatStore.conversations.length)) {
      await openConversation(initial?.id || chatStore.conversations[0].id)
    }
  } catch (error) {
    ElMessage.error('加载会话失败')
  }
}

onMounted(() => {
  window.addEventListener('resize', onResize)
  load()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chatStore.stopPolling()
})
</script>

<template>
  <div class="messages-container">
    <header class="messages-header">
      <h1>消息中心</h1>
      <button type="button" class="messages-search-toggle" :class="{ 'is-active': searchOpen }" @click="searchOpen = !searchOpen">
        {{ searchOpen ? '返回会话' : '搜索消息' }}
      </button>
    </header>

    <div class="messages-layout">
      <aside v-if="!showMobileDetail" class="messages-sidebar">
        <div class="messages-categories">
          <button
            v-for="item in categories"
            :key="item.id"
            type="button"
            class="messages-category"
            :class="{ 'is-active': selectedCategory === item.id }"
            @click="openCategory(item.id)"
          >
            <strong>{{ item.label }}</strong>
            <span v-if="categoryUnread(item.id)" class="messages-category__badge">{{ categoryUnread(item.id) }}</span>
          </button>
        </div>

        <div v-if="chatStore.loading && !conversationRows.length" class="messages-loading">
          <el-skeleton animated :rows="3" />
        </div>

        <div v-else-if="conversationRows.length" class="messages-conversation-list">
          <article
            v-for="item in conversationRows"
            :key="item.id"
            class="messages-conversation"
            :class="{ 'is-active': activeConversation?.id === item.id, 'is-pinned': item.isPinned, 'is-muted': item.isMuted }"
          >
            <button type="button" class="messages-conversation__select" @click="openConversation(item.id)">
              <div class="messages-avatar">{{ item.title.slice(0, 1) }}</div>
              <div class="messages-conversation__content">
                <div class="messages-conversation__top">
                  <strong>{{ item.title }}</strong>
                  <span class="messages-time">{{ item.time }}</span>
                </div>
                <p class="messages-preview">{{ item.preview }}</p>
                <div v-if="item.isPinned || item.isMuted" class="messages-states">
                  <span v-if="item.isPinned">置顶</span>
                  <span v-if="item.isMuted">静音</span>
                </div>
              </div>
              <span v-if="item.unread" class="messages-unread">{{ item.unread }}</span>
            </button>
            <ConversationMenu
              :is-pinned="item.isPinned"
              :is-muted="item.isMuted"
              @pin="togglePin(item.id)"
              @mute="toggleMute(item.id)"
              @delete="deleteConversation(item.id)"
            />
          </article>
        </div>

        <EmptyState
          v-else
          emoji="💬"
          title="当前分类还没有会话"
          description="新的聊天和通知会显示在这里。"
        />
      </aside>

      <main class="messages-main">
        <div v-if="showMobileDetail" class="messages-mobile-back">
          <button type="button" class="messages-back-btn" @click="backToList">返回</button>
        </div>

        <MessageSearch v-if="searchOpen" @open-result="openSearchResult" />
        <NotificationsView v-else-if="selectedCategory === 'notifications'" embedded />

        <template v-else-if="activeConversation">
          <header class="messages-detail-header">
            <div class="messages-avatar messages-avatar--large">
              {{ (activeConversation.peerUser?.nickname || activeConversation.peerUser?.username || '用户').slice(0, 1) }}
            </div>
            <div>
              <h2>{{ activeConversation.peerUser?.nickname || activeConversation.peerUser?.username || '用户' }}</h2>
              <div class="messages-states">
                <span v-if="activeConversation.isPinned">置顶</span>
                <span v-if="activeConversation.isMuted">静音</span>
              </div>
            </div>
          </header>

          <div ref="threadRef" class="messages-thread">
            <div v-if="chatStore.loading && !displayMessages.length" class="messages-loading">
              <el-skeleton animated :rows="5" />
            </div>

            <article
              v-for="message in displayMessages"
              v-else
              :key="message.id"
              class="messages-bubble"
              :class="[
                `messages-bubble--${message.role}`,
                {
                  'messages-bubble--image': message.messageType === 'image',
                  'messages-bubble--card': ['product_card', 'order_card'].includes(message.messageType),
                  'is-recalled': message.isRecalled,
                  'is-highlighted': message.isHighlighted
                }
              ]"
              :data-message-id="message.id"
            >
              <button v-if="message.canRecall" type="button" class="messages-recall-btn" @click="recallMessage(message)">
                撤回
              </button>

              <p v-if="message.isRecalled" class="messages-recalled-text">{{ message.recalledText }}</p>

              <template v-else-if="message.messageType === 'image'">
                <button
                  v-if="message.mediaUrl && !message.imageFailed"
                  type="button"
                  class="messages-image-btn"
                  @click="previewImageUrl = message.mediaUrl"
                >
                  <img :src="message.mediaUrl" :alt="message.body || '图片消息'" class="messages-image" @error="onImageError(message.id)" />
                </button>
                <div v-else class="messages-image-fallback">图片加载失败</div>
                <p v-if="message.body" class="messages-image-caption">{{ message.body }}</p>
              </template>

              <ProductCardMessage
                v-else-if="message.messageType === 'product_card'"
                :product="message.product || { id: message.productId }"
                :body="message.body"
                @open="openProduct"
              />
              <OrderCardMessage
                v-else-if="message.messageType === 'order_card'"
                :order="message.order || { id: message.orderId }"
                :body="message.body"
                @open="openOrder"
              />
              <p v-else>{{ message.body }}</p>
            </article>
          </div>

          <div class="messages-input-area">
            <QuickReplyPanel
              v-if="quickReplyOpen"
              class="messages-quick-reply-panel"
              :scenario="quickReplyScenario"
              @select="selectQuickReply"
            />
            <div class="messages-composer">
              <ImageUploader @selected="sendImage" />
              <button type="button" class="messages-quick-reply-btn" :class="{ 'is-active': quickReplyOpen }" @click="quickReplyOpen = !quickReplyOpen">
                快捷
              </button>
              <input ref="inputRef" v-model="messageInput" class="messages-input" type="text" placeholder="输入消息..." @keyup.enter="sendText" />
              <button type="button" class="messages-send-btn" :disabled="!messageInput.trim() || chatStore.sending" @click="sendText">
                {{ chatStore.sending ? '发送中...' : '发送' }}
              </button>
            </div>
          </div>
        </template>

        <EmptyState
          v-else
          emoji="💬"
          title="选择一个会话查看详情"
          description="点击左侧会话列表开始查看。"
        />
      </main>
    </div>

    <ElImageViewer
      v-if="previewImageUrl"
      :url-list="[previewImageUrl]"
      :initial-index="0"
      hide-on-click-modal
      @close="previewImageUrl = ''"
    />
  </div>
</template>

<style scoped>
.messages-container {
  --msg-primary: #ea580c;
  --msg-primary-bg: #fff7ed;
  --msg-primary-soft: #fed7aa;
  --msg-paper: #fafaf9;
  --msg-text: #1f2937;
  --msg-muted: #6b7280;
  max-width: 1400px;
  margin: 0 auto;
  padding: 32px;
}

.messages-header,
.messages-categories,
.messages-composer,
.messages-detail-header,
.messages-states {
  display: flex;
  align-items: center;
}

.messages-header {
  gap: 16px;
  margin-bottom: 32px;
}

.messages-header h1 {
  margin: 0;
  font-size: 28px;
}

.messages-search-toggle,
.messages-category,
.messages-back-btn,
.messages-send-btn,
.messages-quick-reply-btn {
  border: none;
  border-radius: 12px;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.messages-search-toggle {
  margin-left: auto;
  height: 40px;
  padding: 0 16px;
  background: var(--msg-primary-bg);
  color: var(--msg-primary);
}

.messages-search-toggle.is-active {
  background: var(--msg-primary);
  color: #fff;
}

.messages-layout {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 20px;
  min-height: 720px;
}

.messages-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.messages-categories {
  gap: 10px;
  flex-wrap: wrap;
}

.messages-category {
  min-height: 40px;
  padding: 0 14px;
  background: var(--msg-primary-bg);
  color: var(--msg-muted);
}

.messages-category.is-active {
  background: var(--msg-primary-soft);
  color: var(--msg-primary);
}

.messages-category__badge,
.messages-unread {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  border-radius: 999px;
  padding: 0 5px;
  background: #dc2626;
  color: #fff;
  font-size: 11px;
}

.messages-conversation-list {
  display: grid;
  gap: 10px;
}

.messages-conversation {
  display: flex;
  gap: 6px;
  padding: 8px;
  border-radius: 16px;
  background: transparent;
  position: relative;
}

.messages-conversation.is-active,
.messages-conversation:hover {
  background: var(--msg-primary-bg);
}

.messages-conversation.is-pinned {
  border: 1px solid var(--msg-primary-soft);
}

.messages-conversation.is-muted {
  opacity: 0.72;
}

.messages-conversation__select {
  flex: 1;
  min-width: 0;
  display: flex;
  gap: 12px;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.messages-avatar {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(234, 88, 12, 0.12);
  color: var(--msg-primary);
  font-weight: 800;
  flex-shrink: 0;
}

.messages-avatar--large {
  width: 40px;
  height: 40px;
}

.messages-conversation__content {
  min-width: 0;
  flex: 1;
}

.messages-conversation__top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.messages-conversation__top strong,
.messages-preview {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.messages-time,
.messages-preview {
  color: var(--msg-muted);
  font-size: 13px;
}

.messages-preview {
  margin: 6px 0 0;
}

.messages-states {
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 6px;
}

.messages-states span {
  padding: 2px 7px;
  border-radius: 999px;
  background: rgba(234, 88, 12, 0.1);
  color: var(--msg-primary);
  font-size: 12px;
  font-weight: 700;
}

.messages-main {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(31, 41, 55, 0.06);
}

.messages-mobile-back,
.messages-detail-header,
.messages-input-area {
  padding: 16px 24px;
  border-bottom: 1px solid var(--msg-paper);
}

.messages-detail-header {
  gap: 12px;
}

.messages-detail-header h2 {
  margin: 0;
  font-size: 18px;
}

.messages-thread {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
  padding: 24px;
  background: var(--msg-paper);
}

.messages-bubble {
  position: relative;
  max-width: 66%;
  padding: 12px 16px;
  border-radius: 16px;
}

.messages-bubble p {
  margin: 0;
  line-height: 1.6;
}

.messages-bubble--other {
  align-self: flex-start;
  background: #fff;
  color: var(--msg-text);
  border-bottom-left-radius: 6px;
  box-shadow: 0 2px 8px rgba(31, 41, 55, 0.06);
}

.messages-bubble--self {
  align-self: flex-end;
  background: linear-gradient(135deg, #fb923c 0%, var(--msg-primary) 100%);
  color: #fff;
  border-bottom-right-radius: 6px;
}

.messages-bubble--self p {
  color: #fff;
}

.messages-bubble--card,
.messages-bubble--self.messages-bubble--card {
  padding: 0;
  background: transparent;
  box-shadow: none;
}

.messages-bubble.is-recalled {
  background: #e7e5e4;
  color: var(--msg-muted);
  box-shadow: none;
}

.messages-bubble.is-highlighted {
  outline: 3px solid #facc15;
  outline-offset: 3px;
}

.messages-recall-btn {
  position: absolute;
  top: -28px;
  right: 0;
  display: none;
  height: 24px;
  padding: 0 9px;
  border: none;
  border-radius: 8px;
  background: rgba(31, 41, 55, 0.85);
  color: #fff;
  font-size: 12px;
  cursor: pointer;
}

.messages-bubble:hover .messages-recall-btn {
  display: inline-flex;
  align-items: center;
}

.messages-image-btn {
  display: block;
  border: none;
  padding: 0;
  background: transparent;
  cursor: zoom-in;
}

.messages-image {
  max-width: min(300px, 58vw);
  max-height: 360px;
  border-radius: 12px;
  object-fit: contain;
}

.messages-image-fallback {
  width: min(300px, 58vw);
  min-height: 132px;
  border-radius: 12px;
  background: #e7e5e4;
  color: var(--msg-muted);
  display: flex;
  align-items: center;
  justify-content: center;
}

.messages-input-area {
  position: relative;
  background: #fffbf5;
  border-top: 1px solid var(--msg-paper);
  border-bottom: 0;
}

.messages-composer {
  gap: 10px;
}

.messages-quick-reply-panel {
  position: absolute;
  left: 24px;
  bottom: calc(100% - 8px);
  z-index: 20;
}

.messages-quick-reply-btn,
.messages-send-btn {
  height: 42px;
  padding: 0 16px;
  background: #fff;
  color: var(--msg-primary);
  border: 1px solid var(--msg-primary-soft);
}

.messages-send-btn {
  background: var(--msg-primary);
  color: #fff;
}

.messages-send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.messages-input {
  flex: 1;
  min-width: 0;
  height: 42px;
  padding: 0 16px;
  border: 1px solid #e7e5e4;
  border-radius: 16px;
  font: inherit;
}

.messages-loading {
  padding: 16px;
}

@media (max-width: 900px) {
  .messages-container {
    padding: 20px;
  }

  .messages-layout {
    grid-template-columns: 1fr;
  }

  .messages-bubble {
    max-width: 86%;
  }
}
</style>
