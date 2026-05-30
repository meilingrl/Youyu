package com.youyu.backend.service.chat;

import java.util.Map;

public interface SupportConsoleService {
    Map<String, Object> listQueue(String filter, Long adminId, int page, int size);
    Map<String, Object> getConversation(Long conversationId, Long adminId);
    Map<String, Object> getMessages(Long conversationId, Long adminId, int page, int size);
    Map<String, Object> claim(Long conversationId, Long adminId);
    Map<String, Object> reply(Long conversationId, Long adminId, String body);
    Map<String, Object> close(Long conversationId, Long adminId);
    void markRead(Long conversationId, Long adminId);
}
