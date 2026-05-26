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
