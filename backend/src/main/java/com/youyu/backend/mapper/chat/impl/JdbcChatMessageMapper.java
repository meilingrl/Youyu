package com.youyu.backend.mapper.chat.impl;

import com.youyu.backend.common.support.JdbcGeneratedKey;
import com.youyu.backend.mapper.chat.ChatMessageMapper;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

@Primary
@Component
public class JdbcChatMessageMapper implements ChatMessageMapper {

    private final JdbcTemplate jdbcTemplate;
    private Boolean messageTypeColumnsAvailable;
    private Boolean readStatusColumnsAvailable;
    private Boolean cardReferenceColumnsAvailable;
    private Boolean recallColumnsAvailable;

    public JdbcChatMessageMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> findByConversationId(Long conversationId, int offset, int limit) {
        String typeSelect = messageTypeColumnsAvailable()
                ? "message_type, media_url,"
                : "'text' as message_type, NULL as media_url,";
        String readSelect = readStatusColumnsAvailable()
                ? "is_read, read_at,"
                : "FALSE as is_read, NULL as read_at,";
        String recallSelect = recallColumnsAvailable()
                ? "is_recalled, recalled_at,"
                : "FALSE as is_recalled, NULL as recalled_at,";
        if (cardReferenceColumnsAvailable()) {
            return findByConversationIdWithCardRefs(conversationId, offset, limit, typeSelect, readSelect, recallSelect);
        }
        String sql = """
            SELECT id, conversation_id, sender_user_id, body,
                   %s
                   %s
                   %s
                   created_at
            FROM chat_messages
            WHERE conversation_id = ?
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """.formatted(typeSelect, readSelect, recallSelect);
        return jdbcTemplate.queryForList(sql, conversationId, limit, offset).stream()
                .map(this::normalizeMessage)
                .toList();
    }

    @Override
    public int countByConversationId(Long conversationId) {
        String sql = "SELECT COUNT(*) FROM chat_messages WHERE conversation_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, conversationId);
        return count == null ? 0 : count.intValue();
    }

    @Override
    public Optional<Map<String, Object>> findById(Long messageId) {
        String typeSelect = messageTypeColumnsAvailable()
                ? "m.message_type, m.media_url,"
                : "'text' as message_type, NULL as media_url,";
        String readSelect = readStatusColumnsAvailable()
                ? "m.is_read, m.read_at,"
                : "FALSE as is_read, NULL as read_at,";
        String recallSelect = recallColumnsAvailable()
                ? "m.is_recalled, m.recalled_at,"
                : "FALSE as is_recalled, NULL as recalled_at,";
        String refSelect = cardReferenceColumnsAvailable()
                ? "m.product_id, m.order_id,"
                : "NULL as product_id, NULL as order_id,";
        String sql = """
            SELECT m.id, m.conversation_id, m.sender_user_id, m.body,
                   %s
                   %s
                   %s
                   %s
                   m.created_at
            FROM chat_messages m
            WHERE m.id = ?
            """.formatted(typeSelect, readSelect, recallSelect, refSelect);
        return jdbcTemplate.queryForList(sql, messageId).stream()
                .findFirst()
                .map(this::normalizeMessage);
    }

