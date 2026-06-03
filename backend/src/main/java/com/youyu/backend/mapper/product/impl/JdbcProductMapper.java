package com.youyu.backend.mapper.product.impl;

import com.youyu.backend.common.support.JdbcGeneratedKey;
import com.youyu.backend.mapper.product.ProductMapper;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

@Primary
@Component
public class JdbcProductMapper implements ProductMapper {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public JdbcProductMapper(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> findAll() {
        return jdbcTemplate.queryForList(baseSql() + " WHERE p.is_deleted = FALSE ORDER BY p.updated_at DESC").stream()
                .map(this::toApiMap)
                .toList();
    }

    @Override
    public List<Map<String, Object>> findProductsPaged(String keyword, String status, String reviewStatus, String productType, int offset, int limit) {
        StringBuilder sql = new StringBuilder(baseSql()).append(" WHERE p.is_deleted = FALSE");
        List<Object> args = new ArrayList<>();
        appendAdminProductFilters(sql, args, keyword, status, reviewStatus, productType);
        sql.append(" ORDER BY p.updated_at DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.queryForList(sql.toString(), args.toArray()).stream()
                .map(this::toApiMap)
                .toList();
    }

    @Override
    public long countProducts(String keyword, String status, String reviewStatus, String productType) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM products p WHERE p.is_deleted = FALSE");
        List<Object> args = new ArrayList<>();
        appendAdminProductFilters(sql, args, keyword, status, reviewStatus, productType);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0L : count;
    }

    @Override
    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products WHERE is_deleted = FALSE", Long.class);
        return count == null ? 0L : count;
    }

