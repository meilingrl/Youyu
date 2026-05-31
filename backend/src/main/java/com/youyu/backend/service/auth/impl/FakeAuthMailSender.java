package com.youyu.backend.service.auth.impl;

import com.youyu.backend.service.auth.AuthMailSender;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("test")
public class FakeAuthMailSender implements AuthMailSender {

    private final List<Delivery> deliveries = new ArrayList<>();

    @Override
    public void validateConfiguration() {
        // The test profile is intentionally network-free.
    }

    @Override
    public synchronized void sendVerificationCode(String recipient, String purpose, String code) {
        deliveries.add(new Delivery(recipient, purpose, code));
    }

    public synchronized List<Delivery> deliveries() {
        return List.copyOf(deliveries);
    }

    public synchronized void clear() {
        deliveries.clear();
    }

    public record Delivery(String recipient, String purpose, String code) {
    }
}
