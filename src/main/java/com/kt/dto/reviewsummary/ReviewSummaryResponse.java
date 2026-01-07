package com.kt.dto.reviewsummary;

import com.kt.domain.reviewsummary.ReviewSummary;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewSummaryResponse(
    Long productId,
    LocalDateTime generatedAt,
    String summary,
    List<String> pros,
    List<String> cons,
    List<String> keywords
) {
    public static ReviewSummaryResponse from(ReviewSummary e) {
        return new ReviewSummaryResponse(
            e.getProductId(),
            e.getGeneratedAt(),
            e.getSummary(),
            e.getPros(),
            e.getCons(),
            e.getKeywords()
        );
    }
}