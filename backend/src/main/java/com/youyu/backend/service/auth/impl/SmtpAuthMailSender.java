package com.youyu.backend.service.auth.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.config.AuthMailProperties;
import com.youyu.backend.service.auth.AuthMailSender;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class SmtpAuthMailSender implements AuthMailSender {

    private final JavaMailSender mailSender;
    private final AuthMailProperties properties;

    public SmtpAuthMailSender(JavaMailSender mailSender, AuthMailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void validateConfiguration() {
        if (!properties.isComplete()) {
            throw deliveryFailure();
        }
    }

    @Override
    public void sendVerificationCode(String recipient, String purpose, String code) {
        validateConfiguration();
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getFrom());
            message.setTo(recipient);
            message.setSubject("Youyu verification code");
            message.setText("""
                    Your Youyu verification code is %s.
                    It expires in 10 minutes. Do not share this code with anyone.
                    Purpose: %s
                    """.formatted(code, purpose));
            mailSender.send(message);
        } catch (MailException exception) {
            throw deliveryFailure();
        }
    }

    private BusinessException deliveryFailure() {
        return new BusinessException(
                ResultCode.INTERNAL_SERVER_ERROR,
                "Email delivery failed. Check the SMTP configuration and try again."
        );
    }
}
