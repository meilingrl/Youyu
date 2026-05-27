import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as chatApi from '@/api/modules/chat'

function unwrapData(response) {
  return response?.data ?? response
}

function toBoolean(value) {
  if (typeof value === 'boolean') return value
  if (typeof value === 'number') return value !== 0
  if (typeof value === 'string') return ['true', '1', 'yes'].includes(value.toLowerCase())
  return false
}

function previewFor(message = {}) {
  const type = message.messageType ?? message.message_type
  if (toBoolean(message.isRecalled ?? message.is_recalled)) return '消息已撤回'
  if (type === 'image') return '[图片]'
  if (type === 'product_card') return '[商品卡片]'
  if (type === 'order_card') return '[订单卡片]'
  return message.body ?? ''
}

function normalizeMessage(message = {}) {
  return {
    ...message,
    id: message.id,
    conversationId: message.conversationId ?? message.conversation_id,
    senderId: message.senderId ?? message.senderUserId ?? message.sender_user_id ?? message.sender_id,
    senderUserId: message.senderUserId ?? message.senderId ?? message.sender_user_id ?? message.sender_id,
    body: message.body ?? '',
    messageType: message.messageType ?? message.message_type ?? 'text',
    mediaUrl: message.mediaUrl ?? message.media_url ?? null,
    productId: message.productId ?? message.product_id ?? null,
    product: message.product ?? null,
    orderId: message.orderId ?? message.order_id ?? null,
    order: message.order ?? null,
    isRead: toBoolean(message.isRead ?? message.is_read),
    isRecalled: toBoolean(message.isRecalled ?? message.is_recalled),
    recalledAt: message.recalledAt ?? message.recalled_at ?? null,
    createdAt: message.createdAt ?? message.created_at
  }
}

function normalizeConversation(conversation = {}) {
  const unreadCount = Number(conversation.unreadCount ?? conversation.unread_count ?? 0)
  const lastMessage = conversation.lastMessage ?? conversation.last_message ?? null
  return {
    ...conversation,
    id: conversation.id,
    userAId: conversation.userAId ?? conversation.user_a_id,
    userBId: conversation.userBId ?? conversation.user_b_id,
    productId: conversation.productId ?? conversation.product_id,
    shopId: conversation.shopId ?? conversation.shop_id,
    peerUser: conversation.peerUser ?? conversation.peer_user ?? null,
    unreadCount: Number.isFinite(unreadCount) ? unreadCount : 0,
    isPinned: toBoolean(conversation.isPinned ?? conversation.is_pinned),
    isMuted: toBoolean(conversation.isMuted ?? conversation.is_muted),
    lastMessageAt: conversation.lastMessageAt ?? conversation.last_message_at,
    lastMessagePreview:
      conversation.lastMessagePreview
      ?? conversation.last_message_preview
      ?? (lastMessage ? previewFor(lastMessage) : ''),
    lastMessageType: conversation.lastMessageType ?? conversation.last_message_type ?? lastMessage?.messageType,
    createdAt: conversation.createdAt ?? conversation.created_at,
    updatedAt: conversation.updatedAt ?? conversation.updated_at
  }
}

function normalizeQuickReply(quickReply = {}) {
  return {
    ...quickReply,
    id: quickReply.id,
    userId: quickReply.userId ?? quickReply.user_id,
    content: quickReply.content ?? '',
    sortOrder: quickReply.sortOrder ?? quickReply.sort_order ?? 0,
    createdAt: quickReply.createdAt ?? quickReply.created_at,
    updatedAt: quickReply.updatedAt ?? quickReply.updated_at
  }
}

function sortConversations(a, b) {
  if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1
  const aTime = new Date(a.lastMessageAt || a.updatedAt || a.createdAt || 0).getTime()
  const bTime = new Date(b.lastMessageAt || b.updatedAt || b.createdAt || 0).getTime()
  return bTime - aTime
}

