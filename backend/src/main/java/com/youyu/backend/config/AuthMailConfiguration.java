package com.youyu.backend.config;

import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
@Profile("!test")
public class AuthMailConfiguration {

    @Bean
    public JavaMailSender authJavaMailSender(AuthMailProperties properties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.getHost());
        sender.setPort(properties.getPort());
        sender.setUsername(properties.getUsername());
        sender.setPassword(properties.getPassword());
        Properties mailProperties = sender.getJavaMailProperties();
        mailProperties.put("mail.smtp.auth", "true");
        mailProperties.put("mail.smtp.ssl.enable", String.valueOf(properties.isSslEnabled()));
        mailProperties.put("mail.smtp.starttls.enable", String.valueOf(!properties.isSslEnabled()));
        mailProperties.put("mail.smtp.starttls.required", String.valueOf(!properties.isSslEnabled()));
        return sender;
    }
}
