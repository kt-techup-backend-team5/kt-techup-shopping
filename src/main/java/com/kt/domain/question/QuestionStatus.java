package com.kt.domain.question;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum QuestionStatus {
	PENDING("답변 대기"),
	ANSWERED("답변 완료");

	private final String description;
}
