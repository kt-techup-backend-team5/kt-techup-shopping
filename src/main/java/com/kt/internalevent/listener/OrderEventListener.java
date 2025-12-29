package com.kt.internalevent.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kt.common.exception.ErrorCode;
import com.kt.domain.order.Order;
import com.kt.domain.payment.event.PaymentEvent;
import com.kt.repository.order.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Order 도메인 이벤트 리스너
 * Payment 도메인에서 발행하는 이벤트를 수신하여 Order 상태를 변경
 */
@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class OrderEventListener {
	private final OrderRepository orderRepository;

	/**
	 * 결제 성공 이벤트 처리
	 * Order 상태를 ORDER_ACCEPTED로 변경
	 */
	@EventListener(PaymentEvent.Success.class)
	public void onPaymentSuccess(PaymentEvent.Success event) {
		log.info("결제 성공 이벤트 수신 - paymentId: {}, orderId: {}", event.paymentId(), event.orderId());

		Order order = orderRepository.findByOrderIdOrThrow(event.orderId());
		order.acceptPayment(event.paymentId());

		log.info("주문 결제 완료 처리 완료 - orderId: {}, status: {}", event.orderId(), order.getStatus());
	}

	/**
	 * 결제 실패 이벤트 처리
	 * Order 상태를 ORDER_CANCELLED로 변경
	 */
	@EventListener(PaymentEvent.Failed.class)
	public void onPaymentFailed(PaymentEvent.Failed event) {
		log.info("결제 실패 이벤트 수신 - paymentId: {}, orderId: {}, reason: {}",
			event.paymentId(), event.orderId(), event.reason());

		Order order = orderRepository.findByOrderIdOrThrow(event.orderId());
		order.cancelByPaymentFailure();

		log.info("결제 실패로 주문 취소 처리 완료 - orderId: {}, status: {}", event.orderId(), order.getStatus());
	}

	/**
	 * 결제 취소 이벤트 처리
	 * TODO: 환불 처리 로직 구현 필요
	 *  - Refund 엔티티 생성
	 *  - 결제 취소 API 호출 (PG사)
	 *  - 재고 복원
	 */
	@EventListener(PaymentEvent.Cancelled.class)
	public void onPaymentCancelled(PaymentEvent.Cancelled event) {
		log.info("결제 취소 이벤트 수신 - paymentId: {}, orderId: {}", event.paymentId(), event.orderId());
		// TODO: 환불 처리 로직 구현
	}
}