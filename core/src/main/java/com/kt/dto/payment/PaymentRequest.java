package com.kt.dto.payment;

import jakarta.validation.constraints.NotBlank;

public record PaymentRequest(
	@NotBlank(message = "결제 타입은 필수입니다")
	String paymentType // 어떤 결제 수단으로 결제할지 (CASH, CARD, PAY 등)
) {
}