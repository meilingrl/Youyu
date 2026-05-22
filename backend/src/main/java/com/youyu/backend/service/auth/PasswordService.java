package com.youyu.backend.service.auth;

import com.youyu.backend.mapper.user.UserMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    private final BCryptPasswordEncoder passwordEncoder;

    public PasswordService(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean verifyAndMigrate(String rawPassword, String storedHash, Long userId, UserMapper userMapper) {
        try {
            if (passwordEncoder.matches(rawPassword, storedHash)) {
                return true;
            }
        } catch (IllegalArgumentException e) {
            // not a bcrypt hash — fall through to legacy verification
        }

        if (rawPassword.equals(storedHash) || sha256(rawPassword).equals(storedHash)) {
            userMapper.updatePasswordHash(userId, passwordEncoder.encode(rawPassword));
            return true;
        }

        return false;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : hashed) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
