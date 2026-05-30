package com.youyu.backend.service.chat;

import java.util.Map;

public interface ChatService {
    Map<String, Object> getConversations(Long userId, int page, int size);
    Map<String, Object> findOrCreateConversation(Long currentUserId, Long peerUserId, Long productId, Long shopId);
    Map<String, Object> getMessages(Long conversationId, Long currentUserId, int page, int size);
    Map<String, Object> searchMessages(Long currentUserId, String keyword, String startTime, String endTime, int page, int size);
    Map<String, Object> sendMessage(Long conversationId, Long currentUserId, String body, String messageType, String mediaUrl, Long productId, Long orderId);
    Map<String, Object> getUnreadCount(Long currentUserId);
    void markConversationRead(Long conversationId, Long currentUserId);
    void updatePinStatus(Long conversationId, Long currentUserId, boolean pinned);
    void updateMuteStatus(Long conversationId, Long currentUserId, boolean muted);
    void deleteConversation(Long conversationId, Long currentUserId);
    void recallMessage(Long messageId, Long currentUserId);
    Map<String, Object> getAutoReplySettings(Long currentUserId);
    void updateAutoReplySettings(Long currentUserId, Boolean enabled, String replyContent);
    Map<String, Object> startSupportSession(Long currentUserId);
    void escalateSupportConversation(Long conversationId, Long currentUserId);
    Map<String, Object> closeSupportSession(Long conversationId, Long currentUserId);
}
