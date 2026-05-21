package com.campusmarket.backend.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartedListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(ApplicationStartedListener.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("CampusMarket backend scaffold is ready.");
    }
}
