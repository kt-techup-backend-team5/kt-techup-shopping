package com.kt.domain.payment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
	PAYMENT_PENDING("결제대기"),
	PAYMENT_SUCCESS("결제완료"),
	PAYMENT_FAILED("결제실패"),
	PAYMENT_CANCELLED("결제 취소");

	private final String description;
}
