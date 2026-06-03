package com.youyu.backend.payment;

import com.jayway.jsonpath.JsonPath;
import com.youyu.backend.BackendTestBase;
import com.youyu.backend.mapper.payment.PaymentRecordMapper;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentGatewayFoundationTest extends BackendTestBase {

    private static final String USER = "mock-1002-USER";

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Test
    void timedOutPaymentAttemptCanBeRetried() throws Exception {
        Number orderId = createPayableOrder();
        String firstPaymentNo = paymentNo(initiatePayment(USER, orderId));
        jdbcTemplate.update("UPDATE payment_records SET initiated_at = ? WHERE payment_no = ?",
                Timestamp.valueOf(LocalDateTime.now().minusHours(1)), firstPaymentNo);

        String secondPaymentNo = paymentNo(initiatePayment(USER, orderId));

        assertThat(secondPaymentNo).isNotEqualTo(firstPaymentNo);
        assertThat(paymentRecordMapper.findByPaymentNo(firstPaymentNo).get("paymentStatus")).isEqualTo("timed_out");
    }

    @Test
    void failedAndCancelledPaymentAttemptsCanBeRetried() throws Exception {
        Number failedOrderId = createPayableOrder();
        String failedPaymentNo = paymentNo(initiatePayment(USER, failedOrderId));
        jdbcTemplate.update("UPDATE payment_records SET payment_status = 'failed' WHERE payment_no = ?", failedPaymentNo);
        assertThat(paymentNo(initiatePayment(USER, failedOrderId))).isNotEqualTo(failedPaymentNo);

        Number cancelledOrderId = createPayableOrder();
        String cancelledPaymentNo = paymentNo(initiatePayment(USER, cancelledOrderId));
        jdbcTemplate.update("UPDATE payment_records SET payment_status = 'cancelled' WHERE payment_no = ?", cancelledPaymentNo);
        assertThat(paymentNo(initiatePayment(USER, cancelledOrderId))).isNotEqualTo(cancelledPaymentNo);
    }

    @Test
    void duplicateCallbackFingerprintIsRejectedAsReplay() throws Exception {
        String paymentNo = paymentNo(initiatePayment(USER, createPayableOrder()));
        String fingerprint = "same-signed-payload";
        assertThat(paymentRecordMapper.recordCallback("alipay_sandbox", fingerprint, paymentNo,
                "trade_status=TRADE_SUCCESS")).isTrue();
        assertThat(paymentRecordMapper.recordCallback("alipay_sandbox", fingerprint, paymentNo,
                "trade_status=TRADE_SUCCESS")).isFalse();
    }

    @Test
    void activePaymentCanResumeWithoutCreatingAnotherAttempt() throws Exception {
        Number orderId = createPayableOrder();
        String paymentNo = paymentNo(initiatePayment(USER, orderId));

        mockMvc.perform(post("/api/payments/%s/resume".formatted(paymentNo))
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.payment.paymentNo").value(paymentNo))
                .andExpect(jsonPath("$.data.payment.paymentStatus").value("initiated"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_records WHERE order_id = ?", Integer.class, orderId)).isEqualTo(1);
    }

    @Test
    void timedOutPaymentCannotResume() throws Exception {
        Number orderId = createPayableOrder();
        String paymentNo = paymentNo(initiatePayment(USER, orderId));
        jdbcTemplate.update("UPDATE payment_records SET initiated_at = ? WHERE payment_no = ?",
                Timestamp.valueOf(LocalDateTime.now().minusHours(1)), paymentNo);

        mockMvc.perform(post("/api/payments/%s/resume".formatted(paymentNo))
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("The payment attempt is no longer active"));

        assertThat(paymentRecordMapper.findByPaymentNo(paymentNo).get("paymentStatus")).isEqualTo("timed_out");
    }

    @Test
    void timedOutMockPaymentCannotCompleteLocally() throws Exception {
        Number orderId = createPayableOrder();
        String paymentNo = paymentNo(initiatePayment(USER, orderId));
        jdbcTemplate.update("UPDATE payment_records SET initiated_at = ? WHERE payment_no = ?",
                Timestamp.valueOf(LocalDateTime.now().minusHours(1)), paymentNo);

        mockMvc.perform(post("/api/payments/%s/mock-success".formatted(paymentNo))
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("The payment attempt is no longer active"));

        assertThat(paymentRecordMapper.findByPaymentNo(paymentNo).get("paymentStatus")).isEqualTo("timed_out");
    }

    private Number createPayableOrder() throws Exception {
        Number cartItemId = addToCart(USER, 3001, 1);
        return createOrder(USER, cartItemId, "digital", null);
    }

    private String paymentNo(String paymentResponse) {
        return JsonPath.read(paymentResponse, "$.data.payment.paymentNo");
    }

}
