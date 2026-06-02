package com.youyu.backend.service.notification;

import java.util.Map;

public interface NotificationService {

    Map<String, Object> listNotifications(Long userId, int page, int size);

    Map<String, Object> getUnreadCount(Long userId);

    Map<String, Object> createNotification(Long userId, String type, String title, String body, String actionUrl);

    Map<String, Object> publishSystemNotification(Long adminUserId, String title, String body, String actionUrl);

    void createOrderStatusNotification(Map<String, Object> order, String statusText, boolean notifySeller);

    void createSupportTicketNotification(Long userId, Map<String, Object> ticket, String title, String body);

    void createMediationNotification(Long userId, Map<String, Object> mediationCase, String title, String body);

    void markRead(Long userId, Long notificationId);

    void markAllRead(Long userId);
}
