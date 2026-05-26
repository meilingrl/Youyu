package com.youyu.backend.mapper.chat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;

public interface ChatMessageMapper {
    List<Map<String, Object>> findByConversationId(Long conversationId, int offset, int limit);
    int countByConversationId(Long conversationId);
    Long insert(Map<String, Object> message);
    Optional<Map<String, Object>> findProductCardSummary(Long productId);
    Optional<Map<String, Object>> findOrderCardSummary(Long orderId);
    int markMessagesRead(Long conversationId, Long readerUserId, LocalDateTime readAt);
}
