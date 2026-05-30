<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElImageViewer } from 'element-plus/es/components/image-viewer/index.mjs'
import 'element-plus/es/components/image-viewer/style/css'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-services'
import EmptyState from '@/components/common/EmptyState.vue'
import ChatSwipeConversationRow from '@/components/chat/ChatSwipeConversationRow.vue'
import EmojiStickerPanel from '@/components/chat/EmojiStickerPanel.vue'
import ImageUploader from '@/components/chat/ImageUploader.vue'
import MessageSearch from '@/components/chat/MessageSearch.vue'
import OrderCardMessage from '@/components/chat/OrderCardMessage.vue'
import ProductCardMessage from '@/components/chat/ProductCardMessage.vue'
import QuickReplyPanel from '@/components/chat/QuickReplyPanel.vue'
import NotificationsView from '@/views/app/NotificationsView.vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { useNotificationStore } from '@/stores/notification'
import { resolveErrorMessage } from '@/utils/error-utils'

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
const emojiOpen = ref(false)
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
const visibleConversations = computed(() => {
  if (selectedCategory.value === 'notifications') return []
  return chatStore.conversations.filter((conversation) => categoryFor(conversation) === selectedCategory.value)
})

const conversationRows = computed(() => {
  return visibleConversations.value.map((conversation) => {
    const peerUser = conversation.peerUser || {}
    return {
      id: conversation.id,
      title: peerUser.nickname || peerUser.username || '用户',
      preview: conversation.lastMessagePreview || '开始对话',
      time: formatTime(conversation.lastMessageAt || conversation.createdAt),
      unread: conversation.isMuted ? '' : formatUnread(conversation.unreadCount),
      categoryLabel: labelForCategory(categoryFor(conversation)),
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

const startingSupport = ref(false)
const escalating = ref(false)
const isSupportConversation = computed(() => activeConversation.value?.type === 'support')
const supportStatus = computed(() => activeConversation.value?.supportStatus || '')
const supportStatusLabel = computed(() => {
  switch (supportStatus.value) {
    case 'ai':
      return '智能客服'
    case 'pending':
      return '等待人工接入'
    case 'human':
      return '人工客服处理中'
    case 'closed':
      return '会话已结束'
    default:
      return '在线客服'
  }
})
const canEscalate = computed(() => isSupportConversation.value && supportStatus.value === 'ai')
const canCloseSupport = computed(() => isSupportConversation.value && supportStatus.value !== 'closed')
const supportSessionClosed = computed(() => isSupportConversation.value && supportStatus.value === 'closed')
const hasOpenSupportSession = computed(() =>
  chatStore.conversations.some((item) => item.type === 'support' && item.supportStatus !== 'closed')
)
const supportStartLabel = computed(() => {
  if (startingSupport.value) return '正在接入...'
  if (hasOpenSupportSession.value) return '继续咨询'
  return '联系在线客服'
})
const closingSupport = ref(false)

async function startSupportSession() {
  if (startingSupport.value) return
  startingSupport.value = true
  try {
    selectedCategory.value = 'support'
    const conversation = await chatStore.startSupportSession()
    chatStore.startPolling(conversation.id)
    scrollToBottom()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '发起客服会话失败，请稍后再试'))
  } finally {
    startingSupport.value = false
  }
}

async function escalateToHuman() {
  if (!chatStore.activeConversationId || escalating.value) return
  escalating.value = true
  try {
    await chatStore.escalateSupportConversation(chatStore.activeConversationId)
    ElMessage.success('已转接人工客服，请稍候')
    scrollToBottom()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '转人工失败，请稍后再试'))
  } finally {
    escalating.value = false
  }
}

async function closeSupportSession() {
  if (!chatStore.activeConversationId || closingSupport.value) return
  try {
    await ElMessageBox.confirm('结束后将无法继续发送消息，可稍后再发起新的咨询。', '结束客服会话', {
      confirmButtonText: '结束会话',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(resolveErrorMessage(error, '结束会话失败，请稍后再试'))
    return
  }
  closingSupport.value = true
  try {
    await chatStore.closeSupportConversation(chatStore.activeConversationId)
    await chatStore.fetchConversations(0, 50, { silent: true })
    ElMessage.success('客服会话已结束')
    scrollToBottom()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '结束会话失败，请稍后再试'))
  } finally {
    closingSupport.value = false
  }
}

function formatUnread(count) {
  const value = Number(count || 0)
  if (value <= 0) return ''
  return value > 99 ? '99+' : String(value)
}

function categoryUnread(categoryId) {
  if (categoryId !== 'notifications') {
    const count = chatStore.conversations
      .filter((conversation) => categoryFor(conversation) === categoryId && !conversation.isMuted)
      .reduce((total, conversation) => total + Number(conversation.unreadCount || 0), 0)
    return formatUnread(count)
  }
  if (categoryId === 'notifications') return formatUnread(notificationStore.unreadCount)
  return ''
}

function categoryFor(conversation = {}) {
  const type = conversation.type || ''
  if (type === 'support') return 'support'
  if (type === 'shop_inquiry' || conversation.shopId) return 'shop'
  return 'trade'
}

function labelForCategory(categoryId) {
  return categories.find((item) => item.id === categoryId)?.label || ''
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

function formatMessageTime(timestamp) {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
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
  emojiOpen.value = false
  if (categoryId === 'notifications') {
    chatStore.activeConversationId = null
  } else if (!isMobile.value && visibleConversations.value.length) {
    const activeVisible = visibleConversations.value.some((item) => String(item.id) === String(chatStore.activeConversationId))
    if (activeVisible) return
    openConversation(visibleConversations.value[0].id)
  }
}

async function openConversation(conversationId, options = {}) {
  chatStore.activeConversationId = conversationId
  quickReplyOpen.value = false
  emojiOpen.value = false
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
  emojiOpen.value = false
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
  emojiOpen.value = false
  try {
    await chatStore.sendMessage(chatStore.activeConversationId, {
      body: '',
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
  emojiOpen.value = false
  nextTick(() => inputRef.value?.focus())
}

function insertEmoji(emoji) {
  messageInput.value = `${messageInput.value}${emoji}`
  nextTick(() => inputRef.value?.focus())
}

async function sendSticker(sticker) {
  if (!chatStore.activeConversationId || chatStore.sending) return
  emojiOpen.value = false
  quickReplyOpen.value = false
  const label = sticker.label || '表情包'
  const mediaUrl = createStickerDataUrl(sticker)
  try {
    await chatStore.sendMessage(chatStore.activeConversationId, {
      body: label,
      messageType: 'image',
      mediaUrl
    })
    scrollToBottom()
  } catch (error) {
    ElMessage.error('发送表情包失败')
  }
}

function createStickerDataUrl(sticker) {
  const label = escapeSvg(sticker.label || 'OK')
  const icon = escapeSvg(sticker.icon || '👍')
  const color = /^#[0-9a-fA-F]{6}$/.test(sticker.color || '') ? sticker.color : '#ea580c'
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="240" height="160" viewBox="0 0 240 160"><rect width="240" height="160" rx="24" fill="#fff7ed"/><circle cx="120" cy="62" r="40" fill="${color}" fill-opacity=".14"/><text x="120" y="76" text-anchor="middle" font-size="46">${icon}</text><text x="120" y="122" text-anchor="middle" font-size="24" font-family="Arial, sans-serif" font-weight="700" fill="#1f2937">${label}</text></svg>`
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`
}

function escapeSvg(value) {
  return String(value).replace(/[&<>"']/g, (char) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  })[char])
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
  const conversation = chatStore.conversations.find((item) => String(item.id) === String(conversationId))
  const isSupport = conversation?.type === 'support'
  try {
    await ElMessageBox.confirm(
      isSupport
        ? '结束并隐藏该客服会话，历史消息会保留。之后可点击「联系在线客服」或「再次咨询」重新发起。'
        : '删除后将不再显示此会话，但历史消息会保留。',
      isSupport ? '结束客服会话' : '删除会话',
      {
        confirmButtonText: isSupport ? '结束会话' : '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await chatStore.deleteConversation(conversationId)
    if (isSupport) {
      ElMessage.success('客服会话已结束')
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(resolveErrorMessage(error, isSupport ? '结束会话失败' : '删除会话失败'))
    }
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

        <button
          v-if="selectedCategory === 'support'"
          type="button"
          class="messages-support-start"
          :disabled="startingSupport"
          @click="startSupportSession"
        >
          {{ supportStartLabel }}
        </button>

        <div v-if="chatStore.loading && !conversationRows.length" class="messages-loading">
          <el-skeleton animated :rows="3" />
        </div>

        <div v-else-if="conversationRows.length" class="messages-conversation-list">
          <ChatSwipeConversationRow
            v-for="item in conversationRows"
            :key="item.id"
            :item="item"
            :active="activeConversation?.id === item.id"
            @select="openConversation(item.id)"
            @pin="togglePin(item.id)"
            @mute="toggleMute(item.id)"
            @delete="deleteConversation(item.id)"
          />
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
                <span v-if="isSupportConversation" class="messages-states__support">{{ supportStatusLabel }}</span>
                <span v-if="activeConversation.isPinned">置顶</span>
                <span v-if="activeConversation.isMuted">静音</span>
              </div>
            </div>
            <div class="messages-detail-actions">
              <button
                v-if="canEscalate"
                type="button"
                class="messages-escalate-btn"
                :disabled="escalating"
                @click="escalateToHuman"
              >
                {{ escalating ? '转接中...' : '转人工' }}
              </button>
              <button
                v-if="canCloseSupport"
                type="button"
                class="messages-close-support-btn"
                :disabled="closingSupport"
                @click="closeSupportSession"
              >
                {{ closingSupport ? '结束中...' : '结束会话' }}
              </button>
              <button
                v-if="supportSessionClosed"
                type="button"
                class="messages-escalate-btn"
                :disabled="startingSupport"
                @click="startSupportSession"
              >
                {{ startingSupport ? '接入中...' : '再次咨询' }}
              </button>
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

              <div class="messages-bubble__hoverbar">
                <time>{{ formatMessageTime(message.createdAt) }}</time>
                <button v-if="message.canRecall" type="button" @click="recallMessage(message)">
                  撤回
                </button>
              </div>
            </article>
          </div>

          <div v-if="supportSessionClosed" class="messages-support-closed-hint">
            本次客服会话已结束。如需继续咨询，请点击上方「再次咨询」。
          </div>

          <div v-else class="messages-input-area">
            <QuickReplyPanel
              v-if="quickReplyOpen"
              class="messages-quick-reply-panel"
              :scenario="quickReplyScenario"
              @select="selectQuickReply"
            />
            <EmojiStickerPanel
              v-if="emojiOpen"
              class="messages-emoji-panel"
              @emoji="insertEmoji"
              @sticker="sendSticker"
            />
            <div class="messages-composer">
              <ImageUploader @selected="sendImage" />
              <button
                type="button"
                class="messages-tool-btn"
                :class="{ 'is-active': emojiOpen }"
                aria-label="表情和表情包"
                @click="emojiOpen = !emojiOpen; quickReplyOpen = false"
              >
                ☺
              </button>
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
.messages-detail-actions,
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
.messages-quick-reply-btn,
.messages-tool-btn {
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

.messages-category__badge {
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

.messages-states__support {
  background: rgba(37, 99, 235, 0.12) !important;
  color: #2563eb !important;
}

.messages-support-start,
.messages-escalate-btn {
  border: none;
  border-radius: 12px;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.messages-support-start {
  width: 100%;
  min-height: 44px;
  background: var(--msg-primary);
  color: #fff;
}

.messages-support-start:disabled,
.messages-escalate-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.messages-detail-actions {
  margin-left: auto;
  gap: 8px;
  flex-shrink: 0;
}

.messages-escalate-btn,
.messages-close-support-btn {
  height: 36px;
  padding: 0 16px;
  background: #fff;
  color: var(--msg-primary);
  border: 1px solid var(--msg-primary-soft);
}

.messages-close-support-btn {
  color: #6b7280;
  border-color: #e5e7eb;
}

.messages-support-closed-hint {
  padding: 16px 20px;
  text-align: center;
  color: var(--msg-muted);
  font-size: 14px;
  border-top: 1px solid #f3f4f6;
  background: var(--msg-paper);
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
  background: var(--msg-primary-bg);
  color: var(--msg-text);
  border: 1px solid var(--msg-primary-soft);
  border-bottom-right-radius: 6px;
}

.messages-bubble--self p {
  color: var(--msg-text);
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

.messages-bubble__hoverbar {
  position: absolute;
  top: 50%;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  padding: 4px 8px;
  border: none;
  border-radius: 999px;
  background: rgba(31, 41, 55, 0.88);
  color: #fff;
  opacity: 0;
  pointer-events: none;
  transform: translateY(-50%) scale(0.96);
  transition: opacity 0.16s ease, transform 0.16s ease;
  white-space: nowrap;
  z-index: 5;
}

.messages-bubble--self .messages-bubble__hoverbar {
  right: calc(100% + 8px);
}

.messages-bubble--other .messages-bubble__hoverbar {
  left: calc(100% + 8px);
}

.messages-bubble:hover .messages-bubble__hoverbar,
.messages-bubble:focus-within .messages-bubble__hoverbar {
  display: inline-flex;
  opacity: 1;
  pointer-events: auto;
  transform: translateY(-50%) scale(1);
}

.messages-bubble__hoverbar time {
  font-size: 12px;
}

.messages-bubble__hoverbar button {
  height: 22px;
  border: none;
  border-radius: 999px;
  padding: 0 8px;
  background: #fff7ed;
  color: var(--msg-primary);
  font: inherit;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
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

.messages-emoji-panel {
  position: absolute;
  left: 50%;
  bottom: calc(100% - 8px);
  z-index: 21;
  width: min(360px, calc(100% - 48px));
  max-width: calc(100% - 48px);
  transform: translateX(-50%);
}

.messages-quick-reply-btn,
.messages-send-btn,
.messages-tool-btn {
  height: 42px;
  padding: 0 16px;
  background: #fff;
  color: var(--msg-primary);
  border: 1px solid var(--msg-primary-soft);
}

.messages-tool-btn {
  width: 42px;
  padding: 0;
  font-size: 20px;
}

.messages-tool-btn.is-active,
.messages-quick-reply-btn.is-active {
  background: var(--msg-primary-bg);
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

  .messages-bubble__hoverbar {
    top: auto;
    bottom: calc(100% + 6px);
    transform: translateY(0) scale(0.96);
  }

  .messages-bubble--self .messages-bubble__hoverbar {
    right: 0;
  }

  .messages-bubble--other .messages-bubble__hoverbar {
    left: 0;
  }

  .messages-bubble:hover .messages-bubble__hoverbar,
  .messages-bubble:focus-within .messages-bubble__hoverbar {
    transform: translateY(0) scale(1);
  }
}
</style>
