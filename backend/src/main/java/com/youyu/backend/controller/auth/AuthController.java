package com.youyu.backend.controller.auth;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.common.support.RequestContext;
import com.youyu.backend.controller.auth.dto.EmailCodeRequest;
import com.youyu.backend.controller.auth.dto.LoginRequest;
import com.youyu.backend.controller.auth.dto.PasswordResetRequest;
import com.youyu.backend.controller.auth.dto.RegisterRequest;
import com.youyu.backend.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest body,
                                                     HttpServletRequest request) {
        return ApiResponse.success(authService.register(body), traceId(request));
    }

    @PostMapping("/email-codes")
    public ApiResponse<Map<String, Object>> sendEmailCode(@Valid @RequestBody EmailCodeRequest body,
                                                          HttpServletRequest request) {
        return ApiResponse.success(
                authService.sendEmailCode(body.getEmail(), body.getPurpose(), requestSource(request)),
                traceId(request)
        );
    }

    @GetMapping("/captcha")
    public ApiResponse<Map<String, Object>> captcha(HttpServletRequest request) {
        return ApiResponse.success(authService.createCaptcha(requestSource(request)), traceId(request));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest body,
                                                  HttpServletRequest request) {
        return ApiResponse.success(
                authService.unifiedLogin(
                        body.getLoginId(),
                        body.getPassword(),
                        body.getCaptchaChallengeId(),
                        body.getCaptchaCode(),
                        requestSource(request)
                ),
                traceId(request)
        );
    }

    @PostMapping("/password-reset")
    public ApiResponse<Map<String, Object>> resetPassword(@Valid @RequestBody PasswordResetRequest body,
                                                          HttpServletRequest request) {
        return ApiResponse.success(authService.resetPassword(body), traceId(request));
    }

    @PostMapping("/logout")
    @LoginRequired
    public ApiResponse<Map<String, Object>> logout(HttpServletRequest request) {
        return ApiResponse.success(
                "Logout skeleton is reserved for future session invalidation logic",
                Map.of("module", "auth"),
                traceId(request)
        );
    }

    @GetMapping("/me")
    @LoginRequired
    public ApiResponse<Map<String, Object>> me(HttpServletRequest request) {
        return ApiResponse.success(authService.currentUser(), traceId(request));
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }

    private String requestSource(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
