package com.youyu.backend.mapper.notification;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface NotificationMapper {

    Long insert(Map<String, Object> notification);

    int insertForActiveUsers(String type, String title, String body, String actionUrl);

    List<Map<String, Object>> findByUserId(Long userId, int offset, int limit);

    long countByUserId(Long userId);

    long countUnreadByUserId(Long userId);

    Optional<Map<String, Object>> findById(Long id);

    boolean markRead(Long id, Long userId);

    int markAllRead(Long userId);
}
