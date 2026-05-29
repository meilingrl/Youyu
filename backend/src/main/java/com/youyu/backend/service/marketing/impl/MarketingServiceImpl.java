package com.youyu.backend.service.marketing.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.common.exception.ForbiddenException;
import com.youyu.backend.mapper.marketing.MarketingMapper;
import com.youyu.backend.mapper.shop.ShopMapper;
import com.youyu.backend.service.marketing.MarketingService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketingServiceImpl implements MarketingService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final MarketingMapper marketingMapper;
    private final ShopMapper shopMapper;

    public MarketingServiceImpl(MarketingMapper marketingMapper, ShopMapper shopMapper) {
        this.marketingMapper = marketingMapper;
        this.shopMapper = shopMapper;
    }

    @Override
    public List<Map<String, Object>> listOwnerCoupons(Long ownerUserId) {
        requireOwnedShop(ownerUserId);
        return marketingMapper.findOwnerCoupons(ownerUserId);
    }

    @Override
    @Transactional
    public Map<String, Object> createOwnerCoupon(Long ownerUserId, Map<String, Object> command) {
        Map<String, Object> shop = requireOwnedShop(ownerUserId);
        Map<String, Object> coupon = couponPayload(ownerUserId, shop, command);
        Long couponId = marketingMapper.insertCoupon(coupon);
        return requireCoupon(couponId);
    }

    @Override
    @Transactional
    public Map<String, Object> updateOwnerCoupon(Long ownerUserId, Long couponId, Map<String, Object> command) {
        ensureCouponOwner(ownerUserId, couponId);
        Map<String, Object> shop = requireOwnedShop(ownerUserId);
        int updated = marketingMapper.updateCoupon(couponId, ownerUserId, couponPayload(ownerUserId, shop, command));
        if (updated == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Claimed coupons cannot be edited; disable and create a new coupon");
        }
        return requireCoupon(couponId);
    }

    @Override
    @Transactional
    public Map<String, Object> updateOwnerCouponStatus(Long ownerUserId, Long couponId, Map<String, Object> command) {
        ensureCouponOwner(ownerUserId, couponId);
        int updated = marketingMapper.updateCouponStatus(couponId, ownerUserId, normalizeStatus(requiredString(command, "status")));
        if (updated == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Coupon not found");
        }
        return requireCoupon(couponId);
    }

    @Override
    public List<Map<String, Object>> listAvailableCoupons(Long shopId) {
        return marketingMapper.findAvailableCoupons(shopId);
    }

    @Override
    @Transactional
    public Map<String, Object> claimCoupon(Long userId, Long couponId) {
        Map<String, Object> coupon = requireCoupon(couponId);
        assertCouponClaimable(coupon);
        if (Objects.equals(coupon.get("ownerUserId"), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Shop owners cannot claim their own coupon");
        }
        int claimed = marketingMapper.incrementClaimedQuantity(couponId);
        if (claimed == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Coupon is no longer claimable");
        }
        try {
            Long userCouponId = marketingMapper.insertUserCoupon(userId, couponId);
            return requireUserCoupon(userCouponId);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Coupon already claimed");
        }
    }

    @Override
    public List<Map<String, Object>> listMyCoupons(Long userId) {
        return marketingMapper.findUserCoupons(userId);
    }

    @Override
    public List<Map<String, Object>> listApplicableUserCoupons(Long userId, Long shopId, BigDecimal orderAmount) {
        if (shopId == null) {
            return List.of();
        }
        return marketingMapper.findApplicableUserCoupons(userId, shopId, orderAmount.setScale(2, RoundingMode.HALF_UP)).stream()
                .map(coupon -> withPreviewDiscount(coupon, orderAmount))
                .toList();
    }

    @Override
    public Map<String, Object> validateCouponForOrder(Long userId, Long userCouponId, Long shopId, BigDecimal orderAmount) {
        if (userCouponId == null) {
            return null;
        }
        Map<String, Object> userCoupon = requireUserCoupon(userCouponId);
        if (!Objects.equals(userCoupon.get("userId"), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Coupon does not belong to current user");
        }
        if (!"claimed".equals(userCoupon.get("userCouponStatus"))) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Coupon has already been used");
        }
        if (!Objects.equals(userCoupon.get("shopId"), shopId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Coupon cannot be used for this shop");
        }
        assertCouponEffective(userCoupon);
        BigDecimal minimum = decimal(userCoupon.get("minimumSpendAmount"));
        if (orderAmount.compareTo(minimum) < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Order amount does not meet coupon threshold");
        }
        return withPreviewDiscount(userCoupon, orderAmount);
    }

    @Override
    @Transactional
    public void markCouponUsed(Long userId, Long userCouponId, Long orderId) {
        if (userCouponId == null) {
            return;
        }
        int updated = marketingMapper.markUserCouponUsed(userCouponId, userId, orderId);
        if (updated == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Coupon has already been used");
        }
    }

    @Override
    public List<Map<String, Object>> listAdminCoupons(String reviewStatus) {
        return marketingMapper.findAdminCoupons(reviewStatus);
    }

    @Override
    @Transactional
    public Map<String, Object> reviewCoupon(Long couponId, Map<String, Object> command, Long reviewerId) {
        String reviewStatus = reviewStatusFromAction(requiredString(command, "action"));
        String rejectReason = "rejected".equals(reviewStatus) ? requiredString(command, "rejectReason") : "";
        int updated = marketingMapper.reviewCoupon(
                couponId,
                reviewStatus,
                rejectReason,
                optionalString(command, "reviewNote"),
                reviewerId
        );
        if (updated == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Coupon not found");
        }
        return requireCoupon(couponId);
    }

    @Override
    @Transactional
    public Map<String, Object> disableCoupon(Long couponId, Long reviewerId) {
        int updated = marketingMapper.disableCoupon(couponId, reviewerId);
        if (updated == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Coupon not found");
        }
        return requireCoupon(couponId);
    }

    @Override
    public List<Map<String, Object>> listOwnerActivities(Long ownerUserId) {
        requireOwnedShop(ownerUserId);
        return marketingMapper.findOwnerActivities(ownerUserId);
    }

    @Override
    @Transactional
    public Map<String, Object> createOwnerActivity(Long ownerUserId, Map<String, Object> command) {
        Map<String, Object> shop = requireOwnedShop(ownerUserId);
        Map<String, Object> activity = activityPayload(ownerUserId, shop, command);
        Long activityId = marketingMapper.insertActivity(activity);
        return requireActivity(activityId);
    }

    @Override
    @Transactional
    public Map<String, Object> updateOwnerActivity(Long ownerUserId, Long activityId, Map<String, Object> command) {
        ensureActivityOwner(ownerUserId, activityId);
        Map<String, Object> shop = requireOwnedShop(ownerUserId);
        int updated = marketingMapper.updateActivity(activityId, ownerUserId, activityPayload(ownerUserId, shop, command));
        if (updated == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Activity not found");
        }
        return requireActivity(activityId);
    }

    @Override
    @Transactional
    public Map<String, Object> updateOwnerActivityStatus(Long ownerUserId, Long activityId, Map<String, Object> command) {
        ensureActivityOwner(ownerUserId, activityId);
        int updated = marketingMapper.updateActivityStatus(activityId, ownerUserId, normalizeStatus(requiredString(command, "status")));
        if (updated == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Activity not found");
        }
        return requireActivity(activityId);
    }

    @Override
    public List<Map<String, Object>> listPublicActivities(Long shopId) {
        return marketingMapper.findPublicActivities(shopId);
    }

    @Override
    public List<Map<String, Object>> listAdminActivities(String reviewStatus) {
        return marketingMapper.findAdminActivities(reviewStatus);
    }

    @Override
    @Transactional
    public Map<String, Object> reviewActivity(Long activityId, Map<String, Object> command, Long reviewerId) {
        String reviewStatus = reviewStatusFromAction(requiredString(command, "action"));
        String rejectReason = "rejected".equals(reviewStatus) ? requiredString(command, "rejectReason") : "";
        int updated = marketingMapper.reviewActivity(
                activityId,
                reviewStatus,
                rejectReason,
                optionalString(command, "reviewNote"),
                reviewerId
        );
        if (updated == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Activity not found");
        }
        return requireActivity(activityId);
    }

    @Override
    @Transactional
    public Map<String, Object> disableActivity(Long activityId, Long reviewerId) {
        int updated = marketingMapper.disableActivity(activityId, reviewerId);
        if (updated == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Activity not found");
        }
        return requireActivity(activityId);
    }

    private Map<String, Object> couponPayload(Long ownerUserId, Map<String, Object> shop, Map<String, Object> command) {
        String type = requiredString(command, "couponType").toUpperCase(Locale.ROOT);
        if (!"FIXED".equals(type) && !"THRESHOLD".equals(type)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "couponType must be FIXED or THRESHOLD");
        }
        BigDecimal discountAmount = positiveDecimal(command, "discountAmount");
        BigDecimal minimumSpendAmount = "THRESHOLD".equals(type)
                ? positiveOrZeroDecimal(command, "minimumSpendAmount")
                : ZERO;
        if ("THRESHOLD".equals(type) && minimumSpendAmount.compareTo(discountAmount) <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "minimumSpendAmount must be greater than discountAmount");
        }
        int totalQuantity = requiredInteger(command, "totalQuantity");
        if (totalQuantity <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "totalQuantity must be greater than 0");
        }
        LocalDateTime startAt = requiredTime(command, "startAt");
        LocalDateTime endAt = requiredTime(command, "endAt");
        if (!endAt.isAfter(startAt)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "endAt must be after startAt");
        }
        return new LinkedHashMap<>(Map.of(
                "shopId", shop.get("id"),
                "ownerUserId", ownerUserId,
                "title", requiredString(command, "title"),
                "description", optionalString(command, "description"),
                "couponType", type,
                "discountAmount", discountAmount,
                "minimumSpendAmount", minimumSpendAmount,
                "totalQuantity", totalQuantity,
                "startAt", startAt,
                "endAt", endAt
        ));
    }

    private Map<String, Object> activityPayload(Long ownerUserId, Map<String, Object> shop, Map<String, Object> command) {
        LocalDateTime startAt = requiredTime(command, "startAt");
        LocalDateTime endAt = requiredTime(command, "endAt");
        if (!endAt.isAfter(startAt)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "endAt must be after startAt");
        }
        return new LinkedHashMap<>(Map.of(
                "shopId", shop.get("id"),
                "ownerUserId", ownerUserId,
                "title", requiredString(command, "title"),
                "description", requiredString(command, "description"),
                "startAt", startAt,
                "endAt", endAt
        ));
    }

    private Map<String, Object> requireOwnedShop(Long ownerUserId) {
        Map<String, Object> shop = shopMapper.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new BusinessException(ResultCode.FORBIDDEN, "Current user does not own a shop"));
        if (!"approved".equals(shop.get("reviewStatus"))) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Shop must be approved before creating marketing content");
        }
        return shop;
    }

    private void ensureCouponOwner(Long ownerUserId, Long couponId) {
        Map<String, Object> coupon = requireCoupon(couponId);
        if (!Objects.equals(coupon.get("ownerUserId"), ownerUserId)) {
            throw new ForbiddenException("Coupon belongs to another shop owner");
        }
    }

    private void ensureActivityOwner(Long ownerUserId, Long activityId) {
        Map<String, Object> activity = requireActivity(activityId);
        if (!Objects.equals(activity.get("ownerUserId"), ownerUserId)) {
            throw new ForbiddenException("Activity belongs to another shop owner");
        }
    }

    private Map<String, Object> requireCoupon(Long couponId) {
        return marketingMapper.findCouponById(couponId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Coupon not found"));
    }

    private Map<String, Object> requireUserCoupon(Long userCouponId) {
        return marketingMapper.findUserCouponById(userCouponId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "User coupon not found"));
    }

    private Map<String, Object> requireActivity(Long activityId) {
        return marketingMapper.findActivityById(activityId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Activity not found"));
    }

    private void assertCouponClaimable(Map<String, Object> coupon) {
        assertCouponEffective(coupon);
        if (integer(coupon.get("claimedQuantity")) >= integer(coupon.get("totalQuantity"))) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Coupon has no remaining quantity");
        }
    }

    private void assertCouponEffective(Map<String, Object> coupon) {
        if (!"approved".equals(coupon.get("reviewStatus")) || !"active".equals(coupon.get("status"))) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Coupon is not active");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startAt = time(coupon.get("startAt"));
        LocalDateTime endAt = time(coupon.get("endAt"));
        if (now.isBefore(startAt) || now.isAfter(endAt)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Coupon is not in effective period");
        }
    }

    private Map<String, Object> withPreviewDiscount(Map<String, Object> coupon, BigDecimal orderAmount) {
        Map<String, Object> result = new LinkedHashMap<>(coupon);
        BigDecimal discount = decimal(coupon.get("discountAmount")).min(orderAmount).setScale(2, RoundingMode.HALF_UP);
        result.put("couponDiscountAmount", discount);
        result.put("discountAmount", decimal(coupon.get("discountAmount")).setScale(2, RoundingMode.HALF_UP));
        return result;
    }

    private String reviewStatusFromAction(String action) {
        return switch (action.toLowerCase(Locale.ROOT)) {
            case "approve", "approved" -> "approved";
            case "reject", "rejected" -> "rejected";
            default -> throw new BusinessException(ResultCode.BAD_REQUEST, "action must be approve or reject");
        };
    }

    private String normalizeStatus(String status) {
        String normalized = status.toLowerCase(Locale.ROOT);
        if (!"active".equals(normalized) && !"disabled".equals(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "status must be active or disabled");
        }
        return normalized;
    }

    private BigDecimal positiveDecimal(Map<String, Object> command, String key) {
        BigDecimal value = requiredDecimal(command, key);
        if (value.compareTo(ZERO) <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, key + " must be greater than 0");
        }
        return value;
    }

    private BigDecimal positiveOrZeroDecimal(Map<String, Object> command, String key) {
        BigDecimal value = requiredDecimal(command, key);
        if (value.compareTo(ZERO) < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, key + " must be greater than or equal to 0");
        }
        return value;
    }

    private BigDecimal requiredDecimal(Map<String, Object> command, String key) {
        Object value = command.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, key + " is required");
        }
        return decimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }

    private int requiredInteger(Map<String, Object> command, String key) {
        Object value = command.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, key + " is required");
        }
        return integer(value);
    }

    private int integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private LocalDateTime requiredTime(Map<String, Object> command, String key) {
        Object value = command.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, key + " is required");
        }
        return time(value);
    }

    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime time) {
            return time;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return LocalDateTime.parse(String.valueOf(value).trim().replace(" ", "T"));
    }

    private String requiredString(Map<String, Object> command, String key) {
        String value = optionalString(command, key);
        if (value.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, key + " is required");
        }
        return value;
    }

    private String optionalString(Map<String, Object> command, String key) {
        Object value = command.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }
}
