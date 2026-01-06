package com.kt.domain.payment;

import com.kt.common.exception.ErrorCode;
import com.kt.common.support.BaseEntity;
import com.kt.common.support.Preconditions;
import com.kt.domain.order.Order;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Payment extends BaseEntity {
	// private Long totalPrice;
	private Long deliveryFee;

	@ManyToOne
	@JoinColumn(name = "payment_type_id")
	private PaymentType paymentType;

	private Long originalPrice;  // 할인 전 주문 총 금액
	private Long discountPrice; // 할인 금액
	private Long finalPrice; // 최종 결제 금액(실제 결제해야하는 돈)

	@Enumerated(EnumType.STRING)
	private PaymentStatus status;

	public Payment(Order order, PaymentType paymentType, Long originalPrice, Long discountPrice, Long deliveryFee,
		Long finalPrice) {
		this.order = order;
		this.paymentType = paymentType;
		this.originalPrice = originalPrice;
		this.discountPrice = discountPrice;
		this.deliveryFee = deliveryFee;
		this.finalPrice = finalPrice;
		this.status = PaymentStatus.PAYMENT_PENDING;
	}

	@OneToOne
	private Order order;

	/**
	 * 결제 성공 처리
	 */
	public void markAsSuccess() {
		Preconditions.validate(this.status == PaymentStatus.PAYMENT_PENDING, ErrorCode.INVALID_PAYMENT_STATUS);
		this.status = PaymentStatus.PAYMENT_SUCCESS;
	}

	/**
	 * 결제 실패 처리
	 */
	public void markAsFailed() {
		Preconditions.validate(this.status == PaymentStatus.PAYMENT_PENDING, ErrorCode.INVALID_PAYMENT_STATUS);
		this.status = PaymentStatus.PAYMENT_FAILED;
	}

	/**
	 * 결제 취소 처리
	 */
	public void cancel() {
		Preconditions.validate(this.status == PaymentStatus.PAYMENT_SUCCESS, ErrorCode.INVALID_PAYMENT_STATUS);
		this.status = PaymentStatus.PAYMENT_CANCELLED;
	}

	/**
	 * 결제가 성공 상태인지 확인
	 */
	public boolean isSuccess() {
		return this.status == PaymentStatus.PAYMENT_SUCCESS;
	}
}
