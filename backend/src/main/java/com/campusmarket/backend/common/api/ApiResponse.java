package com.campusmarket.backend.common.api;

public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;
    private String traceId;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, String code, String message, T data, String traceId) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = traceId;
    }

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(
                true,
                ResultCode.SUCCESS.getCode(),
                ResultCode.SUCCESS.getMessage(),
                data,
                traceId
        );
    }

    public static <T> ApiResponse<T> success(String message, T data, String traceId) {
        return new ApiResponse<>(
                true,
                ResultCode.SUCCESS.getCode(),
                message,
                data,
                traceId
        );
    }

    public static <T> ApiResponse<T> failure(ResultCode resultCode, String message, String traceId) {
        return new ApiResponse<>(
                false,
                resultCode.getCode(),
                message,
                null,
                traceId
        );
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
