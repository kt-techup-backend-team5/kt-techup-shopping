package com.kt.repository.review;

import com.kt.domain.review.Review;
import com.kt.dto.review.ReviewSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewRepositoryCustom {
    Page<Review> searchReviews(ReviewSearchCondition condition, Pageable pageable);
}
