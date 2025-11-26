package com.kt.service;

import com.kt.common.support.Preconditions;
import com.kt.domain.order.Order;
import com.kt.dto.order.OrderRequest;
import com.kt.dto.order.OrderResponse;
import com.kt.repository.order.OrderRepository;

import lombok.RequiredArgsConstructor;

import com.kt.common.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserOrderService {
	private final OrderRepository orderRepository;

	// 주문 상세 조회
	@Transactional(readOnly = true)
	public OrderResponse.Detail getByIdForUser(Long userId, Long orderId) {
		var order = orderRepository.findByIdAndUserIdOrThrow(orderId, userId, ErrorCode.NOT_FOUND_ORDER);
		return mapToDetail(order);
	}

	// 주문 목록 조회
	@Transactional(readOnly = true)
	public Page<OrderResponse.Summary> listMyOrders(Long userId, Pageable pageable) {
		var page = orderRepository.findAllByUserId(userId, pageable);
		return page.map(this::mapToSummary);
	}

    @Transactional
    public void updateOrder(Long userId, Long orderId, OrderRequest.Update request) {
        var order = orderRepository.findByIdAndUserIdOrThrow(orderId, userId, ErrorCode.NOT_FOUND_ORDER);

        Preconditions.validate(order.canUpdate(), ErrorCode.CANNOT_UPDATE_ORDER);
        order.changeReceiver(
                request.receiverName(),
                request.receiverAddress(),
                request.receiverMobile()
        );
    }

	private OrderResponse.Detail mapToDetail(Order order) {
		var items = order.getOrderProducts().stream().map(op -> {
			var product = op.getProduct();
			var price = product.getPrice();
			var qty = op.getQuantity();
			var lineTotal = price * qty;
			return new OrderResponse.Item(
				product.getId(),
				product.getName(),
				price,
				qty,
				lineTotal
			);
		}).toList();

		long totalPrice = items.stream()
			.mapToLong(OrderResponse.Item::lineTotal)
			.sum();

		return new OrderResponse.Detail(
			order.getId(),
			order.getReceiver().getName(),
			order.getReceiver().getAddress(),
			order.getReceiver().getMobile(),
			items,
			totalPrice,
			order.getStatus(),
			order.getCreatedAt()
		);
	}

	private OrderResponse.Summary mapToSummary(Order order) {
		var firstProductName = order.getOrderProducts().stream()
			.map(op -> op.getProduct().getName())
			.findFirst()
			.orElse(null);

		long totalPrice = order.getOrderProducts().stream()
			.mapToLong(op -> op.getProduct().getPrice() * op.getQuantity())
			.sum();

		return new OrderResponse.Summary(
			order.getId(),
			totalPrice,
			order.getCreatedAt(),
			order.getStatus(),
			firstProductName,
			order.getOrderProducts().size()
		);
	}
}