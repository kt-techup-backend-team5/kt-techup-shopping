package com.kt.controller.payment;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kt.common.ApiResult;
import com.kt.dto.payment.PaymentRequest;
import com.kt.service.PaymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class PaymentController {
	private final PaymentService paymentService;

	/**
	 * 특정 주문에 대한 결제를 요청합니다
	 * @param orderId 는 결제할 주문 ID
	 * @param request 결제 수단 타입(현금, 카드, 간편결제)
	 */
	@PostMapping("/{orderId}/pay")
	public ApiResult<Void> pay(@PathVariable Long orderId, PaymentRequest request) {
		paymentService.pay(orderId, request.getPaymentType());
		return ApiResult.ok();
	}

}