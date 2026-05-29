package com.youyu.backend.mapper.marketing.impl;

import com.youyu.backend.common.support.JdbcGeneratedKey;
import com.youyu.backend.mapper.marketing.MarketingMapper;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

@Primary
@Component
public class JdbcMarketingMapper implements MarketingMapper {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMarketingMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> findOwnerCoupons(Long ownerUserId) {
        return jdbcTemplate.queryForList(couponSql() + " WHERE c.owner_user_id = ? ORDER BY c.created_at DESC, c.id DESC", ownerUserId)
                .stream().map(this::couponMap).toList();
    }

    @Override
    public List<Map<String, Object>> findAdminCoupons(String reviewStatus) {
        String sql = couponSql();
        Object[] args = new Object[]{};
        if (reviewStatus != null && !reviewStatus.isBlank()) {
            sql += " WHERE c.review_status = ?";
            args = new Object[]{reviewStatus};
        }
        sql += " ORDER BY c.created_at DESC, c.id DESC";
        return jdbcTemplate.queryForList(sql, args).stream().map(this::couponMap).toList();
    }

    @Override
    public List<Map<String, Object>> findAvailableCoupons(Long shopId) {
        return jdbcTemplate.queryForList(couponSql() + """
                        WHERE c.shop_id = ?
                          AND c.review_status = 'approved'
                          AND c.status = 'active'
                          AND c.start_at <= CURRENT_TIMESTAMP
                          AND c.end_at >= CURRENT_TIMESTAMP
                          AND c.claimed_quantity < c.total_quantity
                        ORDER BY c.end_at, c.id
                        """, shopId)
                .stream().map(this::couponMap).toList();
    }

    @Override
    public List<Map<String, Object>> findUserCoupons(Long userId) {
        return jdbcTemplate.queryForList(userCouponSql() + " WHERE uc.user_id = ? ORDER BY uc.claimed_at DESC, uc.id DESC", userId)
                .stream().map(this::userCouponMap).toList();
    }

    @Override
    public List<Map<String, Object>> findApplicableUserCoupons(Long userId, Long shopId, BigDecimal orderAmount) {
        return jdbcTemplate.queryForList(userCouponSql() + """
                        WHERE uc.user_id = ?
                          AND uc.status = 'claimed'
                          AND c.shop_id = ?
                          AND c.review_status = 'approved'
                          AND c.status = 'active'
                          AND c.start_at <= CURRENT_TIMESTAMP
                          AND c.end_at >= CURRENT_TIMESTAMP
                          AND c.minimum_spend_amount <= ?
                        ORDER BY c.end_at, uc.id
                        """, userId, shopId, orderAmount)
                .stream().map(this::userCouponMap).toList();
    }

    @Override
    public Optional<Map<String, Object>> findCouponById(Long couponId) {
        return jdbcTemplate.queryForList(couponSql() + " WHERE c.id = ?", couponId)
                .stream().findFirst().map(this::couponMap);
    }

    @Override
    public Optional<Map<String, Object>> findUserCouponById(Long userCouponId) {
        return jdbcTemplate.queryForList(userCouponSql() + " WHERE uc.id = ?", userCouponId)
                .stream().findFirst().map(this::userCouponMap);
    }

