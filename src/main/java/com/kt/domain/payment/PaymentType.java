package com.kt.domain.payment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentType {
	CASH("현금"),
	CARD("카드"),
	PAY("간편결제");

	private final String description;
}
