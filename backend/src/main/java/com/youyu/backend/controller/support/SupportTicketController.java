package com.youyu.backend.controller.support;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.common.auth.UserRole;
import com.youyu.backend.common.support.RequestContext;
import com.youyu.backend.service.support.SupportTicketService;
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
@RequestMapping("/api/support/tickets")
@LoginRequired(roles = {UserRole.USER})
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    public SupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> command,
                                                   HttpServletRequest request) {
        return ApiResponse.success(supportTicketService.createTicket(currentUserId(), command), traceId(request));
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "") String status,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int pageSize,
                                                 HttpServletRequest request) {
        return ApiResponse.success(
                supportTicketService.listUserTickets(currentUserId(), status, page, pageSize),
                traceId(request)
        );
    }

    @GetMapping("/{ticketId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long ticketId,
                                                   HttpServletRequest request) {
        return ApiResponse.success(supportTicketService.userTicketDetail(currentUserId(), ticketId), traceId(request));
    }

    @PostMapping("/{ticketId}/messages")
    public ApiResponse<Map<String, Object>> reply(@PathVariable Long ticketId,
                                                  @RequestBody Map<String, Object> command,
                                                  HttpServletRequest request) {
        return ApiResponse.success(
                supportTicketService.addUserMessage(currentUserId(), ticketId, command),
                traceId(request)
        );
    }

    private Long currentUserId() {
        return AuthContextHolder.get() == null ? null : AuthContextHolder.get().getUserId();
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }
}
