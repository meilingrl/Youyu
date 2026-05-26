import service from '@/api/client'

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
  return service.get(`/chat/conversations/${conversationId}/messages`, { params })
}

export async function getUnreadCount() {
  return service.get('/chat/unread-count')
}

export async function markConversationRead(conversationId) {
  return service.post(`/chat/conversations/${conversationId}/read`)
}

/**
 * 发送消息
 */
export async function sendMessage(conversationId, data) {
  return service.post(`/chat/conversations/${conversationId}/messages`, data)
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
