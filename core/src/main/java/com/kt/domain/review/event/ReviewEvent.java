package com.kt.domain.review.event;

public class ReviewEvent {

	/**
	 * 리뷰 작성 이벤트
	 * 포인트 적립 트리거 (100P)
	 */
	public record Created(
		Long reviewId,
		Long userId,
		Long orderProductId,
		Long productId
	) {
	}

	/**
	 * 리뷰 블라인드 이벤트
	 * 포인트 회수 트리거 (-100P)
	 */
	public record Blinded(
		Long reviewId,
		Long userId,
		Long orderProductId,
		String reason
	) {
	}
}
