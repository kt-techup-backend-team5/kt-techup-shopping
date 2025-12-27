package com.kt.dto.question;

import java.time.LocalDateTime;

import com.kt.domain.question.Answer;

public record AnswerResponse(
	Long id,
	String content,
	String adminName,
	Long questionId,
	LocalDateTime createdAt
) {
	public static AnswerResponse from(Answer answer) {
		return new AnswerResponse(
			answer.getId(),
			answer.getContent(),
			answer.getAdmin().getName(),
			answer.getQuestion().getId(),
			answer.getCreatedAt()
		);
	}
}
