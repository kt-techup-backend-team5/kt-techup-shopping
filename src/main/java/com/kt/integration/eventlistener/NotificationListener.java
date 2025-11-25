package com.kt.integration.eventlistener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.kt.common.support.Message;
import com.kt.integration.slack.NotifyApi;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationListener {
	private final NotifyApi notifyApi;

	@EventListener(Message.class)
	public void onMessage(Message message){
		notifyApi.notify(message.message());
	}
}

