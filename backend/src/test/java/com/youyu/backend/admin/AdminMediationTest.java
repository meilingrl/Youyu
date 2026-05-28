package com.youyu.backend.admin;

import com.jayway.jsonpath.JsonPath;
import com.youyu.backend.BackendTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminMediationTest extends BackendTestBase {

    private static final String ADMIN = "mock-9001-ADMIN";
    private static final String USER = "mock-1001-USER";

    @BeforeEach
    void resetMediationFixtures() {
        jdbcTemplate.update("DELETE FROM mediation_cases WHERE source_report_id BETWEEN 61000 AND 61999");
        jdbcTemplate.update("DELETE FROM reports WHERE id BETWEEN 61000 AND 61999");
        jdbcTemplate.update("DELETE FROM chat_messages WHERE id BETWEEN 16000 AND 16999");
        jdbcTemplate.update("DELETE FROM chat_conversations WHERE id BETWEEN 16000 AND 16999");

        jdbcTemplate.update("""
                INSERT INTO reports (
                    id, reporter_user_id, reporter_name, target_type, target_id, target_label,
                    reason_type, content, status, submitted_at, resolution, created_at, updated_at
                ) VALUES
                (61001, 1010, 'Seed Buyer', 'order', 8001, 'Order SEED8001',
                 'delivery_dispute', 'Order delivery evidence is unclear.', 'pending',
                 CURRENT_TIMESTAMP, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                (61002, 1001, 'Zhang San', 'product', 3001, 'Advanced Math Review Pack',
                 'content_issue', 'This is not an order-backed report.', 'pending',
                 CURRENT_TIMESTAMP, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                (61003, 1011, 'Seed Buyer Two', 'order', 8003, 'Order SEED8003',
                 'offline_handoff', 'Offline handoff needs platform decision.', 'pending',
                 CURRENT_TIMESTAMP, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                (61004, 1012, 'Seed Buyer Three', 'order', 999999, 'Missing Order',
                 'missing_order', 'Order id should not resolve.', 'pending',
                 CURRENT_TIMESTAMP, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        jdbcTemplate.update("""
                INSERT INTO chat_conversations (
                    id, type, product_id, shop_id, user_a_id, user_b_id,
                    unread_count_a, unread_count_b, last_message_at, created_at
                ) VALUES (16001, 'direct', NULL, NULL, 1010, 1004, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO chat_messages (
                    id, conversation_id, sender_user_id, body, is_read, message_type, order_id, created_at
                ) VALUES (16001, 16001, 1010, 'Please review order SEED8001 before shipment.', FALSE, 'order_card', 8001, CURRENT_TIMESTAMP)
                """);
    }

    @Test
    void mediationEndpointsRequireAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/mediation-cases")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/admin/mediation-cases"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void eligibleReportEscalationCreatesCaseAndIsIdempotent() throws Exception {
        String firstResponse = mockMvc.perform(post("/api/admin/reports/61001/escalate-to-mediation")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"escalationReason\":\"needs formal dispute review\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.created").value(true))
                .andExpect(jsonPath("$.data.case.sourceReportId").value(61001))
                .andExpect(jsonPath("$.data.case.relatedOrderId").value(8001))
                .andExpect(jsonPath("$.data.case.status").value("opened"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number firstCaseId = JsonPath.read(firstResponse, "$.data.case.id");
        assertEquals("processing", reportStatus(61001));

        String secondResponse = mockMvc.perform(post("/api/admin/reports/61001/escalate-to-mediation")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.created").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number secondCaseId = JsonPath.read(secondResponse, "$.data.case.id");
        assertEquals(firstCaseId.longValue(), secondCaseId.longValue());
        assertEquals(1L, countCasesForReport(61001));
    }

    @Test
    void escalationRejectsNonOrderAndMissingOrderReports() throws Exception {
        mockMvc.perform(post("/api/admin/reports/61002/escalate-to-mediation")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(post("/api/admin/reports/61004/escalate-to-mediation")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void listAndDetailReturnReportOrderRefundParticipantAndReadOnlyChatContext() throws Exception {
        Number caseId = escalate(61001);

        mockMvc.perform(get("/api/admin/mediation-cases")
                        .header("Authorization", "Bearer " + ADMIN)
                        .param("status", "opened")
                        .param("reportId", "61001")
                        .param("keyword", "SEED8001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.items[0].sourceReport.id").value(61001))
                .andExpect(jsonPath("$.data.items[0].orderSummary.id").value(8001));

        Number unreadBefore = unreadMessageFlag(16001);
        mockMvc.perform(get("/api/admin/mediation-cases/{caseId}", caseId)
                        .header("Authorization", "Bearer " + ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.case.id").value(caseId))
                .andExpect(jsonPath("$.data.sourceReport.id").value(61001))
                .andExpect(jsonPath("$.data.order.id").value(8001))
                .andExpect(jsonPath("$.data.orderItems").isArray())
                .andExpect(jsonPath("$.data.refunds").isArray())
                .andExpect(jsonPath("$.data.participants.buyer.id").value(1010))
                .andExpect(jsonPath("$.data.participants.seller.id").value(1004))
                .andExpect(jsonPath("$.data.chatContext.scope").value("order_id"))
                .andExpect(jsonPath("$.data.chatContext.items[0].orderId").value(8001))
                .andExpect(jsonPath("$.data.chatContext.items[0].body").value("Please review order SEED8001 before shipment."));

        assertEquals(unreadBefore.intValue(), unreadMessageFlag(16001).intValue());
    }

    @Test
    void statusTransitionsRejectInvalidMovesAndTerminalReopen() throws Exception {
        Number caseId = escalate(61001);

        mockMvc.perform(put("/api/admin/mediation-cases/{caseId}/status", caseId)
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"decision_pending\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(put("/api/admin/mediation-cases/{caseId}/status", caseId)
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"evidence_review\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.case.status").value("evidence_review"));

        mockMvc.perform(put("/api/admin/mediation-cases/{caseId}/status", caseId)
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"cancelled\",\"cancelReason\":\"duplicate report\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.case.status").value("cancelled"));

        mockMvc.perform(put("/api/admin/mediation-cases/{caseId}/status", caseId)
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"evidence_review\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void finalDecisionIsWriteOnceAndResolvesSourceReport() throws Exception {
        Number caseId = escalate(61003);
        mockMvc.perform(put("/api/admin/mediation-cases/{caseId}/status", caseId)
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"evidence_review\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/mediation-cases/{caseId}/status", caseId)
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"decision_pending\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/mediation-cases/{caseId}/decision", caseId)
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decisionCategory": "order_completion_required",
                                  "decisionSummary": "Evidence is sufficient for completion.",
                                  "enforcementSummary": "Order module keeps operational ownership."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.case.status").value("resolved"))
                .andExpect(jsonPath("$.data.case.decisionCategory").value("order_completion_required"))
                .andExpect(jsonPath("$.data.case.decidedByAdminUserId").value(9001));

        assertEquals("resolved", reportStatus(61003));
        assertTrue(reportResolution(61003).contains("order_completion_required"));

        mockMvc.perform(post("/api/admin/mediation-cases/{caseId}/decision", caseId)
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decisionCategory": "no_action_invalid_or_duplicate",
                                  "decisionSummary": "Try overwrite."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    private Number escalate(long reportId) throws Exception {
        String response = mockMvc.perform(post("/api/admin/reports/{reportId}/escalate-to-mediation", reportId)
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.data.case.id");
    }

    private long countCasesForReport(long reportId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mediation_cases WHERE source_report_id = ?",
                Long.class,
                reportId
        );
        return count == null ? 0L : count;
    }

    private String reportStatus(long reportId) {
        return jdbcTemplate.queryForObject("SELECT status FROM reports WHERE id = ?", String.class, reportId);
    }

    private String reportResolution(long reportId) {
        return jdbcTemplate.queryForObject("SELECT resolution FROM reports WHERE id = ?", String.class, reportId);
    }

    private Number unreadMessageFlag(long messageId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chat_messages WHERE id = ? AND is_read = FALSE", Number.class, messageId);
    }
}
