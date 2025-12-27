package com.kt.dto.question;

import java.time.LocalDateTime;

import com.kt.domain.question.Question;
import com.kt.domain.question.QuestionStatus;

public record QuestionResponse(
	Long id,
	String content,
	boolean isPublic,
	QuestionStatus status,
	String authorName,
	Long productId,
	String productName,
	boolean hasAnswer,
	AnswerResponse answer,
	LocalDateTime createdAt
) {
	public static QuestionResponse from(Question question) {
		return new QuestionResponse(
			question.getId(),
			question.getContent(),
			question.isPublic(),
			question.getStatus(),
			question.getUser().getName(),
			question.getProduct().getId(),
			question.getProduct().getName(),
			question.getAnswer() != null,
			question.getAnswer() != null ? AnswerResponse.from(question.getAnswer()) : null,
			question.getCreatedAt()
		);
	}
}
