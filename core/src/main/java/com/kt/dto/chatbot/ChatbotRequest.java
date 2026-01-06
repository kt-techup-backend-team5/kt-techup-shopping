package com.kt.dto.chatbot;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "챗봇 요청")
public record ChatbotRequest(
	@Schema(description = "사용자 질문", example = "주문 취소는 어떻게 하나요?")
	@NotBlank(message = "질문을 입력해주세요")
	String question
) {
}
