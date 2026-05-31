package com.youyu.backend.service.auth.impl;

import com.youyu.backend.service.auth.ChallengeCodeGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class DeterministicChallengeCodeGenerator implements ChallengeCodeGenerator {

    @Override
    public String emailCode() {
        return "482913";
    }

    @Override
    public String captchaCode() {
        return "AB12";
    }
}
