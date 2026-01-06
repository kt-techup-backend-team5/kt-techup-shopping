package com.kt.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kt.domain.review.Review;
import com.kt.domain.reviewsummary.ReviewSummary;
import com.kt.dto.reviewsummary.ReviewSummaryResponse;
import com.kt.repository.review.ReviewRepository;
import com.kt.repository.reviewsummary.ReviewSummaryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 리뷰 요약 캐시를 조회/갱신하고, 필요 시 AI로 요약 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewSummaryService {

    private final ReviewRepository reviewRepository;
    private final ReviewSummaryRepository reviewSummaryRepository;
    private final ReviewSummaryGenerator reviewSummaryGenerator;

    @Value("${review-summary.max-reviews:30}")
    private int maxReviews;

    @Value("${review-summary.ttl-hours:24}")
    private int ttlHours;

    @Transactional
    public ReviewSummaryResponse getOrGenerate(Long productId) {
        var now = LocalDateTime.now();

        var latestReviews = reviewRepository.findRecentForSummary(productId, PageRequest.of(0, 1));
        Long latestReviewId = latestReviews.isEmpty() ? null : latestReviews.get(0).getId();

        var cached = reviewSummaryRepository.findByProductId(productId).orElse(null);
        if (cached != null && cached.isValid(now) && equalsLong(cached.getLastReviewIdUsed(), latestReviewId)) {
            return ReviewSummaryResponse.from(cached);
        }

        var reviews = reviewRepository.findRecentForSummary(productId, PageRequest.of(0, maxReviews));
        if (reviews.isEmpty()) {
            var empty = ReviewSummary.of(
                productId,
                0,
                null,
                "아직 리뷰가 충분하지 않아 요약을 제공할 수 없습니다.",
                List.of(),
                List.of(),
                List.of(),
                "none",
                "v1",
                now,
                now.plusHours(ttlHours)
            );

            var saved = replaceCache(cached, empty);
            return ReviewSummaryResponse.from(saved);
        }

        var avgRating = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        Map<Integer, Long> ratingCounts = reviews.stream()
            .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));

        var lastReviewIdUsed = reviews.get(0).getId();

        try {
            var ai = reviewSummaryGenerator.generate(reviews, avgRating, ratingCounts);

            var entity = ReviewSummary.of(
                productId,
                reviews.size(),
                lastReviewIdUsed,
                ai.summary(),
                ai.pros(),
                ai.cons(),
                ai.keywords(),
                ai.model(),
                ai.promptVersion(),
                now,
                now.plusHours(ttlHours)
            );

            var saved = replaceCache(cached, entity);
            return ReviewSummaryResponse.from(saved);

        } catch (Exception e) {
            log.warn("리뷰 요약 생성 실패 - productId={}, reviews={}", productId, reviews.size(), e);
            if (cached != null) {
                return ReviewSummaryResponse.from(cached);
            }

            var failed = ReviewSummary.of(
                productId,
                reviews.size(),
                lastReviewIdUsed,
                "리뷰 요약 생성에 실패했습니다. 잠시 후 다시 시도해주세요.",
                List.of(),
                List.of(),
                List.of(),
                "none",
                "v1",
                now,
                now.plusHours(1)
            );

            var saved = replaceCache(null, failed);
            return ReviewSummaryResponse.from(saved);
        }
    }

    private ReviewSummary replaceCache(ReviewSummary oldCache, ReviewSummary newCache) {
        if (oldCache != null) {
            reviewSummaryRepository.delete(oldCache);
            reviewSummaryRepository.flush();
        }

        try {
            return reviewSummaryRepository.save(newCache);
        } catch (DataIntegrityViolationException e) {
            return reviewSummaryRepository.findByProductId(newCache.getProductId())
                .orElse(newCache);
        }
    }

    private boolean equalsLong(Long a, Long b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}