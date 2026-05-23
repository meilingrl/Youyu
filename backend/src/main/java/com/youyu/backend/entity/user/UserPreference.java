package com.youyu.backend.entity.user;

import com.youyu.backend.entity.BaseEntity;

public class UserPreference extends BaseEntity {

    private Long userId;
    private String themeMode;
    private String themeColor;
    private Long defaultAddressId;
    private String defaultFulfillmentType;
    private String defaultPaymentMethod;
    private String defaultSortType;
    private String notificationPreference;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getThemeMode() {
        return themeMode;
    }

    public void setThemeMode(String themeMode) {
        this.themeMode = themeMode;
    }

    public String getThemeColor() {
        return themeColor;
    }

    public void setThemeColor(String themeColor) {
        this.themeColor = themeColor;
    }

    public Long getDefaultAddressId() {
        return defaultAddressId;
    }

    public void setDefaultAddressId(Long defaultAddressId) {
        this.defaultAddressId = defaultAddressId;
    }

    public String getDefaultFulfillmentType() {
        return defaultFulfillmentType;
    }

    public void setDefaultFulfillmentType(String defaultFulfillmentType) {
        this.defaultFulfillmentType = defaultFulfillmentType;
    }

    public String getDefaultPaymentMethod() {
        return defaultPaymentMethod;
    }

    public void setDefaultPaymentMethod(String defaultPaymentMethod) {
        this.defaultPaymentMethod = defaultPaymentMethod;
    }

    public String getDefaultSortType() {
        return defaultSortType;
    }

    public void setDefaultSortType(String defaultSortType) {
        this.defaultSortType = defaultSortType;
    }

    public String getNotificationPreference() {
        return notificationPreference;
    }

    public void setNotificationPreference(String notificationPreference) {
        this.notificationPreference = notificationPreference;
    }
}
