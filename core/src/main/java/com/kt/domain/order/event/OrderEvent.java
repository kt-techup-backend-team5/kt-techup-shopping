package com.kt.domain.order.event;

public class OrderEvent {

	/**
	 * 구매 확정 이벤트
	 * 포인트 적립 트리거
	 */
	public record Confirmed(
		Long orderId,
		Long userId,
		Long actualPaymentAmount  // 실결제 금액 (포인트/쿠폰 할인 후)
	) {
	}
}
