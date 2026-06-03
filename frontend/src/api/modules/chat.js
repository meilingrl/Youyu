import service from '@/api/client'
import { isValidEntityId } from '@/utils/id-utils'

function rejectInvalidConversationId(conversationId) {
  if (!isValidEntityId(conversationId)) {
    return Promise.reject(new Error('无效的会话 ID'))
  }
  return null
}

function normalizeConversationId(conversationId) {
  return String(conversationId).trim()
}

/**
 * 获取会话列表
 */
export async function getConversations(params) {
  return service.get('/chat/conversations', { params })
}

/**
 * 查找或创建会话
 */
export async function createConversation(data) {
  return service.post('/chat/conversations', data)
}

/**
 * 获取消息列表
 */
export async function getMessages(conversationId, params) {
  const invalid = rejectInvalidConversationId(conversationId)
  if (invalid) return invalid
  return service.get(`/chat/conversations/${normalizeConversationId(conversationId)}/messages`, { params })
}

export async function searchMessages(params) {
  return service.get('/chat/messages/search', { params })
}

export async function getUnreadCount() {
  return service.get('/chat/unread-count')
}

export async function markConversationRead(conversationId) {
  const invalid = rejectInvalidConversationId(conversationId)
  if (invalid) return invalid
  return service.post(`/chat/conversations/${normalizeConversationId(conversationId)}/read`)
}

export async function setConversationPinned(conversationId, isPinned) {
  const invalid = rejectInvalidConversationId(conversationId)
  if (invalid) return invalid
  return service.post(`/chat/conversations/${normalizeConversationId(conversationId)}/pin`, { pinned: isPinned })
}

export async function setConversationMuted(conversationId, isMuted) {
  const invalid = rejectInvalidConversationId(conversationId)
  if (invalid) return invalid
  return service.post(`/chat/conversations/${normalizeConversationId(conversationId)}/mute`, { muted: isMuted })
}

export async function deleteConversation(conversationId) {
  const invalid = rejectInvalidConversationId(conversationId)
  if (invalid) return invalid
  return service.delete(`/chat/conversations/${normalizeConversationId(conversationId)}`)
}

/**
 * 发送消息
 */
export async function sendMessage(conversationId, data) {
  const invalid = rejectInvalidConversationId(conversationId)
  if (invalid) return invalid
  return service.post(`/chat/conversations/${normalizeConversationId(conversationId)}/messages`, data)
}

export async function recallMessage(messageId) {
  if (!isValidEntityId(messageId)) {
    return Promise.reject(new Error('无效的消息 ID'))
  }
  return service.post(`/chat/messages/${String(messageId).trim()}/recall`)
}

export async function startSupportSession() {
  return service.post('/chat/support/session')
}

export async function escalateSupportConversation(conversationId) {
  const invalid = rejectInvalidConversationId(conversationId)
  if (invalid) return invalid
  return service.post(`/chat/conversations/${normalizeConversationId(conversationId)}/escalate`)
}

export async function closeSupportConversation(conversationId) {
  const invalid = rejectInvalidConversationId(conversationId)
  if (invalid) return invalid
  return service.post(`/chat/conversations/${normalizeConversationId(conversationId)}/close-support`)
}

export async function getAutoReplySettings() {
  return service.get('/chat/auto-reply')
}

export async function updateAutoReplySettings(data) {
  return service.put('/chat/auto-reply', data)
}

export async function getQuickReplies() {
  return service.get('/chat/quick-replies')
}

export async function createQuickReply(data) {
  return service.post('/chat/quick-replies', data)
}

export async function updateQuickReply(id, data) {
  return service.put(`/chat/quick-replies/${id}`, data)
}

export async function deleteQuickReply(id) {
  return service.delete(`/chat/quick-replies/${id}`)
}

export async function sendProductCardMessage(conversationId, data) {
  return sendMessage(conversationId, {
    ...data,
    messageType: 'product_card'
  })
}

export async function sendOrderCardMessage(conversationId, data) {
  return sendMessage(conversationId, {
    ...data,
    messageType: 'order_card'
  })
}
