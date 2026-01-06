package com.kt.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kt.domain.review.Review;

import lombok.extern.slf4j.Slf4j;

/**
 * OpenAI(ChatClient)를 호출해 리뷰 요약 생성
 */
@Slf4j
@Component
public class OpenAiReviewSummaryGenerator implements ReviewSummaryGenerator {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.prompt-version:v1}")
    private String promptVersion;

    public OpenAiReviewSummaryGenerator(ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Result generate(List<Review> reviews, double avgRating, Map<Integer, Long> ratingCounts) {

        String dist = ratingCounts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + "점:" + e.getValue())
            .collect(Collectors.joining(", "));

        String reviewLines = reviews.stream()
            .map(r -> "- (" + r.getRating() + "점) " + trimTo(r.getContent(), 200))
            .collect(Collectors.joining("\n"));

        String prompt = """
			너는 쇼핑몰 상품 리뷰를 요약하는 시스템이다.
			아래 리뷰 텍스트만 근거로 한국어로 요약하라. 과장/추정/허위 금지.

			반드시 JSON만 출력하라(코드블록 금지, 추가 텍스트 금지).
			형식:
			{"summary":"...","pros":["...","...","..."],"cons":["...","...","..."],"keywords":["...","...","..."]}

			규칙:
			- pros/cons는 정확히 3개
			- keywords는 정확히 3개(명사 위주)
			- summary는 2~3문장

			참고 지표:
			- 평균 평점: %s
			- 평점 분포: %s

			리뷰:
			%s
			""".formatted(String.format("%.2f", avgRating), dist, reviewLines);

        String content = chatClient.prompt()
            .user(prompt)
            .call()
            .content();

        Output out = parseJson(content);

        return new Result(
            out.summary(),
            out.pros(),
            out.cons(),
            out.keywords(),
            model,
            promptVersion
        );
    }

    private Output parseJson(String content) {
        try {
            String json = stripCodeFence(content);
            return objectMapper.readValue(json, Output.class);
        } catch (Exception e) {
            log.warn("AI 응답 JSON 파싱 실패. raw={}", content, e);
            throw new IllegalStateException("AI 응답 JSON 파싱 실패", e);
        }
    }

    private String stripCodeFence(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\s*", "");
            t = t.replaceFirst("\\s*```$", "");
        }
        return t.trim();
    }

    private String trimTo(String s, int max) {
        if (s == null) return "";
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max) + "...";
    }

    private record Output(
        String summary,
        List<String> pros,
        List<String> cons,
        List<String> keywords
    ) {}
}