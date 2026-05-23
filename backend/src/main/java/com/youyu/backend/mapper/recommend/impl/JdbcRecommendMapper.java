package com.youyu.backend.mapper.recommend.impl;

import com.youyu.backend.mapper.recommend.RecommendMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcRecommendMapper implements RecommendMapper {

    private final JdbcTemplate jdbcTemplate;

    public JdbcRecommendMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> findPopularProducts(int limit) {
        String sql = """
                SELECT p.id, p.title, p.subtitle, p.main_image_url, p.sale_price,
                       p.view_count, p.favorite_count,
                       COALESCE(SUM(oi.quantity), 0) AS sold_count,
                       (p.view_count * 0.6 + COALESCE(SUM(oi.quantity), 0) * 10 * 0.4) AS popularity_score,
                       c.name AS category_name, s.name AS shop_name, u.nickname AS seller_name,
                       p.shop_id AS shop_id, p.category_id AS category_id,
                       p.product_type, p.status, p.review_status, p.created_at, p.updated_at
                FROM products p
                LEFT JOIN order_items oi ON oi.product_id = p.id
                LEFT JOIN orders o ON o.id = oi.order_id
                    AND o.order_status = 'completed'
                    AND o.payment_status = 'paid'
                LEFT JOIN categories c ON c.id = p.category_id
                LEFT JOIN shops s ON s.id = p.shop_id
                LEFT JOIN users u ON u.id = p.seller_user_id
                WHERE p.is_deleted = FALSE
                  AND p.status = 'on_sale'
                  AND (p.review_status = 'not_required' OR p.review_status = 'approved')
                GROUP BY p.id, p.title, p.subtitle, p.main_image_url, p.sale_price,
                         p.view_count, p.favorite_count,
                         c.name, s.name, u.nickname,
                         p.shop_id, p.category_id,
                         p.product_type, p.status, p.review_status, p.created_at, p.updated_at
                ORDER BY popularity_score DESC, p.id DESC
                LIMIT ?
                """;
        return jdbcTemplate.queryForList(sql, limit).stream()
                .map(row -> toApiMap(row, "popularity", "热销商品"))
                .toList();
    }

    @Override
    public List<Map<String, Object>> findPopularByCategoryIds(List<Long> categoryIds, int limit) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }
        StringBuilder placeholders = new StringBuilder();
        List<Object> args = new ArrayList<>();
        for (int i = 0; i < categoryIds.size(); i++) {
            if (i > 0) placeholders.append(", ");
            placeholders.append("?");
            args.add(categoryIds.get(i));
        }
        args.add(limit);

        String sql = """
                SELECT p.id, p.title, p.subtitle, p.main_image_url, p.sale_price,
                       p.view_count, p.favorite_count,
                       COALESCE(SUM(oi.quantity), 0) AS sold_count,
                       c.name AS category_name, s.name AS shop_name, u.nickname AS seller_name,
                       p.shop_id AS shop_id, p.category_id AS category_id,
                       p.product_type, p.status, p.review_status, p.created_at, p.updated_at
                FROM products p
                LEFT JOIN order_items oi ON oi.product_id = p.id
                LEFT JOIN orders o ON o.id = oi.order_id
                    AND o.order_status = 'completed'
                    AND o.payment_status = 'paid'
                LEFT JOIN categories c ON c.id = p.category_id
                LEFT JOIN shops s ON s.id = p.shop_id
                LEFT JOIN users u ON u.id = p.seller_user_id
                WHERE p.is_deleted = FALSE
                  AND p.status = 'on_sale'
                  AND (p.review_status = 'not_required' OR p.review_status = 'approved')
                  AND p.category_id IN (%s)
                GROUP BY p.id, p.title, p.subtitle, p.main_image_url, p.sale_price,
                         p.view_count, p.favorite_count,
                         c.name, s.name, u.nickname,
                         p.shop_id, p.category_id,
                         p.product_type, p.status, p.review_status, p.created_at, p.updated_at
                ORDER BY sold_count DESC, p.favorite_count DESC, p.view_count DESC
                LIMIT ?
                """.formatted(placeholders.toString());
        return jdbcTemplate.queryForList(sql, args.toArray()).stream()
                .map(row -> toApiMap(row, "category_preference", null))
                .toList();
    }

    @Override
    public List<Map<String, Object>> findCoPurchased(Long productId, int limit) {
        String sql = """
                SELECT p.id, p.title, p.subtitle, p.main_image_url, p.sale_price,
                       p.view_count, p.favorite_count,
                       COUNT(DISTINCT oi2.order_id) AS co_purchase_count,
                       c.name AS category_name, s.name AS shop_name, u.nickname AS seller_name,
                       p.shop_id AS shop_id, p.category_id AS category_id,
                       p.product_type, p.status, p.review_status, p.created_at, p.updated_at
                FROM order_items oi1
                JOIN orders o ON o.id = oi1.order_id
                    AND o.order_status = 'completed'
                    AND o.payment_status = 'paid'
                JOIN order_items oi2 ON oi2.order_id = oi1.order_id AND oi2.product_id != oi1.product_id
                JOIN products p ON p.id = oi2.product_id
                LEFT JOIN categories c ON c.id = p.category_id
                LEFT JOIN shops s ON s.id = p.shop_id
                LEFT JOIN users u ON u.id = p.seller_user_id
                WHERE oi1.product_id = ?
                  AND p.is_deleted = FALSE
                  AND p.status = 'on_sale'
                  AND (p.review_status = 'not_required' OR p.review_status = 'approved')
                GROUP BY p.id, p.title, p.subtitle, p.main_image_url, p.sale_price,
                         p.view_count, p.favorite_count,
                         c.name, s.name, u.nickname,
                         p.shop_id, p.category_id,
                         p.product_type, p.status, p.review_status, p.created_at, p.updated_at
                ORDER BY co_purchase_count DESC, p.view_count DESC
                LIMIT ?
                """;
        return jdbcTemplate.queryForList(sql, productId, limit).stream()
                .map(row -> toApiMap(row, "also-bought", "购买本商品的用户也买了"))
                .toList();
    }

    private Map<String, Object> toApiMap(Map<String, Object> row, String source, String reason) {
        Map<String, Object> map = new LinkedHashMap<>();
        Object cover = first(row, "MAIN_IMAGE_URL");
        map.put("id", first(row, "ID"));
        map.put("title", first(row, "TITLE"));
        map.put("subtitle", first(row, "SUBTITLE"));
        map.put("categoryId", first(row, "CATEGORY_ID"));
        map.put("categoryName", first(row, "CATEGORY_NAME"));
        map.put("shopId", first(row, "SHOP_ID"));
        map.put("shopName", defaultString(first(row, "SHOP_NAME"), "Personal Seller"));
        map.put("sellerName", first(row, "SELLER_NAME"));
        map.put("salePrice", first(row, "SALE_PRICE"));
        map.put("price", first(row, "SALE_PRICE"));
        map.put("coverUrl", cover);
        map.put("cover", cover);
        map.put("mainImageUrl", cover);
        map.put("productType", first(row, "PRODUCT_TYPE"));
        map.put("type", first(row, "PRODUCT_TYPE"));
        map.put("status", first(row, "STATUS"));
        map.put("reviewStatus", first(row, "REVIEW_STATUS"));
        map.put("viewCount", first(row, "VIEW_COUNT"));
        map.put("favoriteCount", first(row, "FAVORITE_COUNT"));
        map.put("createdAt", first(row, "CREATED_AT"));
        map.put("publishedAt", first(row, "CREATED_AT"));
        map.put("updatedAt", first(row, "UPDATED_AT"));
        map.put("source", source);
        map.put("reason", reason);
        Object score = row.get("POPULARITY_SCORE");
        if (score != null) {
            map.put("score", score);
        }
        Object coCount = row.get("CO_PURCHASE_COUNT");
        if (coCount != null) {
            map.put("coPurchaseCount", coCount);
        }
        return map;
    }

    private Object first(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? row.get(key.toLowerCase(java.util.Locale.ROOT)) : value;
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String defaultString(Object value, String fallback) {
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }
}
