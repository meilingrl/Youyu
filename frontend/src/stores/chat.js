import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as chatApi from '@/api/modules/chat'

export const useChatStore = defineStore('chat', () => {
  // State
  const conversations = ref([])
  const activeConversationId = ref(null)
  const messages = ref([])
  const loading = ref(false)
  const sending = ref(false)
  const pollingTimer = ref(null)

  // Computed
  const activeConversation = computed(() => {
    if (!activeConversationId.value) return null
    return conversations.value.find((c) => c.id === activeConversationId.value)
  })

  // Actions
  async function fetchConversations(page = 0, size = 20) {
    loading.value = true
    try {
      const response = await chatApi.getConversations({ page, size })
      conversations.value = response.content
      return response
    } catch (error) {
      console.error('Failed to fetch conversations:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function findOrCreateConversation(peerUserId, productId, shopId) {
    loading.value = true
    try {
      const conversation = await chatApi.createConversation({
        peerUserId,
        productId,
        shopId
      })
      // Add to list if not exists
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

  async function fetchMessages(conversationId, page = 0, size = 50) {
    loading.value = true
    try {
      const response = await chatApi.getMessages(conversationId, { page, size })
      // Backend returns descending order, frontend needs ascending
      messages.value = response.content.reverse()
      return response
    } catch (error) {
      console.error('Failed to fetch messages:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function sendMessage(conversationId, body) {
    if (!body.trim()) return

    sending.value = true
    try {
      const message = await chatApi.sendMessage(conversationId, { body })
      // Add to message list
      messages.value.push(message)
      // Update conversation lastMessageAt
      const conv = conversations.value.find((c) => c.id === conversationId)
      if (conv) {
        conv.lastMessageAt = message.createdAt
      }
      return message
    } catch (error) {
      console.error('Failed to send message:', error)
      throw error
    } finally {
      sending.value = false
    }
  }

  // Polling logic
  function startPolling(conversationId, interval = 8000) {
    stopPolling()
    pollingTimer.value = setInterval(async () => {
      try {
        const response = await chatApi.getMessages(conversationId, { page: 0, size: 50 })
        const newMessages = response.content.reverse()
        // Only update if there are new messages (avoid flicker)
        if (newMessages.length > messages.value.length) {
          messages.value = newMessages
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

  // Cleanup
  function $reset() {
    conversations.value = []
    activeConversationId.value = null
    messages.value = []
    loading.value = false
    sending.value = false
    stopPolling()
  }

  return {
    // State
    conversations,
    activeConversationId,
    messages,
    loading,
    sending,

    // Computed
    activeConversation,

    // Actions
    fetchConversations,
    findOrCreateConversation,
    fetchMessages,
    sendMessage,
    startPolling,
    stopPolling,
    $reset
  }
})
