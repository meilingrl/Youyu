package com.youyu.backend.mapper.auth.impl;

import com.youyu.backend.common.support.JdbcGeneratedKey;
import com.youyu.backend.mapper.auth.CaptchaChallengeMapper;
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
public class JdbcCaptchaChallengeMapper implements CaptchaChallengeMapper {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCaptchaChallengeMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long insert(String challengeId,
                       String codeHash,
                       String requestSource,
                       LocalDateTime expiresAt) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO auth_captcha_challenges
                            (challenge_id, code_hash, request_source, expires_at)
                            VALUES (?, ?, ?, ?)
                            """,
                    new String[]{"id"}
            );
            statement.setString(1, challengeId);
            statement.setString(2, codeHash);
            statement.setString(3, requestSource);
            statement.setTimestamp(4, Timestamp.valueOf(expiresAt));
            return statement;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "captcha challenge id");
    }

    @Override
    public Optional<Map<String, Object>> findActiveByChallengeId(String challengeId,
                                                                 LocalDateTime now,
                                                                 int maxAttempts) {
        return jdbcTemplate.queryForList("""
                SELECT id, challenge_id, code_hash, request_source, expires_at, is_consumed,
                       consumed_at, attempt_count, created_at, updated_at
                FROM auth_captcha_challenges
                WHERE challenge_id = ?
                  AND is_consumed = FALSE
                  AND expires_at > ?
                  AND attempt_count < ?
                """, challengeId, Timestamp.valueOf(now), maxAttempts)
                .stream().findFirst().map(this::normalize);
    }

    @Override
    public int incrementAttemptCount(Long challengeId) {
        return jdbcTemplate.update("""
                UPDATE auth_captcha_challenges
                SET attempt_count = attempt_count + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_consumed = FALSE
                """, challengeId);
    }

    @Override
    public boolean consume(Long challengeId) {
        return jdbcTemplate.update("""
                UPDATE auth_captcha_challenges
                SET is_consumed = TRUE, consumed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_consumed = FALSE
                """, challengeId) > 0;
    }

    @Override
    public long countCreatedSinceByRequestSource(String requestSource, LocalDateTime since) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM auth_captcha_challenges
                WHERE request_source = ? AND created_at >= ?
                """, Long.class, requestSource, Timestamp.valueOf(since));
        return count == null ? 0L : count;
    }

    private Map<String, Object> normalize(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row.get("id"));
        result.put("challengeId", row.get("challenge_id"));
        result.put("codeHash", row.get("code_hash"));
        result.put("requestSource", row.get("request_source"));
        result.put("expiresAt", row.get("expires_at"));
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
