package com.kt.integration.slack;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SlackConfiguration {
	private final SlackProperties slackProperties;

	@Bean
	public MethodsClient methodsClient() {
		return Slack.getInstance().methods(slackProperties.botToken());
	}
}
