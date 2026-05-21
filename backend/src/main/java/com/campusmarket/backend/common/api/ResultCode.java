package com.campusmarket.backend.common.api;

public enum ResultCode {
    SUCCESS("SUCCESS", "Request succeeded"),
    BAD_REQUEST("BAD_REQUEST", "Invalid request parameters"),
    UNAUTHORIZED("UNAUTHORIZED", "Authentication required"),
    FORBIDDEN("FORBIDDEN", "Access denied"),
    NOT_FOUND("NOT_FOUND", "Resource not found"),
    BUSINESS_ERROR("BUSINESS_ERROR", "Business processing failed"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "Internal server error");

    private final String code;
    private final String message;

    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
