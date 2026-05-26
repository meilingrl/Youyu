package com.youyu.backend.mapper.chat;

import java.util.List;
import java.util.Map;

public interface ChatConversationMapper {
    Map<String, Object> findById(Long id);
    List<Map<String, Object>> findByUserId(Long userId, int offset, int limit);
    int countByUserId(Long userId);
    Map<String, Object> findByParticipants(Long userAId, Long userBId, Long productId, Long shopId);
    Long insert(Map<String, Object> conversation);
    int updateLastMessageAt(Long id, String lastMessageAt);
    int incrementUnreadCount(Long id, Long recipientUserId);
    int clearUnreadCount(Long id, Long userId);
    int sumUnreadCountByUserId(Long userId);
}
