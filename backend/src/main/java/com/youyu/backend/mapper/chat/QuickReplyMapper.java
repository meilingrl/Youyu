package com.youyu.backend.mapper.chat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface QuickReplyMapper {
    List<Map<String, Object>> findByUserId(Long userId);
    Optional<Map<String, Object>> findById(Long id);
    Long insert(Map<String, Object> quickReply);
    boolean update(Long id, Long userId, String content, int sortOrder);
    boolean delete(Long id, Long userId);
}
