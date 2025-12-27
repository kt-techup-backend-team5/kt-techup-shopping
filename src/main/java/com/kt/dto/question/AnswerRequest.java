package com.kt.dto.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AnswerRequest {

	public record Create(
		@NotNull(message = "문의 ID는 필수입니다.")
		Long questionId,

		@NotBlank(message = "답변 내용은 필수입니다.")
		String content
	) {}

	public record Update(
		@NotBlank(message = "답변 내용은 필수입니다.")
		String content
	) {}
}
