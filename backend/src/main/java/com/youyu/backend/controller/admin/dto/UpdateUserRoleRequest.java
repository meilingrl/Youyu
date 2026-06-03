package com.youyu.backend.controller.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateUserRoleRequest {

    @NotBlank
    @Size(max = 32)
    private String role;

    @Size(max = 255)
    private String reason;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
