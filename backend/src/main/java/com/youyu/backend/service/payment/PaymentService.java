package com.youyu.backend.service.payment;

import java.util.Map;

public interface PaymentService {

    Map<String, Object> gatewayInfo();

    Map<String, Object> initiatePayment(Long userId, Long orderId);

    Map<String, Object> completeMockPayment(Long userId, String paymentNo);
}
