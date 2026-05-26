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
        String sql = """
            SELECT id, conversation_id, sender_user_id, body,
                   %s
                   %s
                   created_at
            FROM chat_messages
            WHERE conversation_id = ?
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """.formatted(typeSelect, readSelect);
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

    private Map<String, Object> normalizeMessage(Map<String, Object> raw) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("id", raw.get("id"));
        normalized.put("conversationId", raw.get("conversation_id"));
        normalized.put("senderUserId", raw.get("sender_user_id"));
        normalized.put("body", raw.get("body"));
        normalized.put("messageType", raw.get("message_type"));
        normalized.put("mediaUrl", raw.get("media_url"));
        normalized.put("isRead", raw.get("is_read"));
        normalized.put("readAt", raw.get("read_at"));
        normalized.put("createdAt", raw.get("created_at"));
        return normalized;
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
