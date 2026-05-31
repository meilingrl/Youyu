package com.youyu.backend.service.auth;

public interface ChallengeCodeGenerator {

    String emailCode();

    String captchaCode();
}
