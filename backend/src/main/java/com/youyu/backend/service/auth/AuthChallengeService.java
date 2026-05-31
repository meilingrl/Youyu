package com.youyu.backend.service.auth;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.mapper.auth.CaptchaChallengeMapper;
import com.youyu.backend.mapper.auth.EmailVerificationChallengeMapper;
import com.youyu.backend.mapper.auth.LoginFailureCounterMapper;
import com.youyu.backend.mapper.user.UserMapper;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthChallengeService {

    public static final String PURPOSE_REGISTER = "register";
    public static final String PURPOSE_RESET_PASSWORD = "reset_password";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int EMAIL_CODE_EXPIRY_SECONDS = 600;
    private static final int EMAIL_CODE_COOLDOWN_SECONDS = 60;
    private static final int CAPTCHA_EXPIRY_SECONDS = 300;
    private static final int MAX_CHALLENGE_ATTEMPTS = 5;
    private static final int CAPTCHA_THRESHOLD = 3;
    private static final int MAX_EMAIL_SENDS_PER_EMAIL_HOUR = 5;
    private static final int MAX_EMAIL_SENDS_PER_SOURCE_HOUR = 20;
    private static final int MAX_CAPTCHA_CREATIONS_PER_SOURCE_MINUTE = 20;
    private static final int MAX_LOGIN_FAILURES_PER_SOURCE_TEN_MINUTES = 50;
    private static final int LOGIN_COOLDOWN_THRESHOLD = 10;
    private static final int LOGIN_COOLDOWN_SECONDS = 60;

    private final EmailVerificationChallengeMapper emailChallengeMapper;
    private final CaptchaChallengeMapper captchaChallengeMapper;
    private final LoginFailureCounterMapper loginFailureCounterMapper;
    private final UserMapper userMapper;
    private final AuthMailSender mailSender;
    private final ChallengeCodeGenerator codeGenerator;
    private final ChallengeCodeHasher codeHasher;
    private final CaptchaImageRenderer captchaImageRenderer;
    private final AuthChallengeAttemptService challengeAttemptService;

    public AuthChallengeService(EmailVerificationChallengeMapper emailChallengeMapper,
                                CaptchaChallengeMapper captchaChallengeMapper,
                                LoginFailureCounterMapper loginFailureCounterMapper,
                                UserMapper userMapper,
                                AuthMailSender mailSender,
                                ChallengeCodeGenerator codeGenerator,
                                ChallengeCodeHasher codeHasher,
                                CaptchaImageRenderer captchaImageRenderer,
                                AuthChallengeAttemptService challengeAttemptService) {
        this.emailChallengeMapper = emailChallengeMapper;
        this.captchaChallengeMapper = captchaChallengeMapper;
        this.loginFailureCounterMapper = loginFailureCounterMapper;
        this.userMapper = userMapper;
        this.mailSender = mailSender;
        this.codeGenerator = codeGenerator;
        this.codeHasher = codeHasher;
        this.captchaImageRenderer = captchaImageRenderer;
        this.challengeAttemptService = challengeAttemptService;
    }

    @Transactional
    public Map<String, Object> sendEmailCode(String email, String purpose, String requestSource) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedPurpose = normalizePurpose(purpose);
        String source = normalizeSource(requestSource);
        LocalDateTime now = LocalDateTime.now();
        assertEmailSendAllowed(normalizedEmail, source, now);

        boolean accountExists = userMapper.findByEmail(normalizedEmail).isPresent();
        if (PURPOSE_REGISTER.equals(normalizedPurpose) && accountExists) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Email is already in use");
        }

        mailSender.validateConfiguration();
        String code = codeGenerator.emailCode();
        emailChallengeMapper.insert(
                normalizedEmail,
                normalizedPurpose,
                codeHasher.hash(code),
                source,
                now.plusSeconds(EMAIL_CODE_EXPIRY_SECONDS),
                now.plusSeconds(EMAIL_CODE_COOLDOWN_SECONDS)
        );
        if (PURPOSE_REGISTER.equals(normalizedPurpose) || accountExists) {
            mailSender.sendVerificationCode(normalizedEmail, normalizedPurpose, code);
        }
        return linkedMap(
                "cooldownSeconds", EMAIL_CODE_COOLDOWN_SECONDS,
                "expiresInSeconds", EMAIL_CODE_EXPIRY_SECONDS
        );
    }

    public Map<String, Object> createCaptcha(String requestSource) {
        String source = normalizeSource(requestSource);
        LocalDateTime now = LocalDateTime.now();
        if (captchaChallengeMapper.countCreatedSinceByRequestSource(source, now.minusMinutes(1))
                >= MAX_CAPTCHA_CREATIONS_PER_SOURCE_MINUTE) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Too many CAPTCHA requests. Try again later.");
        }

        String challengeId = UUID.randomUUID().toString();
        String code = codeGenerator.captchaCode();
        captchaChallengeMapper.insert(
                challengeId,
                codeHasher.hash(code),
                source,
                now.plusSeconds(CAPTCHA_EXPIRY_SECONDS)
        );
        return linkedMap(
                "challengeId", challengeId,
                "imageDataUrl", captchaImageRenderer.renderDataUrl(code),
                "expiresInSeconds", CAPTCHA_EXPIRY_SECONDS
        );
    }

    public void verifyAndConsumeEmailCode(String email, String purpose, String code) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedPurpose = normalizePurpose(purpose);
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> latest = emailChallengeMapper.findLatest(normalizedEmail, normalizedPurpose)
                .orElseThrow(this::invalidEmailCode);
        Map<String, Object> active = emailChallengeMapper
                .findLatestActive(normalizedEmail, normalizedPurpose, now, MAX_CHALLENGE_ATTEMPTS)
                .orElseThrow(this::invalidEmailCode);
        if (!Objects.equals(toLong(latest.get("id")), toLong(active.get("id")))) {
            throw invalidEmailCode();
        }

        Long challengeId = toLong(active.get("id"));
        if (!codeHasher.matches(trim(code), Objects.toString(active.get("codeHash"), ""))) {
            challengeAttemptService.recordEmailCodeFailure(challengeId);
            throw invalidEmailCode();
        }
        if (!emailChallengeMapper.consume(challengeId)) {
            throw invalidEmailCode();
        }
    }

    public void validateCaptchaIfRequired(String loginIdentifier,
                                          String requestSource,
                                          String captchaChallengeId,
                                          String captchaCode) {
        String identifier = normalizeLoginIdentifier(loginIdentifier);
        String source = normalizeSource(requestSource);
        assertPasswordLoginAllowed(identifier, source);
        int failureCount = loginFailureCounterMapper.find(identifier, source)
                .map(counter -> toInt(counter.get("failureCount")))
                .orElse(0);
        if (failureCount < CAPTCHA_THRESHOLD) {
            return;
        }
        if (isBlank(captchaChallengeId) || isBlank(captchaCode)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "CAPTCHA is required");
        }

        Map<String, Object> challenge = captchaChallengeMapper
                .findActiveByChallengeId(trim(captchaChallengeId), LocalDateTime.now(), MAX_CHALLENGE_ATTEMPTS)
                .orElseThrow(this::invalidCaptcha);
        Long challengeId = toLong(challenge.get("id"));
        if (!source.equals(Objects.toString(challenge.get("requestSource"), ""))
                || !codeHasher.matches(trim(captchaCode).toUpperCase(Locale.ROOT),
                Objects.toString(challenge.get("codeHash"), ""))) {
            challengeAttemptService.recordCaptchaFailure(challengeId);
            throw invalidCaptcha();
        }
        if (!captchaChallengeMapper.consume(challengeId)) {
            throw invalidCaptcha();
        }
    }

    public void recordLoginFailure(String loginIdentifier, String requestSource) {
        String identifier = normalizeLoginIdentifier(loginIdentifier);
        String source = normalizeSource(requestSource);
        int currentFailures = loginFailureCounterMapper.find(identifier, source)
                .map(counter -> toInt(counter.get("failureCount")))
                .orElse(0);
        LocalDateTime cooldownUntil = currentFailures + 1 >= LOGIN_COOLDOWN_THRESHOLD
                ? LocalDateTime.now().plusSeconds(LOGIN_COOLDOWN_SECONDS)
                : null;
        loginFailureCounterMapper.recordFailure(identifier, source, cooldownUntil);
    }

    public void clearLoginFailures(String loginIdentifier, String requestSource) {
        loginFailureCounterMapper.clear(normalizeLoginIdentifier(loginIdentifier), normalizeSource(requestSource));
    }

    private void assertEmailSendAllowed(String email, String source, LocalDateTime now) {
        emailChallengeMapper.findLatest(email, PURPOSE_REGISTER)
                .filter(challenge -> isFuture(challenge.get("cooldownUntil"), now))
                .ifPresent(challenge -> {
                    throw new BusinessException(ResultCode.BUSINESS_ERROR, "Please wait before requesting another code");
                });
        emailChallengeMapper.findLatest(email, PURPOSE_RESET_PASSWORD)
                .filter(challenge -> isFuture(challenge.get("cooldownUntil"), now))
                .ifPresent(challenge -> {
                    throw new BusinessException(ResultCode.BUSINESS_ERROR, "Please wait before requesting another code");
                });
        if (emailChallengeMapper.countCreatedSinceByEmail(email, now.minusHours(1))
                >= MAX_EMAIL_SENDS_PER_EMAIL_HOUR
                || emailChallengeMapper.countCreatedSinceByRequestSource(source, now.minusHours(1))
                >= MAX_EMAIL_SENDS_PER_SOURCE_HOUR) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Too many email-code requests. Try again later.");
        }
    }

    private void assertPasswordLoginAllowed(String identifier, String source) {
        LocalDateTime now = LocalDateTime.now();
        if (loginFailureCounterMapper.sumFailuresSinceByRequestSource(source, now.minusMinutes(10))
                >= MAX_LOGIN_FAILURES_PER_SOURCE_TEN_MINUTES) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Too many login failures. Try again later.");
        }
        loginFailureCounterMapper.find(identifier, source)
                .filter(counter -> isFuture(counter.get("cooldownUntil"), now))
                .ifPresent(counter -> {
                    throw new BusinessException(ResultCode.BUSINESS_ERROR, "Too many login failures. Try again later.");
                });
    }

    private String normalizePurpose(String purpose) {
        String value = trim(purpose).toLowerCase(Locale.ROOT);
        if (!PURPOSE_REGISTER.equals(value) && !PURPOSE_RESET_PASSWORD.equals(value)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Unsupported email-code purpose");
        }
        return value;
    }

    private String normalizeEmail(String email) {
        String value = trim(email).toLowerCase(Locale.ROOT);
        if (value.length() > 128 || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Email format is invalid");
        }
        return value;
    }

    private String normalizeLoginIdentifier(String loginIdentifier) {
        return trim(loginIdentifier).toLowerCase(Locale.ROOT);
    }

    private String normalizeSource(String requestSource) {
        String value = trim(requestSource);
        if (value.isEmpty()) {
            return "unknown";
        }
        return value.length() > 128 ? value.substring(0, 128) : value;
    }

    private boolean isFuture(Object value, LocalDateTime now) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().isAfter(now);
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.isAfter(now);
        }
        return value != null && LocalDateTime.parse(String.valueOf(value)).isAfter(now);
    }

    private int toInt(Object value) {
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }

    private Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }

    private BusinessException invalidEmailCode() {
        return new BusinessException(ResultCode.BAD_REQUEST, "Email verification code is invalid or expired");
    }

    private BusinessException invalidCaptcha() {
        return new BusinessException(ResultCode.UNAUTHORIZED, "Login ID, password, or CAPTCHA is invalid");
    }

    private Map<String, Object> linkedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
