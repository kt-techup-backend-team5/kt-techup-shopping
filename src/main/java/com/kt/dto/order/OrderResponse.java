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

	record ProductItem(
		String productName,
		Long quantity,
		Long price	// 개발 금액이나 상품 가격 같은거
	) {}

	record Detail(
		Long id,
		String receiverName,
		String receiverAddress,
		String receiverMobile,
		Long totalPrice,
		OrderStatus status,
		LocalDateTime createdAt,
		List<ProductItem> productItems // 한 번의 주문에 여러 종류 상품 포함
	) {
		public static Detail of(Order order) {
			var productItems = order.getOrderProducts().stream()
				.map(op -> new ProductItem(
					op.getProduct().getName(),
					op.getQuantity(),
					op.getProduct().getPrice()
				))
				.toList();
			return new Detail(
				order.getId(),
				order.getReceiver().getName(),
				order.getReceiver().getAddress(),
				order.getReceiver().getMobile(),
				order.getTotalPrice(),
				order.getStatus(),
				order.getCreatedAt(),
				productItems
			);
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