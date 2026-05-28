package com.youyu.backend.admin;

import com.jayway.jsonpath.JsonPath;
import com.youyu.backend.BackendTestBase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAuditLogTest extends BackendTestBase {

    private static final String ADMIN = "mock-9001-ADMIN";
    private static final String USER = "mock-1001-USER";

    @Test
    void updateUserStatusWritesAuditLog() throws Exception {
        long before = countAuditLogs("USER_STATUS_UPDATE", "USER", 1004L);

        mockMvc.perform(put("/api/admin/users/1004/status")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "disabled",
                                  "restrictionReason": "audit restriction"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals(before + 1, countAuditLogs("USER_STATUS_UPDATE", "USER", 1004L));

        String response = mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", "Bearer " + ADMIN)
                        .param("action", "USER_STATUS_UPDATE")
                        .param("targetType", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, Object>> items = JsonPath.read(response, "$.data.items");
        assertTrue(items.stream().anyMatch(item ->
                numberValue(item.get("operatorUserId")) == 9001L
                        && "ADMIN".equals(item.get("operatorRole"))
                        && "USER_STATUS_UPDATE".equals(item.get("action"))
                        && "USER".equals(item.get("targetType"))
                        && numberValue(item.get("targetId")) == 1004L
                        && String.valueOf(item.get("summary")).contains("status=disabled")
                        && String.valueOf(item.get("summary")).contains("reason=audit restriction")
                        && !String.valueOf(item.get("createdAt")).isBlank()
        ));
    }

    @Test
    void updateProductStatusWritesAuditLog() throws Exception {
        long before = countAuditLogs("PRODUCT_STATUS_UPDATE", "PRODUCT", 3011L);

        mockMvc.perform(put("/api/admin/products/3011/status")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"off_sale\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals(before + 1, countAuditLogs("PRODUCT_STATUS_UPDATE", "PRODUCT", 3011L));

        String response = mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", "Bearer " + ADMIN)
                        .param("action", "PRODUCT_STATUS_UPDATE")
                        .param("targetType", "PRODUCT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, Object>> items = JsonPath.read(response, "$.data.items");
        assertTrue(items.stream().anyMatch(item ->
                numberValue(item.get("operatorUserId")) == 9001L
                        && "PRODUCT_STATUS_UPDATE".equals(item.get("action"))
                        && "PRODUCT".equals(item.get("targetType"))
                        && numberValue(item.get("targetId")) == 3011L
                        && String.valueOf(item.get("summary")).contains("status=off_sale")
        ));
    }

    @Test
    void auditLogReadRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private long countAuditLogs(String action, String targetType, Long targetId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM admin_audit_logs
                WHERE action = ? AND target_type = ? AND target_id = ?
                """, Long.class, action, targetType, targetId);
        return count == null ? 0L : count;
    }

    private long numberValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
