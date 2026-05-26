<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElImageViewer } from 'element-plus/es/components/image-viewer/index.mjs'
import 'element-plus/es/components/image-viewer/style/css'
import { ElMessage } from '@/plugins/element-plus-services'
import EmptyState from '@/components/common/EmptyState.vue'
import ImageUploader from '@/components/chat/ImageUploader.vue'
import NotificationsView from '@/views/app/NotificationsView.vue'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'

const chatStore = useChatStore()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()

const windowWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1200)
const selectedCategory = ref('trade')
const messagesThreadRef = ref(null)
const messagesEndRef = ref(null)
const messageInput = ref('')
const failedImageIds = ref(new Set())
const previewImageUrl = ref('')

const categoryOptions = [
  { id: 'trade', label: '交易' },
  { id: 'shop', label: '店铺' },
  { id: 'support', label: '客服' },
  { id: 'notifications', label: '通知' },
  { id: 'group', label: '群聊' }
]

const groupPlaceholders = [
  {
    title: '粉丝群',
    description: '关注的店铺上新、活动通知第一时间收到。'
  },
  {
    title: '优惠群',
    description: '限时折扣、拼单信息实时推送。'
  },
  {
    title: '校园活动群',
    description: '约自习、拼团购、组队活动都在这里。'
  }
]

const isMobile = computed(() => windowWidth.value < 900)

const visibleConversations = computed(() => {
  if (selectedCategory.value === 'group' || selectedCategory.value === 'notifications') {
    return []
  }
  return chatStore.conversations
})

const activeConversation = computed(() => {
  if (selectedCategory.value === 'group' || selectedCategory.value === 'notifications') {
    return null
  }
  if (!chatStore.activeConversationId) {
    if (isMobile.value) return null
    return visibleConversations.value[0] || null
  }
  return chatStore.activeConversation
})

const mobileShowsDetail = computed(() => Boolean(isMobile.value && activeConversation.value))

const selectedCategoryIsNotifications = computed(() => selectedCategory.value === 'notifications')

const conversationDisplayList = computed(() => {
  return visibleConversations.value.map((conv) => {
    const peerUser = conv.peerUser || {}
    const lastMessage = chatStore.messages.length > 0 && chatStore.activeConversationId === conv.id
      ? chatStore.messages[chatStore.messages.length - 1]
      : null

    return {
      id: conv.id,
      title: peerUser.nickname || peerUser.username || '用户',
      preview: messagePreview(conv, lastMessage),
      updatedAt: formatTime(conv.lastMessageAt || conv.createdAt),
      unread: formatUnreadCount(conv.unreadCount || 0)
    }
  })
})

const displayMessages = computed(() => {
  if (!activeConversation.value) return []

  return chatStore.messages.map((msg) => {
    const isSelf = msg.senderId === authStore.currentUser?.id
    return {
      id: msg.id,
      role: isSelf ? 'self' : 'other',
      body: msg.body,
      messageType: msg.messageType || 'text',
      mediaUrl: msg.mediaUrl,
      imageFailed: failedImageIds.value.has(msg.id)
    }
  })
})

function formatUnreadCount(count) {
  const numericCount = Number(count || 0)
  if (numericCount <= 0) return ''
  return numericCount > 99 ? '99+' : String(numericCount)
}

function categoryUnreadCount(categoryId) {
  if (categoryId === 'trade') {
    return formatUnreadCount(chatStore.unreadCount)
  }
  if (categoryId === 'notifications') {
    return formatUnreadCount(notificationStore.unreadCount)
  }
  return ''
}

function messagePreview(conv, lastMessage) {
  if (lastMessage?.messageType === 'image') return '[图片]'
  return lastMessage?.body || conv.lastMessagePreview || conv.last_message_preview || '开始对话'
}

function formatTime(timestamp) {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date

  if (diff < 24 * 60 * 60 * 1000) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }

  if (diff < 48 * 60 * 60 * 1000) {
    return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }

  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

function handleResize() {
  windowWidth.value = window.innerWidth
}

function openCategory(categoryId) {
  selectedCategory.value = categoryId
  chatStore.activeConversationId = null

  if (categoryId !== 'notifications' && !isMobile.value && visibleConversations.value.length > 0) {
    openConversation(visibleConversations.value[0].id)
  }
}

async function openConversation(conversationId) {
  chatStore.activeConversationId = conversationId

  try {
    await chatStore.fetchMessages(conversationId)
    chatStore.startPolling(conversationId)
    scrollToBottom()
  } catch (error) {
    ElMessage.error('加载消息失败')
  }
}

