package com.campusmarket.backend.controller.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateUserStatusRequest {

    @NotBlank(message = "用户状态不能为空")
    private String status;

    @Size(max = 255, message = "限制原因不能超过255个字符")
    private String restrictionReason;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRestrictionReason() {
        return restrictionReason;
    }

    public void setRestrictionReason(String restrictionReason) {
        this.restrictionReason = restrictionReason;
    }
}
