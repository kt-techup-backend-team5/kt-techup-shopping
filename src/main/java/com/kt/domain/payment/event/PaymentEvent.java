package com.kt.domain.payment.event;

public class PaymentEvent {

	/**
	 * 결제 성공 이벤트
	 */
	public record Success(
			Long paymentId,
			Long orderId
	) {
	}

	/**
	 * 결제 실패 이벤트
	 */
	public record Failed(
			Long paymentId,
			Long orderId,
			String reason) {
	}

	/**
	 * 결제 취소 이벤트
	 */
	public record Cancelled(
			Long paymentId,
			Long orderId) {
	}
}