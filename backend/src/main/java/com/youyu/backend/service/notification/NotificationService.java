package com.youyu.backend.service.notification;

import java.util.Map;

public interface NotificationService {

    Map<String, Object> listNotifications(Long userId, int page, int size);

    Map<String, Object> getUnreadCount(Long userId);

    Map<String, Object> createNotification(Long userId, String type, String title, String body, String actionUrl);

    void createOrderStatusNotification(Map<String, Object> order, String statusText, boolean notifySeller);

    void markRead(Long userId, Long notificationId);

    void markAllRead(Long userId);
}
