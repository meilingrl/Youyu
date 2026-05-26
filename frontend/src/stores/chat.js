import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as chatApi from '@/api/modules/chat'

function unwrapData(response) {
  return response?.data ?? response
}

function normalizeConversation(conversation = {}) {
  const unreadCount = Number(conversation.unreadCount ?? conversation.unread_count ?? 0)

  return {
    ...conversation,
    id: conversation.id,
    userAId: conversation.userAId ?? conversation.user_a_id,
    userBId: conversation.userBId ?? conversation.user_b_id,
    productId: conversation.productId ?? conversation.product_id,
    shopId: conversation.shopId ?? conversation.shop_id,
    lastMessageAt: conversation.lastMessageAt ?? conversation.last_message_at,
    createdAt: conversation.createdAt ?? conversation.created_at,
    updatedAt: conversation.updatedAt ?? conversation.updated_at,
    unreadCount: Number.isFinite(unreadCount) ? unreadCount : 0,
    peerUser: conversation.peerUser ?? conversation.peer_user ?? null
  }
}

function normalizeMessage(message = {}) {
  return {
    ...message,
    id: message.id,
    conversationId: message.conversationId ?? message.conversation_id,
    senderId: message.senderId ?? message.senderUserId ?? message.sender_user_id ?? message.sender_id,
    senderUserId: message.senderUserId ?? message.senderId ?? message.sender_user_id ?? message.sender_id,
    messageType: message.messageType ?? message.message_type ?? 'text',
    mediaUrl: message.mediaUrl ?? message.media_url ?? null,
    productId: message.productId ?? message.product_id ?? null,
    product: message.product ?? null,
    orderId: message.orderId ?? message.order_id ?? null,
    order: message.order ?? null,
    createdAt: message.createdAt ?? message.created_at
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

export const useChatStore = defineStore('chat', () => {
  const conversations = ref([])
  const activeConversationId = ref(null)
  const messages = ref([])
  const quickReplies = ref([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const quickRepliesLoading = ref(false)
  const sending = ref(false)
  const pollingTimer = ref(null)

  const activeConversation = computed(() => {
    if (!activeConversationId.value) return null
    return conversations.value.find((c) => c.id === activeConversationId.value)
  })

  async function fetchUnreadCount() {
    try {
      const payload = unwrapData(await chatApi.getUnreadCount())
      const count = Number(payload?.count ?? payload?.unreadCount ?? payload ?? 0)
      unreadCount.value = Number.isFinite(count) ? count : 0
      return unreadCount.value
    } catch (error) {
      console.error('Failed to fetch unread count:', error)
      throw error
    }
  }

  async function fetchConversations(page = 0, size = 20, options = {}) {
    if (!options.silent) {
      loading.value = true
    }

    try {
      const payload = unwrapData(await chatApi.getConversations({ page, size }))
      const content = payload?.content ?? []
      conversations.value = content.map(normalizeConversation)
      unreadCount.value = conversations.value.reduce((total, item) => total + (item.unreadCount || 0), 0)
      return payload
    } catch (error) {
      console.error('Failed to fetch conversations:', error)
      throw error
    } finally {
      if (!options.silent) {
        loading.value = false
      }
    }
  }

  async function findOrCreateConversation(peerUserId, productId, shopId) {
    loading.value = true
    try {
      const conversation = normalizeConversation(unwrapData(await chatApi.createConversation({
        peerUserId,
        productId,
        shopId
      })))
      const exists = conversations.value.find((c) => c.id === conversation.id)
      if (!exists) {
        conversations.value.unshift(conversation)
      }
      activeConversationId.value = conversation.id
      await fetchMessages(conversation.id)
      return conversation
    } catch (error) {
      console.error('Failed to create conversation:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function markConversationRead(conversationId) {
    await chatApi.markConversationRead(conversationId)

    const conversation = conversations.value.find((c) => c.id === conversationId)
    const previousUnreadCount = conversation?.unreadCount || 0
    if (conversation) {
      conversation.unreadCount = 0
    }
    unreadCount.value = Math.max(0, unreadCount.value - previousUnreadCount)

    try {
      await fetchUnreadCount()
    } catch (error) {
      console.error('Failed to refresh unread count after read:', error)
    }
  }

  async function fetchMessages(conversationId, page = 0, size = 50, options = {}) {
    if (!options.silent) {
      loading.value = true
    }

    try {
      const payload = unwrapData(await chatApi.getMessages(conversationId, { page, size }))
      messages.value = (payload?.content ?? []).map(normalizeMessage).reverse()

      if (conversationId) {
        try {
          await markConversationRead(conversationId)
        } catch (error) {
          console.error('Failed to mark conversation read:', error)
        }
      }

      return payload
    } catch (error) {
      console.error('Failed to fetch messages:', error)
      throw error
    } finally {
      if (!options.silent) {
        loading.value = false
      }
    }
  }

  async function sendMessage(conversationId, payload) {
    const messagePayload = typeof payload === 'string'
      ? { body: payload, messageType: 'text' }
      : { messageType: 'text', ...payload }

    const isTextMessage = (messagePayload.messageType ?? 'text') === 'text'
    if (isTextMessage && !messagePayload.body?.trim()) return null
    if (messagePayload.messageType === 'image' && !messagePayload.mediaUrl) return null
    if (messagePayload.messageType === 'product_card' && !messagePayload.productId) return null
    if (messagePayload.messageType === 'order_card' && !messagePayload.orderId) return null

    sending.value = true
    try {
      const message = normalizeMessage(unwrapData(await chatApi.sendMessage(conversationId, messagePayload)))
      messages.value.push(message)

      const conversation = conversations.value.find((c) => c.id === conversationId)
      if (conversation) {
        conversation.lastMessageAt = message.createdAt
      }

      return message
    } catch (error) {
      console.error('Failed to send message:', error)
      throw error
    } finally {
      sending.value = false
    }
  }

  function sendProductCardMessage(conversationId, productId, body = '') {
    return sendMessage(conversationId, {
      body,
      messageType: 'product_card',
      productId
    })
  }

  function sendOrderCardMessage(conversationId, orderId, body = '') {
    return sendMessage(conversationId, {
      body,
      messageType: 'order_card',
      orderId
    })
  }

  async function fetchQuickReplies() {
    quickRepliesLoading.value = true
    try {
      const payload = unwrapData(await chatApi.getQuickReplies())
      quickReplies.value = (Array.isArray(payload) ? payload : []).map(normalizeQuickReply)
      return quickReplies.value
    } catch (error) {
      console.error('Failed to fetch quick replies:', error)
      throw error
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
        await fetchConversations(0, 20, { silent: true })

        if (conversationId) {
          const payload = unwrapData(await chatApi.getMessages(conversationId, { page: 0, size: 50 }))
          const newMessages = (payload?.content ?? []).map(normalizeMessage).reverse()
          if (newMessages.length !== messages.value.length || newMessages.at(-1)?.id !== messages.value.at(-1)?.id) {
            messages.value = newMessages
            await markConversationRead(conversationId)
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
    quickReplies.value = []
    unreadCount.value = 0
    loading.value = false
    quickRepliesLoading.value = false
    sending.value = false
    stopPolling()
  }

  return {
    conversations,
    activeConversationId,
    messages,
    quickReplies,
    unreadCount,
    loading,
    quickRepliesLoading,
    sending,
    activeConversation,
    fetchUnreadCount,
    fetchConversations,
    findOrCreateConversation,
    markConversationRead,
    fetchMessages,
    sendMessage,
    sendProductCardMessage,
    sendOrderCardMessage,
    fetchQuickReplies,
    createQuickReply,
    updateQuickReply,
    deleteQuickReply,
    startPolling,
    stopPolling,
    $reset
  }
})
