package com.youyu.backend.controller.notification;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.common.auth.UserRole;
import com.youyu.backend.common.support.RequestContext;
import com.youyu.backend.service.notification.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@LoginRequired(roles = {UserRole.USER})
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        return ApiResponse.success(notificationService.listNotifications(currentUserId(), page, size), traceId(request));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Object>> unreadCount(HttpServletRequest request) {
        return ApiResponse.success(notificationService.getUnreadCount(currentUserId()), traceId(request));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id, HttpServletRequest request) {
        notificationService.markRead(currentUserId(), id);
        return ApiResponse.success(null, traceId(request));
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllRead(HttpServletRequest request) {
        notificationService.markAllRead(currentUserId());
        return ApiResponse.success(null, traceId(request));
    }

    private Long currentUserId() {
        return AuthContextHolder.get() == null ? null : AuthContextHolder.get().getUserId();
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }
}
