package com.youyu.backend.mapper.chat;

import java.util.Map;
import java.util.Optional;

public interface AutoReplySettingsMapper {
    Optional<Map<String, Object>> findByUserId(Long userId);
    void upsert(Long userId, boolean enabled, String replyContent);
}
