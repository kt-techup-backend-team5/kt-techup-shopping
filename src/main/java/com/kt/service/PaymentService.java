package com.kt.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kt.common.CustomException;
import com.kt.common.ErrorCode;
import com.kt.common.Preconditions;
import com.kt.domain.order.Order;
import com.kt.domain.order.OrderStatus;
import com.kt.domain.payment.Payment;
import com.kt.domain.payment.PaymentType;
import com.kt.repository.order.OrderRepository;
import com.kt.repository.payment.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {
	private final OrderRepository orderRepository;
	private final PaymentRepository paymentRepository;

	public void pay(Long orderId, PaymentType paymentType) {
		// 주문 정보 가져오기
		Order order = orderRepository.findByOrderIdOrThrow(orderId, ErrorCode.NOT_FOUND_ORDER);

		// 주문 상태 확인하기(이미 결제 되었는지)
		Preconditions.validate(order.getStatus() == OrderStatus.PENDING, ErrorCode.ALREADY_PAID_ORDER);

		// TODO: 배송비랑 포인트, 쿠폰 구현 하고 여기 부분 수정해야함
		// 결제 금액 계산 (임시로 배송비 3000원 고정)
		final long originalPrice = order.getTotalPrice();  // 주문의 총 상품 금액
		final long deliveryFee = 3000;
		final long discountPrice = 0;  // 현재 할인쿤폰 포인트 미구현으로 0으로 고정
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

		// Order의 상태를 결제완료(COMPLETED)로 변경
		order.setPaid();
	}
}
