package com.campusmarket.backend.admin;

import com.campusmarket.backend.BackendTestBase;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminGovernanceTest extends BackendTestBase {

    private static final String ADMIN = "mock-9001-ADMIN";
    private static final String USER = "mock-1001-USER";

    // ══════════════════════════════════════════════
    // Dashboard
    // ══════════════════════════════════════════════

    @Test
    void adminDashboardReturnsAggregatedMetrics() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    void adminDashboardDeniedForRegularUser() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminDashboardDeniedForUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // ══════════════════════════════════════════════
    // User management
    // ══════════════════════════════════════════════

    @Test
    void listUsersReturnsResults() throws Exception {
        String response = mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()", greaterThan(0)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Object> items = JsonPath.read(response, "$.data.items");
        assertFalse(items.isEmpty());
    }

    @Test
    void listUsersFilterByKeyword() throws Exception {
        String response = mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + ADMIN)
                        .param("keyword", "zhang"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Object> items = JsonPath.read(response, "$.data.items");
        boolean hasMatch = items.stream().anyMatch(item -> {
            String username = JsonPath.read(item, "$.username");
            return username != null && username.toLowerCase().contains("zhang");
        });
        assertFalse(!hasMatch, "Expected user matching keyword 'zhang'");
    }

    @Test
    void listUsersFilterByStatus() throws Exception {
        String response = mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + ADMIN)
                        .param("status", "active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Object> items = JsonPath.read(response, "$.data.items");
        for (Object item : items) {
            String status = JsonPath.read(item, "$.status");
            org.junit.jupiter.api.Assertions.assertEquals("active", status);
        }
    }

    @Test
    void getUserDetailReturnsFullProfile() throws Exception {
        mockMvc.perform(get("/api/admin/users/1001")
                        .header("Authorization", "Bearer " + ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.username").value("zhangsan"))
                .andExpect(jsonPath("$.data.user.nickname").exists())
                .andExpect(jsonPath("$.data.verifications").isArray());
    }

    @Test
    void updateUserStatusToDisabledAndBack() throws Exception {
        mockMvc.perform(put("/api/admin/users/1002/status")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "disabled",
                                  "restrictionReason": "test restriction"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.status").value("disabled"));

        mockMvc.perform(put("/api/admin/users/1002/status")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "active",
                                  "restrictionReason": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.status").value("active"));
    }

    @Test
    void updateUserStatusRequiresAdminRole() throws Exception {
        mockMvc.perform(put("/api/admin/users/1001/status")
                        .header("Authorization", "Bearer " + USER)
                        .contentType("application/json")
                        .content("{\"status\": \"disabled\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // ══════════════════════════════════════════════
    // Verification review
    // ══════════════════════════════════════════════

    @Test
    void listVerificationsReturnsResults() throws Exception {
        mockMvc.perform(get("/api/admin/verifications")
                        .header("Authorization", "Bearer " + ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()", greaterThan(0)));
    }

    @Test
    void listVerificationsFilterByPendingStatus() throws Exception {
        String response = mockMvc.perform(get("/api/admin/verifications")
                        .header("Authorization", "Bearer " + ADMIN)
                        .param("status", "pending_review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Object> items = JsonPath.read(response, "$.data.items");
        for (Object item : items) {
            String status = JsonPath.read(item, "$.verificationStatus");
            org.junit.jupiter.api.Assertions.assertEquals("pending_review", status);
        }
    }

    @Test
    void reviewVerificationApprove() throws Exception {
        mockMvc.perform(put("/api/admin/verifications/2002/review")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType("application/json")
                        .content("""
                                {
                                  "action": "approve",
                                  "reviewNote": "test approve"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verification.verificationStatus").value("approved"));
    }

    @Test
    void reviewVerificationReject() throws Exception {
        mockMvc.perform(put("/api/admin/verifications/2005/review")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType("application/json")
                        .content("""
                                {
                                  "action": "reject",
                                  "rejectReason": "invalid document",
                                  "reviewNote": "test reject"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verification.verificationStatus").value("rejected"))
                .andExpect(jsonPath("$.data.verification.rejectReason").value("invalid document"));
    }

    // ══════════════════════════════════════════════
    // Product management
    // ══════════════════════════════════════════════

    @Test
    void listAdminProductsReturnsResults() throws Exception {
        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", "Bearer " + ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()", greaterThan(0)));
    }

    @Test
    void listAdminProductsFilterByStatus() throws Exception {
        String response = mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", "Bearer " + ADMIN)
                        .param("status", "on_sale"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Object> items = JsonPath.read(response, "$.data.items");
        for (Object item : items) {
            String status = JsonPath.read(item, "$.status");
            org.junit.jupiter.api.Assertions.assertEquals("on_sale", status);
        }
    }

    @Test
    void updateProductStatusToOffSale() throws Exception {
        mockMvc.perform(put("/api/admin/products/3010/status")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType("application/json")
                        .content("{\"status\": \"off_sale\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.product.status").value("off_sale"));
    }

    // ══════════════════════════════════════════════
    // Shop management
    // ══════════════════════════════════════════════

    @Test
    void listAdminShopsReturnsResults() throws Exception {
        mockMvc.perform(get("/api/admin/shops")
                        .header("Authorization", "Bearer " + ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()", greaterThan(0)));
    }

    @Test
    void getAdminShopDetail() throws Exception {
        mockMvc.perform(get("/api/admin/shops/4001")
                        .header("Authorization", "Bearer " + ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shop.name").value("Course Notes Shop"))
                .andExpect(jsonPath("$.data.shop.status").value("active"));
    }

    @Test
    void updateShopStatusApprovePendingShop() throws Exception {
        mockMvc.perform(put("/api/admin/shops/4003/status")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "active",
                                  "reviewStatus": "approved"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shop.status").value("active"))
                .andExpect(jsonPath("$.data.shop.reviewStatus").value("approved"));
    }

    // ══════════════════════════════════════════════
    // Search governance (admin CRUD)
    // ══════════════════════════════════════════════

    @Test
    void searchGovernanceRulesAdminCRUD() throws Exception {
        String createResponse = mockMvc.perform(post("/api/admin/search/governance-rules")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType("application/json")
                        .content("""
                                {
                                  "ruleType": "SENSITIVE_WORD",
                                  "keyword": "test_spam"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ruleType").value("SENSITIVE_WORD"))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number ruleId = JsonPath.read(createResponse, "$.data.id");

        mockMvc.perform(get("/api/admin/search/governance-rules")
                        .header("Authorization", "Bearer " + ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(put("/api/admin/search/governance-rules/{ruleId}", ruleId)
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType("application/json")
                        .content("{\"isActive\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false));

        deleteSearchGovernanceRule(ADMIN, ruleId);
    }

    // ══════════════════════════════════════════════
    // Cross-cutting auth checks
    // ══════════════════════════════════════════════

    @Test
    void adminEndpointsRejectUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/verifications"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/shops"))
                .andExpect(status().isUnauthorized());
    }
}