    @Override
    public List<Map<String, Object>> searchByUser(
            Long userId, String keyword, LocalDateTime startTime, LocalDateTime endTime, int offset, int limit) {
        String typeSelect = messageTypeColumnsAvailable()
                ? "m.message_type, m.media_url,"
                : "'text' as message_type, NULL as media_url,";
        String readSelect = readStatusColumnsAvailable()
                ? "m.is_read, m.read_at,"
                : "FALSE as is_read, NULL as read_at,";
        String recallSelect = recallColumnsAvailable()
                ? "m.is_recalled, m.recalled_at,"
                : "FALSE as is_recalled, NULL as recalled_at,";
        String refSelect = cardReferenceColumnsAvailable()
                ? "m.product_id, m.order_id,"
                : "NULL as product_id, NULL as order_id,";

        StringBuilder sql = new StringBuilder("""
            SELECT m.id, m.conversation_id, m.sender_user_id, m.body,
                   %s
                   %s
                   %s
                   %s
                   m.created_at
            FROM chat_messages m
            INNER JOIN chat_conversations c ON c.id = m.conversation_id
            WHERE ((c.user_a_id = ? AND c.deleted_by_a_at IS NULL)
               OR (c.user_b_id = ? AND c.deleted_by_b_at IS NULL))
            """.formatted(typeSelect, readSelect, recallSelect, refSelect));
        java.util.ArrayList<Object> params = new java.util.ArrayList<>();
        params.add(userId);
        params.add(userId);
        appendSearchFilters(sql, params, keyword, startTime, endTime);
        sql.append(" ORDER BY m.created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.queryForList(sql.toString(), params.toArray()).stream()
                .map(this::normalizeMessage)
                .toList();
    }

    @Override
    public int countSearchByUser(Long userId, String keyword, LocalDateTime startTime, LocalDateTime endTime) {
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*)
            FROM chat_messages m
            INNER JOIN chat_conversations c ON c.id = m.conversation_id
            WHERE ((c.user_a_id = ? AND c.deleted_by_a_at IS NULL)
               OR (c.user_b_id = ? AND c.deleted_by_b_at IS NULL))
            """);
        java.util.ArrayList<Object> params = new java.util.ArrayList<>();
        params.add(userId);
        params.add(userId);
        appendSearchFilters(sql, params, keyword, startTime, endTime);

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count == null ? 0 : count.intValue();
    }

    @Override
    public Long insert(Map<String, Object> message) {
        if (messageTypeColumnsAvailable() && readStatusColumnsAvailable() && cardReferenceColumnsAvailable()) {
            return insertWithMessageMetadataAndRefs(message);
        }
        if (messageTypeColumnsAvailable() && readStatusColumnsAvailable()) {
            return insertWithMessageMetadata(message);
        }
        if (messageTypeColumnsAvailable()) {
            return insertWithMessageType(message);
        }
        String sql = """
            INSERT INTO chat_messages (conversation_id, sender_user_id, body, created_at)
            VALUES (?, ?, ?, ?)
            """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setLong(1, (Long) message.get("conversationId"));
            ps.setLong(2, (Long) message.get("senderUserId"));
            ps.setString(3, (String) message.get("body"));
            ps.setTimestamp(4, Timestamp.valueOf((LocalDateTime) message.get("createdAt")));
            return ps;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "message_id");
    }

    @Override
    public int recall(Long messageId, LocalDateTime recalledAt) {
        if (!recallColumnsAvailable()) {
            return 0;
        }
        String sql = """
            UPDATE chat_messages
            SET is_recalled = TRUE, recalled_at = ?
            WHERE id = ? AND is_recalled = FALSE
            """;
        return jdbcTemplate.update(sql, Timestamp.valueOf(recalledAt), messageId);
    }

    @Override
    public Optional<Map<String, Object>> findProductCardSummary(Long productId) {
        if (productId == null) {
            return Optional.empty();
        }
        String sql = """
            SELECT id, title, sale_price, status, review_status, main_image_url
            FROM products
            WHERE id = ? AND is_deleted = FALSE
            """;
        return jdbcTemplate.queryForList(sql, productId).stream()
                .findFirst()
                .map(this::normalizeProduct);
    }

    @Override
    public Optional<Map<String, Object>> findOrderCardSummary(Long orderId) {
        if (orderId == null) {
            return Optional.empty();
        }
        String sql = """
            SELECT o.id, o.order_no, o.order_status, o.pay_amount, o.submitted_at,
                   o.buyer_user_id, o.seller_user_id,
                   oi.title_snapshot AS product_title, oi.image_snapshot AS product_image
            FROM orders o
            LEFT JOIN (
                SELECT order_id, MIN(id) AS first_item_id
                FROM order_items
                GROUP BY order_id
            ) first_oi ON first_oi.order_id = o.id
            LEFT JOIN order_items oi ON oi.id = first_oi.first_item_id
            WHERE o.id = ?
            """;
        return jdbcTemplate.queryForList(sql, orderId).stream()
                .findFirst()
                .map(this::normalizeOrder);
    }

    @Override
    public int markMessagesRead(Long conversationId, Long readerUserId, LocalDateTime readAt) {
        if (!readStatusColumnsAvailable()) {
            return 0;
        }
        String sql = """
            UPDATE chat_messages
            SET is_read = TRUE, read_at = ?
            WHERE conversation_id = ?
              AND sender_user_id <> ?
              AND is_read = FALSE
            """;
        return jdbcTemplate.update(sql, Timestamp.valueOf(readAt), conversationId, readerUserId);
    }

    private List<Map<String, Object>> findByConversationIdWithCardRefs(
            Long conversationId, int offset, int limit, String typeSelect, String readSelect, String recallSelect) {
        String sql = """
            SELECT m.id, m.conversation_id, m.sender_user_id, m.body,
                   %s
                   %s
                   %s
                   m.product_id, m.order_id, m.created_at,
                   p.id AS product_ref_id, p.title AS product_title, p.sale_price AS product_price,
                   p.status AS product_status, p.main_image_url AS product_image_url,
                   o.id AS order_ref_id, o.order_no, o.order_status, o.pay_amount AS order_total_amount,
                   o.submitted_at AS order_created_at,
                   oi.title_snapshot AS order_product_title, oi.image_snapshot AS order_product_image
            FROM chat_messages m
            LEFT JOIN products p ON p.id = m.product_id AND p.is_deleted = FALSE
            LEFT JOIN orders o ON o.id = m.order_id
            LEFT JOIN (
                SELECT order_id, MIN(id) AS first_item_id
                FROM order_items
                GROUP BY order_id
            ) first_oi ON first_oi.order_id = o.id
            LEFT JOIN order_items oi ON oi.id = first_oi.first_item_id
            WHERE m.conversation_id = ?
            ORDER BY m.created_at DESC
            LIMIT ? OFFSET ?
            """.formatted(prefixColumns("m.", typeSelect), prefixColumns("m.", readSelect), prefixColumns("m.", recallSelect));
        return jdbcTemplate.queryForList(sql, conversationId, limit, offset).stream()
                .map(this::normalizeMessage)
                .toList();
    }

    private Map<String, Object> normalizeMessage(Map<String, Object> raw) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("id", raw.get("id"));
        normalized.put("conversationId", raw.get("conversation_id"));
        normalized.put("senderUserId", raw.get("sender_user_id"));
        normalized.put("body", raw.get("body"));
        normalized.put("messageType", raw.get("message_type"));
        normalized.put("mediaUrl", raw.get("media_url"));
        normalized.put("productId", raw.get("product_id"));
        normalized.put("orderId", raw.get("order_id"));
        normalized.put("product", normalizeProductFromMessage(raw));
        normalized.put("order", normalizeOrderFromMessage(raw));
        normalized.put("isRead", raw.get("is_read"));
        normalized.put("readAt", raw.get("read_at"));
        normalized.put("isRecalled", raw.get("is_recalled"));
        normalized.put("recalledAt", raw.get("recalled_at"));
        normalized.put("createdAt", raw.get("created_at"));
        return normalized;
    }

    private void appendSearchFilters(
            StringBuilder sql, java.util.List<Object> params, String keyword, LocalDateTime startTime, LocalDateTime endTime) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND LOWER(m.body) LIKE ?");
            params.add("%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%");
        }
        if (startTime != null) {
            sql.append(" AND m.created_at >= ?");
            params.add(Timestamp.valueOf(startTime));
        }
        if (endTime != null) {
            sql.append(" AND m.created_at <= ?");
            params.add(Timestamp.valueOf(endTime));
        }
    }

    private Long insertWithMessageMetadata(Map<String, Object> message) {
        String sql = """
            INSERT INTO chat_messages (conversation_id, sender_user_id, body, message_type, media_url, is_read, read_at, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setLong(1, (Long) message.get("conversationId"));
            ps.setLong(2, (Long) message.get("senderUserId"));
            ps.setString(3, (String) message.get("body"));
            ps.setString(4, (String) message.get("messageType"));
            ps.setString(5, (String) message.get("mediaUrl"));
            ps.setBoolean(6, Boolean.TRUE.equals(message.get("isRead")));
            Object readAt = message.get("readAt");
            ps.setTimestamp(7, readAt == null ? null : Timestamp.valueOf((LocalDateTime) readAt));
            ps.setTimestamp(8, Timestamp.valueOf((LocalDateTime) message.get("createdAt")));
            return ps;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "message_id");
    }