function backToList() {
  chatStore.stopPolling()
  chatStore.activeConversationId = null
}

async function handleSendMessage() {
  if (!messageInput.value.trim() || chatStore.sending) {
    return
  }

  const body = messageInput.value.trim()
  messageInput.value = ''

  try {
    await chatStore.sendMessage(chatStore.activeConversationId, {
      body,
      messageType: 'text'
    })
    scrollToBottom()
  } catch (error) {
    ElMessage.error('发送消息失败')
    messageInput.value = body
  }
}

async function handleImageSelected(image) {
  if (!chatStore.activeConversationId || chatStore.sending) {
    return
  }

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

function handleImageError(messageId) {
  failedImageIds.value = new Set([...failedImageIds.value, messageId])
}

function openImagePreview(url) {
  if (url) {
    previewImageUrl.value = url
  }
}

function closeImagePreview() {
  previewImageUrl.value = ''
}

function scrollToBottom() {
  nextTick(() => {
    const thread = messagesThreadRef.value
    if (thread) {
      thread.scrollTop = thread.scrollHeight
    }
  })
}

async function loadConversations() {
  try {
    await chatStore.fetchConversations()
    chatStore.fetchUnreadCount().catch((error) => {
      console.error('Failed to load unread count:', error)
    })
    notificationStore.loadUnreadCount().catch((error) => {
      console.error('Failed to load notification unread count:', error)
    })

    if (!isMobile.value && visibleConversations.value.length > 0) {
      await openConversation(visibleConversations.value[0].id)
    }
  } catch (error) {
    ElMessage.error('加载会话列表失败')
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  loadConversations()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chatStore.stopPolling()
})
</script>

<template>
  <div class="messages-container">
    <header class="messages-header">
      <h1>消息中心</h1>
    </header>

    <div class="messages-layout">
      <aside v-if="!mobileShowsDetail" class="messages-sidebar">
        <div class="messages-categories">
          <button
            v-for="item in categoryOptions"
            :key="item.id"
            type="button"
            class="messages-category"
            :class="{ 'is-active': selectedCategory === item.id }"
            @click="openCategory(item.id)"
          >
            <strong>{{ item.label }}</strong>
            <span v-if="categoryUnreadCount(item.id)" class="messages-category__badge">
              {{ categoryUnreadCount(item.id) }}
            </span>
          </button>
        </div>

        <div v-if="chatStore.loading && conversationDisplayList.length === 0" class="messages-conversation-list">
          <div class="messages-loading">
            <el-skeleton animated :rows="3" />
          </div>
        </div>

        <div v-else-if="conversationDisplayList.length" class="messages-conversation-list">
          <button
            v-for="item in conversationDisplayList"
            :key="item.id"
            type="button"
            class="messages-conversation"
            :class="{ 'is-active': activeConversation?.id === item.id }"
            @click="openConversation(item.id)"
          >
            <div class="messages-avatar">{{ item.title.slice(0, 1) }}</div>
            <div class="messages-conversation__content">
              <div class="messages-conversation__top">
                <strong>{{ item.title }}</strong>
                <span class="messages-time">{{ item.updatedAt }}</span>
              </div>
              <p class="messages-preview">{{ item.preview }}</p>
            </div>
            <span v-if="item.unread" class="messages-unread">{{ item.unread }}</span>
          </button>
        </div>

        <EmptyState
          v-else
          emoji="○"
          title="当前分类还没有会话"
          :description="selectedCategory === 'group'
            ? '群聊功能即将上线，敬请期待。'
            : '暂时还没有这个分类的消息。'"
        >
          <div v-if="selectedCategory === 'group'" class="messages-group-placeholders">
            <article
              v-for="item in groupPlaceholders"
              :key="item.title"
              class="messages-group-placeholder"
            >
              <strong>{{ item.title }}</strong>
              <p>{{ item.description }}</p>
            </article>
          </div>
        </EmptyState>
      </aside>

      <main class="messages-main">
        <div v-if="mobileShowsDetail" class="messages-mobile-back">
          <button type="button" class="messages-back-btn" @click="backToList">
            返回
          </button>
        </div>

        <NotificationsView v-if="selectedCategoryIsNotifications" embedded />

        <template v-else-if="activeConversation">
          <header class="messages-detail-header">
            <div class="messages-avatar messages-avatar--large">
              {{ (activeConversation.peerUser?.nickname || activeConversation.peerUser?.username || '用户').slice(0, 1) }}
            </div>
            <h2>{{ activeConversation.peerUser?.nickname || activeConversation.peerUser?.username || '用户' }}</h2>
          </header>

          <div ref="messagesThreadRef" class="messages-thread">
            <div v-if="chatStore.loading && displayMessages.length === 0" class="messages-loading">
              <el-skeleton animated :rows="5" />
            </div>

            <template v-else>
              <article
                v-for="message in displayMessages"
                :key="message.id"
                class="messages-bubble"
                :class="[
                  `messages-bubble--${message.role}`,
                  { 'messages-bubble--image': message.messageType === 'image' }
                ]"
              >
                <template v-if="message.messageType === 'image'">
                  <button
                    v-if="message.mediaUrl && !message.imageFailed"
                    type="button"
                    class="messages-image-btn"
                    @click="openImagePreview(message.mediaUrl)"
                  >
                    <img
                      :src="message.mediaUrl"
                      :alt="message.body || '图片消息'"
                      class="messages-image"
                      @error="handleImageError(message.id)"
                    />
                  </button>
                  <div v-else class="messages-image-fallback">
                    图片加载失败
                  </div>
                  <p v-if="message.body" class="messages-image-caption">{{ message.body }}</p>
                </template>
                <p v-else>{{ message.body }}</p>
              </article>
            </template>
            <div ref="messagesEndRef"></div>
          </div>

          <div class="messages-input-area">
            <div class="messages-composer">
              <ImageUploader @selected="handleImageSelected" />
              <input
                v-model="messageInput"
                type="text"
                placeholder="输入消息..."
                class="messages-input"
                @keyup.enter="handleSendMessage"
              />
              <button
                type="button"
                class="messages-send-btn"
                :class="{ 'is-active': messageInput.trim() }"
                :disabled="!messageInput.trim() || chatStore.sending"
                @click="handleSendMessage"
              >
                {{ chatStore.sending ? '发送中...' : '发送' }}
              </button>
            </div>
          </div>
        </template>

        <EmptyState
          v-else
          emoji="○"
          title="选择一个会话查看详情"
          description="点击左侧会话列表中的任意一条消息开始查看。"
        />
      </main>
    </div>

    <ElImageViewer
      v-if="previewImageUrl"
      :url-list="[previewImageUrl]"
      :initial-index="0"
      hide-on-click-modal
      @close="closeImagePreview"
    />
  </div>
