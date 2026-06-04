package com.youyu.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;

class CorsPropertiesTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(ConfigurationPropertiesAutoConfiguration.class, CorsProperties.class);

    @Test
    void corsAllowsLocalDefaultsWithoutActiveProfile() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(CorsProperties.class).getAllowedOrigins()).isEmpty();
        });
    }

    @Test
    void corsRejectsProductionLikeProfileWithoutExplicitOrigins() {
        contextRunner
                .withInitializer(ctx -> ((ConfigurableApplicationContext) ctx)
                        .getEnvironment()
                        .setActiveProfiles("staging"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context)
                            .getFailure()
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("youyu.cors.allowed-origins");
                });
    }

    @Test
    void corsRejectsWildcardOriginsInProductionLikeProfiles() {
        contextRunner
                .withInitializer(ctx -> ((ConfigurableApplicationContext) ctx)
                        .getEnvironment()
                        .setActiveProfiles("prod"))
                .withPropertyValues("youyu.cors.allowed-origins[0]=*")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context)
                            .getFailure()
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Wildcard CORS origins");
                });
    }

    @Test
    void corsAcceptsExplicitOriginsInProductionLikeProfiles() {
        contextRunner
                .withInitializer(ctx -> ((ConfigurableApplicationContext) ctx)
                        .getEnvironment()
                        .setActiveProfiles("prod"))
                .withPropertyValues("youyu.cors.allowed-origins[0]=https://youyu.example.com")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(CorsProperties.class).getAllowedOrigins())
                            .containsExactly("https://youyu.example.com");
                });
    }
}
