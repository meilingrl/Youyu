package com.youyu.backend.mapper.chat.impl;

import com.youyu.backend.common.support.JdbcGeneratedKey;
import com.youyu.backend.mapper.chat.ChatConversationMapper;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

@Primary
@Component
public class JdbcChatConversationMapper implements ChatConversationMapper {

    private final JdbcTemplate jdbcTemplate;

    public JdbcChatConversationMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, Object> findById(Long id) {
        String sql = """
            SELECT cc.id, cc.type, cc.product_id, cc.shop_id, cc.user_a_id, cc.user_b_id,
                   cc.last_message_at, cc.created_at,
                   ua.id as user_a_id, ua.username as user_a_username, ua.nickname as user_a_nickname, ua.avatar as user_a_avatar,
                   ub.id as user_b_id, ub.username as user_b_username, ub.nickname as user_b_nickname, ub.avatar as user_b_avatar
            FROM chat_conversations cc
            LEFT JOIN users ua ON cc.user_a_id = ua.id
            LEFT JOIN users ub ON cc.user_b_id = ub.id
            WHERE cc.id = ?
            """;
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, id);
        return results.isEmpty() ? null : normalizeConversation(results.get(0));
    }

    @Override
    public List<Map<String, Object>> findByUserId(Long userId, int offset, int limit) {
        String sql = """
            SELECT cc.id, cc.type, cc.product_id, cc.shop_id, cc.user_a_id, cc.user_b_id,
                   cc.last_message_at, cc.created_at,
                   ua.id as user_a_id, ua.username as user_a_username, ua.nickname as user_a_nickname, ua.avatar as user_a_avatar,
                   ub.id as user_b_id, ub.username as user_b_username, ub.nickname as user_b_nickname, ub.avatar as user_b_avatar
            FROM chat_conversations cc
            LEFT JOIN users ua ON cc.user_a_id = ua.id
            LEFT JOIN users ub ON cc.user_b_id = ub.id
            WHERE cc.user_a_id = ? OR cc.user_b_id = ?
            ORDER BY cc.last_message_at DESC
            LIMIT ? OFFSET ?
            """;
        return jdbcTemplate.queryForList(sql, userId, userId, limit, offset).stream()
                .map(this::normalizeConversation)
                .toList();
    }

    @Override
    public int countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM chat_conversations WHERE user_a_id = ? OR user_b_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, userId, userId);
        return count == null ? 0 : count.intValue();
    }

    @Override
    public Map<String, Object> findByParticipants(Long userAId, Long userBId, Long productId, Long shopId) {
        StringBuilder sql = new StringBuilder("""
            SELECT cc.id, cc.type, cc.product_id, cc.shop_id, cc.user_a_id, cc.user_b_id,
                   cc.last_message_at, cc.created_at,
                   ua.id as user_a_id, ua.username as user_a_username, ua.nickname as user_a_nickname, ua.avatar as user_a_avatar,
                   ub.id as user_b_id, ub.username as user_b_username, ub.nickname as user_b_nickname, ub.avatar as user_b_avatar
            FROM chat_conversations cc
            LEFT JOIN users ua ON cc.user_a_id = ua.id
            LEFT JOIN users ub ON cc.user_b_id = ub.id
            WHERE cc.user_a_id = ? AND cc.user_b_id = ?
            """);

        List<Object> params = new ArrayList<>();
        params.add(userAId);
        params.add(userBId);

        if (productId == null) {
            sql.append(" AND cc.product_id IS NULL");
        } else {
            sql.append(" AND cc.product_id = ?");
            params.add(productId);
        }

        if (shopId == null) {
            sql.append(" AND cc.shop_id IS NULL");
        } else {
            sql.append(" AND cc.shop_id = ?");
            params.add(shopId);
        }

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return results.isEmpty() ? null : normalizeConversation(results.get(0));
    }

    @Override
    public Long insert(Map<String, Object> conversation) {
        String sql = """
            INSERT INTO chat_conversations (type, product_id, shop_id, user_a_id, user_b_id, last_message_at, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, (String) conversation.get("type"));
            ps.setObject(2, conversation.get("productId"));
            ps.setObject(3, conversation.get("shopId"));
            ps.setLong(4, (Long) conversation.get("userAId"));
            ps.setLong(5, (Long) conversation.get("userBId"));
            ps.setTimestamp(6, Timestamp.valueOf((LocalDateTime) conversation.get("lastMessageAt")));
            ps.setTimestamp(7, Timestamp.valueOf((LocalDateTime) conversation.get("createdAt")));
            return ps;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "conversation_id");
    }

    @Override
    public int updateLastMessageAt(Long id, String lastMessageAt) {
        String sql = "UPDATE chat_conversations SET last_message_at = ? WHERE id = ?";
        return jdbcTemplate.update(sql, Timestamp.valueOf(LocalDateTime.parse(lastMessageAt)), id);
    }

    private Map<String, Object> normalizeConversation(Map<String, Object> raw) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("id", raw.get("id"));
        normalized.put("type", raw.get("type"));
        normalized.put("productId", raw.get("product_id"));
        normalized.put("shopId", raw.get("shop_id"));
        normalized.put("userAId", raw.get("user_a_id"));
        normalized.put("userBId", raw.get("user_b_id"));
        normalized.put("lastMessageAt", raw.get("last_message_at"));
        normalized.put("createdAt", raw.get("created_at"));

        // Add user info
        Map<String, Object> userA = new LinkedHashMap<>();
        userA.put("id", raw.get("user_a_id"));
        userA.put("username", raw.get("user_a_username"));
        userA.put("nickname", raw.get("user_a_nickname"));
        userA.put("avatar", raw.get("user_a_avatar"));
        normalized.put("userA", userA);

        Map<String, Object> userB = new LinkedHashMap<>();
        userB.put("id", raw.get("user_b_id"));
        userB.put("username", raw.get("user_b_username"));
        userB.put("nickname", raw.get("user_b_nickname"));
        userB.put("avatar", raw.get("user_b_avatar"));
        normalized.put("userB", userB);

        return normalized;
    }
}
