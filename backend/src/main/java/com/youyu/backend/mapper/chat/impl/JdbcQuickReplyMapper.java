package com.youyu.backend.mapper.chat.impl;

import com.youyu.backend.common.support.JdbcGeneratedKey;
import com.youyu.backend.mapper.chat.QuickReplyMapper;
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
public class JdbcQuickReplyMapper implements QuickReplyMapper {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public JdbcQuickReplyMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> findByUserId(Long userId) {
        return jdbcTemplate.queryForList(
                """
                        SELECT id, user_id, content, sort_order, created_at, updated_at
                        FROM quick_replies
                        WHERE user_id = ?
                        ORDER BY sort_order ASC, created_at ASC
                        """,
                userId
        ).stream().map(this::normalize).toList();
    }

    @Override
    public Optional<Map<String, Object>> findById(Long id) {
        return jdbcTemplate.queryForList(
                """
                        SELECT id, user_id, content, sort_order, created_at, updated_at
                        FROM quick_replies
                        WHERE id = ?
                        """,
                id
        ).stream().findFirst().map(this::normalize);
    }

    @Override
    public Long insert(Map<String, Object> quickReply) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO quick_replies (user_id, content, sort_order, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?)
                            """,
                    new String[]{"id"}
            );
            statement.setLong(1, toLong(quickReply.get("userId")));
            statement.setString(2, string(quickReply.get("content")));
            statement.setInt(3, toInt(quickReply.get("sortOrder")));
            statement.setTimestamp(4, Timestamp.valueOf(now));
            statement.setTimestamp(5, Timestamp.valueOf(now));
            return statement;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "quick reply id");
    }

    @Override
    public boolean update(Long id, Long userId, String content, int sortOrder) {
        return jdbcTemplate.update(
                """
                        UPDATE quick_replies
                        SET content = ?, sort_order = ?, updated_at = ?
                        WHERE id = ? AND user_id = ?
                        """,
                content, sortOrder, Timestamp.valueOf(LocalDateTime.now()), id, userId
        ) > 0;
    }

    @Override
    public boolean delete(Long id, Long userId) {
        return jdbcTemplate.update(
                "DELETE FROM quick_replies WHERE id = ? AND user_id = ?",
                id,
                userId
        ) > 0;
    }

    private Map<String, Object> normalize(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", toLong(row.get("id")));
        result.put("userId", toLong(row.get("user_id")));
        result.put("content", string(row.get("content")));
        result.put("sortOrder", toInt(row.get("sort_order")));
        result.put("createdAt", format(row.get("created_at")));
        result.put("updatedAt", format(row.get("updated_at")));
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

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
