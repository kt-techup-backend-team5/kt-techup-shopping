package com.kt.service;

import com.kt.common.exception.ErrorCode;
import com.kt.common.support.Preconditions;
import com.kt.domain.order.Order;
import com.kt.domain.order.Receiver;
import com.kt.dto.order.OrderRequest;
import com.kt.dto.order.OrderResponse;
import com.kt.repository.address.AddressRepository;
import com.kt.repository.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserOrderService {
	private final OrderRepository orderRepository;
	private final AddressRepository addressRepository;

	// 주문 상세 조회
	@Transactional(readOnly = true)
	public OrderResponse.Detail getByIdForUser(Long userId, Long orderId) {
		var order = orderRepository.findByIdAndUserIdOrThrow(orderId, userId);
		return mapToDetail(order);
	}

	// 주문 목록 조회
	@Transactional(readOnly = true)
	public Page<OrderResponse.Summary> listMyOrders(Long userId, Pageable pageable) {
		var page = orderRepository.findAllByUserId(userId, pageable);
		return page.map(this::mapToSummary);
	}

	/**
	 * 주문 배송지 및 요청사항 변경
	 * AddressId를 받아 DB에서 주소 정보를 조회한 뒤 스냅샷을 갱신합니다.
	 */
	@Transactional
	public void updateOrder(Long userId, Long orderId, OrderRequest.Update request) {
		var order = orderRepository.findByIdAndUserIdOrThrow(orderId, userId);
		Preconditions.validate(order.canUpdate(), ErrorCode.CANNOT_UPDATE_ORDER);

		var address = addressRepository.findByIdAndUserIdOrThrow(request.addressId(), userId);

		var newReceiver = new Receiver(
			address.getName(),
			address.getMobile(),
			address.getZipcode(),
			address.getAddress(),
			address.getDetailAddress()
		);

		order.changeDeliveryInfo(newReceiver, request.deliveryRequest());
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

		long totalPrice = order.getTotalPrice();

		return new OrderResponse.Detail(
			order.getId(),
			order.getReceiver().getName(),
			order.getReceiver().getMobile(),
			order.getReceiver().getZipcode(),
			order.getReceiver().getAddress(),
			order.getReceiver().getDetailAddress(),
			order.getDeliveryRequest(),
			items,
			totalPrice,
			order.getUsedPoints(),
			order.getStatus(),
			order.getCreatedAt()
		);
	}

	private OrderResponse.Summary mapToSummary(Order order) {
		var firstProductName = order.getOrderProducts().stream()
			.map(op -> op.getProduct().getName())
			.findFirst()
			.orElse(null);

		return new OrderResponse.Summary(
			order.getId(),
			order.getTotalPrice(),
			order.getCreatedAt(),
			order.getStatus(),
			firstProductName,
			order.getOrderProducts().size()
		);
	}
}