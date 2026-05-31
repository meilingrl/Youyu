package com.youyu.backend.service.auth;

public interface AuthMailSender {

    void validateConfiguration();

    void sendVerificationCode(String recipient, String purpose, String code);
}
