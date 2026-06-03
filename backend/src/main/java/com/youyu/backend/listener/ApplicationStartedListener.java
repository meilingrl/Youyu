package com.youyu.backend.listener;

import com.youyu.backend.config.ChatSupportSchemaUpgrader;
import com.youyu.backend.config.PaymentSchemaUpgrader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartedListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(ApplicationStartedListener.class);

    private final ChatSupportSchemaUpgrader chatSupportSchemaUpgrader;
    private final PaymentSchemaUpgrader paymentSchemaUpgrader;

    public ApplicationStartedListener(ChatSupportSchemaUpgrader chatSupportSchemaUpgrader,
                                      PaymentSchemaUpgrader paymentSchemaUpgrader) {
        this.chatSupportSchemaUpgrader = chatSupportSchemaUpgrader;
        this.paymentSchemaUpgrader = paymentSchemaUpgrader;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        chatSupportSchemaUpgrader.upgradeIfNeeded();
        paymentSchemaUpgrader.upgradeIfNeeded();
        log.info("Youyu backend scaffold is ready.");
    }
}
