package com.kt.domain.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
	PENDING("결제대기"),
	COMPLETED("결제완료"),
	CANCELLED("주문취소"),
	SHIPPED("배송중"),
	DELIVERED("배송완료"),
	CONFIRMED("구매확정");

	private final String description;
}
