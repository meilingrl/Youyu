package com.youyu.backend.mapper.marketing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MarketingMapper {

    List<Map<String, Object>> findOwnerCoupons(Long ownerUserId);

    List<Map<String, Object>> findAdminCoupons(String reviewStatus);

    List<Map<String, Object>> findAvailableCoupons(Long shopId);

    List<Map<String, Object>> findUserCoupons(Long userId);

    List<Map<String, Object>> findApplicableUserCoupons(Long userId, Long shopId, BigDecimal orderAmount);

    Optional<Map<String, Object>> findCouponById(Long couponId);

    Optional<Map<String, Object>> findUserCouponById(Long userCouponId);

    Long insertCoupon(Map<String, Object> coupon);

    int updateCoupon(Long couponId, Long ownerUserId, Map<String, Object> coupon);

    int updateCouponStatus(Long couponId, Long ownerUserId, String status);

    int reviewCoupon(Long couponId, String reviewStatus, String rejectReason, String reviewNote, Long reviewerId);

    int disableCoupon(Long couponId, Long reviewerId);

    int incrementClaimedQuantity(Long couponId);

    Long insertUserCoupon(Long userId, Long couponId);

    int markUserCouponUsed(Long userCouponId, Long userId, Long orderId);

    List<Map<String, Object>> findOwnerActivities(Long ownerUserId);

    List<Map<String, Object>> findAdminActivities(String reviewStatus);

    List<Map<String, Object>> findPublicActivities(Long shopId);

    Optional<Map<String, Object>> findActivityById(Long activityId);

    Long insertActivity(Map<String, Object> activity);

    int updateActivity(Long activityId, Long ownerUserId, Map<String, Object> activity);

    int updateActivityStatus(Long activityId, Long ownerUserId, String status);

    int reviewActivity(Long activityId, String reviewStatus, String rejectReason, String reviewNote, Long reviewerId);

    int disableActivity(Long activityId, Long reviewerId);
}
