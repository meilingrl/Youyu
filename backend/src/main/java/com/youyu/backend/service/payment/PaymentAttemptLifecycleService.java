package com.youyu.backend.service.payment;

import com.youyu.backend.mapper.payment.PaymentRecordMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentAttemptLifecycleService {

    private final PaymentRecordMapper paymentRecordMapper;

    public PaymentAttemptLifecycleService(PaymentRecordMapper paymentRecordMapper) {
        this.paymentRecordMapper = paymentRecordMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Object> expireTimedOutAttempts(Long orderId, String paymentNo, LocalDateTime initiatedBefore) {
        paymentRecordMapper.markTimedOutPayments(orderId, initiatedBefore);
        return paymentRecordMapper.findByPaymentNo(paymentNo);
    }
}
