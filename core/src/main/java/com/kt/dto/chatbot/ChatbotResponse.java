package com.kt.dto.chatbot;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "챗봇 응답")
public record ChatbotResponse(
	@Schema(description = "챗봇 답변", example = "주문 취소는 마이페이지 > 주문내역에서 가능합니다.")
	String answer
) {
}
