package com.youyu.backend.notification;

import com.youyu.backend.BackendTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminNotificationPublishTest extends BackendTestBase {

    private static final String ADMIN = "mock-9001-ADMIN";
    private static final String SUPPORT_AGENT = "mock-9102-SUPPORT_AGENT";
    private static final String TITLE = "TST-system-notification";

    @BeforeEach
    void resetFixtures() {
        jdbcTemplate.update("DELETE FROM notifications WHERE title = ?", TITLE);
        jdbcTemplate.update("DELETE FROM admin_audit_logs WHERE action = 'SYSTEM_NOTIFICATION_PUBLISH'");
    }

    @Test
    void adminCanPublishSystemNotificationToActiveUsers() throws Exception {
        mockMvc.perform(post("/api/admin/notifications")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "body": "Platform maintenance starts at 23:00.",
                                  "actionUrl": "/app/home"
                                }
                                """.formatted(TITLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("system"))
                .andExpect(jsonPath("$.data.actionUrl").value("/app/home"))
                .andExpect(jsonPath("$.data.recipientCount", greaterThan(0)));

        Integer notificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE title = ? AND type = 'system'",
                Integer.class,
                TITLE
        );
        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_logs WHERE action = 'SYSTEM_NOTIFICATION_PUBLISH'",
                Integer.class
        );
        org.junit.jupiter.api.Assertions.assertTrue(notificationCount != null && notificationCount > 0);
        org.junit.jupiter.api.Assertions.assertEquals(1, auditCount);
    }

    @Test
    void specialistAdminCannotPublishSystemNotification() throws Exception {
        mockMvc.perform(post("/api/admin/notifications")
                        .header("Authorization", "Bearer " + SUPPORT_AGENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "body": "This should not be published."
                                }
                                """.formatted(TITLE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void titleAndBodyAreRequired() throws Exception {
        mockMvc.perform(post("/api/admin/notifications")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"body\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
