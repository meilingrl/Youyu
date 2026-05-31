package com.youyu.backend.mapper.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.youyu.backend.BackendTestBase;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuthChallengeMapperTest extends BackendTestBase {

    @Autowired
    private EmailVerificationChallengeMapper emailChallengeMapper;

    @Autowired
    private CaptchaChallengeMapper captchaChallengeMapper;

    @Autowired
    private LoginFailureCounterMapper loginFailureCounterMapper;

    @BeforeEach
    void clearChallenges() {
        jdbcTemplate.update("DELETE FROM auth_email_verification_challenges");
        jdbcTemplate.update("DELETE FROM auth_captcha_challenges");
        jdbcTemplate.update("DELETE FROM auth_login_failure_counters");
    }

    @Test
    void emailChallengeStoresHashAndSupportsSingleConsumption() {
        LocalDateTime now = LocalDateTime.now();
        Long challengeId = emailChallengeMapper.insert(
                "auth-wave1@example.test",
                "register",
                "sha256:email-code-hash",
                "test-source",
                now.plusMinutes(10),
                now.plusMinutes(1)
        );

        Map<String, Object> challenge = emailChallengeMapper
                .findLatestActive("AUTH-WAVE1@EXAMPLE.TEST", "register", now, 5)
                .orElseThrow();

        assertThat(challenge.get("id")).isEqualTo(challengeId);
        assertThat(challenge.get("codeHash")).isEqualTo("sha256:email-code-hash");
        assertThat(challenge).doesNotContainKey("code");
        assertThat(emailChallengeMapper.incrementAttemptCount(challengeId)).isEqualTo(1);
        assertThat(emailChallengeMapper.consume(challengeId)).isTrue();
        assertThat(emailChallengeMapper.consume(challengeId)).isFalse();
        assertThat(emailChallengeMapper.findLatestActive("auth-wave1@example.test", "register", now, 5)).isEmpty();
    }

    @Test
    void captchaChallengeStoresHashAndStopsReturningConsumedChallenge() {
        LocalDateTime now = LocalDateTime.now();
        Long id = captchaChallengeMapper.insert(
                "opaque-captcha-id",
                "sha256:captcha-code-hash",
                "test-source",
                now.plusMinutes(5)
        );

        Map<String, Object> challenge = captchaChallengeMapper
                .findActiveByChallengeId("opaque-captcha-id", now, 5)
                .orElseThrow();

        assertThat(challenge.get("codeHash")).isEqualTo("sha256:captcha-code-hash");
        assertThat(challenge).doesNotContainKey("code");
        assertThat(captchaChallengeMapper.incrementAttemptCount(id)).isEqualTo(1);
        assertThat(captchaChallengeMapper.consume(id)).isTrue();
        assertThat(captchaChallengeMapper.findActiveByChallengeId("opaque-captcha-id", now, 5)).isEmpty();
    }

    @Test
    void loginFailureCounterIncrementsAndClearsByIdentifierAndSource() {
        loginFailureCounterMapper.recordFailure("sample-user", "test-source", null);
        loginFailureCounterMapper.recordFailure("sample-user", "test-source", LocalDateTime.now().plusMinutes(1));

        Map<String, Object> counter = loginFailureCounterMapper.find("sample-user", "test-source").orElseThrow();

        assertThat(((Number) counter.get("failureCount")).intValue()).isEqualTo(2);
        assertThat(loginFailureCounterMapper.sumFailuresSinceByRequestSource(
                "test-source", LocalDateTime.now().minusMinutes(1))).isEqualTo(2);
        assertThat(loginFailureCounterMapper.clear("sample-user", "test-source")).isTrue();
        assertThat(loginFailureCounterMapper.find("sample-user", "test-source")).isEmpty();
    }
}
