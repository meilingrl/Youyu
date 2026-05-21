package com.campusmarket.backend.controller.admin;

import com.campusmarket.backend.common.api.ApiResponse;
import com.campusmarket.backend.common.support.RequestContext;
import com.campusmarket.backend.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AuthService authService;

    public AdminAuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> payload,
                                                  HttpServletRequest request) {
        return ApiResponse.success(
                authService.unifiedLogin(
                        payload.getOrDefault("loginId", ""),
                        payload.getOrDefault("password", "")
                ),
                traceId(request)
        );
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }
}
