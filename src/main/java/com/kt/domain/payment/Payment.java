package com.kt.domain.payment;

import com.kt.common.BaseEntity;
import com.kt.domain.order.Order;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Payment extends BaseEntity {
	// private Long totalPrice;
	private Long deliveryFee;
	@Enumerated(EnumType.STRING)
	private PaymentType type;
	private Long originalPrice;  // 할인 전 주문 총 금액
	private Long discountPrice; // 할인 금액
	private Long finalPrice; // 최종 결제 금액(실제 결제해야하는 돈)

	public Payment(Order order, PaymentType type, Long originalPrice, Long discountPrice, Long deliveryFee,
		Long finalPrice) {
		this.order = order;
		this.type = type;
		this.originalPrice = originalPrice;
		this.discountPrice = discountPrice;
		this.deliveryFee = deliveryFee;
		this.finalPrice = finalPrice;
	}

	@OneToOne
	private Order order;
}
