package com.youyu.backend.service.payment.impl;

import com.youyu.backend.service.payment.PaymentGatewayService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MockPaymentGatewayServiceImpl implements PaymentGatewayService {

    @Override
    public String gatewayCode() {
        return "MOCK";
    }

    @Override
    public Map<String, Object> createPayment(String orderNo) {
        return Map.of(
                "gateway", gatewayCode(),
                "orderNo", orderNo,
                "status", "PENDING",
                "message", "Mock payment gateway is reserved for future sandbox integration"
        );
    }
}
