package com.youyu.backend.service.auth.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.AuthUser;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.controller.auth.dto.PasswordResetRequest;
import com.youyu.backend.controller.auth.dto.RegisterRequest;
import com.youyu.backend.mapper.user.UserMapper;
import com.youyu.backend.service.auth.AuthChallengeService;
import com.youyu.backend.service.auth.AuthService;
import com.youyu.backend.service.auth.AuthTokenService;
import com.youyu.backend.service.auth.PasswordService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthTokenService authTokenService;
    private final UserMapper userMapper;
    private final PasswordService passwordService;
    private final AuthChallengeService authChallengeService;

    public AuthServiceImpl(AuthTokenService authTokenService,
                           UserMapper userMapper,
                           PasswordService passwordService,
                           AuthChallengeService authChallengeService) {
        this.authTokenService = authTokenService;
        this.userMapper = userMapper;
        this.passwordService = passwordService;
        this.authChallengeService = authChallengeService;
    }

    @Override
    public Map<String, Object> unifiedLogin(String loginId,
                                            String password,
                                            String captchaChallengeId,
                                            String captchaCode,
                                            String requestSource) {
        String idTrim = trim(loginId);
        if (idTrim.isEmpty() || isBlank(password)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Login ID and password are required");
        }
        authChallengeService.validateCaptchaIfRequired(
                idTrim, requestSource, captchaChallengeId, captchaCode
        );

        Optional<Map<String, Object>> userOpt = userMapper.findByLoginId(idTrim);
        if (userOpt.isEmpty()) {
            authChallengeService.recordLoginFailure(idTrim, requestSource);
            throw invalidCredentials();
        }

        Map<String, Object> user = userOpt.get();
        Long uid = toLong(user.get("id"));
        String storedHash = Objects.toString(user.get("passwordHash"), Objects.toString(user.get("password"), ""));
        if (!passwordService.verifyAndMigrate(password, storedHash, uid, userMapper)) {
            authChallengeService.recordLoginFailure(idTrim, requestSource);
            throw invalidCredentials();
        }
        authChallengeService.clearLoginFailures(idTrim, requestSource);
        String status = String.valueOf(user.get("status"));
        if ("disabled".equalsIgnoreCase(status) || "deleted".equalsIgnoreCase(status)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Account is disabled. Contact support.");
        }
        userMapper.updateLastLoginAt(uid);
        Map<String, Object> userBrief = new LinkedHashMap<>();
        userBrief.put("id", String.valueOf(uid));
        userBrief.put("loginId", Objects.toString(user.get("username"), idTrim));
        userBrief.put("nickname", Objects.toString(user.get("nickname"), "User"));
        userBrief.put("verificationStatus", Objects.toString(user.get("verificationStatus"), "unverified"));
        userBrief.put("privilege", userMapper.findPrivilegeProfile(uid).orElseGet(Map::of));

        Map<String, Object> result = new LinkedHashMap<>();
        String role = String.valueOf(user.getOrDefault("role", "USER"));
        result.put("token", authTokenService.generate(uid, role));
        result.put("role", role.toLowerCase());
        result.put("user", userBrief);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> register(RegisterRequest request, String requestSource, String userAgent) {
        String username = trim(request.getUsername());
        String email = trim(request.getEmail());
        String phone = trim(request.getPhone());
        String nickname = trim(request.getNickname());
        String password = request.getPassword();
        if (isBlank(username) || isBlank(password) || isBlank(nickname) || isBlank(email)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Username, password, nickname, and email are required");
        }
        if (!Boolean.TRUE.equals(request.getAgreedToTerms())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "User agreement and privacy policy consent is required");
        }
        if (userMapper.findByLoginId(username).isPresent() || userMapper.findByEmail(email).isPresent()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Username or email is already in use");
        }
        authChallengeService.verifyAndConsumeEmailCode(
                email, AuthChallengeService.PURPOSE_REGISTER, request.getEmailCode()
        );

        try {
            Long userId = userMapper.insert(username, phone, email, passwordService.encode(password), nickname);
            userMapper.insertDefaultPrivilegeProfile(userId);
            userMapper.insertConsentLog(userId, "registration_terms", true, "registration", requestSource, userAgent);
            userMapper.insertConsentLog(userId, "privacy_policy", true, "registration", requestSource, userAgent);
            Map<String, Object> user = userMapper.findById(userId)
                    .orElseThrow(() -> new BusinessException(
                            ResultCode.INTERNAL_SERVER_ERROR,
                            "Unable to read registered user"
                    ));
            return linkedMap("user", publicUserBrief(user));
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Username or email is already in use");
        }
    }

    @Override
    public Map<String, Object> sendEmailCode(String email, String purpose, String requestSource) {
        return authChallengeService.sendEmailCode(email, purpose, requestSource);
    }

    @Override
    public Map<String, Object> createCaptcha(String requestSource) {
        return authChallengeService.createCaptcha(requestSource);
    }

    @Override
    @Transactional
    public Map<String, Object> resetPassword(PasswordResetRequest request) {
        String email = trim(request.getEmail());
        String newPassword = request.getNewPassword();
        if (isBlank(email) || isBlank(newPassword)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Email and new password are required");
        }
        authChallengeService.verifyAndConsumeEmailCode(
                email, AuthChallengeService.PURPOSE_RESET_PASSWORD, request.getEmailCode()
        );
        Map<String, Object> user = userMapper.findByEmail(email)
                .orElseThrow(() -> new BusinessException(
                        ResultCode.BAD_REQUEST,
                        "Email verification code is invalid or expired"
                ));
        userMapper.updatePasswordHash(toLong(user.get("id")), passwordService.encode(newPassword));
        return Map.of("reset", true);
    }

    @Override
    public Map<String, Object> currentUser() {
        AuthUser authUser = AuthContextHolder.get();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", authUser == null ? null : authUser.getUserId());
        result.put("role", authUser == null ? null : authUser.getRole());
        return result;
    }

    private Map<String, Object> publicUserBrief(Map<String, Object> user) {
        return linkedMap(
                "id", String.valueOf(user.get("id")),
                "loginId", user.get("username"),
                "nickname", user.get("nickname"),
                "verificationStatus", user.get("verificationStatus")
        );
    }

    private Map<String, Object> linkedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }

    private Long toLong(Object raw) {
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(raw));
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(ResultCode.UNAUTHORIZED, "Login ID, password, or CAPTCHA is invalid");
    }
}
