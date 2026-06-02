package com.youyu.backend.controller.notification;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.auth.AdminPermission;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.common.auth.UserRole;
import com.youyu.backend.common.support.RequestContext;
import com.youyu.backend.service.notification.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notifications")
@LoginRequired(roles = {UserRole.ADMIN}, permissions = {AdminPermission.ADMIN_NOTIFICATIONS_PUBLISH})
public class AdminNotificationController {

    private final NotificationService notificationService;

    public AdminNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> publish(@RequestBody Map<String, String> payload,
                                                    HttpServletRequest request) {
        return ApiResponse.success(
                notificationService.publishSystemNotification(
                        currentAdminUserId(),
                        payload.getOrDefault("title", ""),
                        payload.getOrDefault("body", ""),
                        payload.getOrDefault("actionUrl", "")
                ),
                traceId(request)
        );
    }

    private Long currentAdminUserId() {
        return AuthContextHolder.get() == null ? null : AuthContextHolder.get().getUserId();
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }
}
