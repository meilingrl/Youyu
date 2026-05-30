package com.youyu.backend.mapper.chat;

import java.util.List;
import java.util.Map;

public interface ChatConversationMapper {
    Map<String, Object> findById(Long id);
    List<Map<String, Object>> findByUserId(Long userId, int offset, int limit);
    int countByUserId(Long userId);
    Map<String, Object> findByParticipants(Long userAId, Long userBId, Long productId, Long shopId);
    Map<String, Object> findSupportByRequesterAndCs(Long requesterUserId, Long csUserId);
    Long insert(Map<String, Object> conversation);
    int updateType(Long id, String type);
    int updateLastMessageAt(Long id, String lastMessageAt);
    int incrementUnreadCount(Long id, Long recipientUserId);
    int clearUnreadCount(Long id, Long userId);
    int sumUnreadCountByUserId(Long userId);
    int updatePinStatus(Long id, Long userId, boolean pinned);
    int updateMuteStatus(Long id, Long userId, boolean muted);
    int softDelete(Long id, Long userId);
    int restoreForUser(Long id, Long userId);
    int updateAutoRepliedAt(Long id, Long targetUserId, java.time.LocalDateTime repliedAt);
    Long findPlatformCsUserId();
    int updateSupportStatus(Long id, String supportStatus);
    int clearSupportAssignment(Long id);
    int assignSupportAgent(Long id, Long adminId);
    void resetSchemaAvailabilityCache();
}
