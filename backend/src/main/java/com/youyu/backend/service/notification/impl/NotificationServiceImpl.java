package com.youyu.backend.service.notification.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.mapper.audit.AdminAuditLogMapper;
import com.youyu.backend.mapper.notification.NotificationMapper;
import com.youyu.backend.service.notification.NotificationService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_ACTION_URL_LENGTH = 512;

    private final NotificationMapper notificationMapper;
    private final AdminAuditLogMapper adminAuditLogMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper,
                                   AdminAuditLogMapper adminAuditLogMapper) {
        this.notificationMapper = notificationMapper;
        this.adminAuditLogMapper = adminAuditLogMapper;
    }

    @Override
    public Map<String, Object> listNotifications(Long userId, int page, int size) {
        requireUser(userId);
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int offset = safePage * safeSize;
        long total = notificationMapper.countByUserId(userId);
        long totalPages = total == 0 ? 0 : (total + safeSize - 1) / safeSize;
        return linkedMap(
                "content", notificationMapper.findByUserId(userId, offset, safeSize),
                "page", safePage,
                "size", safeSize,
                "total", total,
                "totalPages", totalPages
        );
    }

    @Override
    public Map<String, Object> getUnreadCount(Long userId) {
        requireUser(userId);
        return Map.of("count", notificationMapper.countUnreadByUserId(userId));
    }

    @Override
    @Transactional
    public Map<String, Object> createNotification(Long userId, String type, String title, String body, String actionUrl) {
        requireUser(userId);
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("userId", userId);
        notification.put("type", requireText(type, "type", 32));
        notification.put("title", requireText(title, "title", MAX_TITLE_LENGTH));
        notification.put("body", requireText(body, "body", 4000));
        notification.put("actionUrl", trimToLength(actionUrl, MAX_ACTION_URL_LENGTH));
        Long id = notificationMapper.insert(notification);
        return notificationMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "Notification creation failed"));
    }

    @Override
    @Transactional
    public Map<String, Object> publishSystemNotification(Long adminUserId, String title, String body, String actionUrl) {
        if (adminUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Please log in first");
        }
        String safeTitle = requireText(title, "title", MAX_TITLE_LENGTH);
        String safeBody = requireText(body, "body", 4000);
        String safeActionUrl = trimToLength(actionUrl, MAX_ACTION_URL_LENGTH);
        int recipientCount = notificationMapper.insertForActiveUsers("system", safeTitle, safeBody, safeActionUrl);
        adminAuditLogMapper.insert(
                adminUserId,
                "ADMIN",
                "SYSTEM_NOTIFICATION_PUBLISH",
                "NOTIFICATION",
                0L,
                "title=" + safeTitle + "; recipientCount=" + recipientCount
        );
        return linkedMap(
                "type", "system",
                "title", safeTitle,
                "body", safeBody,
                "actionUrl", safeActionUrl,
                "recipientCount", recipientCount
        );
    }

    @Override
    @Transactional
    public void createOrderStatusNotification(Map<String, Object> order, String statusText, boolean notifySeller) {
        if (order == null || order.get("id") == null) {
            return;
        }
        Long orderId = toLong(order.get("id"));
        Long buyerUserId = toLong(order.get("buyerUserId"));
        Long sellerUserId = toLong(order.get("sellerUserId"));
        String orderNo = String.valueOf(order.getOrDefault("orderNo", orderId));
        String actionUrl = "/app/orders/" + orderId;
        String title = "Order status updated";
        String buyerBody = "Order " + orderNo + " status changed to " + statusText + ".";
        createNotification(buyerUserId, "order_status", title, buyerBody, actionUrl);
        if (notifySeller && sellerUserId != null && !Objects.equals(sellerUserId, buyerUserId)) {
            String sellerBody = "Order " + orderNo + " status changed to " + statusText + ".";
            createNotification(sellerUserId, "order_status", title, sellerBody, actionUrl);
        }
    }

    @Override
    @Transactional
    public void createSupportTicketNotification(Long userId, Map<String, Object> ticket, String title, String body) {
        if (userId == null || ticket == null || ticket.get("id") == null) {
            return;
        }
        String actionUrl = "/app/support?ticketId=" + toLong(ticket.get("id"));
        createNotification(userId, "support_ticket", title, body, actionUrl);
    }

    @Override
    @Transactional
    public void createMediationNotification(Long userId, Map<String, Object> mediationCase, String title, String body) {
        if (userId == null || mediationCase == null || mediationCase.get("relatedOrderId") == null) {
            return;
        }
        String actionUrl = "/app/orders/" + toLong(mediationCase.get("relatedOrderId"));
        createNotification(userId, "mediation_update", title, body, actionUrl);
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        requireUser(userId);
        Map<String, Object> notification = notificationMapper.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Notification not found"));
        if (!Objects.equals(notification.get("userId"), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Cannot operate another user's notification");
        }
        notificationMapper.markRead(notificationId, userId);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        requireUser(userId);
        notificationMapper.markAllRead(userId);
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Please log in first");
        }
    }

    private String requireText(String value, String fieldName, int maxLength) {
        String text = value == null ? "" : value.trim();
        if (text.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, fieldName + " is required");
        }
        return trimToLength(text, maxLength);
    }

    private String trimToLength(String value, int maxLength) {
        String text = value == null ? "" : value.trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Map<String, Object> linkedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }
}
