package com.kt.integration.slack;

import java.util.Arrays;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.kt.common.profile.AppProfile;
import com.kt.common.profile.LocalProfile;
import com.slack.api.methods.MethodsClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 실제로 슬랙으로 알림을 보낼 것
@Slf4j
@Component
@AppProfile
@RequiredArgsConstructor
public class DefaultNotifyApi implements NotifyApi {
	private final MethodsClient methodsClient;
	private final SlackProperties slackProperties;
	private final Environment environment;

	@Override
	public void notify(String message) {
		// 슬랙으로 진짜 발송할때는 dev, prod로만 발송
		// 로컬에서는 그냥 로그만 남김
		try {
			methodsClient.chatPostMessage(request -> {
				request.username("spring-Bot")
						.channel(slackProperties.logChannel())
						.text(String.format("```%s - shopping-%s```", message, getActiveProfile()))
						.build();

				return request;
			});
		} catch (Exception e) {
			// log.error(e.getMessage());
			log.error("Failed to send Slack notification", e);
		}
	}

	private String getActiveProfile() {
		return Arrays.stream(environment.getActiveProfiles()).findFirst().orElse("local");
	}
}