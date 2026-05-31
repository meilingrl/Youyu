package com.youyu.backend.service.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class ChallengeCodeHasher {

    private final BCryptPasswordEncoder passwordEncoder;

    public ChallengeCodeHasher(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String hash(String code) {
        return passwordEncoder.encode(code);
    }

    public boolean matches(String code, String hash) {
        return code != null && hash != null && passwordEncoder.matches(code, hash);
    }
}
