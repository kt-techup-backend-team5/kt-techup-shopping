package com.kt.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kt.common.exception.ErrorCode;
import com.kt.common.support.Preconditions;
import com.kt.domain.order.Order;
import com.kt.domain.order.OrderStatus;
import com.kt.domain.payment.Payment;
import com.kt.domain.payment.PaymentType;
import com.kt.domain.payment.event.PaymentEvent;
import com.kt.repository.order.OrderRepository;
import com.kt.repository.payment.PaymentRepository;
import com.kt.repository.payment.PaymentTypeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {
	private final OrderRepository orderRepository;
	private final PaymentRepository paymentRepository;
	private final PaymentTypeRepository paymentTypeRepository;
	private final ApplicationEventPublisher eventPublisher;

	public void pay(Long orderId, PaymentType paymentType) {
		// 주문 정보 가져오기
		Order order = orderRepository.findByOrderIdOrThrow(orderId);

		// 주문 상태 확인하기(이미 결제 되었는지)
		Preconditions.validate(order.getStatus() == OrderStatus.ORDER_CREATED, ErrorCode.ALREADY_PAID_ORDER);

		// 활성화된 결제 타입인지 확인
		Preconditions.validate(paymentType.canUse(), ErrorCode.NOT_FOUND_PAYMENT_TYPE);

		// 결제 금액 계산 (배송비 3000원 고정)
		final long originalPrice = order.getTotalPrice();  // 주문의 총 상품 금액
		final long deliveryFee = 3000;
		// 사용한 포인트
		final long discountPrice = order.getUsedPoints();  // 포인트를 할인으로 처리 (쿠폰 미구현)
		final long finalPrice = originalPrice - discountPrice + deliveryFee;

		// 결제(Payment) 엔티티 생성하고 저장하기
		Payment newPayment = new Payment(
			order,
			paymentType,
			originalPrice,
			discountPrice,
			deliveryFee,
			finalPrice
		);
		paymentRepository.save(newPayment);

		// TODO: 실제 결제 API 호출 (PG사 연동)
		// 현재는 항상 성공하는 것으로 가정
		newPayment.markAsSuccess();

		// 결제 성공 이벤트 발행 -> OrderEventListener가 수신하여 Order 상태 변경
		eventPublisher.publishEvent(new PaymentEvent.Success(newPayment.getId(), orderId));
	}
}