    private Long insertWithMessageMetadataAndRefs(Map<String, Object> message) {
        String sql = """
            INSERT INTO chat_messages (
                conversation_id, sender_user_id, body, message_type, media_url,
                product_id, order_id, is_read, read_at, created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setLong(1, (Long) message.get("conversationId"));
            ps.setLong(2, (Long) message.get("senderUserId"));
            ps.setString(3, (String) message.get("body"));
            ps.setString(4, (String) message.get("messageType"));
            ps.setString(5, (String) message.get("mediaUrl"));
            setNullableLong(ps, 6, message.get("productId"));
            setNullableLong(ps, 7, message.get("orderId"));
            ps.setBoolean(8, Boolean.TRUE.equals(message.get("isRead")));
            Object readAt = message.get("readAt");
            ps.setTimestamp(9, readAt == null ? null : Timestamp.valueOf((LocalDateTime) readAt));
            ps.setTimestamp(10, Timestamp.valueOf((LocalDateTime) message.get("createdAt")));
            return ps;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "message_id");
    }

    private Long insertWithMessageType(Map<String, Object> message) {
        String sql = """
            INSERT INTO chat_messages (conversation_id, sender_user_id, body, message_type, media_url, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setLong(1, (Long) message.get("conversationId"));
            ps.setLong(2, (Long) message.get("senderUserId"));
            ps.setString(3, (String) message.get("body"));
            ps.setString(4, (String) message.get("messageType"));
            ps.setString(5, (String) message.get("mediaUrl"));
            ps.setTimestamp(6, Timestamp.valueOf((LocalDateTime) message.get("createdAt")));
            return ps;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "message_id");
    }

