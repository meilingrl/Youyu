package com.youyu.backend.payment;

import com.jayway.jsonpath.JsonPath;
import com.youyu.backend.BackendTestBase;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentRefundConsistencyTest extends BackendTestBase {

    private static final String USER = "mock-1002-USER";
    private static final String ADMIN = "mock-9001-ADMIN";

    @Test
    void refundApplicationSelectsSuccessfulPaymentRecordExplicitly() throws Exception {
        Number orderId = createPaidOfflineOrder();
        Long successfulPaymentId = findSuccessfulPaymentId(orderId);
        insertLaterFailedPayment(orderId);

        Number refundId = applyRefund(orderId);

        Long refundedPaymentId = jdbcTemplate.queryForObject(
                "SELECT payment_record_id FROM refund_records WHERE id = ?", Long.class, refundId);
        assertThat(refundedPaymentId).isEqualTo(successfulPaymentId);
    }

    @Test
    void duplicateRefundCompletionIsIdempotentAndUpdatesAllRecords() throws Exception {
        Number orderId = createPaidOfflineOrder();
        Long paymentId = findSuccessfulPaymentId(orderId);
        Number refundId = applyRefund(orderId);

        completeRefund(orderId, refundId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("refunded"))
                .andExpect(jsonPath("$.data.paymentStatus").value("refunded"));
        completeRefund(orderId, refundId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("refunded"))
                .andExpect(jsonPath("$.data.paymentStatus").value("refunded"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT refund_status FROM refund_records WHERE id = ?", String.class, refundId))
                .isEqualTo("completed");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT payment_status FROM payment_records WHERE id = ?", String.class, paymentId))
                .isEqualTo("refunded");
    }

    @Test
    void gatewayFailureLeavesRefundDiagnosableAndOrderIncomplete() throws Exception {
        Number orderId = createPaidOfflineOrder();
        jdbcTemplate.update("""
                UPDATE payment_records
                SET payment_method = 'unsupported_gateway'
                WHERE order_id = ? AND payment_status = 'success'
                """, orderId);
        Number refundId = applyRefund(orderId);

        completeRefund(orderId, refundId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT refund_status FROM refund_records WHERE id = ?", String.class, refundId))
                .isEqualTo("failed");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT failed_reason FROM refund_records WHERE id = ?", String.class, refundId))
                .contains("Unsupported payment method");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT order_status FROM orders WHERE id = ?", String.class, orderId))
                .isEqualTo("refunding");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT payment_status FROM orders WHERE id = ?", String.class, orderId))
                .isEqualTo("refunding");
    }

    private Number createPaidOfflineOrder() throws Exception {
        Number cartItemId = addToCart(USER, 3002, 1);
        Number orderId = createOrder(USER, cartItemId, "offline",
                "\"offlineMeetTime\": \"2026-07-01 12:00\", \"offlineMeetLocation\": \"Library\"");
        initiateAndPay(USER, orderId);
        return orderId;
    }

    private Number applyRefund(Number orderId) throws Exception {
        String response = mockMvc.perform(post("/api/orders/{orderId}/refunds", orderId)
                        .header("Authorization", "Bearer " + USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refundReason\": \"Item does not match description\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("refunding"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.data.refunds[0].id");
    }

    private org.springframework.test.web.servlet.ResultActions completeRefund(Number orderId, Number refundId)
            throws Exception {
        return mockMvc.perform(post("/api/admin/orders/{orderId}/refunds/{refundId}/complete", orderId, refundId)
                        .header("Authorization", "Bearer " + ADMIN));
    }

    private Long findSuccessfulPaymentId(Number orderId) {
        return jdbcTemplate.queryForObject("""
                SELECT id FROM payment_records
                WHERE order_id = ? AND payment_status = 'success'
                """, Long.class, orderId);
    }

    private void insertLaterFailedPayment(Number orderId) {
        jdbcTemplate.update("""
                INSERT INTO payment_records (
                    order_id, payment_no, payment_method, payment_channel, payment_status,
                    amount, initiated_at, failed_reason, callback_summary
                ) VALUES (?, ?, 'mock', 'internal_mock', 'failed', ?, ?, 'declined', '')
                """,
                orderId,
                "PAY-FAILED-" + orderId,
                new BigDecimal("88.00"),
                Timestamp.valueOf(LocalDateTime.now().plusMinutes(1)));
    }
}
