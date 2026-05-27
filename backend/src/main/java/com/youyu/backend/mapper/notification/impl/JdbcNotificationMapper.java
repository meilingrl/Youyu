package com.youyu.backend.mapper.notification.impl;

import com.youyu.backend.common.support.JdbcGeneratedKey;
import com.youyu.backend.mapper.notification.NotificationMapper;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

@Component
public class JdbcNotificationMapper implements NotificationMapper {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public JdbcNotificationMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long insert(Map<String, Object> notification) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime createdAt = LocalDateTime.now();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO notifications (user_id, type, title, body, action_url, is_read, created_at)
                            VALUES (?, ?, ?, ?, ?, FALSE, ?)
                            """,
                    new String[]{"id"}
            );
            statement.setLong(1, toLong(notification.get("userId")));
            statement.setString(2, string(notification.get("type")));
            statement.setString(3, string(notification.get("title")));
            statement.setString(4, string(notification.get("body")));
            statement.setString(5, string(notification.get("actionUrl")));
            statement.setTimestamp(6, Timestamp.valueOf(createdAt));
            return statement;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "notification id");
    }

    @Override
    public List<Map<String, Object>> findByUserId(Long userId, int offset, int limit) {
        return jdbcTemplate.queryForList(
                """
                        SELECT id, user_id, type, title, body, action_url, is_read, created_at
                        FROM notifications
                        WHERE user_id = ?
                        ORDER BY created_at DESC, id DESC
                        LIMIT ? OFFSET ?
                        """,
                userId, limit, offset
        ).stream().map(this::normalize).toList();
    }

    @Override
    public long countByUserId(Long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE user_id = ?",
                Long.class,
                userId
        );
        return count == null ? 0L : count;
    }

    @Override
    public long countUnreadByUserId(Long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE",
                Long.class,
                userId
        );
        return count == null ? 0L : count;
    }

    @Override
    public Optional<Map<String, Object>> findById(Long id) {
        return jdbcTemplate.queryForList(
                """
                        SELECT id, user_id, type, title, body, action_url, is_read, created_at
                        FROM notifications
                        WHERE id = ?
                        """,
                id
        ).stream().findFirst().map(this::normalize);
    }

    @Override
    public boolean markRead(Long id, Long userId) {
        return jdbcTemplate.update(
                "UPDATE notifications SET is_read = TRUE WHERE id = ? AND user_id = ?",
                id,
                userId
        ) > 0;
    }

    @Override
    public int markAllRead(Long userId) {
        return jdbcTemplate.update(
                "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE",
                userId
        );
    }

    private Map<String, Object> normalize(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", toLong(row.get("id")));
        result.put("userId", toLong(row.get("user_id")));
        result.put("type", string(row.get("type")));
        result.put("title", string(row.get("title")));
        result.put("body", string(row.get("body")));
        result.put("actionUrl", string(row.get("action_url")));
        result.put("isRead", toBoolean(row.get("is_read")));
        result.put("createdAt", format(row.get("created_at")));
        return result;
    }

    private String format(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().format(DATETIME_FORMATTER);
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.format(DATETIME_FORMATTER);
        }
        return String.valueOf(value);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return 0L;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
