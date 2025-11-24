package com.kt.dto.order;

import java.time.LocalDateTime;
import java.util.List;

import com.kt.domain.order.Order;
import com.kt.domain.order.OrderStatus;
import com.querydsl.core.annotations.QueryProjection;

public interface OrderResponse {
	record Search(
		Long id,
		String receiverName,
		String productName,
		Long quantity,
		Long totalPrice,
		OrderStatus status,
		LocalDateTime createdAt
	) {
		@QueryProjection
		public Search {
		}
	}

	record Item(
		Long productId,
		String productName,
		Long price,
		Long quantity,
		Long lineTotal
	) {
	}

	// 상세조회용
	record Detail(
		Long id,
		String receiverName,
		String receiverAddress,
		String receiverMobile,
		List<Item> items,
		Long totalPrice,
		OrderStatus status,
		LocalDateTime createdAt
	) {
	}

	// 목록용
	record Summary(
		Long orderId,
		Long totalPrice,
		LocalDateTime createdAt,
		OrderStatus status,
		String firstProductName,
		int productCount
	) {
	}
}