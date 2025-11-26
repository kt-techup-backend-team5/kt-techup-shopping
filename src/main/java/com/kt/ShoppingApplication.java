package com.kt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.kt.common.support.Message;

import lombok.RequiredArgsConstructor;

// @ServletComponentScan
@SpringBootApplication
@ConfigurationPropertiesScan
@RequiredArgsConstructor
@EnableScheduling
public class ShoppingApplication {
	private final ApplicationEventPublisher applicationEventPublisher;

	public static void main(String[] args) {
		SpringApplication.run(ShoppingApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void started() {
		applicationEventPublisher.publishEvent(new Message("Shopping Application Started"));
	}
}
