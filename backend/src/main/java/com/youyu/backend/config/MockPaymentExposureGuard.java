package com.youyu.backend.config;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentExposureGuard {

    private static final Set<String> SAFE_PROFILES = Set.of("dev", "seed", "test", "default");

    private final boolean mockPaymentEnabled;
    private final Environment environment;

    public MockPaymentExposureGuard(
            @Value("${youyu.payment.mock-enabled:true}") boolean mockPaymentEnabled,
            Environment environment) {
        this.mockPaymentEnabled = mockPaymentEnabled;
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        if (!mockPaymentEnabled || isLocalSafeProfile()) {
            return;
        }
        String profileLabel = String.join(",", environment.getActiveProfiles());
        throw new IllegalStateException(
                "youyu.payment.mock-enabled must be false for production-like profile "
                        + profileLabel);
    }

    private boolean isLocalSafeProfile() {
        Set<String> profiles = new LinkedHashSet<>(Arrays.asList(environment.getActiveProfiles()));
        return profiles.isEmpty() || SAFE_PROFILES.containsAll(profiles);
    }
}
