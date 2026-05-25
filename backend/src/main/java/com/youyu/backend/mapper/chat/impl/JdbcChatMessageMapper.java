package com.youyu.backend.mapper.chat.impl;

import com.youyu.backend.common.support.JdbcGeneratedKey;
import com.youyu.backend.mapper.chat.ChatMessageMapper;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

@Primary
@Component
public class JdbcChatMessageMapper implements ChatMessageMapper {

    private final JdbcTemplate jdbcTemplate;

    public JdbcChatMessageMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> findByConversationId(Long conversationId, int offset, int limit) {
        String sql = """
            SELECT id, conversation_id, sender_user_id, body, created_at
            FROM chat_messages
            WHERE conversation_id = ?
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """;
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
    public Long insert(Map<String, Object> message) {
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

    private Map<String, Object> normalizeMessage(Map<String, Object> raw) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("id", raw.get("id"));
        normalized.put("conversationId", raw.get("conversation_id"));
        normalized.put("senderUserId", raw.get("sender_user_id"));
        normalized.put("body", raw.get("body"));
        normalized.put("createdAt", raw.get("created_at"));
        return normalized;
    }
}
