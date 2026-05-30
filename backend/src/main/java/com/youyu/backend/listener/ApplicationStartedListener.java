package com.youyu.backend.listener;

import com.youyu.backend.config.ChatSupportSchemaUpgrader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartedListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(ApplicationStartedListener.class);

    private final ChatSupportSchemaUpgrader chatSupportSchemaUpgrader;

    public ApplicationStartedListener(ChatSupportSchemaUpgrader chatSupportSchemaUpgrader) {
        this.chatSupportSchemaUpgrader = chatSupportSchemaUpgrader;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        chatSupportSchemaUpgrader.upgradeIfNeeded();
        log.info("Youyu backend scaffold is ready.");
    }
}
