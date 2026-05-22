package com.campusmarket.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;

class JwtSecretGuardTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(JwtSecretGuard.class);

    @Test
    void jwtSecretGuardAllowsDevDefaultUnderDevProfile() {
        contextRunner
                .withInitializer(
                        ctx ->
                                ((ConfigurableApplicationContext) ctx)
                                        .getEnvironment()
                                        .setActiveProfiles("dev"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(JwtSecretGuard.class);
                });
    }

    @Test
    void jwtSecretGuardAllowsDevDefaultUnderSeedProfile() {
        contextRunner
                .withInitializer(
                        ctx ->
                                ((ConfigurableApplicationContext) ctx)
                                        .getEnvironment()
                                        .setActiveProfiles("seed"))
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void jwtSecretGuardAllowsDevDefaultWhenNoActiveProfile() {
        contextRunner.run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void jwtSecretGuardRejectsDevDefaultUnderProdProfile() {
        contextRunner
                .withInitializer(
                        ctx ->
                                ((ConfigurableApplicationContext) ctx)
                                        .getEnvironment()
                                        .setActiveProfiles("prod"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context)
                            .getFailure()
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("APP_JWT_SECRET")
                            .hasMessageContaining("prod");
                });
    }

    @Test
    void jwtSecretGuardAcceptsCustomSecretUnderProdProfile() {
        contextRunner
                .withInitializer(
                        ctx ->
                                ((ConfigurableApplicationContext) ctx)
                                        .getEnvironment()
                                        .setActiveProfiles("prod"))
                .withPropertyValues("app.jwt.secret=an-overridden-production-secret-of-32+chars")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
