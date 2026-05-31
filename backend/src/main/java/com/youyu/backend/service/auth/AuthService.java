package com.youyu.backend.service.auth;

import com.youyu.backend.controller.auth.dto.RegisterRequest;
import com.youyu.backend.controller.auth.dto.PasswordResetRequest;
import java.util.Map;

public interface AuthService {

    /**
     * Single entry: resolves admin vs campus user by {@code loginId}, validates password, returns session payload
     * ({@code token}, {@code role}, {@code user}) for the client.
     */
    Map<String, Object> unifiedLogin(String loginId,
                                     String password,
                                     String captchaChallengeId,
                                     String captchaCode,
                                     String requestSource);

    Map<String, Object> register(RegisterRequest request);

    Map<String, Object> sendEmailCode(String email, String purpose, String requestSource);

    Map<String, Object> createCaptcha(String requestSource);

    Map<String, Object> resetPassword(PasswordResetRequest request);

    Map<String, Object> currentUser();
}
