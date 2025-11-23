package com.kt.dto.payment;

import com.kt.domain.payment.PaymentType;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public record PaymentRequest(
	@Getter
	@NotNull
	PaymentType paymentType // 어떤 결제 수단으로 결제할지
) {

}
