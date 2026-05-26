package com.youyu.backend.mapper.chat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;

public interface ChatMessageMapper {
    List<Map<String, Object>> findByConversationId(Long conversationId, int offset, int limit);
    int countByConversationId(Long conversationId);
    Optional<Map<String, Object>> findById(Long messageId);
    List<Map<String, Object>> searchByUser(Long userId, String keyword, LocalDateTime startTime, LocalDateTime endTime, int offset, int limit);
    int countSearchByUser(Long userId, String keyword, LocalDateTime startTime, LocalDateTime endTime);
    Long insert(Map<String, Object> message);
    int recall(Long messageId, LocalDateTime recalledAt);
    Optional<Map<String, Object>> findProductCardSummary(Long productId);
    Optional<Map<String, Object>> findOrderCardSummary(Long orderId);
    int markMessagesRead(Long conversationId, Long readerUserId, LocalDateTime readAt);
}
