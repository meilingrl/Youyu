package com.youyu.backend.common.auth;

public class AuthUser {

    private final Long userId;
    private final String role;
    private final String token;

    public AuthUser(Long userId, String role, String token) {
        this.userId = userId;
        this.role = role;
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getToken() {
        return token;
    }
}
