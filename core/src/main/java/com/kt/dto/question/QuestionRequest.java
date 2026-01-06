package com.kt.dto.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class QuestionRequest {

	public record Create(
		@NotNull(message = "상품 ID는 필수입니다.")
		Long productId,

		@NotBlank(message = "문의 내용은 필수입니다.")
		String content,

		@NotNull(message = "공개 여부는 필수입니다.")
		Boolean isPublic
	) {}

	public record Update(
		@NotBlank(message = "문의 내용은 필수입니다.")
		String content
	) {}

	public record UpdatePublic(
		@NotNull(message = "공개 여부는 필수입니다.")
		Boolean isPublic
	) {}
}
