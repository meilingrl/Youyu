package com.campusmarket.backend.entity.payment;

import com.campusmarket.backend.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RefundRecord extends BaseEntity {

    private Long orderId;
    private Long paymentRecordId;
    private String refundStatus;
    private BigDecimal refundAmount;
    private String refundReason;
    private LocalDateTime appliedAt;
    private LocalDateTime processedAt;
    private LocalDateTime completedAt;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getPaymentRecordId() { return paymentRecordId; }
    public void setPaymentRecordId(Long paymentRecordId) { this.paymentRecordId = paymentRecordId; }
    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
