package com.youyu.backend.controller;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.support.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> health(HttpServletRequest request) {
        return ApiResponse.success(
                Map.of(
                        "service", "youyu-backend",
                        "status", "UP",
                        "database", Map.of("status", databaseStatus())
                ),
                (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE)
        );
    }

    private String databaseStatus() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (RuntimeException ex) {
            return "DOWN";
        }
    }
}
