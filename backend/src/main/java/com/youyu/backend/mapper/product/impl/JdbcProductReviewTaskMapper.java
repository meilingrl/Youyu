package com.youyu.backend.mapper.product.impl;

import com.youyu.backend.common.support.JdbcGeneratedKey;
import com.youyu.backend.mapper.product.ProductReviewTaskMapper;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

@Primary
@Component
public class JdbcProductReviewTaskMapper implements ProductReviewTaskMapper {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProductReviewTaskMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> findAll() {
        return jdbcTemplate.queryForList(baseSql() + " ORDER BY t.submitted_at DESC").stream()
                .map(this::toApiMap)
                .toList();
    }

    @Override
    public List<Map<String, Object>> findReviewTasksPaged(String keyword, String status, int offset, int limit) {
        StringBuilder sql = new StringBuilder(baseSql()).append(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendReviewTaskFilters(sql, args, keyword, status);
        sql.append(" ORDER BY t.submitted_at DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.queryForList(sql.toString(), args.toArray()).stream()
                .map(this::toApiMap)
                .toList();
    }

    @Override
    public long countReviewTasks(String keyword, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM product_review_tasks t JOIN products p ON p.id = t.product_id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendReviewTaskFilters(sql, args, keyword, status);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0L : count;
    }

    @Override
    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product_review_tasks", Long.class);
        return count == null ? 0L : count;
    }

    private void appendReviewTaskFilters(StringBuilder sql, List<Object> args, String keyword, String status) {
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(COALESCE(p.title, '')) LIKE ? OR LOWER(COALESCE(u.nickname, '')) LIKE ?)");
            String like = "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            args.add(like);
            args.add(like);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND t.review_status = ?");
            args.add(status.trim());
        }
    }

    @Override
    public Optional<Map<String, Object>> findById(Long id) {
        return jdbcTemplate.queryForList(baseSql() + " WHERE t.id = ?", id).stream()
                .findFirst()
                .map(this::toApiMap);
    }

    @Override
    public Long insertPending(Long productId) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO product_review_tasks (product_id, review_type, review_status)
                    VALUES (?, 'digital_product', 'pending_review')
                    """, new String[]{"id"});
            ps.setLong(1, productId);
            return ps;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "product review task id");
    }

    @Override
    public void updateReviewResult(Long id, String reviewStatus, Long reviewerId, String rejectReason, String reviewNote) {
        jdbcTemplate.update("""
                UPDATE product_review_tasks
                SET review_status = ?, reviewed_at = CURRENT_TIMESTAMP, reviewed_by = ?,
                    reject_reason = ?, review_note = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, reviewStatus, reviewerId, rejectReason, reviewNote, id);
    }

    private String baseSql() {
        return """
                SELECT t.*, p.title AS product_title, p.seller_user_id, p.product_type,
                       p.status AS product_status, u.nickname AS seller_name
                FROM product_review_tasks t
                JOIN products p ON p.id = t.product_id
                LEFT JOIN users u ON u.id = p.seller_user_id
                """;
    }

    private Map<String, Object> toApiMap(Map<String, Object> row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", first(row, "ID"));
        map.put("productId", first(row, "PRODUCT_ID"));
        map.put("productTitle", first(row, "PRODUCT_TITLE"));
        map.put("sellerUserId", first(row, "SELLER_USER_ID"));
        map.put("sellerName", first(row, "SELLER_NAME"));
        map.put("productType", first(row, "PRODUCT_TYPE"));
        map.put("productStatus", first(row, "PRODUCT_STATUS"));
        map.put("reviewType", first(row, "REVIEW_TYPE"));
        map.put("reviewStatus", first(row, "REVIEW_STATUS"));
        map.put("submittedAt", first(row, "SUBMITTED_AT"));
        map.put("reviewedAt", first(row, "REVIEWED_AT"));
        map.put("reviewedBy", first(row, "REVIEWED_BY"));
        map.put("rejectReason", first(row, "REJECT_REASON"));
        map.put("reviewNote", first(row, "REVIEW_NOTE"));
        return map;
    }

    private Object first(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? row.get(key.toLowerCase(java.util.Locale.ROOT)) : value;
    }

}
