package com.campusmarket.backend.config;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Startup guard for the JWT signing secret.
 *
 * <p>The committed value of {@code app.jwt.secret} ships with a well-known development default so
 * that {@code mvnw spring-boot:run} works locally without any environment variables. That default
 * is acceptable only under dev/seed/test (and the unset "default") profiles. Any other active
 * profile — typically a production-style profile — MUST override the secret via the
 * {@code APP_JWT_SECRET} environment variable.
 *
 * <p>Behavior on application startup:
 * <ul>
 *   <li>If the resolved secret differs from the committed default, this guard is silent.</li>
 *   <li>If the resolved secret equals the committed default AND the active profile set is empty
 *       or contains one of {@code dev}, {@code seed}, {@code test}, {@code default}, a single
 *       WARN line is emitted.</li>
 *   <li>Otherwise, the guard fails fast with an {@link IllegalStateException} that names the
 *       offending profile and the env var that must be set.</li>
 * </ul>
 */
@Component
public class JwtSecretGuard {

    static final String DEV_DEFAULT_SECRET =
            "campusmarket-dev-secret-key-replace-in-production-min32";

    static final Set<String> SAFE_PROFILES = Set.of("dev", "seed", "test", "default");

    private static final Logger log = LoggerFactory.getLogger(JwtSecretGuard.class);

    private final String resolvedSecret;
    private final Environment environment;

    public JwtSecretGuard(
            @Value("${app.jwt.secret:campusmarket-dev-secret-key-replace-in-production-min32}")
                    String resolvedSecret,
            Environment environment) {
        this.resolvedSecret = resolvedSecret;
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        if (!DEV_DEFAULT_SECRET.equals(resolvedSecret)) {
            return;
        }

        String[] activeProfiles = environment.getActiveProfiles();
        Set<String> profiles = new LinkedHashSet<>(Arrays.asList(activeProfiles));

        boolean safe = profiles.isEmpty() || SAFE_PROFILES.containsAll(profiles);

        if (safe) {
            log.warn(
                    "JWT secret is using the development default; set APP_JWT_SECRET in production");
            return;
        }

        String profileLabel = String.join(",", profiles);
        throw new IllegalStateException(
                "APP_JWT_SECRET must be set when running with profile "
                        + profileLabel
                        + "; the committed dev default is forbidden outside dev/seed/test");
    }
}