</template>

<style scoped>
.messages-container {
  --msg-primary: #EA580C;
  --msg-primary-light: #FED7AA;
  --msg-primary-bg: #FFF7ED;
  --msg-warm-white: #FFFBF5;
  --msg-paper: #FAFAF9;
  --msg-warm-yellow: #FEF3C7;
  --msg-warm-yellow-text: #92400E;
  --msg-text-primary: #1F2937;
  --msg-text-secondary: #6B7280;
  --msg-text-tertiary: #9CA3AF;
  --msg-warm-gray: #78716C;
  --msg-space-xs: 8px;
  --msg-space-sm: 12px;
  --msg-space-md: 16px;
  --msg-space-lg: 20px;
  --msg-space-xl: 32px;
  --msg-space-xxl: 56px;
  --msg-radius-sm: 12px;
  --msg-radius-md: 16px;
  --msg-radius-lg: 20px;
  --msg-radius-xl: 24px;
  --msg-radius-pill: 999px;
  --msg-shadow-soft: 0 2px 8px rgba(0, 0, 0, 0.04);
  --msg-shadow-md: 0 4px 12px rgba(0, 0, 0, 0.08);
  --msg-shadow-primary: 0 2px 12px rgba(234, 88, 12, 0.2);
  --msg-transition-fast: 160ms ease-out;
  --msg-transition-base: 220ms ease-out;
  --msg-transition-slow: 280ms cubic-bezier(0.4, 0.0, 0.2, 1);
  max-width: 1400px;
  margin: 0 auto;
  padding: var(--msg-space-xl) var(--msg-space-xl) var(--msg-space-xxl);
}

.messages-header {
  display: flex;
  align-items: center;
  gap: var(--msg-space-md);
  margin-bottom: var(--msg-space-xxl);
}

.messages-header h1 {
  font-size: 28px;
  font-weight: 600;
  color: var(--msg-text-primary);
  margin: 0;
}

