package com.youyu.backend.order;

import com.youyu.backend.BackendTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderAfterSalesDetailTest extends BackendTestBase {

    private static final String BUYER = "mock-1010-USER";
    private static final String ADMIN = "mock-9001-ADMIN";

    @BeforeEach
    void resetFixtures() {
        jdbcTemplate.update("DELETE FROM mediation_cases WHERE source_report_id BETWEEN 62001 AND 62009");
        jdbcTemplate.update("DELETE FROM reports WHERE id BETWEEN 62001 AND 62009");
        jdbcTemplate.update("DELETE FROM notifications WHERE user_id IN (1010, 1004)");
    }

    @Test
    void orderDetailReturnsVisibleAfterSalesContextForUserAndAdmin() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO reports (
                    id, reporter_user_id, reporter_name, target_type, target_id, target_label,
                    reason_type, content, status, submitted_at, resolution, created_at, updated_at
                ) VALUES
                (62001, 1010, 'Seed Buyer', 'order', 8001, 'Order SEED8001',
                 'delivery_dispute', 'Buyer submitted an after-sales report.', 'pending',
                 CURRENT_TIMESTAMP, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                (62002, 1002, 'Li Si', 'order', 8001, 'Order SEED8001',
                 'other_violation', 'Another user report should stay admin-only.', 'pending',
                 CURRENT_TIMESTAMP, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        mockMvc.perform(post("/api/admin/reports/62001/escalate-to-mediation")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"escalationReason\":\"needs formal review\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.case.relatedOrderId").value(8001));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + BUYER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("mediation_update"))
                .andExpect(jsonPath("$.data.content[0].actionUrl").value("/app/orders/8001"));

        mockMvc.perform(get("/api/orders/8001")
                        .header("Authorization", "Bearer " + BUYER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.relatedReports[*].id", hasItem(62001)))
                .andExpect(jsonPath("$.data.relatedReports[*].id", not(hasItem(62002))))
                .andExpect(jsonPath("$.data.mediationSummary.caseNo").exists())
                .andExpect(jsonPath("$.data.afterSalesSummary.currentStage").value("mediation_in_progress"));

        mockMvc.perform(get("/api/admin/orders/8001")
                        .header("Authorization", "Bearer " + ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.relatedReports.length()", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.mediationSummary.sourceReportId").value(62001));
    }
}
