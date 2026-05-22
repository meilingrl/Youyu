package com.youyu.backend.mapper.order.impl;

import com.youyu.backend.mapper.order.DigitalAccessMapper;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

@Primary
@Component
public class JdbcDigitalAccessMapper implements DigitalAccessMapper {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbcTemplate;

    public JdbcDigitalAccessMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long insert(Long orderId, Long userId, Long assetId, String assetName, String accessType) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                            INSERT INTO digital_access_logs (
                                order_id, user_id, asset_id, asset_name, access_type
                            ) VALUES (?, ?, ?, ?, ?)
                            """,
                    new String[]{"id"}
            );
            ps.setLong(1, orderId);
            ps.setLong(2, userId);
            ps.setLong(3, assetId);
            ps.setString(4, assetName);
            ps.setString(5, accessType);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    @Override
    public List<Map<String, Object>> findByOrderId(Long orderId) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM digital_access_logs WHERE order_id = ? ORDER BY accessed_at DESC",
                orderId
        ).stream().map(this::toApiMap).toList();
    }

    private Map<String, Object> toApiMap(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", toLong(row.get("ID")));
        result.put("orderId", toLong(row.get("ORDER_ID")));
        result.put("userId", toLong(row.get("USER_ID")));
        result.put("assetId", toLong(row.get("ASSET_ID")));
        result.put("assetName", defaultString(row.get("ASSET_NAME")));
        result.put("accessType", defaultString(row.get("ACCESS_TYPE")));
        result.put("accessedAt", format(row.get("ACCESSED_AT")));
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
        return Long.parseLong(String.valueOf(value));
    }

    private String defaultString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
