package com.youyu.backend.common.auth;

public enum UserRole {
    USER,
    ADMIN,
    SUPER_ADMIN,
    SUPPORT_AGENT,
    REVIEWER,
    OPERATOR,
    ORDER_ADMIN;

    public boolean isAdminRole() {
        return this != USER;
    }

    public static java.util.Optional<UserRole> fromName(String value) {
        if (value == null || value.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(UserRole.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }
}
