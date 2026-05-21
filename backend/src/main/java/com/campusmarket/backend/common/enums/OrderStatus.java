package com.campusmarket.backend.common.enums;

import com.campusmarket.backend.common.api.ResultCode;
import com.campusmarket.backend.common.exception.BusinessException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public enum OrderStatus {
    PENDING_PAYMENT("pending_payment",
            Set.of("pending_fulfillment", "pending_receipt", "cancelled")),
    PENDING_FULFILLMENT("pending_fulfillment",
            Set.of("pending_receipt", "refunding")),
    PENDING_RECEIPT("pending_receipt",
            Set.of("completed", "refunding")),
    COMPLETED("completed",
            Set.of("refunding")),
    CANCELLED("cancelled", Set.of()),
    REFUNDING("refunding",
            Set.of("refunded")),
    REFUNDED("refunded", Set.of());

    private static final Map<String, OrderStatus> BY_VALUE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(OrderStatus::getValue, s -> s));

    private final String value;
    private final Set<String> allowedNext;

    OrderStatus(String value, Set<String> allowedNext) {
        this.value = value;
        this.allowedNext = allowedNext;
    }

    public String getValue() {
        return value;
    }

    public static OrderStatus fromValue(String value) {
        OrderStatus status = BY_VALUE.get(value);
        if (status == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "未知的订单状态: " + value);
        }
        return status;
    }

    public void requireTransitionTo(String target) {
        if (!allowedNext.contains(target)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "不允许从 " + value + " 转换到 " + target);
        }
    }
}
