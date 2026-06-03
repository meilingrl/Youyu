package com.youyu.backend.service.payment.impl;

import com.youyu.backend.service.payment.PaymentGatewayService;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "youyu.payment", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockPaymentGatewayServiceImpl implements PaymentGatewayService {

    @Override
    public String paymentMethod() {
        return "mock";
    }

    @Override
    public String gatewayCode() {
        return "internal_mock";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public Map<String, Object> createPayment(PaymentInitiationRequest request) {
        return Map.of(
                "gateway", gatewayCode(),
                "paymentMethod", paymentMethod(),
                "orderNo", request.orderNo(),
                "paymentNo", request.paymentNo(),
                "status", "pending"
        );
    }

    @Override
    public GatewayCallbackResult verifyCallback(Map<String, String> parameters) {
        throw new UnsupportedOperationException("Mock payments complete through the local mock-success endpoint");
    }

    @Override
    public Map<String, Object> refund(RefundRequest request) {
        return Map.of(
                "gateway", gatewayCode(),
                "refundNo", request.refundNo(),
                "status", "success"
        );
    }
}