    private void setNullableLong(PreparedStatement ps, int index, Object value) throws java.sql.SQLException {
        if (value == null) {
            ps.setObject(index, null);
        } else {
            ps.setLong(index, ((Number) value).longValue());
        }
    }

    private String prefixColumns(String prefix, String selectFragment) {
        return selectFragment
                .replace("message_type", prefix + "message_type")
                .replace("media_url", prefix + "media_url")
                .replace("is_read", prefix + "is_read")
                .replace("read_at", prefix + "read_at")
                .replace("is_recalled", prefix + "is_recalled")
                .replace("recalled_at", prefix + "recalled_at");
    }

    private Map<String, Object> normalizeProduct(Map<String, Object> raw) {
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("id", first(raw, "id"));
        product.put("title", first(raw, "title"));
        product.put("price", first(raw, "sale_price"));
        product.put("status", first(raw, "status"));
        product.put("reviewStatus", first(raw, "review_status"));
        product.put("imageUrl", first(raw, "main_image_url"));
        return product;
    }

    private Map<String, Object> normalizeOrder(Map<String, Object> raw) {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("id", first(raw, "id"));
        order.put("orderNumber", first(raw, "order_no"));
        order.put("status", first(raw, "order_status"));
        order.put("totalAmount", first(raw, "pay_amount"));
        order.put("productTitle", first(raw, "product_title"));
        order.put("productImage", first(raw, "product_image"));
        order.put("createdAt", first(raw, "submitted_at"));
        order.put("buyerUserId", first(raw, "buyer_user_id"));
        order.put("sellerUserId", first(raw, "seller_user_id"));
        return order;
    }

    private Map<String, Object> normalizeProductFromMessage(Map<String, Object> raw) {
        if (first(raw, "product_ref_id") == null) {
            return null;
        }
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("id", first(raw, "product_ref_id"));
        product.put("title", first(raw, "product_title"));
        product.put("price", first(raw, "product_price"));
        product.put("status", first(raw, "product_status"));
        product.put("imageUrl", first(raw, "product_image_url"));
        return product;
    }

    private Map<String, Object> normalizeOrderFromMessage(Map<String, Object> raw) {
        if (first(raw, "order_ref_id") == null) {
            return null;
        }
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("id", first(raw, "order_ref_id"));
        order.put("orderNumber", first(raw, "order_no"));
        order.put("status", first(raw, "order_status"));
        order.put("totalAmount", first(raw, "order_total_amount"));
        order.put("productTitle", first(raw, "order_product_title"));
        order.put("productImage", first(raw, "order_product_image"));
        order.put("createdAt", first(raw, "order_created_at"));
        return order;
    }

    private Object first(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? row.get(key.toUpperCase(java.util.Locale.ROOT)) : value;
    }

    private boolean messageTypeColumnsAvailable() {
        if (messageTypeColumnsAvailable == null) {
            messageTypeColumnsAvailable = columnExists("chat_messages", "message_type")
                    && columnExists("chat_messages", "media_url");
        }
        return messageTypeColumnsAvailable;
    }

    private boolean readStatusColumnsAvailable() {
        if (readStatusColumnsAvailable == null) {
            readStatusColumnsAvailable = columnExists("chat_messages", "is_read")
                    && columnExists("chat_messages", "read_at");
        }
        return readStatusColumnsAvailable;
    }

    private boolean cardReferenceColumnsAvailable() {
        if (cardReferenceColumnsAvailable == null) {
            cardReferenceColumnsAvailable = columnExists("chat_messages", "product_id")
                    && columnExists("chat_messages", "order_id");
        }
        return cardReferenceColumnsAvailable;
    }

    private boolean recallColumnsAvailable() {
        if (recallColumnsAvailable == null) {
            recallColumnsAvailable = columnExists("chat_messages", "is_recalled")
                    && columnExists("chat_messages", "recalled_at");
        }
        return recallColumnsAvailable;
    }

    private boolean columnExists(String tableName, String columnName) {
        return Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) (conn) -> {
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
                if (rs.next()) {
                    return true;
                }
            }
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, columnName)) {
                return rs.next();
            }
        }));
    }
}
