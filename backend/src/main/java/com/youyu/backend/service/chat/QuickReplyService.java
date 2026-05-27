package com.youyu.backend.service.chat;

import java.util.List;
import java.util.Map;

public interface QuickReplyService {
    List<Map<String, Object>> listQuickReplies(Long userId);
    Map<String, Object> createQuickReply(Long userId, String content, Integer sortOrder);
    void updateQuickReply(Long userId, Long id, String content, Integer sortOrder);
    void deleteQuickReply(Long userId, Long id);
}
