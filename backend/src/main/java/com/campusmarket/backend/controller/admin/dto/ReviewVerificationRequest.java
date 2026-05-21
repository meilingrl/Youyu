package com.campusmarket.backend.controller.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReviewVerificationRequest {

    @NotBlank(message = "审核动作不能为空")
    private String action;

    @Size(max = 255, message = "驳回原因不能超过255个字符")
    private String rejectReason;

    @Size(max = 255, message = "审核备注不能超过255个字符")
    private String reviewNote;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }
}
