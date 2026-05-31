package com.youyu.backend.mapper.auth;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public interface CaptchaChallengeMapper {

    Long insert(String challengeId,
                String codeHash,
                String requestSource,
                LocalDateTime expiresAt);

    Optional<Map<String, Object>> findActiveByChallengeId(String challengeId,
                                                          LocalDateTime now,
                                                          int maxAttempts);

    int incrementAttemptCount(Long challengeId);

    boolean consume(Long challengeId);

    long countCreatedSinceByRequestSource(String requestSource, LocalDateTime since);
}
