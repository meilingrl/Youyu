package com.youyu.backend.mapper.shop.impl;

import com.youyu.backend.common.support.JdbcGeneratedKey;
import com.youyu.backend.mapper.shop.ShopMapper;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

@Primary
@Component
public class JdbcShopMapper implements ShopMapper {

    private final JdbcTemplate jdbcTemplate;

    public JdbcShopMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> findAll() {
        return jdbcTemplate.queryForList(baseSql()
                        + " WHERE s.is_deleted = FALSE ORDER BY s.created_at DESC")
                .stream().map(this::toApiMap).toList();
    }

    @Override
    public List<Map<String, Object>> findShopsPaged(String keyword, String status, String reviewStatus, int offset, int limit) {
        StringBuilder sql = new StringBuilder(baseSql()).append(" WHERE s.is_deleted = FALSE");
        List<Object> args = new ArrayList<>();
        appendShopFilters(sql, args, keyword, status, reviewStatus);
        sql.append(" ORDER BY s.created_at DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.queryForList(sql.toString(), args.toArray()).stream()
                .map(this::toApiMap)
                .toList();
    }

    @Override
    public long countShops(String keyword, String status, String reviewStatus) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM shops s WHERE s.is_deleted = FALSE");
        List<Object> args = new ArrayList<>();
        appendShopFilters(sql, args, keyword, status, reviewStatus);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0L : count;
    }

