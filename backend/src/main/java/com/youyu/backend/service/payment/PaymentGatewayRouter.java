package com.youyu.backend.service.payment;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.exception.BusinessException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayRouter {

    private final Map<String, PaymentGatewayService> gateways;

    public PaymentGatewayRouter(List<PaymentGatewayService> gateways) {
        Map<String, PaymentGatewayService> configuredGateways = new LinkedHashMap<>();
        gateways.forEach(gateway -> configuredGateways.put(normalize(gateway.paymentMethod()), gateway));
        this.gateways = Map.copyOf(configuredGateways);
    }

    public PaymentGatewayService require(String paymentMethod) {
        PaymentGatewayService gateway = gateways.get(normalize(paymentMethod));
        if (gateway == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Unsupported payment method");
        }
        if (!gateway.available()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Payment gateway is not configured");
        }
        return gateway;
    }

    public List<Map<String, Object>> availableMethods() {
        return gateways.values().stream()
                .filter(PaymentGatewayService::available)
                .map(gateway -> Map.<String, Object>of(
                        "paymentMethod", gateway.paymentMethod(),
                        "gateway", gateway.gatewayCode()
                ))
                .toList();
    }

    private String normalize(String paymentMethod) {
        return paymentMethod == null || paymentMethod.isBlank()
                ? "mock"
                : paymentMethod.trim().toLowerCase(Locale.ROOT);
    }
}
