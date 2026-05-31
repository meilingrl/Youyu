package com.youyu.backend.mapper.auth;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public interface EmailVerificationChallengeMapper {

    Long insert(String email,
                String purpose,
                String codeHash,
                String requestSource,
                LocalDateTime expiresAt,
                LocalDateTime cooldownUntil);

    Optional<Map<String, Object>> findLatest(String email, String purpose);

    Optional<Map<String, Object>> findLatestActive(String email,
                                                   String purpose,
                                                   LocalDateTime now,
                                                   int maxAttempts);

    int incrementAttemptCount(Long challengeId);

    boolean consume(Long challengeId);

    long countCreatedSinceByEmail(String email, LocalDateTime since);

    long countCreatedSinceByRequestSource(String requestSource, LocalDateTime since);
}
