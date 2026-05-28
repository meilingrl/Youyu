package com.youyu.backend.admin;

import com.youyu.backend.BackendTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminRolePermissionTest extends BackendTestBase {

    private static final String LEGACY_ADMIN = "mock-9001-ADMIN";
    private static final String SUPER_ADMIN = "mock-9101-SUPER_ADMIN";
    private static final String SUPPORT_AGENT = "mock-9102-SUPPORT_AGENT";
    private static final String REVIEWER = "mock-9103-REVIEWER";
    private static final String OPERATOR = "mock-9104-OPERATOR";
    private static final String ORDER_ADMIN = "mock-9105-ORDER_ADMIN";
    private static final String USER = "mock-1001-USER";

    @Test
    void legacyAdminAndSuperAdminKeepFullAccess() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", "Bearer " + LEGACY_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", "Bearer " + SUPER_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void reviewerCanReviewButCannotManageOrdersOrAuditLogs() throws Exception {
        mockMvc.perform(get("/api/admin/verifications")
                        .header("Authorization", "Bearer " + REVIEWER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + REVIEWER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", "Bearer " + REVIEWER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void operatorCanGovernSearchButCannotReviewVerification() throws Exception {
        mockMvc.perform(get("/api/admin/search/governance-rules")
                        .header("Authorization", "Bearer " + OPERATOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(put("/api/admin/verifications/2002/review")
                        .header("Authorization", "Bearer " + OPERATOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"approve\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void supportCanHandleMediationButCannotRecordFinalDecision() throws Exception {
        mockMvc.perform(get("/api/admin/mediation-cases")
                        .header("Authorization", "Bearer " + SUPPORT_AGENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/admin/mediation-cases/70001/decision")
                        .header("Authorization", "Bearer " + SUPPORT_AGENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decisionCategory": "order_completion_required",
                                  "decisionSummary": "restricted"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void orderAdminCanReadOrdersButCannotManageUsers() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + ORDER_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(put("/api/admin/users/1002/status")
                        .header("Authorization", "Bearer " + ORDER_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"disabled\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void regularUserStillCannotAccessAdminCapabilities() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void seedSpecialistAccountCanLoginWithRole() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "reviewer",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("reviewer"))
                .andExpect(jsonPath("$.data.token").isString());
    }
}
