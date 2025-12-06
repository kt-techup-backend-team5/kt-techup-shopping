package com.kt.domain.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
	PENDING("결제대기"),
	COMPLETED("결제완료"),
	CANCEL_REQUESTED("취소요청"),
	CANCELLED("주문취소"),
	PREPARING("배송준비중"),
	SHIPPING("배송중"),
	DELIVERED("배송완료"),
	CONFIRMED("구매확정"),

	// 환불/반품 관련 상태
	REFUND_REQUESTED("환불요청"),
	REFUND_COMPLETED("환불완료"),
	RETURN_REQUESTED("반품요청"),
	RETURN_COMPLETED("반품완료");

	private final String description;
}
