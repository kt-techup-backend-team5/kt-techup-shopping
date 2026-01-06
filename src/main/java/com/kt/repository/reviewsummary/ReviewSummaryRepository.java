package com.kt.repository.reviewsummary;

import com.kt.domain.reviewsummary.ReviewSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewSummaryRepository extends JpaRepository<ReviewSummary, Long> {
    Optional<ReviewSummary> findByProductId(Long productId);
}