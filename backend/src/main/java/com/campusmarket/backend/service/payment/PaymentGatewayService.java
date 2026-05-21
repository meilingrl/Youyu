package com.campusmarket.backend.service.payment;

import java.util.Map;

public interface PaymentGatewayService {

    String gatewayCode();

    Map<String, Object> createPayment(String orderNo);
}