    private void appendAdminProductFilters(StringBuilder sql, List<Object> args, String keyword, String status, String reviewStatus, String productType) {
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(COALESCE(p.title, '')) LIKE ? OR LOWER(COALESCE(u.nickname, '')) LIKE ? OR LOWER(COALESCE(c.name, '')) LIKE ?)");
            String like = "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND p.status = ?");
            args.add(status.trim());
        }
        if (reviewStatus != null && !reviewStatus.isBlank()) {
            sql.append(" AND p.review_status = ?");
            args.add(reviewStatus.trim());
        }
        if (productType != null && !productType.isBlank()) {
            sql.append(" AND p.product_type = ?");
            args.add(productType.trim());
        }
    }

    @Override
    public List<Map<String, Object>> findPublic() {
        return jdbcTemplate.queryForList(baseSql()
                        + " WHERE p.is_deleted = FALSE AND p.status = 'on_sale'"
                        + " AND (p.review_status = 'not_required' OR p.review_status = 'approved')"
                        + " ORDER BY p.created_at DESC")
                .stream()
                .map(this::toApiMap)
                .toList();
    }

    @Override
    public List<Map<String, Object>> findPublicByFilters(String keyword, Long categoryId, String productType, String sort) {
        StringBuilder sql = new StringBuilder(baseSql())
                .append(" WHERE p.is_deleted = FALSE AND p.status = 'on_sale'")
                .append(" AND (p.review_status = 'not_required' OR p.review_status = 'approved')");
        List<Object> args = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                     AND (
                         LOWER(COALESCE(p.title, '')) LIKE ?
                      OR LOWER(COALESCE(p.subtitle, '')) LIKE ?
                      OR LOWER(COALESCE(p.description, '')) LIKE ?
                      OR LOWER(COALESCE(c.name, '')) LIKE ?
                     )
                    """);
            String like = "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (categoryId != null) {
            sql.append(" AND p.category_id = ?");
            args.add(categoryId);
        }
        if (productType != null && !productType.isBlank()) {
            sql.append(" AND p.product_type = ?");
            args.add(productType.trim());
        }
        sql.append(orderByForPublicSort(sort));

        return jdbcTemplate.queryForList(sql.toString(), args.toArray()).stream()
                .map(this::toApiMap)
                .toList();
    }

    @Override
    public List<Map<String, Object>> findPublicByFiltersPaged(String keyword, Long categoryId, String productType, String sort, int offset, int limit) {
        StringBuilder sql = new StringBuilder(baseSql())
                .append(" WHERE p.is_deleted = FALSE AND p.status = 'on_sale'")
                .append(" AND (p.review_status = 'not_required' OR p.review_status = 'approved')");
        List<Object> args = new ArrayList<>();
        buildFilterClauses(sql, args, keyword, categoryId, productType);
        sql.append(orderByForPublicSort(sort)).append(" LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);

        return jdbcTemplate.queryForList(sql.toString(), args.toArray()).stream()
                .map(this::toApiMap)
                .toList();
    }

    @Override
    public long countPublicByFilters(String keyword, Long categoryId, String productType) {
        boolean needsCategoryJoin = keyword != null && !keyword.isBlank();
        StringBuilder sql = new StringBuilder(needsCategoryJoin
                ? "SELECT COUNT(*) FROM products p LEFT JOIN categories c ON c.id = p.category_id"
                : "SELECT COUNT(*) FROM products p");
        sql.append(" WHERE p.is_deleted = FALSE AND p.status = 'on_sale'")
           .append(" AND (p.review_status = 'not_required' OR p.review_status = 'approved')");
        List<Object> args = new ArrayList<>();
        buildFilterClauses(sql, args, keyword, categoryId, productType);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0L : count;
    }

    @Override
    public List<Map<String, Object>> findPublicByIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> order = new LinkedHashMap<>();
        for (int i = 0; i < productIds.size(); i++) {
            order.putIfAbsent(productIds.get(i), i);
        }
        Map<String, Object> params = Map.of("ids", order.keySet());
        List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(baseSql()
                + " WHERE p.is_deleted = FALSE AND p.status = 'on_sale'"
                + " AND (p.review_status = 'not_required' OR p.review_status = 'approved')"
                + " AND p.id IN (:ids)", params);
        return rows.stream()
                .map(this::toApiMap)
                .sorted((left, right) -> Integer.compare(
                        order.getOrDefault(toLong(left.get("id")), Integer.MAX_VALUE),
                        order.getOrDefault(toLong(right.get("id")), Integer.MAX_VALUE)))
                .toList();
    }

    @Override
    public List<Map<String, Object>> findPublicSearchIndexDocuments() {
        return jdbcTemplate.queryForList(baseSql()
                        + " WHERE p.is_deleted = FALSE AND p.status = 'on_sale'"
                        + " AND (p.review_status = 'not_required' OR p.review_status = 'approved')"
                        + " ORDER BY p.id")
                .stream()
                .map(this::toSearchIndexMap)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Map<String, Object>> findPublicSearchIndexDocumentById(Long productId) {
        if (productId == null) {
            return Optional.empty();
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(baseSql()
                + " WHERE p.is_deleted = FALSE AND p.status = 'on_sale'"
                + " AND (p.review_status = 'not_required' OR p.review_status = 'approved')"
                + " AND p.id = ?", productId);
        return rows.stream().findFirst().map(this::toSearchIndexMap);
    }

    private void buildFilterClauses(StringBuilder sql, List<Object> args, String keyword, Long categoryId, String productType) {
        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                     AND (
                         LOWER(COALESCE(p.title, '')) LIKE ?
                      OR LOWER(COALESCE(p.subtitle, '')) LIKE ?
                      OR LOWER(COALESCE(p.description, '')) LIKE ?
                      OR LOWER(COALESCE(c.name, '')) LIKE ?
                     )
                    """);
            String like = "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (categoryId != null) {
            sql.append(" AND p.category_id = ?");
            args.add(categoryId);
        }
        if (productType != null && !productType.isBlank()) {
            sql.append(" AND p.product_type = ?");
            args.add(productType.trim());
        }
    }

    private String orderByForPublicSort(String sort) {
        return switch (sort == null ? "newest" : sort) {
            case "price_asc" -> " ORDER BY p.sale_price ASC, p.created_at DESC, p.id DESC";
            case "price_desc" -> " ORDER BY p.sale_price DESC, p.created_at DESC, p.id DESC";
            case "sales_desc" -> " ORDER BY p.favorite_count DESC, p.view_count DESC, p.created_at DESC, p.id DESC";
            case "newest" -> " ORDER BY p.created_at DESC, p.id DESC";
            default -> " ORDER BY p.created_at DESC, p.id DESC";
        };
    }

    @Override
    public List<Map<String, Object>> findBySellerId(Long sellerUserId) {
        return jdbcTemplate.queryForList(baseSql()
                        + " WHERE p.is_deleted = FALSE AND p.seller_user_id = ? ORDER BY p.updated_at DESC",
                sellerUserId).stream().map(this::toApiMap).toList();
    }

    @Override
    public List<Map<String, Object>> findByShopId(Long shopId) {
        return jdbcTemplate.queryForList(baseSql()
                        + " WHERE p.is_deleted = FALSE AND p.shop_id = ? ORDER BY p.updated_at DESC",
                shopId).stream().map(this::toApiMap).toList();
    }

    @Override
    public List<Map<String, Object>> findFavoritesByUserId(Long userId) {
        return jdbcTemplate.queryForList("""
                SELECT p.*, c.name AS category_name, s.name AS shop_name, u.nickname AS seller_name
                FROM product_favorites pf
                INNER JOIN products p ON p.id = pf.product_id
                LEFT JOIN categories c ON c.id = p.category_id
                LEFT JOIN shops s ON s.id = p.shop_id
                LEFT JOIN users u ON u.id = p.seller_user_id
                WHERE pf.user_id = ?
                  AND p.is_deleted = FALSE
                  AND p.status = 'on_sale'
                  AND (p.review_status = 'not_required' OR p.review_status = 'approved')
                ORDER BY pf.created_at DESC, pf.id DESC
                """, userId).stream().map(this::toApiMap).toList();
    }

    @Override
    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(baseSql()
                + " WHERE p.is_deleted = FALSE AND p.id = ?", id);
        return rows.stream().findFirst().map(this::toApiMap);
    }

    @Override
    public boolean isFavorite(Long userId, Long productId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM product_favorites
                WHERE user_id = ? AND product_id = ?
                """, Integer.class, userId, productId);
        return count != null && count > 0;
    }

    @Override
    public List<Map<String, Object>> findMediaByProductId(Long productId) {
        return jdbcTemplate.queryForList("""
                SELECT id, product_id, media_type, media_url, sort_order, created_at
                FROM product_media
                WHERE product_id = ?
                ORDER BY sort_order, id
                """, productId).stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("ID"));
            item.put("productId", row.get("PRODUCT_ID"));
            item.put("mediaType", row.get("MEDIA_TYPE"));
            item.put("url", row.get("MEDIA_URL"));
            item.put("mediaUrl", row.get("MEDIA_URL"));
            item.put("sortOrder", row.get("SORT_ORDER"));
            item.put("createdAt", row.get("CREATED_AT"));
            return item;
        }).toList();
    }

    @Override
    public List<Map<String, Object>> findDigitalAssetsByProductId(Long productId) {
        return jdbcTemplate.queryForList("""
                SELECT id, product_id, asset_type, asset_name, storage_path, is_preview, preview_rule, status, sort_order, created_at
                FROM product_digital_assets
                WHERE product_id = ? AND status = 'active'
                ORDER BY sort_order, id
                """, productId).stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", first(row, "ID"));
            item.put("productId", first(row, "PRODUCT_ID"));
            item.put("assetType", first(row, "ASSET_TYPE"));
            item.put("assetName", first(row, "ASSET_NAME"));
            item.put("assetUrl", first(row, "STORAGE_PATH"));
            item.put("storagePath", first(row, "STORAGE_PATH"));
            item.put("isPreviewAsset", first(row, "IS_PREVIEW"));
            item.put("isFullAsset", !bool(first(row, "IS_PREVIEW")));
            item.put("previewRule", first(row, "PREVIEW_RULE"));
            item.put("previewOrder", first(row, "SORT_ORDER"));
            item.put("status", first(row, "STATUS"));
            item.put("createdAt", first(row, "CREATED_AT"));
            return item;
        }).toList();
    }

    @Override
    public void addFavorite(Long userId, Long productId) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO product_favorites (user_id, product_id, updated_at)
                    VALUES (?, ?, CURRENT_TIMESTAMP)
                    """, userId, productId);
        } catch (DuplicateKeyException ignored) {
            jdbcTemplate.update("""
                    UPDATE product_favorites
                    SET updated_at = CURRENT_TIMESTAMP
                    WHERE user_id = ? AND product_id = ?
                    """, userId, productId);
        }
    }

    @Override
    public boolean removeFavorite(Long userId, Long productId) {
        return jdbcTemplate.update("""
                DELETE FROM product_favorites
                WHERE user_id = ? AND product_id = ?
                """, userId, productId) > 0;
    }

    @Override
    public Long insert(Map<String, Object> product) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO products (
                      seller_user_id, shop_id, category_id, title, subtitle, description,
                      detail_content, product_type, status, review_status, main_image_url,
                      sale_price, original_price, stock_quantity, supports_logistics,
                      supports_offline_delivery, supports_digital_delivery, allow_preview,
                      preview_rule_text, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """, new String[]{"id"});
            ps.setObject(1, product.get("sellerUserId"));
            ps.setObject(2, product.get("shopId"));
            ps.setObject(3, product.get("categoryId"));
            ps.setString(4, string(product.get("title")));
            ps.setString(5, string(product.get("subtitle")));
            ps.setString(6, string(product.get("description")));
            ps.setString(7, string(product.get("description")));
            ps.setString(8, string(product.get("productType")));
            ps.setString(9, string(product.get("status")));
            ps.setString(10, string(product.get("reviewStatus")));
            ps.setString(11, string(product.get("coverUrl")));
            ps.setBigDecimal(12, decimal(product.get("salePrice")));
            ps.setBigDecimal(13, nullableDecimal(product.get("originalPrice")));
            ps.setObject(14, product.get("stock"));
            ps.setBoolean(15, bool(product.get("supportsLogistics")));
            ps.setBoolean(16, bool(product.get("supportsOfflineDelivery")));
            ps.setBoolean(17, bool(product.get("supportsDigitalDelivery")));
            ps.setBoolean(18, bool(product.get("allowPreview")));
            ps.setString(19, string(product.get("previewRuleText")));
            return ps;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "product id");
    }

    @Override
    public void replaceMedia(Long productId, List<String> mediaUrls) {
        jdbcTemplate.update("DELETE FROM product_media WHERE product_id = ?", productId);
        for (int index = 0; index < mediaUrls.size(); index++) {
            jdbcTemplate.update("""
                    INSERT INTO product_media (product_id, media_type, media_url, sort_order)
                    VALUES (?, ?, ?, ?)
                    """, productId, index == 0 ? "cover" : "detail", mediaUrls.get(index), index + 1);
        }
    }

    @Override
    public void update(Long productId, Map<String, Object> product) {
        jdbcTemplate.update("""
                UPDATE products
                SET shop_id = ?, category_id = ?, title = ?, subtitle = ?, description = ?,
                    detail_content = ?, product_type = ?, status = ?, review_status = ?,
                    review_reject_reason = NULL, main_image_url = ?, sale_price = ?,
                    original_price = ?, stock_quantity = ?, supports_logistics = ?,
                    supports_offline_delivery = ?, supports_digital_delivery = ?,
                    allow_preview = ?, preview_rule_text = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND seller_user_id = ? AND is_deleted = FALSE
                """,
                product.get("shopId"),
                product.get("categoryId"),
                string(product.get("title")),
                string(product.get("subtitle")),
                string(product.get("description")),
                string(product.get("description")),
                string(product.get("productType")),
                string(product.get("status")),
                string(product.get("reviewStatus")),
                string(product.get("coverUrl")),
                decimal(product.get("salePrice")),
                nullableDecimal(product.get("originalPrice")),
                product.get("stock"),
                bool(product.get("supportsLogistics")),
                bool(product.get("supportsOfflineDelivery")),
                bool(product.get("supportsDigitalDelivery")),
                bool(product.get("allowPreview")),
                string(product.get("previewRuleText")),
                productId,
                product.get("sellerUserId"));
    }

    @Override
    public void updateStatus(Long productId, String status) {
        jdbcTemplate.update("UPDATE products SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND is_deleted = FALSE",
                status, productId);
    }

    @Override
    public void updateFavoriteCount(Long productId, int delta) {
        jdbcTemplate.update("""
                UPDATE products
                SET favorite_count = CASE
                    WHEN favorite_count + ? < 0 THEN 0
                    ELSE favorite_count + ?
                END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_deleted = FALSE
                """, delta, delta, productId);
    }

    @Override
    public void updateReviewResult(Long productId, String reviewStatus, String status, String rejectReason) {
        jdbcTemplate.update("""
                UPDATE products
                SET review_status = ?, status = ?, review_reject_reason = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_deleted = FALSE
                """, reviewStatus, status, rejectReason, productId);
    }

    @Override
    public void softDelete(Long productId) {
        jdbcTemplate.update("""
                UPDATE products
                SET is_deleted = TRUE, status = 'closed', deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, productId);
    }

    private String baseSql() {
        return """
                SELECT p.*, c.name AS category_name, s.name AS shop_name, u.nickname AS seller_name
                FROM products p
                LEFT JOIN categories c ON c.id = p.category_id
                LEFT JOIN shops s ON s.id = p.shop_id
                LEFT JOIN users u ON u.id = p.seller_user_id
                """;
    }

    private Map<String, Object> toApiMap(Map<String, Object> row) {
        Map<String, Object> map = new LinkedHashMap<>();
        Object cover = first(row, "MAIN_IMAGE_URL");
        map.put("id", first(row, "ID"));
        map.put("sellerUserId", first(row, "SELLER_USER_ID"));
        map.put("sellerId", first(row, "SELLER_USER_ID"));
        map.put("sellerName", first(row, "SELLER_NAME"));
        map.put("shopId", first(row, "SHOP_ID"));
        map.put("shopName", defaultString(first(row, "SHOP_NAME"), "Personal Seller"));
        map.put("categoryId", first(row, "CATEGORY_ID"));
        map.put("categoryName", first(row, "CATEGORY_NAME"));
        map.put("title", first(row, "TITLE"));
        map.put("subtitle", first(row, "SUBTITLE"));
        map.put("description", first(row, "DESCRIPTION"));
        map.put("detail", first(row, "DETAIL_CONTENT"));
        map.put("productType", first(row, "PRODUCT_TYPE"));
        map.put("type", first(row, "PRODUCT_TYPE"));
        map.put("status", first(row, "STATUS"));
        map.put("reviewStatus", first(row, "REVIEW_STATUS"));
        map.put("reviewRejectReason", first(row, "REVIEW_REJECT_REASON"));
        map.put("salePrice", first(row, "SALE_PRICE"));
        map.put("price", first(row, "SALE_PRICE"));
        map.put("originalPrice", first(row, "ORIGINAL_PRICE"));
        map.put("stock", first(row, "STOCK_QUANTITY"));
        map.put("stockQuantity", first(row, "STOCK_QUANTITY"));
        map.put("coverUrl", cover);
        map.put("cover", cover);
        map.put("supportsLogistics", first(row, "SUPPORTS_LOGISTICS"));
        map.put("supportsOfflineDelivery", first(row, "SUPPORTS_OFFLINE_DELIVERY"));
        map.put("supportsDigitalDelivery", first(row, "SUPPORTS_DIGITAL_DELIVERY"));
        map.put("allowPreview", first(row, "ALLOW_PREVIEW"));
        map.put("previewRuleText", first(row, "PREVIEW_RULE_TEXT"));
        map.put("previewHint", first(row, "PREVIEW_RULE_TEXT"));
        map.put("viewCount", first(row, "VIEW_COUNT"));
        map.put("favoriteCount", first(row, "FAVORITE_COUNT"));
        map.put("createdAt", first(row, "CREATED_AT"));
        map.put("publishedAt", first(row, "CREATED_AT"));
        map.put("updatedAt", first(row, "UPDATED_AT"));
        map.put("deliveryMethods", deliveryMethods(map));
        return map;
    }

    private Map<String, Object> toSearchIndexMap(Map<String, Object> row) {
        Map<String, Object> product = toApiMap(row);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("productId", product.get("id"));
        map.put("title", product.get("title"));
        map.put("subtitle", product.get("subtitle"));
        map.put("description", product.get("description"));
        map.put("categoryId", product.get("categoryId"));
        map.put("categoryName", product.get("categoryName"));
        map.put("productType", product.get("productType"));
        map.put("status", product.get("status"));
        map.put("salePrice", product.get("salePrice"));
        map.put("favoriteCount", product.get("favoriteCount"));
        map.put("viewCount", product.get("viewCount"));
        map.put("createdAt", product.get("createdAt"));
        map.put("shopId", product.get("shopId"));
        map.put("shopName", product.get("shopName"));
        return map;
    }

    private List<String> deliveryMethods(Map<String, Object> product) {
        java.util.ArrayList<String> methods = new java.util.ArrayList<>();
        if (bool(product.get("supportsLogistics"))) {
            methods.add("logistics");
        }
        if (bool(product.get("supportsOfflineDelivery"))) {
            methods.add("offline");
        }
        if (bool(product.get("supportsDigitalDelivery"))) {
            methods.add("digital");
        }
        return methods;
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

    private boolean bool(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private BigDecimal decimal(Object value) {
        return nullableDecimal(value) == null ? BigDecimal.ZERO : nullableDecimal(value);
    }

    private BigDecimal nullableDecimal(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

}
