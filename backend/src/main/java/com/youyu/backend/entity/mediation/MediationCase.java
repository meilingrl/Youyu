package com.youyu.backend.entity.mediation;

import java.time.LocalDateTime;

public class MediationCase {

    private Long id;
    private String caseNo;
    private Long sourceReportId;
    private Long relatedOrderId;
    private Long buyerUserId;
    private Long sellerUserId;
    private Long reporterUserId;
    private String status;
    private String decisionCategory;
    private String decisionSummary;
    private String enforcementSummary;
    private String cancelReason;
    private Long decidedByAdminUserId;
    private LocalDateTime decidedAt;
    private Long createdByAdminUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastStatusChangedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCaseNo() {
        return caseNo;
    }

    public void setCaseNo(String caseNo) {
        this.caseNo = caseNo;
    }

    public Long getSourceReportId() {
        return sourceReportId;
    }

    public void setSourceReportId(Long sourceReportId) {
        this.sourceReportId = sourceReportId;
    }

    public Long getRelatedOrderId() {
        return relatedOrderId;
    }

    public void setRelatedOrderId(Long relatedOrderId) {
        this.relatedOrderId = relatedOrderId;
    }

    public Long getBuyerUserId() {
        return buyerUserId;
    }

    public void setBuyerUserId(Long buyerUserId) {
        this.buyerUserId = buyerUserId;
    }

    public Long getSellerUserId() {
        return sellerUserId;
    }

    public void setSellerUserId(Long sellerUserId) {
        this.sellerUserId = sellerUserId;
    }

    public Long getReporterUserId() {
        return reporterUserId;
    }

    public void setReporterUserId(Long reporterUserId) {
        this.reporterUserId = reporterUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDecisionCategory() {
        return decisionCategory;
    }

    public void setDecisionCategory(String decisionCategory) {
        this.decisionCategory = decisionCategory;
    }

    public String getDecisionSummary() {
        return decisionSummary;
    }

    public void setDecisionSummary(String decisionSummary) {
        this.decisionSummary = decisionSummary;
    }

    public String getEnforcementSummary() {
        return enforcementSummary;
    }

    public void setEnforcementSummary(String enforcementSummary) {
        this.enforcementSummary = enforcementSummary;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public Long getDecidedByAdminUserId() {
        return decidedByAdminUserId;
    }

    public void setDecidedByAdminUserId(Long decidedByAdminUserId) {
        this.decidedByAdminUserId = decidedByAdminUserId;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public Long getCreatedByAdminUserId() {
        return createdByAdminUserId;
    }

    public void setCreatedByAdminUserId(Long createdByAdminUserId) {
        this.createdByAdminUserId = createdByAdminUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastStatusChangedAt() {
        return lastStatusChangedAt;
    }

    public void setLastStatusChangedAt(LocalDateTime lastStatusChangedAt) {
        this.lastStatusChangedAt = lastStatusChangedAt;
    }
}
