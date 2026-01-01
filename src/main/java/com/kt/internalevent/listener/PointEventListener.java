package com.kt.internalevent.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kt.domain.order.event.OrderEvent;
import com.kt.domain.refund.event.RefundEvent;
import com.kt.domain.review.event.ReviewEvent;
import com.kt.service.PointService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Point 도메인 이벤트 리스너
 * Order, Review 도메인에서 발행하는 이벤트를 수신하여 포인트 적립/회수 처리
 */
@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class PointEventListener {
	private final PointService pointService;

	/**
	 * 구매 확정 이벤트 처리
	 * 실결제 금액의 5%를 포인트로 적립
	 */
	@EventListener(OrderEvent.Confirmed.class)
	public void onOrderConfirmed(OrderEvent.Confirmed event) {
		log.info("구매 확정 이벤트 수신 - orderId: {}, userId: {}, amount: {}",
				event.orderId(), event.userId(), event.actualPaymentAmount());

		pointService.creditPointsForOrder(event.userId(), event.orderId(), event.actualPaymentAmount());

		log.info("구매 확정 포인트 적립 처리 완료 - orderId: {}", event.orderId());
	}

	/**
	 * 환불/반품 승인 완료 이벤트 처리
	 * 1. 적립된 포인트(5%) 회수
	 * 2. 사용한 포인트 복구
	 */
	@EventListener(RefundEvent.Approved.class)
	public void onRefundApproved(RefundEvent.Approved event) {
		log.info("환불 승인 이벤트 수신 - refundId: {}, orderId: {}, userId: {}",
				event.refundId(), event.orderId(), event.userId());

		// 1. 적립된 포인트(5%) 회수
		pointService.retrievePointsForRefund(event.userId(), event.orderId());

		// 2. 주문 시 사용한 포인트 복구
		pointService.refundUsedPointsForRefund(event.userId(), event.orderId());

		log.info("환불 포인트 처리 완료 (회수 + 복구) - orderId: {}", event.orderId());
	}

	/**
	 * 리뷰 작성 이벤트 처리
	 * 100P 적립
	 */
	@EventListener(ReviewEvent.Created.class)
	public void onReviewCreated(ReviewEvent.Created event) {
		log.info("리뷰 작성 이벤트 수신 - reviewId: {}, userId: {}, orderProductId: {}",
				event.reviewId(), event.userId(), event.orderProductId());

		pointService.creditPointsForReview(event.userId(), event.reviewId(), event.orderProductId());

		log.info("리뷰 작성 포인트 적립 처리 완료 - reviewId: {}", event.reviewId());
	}

	/**
	 * 리뷰 블라인드 이벤트 처리
	 * 100P 회수
	 */
	@EventListener(ReviewEvent.Blinded.class)
	public void onReviewBlinded(ReviewEvent.Blinded event) {
		log.info("리뷰 블라인드 이벤트 수신 - reviewId: {}, userId: {}, orderProductId: {}",
				event.reviewId(), event.userId(), event.orderProductId());

		pointService.retrievePointsForReviewBlind(event.userId(), event.reviewId(), event.orderProductId());

		log.info("리뷰 블라인드 포인트 회수 처리 완료 - reviewId: {}", event.reviewId());
	}
}
