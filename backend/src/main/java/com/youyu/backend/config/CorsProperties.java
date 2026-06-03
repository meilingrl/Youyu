package com.youyu.backend.config;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "youyu.cors")
public class CorsProperties {

    private static final Set<String> SAFE_PROFILES = Set.of("dev", "seed", "test", "default");

    private final Environment environment;

    private List<String> allowedOrigins = new ArrayList<>();
    private List<String> allowedOriginPatterns = new ArrayList<>();
    private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    private List<String> allowedHeaders = new ArrayList<>(List.of("*"));
    private boolean allowCredentials = true;
    private long maxAge = 3600;

    public CorsProperties(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        allowedOrigins = clean(allowedOrigins);
        allowedOriginPatterns = clean(allowedOriginPatterns);
        allowedMethods = clean(allowedMethods);
        allowedHeaders = clean(allowedHeaders);

        if (isLocalSafeProfile()) {
            return;
        }
        if (allowedOrigins.isEmpty() && allowedOriginPatterns.isEmpty()) {
            throw new IllegalStateException(
                    "youyu.cors.allowed-origins must be set for production-like profiles");
        }
        if (containsWildcard(allowedOrigins) || containsWildcard(allowedOriginPatterns)) {
            throw new IllegalStateException(
                    "Wildcard CORS origins are forbidden for production-like profiles");
        }
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    private boolean isLocalSafeProfile() {
        Set<String> profiles = new LinkedHashSet<>(Arrays.asList(environment.getActiveProfiles()));
        return profiles.isEmpty() || SAFE_PROFILES.containsAll(profiles);
    }

    private boolean containsWildcard(List<String> values) {
        return values.stream().anyMatch("*"::equals);
    }

    private List<String> clean(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }
}
