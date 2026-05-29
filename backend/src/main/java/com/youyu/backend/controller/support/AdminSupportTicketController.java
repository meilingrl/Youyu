package com.youyu.backend.controller.support;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.auth.AdminPermission;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.common.support.RequestContext;
import com.youyu.backend.service.support.SupportTicketService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/support/tickets")
@LoginRequired(permissions = {AdminPermission.ADMIN_SUPPORT_TICKETS_HANDLE})
public class AdminSupportTicketController {

    private final SupportTicketService supportTicketService;

    public AdminSupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "") String status,
                                                 @RequestParam(defaultValue = "") String category,
                                                 @RequestParam(defaultValue = "false") boolean assignedToMe,
                                                 @RequestParam(defaultValue = "") String keyword,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int pageSize,
                                                 HttpServletRequest request) {
        return ApiResponse.success(
                supportTicketService.listAdminTickets(
                        status,
                        category,
                        assignedToMe,
                        keyword,
                        page,
                        pageSize,
                        currentAdminUserId()
                ),
                traceId(request)
        );
    }

    @GetMapping("/{ticketId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long ticketId,
                                                   HttpServletRequest request) {
        return ApiResponse.success(supportTicketService.adminTicketDetail(ticketId), traceId(request));
    }

    @PutMapping("/{ticketId}/status")
    public ApiResponse<Map<String, Object>> updateStatus(@PathVariable Long ticketId,
                                                         @RequestBody Map<String, Object> command,
                                                         HttpServletRequest request) {
        return ApiResponse.success(
                supportTicketService.updateStatus(ticketId, command, currentAdminUserId()),
                traceId(request)
        );
    }

    @PostMapping("/{ticketId}/messages")
    public ApiResponse<Map<String, Object>> reply(@PathVariable Long ticketId,
                                                  @RequestBody Map<String, Object> command,
                                                  HttpServletRequest request) {
        return ApiResponse.success(
                supportTicketService.addAdminMessage(ticketId, command, currentAdminUserId()),
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
