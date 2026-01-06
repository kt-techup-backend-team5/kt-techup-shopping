package com.kt.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.kt.dto.chatbot.ChatbotRequest;
import com.kt.dto.chatbot.ChatbotResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChatbotService {
	private final ChatClient chatClient;
	private final UserOrderService userOrderService;
	private final String systemPrompt;

	private static final Pattern ORDER_INQUIRY_PATTERN = Pattern.compile(
		".*(주문|구매|샀|산|내역|order).*",
		Pattern.CASE_INSENSITIVE
	);

	public ChatbotService(ChatClient chatClient, UserOrderService userOrderService) {
		this.chatClient = chatClient;
		this.userOrderService = userOrderService;
		this.systemPrompt = loadSystemPrompt();
	}

	public ChatbotResponse chat(Long userId, ChatbotRequest request) {
		String userQuestion = request.question();
		String contextInfo = "";

		// 주문 관련 질문인지 확인
		if (isOrderInquiry(userQuestion)) {
			contextInfo = getUserOrderContext(userId);
		}

		String finalPrompt = userQuestion;
		if (!contextInfo.isEmpty()) {
			finalPrompt = contextInfo + "\n\n사용자 질문: " + userQuestion;
		}

		String answer = chatClient.prompt()
			.system(systemPrompt)
			.user(finalPrompt)
			.call()
			.content();

		log.info("AI 챗봇 응답 - userId: {}, questionLength: {}자, answerLength: {}자, orderContext: {}",
			userId, userQuestion.length(), answer.length(), !contextInfo.isEmpty());

		return new ChatbotResponse(answer);
	}

	private boolean isOrderInquiry(String question) {
		return ORDER_INQUIRY_PATTERN.matcher(question).matches();
	}

	private String getUserOrderContext(Long userId) {
		try {
			var pageable = PageRequest.of(0, 10);
			var orderPage = userOrderService.listMyOrders(userId, pageable);

			if (orderPage.isEmpty()) {
				return "[사용자 주문 내역: 없음]";
			}

			StringBuilder context = new StringBuilder("[사용자의 최근 주문 내역]\n");
			var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

			orderPage.getContent().forEach(order -> {
				context.append(String.format(
					"- 주문번호: %d, 상품: %s %s개, 금액: %,d원, 상태: %s, 주문일: %s\n",
					order.orderId(),
					order.firstProductName(),
					order.productCount() > 1 ? "외 " + (order.productCount() - 1) : "",
					order.totalPrice(),
					getOrderStatusKorean(order.status().name()),
					order.createdAt().format(formatter)
				));
			});

			return context.toString();
		} catch (Exception e) {
			return "[사용자 주문 내역 조회 실패]";
		}
	}

	private String getOrderStatusKorean(String status) {
		return switch (status) {
			case "ORDER_CREATED" -> "주문 생성";
			case "ORDER_ACCEPTED" -> "주문 승인";
			case "ORDER_PREPARING" -> "주문 준비중";
			case "ORDER_SHIPPING" -> "배송중";
			case "ORDER_DELIVERED" -> "배송 완료";
			case "ORDER_CANCELLED" -> "주문 취소";
			default -> status;
		};
	}

	private String loadSystemPrompt() {
		try {
			var resource = new ClassPathResource("prompts/chatbot-system-prompt.txt");
			return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			// Fallback 프롬프트
			return """
				당신은 케클케클 쇼핑몰의 고객 지원 AI 어시스턴트입니다.
				고객의 질문에 친절하고 정확하게 답변해주세요.
				주문, 배송, 결제, 반품, 교환 등 쇼핑몰 관련 질문에 답변합니다.
				""";
		}
	}
}
