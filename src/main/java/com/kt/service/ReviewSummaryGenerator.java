package com.kt.service;

import java.util.List;
import java.util.Map;

import com.kt.domain.review.Review;

/**
 * 리뷰 목록을 입력으로 받아 요약 결과 생성(AI 구현체로 연결)
 */
public interface ReviewSummaryGenerator {
    Result generate(List<Review> reviews, double avgRating, Map<Integer, Long> ratingCounts);

    record Result(
        String summary,
        List<String> pros,
        List<String> cons,
        List<String> keywords,
        String model,
        String promptVersion
    ) {}
}