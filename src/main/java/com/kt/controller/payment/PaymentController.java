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
	public ApiResult<Void> pay(@PathVariable Long orderId, PaymentRequest request){
		paymentService.pay(orderId, request.getPaymentType());
		return ApiResult.ok();
	}

}
/*
   1. API 엔드포인트 만들기: POST /orders/{order_id}/pay 같은 결제 요청을 받을 API를 PaymentController에
      만듭니다.결제 정보 받기: 이 API는 요청 본문(body)으로 "어떤 주문 ID에 대해", "어떤 결제 수단(PaymentType)으로"
      결제할 것인지 정보를 받습니다.
   2. 결제 정보 받기: 이 API는 요청 본문(body)으로 "어떤 주문 ID에 대해", "어떤 결제 수단(PaymentType)으로"
      결제할 것인지 정보를 받습니다.
   3. Mock 결제 처리 (핵심 로직):
       - 전달받은 order_id로 주문 정보를 조회합니다.
       - 주문 정보와 배송비 등을 바탕으로 최종 결제 금액을 계산합니다. (total_price, delivery_fee,
         discount_price 등을 사용)
       - `Payment` 엔티티(영수증)를 생성하고, 계산된 금액들과 어떤 카드로 결제했는지(payment_type) 등을 채워서
         데이터베이스에 저장합니다.
       - 마지막으로, 원래 Order의 상태를 PAID(결제 완료)로 변경합니다.
 */
