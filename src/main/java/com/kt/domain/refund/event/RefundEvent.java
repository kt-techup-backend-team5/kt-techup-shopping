package com.kt.domain.refund.event;

public class RefundEvent {

	/**
	 * 환불/반품 승인 완료 이벤트
	 * 포인트 회수 트리거
	 */
	public record Approved(
		Long refundId,
		Long orderId,
		Long userId
	) {
	}

	/**
	 * 환불/반품 거절 이벤트
	 */
	public record Rejected(
		Long refundId,
		Long orderId,
		String reason
	) {
	}
}
