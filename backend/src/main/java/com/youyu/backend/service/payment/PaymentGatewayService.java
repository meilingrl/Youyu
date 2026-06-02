package com.youyu.backend.service.payment;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentGatewayService {

    String paymentMethod();

    String gatewayCode();

    boolean available();

    Map<String, Object> createPayment(PaymentInitiationRequest request);

    GatewayCallbackResult verifyCallback(Map<String, String> parameters);

    Map<String, Object> refund(RefundRequest request);

    record PaymentInitiationRequest(String paymentNo, String orderNo, BigDecimal amount, String subject) {
    }

    record GatewayCallbackResult(String paymentNo,
                                 String providerTradeNo,
                                 String paymentStatus,
                                 BigDecimal amount,
                                 String eventFingerprint,
                                 String callbackSummary) {
    }

    record RefundRequest(String paymentNo, String refundNo, BigDecimal amount, String reason) {
    }
}
