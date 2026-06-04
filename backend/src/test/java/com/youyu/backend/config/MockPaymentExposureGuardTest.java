package com.youyu.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;

class MockPaymentExposureGuardTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(MockPaymentExposureGuard.class);

    @Test
    void mockPaymentGuardAllowsLocalDefaultWithoutActiveProfile() {
        contextRunner.run(context ->
                assertThat(context).hasSingleBean(MockPaymentExposureGuard.class));
    }

    @Test
    void mockPaymentGuardRejectsEnabledMockPaymentInProductionLikeProfile() {
        contextRunner
                .withInitializer(ctx -> ((ConfigurableApplicationContext) ctx)
                        .getEnvironment()
                        .setActiveProfiles("prod"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context)
                            .getFailure()
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("youyu.payment.mock-enabled")
                            .hasMessageContaining("prod");
                });
    }

    @Test
    void mockPaymentGuardAcceptsDisabledMockPaymentInProductionLikeProfile() {
        contextRunner
                .withInitializer(ctx -> ((ConfigurableApplicationContext) ctx)
                        .getEnvironment()
                        .setActiveProfiles("prod"))
                .withPropertyValues("youyu.payment.mock-enabled=false")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
