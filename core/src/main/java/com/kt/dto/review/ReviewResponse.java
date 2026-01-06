package com.kt.dto.review;

import com.kt.domain.review.Review;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReviewResponse {

	private final Long id;
	private final String content;
	private final int rating;
	private final String authorName;
	private final boolean isBlinded;
	private final LocalDateTime createdAt;

	public ReviewResponse(Review review) {
		this.id = review.getId();
		this.content = review.getContent();
		this.rating = review.getRating();
		this.authorName = review.getUser().getName();
		this.isBlinded = review.isBlinded();
		this.createdAt = review.getCreatedAt();
	}
}
