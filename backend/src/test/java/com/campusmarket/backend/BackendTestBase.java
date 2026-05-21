package com.campusmarket.backend;

import com.jayway.jsonpath.JsonPath;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(classes = CampusMarketBackendApplication.class)
public abstract class BackendTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    // ── Flow helpers ──

    protected Number addToCart(String token, long productId, int quantity) throws Exception {
        String response = mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": %d, \"quantity\": %d}".formatted(productId, quantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.data.items[0].id");
    }

    protected String createOrderAndReturnJson(String token, Number cartItemId, String fulfillmentType,
                                               String extraJson) throws Exception {
        StringBuilder json = new StringBuilder();
        json.append("{\"cartItemIds\": [%s], \"fulfillmentType\": \"%s\""
                .formatted(cartItemId, fulfillmentType));
        if (extraJson != null && !extraJson.isBlank()) {
            json.append(", ").append(extraJson);
        }
        json.append("}");
        return mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    protected Number createOrder(String token, Number cartItemId, String fulfillmentType,
                                  String extraJson) throws Exception {
        String response = createOrderAndReturnJson(token, cartItemId, fulfillmentType, extraJson);
        return JsonPath.read(response, "$.data.id");
    }

    protected String initiatePayment(String token, Number orderId) throws Exception {
        return mockMvc.perform(post("/api/payments/orders/%s/initiate".formatted(orderId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.payment.paymentStatus").value("initiated"))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    protected String initiateAndPay(String token, Number orderId) throws Exception {
        String paymentResponse = initiatePayment(token, orderId);
        String paymentNo = JsonPath.read(paymentResponse, "$.data.payment.paymentNo");
        mockMvc.perform(post("/api/payments/%s/mock-success".formatted(paymentNo))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        return paymentNo;
    }

    // ── Search governance helpers ──

    protected long countSearchLogs() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM search_logs", Long.class);
        return count == null ? 0L : count;
    }

    protected void insertSearchLog(String keyword, String normalizedKeyword, int resultCount,
                                    LocalDateTime createdAt) {
        jdbcTemplate.update("""
                INSERT INTO search_logs (keyword, normalized_keyword, user_id, result_count, created_at)
                VALUES (?, ?, NULL, ?, ?)
                """, keyword, normalizedKeyword, resultCount, Timestamp.valueOf(createdAt));
    }

    protected Number createSearchGovernanceRule(String adminToken, String ruleType,
                                                 String keyword) throws Exception {
        String createResponse = mockMvc.perform(post("/api/admin/search/governance-rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleType": "%s",
                                  "keyword": "%s"
                                }
                                """.formatted(ruleType, keyword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(createResponse, "$.data.id");
    }

    protected void deleteSearchGovernanceRule(String adminToken, Number ruleId) throws Exception {
        mockMvc.perform(delete("/api/admin/search/governance-rules/{ruleId}", ruleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── Review helpers ──

    protected Number firstReviewTaskId(String adminToken, Number productId) throws Exception {
        String tasksResponse = mockMvc.perform(get("/api/admin/review-tasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "pending_review"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Number> ids = JsonPath.read(tasksResponse,
                "$.data.items[?(@.productId == %s)].id".formatted(productId));
        return ids.get(0);
    }

    // ── Hot search helpers ──

    protected int indexOfKeyword(List<Map<String, Object>> items, String normalizedKeyword) {
        for (int i = 0; i < items.size(); i++) {
            if (normalizedKeyword.equals(String.valueOf(items.get(i).get("normalizedKeyword")))) {
                return i;
            }
        }
        return -1;
    }

    protected int toIntValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