    @Override
    public Long insertCoupon(Map<String, Object> coupon) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO marketing_coupons (
                        shop_id, owner_user_id, title, description, coupon_type,
                        discount_amount, minimum_spend_amount, total_quantity,
                        status, review_status, start_at, end_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'active', 'pending_review', ?, ?, CURRENT_TIMESTAMP)
                    """, new String[]{"id"});
            ps.setObject(1, coupon.get("shopId"));
            ps.setObject(2, coupon.get("ownerUserId"));
            ps.setString(3, string(coupon.get("title")));
            ps.setString(4, string(coupon.get("description")));
            ps.setString(5, string(coupon.get("couponType")));
            ps.setBigDecimal(6, decimal(coupon.get("discountAmount")));
            ps.setBigDecimal(7, decimal(coupon.get("minimumSpendAmount")));
            ps.setInt(8, integer(coupon.get("totalQuantity")));
            ps.setTimestamp(9, timestamp(coupon.get("startAt")));
            ps.setTimestamp(10, timestamp(coupon.get("endAt")));
            return ps;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "marketing coupon id");
    }

    @Override
    public int updateCoupon(Long couponId, Long ownerUserId, Map<String, Object> coupon) {
        return jdbcTemplate.update("""
                        UPDATE marketing_coupons
                        SET title = ?, description = ?, coupon_type = ?, discount_amount = ?,
                            minimum_spend_amount = ?, total_quantity = ?, review_status = 'pending_review',
                            reject_reason = NULL, review_note = NULL, reviewed_by = NULL, reviewed_at = NULL,
                            start_at = ?, end_at = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND owner_user_id = ? AND claimed_quantity = 0
                        """,
                string(coupon.get("title")),
                string(coupon.get("description")),
                string(coupon.get("couponType")),
                decimal(coupon.get("discountAmount")),
                decimal(coupon.get("minimumSpendAmount")),
                integer(coupon.get("totalQuantity")),
                timestamp(coupon.get("startAt")),
                timestamp(coupon.get("endAt")),
                couponId,
                ownerUserId);
    }

    @Override
    public int updateCouponStatus(Long couponId, Long ownerUserId, String status) {
        return jdbcTemplate.update("""
                        UPDATE marketing_coupons
                        SET status = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND owner_user_id = ?
                        """, status, couponId, ownerUserId);
    }

    @Override
    public int reviewCoupon(Long couponId, String reviewStatus, String rejectReason, String reviewNote, Long reviewerId) {
        return jdbcTemplate.update("""
                        UPDATE marketing_coupons
                        SET review_status = ?, reject_reason = ?, review_note = ?, reviewed_by = ?,
                            reviewed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """, reviewStatus, rejectReason, reviewNote, reviewerId, couponId);
    }

    @Override
    public int disableCoupon(Long couponId, Long reviewerId) {
        return jdbcTemplate.update("""
                        UPDATE marketing_coupons
                        SET status = 'disabled', reviewed_by = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """, reviewerId, couponId);
    }

    @Override
    public int incrementClaimedQuantity(Long couponId) {
        return jdbcTemplate.update("""
                        UPDATE marketing_coupons
                        SET claimed_quantity = claimed_quantity + 1, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                          AND review_status = 'approved'
                          AND status = 'active'
                          AND start_at <= CURRENT_TIMESTAMP
                          AND end_at >= CURRENT_TIMESTAMP
                          AND claimed_quantity < total_quantity
                        """, couponId);
    }

    @Override
    public Long insertUserCoupon(Long userId, Long couponId) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO user_coupons (user_id, coupon_id, status, claimed_at)
                    VALUES (?, ?, 'claimed', CURRENT_TIMESTAMP)
                    """, new String[]{"id"});
            ps.setObject(1, userId);
            ps.setObject(2, couponId);
            return ps;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "user coupon id");
    }

    @Override
    public int markUserCouponUsed(Long userCouponId, Long userId, Long orderId) {
        return jdbcTemplate.update("""
                        UPDATE user_coupons
                        SET status = 'used', used_at = CURRENT_TIMESTAMP, order_id = ?
                        WHERE id = ? AND user_id = ? AND status = 'claimed'
                        """, orderId, userCouponId, userId);
    }

    @Override
    public List<Map<String, Object>> findOwnerActivities(Long ownerUserId) {
        return jdbcTemplate.queryForList(activitySql() + " WHERE a.owner_user_id = ? ORDER BY a.created_at DESC, a.id DESC", ownerUserId)
                .stream().map(this::activityMap).toList();
    }

    @Override
    public List<Map<String, Object>> findAdminActivities(String reviewStatus) {
        String sql = activitySql();
        Object[] args = new Object[]{};
        if (reviewStatus != null && !reviewStatus.isBlank()) {
            sql += " WHERE a.review_status = ?";
            args = new Object[]{reviewStatus};
        }
        sql += " ORDER BY a.created_at DESC, a.id DESC";
        return jdbcTemplate.queryForList(sql, args).stream().map(this::activityMap).toList();
    }

    @Override
    public List<Map<String, Object>> findPublicActivities(Long shopId) {
        return jdbcTemplate.queryForList(activitySql() + """
                        WHERE a.shop_id = ?
                          AND a.review_status = 'approved'
                          AND a.status = 'active'
                          AND a.start_at <= CURRENT_TIMESTAMP
                          AND a.end_at >= CURRENT_TIMESTAMP
                        ORDER BY a.start_at DESC, a.id DESC
                        """, shopId)
                .stream().map(this::activityMap).toList();
    }

    @Override
    public Optional<Map<String, Object>> findActivityById(Long activityId) {
        return jdbcTemplate.queryForList(activitySql() + " WHERE a.id = ?", activityId)
                .stream().findFirst().map(this::activityMap);
    }

    @Override
    public Long insertActivity(Map<String, Object> activity) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO shop_activities (
                        shop_id, owner_user_id, title, description, status, review_status,
                        start_at, end_at, updated_at
                    ) VALUES (?, ?, ?, ?, 'active', 'pending_review', ?, ?, CURRENT_TIMESTAMP)
                    """, new String[]{"id"});
            ps.setObject(1, activity.get("shopId"));
            ps.setObject(2, activity.get("ownerUserId"));
            ps.setString(3, string(activity.get("title")));
            ps.setString(4, string(activity.get("description")));
            ps.setTimestamp(5, timestamp(activity.get("startAt")));
            ps.setTimestamp(6, timestamp(activity.get("endAt")));
            return ps;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "shop activity id");
    }

    @Override
    public int updateActivity(Long activityId, Long ownerUserId, Map<String, Object> activity) {
        return jdbcTemplate.update("""
                        UPDATE shop_activities
                        SET title = ?, description = ?, review_status = 'pending_review',
                            reject_reason = NULL, review_note = NULL, reviewed_by = NULL, reviewed_at = NULL,
                            start_at = ?, end_at = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND owner_user_id = ?
                        """,
                string(activity.get("title")),
                string(activity.get("description")),
                timestamp(activity.get("startAt")),
                timestamp(activity.get("endAt")),
                activityId,
                ownerUserId);
    }

    @Override
    public int updateActivityStatus(Long activityId, Long ownerUserId, String status) {
        return jdbcTemplate.update("""
                        UPDATE shop_activities
                        SET status = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND owner_user_id = ?
                        """, status, activityId, ownerUserId);
    }

    @Override
    public int reviewActivity(Long activityId, String reviewStatus, String rejectReason, String reviewNote, Long reviewerId) {
        return jdbcTemplate.update("""
                        UPDATE shop_activities
                        SET review_status = ?, reject_reason = ?, review_note = ?, reviewed_by = ?,
                            reviewed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """, reviewStatus, rejectReason, reviewNote, reviewerId, activityId);
    }

    @Override
    public int disableActivity(Long activityId, Long reviewerId) {
        return jdbcTemplate.update("""
                        UPDATE shop_activities
                        SET status = 'disabled', reviewed_by = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """, reviewerId, activityId);
    }

    private String couponSql() {
        return """
                SELECT c.*, s.name AS shop_name, u.nickname AS owner_name
                FROM marketing_coupons c
                LEFT JOIN shops s ON s.id = c.shop_id
                LEFT JOIN users u ON u.id = c.owner_user_id
                """;
    }

    private String userCouponSql() {
        return """
                SELECT uc.id AS user_coupon_id, uc.user_id, uc.status AS user_coupon_status,
                       uc.claimed_at, uc.used_at, uc.order_id,
                       c.*, s.name AS shop_name
                FROM user_coupons uc
                JOIN marketing_coupons c ON c.id = uc.coupon_id
                LEFT JOIN shops s ON s.id = c.shop_id
                """;
    }

    private String activitySql() {
        return """
                SELECT a.*, s.name AS shop_name, u.nickname AS owner_name
                FROM shop_activities a
                LEFT JOIN shops s ON s.id = a.shop_id
                LEFT JOIN users u ON u.id = a.owner_user_id
                """;
    }

    private Map<String, Object> couponMap(Map<String, Object> row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", first(row, "ID"));
        map.put("shopId", first(row, "SHOP_ID"));
        map.put("shopName", first(row, "SHOP_NAME"));
        map.put("ownerUserId", first(row, "OWNER_USER_ID"));
        map.put("ownerName", first(row, "OWNER_NAME"));
        map.put("title", first(row, "TITLE"));
        map.put("description", first(row, "DESCRIPTION"));
        map.put("couponType", first(row, "COUPON_TYPE"));
        map.put("discountAmount", first(row, "DISCOUNT_AMOUNT"));
        map.put("minimumSpendAmount", first(row, "MINIMUM_SPEND_AMOUNT"));
        map.put("totalQuantity", first(row, "TOTAL_QUANTITY"));
        map.put("claimedQuantity", first(row, "CLAIMED_QUANTITY"));
        map.put("remainingQuantity", remaining(first(row, "TOTAL_QUANTITY"), first(row, "CLAIMED_QUANTITY")));
        map.put("status", first(row, "STATUS"));
        map.put("reviewStatus", first(row, "REVIEW_STATUS"));
        map.put("rejectReason", first(row, "REJECT_REASON"));
        map.put("reviewNote", first(row, "REVIEW_NOTE"));
        map.put("reviewedBy", first(row, "REVIEWED_BY"));
        map.put("reviewedAt", first(row, "REVIEWED_AT"));
        map.put("startAt", first(row, "START_AT"));
        map.put("endAt", first(row, "END_AT"));
        map.put("createdAt", first(row, "CREATED_AT"));
        map.put("updatedAt", first(row, "UPDATED_AT"));
        return map;
    }

    private Map<String, Object> userCouponMap(Map<String, Object> row) {
        Map<String, Object> map = couponMap(row);
        Object userCouponId = first(row, "USER_COUPON_ID");
        map.put("userCouponId", userCouponId);
        map.put("id", userCouponId);
        map.put("couponId", first(row, "ID"));
        map.put("userId", first(row, "USER_ID"));
        map.put("userCouponStatus", first(row, "USER_COUPON_STATUS"));
        map.put("claimedAt", first(row, "CLAIMED_AT"));
        map.put("usedAt", first(row, "USED_AT"));
        map.put("orderId", first(row, "ORDER_ID"));
        return map;
    }

    private Map<String, Object> activityMap(Map<String, Object> row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", first(row, "ID"));
        map.put("shopId", first(row, "SHOP_ID"));
        map.put("shopName", first(row, "SHOP_NAME"));
        map.put("ownerUserId", first(row, "OWNER_USER_ID"));
        map.put("ownerName", first(row, "OWNER_NAME"));
        map.put("title", first(row, "TITLE"));
        map.put("description", first(row, "DESCRIPTION"));
        map.put("status", first(row, "STATUS"));
        map.put("reviewStatus", first(row, "REVIEW_STATUS"));
        map.put("rejectReason", first(row, "REJECT_REASON"));
        map.put("reviewNote", first(row, "REVIEW_NOTE"));
        map.put("reviewedBy", first(row, "REVIEWED_BY"));
        map.put("reviewedAt", first(row, "REVIEWED_AT"));
        map.put("startAt", first(row, "START_AT"));
        map.put("endAt", first(row, "END_AT"));
        map.put("createdAt", first(row, "CREATED_AT"));
        map.put("updatedAt", first(row, "UPDATED_AT"));
        return map;
    }

    private Object first(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? row.get(key.toLowerCase(Locale.ROOT)) : value;
    }

    private int remaining(Object total, Object claimed) {
        return integer(total) - integer(claimed);
    }

    private int integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private Timestamp timestamp(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp;
        }
        if (value instanceof LocalDateTime time) {
            return Timestamp.valueOf(time);
        }
        return Timestamp.valueOf(String.valueOf(value).trim().replace("T", " "));
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
