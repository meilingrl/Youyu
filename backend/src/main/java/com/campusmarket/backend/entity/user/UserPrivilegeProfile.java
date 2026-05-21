package com.campusmarket.backend.entity.user;

import com.campusmarket.backend.entity.BaseEntity;

public class UserPrivilegeProfile extends BaseEntity {
    private Long userId;
    private boolean canPurchase;
    private boolean canPublish;
    private boolean canReview;
    private boolean canApplyShop;
    private boolean restricted;
    private String restrictedReason;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isCanPurchase() {
        return canPurchase;
    }

    public void setCanPurchase(boolean canPurchase) {
        this.canPurchase = canPurchase;
    }

    public boolean isCanPublish() {
        return canPublish;
    }

    public void setCanPublish(boolean canPublish) {
        this.canPublish = canPublish;
    }

    public boolean isCanReview() {
        return canReview;
    }

    public void setCanReview(boolean canReview) {
        this.canReview = canReview;
    }

    public boolean isCanApplyShop() {
        return canApplyShop;
    }

    public void setCanApplyShop(boolean canApplyShop) {
        this.canApplyShop = canApplyShop;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    public String getRestrictedReason() {
        return restrictedReason;
    }

    public void setRestrictedReason(String restrictedReason) {
        this.restrictedReason = restrictedReason;
    }
}
