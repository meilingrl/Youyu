package com.youyu.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.youyu.backend.common.auth.AuthUser;
import com.youyu.backend.service.auth.impl.JwtAuthTokenServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

class MockAuthBoundaryTest {

    private static final String SECRET = "test-secret-for-jwt-boundary-check-32chars";
    private static final String MOCK_USER_TOKEN = "mock-" + "1001-USER";

    @Test
    void mockTokenResolvesWhenEnabledWithoutProductionLikeProfile() {
        JwtAuthTokenServiceImpl service = new JwtAuthTokenServiceImpl(SECRET, 72, true, new MockEnvironment());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + MOCK_USER_TOKEN);

        Optional<AuthUser> user = service.resolve(request);

        assertThat(user).isPresent();
        assertThat(user.get().getUserId()).isEqualTo(1001L);
        assertThat(user.get().getRole()).isEqualTo("USER");
    }

    @Test
    void mockTokenDoesNotResolveWhenDisabled() {
        JwtAuthTokenServiceImpl service = new JwtAuthTokenServiceImpl(SECRET, 72, false, new MockEnvironment());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + MOCK_USER_TOKEN);

        assertThat(service.resolve(request)).isEmpty();
    }

    @Test
    void mockTokenDoesNotResolveInStagingProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("staging");
        JwtAuthTokenServiceImpl service = new JwtAuthTokenServiceImpl(SECRET, 72, true, environment);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + MOCK_USER_TOKEN);

        assertThat(service.resolve(request)).isEmpty();
    }

    @Test
    void headerAuthDoesNotResolveInStagingProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("staging");
        JwtAuthTokenServiceImpl service = new JwtAuthTokenServiceImpl(SECRET, 72, true, environment);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "1001");
        request.addHeader("X-User-Role", "USER");

        assertThat(service.resolve(request)).isEmpty();
    }
}
