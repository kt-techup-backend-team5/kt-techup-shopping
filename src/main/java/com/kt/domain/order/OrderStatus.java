package com.kt.domain.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
	ORDER_CREATED("주문생성(결제 전)"),
	ORDER_ACCEPTED("주문접수(결제 성공)"),
	ORDER_PREPARING("배송준비중"),
	ORDER_SHIPPING("배송중"),
	ORDER_DELIVERED("배송완료"),
	ORDER_CONFIRMED("구매확정"),
	ORDER_CANCELLED("주문취소");

	private final String description;

	/**
	 * 결제가 완료된 주문인지 확인
	 */
	public boolean isPaid() {
		return this != ORDER_CREATED && this != ORDER_CANCELLED;
	}

	/**
	 * 배송 프로세스를 시작할 수 있는 상태인지 확인
	 */
	public boolean canStartShipping() {
		return this == ORDER_ACCEPTED;
	}

	/**
	 * 취소 가능한 상태인지 확인
	 */
	public boolean isCancellable() {
		return this == ORDER_CREATED || this == ORDER_ACCEPTED || this == ORDER_PREPARING;
	}
}
