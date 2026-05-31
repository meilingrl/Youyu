package com.youyu.backend.mapper.auth.impl;

import com.youyu.backend.common.support.JdbcGeneratedKey;
import com.youyu.backend.mapper.auth.EmailVerificationChallengeMapper;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

@Component
public class JdbcEmailVerificationChallengeMapper implements EmailVerificationChallengeMapper {

    private final JdbcTemplate jdbcTemplate;

    public JdbcEmailVerificationChallengeMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long insert(String email,
                       String purpose,
                       String codeHash,
                       String requestSource,
                       LocalDateTime expiresAt,
                       LocalDateTime cooldownUntil) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO auth_email_verification_challenges
                            (email, purpose, code_hash, request_source, expires_at, cooldown_until)
                            VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    new String[]{"id"}
            );
            statement.setString(1, email);
            statement.setString(2, purpose);
            statement.setString(3, codeHash);
            statement.setString(4, requestSource);
            statement.setTimestamp(5, Timestamp.valueOf(expiresAt));
            statement.setTimestamp(6, Timestamp.valueOf(cooldownUntil));
            return statement;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "email verification challenge id");
    }

    @Override
    public Optional<Map<String, Object>> findLatest(String email, String purpose) {
        return jdbcTemplate.queryForList("""
                SELECT id, email, purpose, code_hash, request_source, expires_at, cooldown_until,
                       is_consumed, consumed_at, attempt_count, created_at, updated_at
                FROM auth_email_verification_challenges
                WHERE LOWER(email) = LOWER(?) AND purpose = ?
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """, email, purpose).stream().findFirst().map(this::normalize);
    }

    @Override
    public Optional<Map<String, Object>> findLatestActive(String email,
                                                          String purpose,
                                                          LocalDateTime now,
                                                          int maxAttempts) {
        return jdbcTemplate.queryForList("""
                SELECT id, email, purpose, code_hash, request_source, expires_at, cooldown_until,
                       is_consumed, consumed_at, attempt_count, created_at, updated_at
                FROM auth_email_verification_challenges
                WHERE LOWER(email) = LOWER(?)
                  AND purpose = ?
                  AND is_consumed = FALSE
                  AND expires_at > ?
                  AND attempt_count < ?
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """, email, purpose, Timestamp.valueOf(now), maxAttempts)
                .stream().findFirst().map(this::normalize);
    }

    @Override
    public int incrementAttemptCount(Long challengeId) {
        return jdbcTemplate.update("""
                UPDATE auth_email_verification_challenges
                SET attempt_count = attempt_count + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_consumed = FALSE
                """, challengeId);
    }

    @Override
    public boolean consume(Long challengeId) {
        return jdbcTemplate.update("""
                UPDATE auth_email_verification_challenges
                SET is_consumed = TRUE, consumed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_consumed = FALSE
                """, challengeId) > 0;
    }

    @Override
    public long countCreatedSinceByEmail(String email, LocalDateTime since) {
        return count("""
                SELECT COUNT(*)
                FROM auth_email_verification_challenges
                WHERE LOWER(email) = LOWER(?) AND created_at >= ?
                """, email, Timestamp.valueOf(since));
    }

    @Override
    public long countCreatedSinceByRequestSource(String requestSource, LocalDateTime since) {
        return count("""
                SELECT COUNT(*)
                FROM auth_email_verification_challenges
                WHERE request_source = ? AND created_at >= ?
                """, requestSource, Timestamp.valueOf(since));
    }

    private long count(String sql, Object... args) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
        return count == null ? 0L : count;
    }

    private Map<String, Object> normalize(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row.get("id"));
        result.put("email", row.get("email"));
        result.put("purpose", row.get("purpose"));
        result.put("codeHash", row.get("code_hash"));
        result.put("requestSource", row.get("request_source"));
        result.put("expiresAt", row.get("expires_at"));
        result.put("cooldownUntil", row.get("cooldown_until"));
        result.put("isConsumed", booleanValue(row.get("is_consumed")));
        result.put("consumedAt", row.get("consumed_at"));
        result.put("attemptCount", row.get("attempt_count"));
        result.put("createdAt", row.get("created_at"));
        result.put("updatedAt", row.get("updated_at"));
        return result;
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
