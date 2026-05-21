package com.campusmarket.backend.service.auth.impl;

import com.campusmarket.backend.common.auth.AuthUser;
import com.campusmarket.backend.service.auth.AuthTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class JwtAuthTokenServiceImpl implements AuthTokenService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Pattern MOCK_TOKEN_PATTERN = Pattern.compile("mock-\\d+-[A-Z]+");

    private final SecretKey signingKey;
    private final long expirationHours;

    public JwtAuthTokenServiceImpl(@Value("${app.jwt.secret:campusmarket-dev-secret-key-replace-in-production-min32}") String secret,
                                   @Value("${app.jwt.expiration-hours:72}") long expirationHours) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = expirationHours;
    }

    @Override
    public String generate(Long userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationHours, ChronoUnit.HOURS)))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public Optional<AuthUser> resolve(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            String token = authorization.substring(BEARER_PREFIX.length()).trim();
            if (MOCK_TOKEN_PATTERN.matcher(token).matches()) {
                return resolveMockToken(token);
            }

            try {
                Claims claims = Jwts.parser()
                        .verifyWith(signingKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
                return Optional.of(new AuthUser(
                        toLong(claims.get("userId")),
                        String.valueOf(claims.get("role")),
                        token
                ));
            } catch (JwtException | IllegalArgumentException exception) {
                return Optional.empty();
            }
        }

        String userIdHeader = request.getHeader("X-User-Id");
        String roleHeader = request.getHeader("X-User-Role");
        if (userIdHeader == null || roleHeader == null) {
            return Optional.empty();
        }

        return Optional.of(new AuthUser(
                parseLong(userIdHeader),
                roleHeader.toUpperCase(),
                "header-auth"
        ));
    }

    private Optional<AuthUser> resolveMockToken(String token) {
        String[] segments = token.split("-");
        if (segments.length != 3 || !"mock".equalsIgnoreCase(segments[0])) {
            return Optional.empty();
        }
        return Optional.of(new AuthUser(
                parseLong(segments[1]),
                segments[2].toUpperCase(),
                token
        ));
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        return parseLong(String.valueOf(value));
    }
}