.messages-layout {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: var(--msg-space-lg);
  min-height: 720px;
}

.messages-sidebar {
  display: flex;
  flex-direction: column;
  gap: var(--msg-space-lg);
}

.messages-categories {
  display: flex;
  gap: var(--msg-space-sm);
  flex-wrap: wrap;
}

.messages-category {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: var(--msg-space-sm) var(--msg-space-lg);
  border: none;
  border-radius: var(--msg-radius-md);
  background: var(--msg-primary-bg);
  color: var(--msg-warm-gray);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--msg-transition-fast);
}

.messages-category__badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: var(--msg-radius-pill);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #DC2626;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
}

.messages-category:hover {
  transform: translateY(-2px);
  box-shadow: var(--msg-shadow-soft);
}

.messages-category.is-active {
  background: var(--msg-primary-light);
  color: var(--msg-primary);
  box-shadow: var(--msg-shadow-md);
}

.messages-loading {
  padding: var(--msg-space-lg);
}

.messages-conversation-list {
  display: flex;
  flex-direction: column;
  gap: var(--msg-space-sm);
}

.messages-conversation {
  display: flex;
  align-items: flex-start;
  gap: var(--msg-space-md);
  padding: var(--msg-space-md) 18px;
  border: none;
  border-radius: var(--msg-radius-lg);
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: all var(--msg-transition-fast);
  position: relative;
}

.messages-conversation:hover {
  background: var(--msg-warm-white);
  transform: translateY(-2px);
  box-shadow: var(--msg-shadow-soft);
}

.messages-conversation.is-active {
  background: linear-gradient(135deg, var(--msg-primary-bg) 0%, var(--msg-primary-light) 100%);
  box-shadow: var(--msg-shadow-md);
}

.messages-avatar {
  width: 48px;
  height: 48px;
  border-radius: var(--msg-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(234, 88, 12, 0.12);
  color: var(--msg-primary);
  font-size: 18px;
  font-weight: 700;
  flex-shrink: 0;
}

.messages-avatar--large {
  width: 40px;
  height: 40px;
  border-radius: var(--msg-radius-sm);
}

.messages-conversation__content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--msg-space-xs);
}

.messages-conversation__top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: var(--msg-space-sm);
}

