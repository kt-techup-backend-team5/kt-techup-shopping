package com.kt.controller.chatbot;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kt.common.response.ApiResult;
import com.kt.dto.chatbot.ChatbotRequest;
import com.kt.dto.chatbot.ChatbotResponse;
import com.kt.security.CurrentUser;
import com.kt.service.ChatbotService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Chatbot", description = "AI 챗봇 API")
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {
	private final ChatbotService chatbotService;

	@Operation(
		summary = "챗봇 질문",
		description = "AI 챗봇에게 질문하고 답변을 받습니다. 로그인한 사용자의 정보를 바탕으로 맞춤 답변을 제공합니다.",
		security = @SecurityRequirement(name = "Bearer Authentication")
	)
	@PostMapping("/chat")
	public ApiResult<ChatbotResponse> chat(
		@AuthenticationPrincipal CurrentUser currentUser,
		@Validated @RequestBody ChatbotRequest request
	) {
		ChatbotResponse response = chatbotService.chat(currentUser.getId(), request);
		return ApiResult.ok(response);
	}
}
