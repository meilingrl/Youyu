package com.youyu.backend.mapper.chat.impl;

import com.youyu.backend.mapper.chat.AutoReplySettingsMapper;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcAutoReplySettingsMapper implements AutoReplySettingsMapper {

    private final JdbcTemplate jdbcTemplate;
    private Boolean tableAvailable;

    public JdbcAutoReplySettingsMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Map<String, Object>> findByUserId(Long userId) {
        if (!tableAvailable()) {
            return Optional.empty();
        }
        String sql = """
            SELECT id, user_id, is_enabled, reply_content, created_at, updated_at
            FROM auto_reply_settings
            WHERE user_id = ?
            """;
        return jdbcTemplate.queryForList(sql, userId).stream()
                .findFirst()
                .map(this::normalize);
    }

    @Override
    public void upsert(Long userId, boolean enabled, String replyContent) {
        String sql = """
            INSERT INTO auto_reply_settings (user_id, is_enabled, reply_content)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE
                is_enabled = VALUES(is_enabled),
                reply_content = VALUES(reply_content),
                updated_at = CURRENT_TIMESTAMP
            """;
        jdbcTemplate.update(sql, userId, enabled, replyContent);
    }

    private Map<String, Object> normalize(Map<String, Object> raw) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("id", first(raw, "id"));
        normalized.put("userId", first(raw, "user_id"));
        normalized.put("isEnabled", first(raw, "is_enabled"));
        normalized.put("replyContent", first(raw, "reply_content"));
        normalized.put("createdAt", first(raw, "created_at"));
        normalized.put("updatedAt", first(raw, "updated_at"));
        return normalized;
    }

    private Object first(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? row.get(key.toUpperCase(java.util.Locale.ROOT)) : value;
    }

    private boolean tableAvailable() {
        if (tableAvailable == null) {
            tableAvailable = Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) (conn) -> {
                try (ResultSet rs = conn.getMetaData().getTables(null, null, "AUTO_REPLY_SETTINGS", null)) {
                    if (rs.next()) {
                        return true;
                    }
                }
                try (ResultSet rs = conn.getMetaData().getTables(null, null, "auto_reply_settings", null)) {
                    return rs.next();
                }
            }));
        }
        return tableAvailable;
    }
}