.messages-conversation__top strong {
  font-size: 15px;
  font-weight: 600;
  color: var(--msg-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.messages-time {
  font-size: 12px;
  color: var(--msg-text-tertiary);
  flex-shrink: 0;
}

.messages-preview {
  font-size: 14px;
  color: var(--msg-text-secondary);
  line-height: 1.5;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.messages-unread {
  position: absolute;
  top: var(--msg-space-md);
  right: 18px;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: var(--msg-radius-pill);
  display: flex;
  align-items: center;
  justify-content: center;
  background: #DC2626;
  color: white;
  font-size: 12px;
  font-weight: 700;
}

.messages-main {
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: var(--msg-radius-xl);
  box-shadow: var(--msg-shadow-soft);
  overflow: hidden;
}

.messages-mobile-back {
  padding: var(--msg-space-md) var(--msg-space-lg);
  border-bottom: 1px solid var(--msg-paper);
}

.messages-back-btn {
  padding: var(--msg-space-xs) var(--msg-space-md);
  border: none;
  border-radius: 10px;
  background: var(--msg-paper);
  color: var(--msg-text-primary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--msg-transition-fast);
}

.messages-back-btn:hover {
  background: var(--msg-primary-bg);
  color: var(--msg-primary);
}

.messages-detail-header {
  display: flex;
  align-items: center;
  gap: var(--msg-space-md);
  padding: var(--msg-space-lg) var(--msg-space-xl);
  border-bottom: 1px solid var(--msg-paper);
}

.messages-detail-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: var(--msg-text-primary);
  margin: 0;
}

.messages-thread {
  flex: 1;
  padding: var(--msg-space-xl);
  background: var(--msg-paper);
  display: flex;
  flex-direction: column;
  gap: var(--msg-space-lg);
  overflow-y: auto;
}

.messages-bubble {
  max-width: 65%;
  padding: 14px 18px;
  border-radius: var(--msg-radius-lg);
}

.messages-bubble p {
  margin: 0;
  font-size: 15px;
  line-height: 1.6;
  color: var(--msg-text-primary);
}

.messages-bubble--other {
  align-self: flex-start;
  background: white;
  border-radius: var(--msg-radius-lg) var(--msg-radius-lg) var(--msg-radius-lg) 6px;
  box-shadow: var(--msg-shadow-soft);
}

.messages-bubble--self {
  align-self: flex-end;
  background: linear-gradient(135deg, #FB923C 0%, var(--msg-primary) 100%);
  border-radius: var(--msg-radius-lg) var(--msg-radius-lg) 6px var(--msg-radius-lg);
  box-shadow: var(--msg-shadow-primary);
}

.messages-bubble--self p {
  color: white;
}

.messages-bubble--image {
  padding: 8px;
  background: white;
}

.messages-bubble--self.messages-bubble--image {
  background: #FFF7ED;
}

.messages-image-btn {
  display: block;
  padding: 0;
  border: none;
  background: transparent;
  cursor: zoom-in;
  border-radius: 12px;
  overflow: hidden;
}

.messages-image {
  display: block;
  max-width: min(300px, 58vw);
  max-height: 360px;
  width: auto;
  height: auto;
  border-radius: 12px;
  object-fit: contain;
}

.messages-image-fallback {
  width: min(300px, 58vw);
  min-height: 132px;
  border-radius: 12px;
  background: #E7E5E4;
  color: var(--msg-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.messages-image-caption {
  margin-top: 8px !important;
  color: var(--msg-text-secondary) !important;
}

.messages-input-area {
  padding: var(--msg-space-lg) var(--msg-space-xl);
  background: var(--msg-warm-white);
  border-top: 1px solid var(--msg-paper);
  display: flex;
  flex-direction: column;
  gap: var(--msg-space-md);
}

.messages-composer {
  display: flex;
  gap: var(--msg-space-sm);
  align-items: center;
}

.messages-input {
  flex: 1;
  min-width: 0;
  padding: var(--msg-space-sm) 18px;
  border: 1px solid var(--msg-paper);
  border-radius: 18px;
  background: white;
  color: var(--msg-text-primary);
  font-size: 15px;
  font-family: inherit;
  outline: none;
  transition: all var(--msg-transition-fast);
}

.messages-input:focus {
  border-color: var(--msg-primary-light);
}

.messages-send-btn {
  padding: var(--msg-space-sm) 28px;
  border: none;
  border-radius: 18px;
  background: var(--msg-text-tertiary);
  color: white;
  font-size: 15px;
  font-weight: 600;
  cursor: not-allowed;
  transition: all var(--msg-transition-fast);
  flex-shrink: 0;
}

.messages-send-btn.is-active {
  background: linear-gradient(135deg, #FB923C 0%, var(--msg-primary) 100%);
  cursor: pointer;
}

.messages-send-btn.is-active:hover {
  transform: translateY(-2px);
  box-shadow: var(--msg-shadow-primary);
}

.messages-send-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.messages-group-placeholders {
  display: flex;
  flex-direction: column;
  gap: var(--msg-space-sm);
  margin-top: var(--msg-space-md);
}

.messages-group-placeholder {
  padding: var(--msg-space-md) 18px;
  border-radius: 18px;
  background: var(--msg-warm-white);
  border: 1px solid var(--msg-paper);
}

.messages-group-placeholder strong {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: var(--msg-text-primary);
  margin-bottom: var(--msg-space-xs);
}

.messages-group-placeholder p {
  margin: 0;
  font-size: 14px;
  color: var(--msg-text-secondary);
  line-height: 1.6;
}

@media (max-width: 900px) {
  .messages-layout {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .messages-container {
    padding: var(--msg-space-lg);
  }

  .messages-header {
    margin-bottom: var(--msg-space-xl);
  }

  .messages-categories {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .messages-thread {
    padding: var(--msg-space-lg);
  }

  .messages-input-area {
    padding: var(--msg-space-md);
  }

  .messages-bubble {
    max-width: 85%;
  }
}

@media (max-width: 640px) {
  .messages-container {
    padding: var(--msg-space-md);
  }

  .messages-header h1 {
    font-size: 24px;
  }

  .messages-conversation {
    padding: var(--msg-space-sm) var(--msg-space-md);
  }

  .messages-avatar {
    width: 40px;
    height: 40px;
  }

  .messages-detail-header {
    padding: var(--msg-space-md);
  }

  .messages-thread {
    padding: var(--msg-space-md);
  }

  .messages-bubble {
    max-width: 90%;
  }

  .messages-send-btn {
    padding: var(--msg-space-sm) 18px;
  }
}
</style>
