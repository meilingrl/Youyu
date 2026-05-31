package com.youyu.backend.controller.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    private String loginId;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String captchaChallengeId;

    private String captchaCode;

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCaptchaChallengeId() {
        return captchaChallengeId;
    }

    public void setCaptchaChallengeId(String captchaChallengeId) {
        this.captchaChallengeId = captchaChallengeId;
    }

    public String getCaptchaCode() {
        return captchaCode;
    }

    public void setCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
    }
}
