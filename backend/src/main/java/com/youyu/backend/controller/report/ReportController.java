package com.youyu.backend.controller.report;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.common.auth.UserRole;
import com.youyu.backend.common.support.RequestContext;
import com.youyu.backend.service.report.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@LoginRequired
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/skeleton")
    public ApiResponse<Map<String, Object>> skeleton(HttpServletRequest request) {
        return ApiResponse.success(
                reportService.moduleInfo(),
                (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE)
        );
    }

    @PostMapping
    @LoginRequired(roles = {UserRole.USER})
    public ApiResponse<Map<String, Object>> submit(@RequestBody Map<String, Object> command,
                                                   HttpServletRequest request) {
        return ApiResponse.success(
                reportService.submitReport(currentUserId(), command),
                (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE)
        );
    }

    private Long currentUserId() {
        return AuthContextHolder.get() == null ? null : AuthContextHolder.get().getUserId();
    }
}
