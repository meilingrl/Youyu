package com.youyu.backend.common.auth;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AdminPermissionPolicy {

    private static final EnumSet<AdminPermission> ALL_ADMIN_PERMISSIONS = EnumSet.allOf(AdminPermission.class);
    private static final Map<UserRole, Set<AdminPermission>> ROLE_PERMISSIONS = new EnumMap<>(UserRole.class);

    static {
        ROLE_PERMISSIONS.put(UserRole.ADMIN, ALL_ADMIN_PERMISSIONS);
        ROLE_PERMISSIONS.put(UserRole.SUPER_ADMIN, ALL_ADMIN_PERMISSIONS);
        ROLE_PERMISSIONS.put(UserRole.SUPPORT_AGENT, EnumSet.of(
                AdminPermission.ADMIN_DASHBOARD_VIEW,
                AdminPermission.ADMIN_SUPPORT_CONTEXT_VIEW,
                AdminPermission.ADMIN_USERS_VIEW,
                AdminPermission.ADMIN_PRODUCTS_VIEW,
                AdminPermission.ADMIN_SHOPS_VIEW,
                AdminPermission.ADMIN_REPORTS_HANDLE,
                AdminPermission.ADMIN_SUPPORT_TICKETS_HANDLE,
                AdminPermission.ADMIN_SEARCH_LOGS_VIEW,
                AdminPermission.ADMIN_ORDERS_READ,
                AdminPermission.ADMIN_MEDIATION_HANDLE
        ));
        ROLE_PERMISSIONS.put(UserRole.REVIEWER, EnumSet.of(
                AdminPermission.ADMIN_DASHBOARD_VIEW,
                AdminPermission.ADMIN_VERIFICATIONS_REVIEW,
                AdminPermission.ADMIN_PRODUCTS_VIEW,
                AdminPermission.ADMIN_PRODUCTS_REVIEW,
                AdminPermission.ADMIN_SHOPS_VIEW,
                AdminPermission.ADMIN_SHOPS_MANAGE
        ));
        ROLE_PERMISSIONS.put(UserRole.OPERATOR, EnumSet.of(
                AdminPermission.ADMIN_DASHBOARD_VIEW,
                AdminPermission.ADMIN_PRODUCTS_VIEW,
                AdminPermission.ADMIN_SEARCH_GOVERN,
                AdminPermission.ADMIN_SEARCH_LOGS_VIEW
        ));
        ROLE_PERMISSIONS.put(UserRole.ORDER_ADMIN, EnumSet.of(
                AdminPermission.ADMIN_DASHBOARD_VIEW,
                AdminPermission.ADMIN_ORDERS_READ,
                AdminPermission.ADMIN_ORDERS_MANAGE,
                AdminPermission.ADMIN_MEDIATION_HANDLE
        ));
    }

    private AdminPermissionPolicy() {
    }

    public static boolean hasPermission(String roleName, AdminPermission permission) {
        Optional<UserRole> role = UserRole.fromName(roleName);
        return role.isPresent() && ROLE_PERMISSIONS.getOrDefault(role.get(), Set.of()).contains(permission);
    }

    public static boolean hasAnyPermission(String roleName, AdminPermission[] permissions) {
        for (AdminPermission permission : permissions) {
            if (hasPermission(roleName, permission)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAdminRole(String roleName) {
        return UserRole.fromName(roleName).map(UserRole::isAdminRole).orElse(false);
    }

    public static String normalizeRoleName(String roleName) {
        return roleName == null ? "" : roleName.trim().toUpperCase(Locale.ROOT);
    }
}
