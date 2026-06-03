package com.youyu.backend.mapper.payment;

import java.time.LocalDateTime;
import java.util.Map;

public interface PaymentRecordMapper {

    Map<String, Object> findByPaymentNo(String paymentNo);

    boolean recordCallback(String gatewayCode, String eventFingerprint, String paymentNo, String callbackSummary);

    void updateStatus(String paymentNo, String paymentStatus, String providerTradeNo,
                      String failedReason, String callbackSummary, LocalDateTime succeededAt);

    int markTimedOutPayments(Long orderId, LocalDateTime initiatedBefore);
}
