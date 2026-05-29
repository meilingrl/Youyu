package com.youyu.backend.service.marketing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface MarketingService {

    List<Map<String, Object>> listOwnerCoupons(Long ownerUserId);

    Map<String, Object> createOwnerCoupon(Long ownerUserId, Map<String, Object> command);

    Map<String, Object> updateOwnerCoupon(Long ownerUserId, Long couponId, Map<String, Object> command);

    Map<String, Object> updateOwnerCouponStatus(Long ownerUserId, Long couponId, Map<String, Object> command);

    List<Map<String, Object>> listAvailableCoupons(Long shopId);

    Map<String, Object> claimCoupon(Long userId, Long couponId);

    List<Map<String, Object>> listMyCoupons(Long userId);

    List<Map<String, Object>> listApplicableUserCoupons(Long userId, Long shopId, BigDecimal orderAmount);

    Map<String, Object> validateCouponForOrder(Long userId, Long userCouponId, Long shopId, BigDecimal orderAmount);

    void markCouponUsed(Long userId, Long userCouponId, Long orderId);

    List<Map<String, Object>> listAdminCoupons(String reviewStatus);

    Map<String, Object> reviewCoupon(Long couponId, Map<String, Object> command, Long reviewerId);

    Map<String, Object> disableCoupon(Long couponId, Long reviewerId);

    List<Map<String, Object>> listOwnerActivities(Long ownerUserId);

    Map<String, Object> createOwnerActivity(Long ownerUserId, Map<String, Object> command);

    Map<String, Object> updateOwnerActivity(Long ownerUserId, Long activityId, Map<String, Object> command);

    Map<String, Object> updateOwnerActivityStatus(Long ownerUserId, Long activityId, Map<String, Object> command);

    List<Map<String, Object>> listPublicActivities(Long shopId);

    List<Map<String, Object>> listAdminActivities(String reviewStatus);

    Map<String, Object> reviewActivity(Long activityId, Map<String, Object> command, Long reviewerId);

    Map<String, Object> disableActivity(Long activityId, Long reviewerId);
}
