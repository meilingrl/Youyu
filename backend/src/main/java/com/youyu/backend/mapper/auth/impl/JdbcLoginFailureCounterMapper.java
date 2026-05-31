package com.youyu.backend.mapper.auth.impl;

import com.youyu.backend.mapper.auth.LoginFailureCounterMapper;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcLoginFailureCounterMapper implements LoginFailureCounterMapper {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLoginFailureCounterMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Map<String, Object>> find(String loginIdentifier, String requestSource) {
        return jdbcTemplate.queryForList("""
                SELECT id, login_identifier, request_source, failure_count, cooldown_until,
                       last_failed_at, created_at, updated_at
                FROM auth_login_failure_counters
                WHERE login_identifier = ? AND request_source = ?
                """, loginIdentifier, requestSource).stream().findFirst().map(this::normalize);
    }

    @Override
    public void recordFailure(String loginIdentifier, String requestSource, LocalDateTime cooldownUntil) {
        jdbcTemplate.update("""
                INSERT INTO auth_login_failure_counters
                (login_identifier, request_source, failure_count, cooldown_until, last_failed_at, updated_at)
                VALUES (?, ?, 1, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                  failure_count = failure_count + 1,
                  cooldown_until = VALUES(cooldown_until),
                  last_failed_at = CURRENT_TIMESTAMP,
                  updated_at = CURRENT_TIMESTAMP
                """, loginIdentifier, requestSource, timestamp(cooldownUntil));
    }

    @Override
    public boolean clear(String loginIdentifier, String requestSource) {
        return jdbcTemplate.update("""
                DELETE FROM auth_login_failure_counters
                WHERE login_identifier = ? AND request_source = ?
                """, loginIdentifier, requestSource) > 0;
    }

    @Override
    public long sumFailuresSinceByRequestSource(String requestSource, LocalDateTime since) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(failure_count), 0)
                FROM auth_login_failure_counters
                WHERE request_source = ? AND last_failed_at >= ?
                """, Long.class, requestSource, Timestamp.valueOf(since));
        return count == null ? 0L : count;
    }

    private Map<String, Object> normalize(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row.get("id"));
        result.put("loginIdentifier", row.get("login_identifier"));
        result.put("requestSource", row.get("request_source"));
        result.put("failureCount", row.get("failure_count"));
        result.put("cooldownUntil", row.get("cooldown_until"));
        result.put("lastFailedAt", row.get("last_failed_at"));
        result.put("createdAt", row.get("created_at"));
        result.put("updatedAt", row.get("updated_at"));
        return result;
    }

    private Timestamp timestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