export const useChatStore = defineStore('chat', () => {
  const conversations = ref([])
  const activeConversationId = ref(null)
  const messages = ref([])
  const searchResults = ref([])
  const searchPagination = ref({ page: 0, size: 20, total: 0, totalPages: 0 })
  const quickReplies = ref([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const searchLoading = ref(false)
  const quickRepliesLoading = ref(false)
  const sending = ref(false)
  const pollingTimer = ref(null)

  const activeConversation = computed(() => {
    if (!activeConversationId.value) return null
    return conversations.value.find((item) => String(item.id) === String(activeConversationId.value)) || null
  })

  function refreshUnreadCount() {
    unreadCount.value = conversations.value.reduce((total, item) => total + (item.isMuted ? 0 : item.unreadCount || 0), 0)
  }

  async function fetchUnreadCount() {
    const payload = unwrapData(await chatApi.getUnreadCount())
    const count = Number(payload?.count ?? payload?.unreadCount ?? payload ?? 0)
    unreadCount.value = Number.isFinite(count) ? count : 0
    return unreadCount.value
  }

  async function fetchConversations(page = 0, size = 50, options = {}) {
    if (!options.silent) loading.value = true
    try {
      const payload = unwrapData(await chatApi.getConversations({ page, size }))
      conversations.value = (payload?.content ?? []).map(normalizeConversation).sort(sortConversations)
      refreshUnreadCount()
      return payload
    } finally {
      if (!options.silent) loading.value = false
    }
  }

  async function fetchMessages(conversationId, page = 0, size = 50, options = {}) {
    if (!options.silent) loading.value = true
    try {
      const payload = unwrapData(await chatApi.getMessages(conversationId, { page, size }))
      messages.value = (payload?.content ?? []).map(normalizeMessage).reverse()
      if (conversationId) {
        await markConversationRead(conversationId).catch(() => {})
      }
      return payload
    } finally {
      if (!options.silent) loading.value = false
    }
  }

  async function findOrCreateConversation(peerUserId, productId, shopId) {
    loading.value = true
    try {
      const conversation = normalizeConversation(unwrapData(await chatApi.createConversation({ peerUserId, productId, shopId })))
      const existing = conversations.value.find((item) => String(item.id) === String(conversation.id))
      if (!existing) conversations.value.unshift(conversation)
      activeConversationId.value = conversation.id
      await fetchMessages(conversation.id)
      return conversation
    } finally {
      loading.value = false
    }
  }

  async function sendMessage(conversationId, payload) {
    const messagePayload = typeof payload === 'string'
      ? { body: payload, messageType: 'text' }
      : { messageType: 'text', ...payload }
    if (messagePayload.messageType === 'text' && !messagePayload.body?.trim()) return null
    if (messagePayload.messageType === 'image' && !messagePayload.mediaUrl) return null
    if (messagePayload.messageType === 'product_card' && !messagePayload.productId) return null
    if (messagePayload.messageType === 'order_card' && !messagePayload.orderId) return null

    sending.value = true
    try {
      const message = normalizeMessage(unwrapData(await chatApi.sendMessage(conversationId, messagePayload)))
      messages.value.push(message)
      const conversation = conversations.value.find((item) => String(item.id) === String(conversationId))
      if (conversation) {
        conversation.lastMessageAt = message.createdAt
        conversation.lastMessagePreview = previewFor(message)
        conversation.lastMessageType = message.messageType
        conversations.value = [...conversations.value].sort(sortConversations)
      }
      return message
    } finally {
      sending.value = false
    }
  }

  function sendProductCardMessage(conversationId, productId, body = '') {
    return sendMessage(conversationId, { body, messageType: 'product_card', productId })
  }

  function sendOrderCardMessage(conversationId, orderId, body = '') {
    return sendMessage(conversationId, { body, messageType: 'order_card', orderId })
  }

  async function markConversationRead(conversationId) {
    await chatApi.markConversationRead(conversationId)
    const conversation = conversations.value.find((item) => String(item.id) === String(conversationId))
    if (conversation) {
      conversation.unreadCount = 0
      refreshUnreadCount()
    }
  }

  async function searchMessages(params = {}) {
    searchLoading.value = true
    try {
      const payload = unwrapData(await chatApi.searchMessages(params))
      const content = (payload?.content ?? []).map(normalizeMessage)
      searchResults.value = Number(params.page ?? 0) > 0 ? [...searchResults.value, ...content] : content
      searchPagination.value = {
        page: Number(payload?.page ?? payload?.number ?? params.page ?? 0),
        size: Number(payload?.size ?? params.size ?? 20),
        total: Number(payload?.total ?? payload?.totalElements ?? content.length),
        totalPages: Number(payload?.totalPages ?? payload?.total_pages ?? 1)
      }
      return payload
    } finally {
      searchLoading.value = false
    }
  }

  function clearSearchResults() {
    searchResults.value = []
    searchPagination.value = { page: 0, size: 20, total: 0, totalPages: 0 }
  }

  async function setConversationPinned(conversationId, isPinned) {
    await chatApi.setConversationPinned(conversationId, isPinned)
    const conversation = conversations.value.find((item) => String(item.id) === String(conversationId))
    if (conversation) {
      conversation.isPinned = isPinned
      conversations.value = [...conversations.value].sort(sortConversations)
    }
  }

  async function setConversationMuted(conversationId, isMuted) {
    await chatApi.setConversationMuted(conversationId, isMuted)
    const conversation = conversations.value.find((item) => String(item.id) === String(conversationId))
    if (conversation) {
      conversation.isMuted = isMuted
      refreshUnreadCount()
    }
  }

  async function deleteConversation(conversationId) {
    await chatApi.deleteConversation(conversationId)
    conversations.value = conversations.value.filter((item) => String(item.id) !== String(conversationId))
    if (String(activeConversationId.value) === String(conversationId)) {
      activeConversationId.value = null
      messages.value = []
    }
    refreshUnreadCount()
  }

  async function recallMessage(messageId) {
    await chatApi.recallMessage(messageId)
    const message = messages.value.find((item) => String(item.id) === String(messageId))
    if (message) {
      message.isRecalled = true
      message.recalledAt = new Date().toISOString()
      if (messages.value.at(-1)?.id === message.id && activeConversationId.value) {
        const conversation = conversations.value.find((item) => String(item.id) === String(activeConversationId.value))
        if (conversation) conversation.lastMessagePreview = '消息已撤回'
      }
    }
  }

  async function fetchQuickReplies() {
    quickRepliesLoading.value = true
    try {
      const payload = unwrapData(await chatApi.getQuickReplies())
      quickReplies.value = (Array.isArray(payload) ? payload : []).map(normalizeQuickReply)
      return quickReplies.value
    } finally {
      quickRepliesLoading.value = false
    }
  }

  async function createQuickReply(payload) {
    const response = unwrapData(await chatApi.createQuickReply(payload))
    await fetchQuickReplies()
    return response
  }

  async function updateQuickReply(id, payload) {
    await chatApi.updateQuickReply(id, payload)
    await fetchQuickReplies()
  }

  async function deleteQuickReply(id) {
    await chatApi.deleteQuickReply(id)
    quickReplies.value = quickReplies.value.filter((item) => item.id !== id)
  }

  function startPolling(conversationId, interval = 8000) {
    stopPolling()
    pollingTimer.value = setInterval(async () => {
      try {
        await fetchConversations(0, 50, { silent: true })
        if (conversationId) {
          const payload = unwrapData(await chatApi.getMessages(conversationId, { page: 0, size: 50 }))
          const nextMessages = (payload?.content ?? []).map(normalizeMessage).reverse()
          if (nextMessages.length !== messages.value.length || nextMessages.at(-1)?.id !== messages.value.at(-1)?.id) {
            messages.value = nextMessages
            await markConversationRead(conversationId).catch(() => {})
          }
        }
      } catch (error) {
        console.error('Polling failed:', error)
      }
    }, interval)
  }

  function stopPolling() {
    if (pollingTimer.value) {
      clearInterval(pollingTimer.value)
      pollingTimer.value = null
    }
  }

  function $reset() {
    conversations.value = []
    activeConversationId.value = null
    messages.value = []
    searchResults.value = []
    searchPagination.value = { page: 0, size: 20, total: 0, totalPages: 0 }
    quickReplies.value = []
    unreadCount.value = 0
    loading.value = false
    searchLoading.value = false
    quickRepliesLoading.value = false
    sending.value = false
    stopPolling()
  }

  return {
    conversations,
    activeConversationId,
    messages,
    searchResults,
    searchPagination,
    quickReplies,
    unreadCount,
    loading,
    searchLoading,
    quickRepliesLoading,
    sending,
    activeConversation,
    fetchUnreadCount,
    fetchConversations,
    fetchMessages,
    findOrCreateConversation,
    sendMessage,
    sendProductCardMessage,
    sendOrderCardMessage,
    markConversationRead,
    searchMessages,
    clearSearchResults,
    setConversationPinned,
    setConversationMuted,
    deleteConversation,
    recallMessage,
    fetchQuickReplies,
    createQuickReply,
    updateQuickReply,
    deleteQuickReply,
    startPolling,
    stopPolling,
    $reset
  }
})
