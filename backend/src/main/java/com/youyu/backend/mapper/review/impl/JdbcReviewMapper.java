package com.youyu.backend.mapper.review.impl;

import com.youyu.backend.mapper.review.ReviewMapper;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

@Primary
@Component
public class JdbcReviewMapper implements ReviewMapper {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbcTemplate;

    public JdbcReviewMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── Product reviews ──

    @Override
    public Long insertProductReview(Map<String, Object> reviewData) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                            INSERT INTO reviews (order_item_id, buyer_user_id, product_id, score, content)
                            VALUES (?, ?, ?, ?, ?)
                            """,
                    new String[]{"id"}
            );
            ps.setLong(1, toLong(reviewData.get("orderItemId")));
            ps.setLong(2, toLong(reviewData.get("buyerUserId")));
            ps.setLong(3, toLong(reviewData.get("productId")));
            ps.setInt(4, toInt(reviewData.get("score")));
            ps.setString(5, defaultString(reviewData.get("content")));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    @Override
    public Optional<Map<String, Object>> findProductReviewById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM reviews WHERE id = ?", id);
        return rows.stream().findFirst().map(this::normalizeReview);
    }

    @Override
    public List<Map<String, Object>> findProductReviewsByProductId(Long productId, int offset, int limit) {
        return jdbcTemplate.queryForList(
                """
                        SELECT r.*, u.nickname AS reviewer_nickname, u.avatar AS reviewer_avatar
                        FROM reviews r
                        LEFT JOIN users u ON r.buyer_user_id = u.id
                        WHERE r.product_id = ?
                        ORDER BY r.created_at DESC
                        LIMIT ? OFFSET ?
                        """,
                productId, limit, offset
        ).stream().map(this::normalizeReviewWithUser).toList();
    }

    @Override
    public long countProductReviewsByProductId(Long productId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviews WHERE product_id = ?", Long.class, productId);
        return count == null ? 0 : count;
    }

    @Override
    public List<Map<String, Object>> summarizeProductRatings(Long productId) {
        // COALESCE 确保无评价时 avg_score 返回 0.0 而非 NULL——SQL 的 AVG() 对空结果集返回 NULL。
        return jdbcTemplate.queryForList(
                "SELECT COUNT(*) AS cnt, COALESCE(AVG(score), 0.0) AS avg_score FROM reviews WHERE product_id = ?",
                productId
        ).stream().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("count", toLong(row.get("cnt")));
            map.put("avgScore", toDouble(row.get("avg_score")));
            return map;
        }).toList();
    }

    // ── Shop reviews ──

    @Override
    public Long insertShopReview(Map<String, Object> reviewData) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                            INSERT INTO shop_reviews (shop_id, buyer_user_id, score, content)
                            VALUES (?, ?, ?, ?)
                            """,
                    new String[]{"id"}
            );
            ps.setLong(1, toLong(reviewData.get("shopId")));
            ps.setLong(2, toLong(reviewData.get("buyerUserId")));
            ps.setInt(3, toInt(reviewData.get("score")));
            ps.setString(4, defaultString(reviewData.get("content")));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    @Override
    public Optional<Map<String, Object>> findShopReviewById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM shop_reviews WHERE id = ?", id);
        return rows.stream().findFirst().map(this::normalizeShopReview);
    }

    @Override
    public List<Map<String, Object>> findShopReviewsByShopId(Long shopId, int offset, int limit) {
        return jdbcTemplate.queryForList(
                """
                        SELECT sr.*, u.nickname AS reviewer_nickname, u.avatar AS reviewer_avatar
                        FROM shop_reviews sr
                        LEFT JOIN users u ON sr.buyer_user_id = u.id
                        WHERE sr.shop_id = ?
                        ORDER BY sr.created_at DESC
                        LIMIT ? OFFSET ?
                        """,
                shopId, limit, offset
        ).stream().map(this::normalizeShopReviewWithUser).toList();
    }

    @Override
    public long countShopReviewsByShopId(Long shopId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shop_reviews WHERE shop_id = ?", Long.class, shopId);
        return count == null ? 0 : count;
    }

    @Override
    public List<Map<String, Object>> summarizeShopRatings(Long shopId) {
        // 同上：COALESCE 防止空结果集时 AVG() 返回 NULL。
        return jdbcTemplate.queryForList(
                "SELECT COUNT(*) AS cnt, COALESCE(AVG(score), 0.0) AS avg_score FROM shop_reviews WHERE shop_id = ?",
                shopId
        ).stream().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("count", toLong(row.get("cnt")));
            map.put("avgScore", toDouble(row.get("avg_score")));
            return map;
        }).toList();
    }

    // ── Rating updates ──

    @Override
    public void updateProductRating(Long productId, double avgScore, int count) {
        jdbcTemplate.update(
                "UPDATE products SET rating_score = ?, review_count = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                avgScore, count, productId
        );
    }

    @Override
    public void updateShopRating(Long shopId, double avgScore, int count) {
        jdbcTemplate.update(
                "UPDATE shops SET rating_score = ?, review_count = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                avgScore, count, shopId
        );
    }

    // ── Pending / My reviews ──

    /**
     * 查找买家已完成但尚未评价的订单商品。
     *
     * 使用 LEFT JOIN + WHERE r.id IS NULL 实现反连接（anti-join）：
     * 找出 order_items 中不存在对应 review 的行。
     * 注意：r.buyer_user_id = ? 写在 LEFT JOIN 条件中而非 WHERE 中——
     * 如果放到 WHERE 会使 LEFT JOIN 退化为 INNER JOIN（因为 NULL = ? 永远为 false）。
     */
    @Override
    public List<Map<String, Object>> findPendingReviewableOrderItems(Long buyerUserId) {
        return jdbcTemplate.queryForList(
                """
                        SELECT oi.id, oi.order_id, oi.product_id, oi.title_snapshot,
                               oi.image_snapshot, oi.price_snapshot, oi.quantity,
                               o.shop_id, o.completed_at
                        FROM order_items oi
                        JOIN orders o ON oi.order_id = o.id
                        LEFT JOIN reviews r ON r.order_item_id = oi.id AND r.buyer_user_id = ?
                        WHERE o.buyer_user_id = ?
                          AND o.order_status = 'completed'
                          AND r.id IS NULL
                        ORDER BY o.completed_at DESC
                        """,
                buyerUserId, buyerUserId
        ).stream().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", toLong(row.get("id")));
            map.put("orderId", toLong(row.get("order_id")));
            map.put("productId", toLong(row.get("product_id")));
            map.put("titleSnapshot", defaultString(row.get("title_snapshot")));
            map.put("imageSnapshot", defaultString(row.get("image_snapshot")));
            map.put("priceSnapshot", toDouble(row.get("price_snapshot")));
            map.put("quantity", toInt(row.get("quantity")));
            map.put("shopId", toLong(row.get("shop_id")));
            map.put("completedAt", format(row.get("completed_at")));
            return map;
        }).toList();
    }

    @Override
    public List<Map<String, Object>> findMyProductReviews(Long buyerUserId) {
        // LEFT JOIN（而非 INNER JOIN）：即使 order_item 被删除或成为孤儿行，
        // 用户的评价历史仍然展示（product_title / product_image 为 null），优雅降级。
        return jdbcTemplate.queryForList(
                """
                        SELECT r.*, oi.title_snapshot AS product_title, oi.image_snapshot AS product_image
                        FROM reviews r
                        LEFT JOIN order_items oi ON r.order_item_id = oi.id
                        WHERE r.buyer_user_id = ?
                        ORDER BY r.created_at DESC
                        """,
                buyerUserId
        ).stream().map(row -> {
            Map<String, Object> map = normalizeReview(row);
            map.put("productTitle", defaultString(row.get("product_title")));
            map.put("productImage", defaultString(row.get("product_image")));
            return map;
        }).toList();
    }

    @Override
    public List<Map<String, Object>> findMyShopReviews(Long buyerUserId) {
        return jdbcTemplate.queryForList(
                """
                        SELECT sr.*, s.name AS shop_name, s.avatar_url AS shop_avatar
                        FROM shop_reviews sr
                        LEFT JOIN shops s ON sr.shop_id = s.id
                        WHERE sr.buyer_user_id = ?
                        ORDER BY sr.created_at DESC
                        """,
                buyerUserId
        ).stream().map(row -> {
            Map<String, Object> map = normalizeShopReview(row);
            map.put("shopName", defaultString(row.get("shop_name")));
            map.put("shopAvatar", defaultString(row.get("shop_avatar")));
            return map;
        }).toList();
    }

    @Override
    public Optional<Map<String, Object>> findOrderContextByOrderItemId(Long orderItemId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        SELECT o.buyer_user_id, o.order_status, oi.product_id
                        FROM order_items oi
                        JOIN orders o ON oi.order_id = o.id
                        WHERE oi.id = ?
                        """,
                orderItemId
        );
        return rows.stream().findFirst().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("buyerUserId", toLong(row.get("buyer_user_id")));
            map.put("orderStatus", defaultString(row.get("order_status")));
            map.put("productId", toLong(row.get("product_id")));
            return map;
        });
    }

    // ── Row normalization helpers ──

    private Map<String, Object> normalizeReview(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row.get("id"));
        result.put("orderItemId", toLong(row.get("order_item_id")));
        result.put("buyerUserId", toLong(row.get("buyer_user_id")));
        result.put("productId", toLong(row.get("product_id")));
        result.put("score", toInt(row.get("score")));
        result.put("content", defaultString(row.get("content")));
        result.put("createdAt", format(row.get("created_at")));
        result.put("updatedAt", format(row.get("updated_at")));
        return result;
    }

    private Map<String, Object> normalizeReviewWithUser(Map<String, Object> row) {
        Map<String, Object> result = normalizeReview(row);
        result.put("reviewerNickname", defaultString(row.get("reviewer_nickname")));
        result.put("reviewerAvatar", defaultString(row.get("reviewer_avatar")));
        return result;
    }

    private Map<String, Object> normalizeShopReview(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row.get("id"));
        result.put("shopId", toLong(row.get("shop_id")));
        result.put("buyerUserId", toLong(row.get("buyer_user_id")));
        result.put("score", toInt(row.get("score")));
        result.put("content", defaultString(row.get("content")));
        result.put("createdAt", format(row.get("created_at")));
        result.put("updatedAt", format(row.get("updated_at")));
        return result;
    }

    private Map<String, Object> normalizeShopReviewWithUser(Map<String, Object> row) {
        Map<String, Object> result = normalizeShopReview(row);
        result.put("reviewerNickname", defaultString(row.get("reviewer_nickname")));
        result.put("reviewerAvatar", defaultString(row.get("reviewer_avatar")));
        return result;
    }

    // ── Type conversion helpers ──

    private String format(Object value) {
        if (value == null) return "";
        if (value instanceof Timestamp ts) return ts.toLocalDateTime().format(DATETIME_FORMATTER);
        if (value instanceof LocalDateTime ldt) return ldt.format(DATETIME_FORMATTER);
        return String.valueOf(value);
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value == null) return 0L;
        return Long.parseLong(String.valueOf(value));
    }

    private int toInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        if (value == null) return 0;
        return Integer.parseInt(String.valueOf(value));
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        if (value == null) return 0.0;
        return Double.parseDouble(String.valueOf(value));
    }

    private String defaultString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
