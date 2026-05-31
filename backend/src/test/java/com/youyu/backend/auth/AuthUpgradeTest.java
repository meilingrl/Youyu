package com.youyu.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.youyu.backend.BackendTestBase;
import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.config.AuthMailProperties;
import com.youyu.backend.service.auth.impl.FakeAuthMailSender;
import com.youyu.backend.service.auth.impl.SmtpAuthMailSender;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:youyu-auth-upgrade-test;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"
})
class AuthUpgradeTest extends BackendTestBase {

    @Autowired
    private FakeAuthMailSender fakeMailSender;

    @BeforeEach
    void clearAuthState() {
        clearCreatedUsers();
        clearChallenges();
        fakeMailSender.clear();
    }

    @AfterEach
    void clearCreatedUsersAfterTest() {
        clearCreatedUsers();
        clearChallenges();
        fakeMailSender.clear();
    }

    private void clearChallenges() {
        jdbcTemplate.update("DELETE FROM auth_email_verification_challenges");
        jdbcTemplate.update("DELETE FROM auth_captcha_challenges");
        jdbcTemplate.update("DELETE FROM auth_login_failure_counters");
    }

    @Test
    void verifiedRegistrationStoresOnlyHashAndReturnsNoSession() throws Exception {
        String email = "auth-register@example.test";
        sendEmailCode(email, "register")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cooldownSeconds").value(60))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(600));

        assertThat(fakeMailSender.deliveries()).hasSize(1);
        assertThat(fakeMailSender.deliveries().get(0).code()).isEqualTo("482913");
        String storedHash = jdbcTemplate.queryForObject(
                "SELECT code_hash FROM auth_email_verification_challenges WHERE email = ?",
                String.class,
                email
        );
        assertThat(storedHash).isNotEqualTo("482913");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("auth_wave_register", email, "482913")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.loginId").value("auth_wave_register"))
                .andExpect(jsonPath("$.data.user.verificationStatus").value("unverified"))
                .andExpect(jsonPath("$.data.token").doesNotExist())
                .andExpect(jsonPath("$.data.role").doesNotExist());

        Boolean consumed = jdbcTemplate.queryForObject(
                "SELECT is_consumed FROM auth_email_verification_challenges WHERE email = ?",
                Boolean.class,
                email
        );
        assertThat(consumed).isTrue();
    }

    @Test
    void emailCodeCooldownAndAttemptLimitAreEnforced() throws Exception {
        String email = "auth-attempts@example.test";
        sendEmailCode(email, "register").andExpect(status().isOk());
        sendEmailCode(email, "register")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));

        for (int index = 0; index < 5; index++) {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registrationJson("auth_wave_attempts", email, "000000")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("auth_wave_attempts", email, "482913")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void expiredEmailCodeCannotBeConsumed() throws Exception {
        String email = "auth-expired@example.test";
        sendEmailCode(email, "register").andExpect(status().isOk());
        jdbcTemplate.update(
                "UPDATE auth_email_verification_challenges SET expires_at = DATEADD('MINUTE', -2, CURRENT_TIMESTAMP) WHERE email = ?",
                email
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("auth_wave_expired", email, "482913")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void resetSendResponseIsEnumerationSafeAndResetCreatesNoSession() throws Exception {
        String existingEmail = "auth-reset@example.test";
        registerAccount("auth_wave_reset", existingEmail);
        jdbcTemplate.update(
                "UPDATE auth_email_verification_challenges SET cooldown_until = DATEADD('MINUTE', -2, CURRENT_TIMESTAMP) WHERE email = ?",
                existingEmail
        );
        fakeMailSender.clear();

        String existingResponse = sendEmailCode(existingEmail, "reset_password")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String missingResponse = sendEmailCode("auth-missing@example.test", "reset_password")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> existingData = JsonPath.read(existingResponse, "$.data");
        Map<String, Object> missingData = JsonPath.read(missingResponse, "$.data");
        assertThat(missingData).isEqualTo(existingData);
        assertThat(fakeMailSender.deliveries()).hasSize(1);
        assertThat(fakeMailSender.deliveries().get(0).recipient()).isEqualTo(existingEmail);

        mockMvc.perform(post("/api/auth/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "emailCode": "482913",
                                  "newPassword": "new-pass123456"
                                }
                                """.formatted(existingEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reset").value(true))
                .andExpect(jsonPath("$.data.token").doesNotExist());

        login("auth_wave_reset", "new-pass123456")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isString());
    }

    @Test
    void captchaIsRequiredAfterThreePasswordFailuresAndRenderedAsPng() throws Exception {
        for (int index = 0; index < 3; index++) {
            login("zhangsan", "wrong-password")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        login("zhangsan", "user123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));

        String captchaResponse = mockMvc.perform(get("/api/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.imageDataUrl").value(org.hamcrest.Matchers.startsWith("data:image/png;base64,")))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(300))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String challengeId = JsonPath.read(captchaResponse, "$.data.challengeId");
        String storedHash = jdbcTemplate.queryForObject(
                "SELECT code_hash FROM auth_captcha_challenges WHERE challenge_id = ?",
                String.class,
                challengeId
        );
        assertThat(storedHash).isNotEqualTo("AB12");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "zhangsan",
                                  "password": "user123",
                                  "captchaChallengeId": "%s",
                                  "captchaCode": "AB12"
                                }
                                """.formatted(challengeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isString());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_login_failure_counters WHERE login_identifier = 'zhangsan'",
                Long.class
        )).isZero();
    }

    @Test
    void emailCodeSourceLimitIsEnforcedWithoutNetworkDelivery() throws Exception {
        for (int index = 0; index < 20; index++) {
            sendEmailCode("auth-source-limit-" + index + "@example.test", "reset_password")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
        sendEmailCode("auth-source-limit-overflow@example.test", "reset_password")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
        assertThat(fakeMailSender.deliveries()).isEmpty();
    }

    @Test
    void smtpSenderReportsMissingConfigurationHonestly() {
        SmtpAuthMailSender sender = new SmtpAuthMailSender(mock(JavaMailSender.class), new AuthMailProperties());

        assertThatThrownBy(() -> sender.sendVerificationCode(
                "synthetic-recipient@example.test",
                "register",
                "482913"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getResultCode())
                .isEqualTo(ResultCode.INTERNAL_SERVER_ERROR);
    }

    private org.springframework.test.web.servlet.ResultActions sendEmailCode(String email, String purpose)
            throws Exception {
        return mockMvc.perform(post("/api/auth/email-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "purpose": "%s"
                        }
                        """.formatted(email, purpose)));
    }

    private org.springframework.test.web.servlet.ResultActions login(String loginId, String password)
            throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "loginId": "%s",
                          "password": "%s"
                        }
                        """.formatted(loginId, password)));
    }

    private void registerAccount(String username, String email) throws Exception {
        sendEmailCode(email, "register").andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(username, email, "482913")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private String registrationJson(String username, String email, String emailCode) {
        return """
                {
                  "username": "%s",
                  "password": "pass123456",
                  "nickname": "Auth Wave Test",
                  "email": "%s",
                  "emailCode": "%s"
                }
                """.formatted(username, email, emailCode);
    }

    private void clearCreatedUsers() {
        jdbcTemplate.update("""
                DELETE FROM user_privilege_profiles
                WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'auth_wave_%')
                """);
        jdbcTemplate.update("DELETE FROM users WHERE username LIKE 'auth_wave_%'");
    }
}
