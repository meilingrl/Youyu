package com.youyu.backend.mapper.audit.impl;

import com.youyu.backend.mapper.audit.AdminAuditLogMapper;
import com.youyu.backend.mapper.common.MapperTypeConverters;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Primary
@Component
public class JdbcAdminAuditLogMapper implements AdminAuditLogMapper {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public JdbcAdminAuditLogMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insert(Long operatorUserId,
                       String operatorRole,
                       String action,
                       String targetType,
                       Long targetId,
                       String summary) {
        jdbcTemplate.update(
                """
                        INSERT INTO admin_audit_logs
                        (operator_user_id, operator_role, action, target_type, target_id, summary, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                        """,
                operatorUserId,
                operatorRole,
                action,
                targetType,
                targetId,
                summary
        );
    }

    @Override
    public List<Map<String, Object>> findPaged(String action, String targetType, int offset, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, operator_user_id, operator_role, action, target_type, target_id, summary, created_at
                FROM admin_audit_logs
                WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, action, targetType);
        sql.append(" ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.queryForList(sql.toString(), args.toArray()).stream()
                .map(this::normalize)
                .toList();
    }

    @Override
    public long count(String action, String targetType) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM admin_audit_logs WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, action, targetType);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0L : count;
    }

    private void appendFilters(StringBuilder sql, List<Object> args, String action, String targetType) {
        if (action != null && !action.isBlank()) {
            sql.append(" AND action = ?");
            args.add(action.trim());
        }
        if (targetType != null && !targetType.isBlank()) {
            sql.append(" AND target_type = ?");
            args.add(targetType.trim());
        }
    }

    private Map<String, Object> normalize(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", first(row, "id", "ID"));
        result.put("operatorUserId", first(row, "operator_user_id", "OPERATOR_USER_ID"));
        result.put("operatorRole", first(row, "operator_role", "OPERATOR_ROLE"));
        result.put("action", first(row, "action", "ACTION"));
        result.put("targetType", first(row, "target_type", "TARGET_TYPE"));
        result.put("targetId", first(row, "target_id", "TARGET_ID"));
        result.put("summary", first(row, "summary", "SUMMARY"));
        result.put("createdAt", format(first(row, "created_at", "CREATED_AT")));
        return result;
    }

    private Object first(Map<String, Object> row, String camelKey, String upperKey) {
        return MapperTypeConverters.first(row, camelKey, upperKey);
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
}
