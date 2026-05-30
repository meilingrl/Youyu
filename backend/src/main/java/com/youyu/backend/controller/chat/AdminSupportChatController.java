package com.youyu.backend.controller.chat;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.auth.AdminPermission;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.common.support.RequestContext;
import com.youyu.backend.service.chat.SupportConsoleService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/support/chat")
@LoginRequired(permissions = {AdminPermission.ADMIN_SUPPORT_TICKETS_HANDLE})
public class AdminSupportChatController {

    private final SupportConsoleService supportConsoleService;

    public AdminSupportChatController(SupportConsoleService supportConsoleService) {
        this.supportConsoleService = supportConsoleService;
    }

    @GetMapping("/conversations")
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "pending") String filter,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  HttpServletRequest request) {
        return ApiResponse.success(supportConsoleService.listQueue(filter, currentAdminId(), page, size), traceId(request));
    }

    @GetMapping("/conversations/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(supportConsoleService.getConversation(id, currentAdminId()), traceId(request));
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<Map<String, Object>> messages(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "50") int size,
                                                      HttpServletRequest request) {
        return ApiResponse.success(supportConsoleService.getMessages(id, currentAdminId(), page, size), traceId(request));
    }

    @PostMapping("/conversations/{id}/claim")
    public ApiResponse<Map<String, Object>> claim(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(supportConsoleService.claim(id, currentAdminId()), traceId(request));
    }

    @PostMapping("/conversations/{id}/messages")
    public ApiResponse<Map<String, Object>> reply(@PathVariable Long id,
                                                   @RequestBody SupportReplyRequest req,
                                                   HttpServletRequest request) {
        return ApiResponse.success(supportConsoleService.reply(id, currentAdminId(), req.getBody()), traceId(request));
    }

    @PostMapping("/conversations/{id}/close")
    public ApiResponse<Map<String, Object>> close(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(supportConsoleService.close(id, currentAdminId()), traceId(request));
    }

    @PostMapping("/conversations/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id, HttpServletRequest request) {
        supportConsoleService.markRead(id, currentAdminId());
        return ApiResponse.success(null, traceId(request));
    }

    private Long currentAdminId() {
        return AuthContextHolder.get() == null ? null : AuthContextHolder.get().getUserId();
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }

    public static class SupportReplyRequest {
        private String body;

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }
}
