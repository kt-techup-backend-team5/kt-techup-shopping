package com.kt.dto.review;

import com.kt.domain.review.Review;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminReviewResponse {

	private final Long id;
	private final String content;
	private final int rating;
	private final String authorName;
	private final String productName;
	private final boolean isBlinded;
	private final String blindReason;
	private final LocalDateTime blindedAt;
	private final String blindedByName;
	private final LocalDateTime createdAt;

	public AdminReviewResponse(Review review) {
		this.id = review.getId();
		this.content = review.getContent();
		this.rating = review.getRating();
		this.authorName = review.getUser().getName();
		this.productName = review.getProduct().getName();
		this.isBlinded = review.isBlinded();
		this.blindReason = review.getBlindReason();
		this.blindedAt = review.getBlindedAt();
		this.blindedByName = review.getBlindedBy() != null ? review.getBlindedBy().getName() : null;
		this.createdAt = review.getCreatedAt();
	}
}