package com.youyu.backend.service.chat;

import java.util.Map;

public interface ChatService {
    Map<String, Object> getConversations(Long userId, int page, int size);
    Map<String, Object> findOrCreateConversation(Long currentUserId, Long peerUserId, Long productId, Long shopId);
    Map<String, Object> getMessages(Long conversationId, Long currentUserId, int page, int size);
    Map<String, Object> sendMessage(Long conversationId, Long currentUserId, String body, String messageType, String mediaUrl, Long productId, Long orderId);
    Map<String, Object> getUnreadCount(Long currentUserId);
    void markConversationRead(Long conversationId, Long currentUserId);
}