    @Override
    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM shops WHERE is_deleted = FALSE", Long.class);
        return count == null ? 0L : count;
    }

    private void appendShopFilters(StringBuilder sql, List<Object> args, String keyword, String status, String reviewStatus) {
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(COALESCE(s.name, '')) LIKE ? OR LOWER(COALESCE(u.nickname, '')) LIKE ? OR LOWER(COALESCE(s.description, '')) LIKE ?)");
            String like = "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND s.status = ?");
            args.add(status.trim());
        }
        if (reviewStatus != null && !reviewStatus.isBlank()) {
            sql.append(" AND s.review_status = ?");
            args.add(reviewStatus.trim());
        }
    }

    @Override
    public Optional<Map<String, Object>> findById(Long id) {
        return jdbcTemplate.queryForList(baseSql()
                        + " WHERE s.is_deleted = FALSE AND s.id = ?", id)
                .stream().findFirst().map(this::toApiMap);
    }

    @Override
    public Optional<Map<String, Object>> findByOwnerUserId(Long ownerUserId) {
        return jdbcTemplate.queryForList(baseSql()
                        + " WHERE s.is_deleted = FALSE AND s.owner_user_id = ?", ownerUserId)
                .stream().findFirst().map(this::toApiMap);
    }

    @Override
    public Optional<Map<String, Object>> findCapabilityByShopId(Long shopId) {
        return jdbcTemplate.queryForList("""
                SELECT *
                FROM shop_capability_profiles
                WHERE shop_id = ?
                """, shopId).stream().findFirst().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", first(row, "ID"));
            map.put("shopId", first(row, "SHOP_ID"));
            map.put("capabilityLevel", first(row, "CAPABILITY_LEVEL"));
            map.put("maxProductCount", first(row, "MAX_ACTIVE_PRODUCT_COUNT"));
            map.put("canSetNotice", first(row, "CAN_CONFIG_ANNOUNCEMENT"));
            map.put("canSetLoyaltyDiscount", first(row, "CAN_CONFIG_LOYALTY_OFFER"));
            map.put("canUseCoupon", first(row, "CAN_ISSUE_LIGHT_COUPON"));
            map.put("canJoinActivity", first(row, "CAN_JOIN_PLATFORM_ACTIVITY"));
            return map;
        });
    }

    @Override
    public Long insertApplication(Map<String, Object> shop) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO shops (
                          owner_user_id, name, description, cover_url, announcement,
                          status, review_status, updated_at
                        ) VALUES (?, ?, ?, ?, ?, 'inactive', 'pending_review', CURRENT_TIMESTAMP)
                        """, new String[]{"id"});
                ps.setObject(1, shop.get("ownerUserId"));
                ps.setString(2, string(shop.get("name")));
                ps.setString(3, string(shop.get("description")));
                ps.setString(4, string(shop.get("coverUrl")));
                ps.setString(5, string(shop.get("announcement")));
                return ps;
            }, keyHolder);
        } catch (DuplicateKeyException exception) {
            throw exception;
        }
        Long shopId = JdbcGeneratedKey.requiredLong(keyHolder, "shop id");
        jdbcTemplate.update("""
                INSERT INTO shop_capability_profiles (shop_id, capability_level, max_active_product_count)
                VALUES (?, 'basic', 20)
                """, shopId);
        return shopId;
    }

    @Override
    public void updateStatus(Long shopId, String status, String reviewStatus, Long reviewerId, String rejectReason) {
        String resolvedStatus = status == null || status.isBlank() ? null : status;
        String resolvedReviewStatus = reviewStatus == null || reviewStatus.isBlank() ? null : reviewStatus;
        jdbcTemplate.update("""
                UPDATE shops
                SET status = COALESCE(?, status),
                    review_status = COALESCE(?, review_status),
                    reviewed_at = CASE WHEN ? IS NULL THEN reviewed_at ELSE CURRENT_TIMESTAMP END,
                    reviewed_by = CASE WHEN ? IS NULL THEN reviewed_by ELSE ? END,
                    reject_reason = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_deleted = FALSE
                """,
                resolvedStatus,
                resolvedReviewStatus,
                resolvedReviewStatus,
                reviewerId,
                reviewerId,
                rejectReason,
                shopId);
    }

    @Override
    public Map<String, Object> summarizeMonthlyInsight(Long shopId, LocalDateTime startInclusive, LocalDateTime endExclusive) {
        return jdbcTemplate.queryForList(
                        """
                                SELECT
                                  (SELECT COALESCE(SUM(o.pay_amount), 0)
                                   FROM orders o
                                   WHERE o.shop_id = s.id
                                     AND o.order_status = 'completed'
                                     AND o.payment_status = 'paid'
                                     AND o.completed_at >= ?
                                     AND o.completed_at < ?) AS monthly_sales_amount,
                                  (SELECT COUNT(*)
                                   FROM orders o
                                   WHERE o.shop_id = s.id
                                     AND o.order_status = 'completed'
                                     AND o.payment_status = 'paid'
                                     AND o.completed_at >= ?
                                     AND o.completed_at < ?) AS monthly_order_count,
                                  (SELECT COUNT(DISTINCT o.buyer_user_id)
                                   FROM orders o
                                   WHERE o.shop_id = s.id
                                     AND o.order_status = 'completed'
                                     AND o.payment_status = 'paid'
                                     AND (
                                       SELECT COUNT(*)
                                       FROM orders o2
                                       WHERE o2.shop_id = s.id
                                         AND o2.buyer_user_id = o.buyer_user_id
                                         AND o2.order_status = 'completed'
                                         AND o2.payment_status = 'paid'
                                     ) > 1) AS repeat_buyer_count,
                                  (SELECT COALESCE(SUM(p.view_count), 0)
                                   FROM products p
                                   WHERE p.shop_id = s.id AND p.is_deleted = FALSE) AS view_count_summary,
                                  (SELECT COALESCE(SUM(p.favorite_count), 0)
                                   FROM products p
                                   WHERE p.shop_id = s.id AND p.is_deleted = FALSE) AS favorite_count_summary
                                FROM shops s
                                WHERE s.id = ? AND s.is_deleted = FALSE
                                """,
                        Timestamp.valueOf(startInclusive),
                        Timestamp.valueOf(endExclusive),
                        Timestamp.valueOf(startInclusive),
                        Timestamp.valueOf(endExclusive),
                        shopId
                ).stream()
                .findFirst()
                .map(row -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("monthlySalesAmount", first(row, "MONTHLY_SALES_AMOUNT"));
                    result.put("monthlyOrderCount", first(row, "MONTHLY_ORDER_COUNT"));
                    result.put("repeatBuyerCount", first(row, "REPEAT_BUYER_COUNT"));
                    result.put("viewCountSummary", first(row, "VIEW_COUNT_SUMMARY"));
                    result.put("favoriteCountSummary", first(row, "FAVORITE_COUNT_SUMMARY"));
                    return result;
                })
                .orElseGet(() -> Map.of(
                        "monthlySalesAmount", 0,
                        "monthlyOrderCount", 0,
                        "repeatBuyerCount", 0,
                        "viewCountSummary", 0,
                        "favoriteCountSummary", 0
                ));
    }

    @Override
    public List<Map<String, Object>> findHotProducts(Long shopId, LocalDateTime startInclusive, LocalDateTime endExclusive, int limit) {
        return jdbcTemplate.queryForList(
                        """
                                SELECT p.id AS product_id, p.title,
                                       COALESCE(SUM(CASE WHEN o.id IS NOT NULL THEN oi.quantity ELSE 0 END), 0) AS sold_count,
                                       COALESCE(SUM(CASE WHEN o.id IS NOT NULL THEN oi.subtotal_amount ELSE 0 END), 0) AS sales_amount,
                                       p.favorite_count, p.view_count
                                FROM products p
                                LEFT JOIN order_items oi ON oi.product_id = p.id
                                LEFT JOIN orders o ON o.id = oi.order_id
                                    AND o.order_status = 'completed'
                                    AND o.payment_status = 'paid'
                                    AND o.completed_at >= ?
                                    AND o.completed_at < ?
                                WHERE p.shop_id = ?
                                  AND p.is_deleted = FALSE
                                GROUP BY p.id, p.title, p.favorite_count, p.view_count
                                ORDER BY sold_count DESC, p.favorite_count DESC, p.view_count DESC, p.id DESC
                                LIMIT ?
                                """,
                        Timestamp.valueOf(startInclusive),
                        Timestamp.valueOf(endExclusive),
                        shopId,
                        limit
                ).stream()
                .map(row -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("productId", first(row, "PRODUCT_ID"));
                    result.put("title", first(row, "TITLE"));
                    result.put("soldCount", first(row, "SOLD_COUNT"));
                    result.put("salesAmount", first(row, "SALES_AMOUNT"));
                    result.put("favoriteCount", first(row, "FAVORITE_COUNT"));
                    result.put("viewCount", first(row, "VIEW_COUNT"));
                    return result;
                })
                .toList();
    }

    @Override
    public List<Map<String, Object>> summarizeCompletedSalesByCategory(int limit) {
        return jdbcTemplate.queryForList(
                        """
                                SELECT p.category_id, COALESCE(c.name, '未分类') AS category_name,
                                       COALESCE(SUM(oi.subtotal_amount), 0) AS sales_amount,
                                       COALESCE(SUM(oi.quantity), 0) AS sold_count,
                                       COUNT(DISTINCT o.id) AS order_count
                                FROM orders o
                                JOIN order_items oi ON oi.order_id = o.id
                                LEFT JOIN products p ON p.id = oi.product_id
                                LEFT JOIN categories c ON c.id = p.category_id
                                WHERE o.order_status = 'completed'
                                  AND o.payment_status = 'paid'
                                GROUP BY p.category_id, c.name
                                ORDER BY sales_amount DESC, sold_count DESC, category_name
                                LIMIT ?
                                """,
                        limit
                ).stream()
                .map(row -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("categoryId", first(row, "CATEGORY_ID"));
                    result.put("categoryName", first(row, "CATEGORY_NAME"));
                    result.put("salesAmount", first(row, "SALES_AMOUNT"));
                    result.put("soldCount", first(row, "SOLD_COUNT"));
                    result.put("orderCount", first(row, "ORDER_COUNT"));
                    return result;
                })
                .toList();
    }

    @Override
    public List<Map<String, Object>> rankShopsByCompletedSales(int limit) {
        return jdbcTemplate.queryForList(
                        """
                                SELECT s.id AS shop_id, s.name AS shop_name,
                                       COALESCE(SUM(o.pay_amount), 0) AS sales_amount,
                                       COALESCE((
                                         SELECT SUM(oi.quantity)
                                         FROM orders sold_orders
                                         JOIN order_items oi ON oi.order_id = sold_orders.id
                                         WHERE sold_orders.shop_id = s.id
                                           AND sold_orders.order_status = 'completed'
                                           AND sold_orders.payment_status = 'paid'
                                       ), 0) AS sold_count,
                                       COUNT(DISTINCT o.id) AS order_count
                                FROM shops s
                                JOIN orders o ON o.shop_id = s.id
                                WHERE s.is_deleted = FALSE
                                  AND o.order_status = 'completed'
                                  AND o.payment_status = 'paid'
                                GROUP BY s.id, s.name
                                ORDER BY sales_amount DESC, sold_count DESC, s.id
                                LIMIT ?
                                """,
                        limit
                ).stream()
                .map(row -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("shopId", first(row, "SHOP_ID"));
                    result.put("shopName", first(row, "SHOP_NAME"));
                    result.put("salesAmount", first(row, "SALES_AMOUNT"));
                    result.put("soldCount", first(row, "SOLD_COUNT"));
                    result.put("orderCount", first(row, "ORDER_COUNT"));
                    return result;
                })
                .toList();
    }

    private String baseSql() {
        return """
                SELECT s.*, u.nickname AS owner_name
                FROM shops s
                LEFT JOIN users u ON u.id = s.owner_user_id
                """;
    }

    private Map<String, Object> toApiMap(Map<String, Object> row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", first(row, "ID"));
        map.put("ownerUserId", first(row, "OWNER_USER_ID"));
        map.put("ownerId", first(row, "OWNER_USER_ID"));
        map.put("ownerName", first(row, "OWNER_NAME"));
        map.put("name", first(row, "NAME"));
        map.put("description", first(row, "DESCRIPTION"));
        map.put("avatarUrl", first(row, "AVATAR_URL"));
        map.put("coverUrl", first(row, "COVER_URL"));
        map.put("cover", first(row, "COVER_URL"));
        map.put("announcement", first(row, "ANNOUNCEMENT"));
        map.put("notice", first(row, "ANNOUNCEMENT"));
        map.put("status", first(row, "STATUS"));
        map.put("reviewStatus", first(row, "REVIEW_STATUS"));
        map.put("reviewedAt", first(row, "REVIEWED_AT"));
        map.put("reviewedBy", first(row, "REVIEWED_BY"));
        map.put("rejectReason", first(row, "REJECT_REASON"));
        map.put("ratingScore", first(row, "RATING_SCORE"));
        map.put("score", first(row, "RATING_SCORE"));
        map.put("followerCount", first(row, "FOLLOWER_COUNT"));
        map.put("createdAt", first(row, "CREATED_AT"));
        map.put("updatedAt", first(row, "UPDATED_AT"));
        return map;
    }

    private Object first(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? row.get(key.toLowerCase(java.util.Locale.ROOT)) : value;
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

}
