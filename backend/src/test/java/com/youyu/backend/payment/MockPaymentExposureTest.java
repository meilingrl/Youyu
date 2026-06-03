package com.youyu.backend.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.youyu.backend.service.payment.impl.MockPaymentGatewayServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class MockPaymentExposureTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(MockPaymentGatewayServiceImpl.class);

    @Test
    void mockPaymentGatewayIsAvailableForLocalDefault() {
        contextRunner.run(context ->
                assertThat(context).hasSingleBean(MockPaymentGatewayServiceImpl.class));
    }

    @Test
    void mockPaymentGatewayIsNotRegisteredWhenDisabled() {
        contextRunner
                .withPropertyValues("youyu.payment.mock-enabled=false")
                .run(context ->
                        assertThat(context).doesNotHaveBean(MockPaymentGatewayServiceImpl.class));
    }
}
