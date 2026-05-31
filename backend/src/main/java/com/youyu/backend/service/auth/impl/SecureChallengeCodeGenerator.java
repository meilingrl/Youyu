package com.youyu.backend.service.auth.impl;

import com.youyu.backend.service.auth.ChallengeCodeGenerator;
import java.security.SecureRandom;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class SecureChallengeCodeGenerator implements ChallengeCodeGenerator {

    private static final char[] CAPTCHA_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final SecureRandom random = new SecureRandom();

    @Override
    public String emailCode() {
        return "%06d".formatted(random.nextInt(1_000_000));
    }

    @Override
    public String captchaCode() {
        StringBuilder value = new StringBuilder();
        for (int index = 0; index < 4; index++) {
            value.append(CAPTCHA_ALPHABET[random.nextInt(CAPTCHA_ALPHABET.length)]);
        }
        return value.toString();
    }
}
