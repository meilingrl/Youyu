package com.youyu.backend.mapper.auth;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public interface LoginFailureCounterMapper {

    Optional<Map<String, Object>> find(String loginIdentifier, String requestSource);

    void recordFailure(String loginIdentifier, String requestSource, LocalDateTime cooldownUntil);

    boolean clear(String loginIdentifier, String requestSource);

    long sumFailuresSinceByRequestSource(String requestSource, LocalDateTime since);
}
