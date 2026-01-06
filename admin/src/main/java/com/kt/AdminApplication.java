package com.kt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

import com.kt.common.support.Message;

import lombok.RequiredArgsConstructor;

@SpringBootApplication
@ConfigurationPropertiesScan
@RequiredArgsConstructor
public class AdminApplication {
    private final ApplicationEventPublisher applicationEventPublisher;

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void started() {
        applicationEventPublisher.publishEvent(new Message("Shopping Admin Application Started"));
    }
}
