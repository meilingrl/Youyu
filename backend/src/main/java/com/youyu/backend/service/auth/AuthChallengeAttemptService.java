package com.youyu.backend.service.auth;

import com.youyu.backend.mapper.auth.CaptchaChallengeMapper;
import com.youyu.backend.mapper.auth.EmailVerificationChallengeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthChallengeAttemptService {

    private final EmailVerificationChallengeMapper emailChallengeMapper;
    private final CaptchaChallengeMapper captchaChallengeMapper;

    public AuthChallengeAttemptService(EmailVerificationChallengeMapper emailChallengeMapper,
                                       CaptchaChallengeMapper captchaChallengeMapper) {
        this.emailChallengeMapper = emailChallengeMapper;
        this.captchaChallengeMapper = captchaChallengeMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEmailCodeFailure(Long challengeId) {
        emailChallengeMapper.incrementAttemptCount(challengeId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordCaptchaFailure(Long challengeId) {
        captchaChallengeMapper.incrementAttemptCount(challengeId);
    }
}
