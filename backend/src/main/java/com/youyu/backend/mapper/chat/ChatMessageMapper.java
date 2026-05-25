package com.youyu.backend.mapper.chat;

import java.util.List;
import java.util.Map;

public interface ChatMessageMapper {
    List<Map<String, Object>> findByConversationId(Long conversationId, int offset, int limit);
    int countByConversationId(Long conversationId);
    Long insert(Map<String, Object> message);
}
